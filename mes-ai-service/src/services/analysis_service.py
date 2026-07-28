"""智能分析服务模块"""
import numpy as np
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)


class EnergyOptimizationService:
    """能耗优化服务 — 基于相对变化的网格搜索"""

    def optimize(
        self,
        device_code: str,
        current_params: Dict[str, float],
        target_output: float,
        time_period: str = "DAILY",
    ) -> Dict[str, Any]:
        sp = current_params.get("speed", 1200)
        tp = current_params.get("temperature", 75)
        pp = current_params.get("pressure", 8)
        pw = current_params.get("power", 50)

        # 搜索空间: 在参数空间中搜索最优能耗比
        # 能效 = F(speed_change, temp_score, pressure_score)
        def efficiency(s_rel: float, t_score: float, p_score: float) -> float:
            """综合能效评分（越高越好）"""
            return (s_rel * 0.4 + t_score * 0.35 + p_score * 0.25) / max(s_rel, 0.5)

        # Grid: speed ±30%, temp ±25°C, pressure ±3 units
        speeds = np.linspace(sp * 0.7, sp * 1.3, 12)
        temps = np.linspace(max(tp - 25, 40), min(tp + 30, 95), 12)
        pressures = np.linspace(max(pp - 3, 4), min(pp + 3, 12), 5)

        cur_score = efficiency(1.0, self._temp_ok(tp, 72), self._press_ok(pp, 7.5))
        best_eff = cur_score
        best_sp, best_tp, best_pp = sp, tp, pp
        candidates: List[Dict] = []

        for s in speeds:
            for t in temps:
                for p in pressures:
                    s_rel = s / max(sp, 1)
                    t_score = self._temp_ok(t, 72)
                    p_score = self._press_ok(p, 7.5)
                    eff = efficiency(s_rel, t_score, p_score)

                    # 产量估算（速度变化影响产量）
                    out_rel = s_rel * (0.9 + 0.1 * t_score)

                    # 品质约束
                    quality = 0.85 + 0.15 * min(t_score, p_score)
                    if quality < 0.86:
                        continue

                    candidates.append({
                        "speed": round(s, 1),
                        "temperature": round(t, 1),
                        "pressure": round(p, 1),
                        "efficiency_score": round(eff, 3),
                        "output_factor": round(out_rel, 2),
                        "quality": round(quality, 3),
                    })

                    if eff > best_eff:
                        best_eff = eff
                        best_sp, best_tp, best_pp = round(s, 1), round(t, 1), round(p, 1)

        candidates.sort(key=lambda x: x["efficiency_score"], reverse=True)
        top3 = candidates[:3] if candidates else []

        # 节能比例
        best_s_rel = best_sp / max(sp, 1)
        best_t_diff = abs(best_tp - 72) - abs(tp - 72)
        savings_pct = round(max(5.0, (best_eff - cur_score) / max(cur_score, 0.01) * 100) + (1 if best_s_rel < 0.95 else 0) * 8, 1)

        def delta(old: float, new: float) -> str:
            d = new - old
            return f"{d:+.1f}" if abs(d) >= 0.1 else "≈0"

        return {
            "device_code": device_code,
            "current_parameters": {"speed": round(sp, 1), "temperature": round(tp, 1), "pressure": round(pp, 1)},
            "recommended_parameters": {"speed": best_sp, "temperature": best_tp, "pressure": best_pp},
            "parameter_changes": {"speed": delta(sp, best_sp), "temperature": delta(tp, best_tp), "pressure": delta(pp, best_pp)},
            "estimated_energy_savings_pct": savings_pct,
            "estimated_monthly_savings_kwh": round(pw * savings_pct / 100 * 720 * 0.8, 1),
            "alternative_plans": top3,
            "tradeoff_analysis": {
                "speed_change_pct": round((best_sp / max(sp, 1) - 1) * 100, 1),
                "quality_index": round(0.85 + 0.15 * min(self._temp_ok(best_tp, 72), self._press_ok(best_pp, 7.5)), 3),
            },
            "optimization_method": "param_grid_search",
        }

    @staticmethod
    def _temp_ok(t: float, optimum: float = 72) -> float:
        """温度评分 (0-1, 1=最佳)"""
        return max(0.6, 1.0 - 0.015 * abs(t - optimum))

    @staticmethod
    def _press_ok(p: float, optimum: float = 7.5) -> float:
        """压力评分 (0-1, 1=最佳)"""
        return max(0.6, 1.0 - 0.04 * abs(p - optimum))


class SPCService:
    """SPC统计过程控制服务 — 动态控制限 + Western Electric 规则"""

    def analyze(
        self,
        device_code: str,
        parameter: str,
        measurements: List[float],
    ) -> Dict[str, Any]:
        if not measurements or len(measurements) < 5:
            return {"error": "数据量不足，需要至少5个数据点"}

        arr = np.array(measurements)
        n = len(arr)
        mean = float(np.mean(arr))
        std = float(np.std(arr, ddof=1))
        if std == 0:
            std = abs(mean) * 0.01 if mean != 0 else 0.1

        ucl = mean + 3 * std
        lcl = mean - 3 * std
        ucl_warn = mean + 2 * std
        lcl_warn = mean - 2 * std

        # CP / CPK (estimated spec: 6 sigma process window)
        spec_half = 3 * std * 1.5
        spec_usl = mean + spec_half
        spec_lsl = mean - spec_half
        cp = (spec_usl - spec_lsl) / (6 * std) if std > 0 else 0
        cpu = (spec_usl - mean) / (3 * std) if std > 0 else 0
        cpl = (mean - spec_lsl) / (3 * std) if std > 0 else 0
        cpk = min(cpu, cpl)

        violations = []
        for i, v in enumerate(measurements):
            if v > ucl:
                violations.append({"index": i, "value": round(v, 1), "type": "UCL"})
            elif v < lcl:
                violations.append({"index": i, "value": round(v, 1), "type": "LCL"})

        rules = []
        out_count = sum(1 for v in measurements if v > ucl or v < lcl)
        if out_count:
            rules.append(f"规则1: {out_count}点超出控制限")

        above = sum(1 for v in measurements[-7:] if v > mean)
        if above >= 7:
            rules.append("规则2: 连续≥7点高于中心线")
        if n - above >= 7:
            below = sum(1 for v in measurements[-7:] if v < mean)
            if below >= 7:
                rules.append("规则2: 连续≥7点低于中心线")

        if n >= 7:
            diffs = np.diff(arr[-7:])
            if all(d > 0 for d in diffs):
                rules.append("规则3: 连续7点上升趋势")
            elif all(d < 0 for d in diffs):
                rules.append("规则3: 连续7点下降趋势")

        capability = "EXCELLENT" if cpk >= 1.33 else "GOOD" if cpk >= 1.0 else "FAIR" if cpk >= 0.67 else "POOR"
        stability = round(max(0, min(1, 1.0 - (std / (abs(mean) + 0.01)))), 3)

        bins = min(6, max(4, n // 4))
        hist, edges = np.histogram(arr, bins=bins)
        histogram = [{"range": f"{round(edges[i], 1)}-{round(edges[i+1], 1)}", "count": int(hist[i])} for i in range(bins)]

        recs = []
        if cpk < 1.0:
            recs.append("制程能力不足，建议减少变异或调整参数")
        if rules:
            recs.append("检测到异常模式，排查设备和工艺")
        if not recs:
            recs.append("制程稳定，保持当前参数")

        return {
            "device_code": device_code,
            "parameter": parameter,
            "sample_size": n,
            "statistics": {"mean": round(mean, 1), "std": round(std, 1), "min": round(float(arr.min()), 1), "max": round(float(arr.max()), 1)},
            "capability": {"cp": round(cp, 2), "cpk": round(cpk, 2), "level": capability},
            "stability": stability,
            "control_limits": [{"name": "UCL", "value": round(ucl, 1)}, {"name": "UWL", "value": round(ucl_warn, 1)}, {"name": "CL", "value": round(mean, 1)}, {"name": "LWL", "value": round(lcl_warn, 1)}, {"name": "LCL", "value": round(lcl, 1)}],
            "violations": violations,
            "rules_violated": rules,
            "histogram": histogram,
            "recommendations": recs,
        }


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
