<template>
  <div class="ai-layout">
    <div class="ai-sidebar">
      <div class="sidebar-hero">
        <div class="hero-glow"></div>
        <div class="hero-icon-box">
          <el-icon :size="28"><MagicStick /></el-icon>
        </div>
        <h2 class="hero-title">AI 生产助理</h2>
        <p class="hero-desc">基于大语言模型的智能生产助手，可查询设备状态、诊断异常、创建工单、搜索手册</p>
      </div>

      <div class="conv-section">
        <div class="conv-header">
          <h4 class="conv-title">聊天记录</h4>
          <button class="conv-new-btn" @click="store.newChat()">
            <el-icon :size="14"><Plus /></el-icon>
          </button>
        </div>
        <div class="conv-list">
          <div
            v-for="conv in store.conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: conv.id === store.currentId }"
            @click="store.selectConversation(conv.id)"
          >
            <span class="conv-name">{{ conv.title }}</span>
            <span class="conv-time">{{ formatDate(conv.updated_at) }}</span>
            <button class="conv-del-btn" @click.stop="store.removeConversation(conv.id)" title="删除">
              <el-icon :size="12"><Close /></el-icon>
            </button>
          </div>
          <div v-if="store.loadingList" class="conv-empty">加载中...</div>
          <div v-else-if="!store.conversations.length" class="conv-empty">暂无聊天记录</div>
        </div>
      </div>

      <div class="capabilities">
        <h4 class="cap-title">快捷能力</h4>
        <div class="cap-list">
          <div v-for="cap in capabilities" :key="cap.label" class="cap-card" @click="assistantRef?.focusInput(cap.prompt)">
            <div class="cap-icon-box">
              <el-icon :size="16"><component :is="cap.icon" /></el-icon>
            </div>
            <div class="cap-info">
              <span class="cap-label">{{ cap.label }}</span>
              <span class="cap-desc">{{ cap.desc }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <div class="status-dot online"></div>
        <span>Agent 在线</span>
      </div>
    </div>

    <div class="ai-main">
      <AiAssistant ref="assistantRef" :floating="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AiAssistant from '@/components/ai/AiAssistant.vue'
import { useAiChatStore } from '@/stores/aiChat'
import { MagicStick, Monitor, Warning, Document, Notebook, TrendCharts, Setting, Close, Plus } from '@element-plus/icons-vue'
import type { Component } from 'vue'

const assistantRef = ref<InstanceType<typeof AiAssistant> | null>(null)
const store = useAiChatStore()

const capabilities: { icon: Component; label: string; desc: string; prompt: string }[] = [
  { icon: Monitor, label: '设备监控', desc: '查询设备实时状态与数据', prompt: '查看所有设备状态' },
  { icon: Warning, label: '异常诊断', desc: '自动检测温度/状态异常', prompt: '查看 DEV-001 温度状态是否正常' },
  { icon: Document, label: '工单创建', desc: '一键生成维修工单', prompt: '创建一条 HIGH 优先级的维修工单' },
  { icon: Notebook, label: '手册检索', desc: '搜索设备维护文档', prompt: '主轴温度过高怎么处理' },
  { icon: TrendCharts, label: '数据分析', desc: '趋势分析与统计报告', prompt: '分析最近设备报警趋势' },
  { icon: Setting, label: '自动规则', desc: '条件触发自动操作', prompt: '如果温度超过55°C就创建工单' },
]

function formatDate(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return Math.floor(diff / 60_000) + '分钟前'
  if (diff < 86400_000) return Math.floor(diff / 3600_000) + '小时前'
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.ai-layout { display: flex; height: 100%; border-radius: 12px; overflow: hidden; }

/* ===== Sidebar ===== */
.ai-sidebar {
  width: 280px; flex-shrink: 0; background: var(--bg-sidebar, #0d0d12);
  border-right: 1px solid var(--border-color, #252530);
  display: flex; flex-direction: column; padding: 24px 0 0; gap: 0; overflow: hidden;
}

.sidebar-hero { text-align: center; position: relative; padding: 0 20px 20px; border-bottom: 1px solid var(--border-color, #252530); }
.hero-glow { position: absolute; top: -30px; left: 50%; transform: translateX(-50%); width: 80px; height: 80px; border-radius: 50%; background: radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%); pointer-events: none; }
.hero-icon-box {
  width: 56px; height: 56px; border-radius: 14px;
  background: var(--gradient-primary, linear-gradient(135deg, #6366f1, #8b5cf6)); color: #fff;
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 12px; position: relative; z-index: 1;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.35);
}
.hero-title { font-size: 18px; font-weight: 700; color: var(--text-primary, #f0f0f5); margin: 0 0 8px; position: relative; z-index: 1; }
.hero-desc { font-size: 12px; color: var(--text-muted, #505060); line-height: 1.6; margin: 0; position: relative; z-index: 1; }

/* ===== Conversation List ===== */
.conv-section { padding: 12px 14px; border-bottom: 1px solid var(--border-color, #252530); flex-shrink: 0; }
.conv-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.conv-title { font-size: 11px; font-weight: 600; color: var(--text-muted, #505060); text-transform: uppercase; letter-spacing: 0.5px; margin: 0; }
.conv-new-btn {
  width: 24px; height: 24px; border-radius: 5px; border: none;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  background: transparent; color: var(--text-muted, #505060); transition: all 0.15s ease;
}
.conv-new-btn:hover { background: var(--bg-hover, #1a1a28); color: var(--accent, #6366f1); }
.conv-list { display: flex; flex-direction: column; gap: 2px; max-height: 180px; overflow-y: auto; }
.conv-item {
  display: flex; align-items: center; gap: 6px; padding: 7px 10px; border-radius: 6px;
  cursor: pointer; transition: all 0.15s ease; position: relative;
}
.conv-item:hover { background: var(--bg-hover, #1a1a28); }
.conv-item.active { background: var(--accent-light, rgba(99,102,241,0.12)); }
.conv-item.active .conv-name { color: var(--accent, #6366f1); font-weight: 500; }
.conv-name { font-size: 13px; color: var(--text-primary, #f0f0f5); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 10px; color: var(--text-muted, #505060); flex-shrink: 0; }
.conv-del-btn {
  width: 20px; height: 20px; border-radius: 4px; border: none; display: flex;
  align-items: center; justify-content: center; cursor: pointer;
  background: transparent; color: var(--text-muted, #505060); opacity: 0; transition: all 0.15s ease;
}
.conv-item:hover .conv-del-btn { opacity: 1; }
.conv-del-btn:hover { background: rgba(239,68,68,0.15); color: var(--danger, #ef4444); }
.conv-empty { font-size: 12px; color: var(--text-muted, #505060); padding: 10px 0; text-align: center; }

/* ===== Capabilities ===== */
.capabilities { flex: 1; overflow-y: auto; padding: 12px 14px; }
.cap-title { font-size: 11px; font-weight: 600; color: var(--text-muted, #505060); text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 8px; }
.cap-list { display: flex; flex-direction: column; gap: 2px; }
.cap-card {
  display: flex; align-items: center; gap: 10px; padding: 8px 10px; border-radius: var(--radius-sm, 6px);
  cursor: pointer; transition: all 0.15s ease;
}
.cap-card:hover { background: var(--bg-hover, #1a1a28); }
.cap-icon-box {
  width: 28px; height: 28px; border-radius: 7px;
  background: var(--accent-light, rgba(99,102,241,0.12)); color: var(--accent, #6366f1);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.cap-info { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.cap-label { font-size: 12px; font-weight: 500; color: var(--text-primary, #f0f0f5); }
.cap-desc { font-size: 11px; color: var(--text-muted, #505060); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.sidebar-footer {
  display: flex; align-items: center; gap: 8px; padding: 14px 18px;
  border-top: 1px solid var(--border-color, #252530); font-size: 12px; color: var(--text-secondary, #a0a0b0);
}
.status-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.status-dot.online { background: var(--success, #10b981); box-shadow: 0 0 8px rgba(16, 185, 129, 0.4); }

/* ===== Main ===== */
.ai-main { flex: 1; min-width: 0; background: var(--bg-card, #12121a); }
</style>
