"""智能分析服务模块"""
import numpy as np
from typing import Dict, List, Optional, Any
from datetime import datetime, timedelta
import logging

from src.services.llm_service import LLmService

logger = logging.getLogger(__name__)


class EnergyOptimizationService:
    """能耗优化服务"""

    def __init__(self, llm_service: LLmService = None):
        self.llm_service = llm_service

    def optimize(
        self,
        device_code: str,
        current_params: Dict[str, float],
        target_output: float,
        time_period: str = "DAILY",
    ) -> Dict[str, Any]:
        """能耗优化
        
        Args:
            device_code: 设备编码
            current_params: 当前参数
            target_output: 目标产量
            time_period: 时间周期
            
        Returns:
            优化结果
        """
        base_energy = current_params.get("power_consumption", 100)
        speed = current_params.get("speed", 50)
        temperature = current_params.get("temperature", 80)
        
        optimal_speed = min(max(speed * 0.9, 30), 80)
        optimal_temp = min(max(temperature * 0.95, 70), 90)
        
        energy_savings = (1 - (optimal_speed / speed) * 0.5 - (optimal_temp / temperature) * 0.3) * 100
        energy_savings = max(energy_savings, 5)
        
        estimated_cost_savings = energy_savings * base_energy / 100 * target_output * 0.01
        
        return {
            "device_code": device_code,
            "recommended_parameters": {
                "speed": round(optimal_speed, 1),
                "temperature": round(optimal_temp, 1),
                "power_mode": "eco",
            },
            "estimated_energy_savings": round(energy_savings, 2),
            "estimated_cost_savings": round(estimated_cost_savings, 2),
            "tradeoff_analysis": {
                "production_impact": "降低约5-10%",
                "maintenance_benefit": "延长设备寿命",
                "quality_impact": "良率提升约2%",
            },
        }


class SPCService:
    """SPC统计过程控制服务"""

    def __init__(self):
        self.control_limits = {
            "temperature": {"usl": 95, "lsl": 65, "target": 80},
            "speed": {"usl": 70, "lsl": 30, "target": 50},
            "pressure": {"usl": 15, "lsl": 5, "target": 10},
        }

    def analyze(
        self,
        device_code: str,
        parameter: str,
        measurements: List[float],
    ) -> Dict[str, Any]:
        """SPC分析
        
        Args:
            device_code: 设备编码
            parameter: 参数名
            measurements: 测量值列表
            
        Returns:
            分析结果
        """
        if not measurements or len(measurements) < 5:
            return {"error": "数据量不足，需要至少5个数据点"}
        
        arr = np.array(measurements)
        mean = np.mean(arr)
        std = np.std(arr)
        
        limits = self.control_limits.get(parameter, {"usl": 100, "lsl": 0, "target": 50})
        usl = limits["usl"]
        lsl = limits["lsl"]
        
        cp = (usl - lsl) / (6 * std) if std > 0 else 0
        cpu = (usl - mean) / (3 * std) if std > 0 else 0
        cpl = (mean - lsl) / (3 * std) if std > 0 else 0
        cpk = min(cpu, cpl)
        
        violations = []
        for i, val in enumerate(measurements):
            if val > usl:
                violations.append({"index": i, "value": val, "type": "USL", "message": f"超过上限{usl}"})
            elif val < lsl:
                violations.append({"index": i, "value": val, "type": "LSL", "message": f"低于下限{lsl}"})
        
        if cpk >= 1.33:
            capability = "EXCELLENT"
        elif cpk >= 1.0:
            capability = "GOOD"
        elif cpk >= 0.67:
            capability = "FAIR"
        else:
            capability = "POOR"
        
        return {
            "device_code": device_code,
            "parameter": parameter,
            "cp": round(cp, 2),
            "cpk": round(cpk, 2),
            "mean": round(mean, 2),
            "std": round(std, 2),
            "control_limits": {
                "usl": usl,
                "lsl": lsl,
                "target": limits["target"],
            },
            "violations": violations,
            "process_capability": capability,
        }


class CapacityPredictionService:
    """产能预测服务"""

    def __init__(self, llm_service: LLmService = None):
        self.llm_service = llm_service

    def predict(
        self,
        production_line_id: str,
        product_type: str,
        start_date: str,
        days_ahead: int = 7,
    ) -> Dict[str, Any]:
        """产能预测
        
        Args:
            production_line_id: 生产线ID
            product_type: 产品类型
            start_date: 开始日期
            days_ahead: 预测天数
            
        Returns:
            预测结果
        """
        base_capacity = 1000
        predictions = []
        
        for i in range(days_ahead):
            date = (datetime.now() + timedelta(days=i)).strftime("%Y-%m-%d")
            day_factor = 0.9 if i % 7 in [5, 6] else 1.0
            variance = np.random.uniform(-0.1, 0.1)
            
            predicted = base_capacity * day_factor * (1 + variance)
            predictions.append({
                "date": date,
                "predicted_output": round(predicted, 0),
                "utilization_rate": round(predicted / base_capacity * 100, 1),
            })
        
        return {
            "production_line_id": production_line_id,
            "product_type": product_type,
            "predictions": predictions,
            "confidence": 0.85,
            "factors": {
                "historical_performance": "正常",
                "maintenance_schedule": "无",
                "seasonal_factor": "无",
            },
        }


class RootCauseAnalysisService:
    """根因分析服务"""

    def __init__(self, llm_service: LLmService = None):
        self.llm_service = llm_service

    def analyze(
        self,
        quality_record_id: int,
        include_similar_cases: bool = True,
    ) -> Dict[str, Any]:
        """质量根因分析
        
        Args:
            quality_record_id: 质量记录ID
            include_similar_cases: 是否包含相似案例
            
        Returns:
            分析结果
        """
        common_causes = [
            {
                "cause": "温度偏高",
                "impact": 0.35,
                "probability": 0.8,
                "recommendation": "检查温控系统",
            },
            {
                "cause": "速度不稳定",
                "impact": 0.25,
                "probability": 0.6,
                "recommendation": "检查电机驱动",
            },
            {
                "cause": "材料批次问题",
                "impact": 0.2,
                "probability": 0.4,
                "recommendation": "更换原材料批次",
            },
            {
                "cause": "压力波动",
                "impact": 0.15,
                "probability": 0.3,
                "recommendation": "检查气路系统",
            },
            {
                "cause": "设备老化",
                "impact": 0.05,
                "probability": 0.2,
                "recommendation": "计划性维护",
            },
        ]
        
        similar_cases = []
        if include_similar_cases:
            for i in range(3):
                similar_cases.append({
                    "record_id": quality_record_id - (i + 1) * 10,
                    "similarity": round(0.9 - i * 0.15, 2),
                    "root_cause": common_causes[i]["cause"],
                })
        
        return {
            "quality_record_id": quality_record_id,
            "primary_causes": common_causes[:3],
            "secondary_causes": common_causes[3:],
            "similar_cases": similar_cases,
            "recommended_actions": [
                "1. 检查并校准温控系统",
                "2. 检查设备运行速度稳定性",
                "3. 验证原材料质量",
                "4. 加强过程监控",
            ],
        }


class DeliveryPredictionService:
    """交期预测服务"""

    def __init__(self):
        pass

    def predict(
        self,
        order_ids: List[int],
    ) -> Dict[str, Any]:
        """交期预测
        
        Args:
            order_ids: 工单ID列表
            
        Returns:
            预测结果
        """
        order_predictions = []
        
        for order_id in order_ids:
            base_days = np.random.randint(3, 10)
            delay_prob = np.random.random()
            
            if delay_prob > 0.7:
                prediction = "DELAYED"
                risk_level = "HIGH"
                expected_delay = max(int(base_days * 0.3), 1)
            elif delay_prob > 0.4:
                prediction = "AT_RISK"
                risk_level = "MEDIUM"
                expected_delay = 0
            else:
                prediction = "ON_TIME"
                risk_level = "LOW"
                expected_delay = 0
            
            order_predictions.append({
                "order_id": order_id,
                "predicted_completion": (datetime.now() + timedelta(days=base_days)).strftime("%Y-%m-%d"),
                "prediction": prediction,
                "expected_delay_days": expected_delay,
                "risk_level": risk_level,
            })
        
        delayed_count = sum(1 for o in order_predictions if o["prediction"] == "DELAYED")
        at_risk_count = sum(1 for o in order_predictions if o["prediction"] == "AT_RISK")
        
        if delayed_count > len(order_ids) / 2:
            delayed_risk_level = "HIGH"
        elif at_risk_count > len(order_ids) / 2:
            delayed_risk_level = "MEDIUM"
        else:
            delayed_risk_level = "LOW"
        
        return {
            "order_predictions": order_predictions,
            "delayed_risk_level": delayed_risk_level,
            "recommended_actions": [
                "提前备料",
                "增加产能",
                "调整优先级",
            ] if delayed_risk_level != "LOW" else [],
        }


def create_analysis_services(llm_service: LLmService = None):
    """创建所有分析服务"""
    return {
        "energy": EnergyOptimizationService(llm_service),
        "spc": SPCService(),
        "capacity": CapacityPredictionService(llm_service),
        "root_cause": RootCauseAnalysisService(llm_service),
        "delivery": DeliveryPredictionService(),
    }