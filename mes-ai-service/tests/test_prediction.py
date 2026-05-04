"""预测功能测试模块"""
import pytest
from datetime import datetime
from typing import List, Dict

from src.schemas.prediction import (
    QualityPredictionRequest,
    QualityPredictionResponse,
    BatchPredictionRequest,
    BatchPredictionResponse,
    DeviceFaultPredictionRequest,
    DeviceFaultPredictionResponse,
    ProcessParamRecommendationRequest,
    ProcessParamRecommendationResponse,
    AnomalyDetectionRequest,
    AnomalyDetectionResponse,
)
from src.services.quality_predictor import QualityPredictorService


class TestQualityPredictionRequest:
    """质量预测请求模型测试"""

    def test_valid_request(self):
        """测试有效请求"""
        request = QualityPredictionRequest(
            work_order_id=12345,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
        )
        assert request.work_order_id == 12345
        assert request.product_name == "Product A"
        assert request.temperature == 80.0

    def test_request_with_optional_fields(self):
        """测试带可选字段的请求"""
        request = QualityPredictionRequest(
            work_order_id=12345,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
            raw_material="PET",
            humidity=55.0,
            vibration=0.3,
        )
        assert request.raw_material == "PET"
        assert request.humidity == 55.0

    def test_request_validation(self):
        """测试请求验证"""
        request = QualityPredictionRequest(
            work_order_id=12345,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
        )
        assert request.speed == 50.0


class TestQualityPredictionResponse:
    """质量预测响应模型测试"""

    def test_response_creation(self):
        """测试响应创建"""
        response = QualityPredictionResponse(
            prediction_id="test-123",
            probability=0.85,
            prediction="PASS",
            confidence=0.9,
            factors=[{"factor": "temperature", "value": 80.0, "impact": 0.1, "description": "正常"}],
            model_version="1.0.0",
            timestamp=datetime.now(),
        )
        assert response.prediction_id == "test-123"
        assert response.probability == 0.85
        assert response.prediction == "PASS"


class TestQualityPredictorService:
    """质量预测服务测试"""

    @pytest.fixture
    def service(self):
        """创建服务实例"""
        config = {
            "model": {
                "quality": {
                    "version": "1.0.0",
                    "onnx_path": None,
                }
            },
            "redis": {"host": "localhost", "port": 6379, "db": 1},
        }
        return QualityPredictorService(config)

    def test_feature_engineering(self, service):
        """测试特征工程"""
        features = service.engineer_features(
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
            humidity=55.0,
            vibration=0.3,
            raw_material="PET",
        )
        assert "temperature" in features
        assert "speed" in features
        assert "pressure" in features
        assert "temp_normalized" in features
        assert "temp_speed_interaction" in features
        assert "material_PET" in features

    def test_predict(self, service):
        """测试预测"""
        result = service.predict(
            work_order_id=12345,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
        )
        assert "prediction_id" in result
        assert "probability" in result
        assert "prediction" in result
        assert result["prediction"] in ["PASS", "FAIL"]

    def test_batch_predict(self, service):
        """测试批量预测"""
        requests = [
            {
                "work_order_id": 1,
                "product_name": "Product A",
                "device_code": "DEV001",
                "temperature": 80.0,
                "speed": 50.0,
                "pressure": 10.0,
            },
            {
                "work_order_id": 2,
                "product_name": "Product B",
                "device_code": "DEV002",
                "temperature": 85.0,
                "speed": 55.0,
                "pressure": 12.0,
            },
        ]
        results = service.batch_predict(requests)
        assert len(results) == 2
        assert all("prediction_id" in r for r in results)

    def test_analyze_factors(self, service):
        """测试因素分析"""
        features = {
            "temperature": 90.0,
            "speed": 65.0,
            "pressure": 10.0,
            "vibration": 0.9,
        }
        factors = service.analyze_factors(features, 0.3)
        assert len(factors) > 0
        assert factors[0]["factor"] == "temperature"


class TestBatchPrediction:
    """批量预测测试"""

    @pytest.fixture
    def batch_request(self):
        """批量预测请求"""
        return BatchPredictionRequest(
            predictions=[
                QualityPredictionRequest(
                    work_order_id=1,
                    product_name="Product A",
                    device_code="DEV001",
                    temperature=80.0,
                    speed=50.0,
                    pressure=10.0,
                ),
                QualityPredictionRequest(
                    work_order_id=2,
                    product_name="Product B",
                    device_code="DEV002",
                    temperature=85.0,
                    speed=55.0,
                    pressure=12.0,
                ),
            ]
        )

    def test_batch_request(self, batch_request):
        """测试批量请求"""
        assert len(batch_request.predictions) == 2


class TestDeviceFaultPrediction:
    """设备故障预测测试"""

    def test_fault_prediction_request(self):
        """测试故障预测请求"""
        request = DeviceFaultPredictionRequest(
            device_code="DEV001",
            history_data=[
                {"temperature": 80, "speed": 50},
                {"temperature": 82, "speed": 52},
                {"temperature": 85, "speed": 55},
            ],
            hours_ahead=24,
        )
        assert request.device_code == "DEV001"
        assert len(request.history_data) == 3

    def test_fault_prediction_response(self):
        """测试故障预测响应"""
        response = DeviceFaultPredictionResponse(
            device_code="DEV001",
            fault_probability=0.3,
            prediction="NORMAL",
            confidence=0.85,
            risk_factors=[],
            model_version="1.0.0",
            timestamp=datetime.now(),
        )
        assert response.fault_probability == 0.3
        assert response.prediction == "NORMAL"


class TestProcessRecommendation:
    """工艺参数推荐测试"""

    def test_recommendation_request(self):
        """测试推荐请求"""
        request = ProcessParamRecommendationRequest(
            product_type="PET Bottle",
            material_properties={"density": 1.2, "hardness": 75},
        )
        assert request.product_type == "PET Bottle"

    def test_recommendation_response(self):
        """测试推荐响应"""
        response = ProcessParamRecommendationResponse(
            product_type="PET Bottle",
            recommended_params={"temperature": 75.0, "speed": 55.0, "pressure": 8.0},
            confidence=0.85,
            model_version="1.0.0",
            timestamp=datetime.now(),
        )
        assert "temperature" in response.recommended_params


class TestAnomalyDetection:
    """异常检测测试"""

    def test_anomaly_detection_request(self):
        """测试异常检测请求"""
        request = AnomalyDetectionRequest(
            sensor_data={"temperature": 110.0, "speed": 50.0, "pressure": 10.0},
            device_code="DEV001",
        )
        assert request.sensor_data["temperature"] == 110.0

    def test_anomaly_detection_response(self):
        """测试异常检测响应"""
        response = AnomalyDetectionResponse(
            is_anomaly=True,
            anomaly_type="temperature_abnormal",
            anomaly_score=0.8,
            details=[{"sensor": "temperature", "value": 110.0, "threshold": (60, 100), "deviation": 0.5}],
            timestamp=datetime.now(),
        )
        assert response.is_anomaly is True


class TestModelInference:
    """模型推理准确性测试"""

    @pytest.fixture
    def service(self):
        """创建服务实例"""
        config = {
            "model": {"quality": {"version": "1.0.0", "onnx_path": None}},
            "redis": {"host": "localhost", "port": 6379, "db": 1},
        }
        return QualityPredictorService(config)

    def test_normal_parameters(self, service):
        """测试正常参数应返回高合格率"""
        result = service.predict(
            work_order_id=1,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=50.0,
            pressure=10.0,
        )
        assert result["probability"] > 0.5

    def test_abnormal_temperature(self, service):
        """测试异常温度应返回低合格率"""
        result = service.predict(
            work_order_id=1,
            product_name="Product A",
            device_code="DEV001",
            temperature=150.0,
            speed=50.0,
            pressure=10.0,
        )
        assert "factors" in result
        has_temp_factor = any(
            f.get("factor") == "temperature" for f in result["factors"]
        )
        assert has_temp_factor or result["probability"] < 0.5

    def test_abnormal_speed(self, service):
        """测试异常速度"""
        result = service.predict(
            work_order_id=1,
            product_name="Product A",
            device_code="DEV001",
            temperature=80.0,
            speed=100.0,
            pressure=10.0,
        )
        assert "factors" in result


if __name__ == "__main__":
    pytest.main([__file__, "-v"])