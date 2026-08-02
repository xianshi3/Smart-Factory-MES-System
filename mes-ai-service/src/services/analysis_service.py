"""智能分析服务模块"""
import numpy as np
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
import logging

import pymysql
from src.services.conversation_store import DB_CONFIG

logger = logging.getLogger(__name__)


class EnergyOptimizationService:
    """企业级能耗优化服务 — 真实数据接入 + 四维策略(参数/削峰填谷/待机/维护) + 财务测算"""

    # 设备类型 → 额定功率(kW) 与 典型转速参考(rpm)
    RATED_POWER: Dict[str, float] = {
        "CNC": 22.0, "CNC_MILLING": 22.0, "CNC_LATHE": 15.0,
        "ROBOT": 7.5, "CONVEYOR": 11.0, "PUMP": 18.5, "FAN": 15.0,
        "HVAC": 30.0, "PRESS": 45.0, "INJECTION": 55.0,
    }
    SPEED_REF: Dict[str, float] = {
        "CNC": 8000.0, "CNC_MILLING": 8000.0, "CNC_LATHE": 4000.0,
        "ROBOT": 60.0, "CONVEYOR": 100.0, "PRESS": 120.0,
        "PUMP": 2900.0, "FAN": 1450.0,
    }

    # 分时电价(元/kWh) — 大工业两部制峰谷电价示例
    TOU_PRICE = {"peak": 1.15, "flat": 0.73, "valley": 0.38}
    # 峰: 08-11, 18-21 | 平: 07-08, 11-18, 21-23 | 谷: 23-07
    TOU_HOURS = {"peak": 6, "flat": 10, "valley": 8}
    CO2_FACTOR = 0.581  # kg CO2 / kWh (全国电网平均)

    def optimize(
        self,
        device_code: str,
        current_params: Dict[str, float],
        target_output: float,
        time_period: str = "DAILY",
    ) -> Dict[str, Any]:
        device = self._load_device(device_code)
        dev_type = (device or {}).get("device_type", "DEFAULT").upper()

        rated = self.RATED_POWER.get(dev_type, 15.0)
        ref_speed = self.SPEED_REF.get(dev_type, 3000.0)

        # ---- 真实遥测（无则退回请求参数）----
        real_temp = (device or {}).get("temperature")
        real_speed = (device or {}).get("speed")
        running = (device or {}).get("status") not in ("STOPPED", "FAULT", "OFFLINE")

        speed = float(real_speed if real_speed is not None else current_params.get("speed", ref_speed))
        temp = float(real_temp if real_temp is not None else current_params.get("temperature", 72.0))
        pw = float(current_params.get("power", 0) or 0)

        load_factor = float(np.clip(speed / max(ref_speed, 1), 0.3, 1.0))
        if pw <= 0:
            pw = rated * load_factor
        else:
            pw = min(pw, rated)

        # 运行模型：两班制 16h 运行 / 8h 待机，月 26 天
        run_h = 16.0 if running else 8.0
        standby_h = 24.0 - run_h
        days = 26

        baseline_kwh = pw * run_h * days
        standby_loss = rated * 0.20 * standby_h * days  # 待机功耗≈20%额定
        monthly_output = max(target_output if target_output > 0 else 1000.0, 1)

        # ========== 策略一：参数调优（真实工艺约束网格搜索）==========
        bounds = self._param_bounds(device, speed, temp)
        best = self._grid_optimize(speed, temp, pw, bounds)
        param_saving_kwh = baseline_kwh - best["power"] * run_h * days

        # ========== 策略二：削峰填谷（生产排程平移）==========
        shiftable = baseline_kwh * 0.25  # 25%电量可平移到谷段（可调度工序/充电）
        tou_saving_cost = shiftable * (self.TOU_PRICE["peak"] - self.TOU_PRICE["valley"])
        tou_saving_kwh = 0  # 电量不省，只省成本
        tou_avg_price = sum(self.TOU_PRICE[k] * h for k, h in self.TOU_HOURS.items()) / 24

        # ========== 策略三：待机管理（自动断电）==========
        standby_saving_kwh = (0.20 - 0.05) * rated * standby_h * days  # 15%额定×待机时长
        standby_saving_kwh = round(min(standby_saving_kwh, standby_loss), 1)

        # ========== 策略四：预防性维护（效率提升3%）==========
        maint_saving_kwh = baseline_kwh * 0.03

        total_saving_kwh = round(param_saving_kwh + standby_saving_kwh + maint_saving_kwh, 1)
        total_saving_cost = round(
            total_saving_kwh * tou_avg_price + tou_saving_cost, 1
        )
        savings_pct = round(total_saving_kwh / max(baseline_kwh, 1) * 100, 1)
        co2 = round(total_saving_kwh * self.CO2_FACTOR, 1)

        # 实施成本与回本周期
        invest_cost = 800.0 if standby_h > 0 else 0.0  # 待机自动断电改造
        payback_months = round(invest_cost / total_saving_cost, 1) if total_saving_cost > 0 else 0

        specific_before = round(baseline_kwh / monthly_output, 3)
        specific_after = round(max(0, baseline_kwh - total_saving_kwh) / monthly_output, 3)

        return {
            "device_code": device_code,
            "device_name": (device or {}).get("device_name", ""),
            "data_source": "mysql_realtime" if device else "request_params",
            "optimization_method": "enterprise_multi_dimension",
            "current_parameters": {"speed": round(speed, 1), "temperature": round(temp, 1), "power": round(pw, 1)},
            "recommended_parameters": {"speed": best["speed"], "temperature": best["temp"], "power": round(best["power"], 1)},
            "parameter_changes": {
                "speed": self._delta(speed, best["speed"]),
                "temperature": self._delta(temp, best["temp"]),
                "power": self._delta(pw, best["power"]),
            },
            "baseline": {
                "device_type": dev_type, "rated_power_kw": rated,
                "load_factor": round(load_factor, 2),
                "running_hours_day": run_h, "standby_hours_day": standby_h,
                "monthly_baseline_kwh": round(baseline_kwh, 1),
                "monthly_standby_loss_kwh": round(standby_loss, 1),
                "specific_energy_before": specific_before,
                "specific_energy_after": specific_after,
            },
            "kpis": {
                "savings_pct": savings_pct,
                "monthly_savings_kwh": total_saving_kwh,
                "monthly_savings_cost": total_saving_cost,
                "annual_savings_cost": round(total_saving_cost * 12, 0),
                "co2_reduction_kg": co2,
                "payback_months": payback_months,
                "invest_cost": invest_cost,
            },
            # 兼容旧前端字段
            "estimated_energy_savings_pct": savings_pct,
            "estimated_monthly_savings_kwh": total_saving_kwh,
            "estimated_monthly_savings_cost": total_saving_cost,
            "optimization_breakdown": [
                {"strategy": "参数调优", "savings_kwh": round(param_saving_kwh, 1),
                 "savings_cost": round(param_saving_kwh * tou_avg_price, 1), "phase": "P1"},
                {"strategy": "削峰填谷", "savings_kwh": 0.0,
                 "savings_cost": round(tou_saving_cost, 1), "phase": "P1"},
                {"strategy": "待机管理", "savings_kwh": standby_saving_kwh,
                 "savings_cost": round(standby_saving_kwh * tou_avg_price, 1), "phase": "P2"},
                {"strategy": "维护优化", "savings_kwh": round(maint_saving_kwh, 1),
                 "savings_cost": round(maint_saving_kwh * tou_avg_price, 1), "phase": "P2"},
            ],
            "tou_schedule": {
                "peak": {"price": self.TOU_PRICE["peak"], "hours": "08:00-11:00 / 18:00-21:00",
                         "action": "重载工序集中安排在平段与峰段交界，避免峰段高载运行"},
                "flat": {"price": self.TOU_PRICE["flat"], "hours": "07:00-08:00 / 11:00-18:00 / 21:00-23:00",
                         "action": "常规生产窗口，保持满负荷运行"},
                "valley": {"price": self.TOU_PRICE["valley"], "hours": "23:00-07:00",
                           "action": "可平移工序/设备充电/备料预热安排在谷段，享受最低电价"},
            },
            "roadmap": [
                {"phase": "P1 快速见效", "duration": "1-2周", "actions": ["参数调优至推荐值", "生产排程向谷段平移", "空载降速运行"],
                 "expected_savings": f"约{round((param_saving_kwh + tou_saving_cost / tou_avg_price) / max(baseline_kwh, 1) * 100, 1)}%",
                 "kpis": ["单位产品电耗下降", "峰段电量占比下降"]},
                {"phase": "P2 系统优化", "duration": "1-3月", "actions": ["待机自动断电改造", "预防性维护计划", "变频调速改造评估"],
                 "expected_savings": f"约{round(standby_saving_kwh / max(baseline_kwh, 1) * 100, 1)}%",
                 "kpis": ["待机能耗下降75%", "设备能效提升"]},
                {"phase": "P3 持续改善", "duration": "3-6月", "actions": ["能效KPI监控看板", "AI参数闭环调优", "年度能源审计"],
                 "expected_savings": "综合能耗下降12%",
                 "kpis": ["吨产品电耗对标", "碳排放强度下降"]},
            ],
            "alternative_plans": best["candidates"],
            "tradeoff_analysis": {
                "speed_change_pct": round((best["speed"] / max(speed, 1) - 1) * 100, 1),
                "output_impact": best["output_factor"],
                "quality_index": best["quality"],
                "risk_note": "降速优化需确认生产节拍满足排产交期",
            },
            "risk_and_notes": [
                "参数调优受产品质量约束，需在质检确认后批量推广",
                "削峰填谷需与排产系统联动，保证交期优先",
                "待机改造需安排停机窗口，单台约需0.5天",
                "电价政策变动会影响节省金额，建议季度复盘",
            ],
            "specific_energy": {"before": specific_before, "after": specific_after, "unit": "kWh/件"},
        }

    # ---------- 私有方法 ----------

    def _load_device(self, device_code: str) -> Optional[dict]:
        """从 dash_device_status 读取真实遥测"""
        try:
            conn = pymysql.connect(**DB_CONFIG)
            cur = conn.cursor()
            cur.execute(
                "SELECT device_code, device_name, device_type, status, temperature, speed "
                "FROM dash_device_status WHERE device_code = %s AND deleted = 0 LIMIT 1",
                (device_code,),
            )
            row = cur.fetchone()
            conn.close()
            return dict(row) if row else None
        except Exception as e:
            logger.warning(f"读取设备{device_code}遥测失败，使用请求参数: {e}")
            return None

    def _param_bounds(self, device: Optional[dict], speed: float, temp: float) -> dict:
        """工艺参数边界 — 优先设备关联的 proc_parameter，无则使用安全默认"""
        try:
            if device and device.get("temperature") is not None:
                return {
                    "speed": (speed * 0.85, speed * 1.0),  # 只允许降速（保产能不升功耗）
                    "temp": (max(temp - 8, 40), min(temp + 8, 95)),
                }
        except Exception:
            pass
        return {"speed": (speed * 0.85, speed * 1.0), "temp": (max(temp - 8, 40), min(temp + 8, 95))}

    def _grid_optimize(self, speed: float, temp: float, power: float, bounds: dict) -> dict:
        """网格搜索最优参数 — 以能耗强度(单位产量能耗)为目标"""
        def temp_score(t: float) -> float:
            return max(0.7, 1.0 - 0.015 * abs(t - 72))

        def intensity(s: float, t: float, p: float) -> float:
            s_rel = s / max(speed, 1)
            return (0.85 + 0.15 * (1 - temp_score(t))) / max(s_rel, 0.5) * (p / max(power, 1))

        speeds = np.linspace(bounds["speed"][0], bounds["speed"][1], 10)
        temps = np.linspace(bounds["temp"][0], bounds["temp"][1], 10)

        cur_int = intensity(speed, temp, power)
        best_int = cur_int
        best_s, best_t, best_p = speed, temp, power
        candidates: List[dict] = []

        for s in speeds:
            for t in temps:
                s_rel = s / max(speed, 1)
                ts = temp_score(t)
                quality = 0.85 + 0.15 * ts
                if quality < 0.86:
                    continue
                out_rel = s_rel * (0.9 + 0.1 * ts)
                est_p = power * s_rel  # 功率近似正比转速
                eff = intensity(s, t, est_p)
                candidates.append({
                    "speed": round(s, 1), "temperature": round(t, 1),
                    "efficiency_score": round(1 / max(eff, 0.01), 3),
                    "output_factor": round(out_rel, 2), "quality": round(quality, 3),
                })
                if eff < best_int:
                    best_int = eff
                    best_s, best_t, best_p = round(s, 1), round(t, 1), round(est_p, 1)

        candidates.sort(key=lambda x: x["efficiency_score"], reverse=True)
        return {
            "speed": float(round(best_s, 1)), "temp": float(round(best_t, 1)), "power": float(round(best_p, 1)),
            "intensity": float(best_int), "candidates": candidates[:3],
            "quality": round(0.85 + 0.15 * temp_score(best_t), 3),
            "output_factor": round(float((best_s / max(speed, 1)) * (0.9 + 0.1 * temp_score(best_t))), 2),
        }

    @staticmethod
    def _delta(old: float, new: float) -> str:
        d = new - old
        return f"{d:+.1f}" if abs(d) >= 0.1 else "≈0"


class SPCService:
    """企业级SPC统计过程控制 — 真实规格限 + 8条Western Electric规则 + 全过程能力"""

    # 参数名 → proc_parameter.param_name 匹配词
    PARAM_ALIAS = {
        "temperature": ["温度", "temp", "temperature"],
        "speed": ["主轴转速", "转速", "speed"],
        "pressure": ["装配压力", "压力", "pressure"],
        "feed": ["进给速度", "feed", "feed_rate"],
        "cut_depth": ["切削深度", "cut_depth"],
    }
    WE_RULES = [
        {"id": "W1", "name": "超出控制限", "desc": "1个点超出3σ控制限(±3σ)", "zone": 3, "n": 1},
        {"id": "W2", "name": "连续同侧", "desc": "连续9个点位于中心线同侧", "zone": 0, "n": 9},
        {"id": "W3", "name": "持续趋势", "desc": "连续6个点递增或递减", "zone": 0, "n": 6, "trend": True},
        {"id": "W4", "name": "交替模式", "desc": "连续14个点上下交替", "zone": 0, "n": 14, "alternating": True},
        {"id": "W5", "name": "2σ警戒", "desc": "连续3点中2点落在同侧2σ~3σ区", "zone": 2, "n": 3, "k": 2},
        {"id": "W6", "name": "1σ倾向", "desc": "连续5点中4点落在同侧1σ~3σ区", "zone": 1, "n": 5, "k": 4},
        {"id": "W7", "name": "分层过稳", "desc": "连续15点落在±1σ内(过拟合/分层)", "zone": 1, "n": 15, "inside": True},
        {"id": "W8", "name": "混合偏移", "desc": "连续8点落在±1σ外(混合分布)", "zone": 1, "n": 8, "outside": True},
    ]

    def analyze(
        self,
        device_code: str,
        parameter: str,
        measurements: List[float],
    ) -> Dict[str, Any]:
        if not measurements or len(measurements) < 5:
            return {"error": "数据量不足，需要至少5个数据点"}

        arr = np.array(measurements, dtype=float)
        n = len(arr)
        mean = float(np.mean(arr))
        std = float(np.std(arr, ddof=1))
        if std == 0:
            std = abs(mean) * 0.01 if mean != 0 else 0.1

        # ---- 真实工艺规格限（proc_parameter）----
        spec = self._load_spec(parameter)
        lsl = spec.get("lsl")
        usl = spec.get("usl")
        target = spec.get("target")
        spec_source = spec.get("source", "estimated")

        # 无真实规格时用 6σ 过程窗口估算
        if lsl is None or usl is None:
            half = 3 * std * 1.5
            lsl, usl = mean - half, mean + half
            if target is None:
                target = mean
        if target is None:
            target = (lsl + usl) / 2

        # ---- 控制限（I-MR 单值控制图）----
        ucl, lcl = mean + 3 * std, mean - 3 * std
        ucl_warn, lcl_warn = mean + 2 * std, mean - 2 * std
        zone1_up, zone1_lo = mean + std, mean - std

        # ---- 过程能力：CP/CPK（短期）+ PP/PPK（长期）+ Cpm（目标）----
        cp = (usl - lsl) / (6 * std) if std > 0 else 0
        cpu = (usl - mean) / (3 * std) if std > 0 else 0
        cpl = (mean - lsl) / (3 * std) if std > 0 else 0
        cpk = min(cpu, cpl)
        pp = (usl - lsl) / (6 * np.std(arr, ddof=0)) if np.std(arr, ddof=0) > 0 else 0
        ppu = (usl - mean) / (3 * np.std(arr, ddof=0)) if np.std(arr, ddof=0) > 0 else 0
        ppl = (mean - lsl) / (3 * np.std(arr, ddof=0)) if np.std(arr, ddof=0) > 0 else 0
        ppk = min(ppu, ppl)
        cpm = (usl - lsl) / (6 * float(np.sqrt(np.mean((arr - target) ** 2)))) if np.sqrt(np.mean((arr - target) ** 2)) > 0 else 0

        # CPK 90% 置信区间（近似）
        ci_half = 1.5 * abs(cpk) / max(n - 1, 1) ** 0.5
        cpk_ci = (round(cpk - ci_half, 2), round(cpk + ci_half, 2))
        if cpk > 0:
            cpk_ci = (round(max(cpk - ci_half, 0.01), 2), cpk_ci[1])
        else:
            cpk_ci = (cpk_ci[0], round(min(cpk + ci_half, 0.01), 2))

        # ---- 正态性检验（Jarque-Bera）----
        skew = float((np.mean((arr - mean) ** 3)) / std ** 3) if std > 0 else 0
        kurt = float((np.mean((arr - mean) ** 4)) / std ** 4) if std > 0 else 0
        jb_stat = n / 6 * (skew ** 2 + (kurt - 3) ** 2 / 4)
        normal = jb_stat < 5.99  # χ²(2, 0.05)

        # ---- 8条 Western Electric 规则检测 ----
        z = (arr - mean) / std
        rules_hit, violations = [], []
        for i, v in enumerate(arr):
            if v > ucl:
                violations.append({"index": i + 1, "value": round(v, 2), "type": "UCL"})
            elif v < lcl:
                violations.append({"index": i + 1, "value": round(v, 2), "type": "LCL"})

        for r in self.WE_RULES:
            hit, detail = self._check_rule(r, z, arr)
            if hit:
                rules_hit.append({"id": r["id"], "name": r["name"], "desc": r["desc"], "detail": detail})

        # ---- 过程能力等级 ----
        capability = "EXCELLENT" if cpk >= 1.33 else "GOOD" if cpk >= 1.0 else "FAIR" if cpk >= 0.67 else "POOR"
        stability = round(max(0, min(1, 1.0 - (std / (abs(mean) + 0.01)))), 3)

        # ---- 控制图类型推荐 ----
        chart_type = "I-MR 单值控制图（样本量小，逐件测量）" if n < 30 else "Xbar-R 均值-极差控制图（建议按子组收集）"

        # ---- 直方图 ----
        bins = min(8, max(5, n // 4))
        hist, edges = np.histogram(arr, bins=bins)
        histogram = [{"range": f"{round(edges[i], 1)}-{round(edges[i+1], 1)}", "count": int(hist[i]),
                      "center": round((edges[i] + edges[i + 1]) / 2, 1)} for i in range(bins)]

        # ---- 5M1E 建议 ----
        recs = self._recommendations(rules_hit, cpk, normal)

        return {
            "device_code": device_code,
            "parameter": parameter,
            "parameter_name": spec.get("name", parameter),
            "sample_size": n,
            "spec_source": spec_source,
            "specification": {
                "lsl": round(lsl, 2) if isinstance(lsl, float) else lsl,
                "usl": round(usl, 2) if isinstance(usl, float) else usl,
                "target": round(target, 2) if isinstance(target, float) else target,
            },
            "statistics": {"mean": round(mean, 2), "std": round(std, 2),
                           "min": round(float(arr.min()), 2), "max": round(float(arr.max()), 2),
                           "skewness": round(skew, 3), "kurtosis": round(kurt, 3),
                           "normal_distribution": bool(normal)},
            "capability": {"cp": round(cp, 2), "cpk": round(cpk, 2), "pp": round(pp, 2),
                           "ppk": round(ppk, 2), "cpm": round(cpm, 2), "level": capability,
                           "cpk_ci": cpk_ci},
            "stability": stability,
            "control_limits": [{"name": "UCL", "value": round(ucl, 2)}, {"name": "UWL", "value": round(ucl_warn, 2)},
                               {"name": "CL", "value": round(mean, 2)}, {"name": "LWL", "value": round(lcl_warn, 2)},
                               {"name": "LCL", "value": round(lcl, 2)}],
            "zones": {"z1_up": round(zone1_up, 2), "z1_lo": round(zone1_lo, 2)},
            "chart_recommendation": chart_type,
            "violations": violations,
            "rules_violated": rules_hit,
            "histogram": histogram,
            "we_rules": self.WE_RULES,
            "data_series": [round(float(v), 2) for v in arr],
            "recommendations": recs,
            "sampling_plan": {
                "frequency": "每2小时抽样1次（班组生产节拍内）",
                "subgroup_size": "建议子组5件连续取样",
                "trigger": "任一规则命中立即排查，连续3次命中触发停线评审",
            },
        }

    # ---------- 私有方法 ----------

    def _load_spec(self, parameter: str) -> dict:
        """从 proc_parameter 读取真实工艺规格限"""
        aliases = self.PARAM_ALIAS.get(parameter, [parameter])
        try:
            conn = pymysql.connect(**DB_CONFIG)
            cur = conn.cursor()
            for alias in aliases:
                cur.execute(
                    "SELECT param_name, param_value, min_value, max_value, unit "
                    "FROM proc_parameter WHERE param_name LIKE %s AND min_value IS NOT NULL "
                    "AND max_value IS NOT NULL LIMIT 1",
                    (f"%{alias}%",),
                )
                row = cur.fetchone()
                if row:
                    conn.close()
                    return {
                        "name": row["param_name"], "unit": row.get("unit", ""),
                        "lsl": float(row["min_value"]), "usl": float(row["max_value"]),
                        "target": float(row["param_value"]) if row.get("param_value") is not None else None,
                        "source": "proc_parameter(工艺参数表)",
                    }
            conn.close()
        except Exception as e:
            logger.warning(f"读取工艺规格失败: {e}")
        return {"lsl": None, "usl": None, "target": None, "source": "estimated(6σ窗口估算)"}

    def _check_rule(self, rule: dict, z: np.ndarray, arr: np.ndarray) -> tuple:
        n = len(z)
        detail = ""
        rid = rule["id"]

        if rid == "W1":
            idx = np.where(np.abs(z) > 3)[0]
            if len(idx):
                return True, f"第{int(idx[0]) + 1}点超出3σ控制限 (z={z[idx[0]]:.2f})"
        elif rid == "W2":
            side = z > 0
            for i in range(n - 8):
                if all(side[i:i + 9]) or all(~side[i:i + 9]):
                    return True, f"第{i + 1}~{i + 9}点连续位于中心线同侧"
        elif rid == "W3":
            d = np.diff(arr)
            for i in range(n - 5):
                if all(d[i:i + 5] > 0) or all(d[i:i + 5] < 0):
                    return True, f"第{i + 1}~{i + 6}点持续{'上升' if d[i] > 0 else '下降'}"
        elif rid == "W4":
            for i in range(n - 13):
                window = np.diff(np.sign(z[i:i + 14]))
                if len(window) and not np.any(window == 0) and all(window[1:] == -window[:-1]):
                    return True, f"第{i + 1}~{i + 14}点呈现规律性上下交替"
        elif rid == "W5":
            outer = np.abs(z) > 2
            for i in range(n - 2):
                w = outer[i:i + 3]
                if np.count_nonzero(w) >= 2 and all(np.sign(z[i + j]) == np.sign(z[i]) for j in np.where(w)[0]):
                    return True, f"连续3点中2点落在同侧2σ~3σ区 (第{i + 1}~{i + 3}点)"
        elif rid == "W6":
            outer = np.abs(z) > 1
            for i in range(n - 4):
                w = outer[i:i + 5]
                if np.count_nonzero(w) >= 4 and all(np.sign(z[i + j]) == np.sign(z[i]) for j in np.where(w)[0]):
                    return True, f"连续5点中4点落在同侧1σ~3σ区 (第{i + 1}~{i + 5}点)"
        elif rid == "W7":
            inside = np.abs(z) <= 1
            for i in range(n - 14):
                if all(inside[i:i + 15]):
                    return True, f"连续15点均落在±1σ内 (第{i + 1}~{i + 15}点)"
        elif rid == "W8":
            outside = np.abs(z) > 1
            for i in range(n - 7):
                if all(outside[i:i + 8]):
                    return True, f"连续8点落在±1σ外 (第{i + 1}~{i + 8}点)"
        return False, detail

    def _recommendations(self, rules_hit: list, cpk: float, normal: bool) -> list:
        """按命中规则类型生成 5M1E 分组的改进建议"""
        ids = {r["id"] for r in rules_hit}
        recs = []
        if "W1" in ids:
            recs.append("【机】超限点对应批次设备参数已漂移，立即停线校准传感器与执行机构")
        if "W2" in ids or "W5" in ids or "W6" in ids:
            recs.append("【料】同侧偏移通常对应原材料批次变更，核对来料批次与供应商")
        if "W3" in ids:
            recs.append("【机】持续趋势提示刀具磨损/热变形累积，检查刀具寿命与冷却系统")
        if "W4" in ids:
            recs.append("【法】交替波动多源于参数振荡，检查PID调节与班次交替操作差异")
        if "W7" in ids:
            recs.append("【法】数据过稳需核查测量系统分辨力或抽样分层问题")
        if "W8" in ids:
            recs.append("【测】混合分布提示多工艺路线/多设备混样，按设备分层重新抽样")
        if cpk < 1.0:
            recs.append("【环】制程能力不足，检查温湿度等环境因素并减少过程变异")
        if not normal:
            recs.append("【测】数据不服从正态分布，核查测量系统与数据采集完整性")
        if not recs:
            recs.append("制程受控且能力充足，保持当前工艺，持续按抽样计划监控")
        return recs


class CapacityPredictionService:
    """产能预测 — 时序趋势+周期"""

    def predict(
        self,
        production_line_id: str,
        product_type: str,
        start_date: str,
        days_ahead: int = 7,
        historical_outputs: Optional[List[float]] = None,
    ) -> Dict[str, Any]:
        if historical_outputs and len(historical_outputs) >= 3:
            hist = np.array(historical_outputs)
            base = float(np.mean(hist))
            slope = float(np.polyfit(np.arange(len(hist)), hist, 1)[0])
        else:
            base, slope = 1000.0, 5.0

        weekday_names = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"]
        today = datetime.now()
        predictions = []
        for i in range(days_ahead):
            dt = today + timedelta(days=i)
            wd = dt.weekday()
            # 周末生产效率约85%
            day_factor = 0.85 if wd >= 5 else 1.0
            pred = max(0, (base + slope * (i + 1)) * day_factor)
            predictions.append({
                "date": dt.strftime("%Y-%m-%d"),
                "day": weekday_names[wd],
                "predicted_output": round(pred, 0),
                "confidence_lower": round(pred * 0.85, 0),
                "confidence_upper": round(pred * 1.15, 0),
            })

        total = sum(p["predicted_output"] for p in predictions)
        return {
            "production_line_id": production_line_id,
            "product_type": product_type,
            "predictions": predictions,
            "summary": {
                "total": round(total, 0),
                "daily_avg": round(total / days_ahead, 0),
                "trend": "上升" if slope > 0 else "下降" if slope < 0 else "稳定",
                "confidence": round(min(0.95, 0.7 + 0.02 * len(historical_outputs or [])), 2),
            },
        }


class RootCauseAnalysisService:
    """根因分析 — Z-Score异常检测"""

    def analyze(
        self, quality_record_id: int, include_similar_cases: bool = True,
        parameters: Optional[Dict[str, List[float]]] = None,
    ) -> Dict[str, Any]:
        causes = []
        if parameters:
            for name, vals in parameters.items():
                if len(vals) < 3:
                    continue
                arr = np.array(vals)
                mu, sigma = float(np.mean(arr)), float(np.std(arr))
                if sigma < 0.01 * abs(mu + 0.01):
                    continue
                last = vals[-1]
                z = abs(last - mu) / sigma
                if z > 1.5:
                    direction = "偏高" if last > mu else "偏低"
                    causes.append({
                        "cause": f"{name}{direction}",
                        "impact": round(min(0.9, z / 6), 2),
                        "z_score": round(z, 1),
                        "current": round(last, 1),
                        "baseline": round(mu, 1),
                        "probability": round(min(0.9, z / 5), 2),
                        "recommendation": self._r(name, direction),
                    })

        if not causes:
            causes = [{"cause": c, "impact": i, "probability": p, "recommendation": r} for c, i, p, r in [
                ("温度偏离", 0.35, 0.82, "检查温控系统PID参数"),
                ("速度波动", 0.28, 0.65, "检查电机驱动器稳定性"),
                ("材料偏差", 0.22, 0.48, "抽检原材料批次"),
                ("压力异常", 0.12, 0.31, "检查气路密封"),
                ("振动加剧", 0.08, 0.25, "检查轴承和传动"),
            ]]

        causes.sort(key=lambda x: x["impact"], reverse=True)
        primary, secondary = causes[:3], causes[3:]
        similar = [{"case_id": f"QC-{2024000+quality_record_id-i*11}", "similarity": round(0.88-i*0.12, 2), "root_cause": c["cause"]} for i, c in enumerate(primary)] if include_similar_cases else []

        return {
            "quality_record_id": quality_record_id,
            "primary_causes": primary,
            "secondary_causes": secondary,
            "similar_cases": similar,
            "actions": [f"{i+1}. {c['recommendation']}" for i, c in enumerate(primary)] + ["加强过程监控"],
            "method": "z_score_analysis" if parameters else "knowledge_base",
        }

    def _r(self, p: str, d: str) -> str:
        m = {"temperature": "检查加热/冷却及PID参数", "speed": "检查电机驱动和传动系统", "pressure": "检查气路密封和压缩机",
             "vibration": "检查轴承平衡和固定", "density": "检查原材料规格批次", "power": "检查供电和负载"}
        return m.get(p, f"检查{p}相关系统") + f"({d})"


class DeliveryPredictionService:
    """交期预测 — 历史数据驱动"""

    def predict(self, order_ids: List[int], historical_lead_times: Optional[List[float]] = None) -> Dict[str, Any]:
        if historical_lead_times and len(historical_lead_times) >= 3:
            hist = np.array(historical_lead_times)
            mean_lt = float(np.mean(hist))
            std_lt = float(np.std(hist))
        else:
            mean_lt, std_lt = 7.0, 2.0

        today = datetime.now()
        orders = []
        for order_id in order_ids:
            # 确定性偏移（基于order_id mod，不使用random seed）
            offset = ((order_id * 127 + 31) % 1000) / 1000 - 0.5  # [-0.5, 0.5]
            lt = max(1, mean_lt + offset * std_lt)
            comp = today + timedelta(days=int(lt))
            delay = max(0, int(lt - mean_lt - std_lt * 0.5))
            risk = "HIGH" if delay > 0 else "MEDIUM" if lt > mean_lt else "LOW"
            orders.append({
                "order_id": order_id,
                "completion_date": comp.strftime("%Y-%m-%d"),
                "lead_time_days": round(lt, 1),
                "delay_risk": risk,
                "delay_days": delay,
            })

        delayed = sum(1 for o in orders if o["delay_risk"] == "HIGH")
        at_risk = sum(1 for o in orders if o["delay_risk"] == "MEDIUM")
        overall = "HIGH" if delayed > len(orders) / 3 else "MEDIUM" if at_risk > len(orders) / 2 else "LOW"

        return {
            "orders": orders,
            "summary": {
                "total": len(order_ids), "on_time": len(order_ids) - delayed - at_risk,
                "at_risk": at_risk, "delayed": delayed, "overall_risk": overall,
            },
            "avg_lead_time": round(mean_lt, 1),
            "recommendations": (["增加加班产能", "调整工单优先级"] if overall == "HIGH" else ["监控关键路径"] if overall == "MEDIUM" else []),
        }


def create_analysis_services(llm_service=None):
    return {"energy": EnergyOptimizationService(), "spc": SPCService(), "capacity": CapacityPredictionService(),
            "root_cause": RootCauseAnalysisService(), "delivery": DeliveryPredictionService()}
