"""Redis 统一辅助模块 — 连接管理 + 优雅降级

设计原则（保证改动不出问题）:
- Redis 不可用时所有方法静默返回 None/False/空，调用方走 MySQL 原路径
- 连接超时 1 秒，绝不让缓存拖慢业务
- 所有 key 带服务前缀，统一规范
"""
import json
import logging
import os
from typing import Any, Optional, List, Dict

import yaml

logger = logging.getLogger(__name__)

try:
    import redis as _redis
    REDIS_AVAILABLE = True
except ImportError:
    REDIS_AVAILABLE = False
    logger.warning("redis-py 未安装，Redis 缓存功能停用")

_config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(_config_path, "r", encoding="utf-8") as f:
        _yaml = yaml.safe_load(f) or {}
except Exception:
    _yaml = {}

_rc = _yaml.get("redis", {})


class RedisStore:
    """Redis 客户端封装 — 所有操作 try/except，失败即降级"""

    def __init__(self):
        self.client = None
        self.enabled = False
        self._init()

    def _init(self):
        if not REDIS_AVAILABLE:
            return
        try:
            self.client = _redis.Redis(
                host=os.getenv("REDIS_HOST", _rc.get("host", "localhost")),
                port=int(os.getenv("REDIS_PORT", _rc.get("port", 6379))),
                db=int(_rc.get("db", 0)),
                password=_rc.get("password") or None,
                socket_connect_timeout=1,
                socket_timeout=1,
                decode_responses=True,
            )
            self.client.ping()
            self.enabled = True
            logger.info("Redis 已连接 (localhost:6379)，缓存层生效")
        except Exception as e:
            self.client = None
            logger.warning(f"Redis 连接失败，缓存降级为直连 MySQL: {e}")

    # ---------- 基础操作 ----------

    def get(self, key: str) -> Optional[str]:
        try:
            return self.client.get(key) if self.client else None
        except Exception:
            return None

    def setex(self, key: str, ttl: int, value: str) -> bool:
        try:
            return bool(self.client.setex(key, ttl, value)) if self.client else False
        except Exception:
            return False

    def delete(self, *keys: str) -> int:
        try:
            return int(self.client.delete(*keys)) if self.client else 0
        except Exception:
            return 0

    def setnx(self, key: str, value: str, ttl: Optional[int] = None) -> bool:
        """SETNX 分布式锁 — 成功返回 True"""
        try:
            if not self.client:
                return False
            ok = self.client.set(key, value, nx=True)
            if ok and ttl:
                self.client.expire(key, ttl)
            return bool(ok)
        except Exception:
            return False

    def incr(self, key: str, ttl: Optional[int] = None) -> int:
        try:
            if not self.client:
                return 0
            v = int(self.client.incr(key))
            if v == 1 and ttl:
                self.client.expire(key, ttl)
            return v
        except Exception:
            return 0

    def expire(self, key: str, ttl: int) -> bool:
        try:
            return bool(self.client.expire(key, ttl)) if self.client else False
        except Exception:
            return False

    def publish(self, channel: str, message: str) -> int:
        try:
            return int(self.client.publish(channel, message)) if self.client else 0
        except Exception:
            return 0

    # ---------- ZSet（有序集合，用于"最近 N 条"场景） ----------

    def zadd(self, key: str, mapping: Dict[str, float]) -> int:
        try:
            return int(self.client.zadd(key, mapping)) if self.client else 0
        except Exception:
            return 0

    def zrevrange(self, key: str, start: int = 0, end: int = -1) -> List[str]:
        try:
            return list(self.client.zrevrange(key, start, end)) if self.client else []
        except Exception:
            return []

    def zrem(self, key: str, *members: str) -> int:
        try:
            return int(self.client.zrem(key, *members)) if self.client else 0
        except Exception:
            return 0

    def zremrangebyrank(self, key: str, start: int, end: int) -> int:
        try:
            return int(self.client.zremrangebyrank(key, start, end)) if self.client else 0
        except Exception:
            return 0

    def zcard(self, key: str) -> int:
        try:
            return int(self.client.zcard(key)) if self.client else 0
        except Exception:
            return 0

    # ---------- JSON 便捷 ----------

    def get_json(self, key: str) -> Optional[Any]:
        raw = self.get(key)
        if raw is None:
            return None
        try:
            return json.loads(raw)
        except Exception:
            return None

    def set_json(self, key: str, ttl: int, value: Any) -> bool:
        return self.setex(key, ttl, json.dumps(value, ensure_ascii=False, default=str))


redis_store = RedisStore()
