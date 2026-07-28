"""对话历史 MySQL 存储 — 企业级持久化方案"""
from __future__ import annotations
import pymysql
import pymysql.cursors
import json
import os
import uuid
import asyncio
from datetime import datetime
from typing import Optional, Any
import yaml

_config_path = os.path.join(os.path.dirname(__file__), "..", "..", "config.yaml")
try:
    with open(_config_path, "r", encoding="utf-8") as f:
        _yaml_config = yaml.safe_load(f) or {}
except Exception:
    _yaml_config = {}

_db_config = _yaml_config.get("database", {})

DB_CONFIG = {
    "host": os.getenv("MYSQL_HOST", _db_config.get("host", "localhost")),
    "port": int(os.getenv("MYSQL_PORT", _db_config.get("port", 3306))),
    "user": os.getenv("MYSQL_USERNAME", _db_config.get("username", "root")),
    "password": os.getenv("MYSQL_PASSWORD", _db_config.get("password", "root")),
    "database": os.getenv("MYSQL_DATABASE", _db_config.get("database", "mes_db")),
    "charset": "utf8mb4",
    "cursorclass": pymysql.cursors.DictCursor,
    "autocommit": True,
}


def _get_conn() -> pymysql.Connection:
    return pymysql.connect(**DB_CONFIG)


def init_db():
    """建表（兜底 — 正式环境用 init.sql）"""
    conn = _get_conn()
    conn.cursor().execute("""
        CREATE TABLE IF NOT EXISTS `ai_chat_conversations` (
            `id` varchar(36) NOT NULL,
            `user_id` varchar(50) NOT NULL DEFAULT 'default',
            `title` varchar(200) NOT NULL DEFAULT '新对话',
            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
            `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            `deleted` int DEFAULT '0',
            PRIMARY KEY (`id`),
            KEY `idx_user_id` (`user_id`),
            KEY `idx_update_time` (`update_time`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """)
    conn.cursor().execute("""
        CREATE TABLE IF NOT EXISTS `ai_chat_messages` (
            `id` bigint NOT NULL AUTO_INCREMENT,
            `conversation_id` varchar(36) NOT NULL,
            `role` varchar(20) NOT NULL,
            `content` text NOT NULL,
            `steps` json DEFAULT NULL,
            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (`id`),
            KEY `idx_conversation_id` (`conversation_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """)
    conn.close()


async def _run_sync(func, *args, **kwargs):
    return await asyncio.to_thread(func, *args, **kwargs)


def _create_conversation_sync(user_id: str, title: str) -> dict:
    conn = _get_conn()
    cur: Any = conn.cursor()
    conv_id = str(uuid.uuid4())
    cur.execute(
        "INSERT INTO ai_chat_conversations (id, user_id, title) VALUES (%s, %s, %s)",
        (conv_id, user_id, title),
    )
    cur.execute("SELECT * FROM ai_chat_conversations WHERE id = %s", (conv_id,))
    row = cur.fetchone()  # type: ignore[assignment]
    conn.close()
    return _serialize_conv(row)  # type: ignore[arg-type]


def _list_conversations_sync(user_id: str) -> list[dict]:
    conn = _get_conn()
    cur: Any = conn.cursor()
    cur.execute(
        "SELECT id, user_id, title, create_time, update_time "
        "FROM ai_chat_conversations WHERE user_id = %s AND deleted = 0 "
        "ORDER BY update_time DESC",
        (user_id,),
    )
    rows = cur.fetchall()  # type: ignore[assignment]
    conn.close()
    return [_serialize_conv(r) for r in rows]  # type: ignore[call-overload]


def _get_conversation_sync(conv_id: str) -> Optional[dict]:
    conn = _get_conn()
    cur: Any = conn.cursor()
    cur.execute(
        "SELECT * FROM ai_chat_conversations WHERE id = %s AND deleted = 0",
        (conv_id,),
    )
    conv = cur.fetchone()  # type: ignore[assignment]
    if not conv:
        conn.close()
        return None
    result = _serialize_conv(conv)  # type: ignore[arg-type]
    cur.execute(
        "SELECT id, conversation_id, role, content, steps, create_time "
        "FROM ai_chat_messages WHERE conversation_id = %s ORDER BY id ASC",
        (conv_id,),
    )
    messages = cur.fetchall()  # type: ignore[assignment]
    result["messages"] = [
        {
            "id": m["id"],
            "conversation_id": m["conversation_id"],
            "role": m["role"],
            "content": m["content"],
            "steps": json.loads(m["steps"]) if m.get("steps") and isinstance(m["steps"], str) else (m.get("steps") or []),
            "created_at": _fmt(m["create_time"]),
        }
        for m in messages  # type: ignore[union-attr]
    ]
    conn.close()
    return result


def _add_message_sync(conv_id: str, role: str, content: str, steps: Optional[list] = None) -> dict:
    conn = _get_conn()
    cur: Any = conn.cursor()
    steps_json = json.dumps(steps, ensure_ascii=False) if steps else None
    cur.execute(
        "INSERT INTO ai_chat_messages (conversation_id, role, content, steps) VALUES (%s, %s, %s, %s)",
        (conv_id, role, content, steps_json),
    )
    cur.execute(
        "UPDATE ai_chat_conversations SET update_time = NOW() WHERE id = %s",
        (conv_id,),
    )
    msg_id = cur.lastrowid
    cur.execute(
        "SELECT id, conversation_id, role, content, steps, create_time FROM ai_chat_messages WHERE id = %s",
        (msg_id,),
    )
    row = cur.fetchone()  # type: ignore[assignment]
    conn.close()
    r: dict = dict(row)  # type: ignore[arg-type]
    r["steps"] = json.loads(r["steps"]) if r.get("steps") and isinstance(r["steps"], str) else (r.get("steps") or [])
    r["created_at"] = _fmt(r["create_time"])
    return r


def _update_title_sync(conv_id: str, title: str):
    conn = _get_conn()
    conn.cursor().execute(
        "UPDATE ai_chat_conversations SET title = %s WHERE id = %s",
        (title, conv_id),
    )
    conn.close()


def _delete_conversation_sync(conv_id: str):
    conn = _get_conn()
    conn.cursor().execute(
        "UPDATE ai_chat_conversations SET deleted = 1 WHERE id = %s",
        (conv_id,),
    )
    conn.close()


def _fmt(val) -> str:
    if isinstance(val, datetime):
        return val.isoformat()
    return str(val) if val else ""


def _serialize_conv(row: dict) -> dict:
    return {
        "id": row["id"],
        "user_id": row.get("user_id", "default"),
        "title": row.get("title", ""),
        "created_at": _fmt(row.get("create_time", "")),
        "updated_at": _fmt(row.get("update_time") or row.get("create_time", "")),
    }


class ConversationStore:

    async def create_conversation(self, user_id: str = "default", title: str = "新对话") -> dict:
        return await _run_sync(_create_conversation_sync, user_id, title)

    async def list_conversations(self, user_id: str = "default") -> list[dict]:
        return await _run_sync(_list_conversations_sync, user_id)

    async def get_conversation(self, conv_id: str) -> Optional[dict]:
        return await _run_sync(_get_conversation_sync, conv_id)

    async def add_message(self, conv_id: str, role: str, content: str, steps: Optional[list] = None) -> dict:
        return await _run_sync(_add_message_sync, conv_id, role, content, steps)

    async def update_title(self, conv_id: str, title: str):
        await _run_sync(_update_title_sync, conv_id, title)

    async def delete_conversation(self, conv_id: str):
        await _run_sync(_delete_conversation_sync, conv_id)


conversation_store = ConversationStore()
