"""对话历史 SQLite 存储"""
import sqlite3
import json
import os
import uuid
import asyncio
from datetime import datetime
from typing import Optional

DB_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "data", "conversations.db")


def _ensure_db_dir():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)


def _get_conn() -> sqlite3.Connection:
    _ensure_db_dir()
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def init_db():
    conn = _get_conn()
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS conversations (
            id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL DEFAULT 'default',
            title TEXT NOT NULL DEFAULT '新对话',
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            conversation_id TEXT NOT NULL,
            role TEXT NOT NULL CHECK(role IN ('user', 'assistant')),
            content TEXT NOT NULL,
            steps TEXT DEFAULT NULL,
            created_at TEXT NOT NULL,
            FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
        );
    """)
    conn.commit()
    conn.close()


async def _run_sync(func, *args, **kwargs):
    return await asyncio.to_thread(func, *args, **kwargs)


def _create_conversation_sync(user_id: str, title: str) -> dict:
    conn = _get_conn()
    conv_id = str(uuid.uuid4())
    now = datetime.utcnow().isoformat()
    conn.execute(
        "INSERT INTO conversations (id, user_id, title, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
        (conv_id, user_id, title, now, now),
    )
    conn.commit()
    row = conn.execute("SELECT * FROM conversations WHERE id = ?", (conv_id,)).fetchone()
    conn.close()
    return dict(row)


def _list_conversations_sync(user_id: str) -> list[dict]:
    conn = _get_conn()
    rows = conn.execute(
        "SELECT * FROM conversations WHERE user_id = ? ORDER BY updated_at DESC",
        (user_id,),
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def _get_conversation_sync(conv_id: str) -> Optional[dict]:
    conn = _get_conn()
    conv = conn.execute("SELECT * FROM conversations WHERE id = ?", (conv_id,)).fetchone()
    if not conv:
        conn.close()
        return None
    msgs = conn.execute(
        "SELECT * FROM messages WHERE conversation_id = ? ORDER BY id ASC",
        (conv_id,),
    ).fetchall()
    result = dict(conv)
    result["messages"] = []
    for m in msgs:
        d = dict(m)
        if d.get("steps"):
            try:
                d["steps"] = json.loads(d["steps"])
            except (json.JSONDecodeError, TypeError):
                d["steps"] = []
        else:
            d["steps"] = []
        result["messages"].append(d)
    conn.close()
    return result


def _add_message_sync(conv_id: str, role: str, content: str, steps: Optional[list] = None) -> dict:
    conn = _get_conn()
    now = datetime.utcnow().isoformat()
    steps_json = json.dumps(steps, ensure_ascii=False) if steps else None
    conn.execute(
        "INSERT INTO messages (conversation_id, role, content, steps, created_at) VALUES (?, ?, ?, ?, ?)",
        (conv_id, role, content, steps_json, now),
    )
    conn.execute(
        "UPDATE conversations SET updated_at = ? WHERE id = ?",
        (now, conv_id),
    )
    conn.commit()
    msg_id = conn.execute("SELECT last_insert_rowid()").fetchone()[0]
    row = conn.execute("SELECT * FROM messages WHERE id = ?", (msg_id,)).fetchone()
    conn.close()
    return dict(row)


def _update_title_sync(conv_id: str, title: str):
    conn = _get_conn()
    conn.execute("UPDATE conversations SET title = ? WHERE id = ?", (title, conv_id))
    conn.commit()
    conn.close()


def _delete_conversation_sync(conv_id: str):
    conn = _get_conn()
    conn.execute("DELETE FROM conversations WHERE id = ?", (conv_id,))
    conn.commit()
    conn.close()


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
