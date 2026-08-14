"""JWT 鉴权（HS256，标准库实现，无第三方依赖）。

与 Java 后端 JwtUtils（jjwt）签名算法一致：
  base64url(header).base64url(payload).base64url(hmac_sha256(header.payload, JWT_SECRET))
"""
import base64
import hashlib
import hmac
import json
import logging
import os
import time
from contextvars import ContextVar

from fastapi import HTTPException, Request

logger = logging.getLogger(__name__)

_current_token: ContextVar[str] = ContextVar("mes_user_token", default="")


def get_token() -> str:
    """返回当前请求线程的 JWT（供内部工具调用后端 API 时透传）"""
    return _current_token.get()


def set_token(token: str) -> None:
    _current_token.set(token)


def _b64url_decode(s: str) -> bytes:
    padding = "=" * (-len(s) % 4)
    return base64.urlsafe_b64decode(s + padding)


def decode_jwt(token: str) -> dict:
    secret = os.environ.get("JWT_SECRET", "")
    if not secret:
        raise HTTPException(status_code=500, detail="JWT_SECRET 未配置")
    parts = token.split(".")
    if len(parts) != 3:
        raise HTTPException(status_code=401, detail="Token 无效")
    try:
        header = json.loads(_b64url_decode(parts[0]))
        payload = json.loads(_b64url_decode(parts[1]))
    except Exception:
        raise HTTPException(status_code=401, detail="Token 内容无效")
    # 与 Java 端 jjwt 对齐：密钥长度决定 HS256/384/512（如 48 字节密钥 → HS384）
    hash_mod = {"HS256": hashlib.sha256, "HS384": hashlib.sha384, "HS512": hashlib.sha512}.get(header.get("alg"))
    if hash_mod is None:
        raise HTTPException(status_code=401, detail=f"不支持的签名算法: {header.get('alg')}")
    header_payload = f"{parts[0]}.{parts[1]}".encode()
    expected = base64.urlsafe_b64encode(
        hmac.new(secret.encode(), header_payload, hash_mod).digest()
    ).rstrip(b"=").decode()
    if not hmac.compare_digest(expected, parts[2]):
        raise HTTPException(status_code=401, detail="Token 签名无效")
    exp = payload.get("exp")
    if isinstance(exp, (int, float)) and exp < time.time():
        raise HTTPException(status_code=401, detail="登录已过期")
    return payload


def verify_token(request: Request) -> dict:
    """FastAPI 依赖：校验 Authorization: Bearer <JWT>，并把 token 存入请求上下文"""
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未登录或Token缺失")
    token = auth[len("Bearer "):].strip()
    payload = decode_jwt(token)
    set_token(token)
    return payload
