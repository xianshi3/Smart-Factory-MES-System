"""大语言模型服务模块"""
import os
import json
import hashlib
import logging
from typing import Dict, List, Optional, Any
from datetime import datetime

from src.services.redis_store import redis_store

logger = logging.getLogger(__name__)

try:
    from zhipuai import ZhipuAI
    import httpx
    ZHIPU_SDK_AVAILABLE = True
except ImportError:
    ZHIPU_SDK_AVAILABLE = False
    logger.warning("zhipuai SDK 未安装，大模型功能不可用")


def _load_api_key() -> Optional[str]:
    key = os.environ.get("ZHIPU_API_KEY")
    if key:
        return key
    env_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env.local")
    if os.path.exists(env_path):
        with open(env_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line.startswith("ZHIPU_API_KEY="):
                    return line.split("=", 1)[1].strip().strip("\"'")
    return None


def _looks_valid_api_key(key: Optional[str]) -> bool:
    """智谱 key 格式为 `id.secret`；占位符/空值/明显无效的值视为未配置"""
    if not key or not isinstance(key, str):
        return False
    k = key.strip()
    if k.startswith("your-") or k.startswith("${") or k in ("", "null", "None"):
        return False
    return "." in k and len(k) >= 20


class LLmService:
    """大语言模型服务，支持智谱AI"""

    SYSTEM_PROMPT = """你是一个虚拟路径MES系统的AI助手，专门帮助用户分析生产数据、设备状态和质量信息。

你的职责：
1. 分析设备故障原因并提供维护建议
2. 解释质量预测结果和影响因素
3. 提供工艺参数优化建议
4. 回答关于生产效率OEE的问题
5. 帮助用户理解AI预测结果

请用中文回复，保持专业但易懂。"""

    FREE_MODELS = ["glm-4-flash", "glm-3-flash"]

    def __init__(self, config: dict):
        self.config = config
        self.client = None
        self.model_name = "glm-4-flash"
        self._init_client()

    def _init_client(self):
        if not ZHIPU_SDK_AVAILABLE:
            logger.warning("智谱AI SDK未安装")
            return

        api_key = _load_api_key()
        if not _looks_valid_api_key(api_key):
            logger.warning("未配置有效的智谱AI API Key（占位符/空值视为未配置）")
            return

        try:
            # trust_env=False: 忽略系统/环境代理（如本地 git 代理 127.0.0.1:54689），避免初始化卡住
            http_client = httpx.Client(trust_env=False, timeout=60.0)
            self.client = ZhipuAI(api_key=api_key, http_client=http_client)
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

        # ---- 结果缓存：相同请求(消息+上下文+历史)直接复用，降本 ----
        cache_key = self._cache_key(message, context, history)
        cached = redis_store.get_json(cache_key)
        if cached:
            return cached

        # ---- 速率限制：滑动窗口 INCR + EXPIRE，防突刺/防刷 ----
        rate_limit = int(self.config.get("llm", {}).get("rate_limit", 60))
        if redis_store.enabled:
            count = redis_store.incr("ratelimit:llm", ttl=60)
            if count > rate_limit:
                logger.warning(f"LLM 调用触发限流 ({count}/{rate_limit} per 60s)")
                return {
                    "success": False,
                    "message": f"AI 服务繁忙，请稍后再试（每分钟限 {rate_limit} 次）",
                    "content": None,
                    "limited": True,
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
            
            result = {
                "success": True,
                "content": content,
                "model": self.model_name,
                "usage": {
                    "prompt_tokens": response.usage.prompt_tokens if response.usage else 0,
                    "completion_tokens": response.usage.completion_tokens if response.usage else 0,
                }
            }
            # 缓存成功结果 1 小时
            redis_store.set_json(cache_key, 3600, result)
            return result
            
        except Exception as e:
            logger.error(f"大模型对话失败: {e}")
            return {
                "success": False,
                "message": f"对话失败: {str(e)}",
                "content": None,
            }

    def _cache_key(self, message: str, context: Optional[dict], history: Optional[list]) -> str:
        """LLM 结果缓存 key — 内容寻址(完整请求), 不会串上下文"""
        payload = {"m": message, "c": context or {}, "h": (history or [])[-5:], "model": self.model_name}
        digest = hashlib.sha256(json.dumps(payload, ensure_ascii=False, sort_keys=True, default=str).encode("utf-8")).hexdigest()[:32]
        return f"llm:cache:{digest}"

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

        if "alarm" in context:
            alarm = context["alarm"]
            parts.append(f"\n告警信息：")
            parts.append(f"- 告警编码: {alarm.get('code', 'N/A')}")
            parts.append(f"- 告警内容: {alarm.get('message', 'N/A')}")
            parts.append(f"- 级别: {alarm.get('level', 'N/A')}")
            parts.append(f"- 设备: {alarm.get('device', 'N/A')}")
            parts.append(f"- 状态: {alarm.get('status', 'N/A')}")
            parts.append(f"- 时间: {alarm.get('time', 'N/A')}")

        # 通用兜底：未识别的上下文键（页面上下文/自定义数据）直接注入，避免信息丢失
        known_keys = {"device", "work_order", "quality", "fault", "alarm"}
        for key, val in context.items():
            if key in known_keys:
                continue
            parts.append(f"\n{key}:")
            parts.append(json.dumps(val, ensure_ascii=False, default=str)[:2000])

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