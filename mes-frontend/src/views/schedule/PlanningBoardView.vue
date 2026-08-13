<template>
  <div class="page-wrapper planning-page" v-loading="loading">
    <!-- 顶部工具栏：标题 + 图例 + 操作 -->
    <div class="page-header">
      <div class="header-left">
        <h2>生产调度看板</h2>
        <span class="sub-title">工序级排产 · 自动排程 · 冲突检测 · 冻结下发</span>
      </div>
      <div class="header-right">
        <div class="legend-group">
          <span v-for="lg in legendItems" :key="lg.key" class="legend-item">
            <i class="legend-dot" :style="{ background: lg.color }"></i>{{ lg.label }}
          </span>
          <span class="legend-item"><i class="legend-dot" style="background:#e6a23c"></i>冻结</span>
          <span class="legend-item"><i class="legend-dot" style="background:#722ed1"></i>已下发</span>
        </div>
        <span class="header-divider"></span>
        <el-select v-model="granularity" size="small" style="width: 88px">
          <el-option label="日粒度" value="day" />
          <el-option label="小时粒度" value="hour" />
        </el-select>
        <el-date-picker
          v-model="windowRange"
          type="daterange"
          range-separator="~"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          size="small"
          style="width: 230px"
          @change="loadBoard"
        />
        <el-button type="primary" size="small" @click="loadBoard">
          <el-icon><Refresh /></el-icon>&nbsp;刷新
        </el-button>
        <el-switch v-model="autoRefresh" size="small" active-text="自动刷新" inline-prompt />
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="success" size="small" plain @click="doAutoPlan(true)">
        <el-icon><MagicStick /></el-icon>&nbsp;自动排程
      </el-button>
      <el-button type="warning" size="small" plain @click="doAutoPlan(false)">
        <el-icon><RefreshRight /></el-icon>&nbsp;全部重排
      </el-button>
      <el-button size="small" plain @click="doUndo">
        <el-icon><Back /></el-icon>&nbsp;撤销
      </el-button>
      <span class="action-spacer"></span>
      <div class="zoom-group">
        <el-button size="small" plain :icon="ZoomOut" @click="zoomOut" />
        <span class="zoom-text">{{ (zoomLevel * 100).toFixed(0) }}%</span>
        <el-button size="small" plain :icon="ZoomIn" @click="zoomIn" />
        <el-button size="small" plain :icon="Aim" @click="zoomReset" title="重置缩放" />
      </div>
      <span class="work-hours">
        <el-icon><Clock /></el-icon>
        窗口工时 {{ fmtWorkHours(workMinutes) }} · {{ shiftText }}
      </span>
      <el-badge v-if="conflicts.length" :value="conflicts.length" class="conflict-badge">
        <el-tag type="danger" size="small" effect="light">时间冲突</el-tag>
      </el-badge>
    </div>

    <div class="board-layout">
      <!-- 甘特图主体 -->
      <div class="gantt-area">
        <div class="gantt-body" ref="bodyRef">
          <!-- 表头：时间刻度（sticky 固定，双级：月 + 日/小时） -->
          <div class="gantt-head">
            <div class="gantt-row-label head-corner">设备 / 工序</div>
            <div class="gantt-timeline-wrap">
              <!-- 上级刻度：月份 -->
              <div class="gantt-timeline month-row">
                <div
                  v-for="m in monthCells"
                  :key="m.key"
                  class="gantt-month-cell"
                  :style="{ width: m.widthPx + 'px' }"
                >
                  {{ m.label }}
                </div>
              </div>
              <!-- 下级刻度：日 / 小时 -->
              <div class="gantt-timeline day-row">
                <div
                  v-for="cell in headCells"
                  :key="cell.key"
                  class="gantt-day-cell"
                  :class="{ weekend: cell.shaded }"
                  :style="{ width: cell.widthPx + 'px' }"
                >
                  <span class="gantt-day-label">{{ cell.label }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 拖拽落点幽灵（绝对定位，跟随鼠标所在行与时间） -->
          <div v-if="dragGhost" class="drag-ghost" :style="ghostStyle"></div>
          <!-- 待排产池 -->
          <div class="machine-row pending-row">
            <div class="gantt-row-label" @contextmenu.prevent="openRowCtx($event, null, null)">
              <div class="machine-cell">
                <span class="machine-name">待排产池</span>
                <span class="machine-count">{{ unassigned.length }} 单</span>
              </div>
            </div>
            <div class="gantt-track pool-track" ref="poolRef" :style="{ width: ganttTotalPx + 'px' }" :class="{ 'drop-target': dragState && dragOverPool }">
              <div class="gantt-gridline">
                <div v-for="cell in headCells" :key="cell.key" class="grid-col" :class="{ weekend: cell.shaded }" :style="{ width: cell.widthPx + 'px' }"></div>
              </div>
              <div class="pending-list">
                <div
                  v-for="t in unassigned"
                  :key="t.id"
                  class="pending-card"
                  :class="[('pri-' + (t.priority || 'MEDIUM').toLowerCase()), { 'card-selected': selectedTask?.id === t.id && !t.scheduleId }]"
                  @mousedown="onCardDown($event, t)"
                  @click.stop="selectTask(t, null)"
                >
                  <span class="pc-no">{{ t.orderNo }}</span>
                  <span class="pc-name">{{ t.productName }}</span>
                  <span class="pc-qty">{{ t.planQuantity }} 件 · {{ priLabel(t.priority) }}</span>
                </div>
                <div v-if="!unassigned.length" class="pool-empty">暂无待排产工单，可取消排产拖回</div>
              </div>
            </div>
          </div>

          <!-- 每个设备一行：按工单分泳道显示工序 -->
          <div v-for="eq in equipment" :key="eq.id" class="machine-row">
            <div class="gantt-row-label" @contextmenu.prevent="openRowCtx($event, eq, null)">
              <div class="machine-cell">
                <div class="machine-top">
                  <span class="machine-name">{{ eq.workstationName }}</span>
                  <span class="machine-status" :class="eq.status">
                    <i class="status-dot"></i>{{ eq.status }}
                  </span>
                </div>
                <div class="machine-meta">
                  <span class="machine-code">{{ eq.workstationCode }}</span>
                  <span v-if="eq.bottleneck" class="bottleneck-tag">瓶颈</span>
                  <span class="load-text" :class="loadClass(eq.loadRate)">{{ eq.loadRate || 0 }}%</span>
                </div>
                <div class="load-bar">
                  <div class="load-fill" :class="loadClass(eq.loadRate)" :style="{ width: (eq.loadRate || 0) + '%' }"></div>
                </div>
              </div>
            </div>
            <div
              class="gantt-track"
              :data-ws-id="eq.id"
              :style="{ width: ganttTotalPx + 'px' }"
              :class="{ 'drop-target': dragState && dragTargetWsId === eq.id }"
              @click="clearSelection"
            >
              <div class="gantt-gridline">
                <div v-for="cell in headCells" :key="cell.key" class="grid-col" :class="{ weekend: cell.shaded }" :style="{ width: cell.widthPx + 'px' }"></div>
              </div>

              <!-- 按工单分组泳道 -->
              <div v-for="lane in orderLanes(eq)" :key="lane.key" class="order-lane" :class="{ 'lane-selected': lane.id === selectedOrderId }">
                <div class="order-lane-head" @click.stop="selectLane(lane)">
                  <span class="lane-dot" :style="{ background: laneColor(lane.id) }"></span>
                  <span class="lane-no">{{ lane.orderNo }}</span>
                  <el-tag size="small" :type="priType(lane.priority)" effect="plain" style="height:16px;padding:0 6px">{{ priLabel(lane.priority) }}</el-tag>
                  <span v-if="lane.status === 'FROZEN'" class="lane-badge frozen">已冻结</span>
                  <span v-if="lane.status === 'RELEASED'" class="lane-badge released">已下发</span>
                  <span v-if="lane.planStatus === 'DELAYED'" class="lane-badge delayed">延误</span>
                </div>
                <div class="lane-bars">
                  <div
                    v-for="t in lane.steps"
                    :key="t.scheduleId"
                    class="gantt-bar"
                    :class="[
                      'st-' + t.planStatus.toLowerCase(),
                      { 'bar-conflict': conflictStepIds.has(t.scheduleId), 'bar-selected': selectedTask?.scheduleId === t.scheduleId }
                    ]"
                    :style="barStyle(t)"
                    @mousedown="onBarDown($event, t, eq)"
                    @click.stop
                    @contextmenu.prevent="openBarCtx($event, t, eq)"
                  >
                    <i v-if="barResizable(t)" class="resize-h left" @mousedown.stop="onResizeDown($event, t, eq, 'l')"></i>
                    <span class="bar-label">{{ barDisplay(t) }}</span>
                    <i v-if="barResizable(t)" class="resize-h" @mousedown.stop="onResizeDown($event, t, eq, 'r')"></i>
                  </div>
                </div>
              </div>

              <div v-if="!eq.tasks.length" class="track-empty">暂无排产</div>
            </div>
          </div>

          <el-empty v-if="!equipment.length && !unassigned.length" description="暂无排产数据" :image-size="80" />
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="side-panel">
        <el-tabs v-model="sideTab" stretch>
          <el-tab-pane label="详情" name="detail">
            <div v-if="selectedTask" class="detail-wrap">
              <div class="detail-header" :class="'hd-st-' + statClass(selectedTask.planStatus)">
                <div class="detail-title-row">
                  <span class="detail-status-tag">{{ statusLabel(selectedTask.planStatus) }}</span>
                  <span v-if="selectedTask.scheduleStatus === 'FROZEN'" class="detail-status-tag">已冻结</span>
                  <span v-else-if="selectedTask.scheduleStatus === 'RELEASED'" class="detail-status-tag">已下发</span>
                </div>
                <div class="detail-order">{{ selectedTask.orderNo }}</div>
                <div class="detail-product">{{ selectedTask.productName }}</div>
              </div>
              <div class="detail-body">
                <el-descriptions :column="1" size="small" label-width="84px" border>
                  <el-descriptions-item label="工序">{{ selectedTask.stepName || '整单' }}</el-descriptions-item>
                  <el-descriptions-item label="产品型号">{{ selectedTask.productModel || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="优先级">
                    <el-tag size="small" :type="priType(selectedTask.priority)" effect="light">{{ priLabel(selectedTask.priority) }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="计划数量">{{ selectedTask.planQuantity }}</el-descriptions-item>
                  <el-descriptions-item label="已完成">{{ selectedTask.completedQuantity }}</el-descriptions-item>
                  <el-descriptions-item label="生产进度">
                    <el-progress :percentage="selectedTask.progress || 0" :stroke-width="8" />
                  </el-descriptions-item>
                  <el-descriptions-item label="设备">
                    {{ equipmentOf(selectedTask)?.workstationName || '未分配' }}
                  </el-descriptions-item>
                  <el-descriptions-item label="工时">{{ selectedTask.durationMin }} 分钟</el-descriptions-item>
                  <el-descriptions-item label="计划开始">{{ fmtTime(selectedTask.plannedStartTime) }}</el-descriptions-item>
                  <el-descriptions-item label="计划结束">{{ fmtTime(selectedTask.plannedEndTime) }}</el-descriptions-item>
                  <el-descriptions-item label="实际开始">{{ fmtTime(selectedTask.actualStartTime) }}</el-descriptions-item>
                  <el-descriptions-item label="备注">{{ selectedTask.remark || '-' }}</el-descriptions-item>
                </el-descriptions>

                <div class="step-list-title">该工单工序（{{ orderSteps(selectedTask.id).length }}）</div>
                <div v-for="st in orderSteps(selectedTask.id)" :key="st.scheduleId" class="step-item" @click="selectTask(st, equipmentOf(st))">
                  <span class="step-dot" :style="{ background: laneColor(st.id) }"></span>
                  <span class="step-name">{{ st.stepNo ? '工序' + st.stepNo + ' ' + st.stepName : '整单' }}</span>
                  <span class="step-dev">{{ equipmentOf(st)?.workstationName || '未分配' }}</span>
                  <span class="step-time">{{ fmtShort(st.plannedStartTime) }}~{{ fmtShort(st.plannedEndTime) }}</span>
                </div>

                <div class="detail-actions">
                  <el-button size="small" type="primary" plain @click="openAdjustDialog(selectedTask, false)">调整时间</el-button>
                  <el-button size="small" type="warning" plain @click="openAdjustDialog(selectedTask, true)">整单移动</el-button>
                  <el-button size="small" type="success" plain @click="doRelease(selectedTask.id)">下发</el-button>
                  <el-button size="small" type="danger" plain @click="doUnassign(selectedTask.id)">取消排产</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="点击甘特条查看详情" :image-size="70" />
          </el-tab-pane>

          <el-tab-pane label="变更日志" name="logs">
            <div class="log-list">
              <div v-for="lg in logs" :key="lg.id" class="log-item">
                <span class="log-action" :class="logClass(lg.action)">{{ logActionLabel(lg.action) }}</span>
                <span class="log-desc">{{ lg.actionDesc }}</span>
                <span class="log-time">{{ fmtTime(lg.createTime) }}</span>
              </div>
              <el-empty v-if="!logs.length" description="暂无变更记录" :image-size="60" />
            </div>
          </el-tab-pane>

          <el-tab-pane label="冲突" name="conflicts">
            <div class="conflict-list">
              <div v-for="(c, i) in conflicts" :key="i" class="conflict-item">
                <el-icon color="#f56c6c"><WarningFilled /></el-icon>
                <div class="conflict-body">
                  <div class="conflict-title">{{ c.workstationName }}：{{ c.orderNoA }}[{{ c.stepNameA }}] × {{ c.orderNoB }}[{{ c.stepNameB }}]</div>
                  <div class="conflict-time">{{ fmtTime(c.overlapStart) }} ~ {{ fmtTime(c.overlapEnd) }} 重叠</div>
                </div>
              </div>
              <el-empty v-if="!conflicts.length" description="无冲突，排产健康" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div v-if="ctxMenu" class="ctx-menu" :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }" @mouseleave="ctxMenu = null">
      <template v-if="ctxMenu.task">
        <div class="ctx-title">{{ ctxMenu.task.orderNo }}</div>
        <div class="ctx-item" @click="openAdjustDialog(ctxMenu.task, false)">调整本工序时间…</div>
        <div class="ctx-item" @click="openAdjustDialog(ctxMenu.task, true)">整单移动 / 调时间…</div>
        <el-divider style="margin: 4px 0" />
        <div v-if="ctxMenu.task.scheduleStatus !== 'FROZEN'" class="ctx-item" @click="doFreeze(ctxMenu.task, false)">冻结本工序</div>
        <div v-else class="ctx-item" @click="doUnfreeze(ctxMenu.task)">解除冻结</div>
        <div class="ctx-item" @click="doFreeze(ctxMenu.task, true)">冻结整单</div>
        <el-divider style="margin: 4px 0" />
        <div class="ctx-item" @click="doRelease(ctxMenu.task.id)">下发排产</div>
        <div class="ctx-item danger" @click="doUnassign(ctxMenu.task.id)">取消排产（拖回池）</div>
      </template>
      <template v-else-if="ctxMenu.eq">
        <div class="ctx-title">{{ ctxMenu.eq.workstationName }}</div>
        <div class="ctx-item" @click="doFreezeWs(ctxMenu.eq, true)">冻结设备全部工序</div>
        <div class="ctx-item" @click="doFreezeWs(ctxMenu.eq, false)">解冻设备全部工序</div>
      </template>
      <template v-else>
        <div class="ctx-title">待排产池</div>
        <div class="ctx-item" @click="doAutoPlan(true)">自动排程（仅未排）</div>
        <div class="ctx-item" @click="doAutoPlan(false)">全部重排</div>
      </template>
    </div>

    <!-- 调整时间对话框 -->
    <el-dialog v-model="adjustVisible" :title="adjustWhole ? '整单移动 / 调整时间' : '调整工序时间'" width="420px">
      <el-form label-width="90px" size="small">
        <el-form-item label="目标设备">
          <el-select v-model="adjustForm.targetWorkstationId" style="width: 100%">
            <el-option v-for="eq in equipment" :key="eq.id" :label="eq.workstationName + '（' + eq.workstationCode + '）'" :value="eq.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="adjustForm.newStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="!adjustWhole && adjustTask?.scheduleId" label="结束时间">
          <el-date-picker v-model="adjustForm.newEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="强制保存">
          <el-switch v-model="adjustForm.force" />
          <span class="force-tip">忽略冲突直接保存</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="adjustVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="submitAdjust">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPlanningBoard,
  movePlanningTask,
  unassignPlanningOrder,
  autoPlanOrders,
  undoPlanning,
  freezePlanning,
  unfreezePlanning,
  releasePlanning
} from '@/api/services'

interface Task {
  scheduleId?: string
  id: string
  orderNo: string
  productName: string
  productModel?: string
  priority?: string
  status?: string
  planStatus: string
  scheduleStatus?: string
  planQuantity: number
  completedQuantity: number
  progress: number
  stepNo?: number
  stepName?: string
  durationMin?: number
  bottleneck?: boolean
  workstationId?: string
  sortOrder?: number
  plannedStartTime?: string
  plannedEndTime?: string
  actualStartTime?: string
  remark?: string
}

interface EqGroup {
  id: string
  workstationCode: string
  workstationName: string
  status: string
  loadRate: number
  bottleneck?: boolean
  taskCount: number
  tasks: Task[]
}

interface ConflictItem {
  workstationName: string
  scheduleAId?: string
  scheduleBId?: string
  orderNoA: string
  stepNameA: string
  orderNoB: string
  stepNameB: string
  overlapStart: string
  overlapEnd: string
}

interface LogItem {
  id: string
  action: string
  actionDesc: string
  createTime: string
}

interface DragState {
  type: 'bar' | 'pool' | 'resize-r' | 'resize-l'
  task: Task
  originWsId: string | null
  startClientX: number
  startMs: number
  endMs: number
  /** 鼠标按下点在工序内的时间偏移（锚点跟随） */
  grabOffsetMs: number
}

const loading = ref(false)
const equipment = ref<EqGroup[]>([])
const unassigned = ref<Task[]>([])
const shifts = ref<{ shiftName: string; startTime: string; endTime: string }[]>([])
const calendar = ref<{ workDate: string; workday: boolean }[]>([])
const conflicts = ref<ConflictItem[]>([])
const logs = ref<LogItem[]>([])
const workMinutes = ref(0)

const selectedTask = ref<Task | null>(null)
const selectedOrderId = ref<number | null>(null)
let selectedOrderKey: number | null = null
const sideTab = ref('detail')
const granularity = ref<'day' | 'hour'>('day')
const autoRefresh = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const windowRange = ref<string[]>([])
const windowStart = ref('')
const windowEnd = ref('')
const SNAP_MS = 30 * 60 * 1000
/** 甘特每日像素宽度（可缩放，横向滚动用） */
const zoomLevel = ref(1)
const DAY_PX = computed(() => 120 * zoomLevel.value)
const ZOOM_MIN = 0.35
const ZOOM_MAX = 2.5
/** 甘特内容总宽度（像素） */
const ganttTotalPx = computed(() => {
  const days = Math.max(1, Math.round((wsEndMs.value - wsStartMs.value) / 86400000))
  return days * DAY_PX.value
})

// ==================== 拖拽状态 ====================
const dragState = ref<DragState | null>(null)
const dragTargetWsId = ref<string | null>(null)
const dragOverPool = ref(false)
const dragGhost = ref<{ top: number; left: number; width: number } | null>(null)
const poolRef = ref<HTMLElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)

// ==================== 右键菜单 ====================
const ctxMenu = ref<{ x: number; y: number; task: Task | null; eq: EqGroup | null } | null>(null)

// ==================== 调整对话框 ====================
const adjustVisible = ref(false)
const adjustTask = ref<Task | null>(null)
const adjustWhole = ref(false)
const adjustForm = ref<{ targetWorkstationId: number | null; newStart: string; newEnd: string; force: boolean }>({
  targetWorkstationId: null,
  newStart: '',
  newEnd: '',
  force: false
})

const legendItems = [
  { key: 'PENDING', label: '待排产', color: '#909399' },
  { key: 'READY', label: '已排产', color: '#409eff' },
  { key: 'RUNNING', label: '运行中', color: '#2f9e69' },
  { key: 'COMPLETED', label: '已完成', color: '#a8c5a8' },
  { key: 'DELAYED', label: '延误', color: '#f56c6c' }
]

// ==================== 数据加载 ====================
let autoZoomed = false

const loadBoard = async () => {
  loading.value = true
  try {
    const [start, end] = windowRange.value || []
    const res = await getPlanningBoard(start ? start + ' 00:00:00' : undefined, end ? end + ' 23:59:59' : undefined)
    const data = res?.data
    if (!data) return
    equipment.value = data.equipment || []
    unassigned.value = data.unassigned || []
    shifts.value = data.shifts || []
    calendar.value = data.calendar || []
    conflicts.value = data.conflicts || []
    logs.value = data.logs || []
    workMinutes.value = data.workMinutes || 0
    windowStart.value = data.windowStart || ''
    windowEnd.value = data.windowEnd || ''
    // 任务分布远小于窗口时，自动收紧窗口让工序条可读（仅当任务跨度 < 窗口25% 时）
    const tasks = equipment.value.flatMap(eq => eq.tasks).filter(t => t.plannedStartTime && t.plannedEndTime)
    if (!autoZoomed && tasks.length > 0) {
      const ts = tasks.map(t => parseDT(t.plannedStartTime))
      const te = tasks.map(t => parseDT(t.plannedEndTime))
      const tMin = Math.min(...ts)
      const tMax = Math.max(...te)
      if (!isNaN(tMin) && !isNaN(tMax)) {
        const buffer = 12 * 3600 * 1000
        const newStart = new Date(tMin - buffer)
        const newEnd = new Date(tMax + buffer)
        const curStart = parseDT(windowStart.value)
        const curEnd = parseDT(windowEnd.value)
        if ((tMax - tMin) * 4 < (curEnd - curStart)) {
          const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
          autoZoomed = true
          windowRange.value = [fmt(newStart), fmt(newEnd)]
          await loadBoard()
          return
        }
      }
    }
    restoreSelection()
  } catch (e) {
    console.error('加载排产看板失败:', e)
    ElMessage.error('加载排产看板失败')
  } finally {
    loading.value = false
  }
}

/** 刷新后按工单ID恢复选中 */
const restoreSelection = () => {
  if (selectedOrderKey == null) {
    selectedTask.value = null
    selectedOrderId.value = null
    return
  }
  const all = unassigned.value.slice()
  for (const eq of equipment.value) all.push(...eq.tasks)
  const found = all.find(t => t.id === selectedOrderKey)
  if (found) {
    selectedTask.value = found
    selectedOrderId.value = found.id
  } else {
    selectedTask.value = null
    selectedOrderId.value = null
    selectedOrderKey = null
  }
}

// ==================== 时间刻度 ====================
/** 兼容后端 LocalDateTime 各种序列化格式并转时间戳（本地时区） */
const parseDT = (v: string | null | undefined): number => {
  if (!v) return NaN
  const m = String(v).match(/(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/)
  if (!m) return NaN
  const [, y, mo, d, h, mi, s] = m.map(Number)
  return new Date(y, mo - 1, d, h, mi, s).getTime()
}

const wsStartMs = computed(() => parseDT(windowStart.value) || 0)
const wsEndMs = computed(() => parseDT(windowEnd.value) || 0)
const totalMs = computed(() => Math.max(1, wsEndMs.value - wsStartMs.value))
const calMap = computed(() => Object.fromEntries(calendar.value.map(c => [c.workDate, c.workday])))

const cellShaded = (fromMs: number, toMs: number) => {
  const day = new Date(fromMs)
  const dateKey = `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`
  if (granularity.value === 'day') {
    if (dateKey in calMap.value) return !calMap.value[dateKey]
    return day.getDay() === 0 || day.getDay() === 6
  }
  const wins = dayWorkingWindows(day)
  return !wins.some(([s, e]) => s < toMs && e > fromMs)
}

const dayWorkingWindows = (day: Date): Array<[number, number]> => {
  const wins: Array<[number, number]> = []
  for (const s of shifts.value) {
    if (!s.startTime || !s.endTime) continue
    const [sh, sm] = s.startTime.split(':').map(Number)
    const [eh, em] = s.endTime.split(':').map(Number)
    const start = new Date(day)
    start.setHours(sh, sm, 0, 0)
    const end = new Date(day)
    end.setHours(eh, em, 0, 0)
    if (end.getTime() <= start.getTime()) {
      const midnight = new Date(day)
      midnight.setDate(midnight.getDate() + 1)
      midnight.setHours(0, 0, 0, 0)
      wins.push([start.getTime(), midnight.getTime()])
      const next = new Date(midnight)
      next.setHours(eh, em, 0, 0)
      wins.push([midnight.getTime(), next.getTime()])
    } else {
      wins.push([start.getTime(), end.getTime()])
    }
  }
  return wins
}

const headCells = computed(() => {
  const cells: { key: string; label: string; shaded: boolean; widthPx: number }[] = []
  const s = new Date(wsStartMs.value)
  const e = new Date(wsEndMs.value)
  if (granularity.value === 'day') {
    const cur = new Date(s)
    cur.setHours(0, 0, 0, 0)
    while (cur.getTime() <= e.getTime()) {
      const from = cur.getTime()
      const to = cur.getTime() + 24 * 3600 * 1000
      cells.push({ key: String(from), label: `${cur.getMonth() + 1}/${cur.getDate()}`, shaded: cellShaded(from, to), widthPx: DAY_PX.value })
      cur.setDate(cur.getDate() + 1)
    }
  } else {
    const cur = new Date(s)
    cur.setHours(0, 0, 0, 0)
    while (cur.getTime() <= e.getTime()) {
      const from = cur.getTime()
      const to = from + 6 * 3600 * 1000
      const label = `${cur.getMonth() + 1}/${cur.getDate()} ${String(cur.getHours()).padStart(2, '0')}:00`
      cells.push({ key: String(from), label, shaded: cellShaded(from, to), widthPx: DAY_PX.value / 4 })
      cur.setTime(to)
    }
  }
  return cells
})

/** 上级时间刻度：按月份聚合 */
const monthCells = computed(() => {
  const cells: { key: string; label: string; widthPx: number }[] = []
  const s = new Date(wsStartMs.value)
  const e = new Date(wsEndMs.value)
  let cur = new Date(s.getFullYear(), s.getMonth(), 1)
  while (cur.getTime() <= e.getTime()) {
    const year = cur.getFullYear()
    const month = cur.getMonth()
    const next = new Date(year, month + 1, 1)
    // 与窗口交集的天数
    const winStart = new Date(Math.max(s.getTime(), cur.getTime()))
    const winEnd = new Date(Math.min(e.getTime(), next.getTime() - 1))
    const days = Math.max(1, Math.round((winEnd.getTime() - winStart.getTime()) / 86400000) + 1)
    cells.push({ key: `${year}-${month}`, label: `${year}年${month + 1}月`, widthPx: days * DAY_PX.value })
    cur = next
  }
  return cells
})

/** 缩放控制 */
const zoomIn = () => { zoomLevel.value = Math.min(ZOOM_MAX, +(zoomLevel.value + 0.25).toFixed(2)) }
const zoomOut = () => { zoomLevel.value = Math.max(ZOOM_MIN, +(zoomLevel.value - 0.25).toFixed(2)) }
const zoomReset = () => { zoomLevel.value = 1 }

const cellPct = computed(() => (headCells.value.length ? 100 / headCells.value.length : 100))

// ==================== 工单泳道 ====================
const orderLanes = (eq: EqGroup) => {
  const map = new Map<number, { lane: any; steps: Task[] }>()
  for (const t of eq.tasks || []) {
    if (!map.has(t.id)) {
      map.set(t.id, {
        lane: {
          key: eq.id + '-' + t.id,
          id: t.id,
          orderNo: t.orderNo,
          priority: t.priority,
          status: t.scheduleStatus,
          planStatus: t.planStatus
        },
        steps: []
      })
    }
    map.get(t.id).steps.push(t)
  }
  const arr = Array.from(map.values())
  arr.sort((a, b) => {
    const as = a.steps[0]?.plannedStartTime || ''
    const bs = b.steps[0]?.plannedStartTime || ''
    return as < bs ? -1 : as > bs ? 1 : 0
  })
  // 扁平化：lane 字段与 steps 同级，供模板直接使用
  return arr.map(x => ({ ...x.lane, steps: x.steps }))
}

const laneColor = (id: number) => {
  let h = 0
  const s = String(id ?? 0)
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 360
  return `hsl(${h}, 60%, 48%)`
}

const barColor = (t: Task) => {
  // 基于工单ID生成稳定色相，饱和度/亮度适配主题（CSS变量不可用于hsl的calc内，用固定但主题友好的值）
  const hue = (() => {
    let h = 0
    const s = String(t.id ?? 0)
    for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) % 360
    return h
  })()
  const base = `hsl(${hue}, 65%, 52%)`
  switch (t.planStatus) {
    case 'COMPLETED': return `hsl(${hue}, 35%, 60%)`
    case 'DELAYED': return `hsl(${hue}, 70%, 42%)`
    case 'RUNNING': return `hsl(${hue}, 72%, 48%)`
    case 'READY': return base
    default: return `hsl(${hue}, 45%, 55%)`
  }
}

/** bar 显示文字（含状态标记），用于 tooltip */
const barText = (t: Task) => {
  const step = t.stepNo ? `[${t.stepName}] ` : ''
  const main = `${t.orderNo}·${t.productName}`
  const extra = t.planStatus === 'RUNNING' ? ` (${t.progress}%)` : t.bottleneck ? ' ⚡' : ''
  return step + main + extra
}

/** bar 内显示文字：完整信息，超长时省略产品名（最短保留工序名+单号） */
const barDisplay = (t: Task) => {
  const step = t.stepNo ? `[${t.stepName}] ` : ''
  const full = barText(t)
  return measureText(full) > 360 ? step + t.orderNo : full
}

/** 按字数估算文字宽度（11px 字体：中文≈12px，ASCII≈7px），预留左右 padding 与边框 */
const measureText = (text: string) => {
  let w = 0
  for (const ch of text) {
    w += /[\u2E80-\u9FFF\uF900-\uFAFF\uFF00-\uFFEF]/.test(ch) ? 12 : 7
  }
  return w + 14
}

const barStyle = (t: Task) => {
  if (!t.plannedStartTime || !t.plannedEndTime) return { left: '0px', width: '0px', display: 'none' }
  const s = parseDT(t.plannedStartTime)
  const e = parseDT(t.plannedEndTime)
  if (isNaN(s) || isNaN(e) || e <= wsStartMs.value || s >= wsEndMs.value) return { left: '0px', width: '0px', display: 'none' }
  const leftPx = Math.max(0, (s - wsStartMs.value) / totalMs.value) * ganttTotalPx.value
  const rightPx = Math.min(1, (e - wsStartMs.value) / totalMs.value) * ganttTotalPx.value
  const widthPx = Math.max(4, rightPx - leftPx)
  // 按字数自动调整：bar 至少容纳文字宽度
  const textW = measureText(barDisplay(t))
  const minPx = Math.min(textW, 400)
  return { left: leftPx + 'px', width: widthPx + 'px', minWidth: minPx + 'px', background: barColor(t) }
}

/** 条宽足够时才显示 resize 手柄（避免窄条手柄占满无法拖拽） */
const barResizable = (t: Task) => {
  if (!t.plannedStartTime || !t.plannedEndTime) return false
  const s = parseDT(t.plannedStartTime)
  const e = parseDT(t.plannedEndTime)
  if (isNaN(s) || isNaN(e)) return false
  return (e - s) / totalMs.value * ganttTotalPx.value >= 56
}

const barTitle = (t: Task) =>
  `${t.orderNo} · ${t.stepName || '整单'}（${t.durationMin}min）&#10;计划 ${fmtTime(t.plannedStartTime)} ~ ${fmtTime(t.plannedEndTime)}&#10;状态：${statusLabel(t.planStatus)}${t.scheduleStatus === 'FROZEN' ? '（已冻结）' : ''}${t.scheduleStatus === 'RELEASED' ? '（已下发）' : ''}`

const conflictStepIds = computed(() => new Set(conflicts.value.flatMap(c => [c.scheduleAId, c.scheduleBId] as number[])))

// ==================== 拖拽逻辑 ====================
const snapTime = (ms: number) => {
  const snapped = Math.round(ms / SNAP_MS) * SNAP_MS
  return Math.max(wsStartMs.value, Math.min(wsEndMs.value - SNAP_MS, snapped))
}

/** 计算落点幽灵的像素位置（相对 gantt-body），跟随目标行+时间 */
const ghostRect = (startMs: number, durMs: number, wsId: string, minW: number) => {
  const body = bodyRef.value
  const target = trackEls().find(el => String(el.dataset.wsId) === wsId)
  if (!body || !target) return null
  const br = body.getBoundingClientRect()
  const tr = target.getBoundingClientRect()
  const contentW = Math.max(1, tr.width - 8)
  const frac = Math.max(0, Math.min(1, (startMs - wsStartMs.value) / totalMs.value))
  const left = (tr.left - br.left) + 4 + frac * contentW
  const width = Math.max(durMs / totalMs.value * contentW, minW, 12)
  return { top: tr.top - br.top + 6, left, width }
}

const ghostStyle = computed(() => {
  const g = dragGhost.value
  return g ? { top: g.top + 'px', left: g.left + 'px', width: g.width + 'px' } : {}
})

const fmtDateTime = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const onBarDown = (e: MouseEvent, t: Task, eq: EqGroup) => {
  e.stopPropagation()
  e.preventDefault()
  // 任何状态都可点击选中查看详情；冻结/完成仅禁止拖动（在 onDragEnd 拦截）
  selectTask(t, eq)
  if (t.planStatus === 'COMPLETED') return
  const startMs = parseDT(t.plannedStartTime)
  // 锚点：鼠标按下点相对工序起点的时间偏移
  const mouseMs = mouseTimeInTrack(e, eq.id)
  const grabOffsetMs = isNaN(mouseMs) ? 0 : mouseMs - startMs
  dragState.value = { type: 'bar', task: t, originWsId: eq.id, startClientX: e.clientX, startMs, endMs: parseDT(t.plannedEndTime), grabOffsetMs }
  dragTargetWsId.value = eq.id
}

const onResizeDown = (e: MouseEvent, t: Task, eq: EqGroup, dir: 'l' | 'r') => {
  e.stopPropagation()
  e.preventDefault()
  if (t.scheduleStatus === 'FROZEN') {
    ElMessage.warning('该排产已冻结，请先解冻')
    return
  }
  const startMs = parseDT(t.plannedStartTime)
  const endMs = parseDT(t.plannedEndTime)
  dragState.value = { type: dir === 'r' ? 'resize-r' : 'resize-l', task: t, originWsId: eq.id, startClientX: e.clientX, startMs, endMs, grabOffsetMs: 0 }
}

const onCardDown = (e: MouseEvent, t: Task) => {
  e.stopPropagation()
  e.preventDefault()
  dragState.value = { type: 'pool', task: t, originWsId: null, startClientX: e.clientX, startMs: 0, endMs: 0, grabOffsetMs: 0 }
}

const trackEls = () => Array.from(document.querySelectorAll<HTMLElement>('[data-ws-id]'))

/** 计算鼠标横向位移对应的时间（毫秒），基准为 track 内容宽度 */
const deltaFromEvent = (e: MouseEvent, startClientX: number) => {
  const track = trackEls()[0]
  const w = track ? track.getBoundingClientRect().width - 8 : 1
  return (e.clientX - startClientX) / Math.max(1, w) * totalMs.value
}

/** 鼠标在指定设备行内的对应时间（毫秒，未吸附） */
const mouseTimeInTrack = (e: MouseEvent, wsId: string): number => {
  const target = trackEls().find(el => String(el.dataset.wsId) === wsId)
  if (!target) return NaN
  const r = target.getBoundingClientRect()
  const frac = Math.max(0, Math.min(1, (e.clientX - r.left - 4) / Math.max(1, r.width - 8)))
  return wsStartMs.value + frac * totalMs.value
}

/** 用坐标范围命中目标行（不依赖 elementFromPoint，避免被高z-index元素干扰） */
const hitTargetRow = (e: MouseEvent): string | null => {
  const rects = trackEls().map(el => ({ id: String(el.dataset.wsId), r: el.getBoundingClientRect() }))
  // 优先命中包含鼠标点的行（从下往上，最顶层优先）
  for (let i = rects.length - 1; i >= 0; i--) {
    const { id, r } = rects[i]
    if (e.clientY >= r.top && e.clientY <= r.bottom) return id
  }
  return null
}

/** 更新鼠标位置：目标行高亮 + 幽灵条跟随（位移 < 5px 视为点击，不触发拖拽视觉） */
const onDragMove = (e: MouseEvent) => {
  const ds = dragState.value
  if (!ds) return
  // 拖拽启动阈值：未超过视为点击选中，不显示幽灵/高亮
  if (Math.abs(e.clientX - ds.startClientX) < 5) {
    dragGhost.value = null
    return
  }
  document.body.style.cursor = 'grabbing'
  const deltaMs = deltaFromEvent(e, ds.startClientX)

  // 命中目标行（非track区域时保持上次目标，避免幽灵跳回首行）
  const hit = hitTargetRow(e)
  if (hit) {
    dragTargetWsId.value = hit
  }
  dragOverPool.value = !!poolRef.value && (() => {
    const r = poolRef.value.getBoundingClientRect()
    return e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom
  })()

  if (ds.type === 'bar') {
    const durMs = ds.endMs - ds.startMs
    const targetWs = dragTargetWsId.value ?? ds.originWsId
    const mouseMs = mouseTimeInTrack(e, targetWs)
    const newStart = isNaN(mouseMs)
      ? snapTime(ds.startMs + deltaMs)
      : snapTime(mouseMs - ds.grabOffsetMs)
    dragGhost.value = ghostRect(newStart, durMs, targetWs, 0)
  } else if (ds.type === 'pool') {
    const durMs = 480 * 60 * 1000
    if (dragTargetWsId.value !== null) {
      const tMs = snapTime(mouseTimeInTrack(e, dragTargetWsId.value))
      dragGhost.value = ghostRect(tMs, durMs, dragTargetWsId.value, 0)
    } else {
      dragGhost.value = null
    }
  } else if (ds.type === 'resize-r') {
    const newEnd = snapTime(ds.endMs + deltaMs)
    const dur = Math.max(30 * 60 * 1000, newEnd - ds.startMs)
    dragGhost.value = ghostRect(ds.startMs, dur, ds.originWsId!, 0)
  } else if (ds.type === 'resize-l') {
    const newStart = snapTime(ds.startMs + deltaMs)
    const dur = Math.max(30 * 60 * 1000, ds.endMs - newStart)
    dragGhost.value = ghostRect(newStart, dur, ds.originWsId!, 0)
  }
}

/** 乐观更新：把工序条移到新设备/新时间（不整页刷新） */
const applyMoveLocally = (t: Task, targetWsId: string, newStartMs: number, newEndMs?: number) => {
  const newStartStr = fmtDateTime(new Date(newStartMs))
  const newEndStr = newEndMs ? fmtDateTime(new Date(newEndMs)) : fmtDateTime(new Date(newStartMs + (t.durationMin || 0) * 60000))
  t.plannedStartTime = newStartStr
  t.plannedEndTime = newEndStr
  t.workstationId = targetWsId
  // 从源设备移除，加入目标设备并排序
  for (const eq of equipment.value) {
    const idx = eq.tasks.findIndex(x => x.scheduleId === t.scheduleId)
    if (idx >= 0) eq.tasks.splice(idx, 1)
  }
  const dest = equipment.value.find(eq => eq.id === targetWsId)
  if (dest) {
    dest.tasks.push(t)
    dest.tasks.sort((a, b) => (parseDT(a.plannedStartTime) || 0) - (parseDT(b.plannedStartTime) || 0))
  }
}

const onDragEnd = async (e: MouseEvent) => {
  const ds = dragState.value
  if (!ds) return
  dragState.value = null
  dragGhost.value = null
  document.body.style.cursor = ''
  // 纯点击（无位移）只做选中，不发请求不刷新
  if (Math.abs(e.clientX - ds.startClientX) < 4) return
  const t = ds.task
  try {
    if (ds.type === 'bar') {
      if (dragOverPool.value) {
        await doUnassign(t.id)
        return
      }
      if (t.scheduleStatus === 'FROZEN') {
        ElMessage.warning('该排产已冻结，请先解冻')
        return
      }
      const deltaMs = deltaFromEvent(e, ds.startClientX)
      const targetWs = dragTargetWsId.value ?? ds.originWsId
      const mouseMs = mouseTimeInTrack(e, targetWs)
      const newStartMs = isNaN(mouseMs)
        ? snapTime(ds.startMs + deltaMs)
        : snapTime(mouseMs - ds.grabOffsetMs)
      if (targetWs === ds.originWsId && newStartMs === ds.startMs) return
      const newStart = fmtDateTime(new Date(newStartMs))
      // 乐观更新：先本地移动，失败再回滚
      applyMoveLocally(t, targetWs!, newStartMs)
      try {
        await movePlanningTask({
          scheduleId: t.scheduleId,
          workOrderId: t.id,
          targetWorkstationId: targetWs!,
          newStart
        })
        ElMessage.success(targetWs === ds.originWsId ? '已调整时间' : '已切换设备')
      } catch (err) {
        await loadBoard()
        ElMessage.error(String(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '保存失败'))
      }
    } else if (ds.type === 'pool') {
      if (dragTargetWsId.value !== null) {
        const newStartMs = snapTime(mouseTimeInTrack(e, dragTargetWsId.value))
        const newStart = fmtDateTime(new Date(newStartMs))
        await movePlanningTask({ workOrderId: t.id, targetWorkstationId: dragTargetWsId.value, newStart })
        ElMessage.success('排产成功')
        await loadBoard()
      }
    } else if (ds.type === 'resize-r') {
      const deltaMs = deltaFromEvent(e, ds.startClientX)
      const newEndMs = snapTime(ds.endMs + deltaMs)
      if (newEndMs === ds.endMs) return
      const newEnd = fmtDateTime(new Date(newEndMs))
      applyMoveLocally(t, ds.originWsId!, ds.startMs, newEndMs)
      try {
        await movePlanningTask({
          scheduleId: t.scheduleId,
          workOrderId: t.id,
          targetWorkstationId: ds.originWsId!,
          newEnd
        })
        ElMessage.success('已拉伸工时')
      } catch (err) {
        await loadBoard()
        ElMessage.error(String(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '保存失败'))
      }
    } else if (ds.type === 'resize-l') {
      const deltaMs = deltaFromEvent(e, ds.startClientX)
      const newStartMs = snapTime(ds.startMs + deltaMs)
      if (newStartMs === ds.startMs) return
      const newStart = fmtDateTime(new Date(newStartMs))
      applyMoveLocally(t, ds.originWsId!, newStartMs, ds.endMs)
      try {
        await movePlanningTask({
          scheduleId: t.scheduleId,
          workOrderId: t.id,
          targetWorkstationId: ds.originWsId!,
          newStart,
          newEnd: fmtDateTime(new Date(ds.endMs))
        })
        ElMessage.success('已调整起点')
      } catch (err) {
        await loadBoard()
        ElMessage.error(String(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '保存失败'))
      }
    }
  } catch (err: any) {
    const msg = String(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '操作失败')
    if (msg.includes('不存在')) {
      ElMessage.warning('排产数据已变更，已自动刷新最新看板，请重试')
    } else {
      ElMessage.error(msg)
    }
    await loadBoard()
  } finally {
    dragTargetWsId.value = null
    dragOverPool.value = false
  }
}

/** Esc 取消拖拽 */
const onKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && dragState.value) {
    onDragCancel()
  }
}

const onDragCancel = () => {
  if (dragState.value) {
    dragState.value = null
    dragGhost.value = null
    dragTargetWsId.value = null
    dragOverPool.value = false
    document.body.style.cursor = ''
  }
}

// ==================== 操作 ====================
const doAutoPlan = async (onlyPending: boolean) => {
  if (!onlyPending) {
    try {
      await ElMessageBox.confirm('全部重排将清空所有PLANNED状态的排产并按优先级+交期重新排程，已冻结/已下发的保留。继续？', '确认重排', { type: 'warning' })
    } catch {
      return
    }
  }
  loading.value = true
  try {
    const res = await autoPlanOrders({ onlyPending })
    ElMessage.success(`自动排程完成，生成 ${res?.data ?? 0} 条工序排产`)
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('自动排程失败: ' + (err?.response?.data?.msg || err?.message))
  } finally {
    loading.value = false
  }
}

const doUndo = async () => {
  try {
    await undoPlanning()
    ElMessage.success('已撤销上一次变更')
    await loadBoard()
  } catch (err: any) {
    ElMessage.error(String(err?.response?.data?.msg || err?.message || '撤销失败'))
  }
}

const doFreeze = async (t: Task, whole: boolean) => {
  try {
    const data = whole ? { workOrderId: t.id, scope: 'order' } : { scheduleIds: [t.scheduleId], scope: 'step' }
    const res = await freezePlanning(data)
    ElMessage.success(`已冻结 ${res?.data ?? 0} 条工序`)
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('冻结失败: ' + (err?.response?.data?.msg || err?.message))
  }
}

const doUnfreeze = async (t: Task) => {
  try {
    const res = await unfreezePlanning({ workOrderId: t.id, scope: 'order' })
    ElMessage.success(`已解冻 ${res?.data ?? 0} 条工序`)
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('解冻失败: ' + (err?.response?.data?.msg || err?.message))
  }
}

const doFreezeWs = async (eq: EqGroup, freeze: boolean) => {
  try {
    const fn = freeze ? freezePlanning : unfreezePlanning
    const res = await fn({ workstationId: eq.id, scope: 'ws' })
    ElMessage.success(`${freeze ? '冻结' : '解冻'}完成 ${res?.data ?? 0} 条`)
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('操作失败: ' + (err?.response?.data?.msg || err?.message))
  }
}

const doRelease = async (workOrderId: number) => {
  try {
    const res = await releasePlanning(workOrderId)
    ElMessage.success(`已下发 ${res?.data ?? 0} 条工序排产`)
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('下发失败: ' + (err?.response?.data?.msg || err?.message))
  }
}

const doUnassign = async (workOrderId: number) => {
  try {
    await unassignPlanningOrder(workOrderId)
    ElMessage.success('已移回待排产池')
    selectedTask.value = null
    await loadBoard()
  } catch (err: any) {
    ElMessage.error(String(err?.response?.data?.msg || err?.message || '取消排产失败'))
  }
}

const openBarCtx = (e: MouseEvent, t: Task, eq: EqGroup) => {
  selectTask(t, eq)
  ctxMenu.value = { x: e.clientX, y: e.clientY, task: t, eq: null }
}

const openRowCtx = (e: MouseEvent, eq: EqGroup | null, _task: Task | null) => {
  ctxMenu.value = { x: e.clientX, y: e.clientY, task: null, eq }
}

const openAdjustDialog = (t: Task, whole: boolean) => {
  adjustTask.value = t
  adjustWhole.value = whole
  adjustForm.value = {
    targetWorkstationId: t.workstationId ?? equipment.value[0]?.id ?? null,
    newStart: t.plannedStartTime ? t.plannedStartTime.replace('T', ' ').slice(0, 19) : '',
    newEnd: t.plannedEndTime ? t.plannedEndTime.replace('T', ' ').slice(0, 19) : '',
    force: false
  }
  adjustVisible.value = true
}

const submitAdjust = async () => {
  const t = adjustTask.value
  if (!t || !adjustForm.value.targetWorkstationId) {
    ElMessage.warning('请选择目标设备')
    return
  }
  try {
    await movePlanningTask({
      scheduleId: adjustWhole.value ? undefined : t.scheduleId,
      workOrderId: t.id,
      targetWorkstationId: adjustForm.value.targetWorkstationId,
      newStart: adjustForm.value.newStart || undefined,
      newEnd: adjustWhole.value ? undefined : (adjustForm.value.newEnd || undefined),
      force: adjustForm.value.force
    })
    ElMessage.success('保存成功')
    adjustVisible.value = false
    await loadBoard()
  } catch (err: any) {
    const msg = err?.response?.data?.msg || err?.response?.data?.message || err?.message || '保存失败'
    ElMessage.error(String(msg))
  }
}

// ==================== 展示工具 ====================
const equipmentOf = (t: Task) => equipment.value.find(eq => eq.id === t.workstationId) || null

const orderSteps = (workOrderId: number) => {
  const steps: Task[] = []
  for (const eq of equipment.value) {
    for (const t of eq.tasks) {
      if (t.id === workOrderId) steps.push(t)
    }
  }
  return steps.sort((a, b) => (a.plannedStartTime || '') < (b.plannedStartTime || '') ? -1 : 1)
}

const statClass = (s: string) => {
  const map: Record<string, string> = { PENDING: 'pending', READY: 'ready', RUNNING: 'running', COMPLETED: 'completed', DELAYED: 'delayed' }
  return map[s] || 'pending'
}

const statusLabel = (s: string) => {
  const map: Record<string, string> = { PENDING: '待排产', READY: '已排产', RUNNING: '运行中', COMPLETED: '已完成', DELAYED: '延误' }
  return map[s] || s
}

const loadClass = (rate: number) => (rate >= 85 ? 'load-danger' : rate >= 60 ? 'load-warn' : 'load-safe')

const priType = (p?: string) => (p === 'HIGH' ? 'danger' : p === 'LOW' ? 'info' : 'warning')
const priLabel = (p?: string) => ({ HIGH: '高', MEDIUM: '中', LOW: '低' }[p || 'MEDIUM'] || p || '-')

const fmtTime = (v?: string) => (v ? v.replace('T', ' ').slice(0, 19) : '-')
const fmtShort = (v?: string) => (v ? v.replace('T', ' ').slice(5, 16) : '-')
const fmtWorkHours = (min: number) => {
  const d = Math.floor(min / 480)
  const h = Math.round((min % 480) / 60)
  return d > 0 ? `${d}天${h}小时` : `${h}小时`
}

const shiftText = computed(() =>
  shifts.value.map(s => `${s.shiftName} ${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)}`).join('，') || '-'
)

const logActionLabel = (a: string) => {
  const map: Record<string, string> = {
    AUTO_PLAN: '自动排程', REPLAN: '全部重排', MOVE: '调整', RESIZE: '拉伸', ASSIGN: '排产',
    UNASSIGN: '取消排产', FREEZE: '冻结', UNFREEZE: '解冻', RELEASE: '下发', UNDO: '撤销', HOLD: '挂起'
  }
  return map[a] || a
}

const logClass = (a: string) => (a === 'FREEZE' || a === 'RELEASE' ? 'log-warn' : a === 'UNDO' || a === 'REPLAN' ? 'log-info' : '')

const selectTask = (t: Task, eq: EqGroup | null) => {
  selectedTask.value = t
  selectedOrderId.value = t.id
  selectedOrderKey = t.id
}

/** 点击泳道头选中该工单（取第一道工序） */
const selectLane = (lane: any) => {
  const first = lane.steps?.[0]
  if (first) {
    selectTask(first, equipment.value.find(eq => eq.tasks.some(x => x.id === lane.id)) || null)
  }
}

const clearSelection = () => {
  selectedTask.value = null
  selectedOrderId.value = null
  selectedOrderKey = null
}

// ==================== 轮询 ====================
const setupPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (autoRefresh.value) {
    pollTimer = setInterval(() => {
      if (!dragState.value && !loading.value) loadBoard()
    }, 30000)
  }
}

/** 阻止浏览器原生拖拽（复制/文本拖放） */
const preventNativeDrag = (e: Event) => {
  if (dragState.value) e.preventDefault()
}

onMounted(() => {
  const today = new Date()
  const start = new Date(today)
  start.setDate(today.getDate() - 7)
  const end = new Date(today)
  end.setDate(today.getDate() + 21)
  const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  windowRange.value = [fmt(start), fmt(end)]
  loadBoard()
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('keydown', onKeyDown)
  document.addEventListener('dragstart', preventNativeDrag)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('keydown', onKeyDown)
  document.removeEventListener('dragstart', preventNativeDrag)
})

watch(autoRefresh, setupPolling)
</script>

<style scoped lang="scss">
.planning-page {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 1200px;

  /* 看板主体禁用文本选中 */
  .gantt-body,
  .gantt-head,
  .action-bar,
  .ctx-menu {
    user-select: none;
    -webkit-user-select: none;
  }

  /* ===== 顶部工具栏 ===== */
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px 12px;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-lg);
    padding: 10px 16px;
    box-shadow: var(--shadow-sm);
    transition: background var(--transition-normal), border-color var(--transition-normal);

    .header-left {
      display: flex;
      align-items: baseline;
      gap: 12px;
      h2 {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;
        &::before {
          content: '';
          width: 4px;
          height: 16px;
          border-radius: 2px;
          background: var(--gradient-primary);
        }
      }
      .sub-title { font-size: 12px; color: var(--text-tertiary); }
    }
    .header-right {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      .legend-group {
        display: flex;
        gap: 8px;
        margin-right: 4px;
        .legend-item {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: var(--text-secondary);
          .legend-dot { width: 8px; height: 8px; border-radius: 3px; }
        }
      }
      .header-divider {
        width: 1px;
        height: 18px;
        background: var(--border-color);
      }
    }
  }

  /* ===== 操作栏 ===== */
  .action-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-lg);
    padding: 8px 14px;
    box-shadow: var(--shadow-sm);
    transition: background var(--transition-normal), border-color var(--transition-normal);

    .action-spacer { flex: 1; }
    .zoom-group {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 2px 4px;
      background: var(--bg-hover);
      border-radius: var(--radius-sm);
      .zoom-text {
        font-size: 11px;
        font-weight: 600;
        color: var(--text-secondary);
        min-width: 42px;
        text-align: center;
        font-family: 'Consolas', monospace;
      }
      :deep(.el-button) { padding: 4px 6px; margin: 0 !important; }
    }
    .work-hours {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      color: var(--text-secondary);
      padding: 4px 12px;
      background: var(--bg-hover);
      border-radius: var(--radius-sm);
      white-space: nowrap;
    }
    .conflict-badge { margin-left: 8px; }
  }

  /* ===== 布局 ===== */
  .board-layout {
    display: flex;
    gap: 14px;
    align-items: stretch;

    .gantt-area {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 240px);
      min-height: 380px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      transition: background var(--transition-normal), border-color var(--transition-normal);
    }

    .side-panel {
      flex: 0 0 340px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      padding: 0 14px 14px;
      height: calc(100vh - 240px);
      min-height: 380px;
      display: flex;
      flex-direction: column;
      transition: background var(--transition-normal), border-color var(--transition-normal);

      :deep(.el-tabs) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
      :deep(.el-tabs__header) { margin-bottom: 8px; }
      :deep(.el-tabs__content) { flex: 1; overflow-y: auto; min-height: 0; }
      :deep(.el-tabs__item) { color: var(--text-secondary); }
      :deep(.el-tabs__item.is-active) { color: var(--accent); }
      :deep(.el-tabs__active-bar) { background: var(--gradient-primary); height: 3px; border-radius: 2px; }

      .detail-wrap {
        .detail-header {
          padding: 14px 16px;
          color: #fff;
          border-radius: var(--radius-md);
          background: var(--gradient-primary);
          position: relative;
          overflow: hidden;
          &.hd-st-pending { background: linear-gradient(135deg, #6b7280, #9ca3af); }
          &.hd-st-ready { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
          &.hd-st-running { background: linear-gradient(135deg, #10b981, #34d399); }
          &.hd-st-completed { background: linear-gradient(135deg, #64748b, #94a3b8); }
          &.hd-st-delayed { background: linear-gradient(135deg, #ef4444, #f87171); }

          .detail-title-row {
            display: flex;
            gap: 6px;
            .detail-status-tag {
              font-size: 11px;
              padding: 2px 10px;
              border-radius: 10px;
              background: rgba(255, 255, 255, 0.22);
              
            }
          }
          .detail-order { font-size: 16px; font-weight: 700; margin-top: 8px; letter-spacing: 0.5px; }
          .detail-product { font-size: 12px; opacity: 0.92; }
        }

        .detail-body { padding: 14px 2px; }

        .step-list-title {
          font-size: 12px;
          font-weight: 600;
          color: var(--text-tertiary);
          margin: 14px 0 8px;
          display: flex;
          align-items: center;
          gap: 6px;
          &::before {
            content: '';
            width: 3px;
            height: 12px;
            border-radius: 2px;
            background: var(--gradient-primary);
          }
        }
        .step-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 8px 10px;
          border-radius: var(--radius-sm);
          cursor: pointer;
          font-size: 12px;
          border: 1px solid var(--border-light);
          background: var(--bg-hover);
          margin-bottom: 6px;
          transition: all var(--transition-fast);
          &:hover { border-color: var(--accent); background: var(--accent-light); }
          .step-dot { width: 8px; height: 8px; border-radius: 50%; flex: none; }
          .step-name { font-weight: 600; white-space: nowrap; color: var(--text-primary); }
          .step-dev { color: var(--text-tertiary); margin-left: auto; }
          .step-time { color: var(--text-tertiary); font-size: 11px; }
        }

        .detail-actions {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          margin-top: 14px;
        }
      }

      .log-list {
        .log-item {
          display: flex;
          flex-direction: column;
          gap: 3px;
          padding: 10px 6px;
          border-bottom: 1px dashed var(--border-light);
          font-size: 12px;
          .log-action {
            font-weight: 600;
            width: fit-content;
            padding: 1px 10px;
            border-radius: 10px;
            background: var(--bg-hover);
            color: var(--text-secondary);
            &.log-warn { color: var(--warning); background: var(--warning-light); }
            &.log-info { color: var(--accent); background: var(--accent-light); }
          }
          .log-desc { color: var(--text-secondary); }
          .log-time { color: var(--text-tertiary); font-size: 11px; }
        }
      }

      .conflict-list {
        .conflict-item {
          display: flex;
          gap: 8px;
          padding: 10px 6px;
          border-bottom: 1px dashed var(--border-light);
          font-size: 12px;
          .conflict-body {
            .conflict-title { color: var(--danger); font-weight: 600; }
            .conflict-time { color: var(--text-tertiary); font-size: 11px; margin-top: 3px; }
          }
        }
      }
    }
  }

  /* ===== 表头时间轴 ===== */
  .gantt-head {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    min-width: max-content;
    border-bottom: 1px solid var(--border-color);
    background: var(--bg-card);

    .head-corner {
      position: sticky;
      left: 0;
      z-index: 2;
      flex: 0 0 220px;
      text-align: center;
      font-weight: 600;
      font-size: 12px;
      letter-spacing: 1px;
      color: var(--text-tertiary);
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-card);
      border-right: 1px solid var(--border-color);
    }

    .gantt-timeline-wrap {
      flex: 0 0 auto;
      display: flex;
      flex-direction: column;
    }

    .gantt-timeline {
      display: flex;
      &.month-row { border-bottom: 1px solid var(--border-light); }
    }

    .gantt-month-cell {
      flex: none;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 22px;
      font-size: 11px;
      font-weight: 600;
      color: var(--text-primary);
      background: var(--bg-hover);
      border-left: 1px solid var(--border-light);
      white-space: nowrap;
      overflow: hidden;
    }

    .gantt-day-cell {
      display: flex;
      align-items: center;
      justify-content: center;
      border-left: 1px solid var(--border-light);
      height: 24px;
      font-size: 11px;
      color: var(--text-secondary);
      white-space: nowrap;
      overflow: hidden;
      &.weekend { background: var(--warning-light); }
      .gantt-day-label {
        padding: 1px 6px;
        border-radius: 6px;
        background: var(--bg-hover);
      }
    }
  }

  /* ===== 甘特主体 ===== */
  .gantt-body {
    position: relative;
    flex: 1;
    overflow: auto;
    min-height: 0;
    .machine-row {
      display: flex;
      border-bottom: 1px solid var(--border-light);
      min-width: max-content;

      &:hover { background: var(--bg-hover); }

      .gantt-row-label {
        position: sticky;
        left: 0;
        z-index: 4;
        flex: 0 0 220px;
        padding: 12px 14px;
        display: flex;
        flex-direction: column;
        justify-content: center;
        gap: 6px;
        border-right: 1px solid var(--border-light);
        background: var(--bg-card);
        cursor: context-menu;
        transition: background var(--transition-fast);

        .machine-cell {
          .machine-top {
            display: flex;
            align-items: center;
            gap: 8px;
            .machine-name { font-weight: 600; font-size: 13px; color: var(--text-primary); }
            .machine-status {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              font-size: 10px;
              font-weight: 500;
              color: var(--text-tertiary);
              .status-dot {
                width: 7px;
                height: 7px;
                border-radius: 50%;
                flex: none;
              }
              &.IDLE .status-dot { background: var(--success); }
              &.RUNNING .status-dot { background: var(--accent); }
              &.STOPPED .status-dot { background: var(--danger); }
            }
          }

          .machine-meta {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-top: 4px;
            .machine-code {
              font-size: 11px;
              color: var(--text-tertiary);
              font-family: 'Consolas', monospace;
            }
            .bottleneck-tag {
              font-size: 10px;
              font-weight: 500;
              color: var(--danger);
              background: var(--danger-light);
              border-radius: 4px;
              padding: 0 5px;
            }
            .load-text {
              margin-left: auto;
              font-size: 12px;
              font-weight: 600;
              font-family: 'Consolas', monospace;
              &.load-safe { color: var(--success); }
              &.load-warn { color: var(--warning); }
              &.load-danger { color: var(--danger); }
            }
          }

          .load-bar {
            position: relative;
            height: 4px;
            border-radius: 2px;
            background: var(--bg-hover);
            margin-top: 6px;
            .load-fill {
              height: 100%;
              border-radius: 2px;
              transition: width 0.5s ease;
              &.load-safe { background: var(--success); }
              &.load-warn { background: var(--warning); }
              &.load-danger { background: var(--danger); }
            }
          }
        }
      }

      .gantt-track {
        flex: 0 0 auto;
        position: relative;
        min-height: 46px;
        padding: 6px 4px;

        &.drop-target {
          outline: 2px dashed var(--accent);
          outline-offset: -2px;
          background: var(--accent-light);
        }

        .gantt-gridline {
          position: absolute;
          inset: 0;
          display: flex;
          pointer-events: none;
          .grid-col {
            border-left: 1px dashed var(--border-light);
            &.weekend { background: var(--warning-light); opacity: 0.5; }
          }
        }

        .order-lane {
          position: relative;
          padding-top: 20px;
          border-bottom: 1px dashed var(--border-light);

          &.lane-selected {
            background: var(--accent-light);
            border-radius: var(--radius-sm);
          }

          .order-lane-head {
            position: absolute;
            left: 4px;
            top: 3px;
            z-index: 5;
            display: flex;
            align-items: center;
            gap: 5px;
            font-size: 11px;
            color: var(--text-secondary);
            background: transparent;
            border-radius: var(--radius-sm);
            padding: 0 4px;
            max-width: 60%;
            overflow: hidden;
            .lane-dot { width: 7px; height: 7px; border-radius: 50%; flex: none; }
            .lane-no { font-weight: 700; color: var(--text-primary); font-size: 11px; }
            .lane-badge {
              font-size: 10px;
              padding: 0 6px;
              border-radius: 8px;
              color: #fff;
              font-weight: 500;
              &.frozen { background: var(--warning); }
              &.released { background: #8b5cf6; }
              &.delayed { background: var(--danger); }
            }
          }

          .lane-bars {
            position: relative;
            height: 46px;
          }
        }

        .track-empty {
          position: absolute;
          left: 12px;
          top: 50%;
          transform: translateY(-50%);
          font-size: 12px;
          color: var(--text-tertiary);
          pointer-events: none;
        }

        .pending-list {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
          min-height: 36px;
          padding: 4px;
          position: relative;
          z-index: 2;

          .pending-card {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 5px 10px;
            border-radius: var(--radius-sm);
            border: 1px solid var(--border-color);
            background: var(--bg-card);
            cursor: grab;
            font-size: 11px;
            box-shadow: var(--shadow-sm);
            transition: all var(--transition-fast);
            white-space: nowrap;

            &.card-selected {
              outline: 2px solid var(--accent);
              border-color: var(--accent);
              background: var(--accent-light);
            }
            &:hover { border-color: var(--accent); transform: translateY(-1px); }

            .pc-no { color: var(--text-secondary); font-weight: 600; }
            .pc-name { color: var(--text-primary); }
            .pc-qty { color: var(--text-tertiary); }

            &.pri-high { border-left: 3px solid var(--danger); }
            &.pri-medium { border-left: 3px solid var(--warning); }
            &.pri-low { border-left: 3px solid var(--text-tertiary); }

            &:active { cursor: grabbing; }
          }

          .pool-empty { font-size: 12px; color: var(--text-tertiary); padding: 8px; }
        }

        .gantt-bar {
          position: absolute;
          top: 10px;
          height: 26px;
          border-radius: 6px;
          padding: 0 8px;
          display: flex;
          align-items: center;
          overflow: hidden;
          cursor: grab;
          font-size: 11px;
          color: #fff;
          white-space: nowrap;
          z-index: 3;
          transition: border-color 0.15s ease, background 0.15s ease;
          border: 1px solid rgba(255, 255, 255, 0.18);

          .bar-label { overflow: hidden; text-overflow: ellipsis; flex: 1; min-width: 0; white-space: nowrap; }

          &:hover { z-index: 4; }

          &.bar-selected {
            outline: 2px solid var(--accent);
            outline-offset: 1px;
            box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.35);
          }
          &.bar-conflict { border: 2px dashed var(--danger); }

          &.st-completed { opacity: 0.6; cursor: default; filter: saturate(0.7); }
          &.st-delayed { border-color: var(--warning); }
          &.st-running { box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.4); }
          &.st-ready { animation: none; }

          .resize-h {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 8px;
            cursor: ew-resize;
            right: 0;
            border-radius: 0 7px 7px 0;
            background: rgba(255, 255, 255, 0.2);
            &:hover { background: rgba(255, 255, 255, 0.5); }
            &.left {
              right: auto;
              left: 0;
              border-radius: 7px 0 0 7px;
            }
          }
        }

        .drag-ghost {
          position: absolute;
          top: 0;
          height: 26px;
          border-radius: 6px;
          background: var(--accent-light);
          border: 2px dashed var(--accent);
          z-index: 100;
          pointer-events: none;
          box-sizing: border-box;
        }
      }
    }

    .pending-row {
      .gantt-track { min-height: 64px; }
    }
  }

  /* ===== 右键菜单 ===== */
  .ctx-menu {
    position: fixed;
    z-index: 3000;
    min-width: 190px;
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-lg);
    padding: 6px;
    font-size: 13px;
    animation: ctxIn 0.15s ease;

    .ctx-title {
      font-weight: 700;
      padding: 6px 10px;
      color: var(--text-primary);
      border-bottom: 1px solid var(--border-light);
      margin-bottom: 4px;
    }
    .ctx-item {
      padding: 7px 10px;
      border-radius: var(--radius-sm);
      cursor: pointer;
      color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { background: var(--accent-light); color: var(--accent); }
      &.danger { color: var(--danger); }
      &.danger:hover { background: var(--danger-light); }
    }
  }

  .force-tip { font-size: 11px; color: var(--text-tertiary); margin-left: 8px; }
}

@keyframes ctxIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>