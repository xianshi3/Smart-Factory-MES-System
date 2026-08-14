<template>
  <div v-loading="loading" class="page-wrapper planning-page">
    <!-- 顶部工具栏：标题 + 图例 + 操作 -->
    <div class="page-header">
      <div class="header-left">
        <h2>生产调度看板</h2>
        <span class="sub-title">工序级排产 · 自动排程 · 冲突检测 · 冻结下发</span>
      </div>
      <div class="header-right">
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

    <!-- 操作栏：图例 + 排程操作 + 缩放 + 统计 -->
    <div class="action-bar">
      <div class="legend-group">
        <span v-for="lg in legendItems" :key="lg.key" class="legend-item">
          <i class="legend-dot" :style="{ background: lg.color }"></i>{{ lg.label }}
        </span>
        <span class="legend-item"><i class="legend-dot" style="background:#e6a23c"></i>冻结</span>
        <span class="legend-item"><i class="legend-dot" style="background:#722ed1"></i>已下发</span>
      </div>
      <span class="bar-divider"></span>
      <el-button v-permission="'planning:edit'" type="success" size="small" plain @click="doAutoPlan(true)">
        <el-icon><MagicStick /></el-icon>&nbsp;自动排程
      </el-button>
      <el-button v-permission="'planning:edit'" type="warning" size="small" plain @click="doAutoPlan(false)">
        <el-icon><RefreshRight /></el-icon>&nbsp;全部重排
      </el-button>
      <el-button v-permission="'planning:edit'" size="small" plain @click="doUndo">
        <el-icon><Back /></el-icon>&nbsp;撤销
      </el-button>
      <span class="action-spacer"></span>
      <div class="zoom-group">
        <el-button size="small" plain :icon="ZoomOut" @click="zoomOut" />
        <span class="zoom-text">{{ (zoomLevel * 100).toFixed(0) }}%</span>
        <el-button size="small" plain :icon="ZoomIn" @click="zoomIn" />
        <el-button size="small" plain :icon="Aim" title="重置缩放" @click="zoomReset" />
      </div>
      <span class="work-hours">
        <el-icon><Clock /></el-icon>
        窗口工时 {{ fmtWorkHours(workMinutes) }} · {{ shiftText }}
      </span>
    </div>

    <!-- KPI 指标卡 -->
    <div class="kpi-row">
      <div v-for="k in kpiItems" :key="k.key" class="kpi-card">
        <div class="kpi-icon" :style="{ background: k.bg, color: k.color }">
          <el-icon><component :is="k.icon" /></el-icon>
        </div>
        <div class="kpi-info">
          <span class="kpi-value">{{ k.value }}</span>
          <span class="kpi-label">{{ k.label }}</span>
          <span class="kpi-sub">{{ k.sub }}</span>
        </div>
      </div>
    </div>

    <div class="board-layout">
      <!-- 甘特图主体 -->
      <div class="gantt-area">
        <div ref="bodyRef" class="gantt-body">
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
                <div v-if="nowFrac >= 0" class="now-head-tag" :style="{ left: (4 + nowFrac * ganttTotalPx) + 'px' }">现在</div>
              </div>
            </div>
          </div>

          <!-- 拖拽落点幽灵（fixed 视口定位，跟随鼠标所在行与时间） -->
          <div v-if="dragGhost" class="drag-ghost" :class="{ 'ghost-conflict': ghostConflicting }" :style="ghostStyle"></div>
          <!-- 拖拽信息跟随卡片（固定宽度，完整显示工单/时间/目标） -->
          <div v-if="dragState" class="drag-tip-card" :class="{ 'tip-conflict': ghostConflicting }" :style="dragTipStyle">
            <div class="tip-card-title">{{ dragTipTitle }}</div>
            <div class="tip-card-time">
              <el-icon :size="12"><Clock /></el-icon>
              <span>{{ dragTipTime }}</span>
            </div>
            <div class="tip-card-status">{{ dragTipStatus }} · 目标：{{ dragTipWsName }}</div>
            <div v-if="dragHitBound" class="tip-card-bound">已抵相邻工序边界，无法继续</div>
          </div>
          <!-- 待排产池 -->
          <div class="machine-row pending-row">
            <div class="gantt-row-label" @contextmenu.prevent="openRowCtx($event, null, null)">
              <div class="machine-cell">
                <span class="machine-name">待排产池</span>
                <span class="machine-count">{{ unassigned.length }} 单</span>
              </div>
            </div>
            <div ref="poolRef" class="gantt-track pool-track" :style="{ width: ganttTotalPx + 'px' }" :class="{ 'drop-target': dragState && dragOverPool }">
              <div class="gantt-gridline">
                <div v-for="cell in headCells" :key="cell.key" class="grid-col" :class="{ weekend: cell.shaded }" :style="{ width: cell.widthPx + 'px' }"></div>
                <div v-if="nowFrac >= 0" class="now-line" :style="{ left: (nowFrac * ganttTotalPx + 4) + 'px' }"></div>
              </div>
              <transition name="hint-pop">
                <div v-if="dragState && dragOverPool" class="pool-hint"><el-icon><component :is="'Back'" /></el-icon>松手移回待排产池并取消排产</div>
              </transition>
              <div class="pending-list">
                <div
                  v-for="t in sortedUnassigned"
                  :key="t.id"
                  class="pending-card"
                  :class="[('pri-' + (t.priority || 'MEDIUM').toLowerCase()), { 'card-selected': selectedTask?.id === t.id && !t.scheduleId, 'is-dragging': dragState?.task?.id === t.id }]"
                  @mousedown="onCardDown($event, t)"
                  @click.stop="selectTask(t, null)"
                >
                  <span class="pc-no">{{ t.orderNo }}</span>
                  <span class="pc-name">{{ t.productName }}</span>
                  <span class="pc-qty">{{ t.planQuantity }} 件 · {{ priLabel(t.priority) }}<template v-if="t.durationMin"> · {{ t.durationMin }}min</template></span>
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
                <div v-if="nowFrac >= 0" class="now-line" :style="{ left: (nowFrac * ganttTotalPx + 4) + 'px' }"></div>
                <div v-if="dragState && dragTargetWsId === eq.id" class="drag-line" :style="{ left: dragLineLeft + 'px' }"></div>
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
                  <span class="lane-dur">{{ laneDur(lane) }}</span>
                </div>
                <div class="lane-bars">
                  <el-tooltip
                    v-for="t in lane.steps"
                    :key="t.scheduleId"
                    placement="top"
                    :show-after="200"
                    :hide-after="0"
                    effect="dark"
                    :disabled="!!dragState"
                    popper-class="bar-tip"
                  >
                    <template #content>
                      <div class="bar-tip-body">
                        <div class="tip-title">{{ t.orderNo }}<span v-if="t.stepNo"> · {{ t.stepName }}</span></div>
                        <div class="tip-row">状态：{{ statusLabel(t.planStatus) }}<span v-if="t.scheduleStatus === 'FROZEN'">（已冻结）</span><span v-else-if="t.scheduleStatus === 'RELEASED'">（已下发）</span></div>
                        <div class="tip-row">设备：{{ equipmentOf(t)?.workstationName || '未分配' }}</div>
                        <div class="tip-row">计划：{{ fmtTime(t.plannedStartTime) }} ~ {{ fmtTime(t.plannedEndTime) }}</div>
                        <div class="tip-row">工时：{{ t.durationMin }} 分钟<span v-if="t.bottleneck"> · ⚡ 瓶颈</span></div>
                        <div class="tip-row">数量：{{ t.planQuantity }}<span v-if="t.progress"> · 进度 {{ t.progress }}%</span></div>
                      </div>
                    </template>
                    <div
                      class="gantt-bar"
                      :class="[
                        'st-' + t.planStatus.toLowerCase(),
                        { 'bar-conflict': conflictStepIds.has(t.scheduleId), 'bar-selected': selectedTask?.scheduleId === t.scheduleId, 'is-dragging': dragState?.task?.scheduleId === t.scheduleId }
                      ]"
                      :style="barStyle(t)"
                      @mousedown="onBarDown($event, t, eq)"
                      @dblclick.stop="openAdjustDialog(t, false)"
                      @click.stop
                      @contextmenu.prevent="openBarCtx($event, t, eq)"
                    >
                      <i v-if="barResizable(t)" class="resize-h left" @mousedown.stop="onResizeDown($event, t, eq, 'l')"></i>
                      <span class="bar-label">{{ barDisplay(t) }}</span>
                      <i v-if="barResizable(t)" class="resize-h" @mousedown.stop="onResizeDown($event, t, eq, 'r')"></i>
                    </div>
                  </el-tooltip>
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
                  <span class="detail-dev">
                    <el-icon :size="12"><Cpu /></el-icon>{{ equipmentOf(selectedTask)?.workstationName || '未分配' }}
                  </span>
                </div>
                <div class="detail-order">{{ selectedTask.orderNo }}</div>
                <div class="detail-product">{{ selectedTask.productName }}</div>
                <div class="detail-schedule">
                  <span class="ds-time">{{ fmtTime(selectedTask.plannedStartTime) }} ~ {{ fmtTime(selectedTask.plannedEndTime) }}</span>
                  <span class="ds-dur">{{ selectedTask.durationMin }} min</span>
                </div>
              </div>
              <div class="detail-body">
                <div class="info-group">
                  <div class="group-title">基础信息</div>
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-label">工序</span>
                      <span class="info-value">{{ selectedTask.stepName || '整单' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">产品型号</span>
                      <span class="info-value">{{ selectedTask.productModel || '-' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">优先级</span>
                      <span class="info-value">
                        <el-tag size="small" :type="priType(selectedTask.priority)" effect="light">{{ priLabel(selectedTask.priority) }}</el-tag>
                      </span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">计划数量</span>
                      <span class="info-value mono">{{ selectedTask.planQuantity }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">已完成</span>
                      <span class="info-value mono">{{ selectedTask.completedQuantity }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">设备</span>
                      <span class="info-value">{{ equipmentOf(selectedTask)?.workstationCode || '未分配' }}</span>
                    </div>
                  </div>
                </div>

                <div class="info-group">
                  <div class="group-title">生产进度</div>
                  <div class="progress-block">
                    <div class="progress-head">
                      <span class="progress-num">{{ selectedTask.progress || 0 }}%</span>
                      <span class="progress-caption">{{ (selectedTask.progress || 0) >= 100 ? '已全部完成' : (selectedTask.progress || 0) > 0 ? '生产中' : '未开始' }}</span>
                    </div>
                    <el-progress :percentage="selectedTask.progress || 0" :stroke-width="10" :color="progressColor(selectedTask.progress || 0)" :show-text="false" />
                    <div class="progress-meta">
                      <span>实际开始：{{ fmtTime(selectedTask.actualStartTime) }}</span>
                      <span>备注：{{ selectedTask.remark || '-' }}</span>
                    </div>
                  </div>
                </div>

                <div class="step-list-title">该工单工序（{{ orderSteps(selectedTask.id).length }}）</div>
                <el-timeline class="step-timeline">
                  <el-timeline-item
                    v-for="st in orderSteps(selectedTask.id)"
                    :key="st.scheduleId"
                    :timestamp="fmtShort(st.plannedStartTime) + ' ~ ' + fmtShort(st.plannedEndTime)"
                    placement="top"
                    :color="stepNodeColor(st)"
                  >
                    <div
                      class="step-item"
                      :class="{ active: selectedTask?.scheduleId === st.scheduleId }"
                      @click="selectTask(st, equipmentOf(st))"
                    >
                      <span class="step-name">{{ st.stepNo ? '工序' + st.stepNo + ' ' + st.stepName : '整单' }}</span>
                      <span class="step-status" :class="'st-' + st.planStatus.toLowerCase()">{{ statusLabel(st.planStatus) }}</span>
                      <span class="step-dev">{{ equipmentOf(st)?.workstationName || '未分配' }}</span>
                      <span v-if="st.durationMin" class="step-dur">{{ st.durationMin }}min</span>
                    </div>
                  </el-timeline-item>
                </el-timeline>

                <div class="detail-actions">
                  <el-button v-permission="'planning:edit'" size="small" type="primary" plain @click="openAdjustDialog(selectedTask, false)">调整时间</el-button>
                  <el-button v-permission="'planning:edit'" size="small" type="warning" plain @click="openAdjustDialog(selectedTask, true)">整单移动</el-button>
                  <el-button v-permission="'planning:edit'" size="small" type="success" plain @click="doRelease(selectedTask.id)">下发</el-button>
                  <el-button v-permission="'planning:edit'" size="small" type="danger" plain @click="doUnassign(selectedTask.id)">取消排产</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else description="点击甘特条查看详情" :image-size="70" />
          </el-tab-pane>

          <el-tab-pane label="变更日志" name="logs">
            <div class="log-toolbar">
              <span class="log-count">共 {{ logs.length }} 条</span>
              <el-button v-permission="'planning:edit'" size="small" type="danger" plain @click="doClearLogs">
                <el-icon><Delete /></el-icon>&nbsp;清空日志
              </el-button>
            </div>
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
        <div v-permission="'planning:edit'" class="ctx-item" @click="openAdjustDialog(ctxMenu.task, false)">调整本工序时间…</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="openAdjustDialog(ctxMenu.task, true)">整单移动 / 调时间…</div>
        <el-divider style="margin: 4px 0" />
        <div v-if="ctxMenu.task.scheduleStatus !== 'FROZEN'" v-permission="'planning:edit'" class="ctx-item" @click="doFreeze(ctxMenu.task, false)">冻结本工序</div>
        <div v-else v-permission="'planning:edit'" class="ctx-item" @click="doUnfreeze(ctxMenu.task)">解除冻结</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="doFreeze(ctxMenu.task, true)">冻结整单</div>
        <el-divider style="margin: 4px 0" />
        <div v-permission="'planning:edit'" class="ctx-item" @click="doRelease(ctxMenu.task.id)">下发排产</div>
        <div v-permission="'planning:edit'" class="ctx-item danger" @click="doUnassign(ctxMenu.task.id)">取消排产（拖回池）</div>
      </template>
      <template v-else-if="ctxMenu.eq">
        <div class="ctx-title">{{ ctxMenu.eq.workstationName }}</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="doFreezeWs(ctxMenu.eq, true)">冻结设备全部工序</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="doFreezeWs(ctxMenu.eq, false)">解冻设备全部工序</div>
      </template>
      <template v-else>
        <div class="ctx-title">待排产池</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="doAutoPlan(true)">自动排程（仅未排）</div>
        <div v-permission="'planning:edit'" class="ctx-item" @click="doAutoPlan(false)">全部重排</div>
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
import { usePermissionStore } from '@/stores/permission'
import {
  getPlanningBoard,
  movePlanningTask,
  unassignPlanningOrder,
  autoPlanOrders,
  undoPlanning,
  freezePlanning,
  unfreezePlanning,
  releasePlanning,
  clearPlanningLogs
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

const permissionStore = usePermissionStore()
/** 是否有排产编辑权限（拖拽/调整等写操作） */
const canEdit = computed(() => permissionStore.hasPermission('planning:edit'))

const selectedTask = ref<Task | null>(null)
const selectedOrderId = ref<number | null>(null)
let selectedOrderKey: string | null = null
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
/** 拖拽光标在目标行内的 x（px），用于吸附指示线 */
const dragLineLeft = ref(0)
/** resize 时是否已被相邻工序边界钳制 */
const dragHitBound = ref(false)
const dragGhost = ref<{ rect: { top: number; left: number; width: number }; startMs: number; endMs: number; wsId: string } | null>(null)
const poolRef = ref<HTMLElement | null>(null)
const bodyRef = ref<HTMLElement | null>(null)

// ==================== 右键菜单 ====================
const ctxMenu = ref<{ x: number; y: number; task: Task | null; eq: EqGroup | null } | null>(null)

// ==================== 调整对话框 ====================
const adjustVisible = ref(false)
const adjustTask = ref<Task | null>(null)
const adjustWhole = ref(false)
const adjustForm = ref<{ targetWorkstationId: string | number | null; newStart: string; newEnd: string; force: boolean }>({
  targetWorkstationId: null,
  newStart: '',
  newEnd: '',
  force: false
})

const legendItems = [
  { key: 'PENDING', label: '待排产', color: '#909399' },
  { key: 'READY', label: '已排产', color: '#409eff' },  { key: 'RUNNING', label: '运行中', color: '#2f9e69' },
  { key: 'COMPLETED', label: '已完成', color: '#a8c5a8' },
  { key: 'DELAYED', label: '延误', color: '#f56c6c' }
]

// ==================== KPI 统计 ====================
const kpiItems = computed(() => {
  const allTasks = equipment.value.flatMap(eq => eq.tasks)
  const orderIds = new Set<string>()
  allTasks.forEach(t => orderIds.add(t.id))
  unassigned.value.forEach(t => orderIds.add(t.id))
  const totalOrders = orderIds.size
  const scheduledOrders = new Set(allTasks.map(t => t.id)).size
  const totalSteps = allTasks.length
  const avgLoad = equipment.value.length
    ? Math.round(equipment.value.reduce((s, e) => s + (e.loadRate || 0), 0) / equipment.value.length)
    : 0
  const bottlenecks = equipment.value.filter(e => e.bottleneck).length
  return [
    { key: 'orders', icon: 'Document', label: '排产工单', value: `${totalOrders}`, sub: `${scheduledOrders} 已排 / ${unassigned.value.length} 待排`, bg: 'var(--accent-light)', color: 'var(--accent)' },
    { key: 'steps', icon: 'Operation', label: '工序排产', value: `${totalSteps}`, sub: `${equipment.value.length} 台设备`, bg: 'var(--info-light)', color: 'var(--info)' },
    { key: 'load', icon: 'DataLine', label: '平均负载', value: `${avgLoad}%`, sub: `瓶颈设备 ${bottlenecks} 台`, bg: avgLoad >= 85 ? 'var(--danger-light)' : avgLoad >= 60 ? 'var(--warning-light)' : 'var(--success-light)', color: avgLoad >= 85 ? 'var(--danger)' : avgLoad >= 60 ? 'var(--warning)' : 'var(--success)' },
    { key: 'conflicts', icon: 'Warning', label: '时间冲突', value: `${conflicts.value.length}`, sub: conflicts.value.length ? '需处理' : '排产健康', bg: 'var(--danger-light)', color: 'var(--danger)' },
    { key: 'hours', icon: 'Clock', label: '窗口工时', value: fmtWorkHours(workMinutes), sub: shiftText.value, bg: 'var(--success-light)', color: 'var(--success)' }
  ]
})

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

/** 当前时刻在窗口内的横向比例（0~1，窗口外为 -1） */
const nowFrac = computed(() => {
  const now = Date.now()
  if (now < wsStartMs.value || now > wsEndMs.value) return -1
  return (now - wsStartMs.value) / totalMs.value
})

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

const laneColor = (id: number) => swatchOf(id)

/** 泳道总工时（小时，保留 1 位小数） */
const laneDur = (lane: any) => {
  const mins = lane.steps.reduce((sum: number, t: Task) => sum + (parseDT(t.plannedEndTime) - parseDT(t.plannedStartTime)), 0)
  return Number.isFinite(mins) && mins > 0 ? (mins / 60000).toFixed(1) + 'h' : ''
}

/** 待排产池：高优先级在前，同级按数量/时间稳定排序 */
const sortedUnassigned = computed(() =>
  [...unassigned.value].sort((a, b) => {
    const p = { HIGH: 0, MEDIUM: 1, LOW: 2 }
    const pa = p[(a.priority || 'MEDIUM') as keyof typeof p]
    const pb = p[(b.priority || 'MEDIUM') as keyof typeof p]
    if (pa !== pb) return pa - pb
    return String(a.orderNo).localeCompare(String(b.orderNo))
  })
)

/** 工单/泳道和谐色板（低饱和，去纯黄，多工单清晰可分） */
const BAR_SWATCHES = [
  'hsl(220, 62%, 48%)',   // 蓝
  'hsl(250, 58%, 52%)',   // 靛紫
  'hsl(280, 52%, 50%)',   // 紫
  'hsl(190, 62%, 42%)',   // 青
  'hsl(155, 56%, 38%)',   // 绿
  'hsl(345, 62%, 48%)',   // 玫红
  'hsl(26, 68%, 46%)',    // 橙
  'hsl(204, 60%, 54%)'    // 天蓝
]

/** 按 ID 稳定取色板色（工单号与泳道共用，保证条与泳道圆点同色） */
const swatchOf = (id: number | string | null | undefined) => {
  let h = 0
  const s = String(id ?? 0)
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) & 0x7fffffff
  return BAR_SWATCHES[h % BAR_SWATCHES.length]
}

const barColor = (t: Task) => {
  // 状态语义色优先，工单区分用固定色板
  switch (t.planStatus) {
    case 'COMPLETED': return 'hsl(220, 12%, 62%)'
    case 'DELAYED': return 'hsl(350, 72%, 46%)'
    default: return swatchOf(t.id)
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

/** 条宽足够时才显示 resize 手柄（避免窄条手柄占满无法拖拽；已完成工序不可调） */
const barResizable = (t: Task) => {
  if (t.planStatus === 'COMPLETED') return false
  if (!t.plannedStartTime || !t.plannedEndTime) return false
  const s = parseDT(t.plannedStartTime)
  const e = parseDT(t.plannedEndTime)
  if (isNaN(s) || isNaN(e)) return false
  return (e - s) / totalMs.value * ganttTotalPx.value >= 56
}

const conflictStepIds = computed(() => new Set<string>(conflicts.value.flatMap(c => [c.scheduleAId, c.scheduleBId]).filter((x): x is string => !!x)))

// ==================== 拖拽逻辑 ====================
const snapTime = (ms: number) => {
  const snapped = Math.round(ms / SNAP_MS) * SNAP_MS
  return Math.max(wsStartMs.value, Math.min(wsEndMs.value - SNAP_MS, snapped))
}

/** 计算落点幽灵的像素位置（视口坐标，fixed 定位，滚动/裁剪不失效），附时间元数据用于冲突/标签 */
const ghostRect = (startMs: number, durMs: number, wsId: string, minW: number, mouseY?: number) => {
  const target = trackEls().find(el => String(el.dataset.wsId) === wsId)
  if (!target) return null
  const tr = target.getBoundingClientRect()
  const contentW = Math.max(1, ganttTotalPx.value)
  const frac = Math.max(0, Math.min(1, (startMs - wsStartMs.value) / totalMs.value))
  const left = tr.left + 4 + frac * contentW
  const width = Math.max(durMs / totalMs.value * contentW, minW, 12)
  // 垂直跟随鼠标所在位置（26px 条高居中），避免指示在行顶与鼠标错位；越界钳制在行内
  const BAR_H = 26
  const top = mouseY != null
    ? Math.min(Math.max(mouseY - BAR_H / 2, tr.top + 4), tr.bottom - BAR_H - 4)
    : tr.top + 6
  return {
    rect: { top, left, width },
    startMs,
    endMs: startMs + durMs,
    wsId
  }
}

const ghostStyle = computed(() => {
  const g = dragGhost.value
  return g ? { top: g.rect.top + 'px', left: g.rect.left + 'px', width: g.rect.width + 'px' } : {}
})

/** 幽灵时间标签（MM-DD HH:mm ~ HH:mm） */
const ghostTimeLabel = computed(() => {
  const g = dragGhost.value
  if (!g) return ''
  const f = (ms: number) => {
    const d = new Date(ms)
    const p = (n: number) => String(n).padStart(2, '0')
    return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
  }
  return `${f(g.startMs)} ~ ${f(g.endMs)}`
})

// ==================== 拖拽信息卡片 ====================
const dragTip = ref<{ x: number; y: number } | null>(null)
const dragTipStyle = computed(() => {
  if (!dragTip.value) return {}
  const w = 220
  const x = Math.min(dragTip.value.x + 18, window.innerWidth - w - 8)
  const y = Math.min(dragTip.value.y + 18, window.innerHeight - 108)
  return { left: x + 'px', top: y + 'px' }
})
const dragTipTitle = computed(() => {
  const t = dragState.value?.task
  if (!t) return ''
  return t.stepNo ? `${t.orderNo} · ${t.stepName}` : t.orderNo
})
const dragTipTime = computed(() => {
  const t = dragState.value?.task
  if (!t) return ''
  if (dragState.value?.type === 'pool') {
    const d = Math.max((t.durationMin || 480) * 60000, 30 * 60 * 1000)
    return `预计 ${(d / 3600000).toFixed(1)} 小时 · ${ghostTimeLabel.value}`
  }
  return ghostTimeLabel.value || (t.plannedStartTime ? `${fmtShort(t.plannedStartTime)} ~ ${fmtShort(t.plannedEndTime)}` : '')
})
const dragTipStatus = computed(() => {
  const t = dragState.value?.task
  if (!t) return ''
  if (dragState.value?.type === 'pool') return unassigned.value.some(x => x.id === t.id) ? '待排产' : ''
  return t.scheduleStatus === 'FROZEN' ? '已冻结' : t.scheduleStatus === 'RELEASED' ? '已下发' : statusLabel(t.planStatus)
})
const dragTipWsName = computed(() => {
  if (dragOverPool.value) return '待排产池（取消排产）'
  const wsId = dragGhost.value?.wsId ?? dragTargetWsId.value
  const eq = equipment.value.find(x => x.id === wsId)
  return eq ? `${eq.workstationName} ${eq.workstationCode}` : '—'
})

/** 检查某设备时段 [startMs,endMs) 是否与已有排产重叠（可排除自身工序） */
const checkOverlap = (wsId: string, startMs: number, endMs: number, excludeScheduleId?: string) => {
  const eq = equipment.value.find(x => x.id === wsId)
  if (!eq) return false
  return eq.tasks.some(x => {
    if (excludeScheduleId && x.scheduleId === excludeScheduleId) return false
    const s = parseDT(x.plannedStartTime)
    const e = parseDT(x.plannedEndTime)
    return !isNaN(s) && !isNaN(e) && s < endMs && e > startMs
  })
}

/** 幽灵落点与目标行已有工序时间重叠 → 冲突（排除自身） */
const ghostConflicting = computed(() => {
  const g = dragGhost.value
  const ds = dragState.value
  if (!g || !ds) return false
  if (g.startMs >= g.endMs) return false
  return checkOverlap(g.wsId, g.startMs, g.endMs, ds.task.scheduleId)
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
  if (!canEdit.value) {
    ElMessage.warning('无排产编辑权限，不能拖拽调整')
    return
  }
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
  if (t.planStatus === 'COMPLETED') {
    ElMessage.warning('已完成工序不可调整工时')
    return
  }
  if (!canEdit.value) {
    ElMessage.warning('无排产编辑权限，不能调整工时')
    return
  }
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
  if (!canEdit.value) {
    ElMessage.warning('无排产编辑权限，不能拖拽排产')
    return
  }
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

/** 同设备上其他工序的相邻边界：dir 'r' 返回最早下一道开始时间，dir 'l' 返回最晚前一道结束时间 */
const resizeBoundary = (t: Task, eq: EqGroup, dir: 'r' | 'l'): number | null => {
  const myStart = parseDT(t.plannedStartTime)
  const myEnd = parseDT(t.plannedEndTime)
  const others = eq.tasks.filter(x => x.scheduleId !== t.scheduleId)
  if (dir === 'r') {
    let minStart = Infinity
    for (const o of others) {
      const s = parseDT(o.plannedStartTime)
      if (!isNaN(s) && s > myStart) minStart = Math.min(minStart, s)
    }
    return minStart === Infinity ? null : minStart
  }
  let maxEnd = -Infinity
  for (const o of others) {
    const e = parseDT(o.plannedEndTime)
    if (!isNaN(e) && e < myEnd) maxEnd = Math.max(maxEnd, e)
  }
  return maxEnd === -Infinity ? null : maxEnd
}

/** 计算 resize 的新起止（动态钳制到相邻工序边界，保证不覆盖其他排产） */
const calcResize = (ds: DragState, dir: 'r' | 'l', deltaMs: number) => {
  const MIN_DUR = 30 * 60 * 1000
  const eq = equipment.value.find(x => x.id === ds.originWsId)
  let newStartMs = ds.startMs
  let newEndMs = ds.endMs
  let hitBound = false
  if (dir === 'r') {
    let end = snapTime(ds.endMs + deltaMs)
    const bound = eq ? resizeBoundary(ds.task, eq, 'r') : null
    if (bound !== null && end > bound) { end = bound; hitBound = true }
    // 若与下一道工序间距不足最小工时，保持原长（告知边界已到）
    if (end - ds.startMs < MIN_DUR) { end = ds.endMs; hitBound = false }
    newEndMs = end
  } else {
    let start = snapTime(ds.startMs + deltaMs)
    const bound = eq ? resizeBoundary(ds.task, eq, 'l') : null
    if (bound !== null && start < bound) { start = bound; hitBound = true }
    if (ds.endMs - start < MIN_DUR) { start = ds.startMs; hitBound = false }
    newStartMs = start
  }
  return { newStartMs, newEndMs, hitBound }
}

/** 拖拽时靠近平板边缘自动滚动（rAF 循环），保证可拖到远处行/时间 */
let edgeScrollRaf: number | null = null
const stopEdgeScroll = () => {
  if (edgeScrollRaf !== null) {
    cancelAnimationFrame(edgeScrollRaf)
    edgeScrollRaf = null
  }
}
const startEdgeScroll = (e: MouseEvent) => {
  const body = bodyRef.value
  if (!body || !dragState.value) return
  const EDGE = 44
  const STEP = 14
  const tick = () => {
    const ds = dragState.value
    const el = bodyRef.value
    if (!ds || !el) return
    const br = el.getBoundingClientRect()
    let moved = false
    if (e.clientY < br.top + EDGE) { el.scrollTop -= STEP; moved = true }
    else if (e.clientY > br.bottom - EDGE) { el.scrollTop += STEP; moved = true }
    if (e.clientX < br.left + EDGE) { el.scrollLeft -= STEP; moved = true }
    else if (e.clientX > br.right - EDGE) { el.scrollLeft += STEP; moved = true }
    if (moved) {
      // 滚动后刷新幽灵/目标行；onDragMove 内部会重启边缘滚动循环（保持单一 rAF 链）
      onDragMove(e)
    } else {
      edgeScrollRaf = null
    }
  }
  stopEdgeScroll()
  edgeScrollRaf = requestAnimationFrame(tick)
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
  startEdgeScroll(e)
  dragTip.value = { x: e.clientX, y: e.clientY }
  const deltaMs = deltaFromEvent(e, ds.startClientX)

  // 命中目标行（非track区域时保持上次目标，避免幽灵跳回首行）
  const hit = hitTargetRow(e)
  if (hit) {
    dragTargetWsId.value = hit
  }
  const lineEl = trackEls().find(el => String(el.dataset.wsId) === dragTargetWsId.value)
  if (lineEl) dragLineLeft.value = e.clientX - (lineEl.getBoundingClientRect().left + 4)
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
    dragGhost.value = ghostRect(newStart, durMs, targetWs, 0, e.clientY)
  } else if (ds.type === 'pool') {
    const durMs = Math.max((ds.task.durationMin || 480) * 60000, 30 * 60 * 1000)
    if (dragTargetWsId.value !== null) {
      const tMs = snapTime(mouseTimeInTrack(e, dragTargetWsId.value))
      dragGhost.value = ghostRect(tMs, durMs, dragTargetWsId.value, 0, e.clientY)
    } else {
      dragGhost.value = null
    }
  } else if (ds.type === 'resize-r') {
    const r = calcResize(ds, 'r', deltaMs)
    dragHitBound.value = r.hitBound
    dragGhost.value = ghostRect(r.newStartMs, r.newEndMs - r.newStartMs, ds.originWsId!, 0)
  } else if (ds.type === 'resize-l') {
    const r = calcResize(ds, 'l', deltaMs)
    dragHitBound.value = r.hitBound
    dragGhost.value = ghostRect(r.newStartMs, r.newEndMs - r.newStartMs, ds.originWsId!, 0)
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
  stopEdgeScroll()
  dragState.value = null
  dragGhost.value = null
  dragTip.value = null
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
      // 碰撞拦截：落点与目标行已有排产重叠时阻止（避免覆盖）
      if (checkOverlap(targetWs, newStartMs, newStartMs + (ds.endMs - ds.startMs), t.scheduleId)) {
        ElMessage.warning('落点与其他工序时间重叠，请选择空闲时段；如需强制重叠请用「调整时间」并勾选强制保存')
        return
      }
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
        const dur = Math.max((t.durationMin || 480) * 60000, 30 * 60 * 1000)
        if (checkOverlap(dragTargetWsId.value, newStartMs, newStartMs + dur)) {
          ElMessage.warning('该时段与已有排产重叠，请选择空闲时段；如需强制重叠请用「调整时间」并勾选强制保存')
          return
        }
        const newStart = fmtDateTime(new Date(newStartMs))
        await movePlanningTask({ workOrderId: t.id, targetWorkstationId: dragTargetWsId.value, newStart })
        ElMessage.success('排产成功')
        await loadBoard()
      }
    } else if (ds.type === 'resize-r') {
      const r = calcResize(ds, 'r', deltaFromEvent(e, ds.startClientX))
      if (r.newEndMs === ds.endMs) return
      const newEnd = fmtDateTime(new Date(r.newEndMs))
      applyMoveLocally(t, ds.originWsId!, ds.startMs, r.newEndMs)
      try {
        await movePlanningTask({
          scheduleId: t.scheduleId,
          workOrderId: t.id,
          targetWorkstationId: ds.originWsId!,
          newEnd
        })
        ElMessage.success(r.hitBound ? '已拉伸至相邻工序边界' : '已拉伸工时')
      } catch (err) {
        await loadBoard()
        ElMessage.error(String(err?.response?.data?.msg || err?.response?.data?.message || err?.message || '保存失败'))
      }
    } else if (ds.type === 'resize-l') {
      const r = calcResize(ds, 'l', deltaFromEvent(e, ds.startClientX))
      if (r.newStartMs === ds.startMs) return
      const newStart = fmtDateTime(new Date(r.newStartMs))
      applyMoveLocally(t, ds.originWsId!, r.newStartMs, ds.endMs)
      try {
        await movePlanningTask({
          scheduleId: t.scheduleId,
          workOrderId: t.id,
          targetWorkstationId: ds.originWsId!,
          newStart,
          newEnd: fmtDateTime(new Date(ds.endMs))
        })
        ElMessage.success(r.hitBound ? '已拉伸至相邻工序边界' : '已调整起点')
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
    dragHitBound.value = false
  }
}

/** Esc 取消拖拽/关闭右键菜单 */
const onKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    onDragCancel()
    ctxMenu.value = null
  }
}

const onDragCancel = () => {
  stopEdgeScroll()
  dragTip.value = null
  dragHitBound.value = false
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

const doClearLogs = async () => {
  try {
    await ElMessageBox.confirm('将清空全部变更日志（仅审计记录，不影响排产与撤销功能），确定继续？', '清空日志', { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' })
  } catch {
    return
  }
  try {
    await clearPlanningLogs()
    ElMessage.success('变更日志已清空')
    await loadBoard()
  } catch (err: any) {
    ElMessage.error('清空失败: ' + (err?.response?.data?.msg || err?.message))
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

/** 打开右键菜单并做视口边界修正（菜单约 200×280） */
const showCtx = (e: MouseEvent, task: Task | null, eq: EqGroup | null) => {
  ctxMenu.value = {
    x: Math.max(4, Math.min(e.clientX, window.innerWidth - 204)),
    y: Math.max(4, Math.min(e.clientY, window.innerHeight - 284)),
    task,
    eq
  }
}

const openBarCtx = (e: MouseEvent, t: Task, eq: EqGroup) => {
  selectTask(t, eq)
  showCtx(e, t, null)
}

const openRowCtx = (e: MouseEvent, eq: EqGroup | null, _task: Task | null) => {
  showCtx(e, null, eq)
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

const orderSteps = (workOrderId: string | number) => {
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

/** 详情进度条颜色按完成度分档 */
const progressColor = (p: number) => (p >= 100 ? '#2f9e69' : p >= 60 ? '#409eff' : p >= 30 ? '#e6a23c' : '#f56c6c')

/** 时间线节点颜色：优先执行状态，其次调度状态 */
const stepNodeColor = (st: Task) => {
  if (st.planStatus === 'DELAYED') return '#ef4444'
  if (st.planStatus === 'COMPLETED') return '#94a3b8'
  if (st.planStatus === 'RUNNING') return '#10b981'
  if (st.scheduleStatus === 'FROZEN') return '#f59e0b'
  if (st.scheduleStatus === 'RELEASED') return '#8b5cf6'
  return '#6366f1'
}

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
      if (!dragState.value && !loading.value && !adjustVisible.value) loadBoard()
    }, 30000)
  }
}

/** 阻止浏览器原生拖拽（复制/文本拖放） */
const preventNativeDrag = (e: Event) => {
  if (dragState.value) e.preventDefault()
}

/** 点击任意处关闭右键菜单 */
const onDocClick = () => {
  ctxMenu.value = null
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
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  stopEdgeScroll()
  if (pollTimer) clearInterval(pollTimer)
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('keydown', onKeyDown)
  document.removeEventListener('dragstart', preventNativeDrag)
  document.removeEventListener('click', onDocClick)
})

watch(autoRefresh, setupPolling)
</script>

<style scoped lang="scss">
.planning-page {
  height: calc(100vh - 146px);
  min-height: 560px;
  overflow: hidden;
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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
    padding: 6px 14px;
    box-shadow: var(--shadow-sm);
    flex-shrink: 0;
    transition: background var(--transition-normal), border-color var(--transition-normal);

    .header-left {
      display: flex;
      align-items: baseline;
      gap: 10px;
      h2 {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;
        &::before {
          content: '';
          width: 4px;
          height: 14px;
          border-radius: 2px;
          background: var(--gradient-primary);
        }
      }
      .sub-title { font-size: 11px; color: var(--text-tertiary); }
    }
    .header-right {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
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
    padding: 5px 10px;
    box-shadow: var(--shadow-sm);
    transition: background var(--transition-normal), border-color var(--transition-normal);
    flex-shrink: 0;

    .legend-group {
      display: flex;
      align-items: center;
      gap: 10px;
      .legend-item {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 11px;
        color: var(--text-secondary);
        white-space: nowrap;
        .legend-dot { width: 8px; height: 8px; border-radius: 3px; flex: none; }
      }
    }
    .bar-divider {
      width: 1px;
      height: 18px;
      background: var(--border-color);
      margin: 0 2px;
    }

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
  }

  /* ===== KPI 指标卡 ===== */
  .kpi-row {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;
    flex-shrink: 0;

    .kpi-card {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      transition: all var(--transition-fast);

      &:hover { border-color: var(--accent); transform: translateY(-1px); }

      .kpi-icon {
        width: 30px;
        height: 30px;
        border-radius: var(--radius-md);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 15px;
        flex: none;
      }
      .kpi-info {
        display: flex;
        flex-direction: column;
        min-width: 0;
        .kpi-value {
          font-size: 15px;
          font-weight: 700;
          color: var(--text-primary);
          font-family: 'Consolas', monospace;
          line-height: 1.3;
        }
        .kpi-label {
          font-size: 10px;
          color: var(--text-tertiary);
          margin-top: 1px;
        }
        .kpi-sub {
          font-size: 10px;
          color: var(--text-secondary);
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }

  /* ===== 布局 ===== */
  .board-layout {
    display: flex;
    gap: 14px;
    align-items: stretch;
    flex: 1;
    min-height: 0;

    .gantt-area {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      height: 100%;
      min-height: 0;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      transition: background var(--transition-normal), border-color var(--transition-normal);
    }

    .side-panel {
      flex: 0 0 360px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-sm);
      overflow: hidden;
      padding: 0 14px 14px;
      height: 100%;
      min-height: 0;
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
          .detail-order { font-size: 17px; font-weight: 700; margin-top: 8px; letter-spacing: 0.5px; }
          .detail-product { font-size: 12px; opacity: 0.92; margin-top: 2px; }
          .detail-schedule {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-top: 10px;
            padding: 6px 10px;
            border-radius: var(--radius-sm);
            background: rgba(255, 255, 255, 0.14);
            font-size: 11.5px;
            .ds-time { font-family: 'Consolas', monospace; letter-spacing: 0.3px; }
            .ds-dur {
              margin-left: auto;
              font-family: 'Consolas', monospace;
              font-weight: 600;
              padding: 1px 8px;
              border-radius: 8px;
              background: rgba(255, 255, 255, 0.22);
            }
          }
          .detail-dev {
            margin-left: auto;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            padding: 2px 10px;
            border-radius: 10px;
            background: rgba(255, 255, 255, 0.22);
          }
        }

        .detail-body { padding: 12px 2px; }

        .info-group {
          margin-bottom: 10px;
          padding: 10px 12px;
          border: 1px solid var(--border-light);
          border-radius: var(--radius-md);
          background: var(--bg-hover);
          .group-title {
            font-size: 11px;
            font-weight: 600;
            color: var(--text-tertiary);
            margin-bottom: 8px;
            display: flex;
            align-items: center;
            gap: 6px;
            &::before {
              content: '';
              width: 3px;
              height: 11px;
              border-radius: 2px;
              background: var(--gradient-primary);
            }
          }
          .info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 6px 12px;
            .info-item {
              display: flex;
              align-items: baseline;
              gap: 6px;
              min-width: 0;
              .info-label {
                flex: none;
                font-size: 11px;
                color: var(--text-tertiary);
              }
              .info-value {
                font-size: 12px;
                font-weight: 600;
                color: var(--text-primary);
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &.mono { font-family: 'Consolas', monospace; }
              }
            }
          }
          .progress-block {
            .progress-head {
              display: flex;
              align-items: baseline;
              gap: 8px;
              margin-bottom: 6px;
              .progress-num {
                font-size: 18px;
                font-weight: 700;
                font-family: 'Consolas', monospace;
                color: var(--text-primary);
              }
              .progress-caption { font-size: 11px; color: var(--text-tertiary); }
            }
            .progress-meta {
              margin-top: 8px;
              display: flex;
              flex-direction: column;
              gap: 3px;
              font-size: 11px;
              color: var(--text-tertiary);
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
        }

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
          padding: 6px 10px;
          border-radius: var(--radius-sm);
          cursor: pointer;
          font-size: 12px;
          border: 1px solid var(--border-light);
          background: var(--bg-hover);
          transition: all var(--transition-fast);
          &:hover { border-color: var(--accent); background: var(--accent-light); }
          &.active {
            border-color: var(--accent);
            background: var(--accent-light);
            box-shadow: inset 3px 0 0 var(--accent);
          }
          .step-name { font-weight: 600; white-space: nowrap; color: var(--text-primary); }
          .step-status {
            font-size: 10px;
            font-weight: 500;
            padding: 1px 6px;
            border-radius: 8px;
            &.st-pending { color: var(--text-tertiary); background: var(--bg-hover); }
            &.st-ready { color: var(--accent); background: var(--accent-light); }
            &.st-running { color: var(--success); background: var(--success-light); }
            &.st-completed { color: var(--text-tertiary); background: var(--bg-hover); }
            &.st-delayed { color: var(--danger); background: var(--danger-light); }
          }
          .step-dev { color: var(--text-tertiary); margin-left: auto; }
          .step-dur { color: var(--text-tertiary); font-size: 11px; font-family: 'Consolas', monospace; }
        }
        .step-timeline {
          padding-left: 4px;
          :deep(.el-timeline-item__timestamp) { color: var(--text-tertiary); font-size: 11px; padding-bottom: 2px; }
          :deep(.el-timeline-item__content) { padding-bottom: 8px; }
        }

        .detail-actions {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 8px;
          margin-top: 14px;
          :deep(.el-button) { margin: 0; width: 100%; }
        }
      }

      .log-toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 4px 6px 8px;
        border-bottom: 1px solid var(--border-light);
        .log-count { font-size: 12px; color: var(--text-tertiary); }
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
      /* 与下方 track 内格子同起点（track padding-left 4px） */
      padding: 0 4px;
    }

    .gantt-timeline {
      display: flex;
      &.month-row { border-bottom: 1px solid var(--border-light); }
      &.day-row { position: relative; }
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

    .now-head-tag {
      position: absolute;
      top: 50%;
      transform: translate(-50%, -50%);
      font-size: 10px;
      font-weight: 700;
      color: #fff;
      background: var(--danger);
      padding: 1px 6px;
      border-radius: 8px;
      z-index: 3;
      line-height: 15px;
      white-space: nowrap;
      letter-spacing: 0.5px;
      box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
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
          outline: 1.5px dashed rgba(99, 102, 241, 0.85);
          outline-offset: -1px;
          border-radius: var(--radius-sm);
          background: linear-gradient(180deg, rgba(99, 102, 241, 0.10), rgba(139, 92, 246, 0.06));
          box-shadow: inset 0 0 14px rgba(99, 102, 241, 0.12);
        }

        .gantt-gridline {
          position: absolute;
          /* 与 bar/表头同基准（track padding 4px） */
          top: 0;
          bottom: 0;
          left: 4px;
          right: 4px;
          display: flex;
          pointer-events: none;
          .grid-col {
            border-left: 1px dashed var(--border-light);
            &.weekend { background: var(--warning-light); opacity: 0.5; }
          }
          .now-line {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 2px;
            background: var(--danger);
            opacity: 0.75;
            z-index: 2;
            box-shadow: 0 0 4px rgba(239, 68, 68, 0.5);
            &::before {
              content: '';
              position: absolute;
              top: 0;
              left: 50%;
              transform: translateX(-50%);
              border: 4px solid transparent;
              border-top-color: var(--danger);
            }
          }
          .drag-line {
            position: absolute;
            top: 0;
            bottom: 0;
            width: 1.5px;
            background: var(--accent);
            opacity: 0.9;
            z-index: 3;
            box-shadow: 0 0 6px rgba(99, 102, 241, 0.6);
            &::before {
              content: '';
              position: absolute;
              top: 0;
              left: 50%;
              transform: translateX(-50%);
              width: 7px;
              height: 7px;
              border-radius: 50%;
              background: var(--accent);
              box-shadow: 0 0 6px rgba(99, 102, 241, 0.8);
            }
          }
        }

        .order-lane {
          position: relative;
          padding-top: 20px;
          border-bottom: 1px dashed var(--border-light);

          &.lane-selected {
            background: var(--accent-light);
            border-radius: var(--radius-sm);
            border-left: 3px solid var(--accent);
            box-shadow: 0 0 0 1px rgba(99, 102, 241, 0.25);
            .order-lane-head { font-weight: 600; }
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
            .lane-dur {
              font-size: 10px;
              font-family: 'Consolas', monospace;
              color: var(--text-tertiary);
              background: var(--bg-hover);
              border: 1px solid var(--border-light);
              padding: 0 5px;
              border-radius: 7px;
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

            &.pri-high {
              background: rgba(245, 108, 108, 0.08);
              border-color: rgba(245, 108, 108, 0.32);
              .pc-no { color: var(--danger); }
            }
            &.pri-medium {
              background: rgba(64, 158, 255, 0.08);
              border-color: rgba(64, 158, 255, 0.32);
              .pc-no { color: var(--accent); }
            }
            &.pri-low {
              background: rgba(144, 147, 153, 0.06);
              border-color: rgba(144, 147, 153, 0.3);
              .pc-no { color: var(--text-tertiary); }
            }

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

        .is-dragging {
          opacity: 0.35 !important;
          filter: saturate(0.5);
          box-shadow: 0 10px 24px rgba(0, 0, 0, 0.25) !important;
          transform: scale(0.97);
          transform-origin: center;
        }

        .pool-hint {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          z-index: 90;
          display: flex;
          align-items: center;
          gap: 6px;
          background: var(--danger-light);
          color: var(--danger);
          font-size: 13px;
          font-weight: 600;
          padding: 8px 16px;
          border-radius: var(--radius-md);
          border: 1.5px dashed var(--danger);
          pointer-events: none;
          white-space: nowrap;
        }
      }
    }

    /* 拖拽落点幽灵：fixed 视口定位，任何滚动/裁剪下都可见 */
    .drag-ghost {
      position: fixed;
      top: 0;
      height: 26px;
      border-radius: 6px;
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.85), rgba(139, 92, 246, 0.85));
      border: 1.5px solid var(--accent);
      box-shadow: 0 6px 18px rgba(99, 102, 241, 0.4);
      z-index: 5000;
      pointer-events: none;
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: visible;
      animation: ghostPulse 1.4s ease-in-out infinite;

      &.ghost-conflict {
        background: linear-gradient(135deg, rgba(239, 68, 68, 0.85), rgba(249, 115, 22, 0.85));
        border-color: var(--danger);
        box-shadow: 0 6px 18px rgba(239, 68, 68, 0.4);
      }
    }

    /* 拖拽信息跟随卡片：固定宽度，落点条过窄时信息仍完整可见 */
    .drag-tip-card {
      position: fixed;
      z-index: 5001;
      min-width: 200px;
      max-width: 260px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-lg);
      padding: 8px 12px;
      pointer-events: none;
      font-size: 12px;
      color: var(--text-secondary);
      line-height: 1.5;

      .tip-card-title {
        font-weight: 700;
        font-size: 12.5px;
        color: var(--text-primary);
        margin-bottom: 3px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .tip-card-time {
        display: flex;
        align-items: center;
        gap: 5px;
        font-family: 'Consolas', monospace;
        font-size: 11.5px;
        color: var(--text-primary);
        white-space: nowrap;
      }
      .tip-card-status {
        margin-top: 3px;
        font-size: 11px;
        color: var(--text-secondary);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .tip-card-bound {
        margin-top: 4px;
        font-size: 11px;
        font-weight: 600;
        color: var(--warning);
        background: var(--warning-light);
        padding: 2px 8px;
        border-radius: 6px;
      }

      &.tip-conflict {
        border-color: var(--danger);
        .tip-card-time { color: var(--danger); }
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

@keyframes ghostPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.82; }
}

.hint-pop-enter-active, .hint-pop-leave-active { transition: all 0.18s ease; }
.hint-pop-enter-from, .hint-pop-leave-to { opacity: 0; transform: translate(-50%, -50%) scale(0.9); }
</style>

<!-- bar 悬停提示卡（全局样式，随主题） -->
<style lang="scss">
.bar-tip {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-lg) !important;
  padding: 8px 10px !important;

  .bar-tip-body {
    min-width: 220px;
    .tip-title {
      font-weight: 700;
      font-size: 13px;
      color: var(--text-primary);
      margin-bottom: 6px;
      padding-bottom: 6px;
      border-bottom: 1px solid var(--border-light);
    }
    .tip-row {
      font-size: 12px;
      color: var(--text-secondary);
      line-height: 1.7;
    }
  }
}
.bar-tip .el-popper__arrow::before {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}
</style>