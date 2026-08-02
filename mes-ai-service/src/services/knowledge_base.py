"""RAG 知识库 — 设备手册 / 质检标准 / 工艺文档向量检索"""

import json
import logging
import os
from typing import List, Dict, Optional
from pathlib import Path

logger = logging.getLogger(__name__)

try:
    import numpy as np
    NUMPY_AVAILABLE = True
except ImportError:
    NUMPY_AVAILABLE = False

KNOWLEDGE_DIR = Path(__file__).resolve().parent.parent.parent / "knowledge"


def _simple_tokenize(text: str) -> List[str]:
    import re
    tokens = re.findall(r'[\u4e00-\u9fff\w]+', text.lower())
    return tokens


def _compute_similarity(query_tokens: List[str], doc_tokens: List[str]) -> float:
    if not query_tokens or not doc_tokens:
        return 0.0
    q_set = set(query_tokens)
    d_set = set(doc_tokens)
    intersection = q_set & d_set
    union = q_set | d_set
    return len(intersection) / max(len(union), 1)


DEFAULT_DOCUMENTS = [
    {
        "id": "doc-device-001",
        "title": "CNC 加工中心操作手册",
        "category": "设备手册",
        "content": """CNC 加工中心操作规范：
1. 开机前检查冷却液液位、气压是否正常
2. 主轴预热 5 分钟后方可进行加工
3. 正常工作时主轴温度应保持在 35°C-55°C 之间
4. 若温度超过 70°C 需停机冷却并检查主轴轴承
5. 每日保养：清理切屑、检查润滑油位
6. 每周保养：更换冷却液、检查皮带张紧度
7. 每月保养：校准主轴精度、更换润滑油""",
    },
    {
        "id": "doc-device-002",
        "title": "设备温度异常处理指南",
        "category": "设备手册",
        "content": """设备温度异常处理流程：
一、轻微偏高（55°C-70°C）
  1. 检查冷却系统是否正常运行
  2. 检查切削液流量是否充足
  3. 降低进给速度 10%-20%
  4. 持续监控 30 分钟

二、严重偏高（70°C 以上）
  1. 立即停机
  2. 检查主轴轴承是否损坏
  3. 检查冷却管路是否堵塞
  4. 联系维修工程师
  5. 记录异常时间和温度数据

三、预防措施
  1. 定期更换冷却液
  2. 保持散热器清洁
  3. 避免长时间满负荷运行""",
    },
    {
        "id": "doc-quality-001",
        "title": "产品质量检验标准",
        "category": "质检标准",
        "content": """产品质量检验标准（QMS-2026）：
一、外观检查
  1. 表面无划痕、无氧化斑
  2. 颜色均匀，无色差
  3. 边缘无毛刺

二、尺寸检查
  1. 关键尺寸公差 ±0.05mm
  2. 一般尺寸公差 ±0.1mm
  3. 使用 calibrated 量具测量

三、性能测试
  1. 硬度测试：HRC 45-50
  2. 表面粗糙度：Ra ≤ 0.8μm
  3. 平面度：≤ 0.02mm

四、抽样标准
  1. 正常检验：AQL 1.0
  2. 加严检验：AQL 0.65
  3. 放宽检验：AQL 1.5""",
    },
    {
        "id": "doc-quality-002",
        "title": "SPC 统计分析标准",
        "category": "质检标准",
        "content": """统计过程控制（SPC）标准：
一、控制图选择
  1. 计量型数据：X̄-R 图或 X̄-S 图
  2. 计数型数据：p 图或 u 图

二、判异准则
  1. 一点超出控制限
  2. 连续 7 点在同一侧
  3. 连续 7 点上升或下降
  4. 连续 3 点中有 2 点接近控制限
  5. 连续 5 点中有 4 点在中心线同侧

三、过程能力
  1. Cp ≥ 1.33：过程能力充分
  2. 1.0 ≤ Cp < 1.33：过程能力尚可，需监控
  3. Cp < 1.0：过程能力不足，需改进""",
    },
    {
        "id": "doc-workorder-001",
        "title": "工单管理规范",
        "category": "业务流程",
        "content": """工单管理规范：
一、工单类型
  1. 生产工单：产品批量生产
  2. 维修工单：设备故障维修
  3. 质检工单：产品质量检验

二、工单状态流转
  CREATED → RELEASED → IN_PROGRESS → COMPLETED → CLOSED

三、工单优先级
  HIGH：紧急订单或设备故障，需立即处理
  MEDIUM：正常生产计划
  LOW：可延期任务

四、工单关闭条件
  1. 产品已全部完工或维修已完成
  2. 质检已通过
  3. 数据已录入系统""",
    },
    {
        "id": "doc-maintenance-001",
        "title": "设备维护保养计划",
        "category": "设备手册",
        "content": """2026年度设备维护保养计划：
一、日常维护（每班）
  1. 清洁设备表面和切屑
  2. 检查润滑油位
  3. 记录运行参数

二、周维护（每周五）
  1. 清洁冷却系统过滤网
  2. 检查电气线路
  3. 紧固松动螺栓

三、月维护（每月末）
  1. 更换切削液
  2. 检查主轴精度
  3. 润滑丝杠导轨

四、年维护（每年12月）
  1. 全面精度检测
  2. 更换磨损部件
  3. 系统软件升级""",
    },
]


class KnowledgeBase:
    """轻量级 RAG 知识库 — 基于关键词检索的设备手册和质检标准"""

    def __init__(self, kb_dir: Optional[str] = None):
        self.documents: List[Dict] = []
        self.index: List[List[str]] = []

        kb_path = Path(kb_dir) if kb_dir else KNOWLEDGE_DIR
        if kb_path.exists():
            self._load_from_dir(kb_path)
        else:
            self.documents = list(DEFAULT_DOCUMENTS)

        if not self.documents:
            self.documents = list(DEFAULT_DOCUMENTS)

        self._build_index()
        logger.info(f"知识库初始化完成，共 {len(self.documents)} 篇文档")

    def _load_from_dir(self, kb_dir: Path):
        for fpath in kb_dir.glob("*.json"):
            try:
                with open(fpath, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    if isinstance(data, list):
                        self.documents.extend(data)
                    else:
                        self.documents.append(data)
            except Exception as e:
                logger.warning(f"加载知识库文件失败 {fpath}: {e}")
        for fpath in kb_dir.glob("*.txt"):
            try:
                title = fpath.stem
                content = fpath.read_text(encoding="utf-8")
                self.documents.append({
                    "id": f"doc-{title}",
                    "title": title,
                    "category": "通用文档",
                    "content": content,
                })
            except Exception as e:
                logger.warning(f"加载知识库文件失败 {fpath}: {e}")

    def _build_index(self):
        self.index = []
        for doc in self.documents:
            tokens = _simple_tokenize(doc.get("title", "") + " " + doc.get("content", ""))
            self.index.append(tokens)

    def add_document(self, doc: Dict):
        self.documents.append(doc)
        tokens = _simple_tokenize(doc.get("title", "") + " " + doc.get("content", ""))
        self.index.append(tokens)

    def search(self, query: str, top_k: int = 3) -> Dict:
        query_tokens = _simple_tokenize(query)
        scored = []
        for i, doc_tokens in enumerate(self.index):
            score = _compute_similarity(query_tokens, doc_tokens)
            scored.append((score, i))
        scored.sort(key=lambda x: x[0], reverse=True)

        results = []
        for score, idx in scored[:top_k]:
            if score > 0:
                doc = self.documents[idx]
                results.append({
                    "id": doc["id"],
                    "title": doc["title"],
                    "category": doc.get("category", ""),
                    "content": doc["content"][:500],
                    "score": round(score, 3),
                })

        return {
            "success": True,
            "query": query,
            "results": results,
            "total": len(results),
        }

    def get_all_categories(self) -> List[str]:
        cats = set()
        for doc in self.documents:
            cat = doc.get("category", "未分类")
            cats.add(cat)
        return sorted(cats)
