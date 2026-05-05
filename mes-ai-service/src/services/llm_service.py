"""大语言模型服务模块"""
import os
import json
import logging
from typing import Dict, List, Optional, Any
from datetime import datetime

logger = logging.getLogger(__name__)

try:
    from zhipuai import ZhipuAI
    ZHIPU_SDK_AVAILABLE = True
except ImportError:
    ZHIPU_SDK_AVAILABLE = False
    logger.warning("zhipuai SDK 未安装，大模型功能不可用")


class LLmService:
    """大语言模型服务，支持智谱AI"""

    SYSTEM_PROMPT = """你是一个智能工厂MES系统的AI助手，专门帮助用户分析生产数据、设备状态和质量信息。

你的职责：
1. 分析设备故障原因并提供维护建议
2. 解释质量预测结果和影响因素
3. 提供工艺参数优化建议
4. 回答关于生产效率OEE的问题
5. 帮助用户理解AI预测结果

请用中文回复，保持专业但易懂。"""

    FREE_MODELS = ["glm-4-flash", "glm-3-flash"]

    def __init__(self, config: dict):
        """初始化大模型服务
        
        Args:
            config: 配置字典
        """
        self.config = config
        self.client = None
        self.model_name = "glm-4-flash"
        self._init_client()

    def _init_client(self):
        """初始化智谱AI客户端"""
        if not ZHIPU_SDK_AVAILABLE:
            logger.warning("智谱AI SDK未安装")
            return
            
        api_key = os.environ.get("ZHIPU_API_KEY") or self.config.get("llm", {}).get("api_key")
        if not api_key:
            logger.warning("未配置智谱AI API Key")
            return
        
        try:
            self.client = ZhipuAI(api_key=api_key)
            self.model_name = self.config.get("llm", {}).get("model", "glm-4-flash")
            if self.model_name not in self.FREE_MODELS:
                self.model_name = "glm-4-flash"
            logger.info(f"智谱AI客户端已初始化，模型: {self.model_name}")
        except Exception as e:
            logger.error(f"智谱AI客户端初始化失败: {e}")
            self.client = None

    def is_available(self) -> bool:
        """检查大模型服务是否可用"""
        return self.client is not None

    def chat(
        self,
        message: str,
        context: Optional[Dict[str, Any]] = None,
        history: Optional[List[Dict[str, str]]] = None,
    ) -> Dict[str, Any]:
        """对话接口
        
        Args:
            message: 用户消息
            context: 上下文数据（设备信息、工单信息等）
            history: 对话历史
            
        Returns:
            回复结果
        """
        if not self.is_available():
            return {
                "success": False,
                "message": "大模型服务暂不可用，请配置智谱AI API Key",
                "content": None,
            }
        
        try:
            messages = [{"role": "system", "content": self.SYSTEM_PROMPT}]
            
            if history:
                messages.extend(history[-5:])
            
            if context:
                context_prompt = self._build_context_prompt(context)
                messages.append({"role": "system", "content": context_prompt})
            
            messages.append({"role": "user", "content": message})
            
            response = self.client.chat.completions.create(
                model=self.model_name,
                messages=messages,
                temperature=0.7,
                max_tokens=1024,
            )
            
            content = response.choices[0].message.content if response.choices else ""
            
            return {
                "success": True,
                "content": content,
                "model": self.model_name,
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens if response.usage else 0,
                    "completion_tokens": response.usage.completion_tokens if response.usage else 0,
                }
            }
            
        except Exception as e:
            logger.error(f"大模型对话失败: {e}")
            return {
                "success": False,
                "message": f"对话失败: {str(e)}",
                "content": None,
            }

    def _build_context_prompt(self, context: Dict[str, Any]) -> str:
        """构建上下文提示"""
        parts = ["当前上下文信息："]
        
        if "device" in context:
            device = context["device"]
            parts.append(f"\n设备信息：")
            parts.append(f"- 设备编码: {device.get('code', 'N/A')}")
            parts.append(f"- 设备名称: {device.get('name', 'N/A')}")
            parts.append(f"- 运行状态: {device.get('status', 'N/A')}")
            parts.append(f"- 温度: {device.get('temperature', 'N/A')}°C")
            parts.append(f"- 速度: {device.get('speed', 'N/A')}")
        
        if "work_order" in context:
            wo = context["work_order"]
            parts.append(f"\n工单信息：")
            parts.append(f"- 工单编号: {wo.get('orderNo', 'N/A')}")
            parts.append(f"- 产品: {wo.get('productName', 'N/A')}")
            parts.append(f"- 计划数量: {wo.get('planQuantity', 'N/A')}")
            parts.append(f"- 完成数量: {wo.get('completedQuantity', 'N/A')}")
        
        if "quality" in context:
            quality = context["quality"]
            parts.append(f"\n质量预测：")
            parts.append(f"- 合格概率: {quality.get('probability', 'N/A')}")
            parts.append(f"- 预测结果: {quality.get('prediction', 'N/A')}")
            parts.append(f"- 置信度: {quality.get('confidence', 'N/A')}")
        
        if "fault" in context:
            fault = context["fault"]
            parts.append(f"\n故障预测：")
            parts.append(f"- 故障概率: {fault.get('fault_probability', 'N/A')}")
            parts.append(f"- 预测结果: {fault.get('prediction', 'N/A')}")
        
        return "\n".join(parts)

    def analyze_device_fault(self, device: Dict, fault_prediction: Dict) -> str:
        """分析设备故障
        
        Args:
            device: 设备信息
            fault_prediction: 故障预测结果
            
        Returns:
            分析结果
        """
        context = {"device": device, "fault": fault_prediction}
        prompt = f"请分析设备 {device.get('code', '')} 的故障风险，并给出维护建议。"
        result = self.chat(prompt, context=context)
        return result.get("content", "大模型服务暂不可用")

    def analyze_quality(self, quality_data: Dict, process_params: Dict) -> str:
        """分析产品质量
        
        Args:
            quality_data: 质量预测数据
            process_params: 工艺参数
            
        Returns:
            分析结果
        """
        context = {"quality": quality_data}
        prompt = f"请分析产品质量预测结果，并给出优化建议。当前工艺参数: {process_params}"
        result = self.chat(prompt, context=context)
        return result.get("content", "大模型服务暂不���用")

    def recommend_process_params(self, product_type: str, target_yield: float) -> str:
        """推荐工艺参数
        
        Args:
            product_type: 产品类型
            target_yield: 目标良率
            
        Returns:
            推荐结果
        """
        prompt = f"请为产品类型 '{product_type}' 推荐最佳工艺参数，目标良率 {target_yield*100}%"
        result = self.chat(prompt)
        return result.get("content", "大模型服务暂不可用")

    def get_model_info(self) -> Dict[str, Any]:
        """获取模型信息"""
        return {
            "available": self.is_available(),
            "model": self.model_name,
            "provider": "ZhipuAI (智谱AI)",
            "capabilities": [
                "设备故障分析",
                "质量预测解读",
                "工艺参数推荐",
                "生产问题解答",
            ] if self.is_available() else [],
        }