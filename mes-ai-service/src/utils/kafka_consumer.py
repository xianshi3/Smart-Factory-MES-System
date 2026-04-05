"""Kafka 消费者模块"""
import json
import threading
import logging
import redis
from typing import Callable, Optional, Any
from confluent_kafka import Consumer, KafkaError, KafkaException

logger = logging.getLogger(__name__)


class KafkaDeviceDataConsumer:
    """Kafka 设备数据消费者，负责从 Kafka 主题消费设备数据并缓存到 Redis"""
    def __init__(
        self,
        config: dict,
        message_handler: Optional[Callable[[dict], None]] = None,
    ):
        """初始化 Kafka 消费者
        
        Args:
            config: 配置字典
            message_handler: 消息处理回调函数
        """
        kafka_cfg = config.get("kafka", {})
        redis_cfg = config.get("redis", {})

        self.consumer_config = {
            "bootstrap.servers": kafka_cfg.get("bootstrap_servers", "localhost:9092"),
            "group.id": kafka_cfg.get("group_id", "ai-service-group"),
            "auto.offset.reset": "latest",
            "enable.auto.commit": True,
        }

        self.topics = kafka_cfg.get("topics", ["mes-device-data"])
        self.message_handler = message_handler
        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._consumer: Optional[Consumer] = None

        try:
            self.redis_client = redis.Redis(
                host=redis_cfg.get("host", "localhost"),
                port=redis_cfg.get("port", 6379),
                db=redis_cfg.get("db", 1),
                decode_responses=True,
            )
            self.redis_client.ping()
        except Exception as e:
            logger.warning(f"Redis connection failed: {e}")
            self.redis_client = None

    def start(self):
        """启动消费者线程"""
        if self._running:
            logger.warning("Consumer already running")
            return
        self._running = True
        self._thread = threading.Thread(target=self._consume_loop, daemon=True)
        self._thread.start()
        logger.info(f"Kafka consumer started, topics: {self.topics}")

    def stop(self):
        """停止消费者"""
        self._running = False
        if self._consumer:
            self._consumer.close()
        if self._thread:
            self._thread.join(timeout=5)
        logger.info("Kafka consumer stopped")

    def _consume_loop(self):
        """消费循环"""
        try:
            self._consumer = Consumer(self.consumer_config)
            self._consumer.subscribe(self.topics)

            while self._running:
                msg = self._consumer.poll(timeout=1.0)
                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    else:
                        logger.error(f"Kafka error: {msg.error()}")
                        break

                try:
                    data = json.loads(msg.value().decode("utf-8"))
                    self._process_message(data)
                    if self.message_handler:
                        self.message_handler(data)
                except json.JSONDecodeError as e:
                    logger.warning(f"Failed to parse message: {e}")
                except Exception as e:
                    logger.error(f"Error processing message: {e}")

        except KafkaException as e:
            logger.error(f"Kafka exception: {e}")
        finally:
            if self._consumer:
                self._consumer.close()

    def _process_message(self, data: dict):
        """处理消息并存储到 Redis"""
        device_id = data.get("device_id", "unknown")
        redis_key = f"device_data:{device_id}"

        try:
            if self.redis_client:
                pipeline = self.redis_client.pipeline(transaction=False)
                pipeline.lpush(redis_key, json.dumps(data))
                pipeline.ltrim(redis_key, 0, 999)
                pipeline.expire(redis_key, 3600)
                pipeline.execute()
        except Exception as e:
            logger.error(f"Redis write error: {e}")

    def get_device_history(self, device_id: str, limit: int = 100) -> list:
        """获取设备历史数据
        
        Args:
            device_id: 设备ID
            limit: 返回数据条数限制
            
        Returns:
            历史数据列表
        """
        if not self.redis_client:
            return []
        try:
            redis_key = f"device_data:{device_id}"
            raw_data = self.redis_client.lrange(redis_key, 0, limit - 1)
            return [json.loads(item) for item in raw_data if item]
        except Exception as e:
            logger.error(f"Redis read error: {e}")
            return []

    @property
    def is_running(self) -> bool:
        return self._running
