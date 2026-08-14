<template>
  <div ref="containerRef" class="twins-scene" @click="onSceneClick" @mousemove="onMouseMove">
    <div v-if="!ready" class="twins-loading">
      <div class="twins-spinner" />
      <span>加载数字孪生...</span>
    </div>
    <!-- HUD -->
    <div class="twins-hud">
      <div class="hud-row">
        <div class="hud-badge running">{{ statusCount.running }}<span>运行</span></div>
        <div class="hud-badge fault">{{ statusCount.fault }}<span>故障</span></div>
        <div class="hud-badge idle">{{ statusCount.idle }}<span>离线</span></div>
        <div class="hud-badge maintenance">{{ statusCount.maintenance }}<span>维护</span></div>
      </div>
    </div>
    <!-- 视角切换 -->
    <div class="twins-views">
      <button v-for="v in viewPresets" :key="v.key" class="view-btn" :class="{ on: currentView === v.key }" :title="v.label" @click="flyToView(v.key)">
        <el-icon><component :is="v.icon" /></el-icon>
      </button>
    </div>
    <!-- 选中设备信息 -->
    <transition name="slide-up">
      <div v-if="selectedDevice" class="twins-device-panel">
        <div class="panel-header">
          <span class="panel-dot" :style="{ background: statusColor(selectedDevice.status) }" />
          <div class="panel-title-info">
            <strong>{{ selectedDevice.name || selectedDevice.deviceName || '设备' }}</strong>
            <span class="panel-code">{{ selectedDevice.code || selectedDevice.deviceCode }}</span>
          </div>
          <span class="panel-status" :class="selectedDevice.status">{{ statusText(selectedDevice.status) }}</span>
        </div>
        <div class="panel-grid">
          <div>
            <label>温度</label>
            <span :class="tempClass(selectedDevice.temperature)">{{ selectedDevice.temperature != null ? Number(selectedDevice.temperature).toFixed(1) : '--' }}°C</span>
          </div>
          <div><label>转速</label><span>{{ selectedDevice.speed ?? '--' }} rpm</span></div>
          <div><label>功率</label><span>{{ selectedDevice.power ?? '--' }} kW</span></div>
          <div><label>利用率</label><span>{{ selectedDevice.utilization || '0%' }}</span></div>
          <div><label>运行</label><span>{{ selectedDevice.runtime ?? '--' }}</span></div>
          <div><label>OEE</label><span>{{ selectedDevice.efficiency || '--' }}%</span></div>
        </div>
        <div class="panel-bar-wrap">
          <div class="panel-bar"><div :style="{ width: (parseInt(selectedDevice.utilization) || 0) + '%' }"></div></div>
          <span class="panel-bar-label">{{ selectedDevice.utilization || '0%' }}</span>
        </div>
        <!-- 温度趋势（实时 sparkline） -->
        <div class="panel-trend">
          <span class="panel-trend-label">温度趋势 °C</span>
          <svg v-if="selectedTrend && selectedTrend.length > 1" :viewBox="`0 0 ${trendW} ${trendH}`" preserveAspectRatio="none" class="panel-trend-svg">
            <polyline :points="trendPoints(selectedTrend)" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
            <circle v-if="selectedTrend.length" :cx="lastTrendX(selectedTrend)" :cy="lastTrendY(selectedTrend)" r="3" fill="#f59e0b" />
          </svg>
          <span v-else class="panel-trend-empty">等待实时数据...</span>
        </div>
        <div class="panel-actions">
          <button title="AI预测" @click="emit('action', { type:'predict', device: selectedDevice })"><Cpu /></button>
          <button title="SPC分析" @click="emit('action', { type:'spc', device: selectedDevice })"><Histogram /></button>
          <button title="能耗优化" @click="emit('action', { type:'energy', device: selectedDevice })"><Lightning /></button>
          <button title="AI建议" @click="emit('action', { type:'llm', device: selectedDevice })"><ChatLineRound /></button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { useThemeStore } from '@/stores/theme'

import { Cpu, Histogram, Lightning, ChatLineRound } from '@element-plus/icons-vue'
const themeStore = useThemeStore()

const props = defineProps<{ devices: any[] }>()
const emit = defineEmits<{ select: [device: any]; action: [payload: { type: string; device: any }] }>()
const containerRef = ref<HTMLElement>()
const ready = ref(false)
const selectedDevice = ref<any>(null)

// ===== 温度趋势（选中设备实时 sparkline） =====
const trendW = 220
const trendH = 36
const trendBuffer = new Map<string, number[]>()
const selectedTrend = computed(() => {
  const code = selectedDevice.value?.code || selectedDevice.value?.deviceCode
  return code ? trendBuffer.get(code) || [] : []
})
const trendPoints = (arr: number[]) => {
  if (arr.length < 2) return ''
  const min = Math.min(...arr), max = Math.max(...arr)
  const span = max - min || 1
  return arr.map((v, i) => {
    const x = (i / (arr.length - 1)) * trendW
    const y = trendH - 3 - ((v - min) / span) * (trendH - 6)
    return `${x.toFixed(1)},${y.toFixed(1)}`
  }).join(' ')
}
const lastTrendX = (arr: number[]) => trendW
const lastTrendY = (arr: number[]) => {
  const min = Math.min(...arr), max = Math.max(...arr)
  const span = max - min || 1
  return trendH - 3 - ((arr[arr.length - 1] - min) / span) * (trendH - 6)
}

type SceneVars = {
  scene: THREE.Scene
  camera: THREE.PerspectiveCamera
  webgl: THREE.WebGLRenderer
  lbl: CSS2DRenderer
  controls: OrbitControls
  clock: THREE.Clock
  animId: number
  raycaster: THREE.Raycaster
  mouse: THREE.Vector2
}

let S: SceneVars
let deviceNodes: { root: THREE.Group; spindle?: THREE.Mesh | null; led: THREE.Mesh | null; label?: CSS2DObject | null; towerLights?: THREE.Mesh[] }[] = []
let selectedOutline: THREE.Group | null = null
let hoverOutline: THREE.Group | null = null
let factoryGroup = new THREE.Group()
let zoneGroup: THREE.Group | null = null
let lastGridHW = 0; let lastGridHD = 0
const currentView = ref('perspective')

const ST = (s: string) => {
  const up = (s || 'ONLINE').toUpperCase()
  const m: Record<string, string> = { RUNNING: 'ONLINE', IDLE: 'OFFLINE', FAULT: 'FAULT', ALARM: 'FAULT', MAINTENANCE: 'MAINTENANCE', ONLINE: 'ONLINE', OFFLINE: 'OFFLINE' }
  return m[up] || up
}
const CLR: Record<string, number> = { ONLINE: 0x34c759, FAULT: 0xff3b30, OFFLINE: 0x8e8e93, MAINTENANCE: 0xff9500 }
const GLW: Record<string, number> = { ONLINE: 0x1a7a3a, FAULT: 0x7a1a1a, OFFLINE: 0x0, MAINTENANCE: 0x7a3a00 }
const TXT: Record<string, string> = { ONLINE: '正常', FAULT: '故障', OFFLINE: '离线', MAINTENANCE: '维护' }

const statusCount = computed(() => {
  const c = { running: 0, fault: 0, idle: 0, maintenance: 0 }
  props.devices?.forEach((d: any) => {
    const s = ST(d.status)
    if (s === 'ONLINE') c.running++
    else if (s === 'FAULT') c.fault++
    else if (s === 'MAINTENANCE') c.maintenance++
    else c.idle++
  })
  return c
})

function statusColor(s: string) { const st = ST(s); const h = CLR[st]?.toString(16).padStart(6, '0') || '6366f1'; return '#' + h }
function statusText(s: string) { const st = ST(s); const m: Record<string,string> = { ONLINE: '运行中', FAULT: '故障', OFFLINE: '离线', MAINTENANCE: '维护中' }; return m[st] || st }
function tempClass(t: any) {
  const v = parseFloat(t)
  if (isNaN(v)) return ''
  if (v >= 70) return 'temp-danger'
  if (v >= 55) return 'temp-warn'
  return 'temp-ok'
}

const viewPresets = [
  { key: 'perspective', label: '透视图', icon: 'View' },
  { key: 'top', label: '俯视图', icon: 'Top' },
  { key: 'front', label: '前视图', icon: 'Connection' },
]

// ─── materials ───
function mat(c: number, r = 0.35, m = 0.45, e = 0, ei = 0) {
  return new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
}

// ─── label ───
/** HTML 转义，防止设备名等外部数据注入脚本 */
function escHtml(s: string): string {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function labelHtml(text: string, border: string, bg = 'rgba(0,0,0,.72)'): CSS2DObject {
  const div = document.createElement('div')
  div.innerHTML = text
  div.style.cssText = `color:#fff;font-size:10px;font-weight:500;font-family:monospace;
background:${bg};backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);
padding:2px 8px;border-radius:5px;border:1px solid ${border};white-space:nowrap;pointer-events:none;
box-shadow:0 1px 6px rgba(0,0,0,.25)`
  return new CSS2DObject(div)
}

// ─── dynamic factory environment ───
const GRID_COLS = 5; const SX = 5; const SZ = 4.5; const PAD = 3.5

function calcGridBounds(deviceCount: number) {
  const rows = Math.ceil(Math.max(deviceCount, 1) / GRID_COLS)
  return {
    halfW: ((Math.min(deviceCount || 1, GRID_COLS) - 1) * SX) / 2 + PAD,
    halfD: ((rows - 1) * SZ) / 2 + PAD,
    rows,
    ox: -((Math.min(Math.max(deviceCount, 1), GRID_COLS) - 1) * SX) / 2,
    oz: -((rows - 1) * SZ) / 2,
  }
}

function clearFactory() {
  if (!S) return
  factoryGroup.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
  S.scene.remove(factoryGroup)
  factoryGroup = new THREE.Group()
}

function buildFactory(deviceCount: number, dark: boolean) {
  clearFactory()
  const bounds = calcGridBounds(deviceCount)
  const { halfW, halfD } = bounds
  lastGridHW = halfW; lastGridHD = halfD
  const floorW = halfW * 2 + 6; const floorD = halfD * 2 + 6

  const bg = dark ? 0x2a2a2a : 0xe8e8ed
  S.scene.background = new THREE.Color(bg)
  S.scene.fog = new THREE.Fog(bg, floorW * 0.8, floorW * 2.2)

  // floor with tile pattern
  const floorGeo = new THREE.PlaneGeometry(floorW, floorD)
  const floorCanvas = document.createElement('canvas')
  floorCanvas.width = 512; floorCanvas.height = Math.round(512 * floorD / floorW)
  const ctx = floorCanvas.getContext('2d')!
  ctx.fillStyle = dark ? '#2a2a2a' : '#d8d8e0'
  ctx.fillRect(0, 0, floorCanvas.width, floorCanvas.height)
  ctx.strokeStyle = dark ? '#3a3a3a' : '#c0c0cc'
  ctx.lineWidth = 1
  for (let x = 0; x < floorCanvas.width; x += 32) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, floorCanvas.height); ctx.stroke() }
  for (let y = 0; y < floorCanvas.height; y += 32) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(floorCanvas.width, y); ctx.stroke() }
  const floorTex = new THREE.CanvasTexture(floorCanvas)
  floorTex.wrapS = THREE.RepeatWrapping; floorTex.wrapT = THREE.RepeatWrapping
  const floorMat = new THREE.MeshStandardMaterial({ map: floorTex, roughness: 0.85, metalness: 0.05 })
  const floorMeshLocal = new THREE.Mesh(floorGeo, floorMat)
  floorMeshLocal.rotation.x = -Math.PI / 2; floorMeshLocal.receiveShadow = true; factoryGroup.add(floorMeshLocal)

  // safety zone around device grid
  const zoneW = halfW * 2 + 1; const zoneD = halfD * 2 + 1
  const zCanvas = document.createElement('canvas')
  const zcW = 512; const zcH = Math.round(512 * zoneD / zoneW)
  zCanvas.width = Math.max(zcW, 128); zCanvas.height = Math.max(zcH, 128)
  const zctx = zCanvas.getContext('2d')!
  zctx.clearRect(0, 0, zCanvas.width, zCanvas.height)
  zctx.strokeStyle = dark ? '#3a4a20' : '#a0b050'
  zctx.lineWidth = 4
  zctx.setLineDash([16, 8])
  const margin = 8
  zctx.strokeRect(margin, margin, zCanvas.width - margin * 2, zCanvas.height - margin * 2)
  const zoneTex = new THREE.CanvasTexture(zCanvas)
  const zonePlane = new THREE.Mesh(
    new THREE.PlaneGeometry(zoneW + 1.5, zoneD + 1.5),
    new THREE.MeshStandardMaterial({ map: zoneTex, transparent: true, depthWrite: false })
  )
  zonePlane.rotation.x = -Math.PI / 2; zonePlane.position.y = 0.005; zonePlane.receiveShadow = true
  // store for replacement
  if (zoneGroup) { zoneGroup.traverse(c => { if (c instanceof THREE.Mesh && c.material instanceof THREE.Material) c.material.dispose() }); S.scene.remove(zoneGroup) }
  zoneGroup = new THREE.Group(); zoneGroup.add(zonePlane)
  factoryGroup.add(zoneGroup)

  // columns at corners of device grid + safety padding
  const colGeo = new THREE.CylinderGeometry(0.18, 0.22, 3, 8)
  const colMat = mat(dark ? 0x3a3a3a : 0xccccd8, 0.5, 0.6)
  const colCX = halfW - 0.3; const colCZ = halfD - 0.3
  ;[[-colCX, 1.5, -colCZ], [colCX, 1.5, -colCZ], [-colCX, 1.5, colCZ], [colCX, 1.5, colCZ]].forEach(([x, y, z]) => {
    const col = new THREE.Mesh(colGeo, colMat)
    col.position.set(x, y, z); col.castShadow = true; factoryGroup.add(col)
  })

  // ceiling truss lines — semi-transparent, unobtrusive
  const trussGeoX = new THREE.BoxGeometry(0.02, 0.02, halfD * 2 + 2)
  const trussGeoZ = new THREE.BoxGeometry(halfW * 2 + 2, 0.02, 0.02)
  const trussMat = new THREE.MeshStandardMaterial({ color: dark ? 0x3a3a3a : 0xb8b8c8, roughness: 0.5, metalness: 0.3, transparent: true, opacity: 0.25 })
  const trussY = 3.2
  ;[-halfW + 1, halfW - 1, -halfW + 3, halfW - 3].forEach(x => {
    const t = new THREE.Mesh(trussGeoX, trussMat); t.position.set(x, trussY, 0); t.renderOrder = 1; factoryGroup.add(t)
  })
  ;[-halfD + 1, halfD - 1, -halfD + 3, halfD - 3].forEach(z => {
    const t = new THREE.Mesh(trussGeoZ, trussMat); t.position.set(0, trussY, z); t.renderOrder = 1; factoryGroup.add(t)
  })

  // low walls around perimeter
  const wallH = 1.5
  const wallMat = mat(dark ? 0x343434 : 0xb8b8c8, 0.8, 0.1)
  const wallThick = 0.15
  const wx = halfW + PAD; const wz = halfD + PAD
  const wallPositions: [number, number, number, number, number, number][] = [
    [wx + wallThick / 2, wallH / 2, 0, wallThick, wallH, wz * 2 + 4],    // right
    [-wx - wallThick / 2, wallH / 2, 0, wallThick, wallH, wz * 2 + 4],   // left
    [0, wallH / 2, wz + wallThick / 2, wx * 2 + 4, wallH, wallThick],    // front
    [0, wallH / 2, -wz - wallThick / 2, wx * 2 + 4, wallH, wallThick],   // back
  ]
  wallPositions.forEach(([x, y, z, w, h, d]) => {
    const wall = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), wallMat)
    wall.position.set(x, y, z); wall.receiveShadow = true; factoryGroup.add(wall)
  })

  S.scene.add(factoryGroup)
}

// ─── lighting ───
function buildLighting(s: THREE.Scene, dark: boolean) {
  s.add(new THREE.AmbientLight(dark ? 0xbbbbbb : 0xf0f0f0, 0.85))
  const key = new THREE.DirectionalLight(dark ? 0xffffff : 0xffffff, 1.8)
  key.position.set(14, 20, 10)
  key.castShadow = true
  key.shadow.mapSize.set(2048, 2048)
  key.shadow.camera.near = 0.5; key.shadow.camera.far = 60
  key.shadow.camera.left = -20; key.shadow.camera.right = 20
  key.shadow.camera.top = 15; key.shadow.camera.bottom = -15
  key.shadow.bias = -0.0001
  s.add(key)
  const fill = new THREE.DirectionalLight(0x888888, 0.35)
  fill.position.set(-10, 8, -8)
  s.add(fill)
  s.add(new THREE.HemisphereLight(dark ? 0x888888 : 0xaaaaaa, dark ? 0x333333 : 0x888888, 0.25))
}

// ─── CNC加工中心 (立式铣床) ───
function buildMachine(device: any): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const g = GLW[st] || 0x0
  const root = new THREE.Group()
  const fz = 0.525 // 前面板Z轴坐标

  // 【机身底座】— 灰色底板
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.6, 0.14, 2.0), mat(0x8a8a8a, 0.4, 0.6)))
  root.children[root.children.length - 1].position.y = 0.07
  // 【底座底板】— 深色垫板
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.025, 1.5), mat(0x505050, 0.7, 0.7)))
  root.children[root.children.length - 1].position.set(0, 0.015, 0.2)

  // 【顶部装饰条】
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.38, 0.06, 1.68), mat(0x888888, 0.2, 0.75)))
  root.children[root.children.length - 1].position.set(0, 1.96, 0)

  // 【C型框架立柱】— 机身主支撑
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.8, 0.25), mat(0xaaaaaa, 0.25, 0.7)))
  root.children[root.children.length - 1].position.set(0.05, 1.04, -0.95)
  // 【Z轴导轨】— 3条竖向金属轨
  ;[-0.45, 0, 0.45].forEach(rx => {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(0.025, 1.4, 0.015), mat(0x888888, 0.1, 0.9)))
    root.children[root.children.length - 1].position.set(rx, 1.04, -0.825)
  })
  // 【风琴罩】— 5段防护罩
  for (let wi = 0; wi < 5; wi++) {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.03, 0.05), mat(0x666666, 0.5, 0.5)))
    root.children[root.children.length - 1].position.set(0.05, 0.6 + wi * 0.1, -0.8)
  }
  // 【电气柜】— 立柱右侧
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.5, 0.2), mat(0x777777, 0.3, 0.7)))
  root.children[root.children.length - 1].position.set(0.9, 0.7, -0.95)
  // 【电气柜面板】— 深色盖板
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.225, 0.005, 0.205), mat(0x444444, 0.5, 0.5)))
  root.children[root.children.length - 1].position.set(0.9, 0.7, -0.95)
  // 【散热风扇】— 立柱背面圆形格栅
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.01, 12), new THREE.MeshBasicMaterial({ color: 0x333333, side: THREE.DoubleSide })))
  root.children[root.children.length - 1].position.set(0.05, 0.85, -1.08)
  root.children[root.children.length - 1].rotation.x = Math.PI / 2

  // 【主机柜外壳】— 白色面板（左/右/顶/底/后 五面）
  const cabMat = mat(0xf0f0f4, 0.35, 0.5)
  const ct = 0.08
  ;[[-1.16, 1.04, -0.15, ct, 1.8, 1.35],   // 左侧板
   [1.16, 1.04, -0.15, ct, 1.8, 1.35],       // 右侧板
   [0, 1.90, -0.15, 2.24, ct, 1.35],          // 顶板
   [0, 0.18, -0.15, 2.24, ct, 1.35],          // 底板前缘
   [0, 1.04, -0.785, 2.24, 1.64, ct],         // 后板
  ].forEach(([x, y, z, w, h, d]) => {
    const wall = new THREE.Mesh(new THREE.BoxGeometry(w, h, d), cabMat)
    wall.position.set(x, y, z); wall.castShadow = true; root.add(wall)
  })

  // 【门框】— 玻璃门四周框架
  const frameMat = mat(0xf0f0f4, 0.35, 0.5)
  const ft = 0.02
  ;[[-0.975, 1.04, fz, 0.29, 1.64, ft],      // 左门框
   [1.025, 1.04, fz, 0.19, 1.64, ft],          // 右门框
   [0.05, 1.745, fz, 1.76, 0.23, ft],          // 上门框
   [0.05, 0.245, fz, 1.76, 0.05, ft],          // 下门框
  ].forEach(([x, y, z, w, h, d]) => {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(w, h, d), frameMat))
    root.children[root.children.length - 1].position.set(x, y, z)
  })

  // 【侧边散热口】— 右侧板上的竖槽
  for (let i = 0; i < 4; i++) {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(0.005, 0.15, 0.03), mat(0x2a2a2a, 0.8, 0.3)))
    root.children[root.children.length - 1].position.set(1.195, 0.7 + i * 0.28, 0.1)
  }

  // 【玻璃门】— 半透明安全玻璃
  const glassMat = new THREE.MeshPhysicalMaterial({ color: 0x88ccdd, transparent: true, opacity: 0.25, roughness: 0.02, metalness: 0.02, depthWrite: false, side: THREE.DoubleSide })
  const glass = new THREE.Mesh(new THREE.BoxGeometry(1.76, 1.36, 0.01), glassMat)
  glass.position.set(0.05, 0.95, fz); glass.renderOrder = 999; root.add(glass)

  // 【门把手】— 竖直拉手+上下固定座
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.45, 0.03), mat(0xd0d0d0, 0.05, 0.95)))
  root.children[root.children.length - 1].position.set(0.88, 0.95, fz + 0.02)
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.025, 0.04), mat(0x999999, 0.1, 0.9)))
  root.children[root.children.length - 1].position.set(0.88, 1.17, fz + 0.02)
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.025, 0.04), mat(0x999999, 0.1, 0.9)))
  root.children[root.children.length - 1].position.set(0.88, 0.73, fz + 0.02)

  // 【品牌铭牌】— 门框上方黑色底板+蓝色LED条
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.05, 0.005), mat(0x222222, 0.3, 0.6)))
  root.children[root.children.length - 1].position.set(0.05, 1.83, fz)
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.015, 0.006), new THREE.MeshBasicMaterial({ color: 0x4499ff })))
  root.children[root.children.length - 1].position.set(0.05, 1.83, fz + 0.005)

  // ══════════════════════════════════════════
  // 【设备内部】— 透过玻璃可见
  // ══════════════════════════════════════════
  // 【鞍座】— Y轴滑台底座，y=0.445顶面
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.2, 0.35), mat(0x888888, 0.3, 0.7)))
  root.children[root.children.length - 1].position.set(0.05, 0.345, 0.3)
  // 【工作台】— 台面 y=0.495顶
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.05, 0.45), mat(0xa0a0a0, 0.3, 0.8)))
  root.children[root.children.length - 1].position.set(0.05, 0.47, 0.3)
  // 【T型槽】— 5条嵌入台面
  for (let ti = -2; ti <= 2; ti++) {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(0.008, 0.008, 0.45), mat(0x444444, 0.8, 0.3)))
    root.children[root.children.length - 1].position.set(0.05 + ti * 0.15, 0.498, 0.3)
  }
  // 【虎钳底座】— 钳体 y=0.555顶
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.06, 0.2), mat(0x888888, 0.25, 0.75)))
  root.children[root.children.length - 1].position.set(0.05, 0.525, 0.3)
  // 【虎钳丝杠】— 前方水平丝杆
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.012, 0.06, 8), mat(0xcccccc, 0.08, 0.9)))
  root.children[root.children.length - 1].position.set(-0.1, 0.50, 0.3)
  root.children[root.children.length - 1].rotation.z = Math.PI / 2
  // 【虎钳固定钳口】— 后方夹块 y=0.595顶
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.04, 0.16), mat(0x666666, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(0.15, 0.575, 0.3)
  // 【虎钳活动钳口】— 前方夹块
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.04, 0.16), mat(0x777777, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(-0.07, 0.575, 0.3)
  // 【工件】— 铝块夹持在钳口中 y=0.595底面
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.08, 0.1), mat(0xc0c8d0, 0.22, 0.85)))
  root.children[root.children.length - 1].position.set(0.04, 0.590, 0.3)

  // 【Z轴滑枕】— 连接主轴电机座到立柱导轨区（防浮空）
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.12, 0.6), mat(0x707078, 0.22, 0.72)))
  root.children[root.children.length - 1].position.set(0.05, 0.88, -0.15)
  // 【主轴电机】— 圆柱形电主轴马达
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.095, 0.095, 0.2, 16), mat(0xbbbbbb, 0.2, 0.7)))
  root.children[root.children.length - 1].position.set(0.05, 0.9, 0.15)
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.09, 0.09, 0.18, 16), mat(0xd8d8d8, 0.15, 0.78)))
  root.children[root.children.length - 1].position.set(0.05, 0.9, 0.15)
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.10, 0.10, 0.015, 16), mat(0x666666, 0.3, 0.6)))
  root.children[root.children.length - 1].position.set(0.05, 1.0, 0.15)
  // 【主轴】— 高速旋转电主轴 (动画驱动)
  const spindle = new THREE.Mesh(
    new THREE.CylinderGeometry(0.07, 0.1, 0.28, 16),
    mat(c, 0.06, 0.98, g, 0.25)
  )
  spindle.position.set(0.05, 0.8, 0.15); spindle.castShadow = true; root.add(spindle)

  // 【中轴承座】— 主轴中部
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.10, 0.10, 0.035, 16), mat(0x999999, 0.1, 0.85)))
  root.children[root.children.length - 1].position.set(0.05, 0.735, 0.15)
  // 【下轴承座】— 主轴下部
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.083, 0.086, 0.022, 16), mat(0x777777, 0.08, 0.9)))
  root.children[root.children.length - 1].position.set(0.05, 0.69, 0.15)
  // 【主轴鼻锥】— 下端锥形法兰
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.08, 0.025, 16), mat(0xaaaaaa, 0.05, 0.93)))
  root.children[root.children.length - 1].position.set(0.05, 0.655, 0.15)
  // 【HSK刀柄】— 锥度插入主轴鼻锥
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.04, 0.06, 8), mat(0xaaaaaa, 0.1, 0.95)))
  root.children[root.children.length - 1].position.set(0.05, 0.62, 0.15)
  // 【铣刀】— 立铣刀，上端嵌入刀柄
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.02, 0.08, 6), mat(0x887744, 0.3, 0.8)))
  root.children[root.children.length - 1].position.set(0.05, 0.56, 0.15)

  // ══════════════════════════════════════════
  // 【操作面板】
  // ══════════════════════════════════════════
  const pnl = new THREE.Group()
  // 【面板外壳】
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.7, 0.06), mat(0x666666, 0.2, 0.7)))
  // 【屏幕边框】
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.34, 0.22, 0.008), mat(0x111111, 0.3, 0.3)))
  pnl.children[pnl.children.length - 1].position.set(0, 0.19, 0.035)
  // 【屏幕背景】
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.18, 0.005), new THREE.MeshBasicMaterial({ color: 0x226633 })))
  pnl.children[pnl.children.length - 1].position.set(0, 0.19, 0.04)
  // 【屏幕发光】
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.28, 0.16, 0.002), new THREE.MeshBasicMaterial({ color: 0x44ff88, transparent: true, opacity: 0.15 })))
  pnl.children[pnl.children.length - 1].position.set(0, 0.19, 0.043)
  // 【急停按钮】— 红色蘑菇头
  const eStop = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 0.015, 12), new THREE.MeshStandardMaterial({ color: 0xff2222, roughness: 0.3, emissive: 0xff2222, emissiveIntensity: 0.15 }))
  eStop.position.set(0.12, -0.08, 0.035); eStop.rotation.x = Math.PI / 2; pnl.add(eStop)
  // 【旋钮】— 银色超驰旋钮
  pnl.add(new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.012, 8), mat(0x888888, 0.2, 0.8)))
  pnl.children[pnl.children.length - 1].position.set(-0.12, -0.08, 0.035)
  pnl.children[pnl.children.length - 1].rotation.x = Math.PI / 2
  // 【操作按钮组】— 6个彩色按钮
  const btnColors = [0x44cc44, 0xffcc00, 0xff6644, 0x4488ff, 0x888888, 0x888888]
  btnColors.forEach((bc, bi) => {
    const col = bi % 3; const row = Math.floor(bi / 3)
    const btn = new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.008, 8),
      new THREE.MeshStandardMaterial({ color: bc, roughness: 0.3, emissive: bc, emissiveIntensity: 0.15 }))
    btn.position.set(-0.08 + col * 0.08, -0.24 + row * 0.06, 0.035)
    btn.rotation.x = Math.PI / 2; pnl.add(btn)
  })
  pnl.position.set(1.42, 1.0, 0.5); root.add(pnl)
  // 【面板支撑臂】— 连接面板到机身
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.025, 0.55, 8), mat(0x555555, 0.3, 0.7)))
  root.children[root.children.length - 1].position.set(1.20, 0.98, 0.5)

  // ══════════════════════════════════════════
  // 【塔灯】— 红/黄/绿 三色信号灯
  // ══════════════════════════════════════════
  const tower = new THREE.Group()
  tower.add(new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.04, 0.12, 8), mat(0x666666, 0.3, 0.8)))
  tower.children[0].position.y = 2.02          // 灯杆
  tower.add(new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.02, 0.08), mat(0x666666, 0.3, 0.7)))
  tower.children[1].position.set(0, 1.96, 0)   // 底座
  ;[0xff4444, 0xffcc00, 0x44cc44].forEach((tc, ti) => {
    const seg = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, 0.036, 8),
      new THREE.MeshStandardMaterial({ color: tc, emissive: tc, emissiveIntensity: 0.25, roughness: 0.3 }))
    seg.position.y = 1.94 + ti * 0.045; tower.add(seg)  // 红/黄/绿 三段灯
  })
  const led = new THREE.Mesh(
    new THREE.SphereGeometry(0.04, 8, 8),
    new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 1.0 })
  )
  led.position.y = 2.1; tower.add(led)          // 顶部LED状态球
  tower.position.set(-0.6, 0, 0.6); root.add(tower)

  // ══════════════════════════════════════════
  // 【刀库侧窗】— 左侧透明窗口+6把圆形排列刀具
  // ══════════════════════════════════════════
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.01, 0.6, 0.5),
    new THREE.MeshPhysicalMaterial({ color: 0x999999, transparent: true, opacity: 0.3, roughness: 0.05, depthWrite: false })))
  root.children[root.children.length - 1].position.set(-1.2, 1.02, 0)  // 透明窗
  for (let ti = 0; ti < 6; ti++) {
    const a = (ti / 6) * Math.PI * 2
    root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.018, 0.04, 6), mat(0x999999, 0.08, 0.92)))
    root.children[root.children.length - 1].position.set(-1.18, 1.02 + Math.sin(a) * 0.24, Math.cos(a) * 0.18)  // 刀套
    root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.008, 0.008, 0.03, 6), mat(0x887744, 0.3, 0.8)))
    root.children[root.children.length - 1].position.set(-1.17, 1.02 + Math.sin(a) * 0.24, Math.cos(a) * 0.18)  // 刀具
  }

  // ══════════════════════════════════════════
  // 【排屑槽】
  // ══════════════════════════════════════════
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.06, 0.35), mat(0x909090, 0.45, 0.7)))
  root.children[root.children.length - 1].position.set(0, 0.15, 0.75)   // 抽屉主体
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.04, 0.005), mat(0xbbbbbb, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(0, 0.15, 0.925)  // 面板
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.015, 0.02), mat(0xd0d0d0, 0.1, 0.9)))
  root.children[root.children.length - 1].position.set(0, 0.2, 0.93)    // 拉手

  // ══════════════════════════════════════════
  // 【地脚】— 4个圆形垫脚
  // ══════════════════════════════════════════
  const padGeo = new THREE.CylinderGeometry(0.1, 0.13, 0.05, 8)
  ;[[-1.1, 0.025, -0.85], [1.1, 0.025, -0.85], [-1.1, 0.025, 0.85], [1.1, 0.025, 0.85]].forEach(([px, py, pz]) => {
    root.add(new THREE.Mesh(padGeo, mat(0x707070, 0.5, 0.6)))
    root.children[root.children.length - 1].position.set(px, py, pz)
  })

  // ══════════════════════════════════════════
  // 【数据标签】— 设备名称+状态（updateNode 会实时刷新数值）
  // ══════════════════════════════════════════
  const devName = device.name || device.deviceName || device.deviceCode || '设备'
  const dataDiv = labelHtml(
    `<b>${escHtml(devName)}</b><br><span style="color:#${c.toString(16).padStart(6, '0')}">●</span> ${escHtml(TXT[st] || '')}`,
    '#' + c.toString(16).padStart(6, '0')
  )
  dataDiv.position.set(0, 2.3, 0); root.add(dataDiv)

  // 记录塔灯三色灯（红/黄/绿），供故障闪烁动画使用
  const towerLights: THREE.Mesh[] = []
  tower.children.forEach((ch, i) => {
    if (i >= 2 && ch instanceof THREE.Mesh) towerLights.push(ch)
  })

  root.userData = { device, spindle, led, label: dataDiv }
  return root
}

// ─── selection glow outline ───
function makeOutline(color: number): THREE.Group {
  const g = new THREE.Group()
  const mat = new THREE.MeshBasicMaterial({ color, wireframe: true, transparent: true, opacity: 0.55, polygonOffset: true, polygonOffsetFactor: -1, polygonOffsetUnits: -1 })
  const mesh = new THREE.Mesh(new THREE.BoxGeometry(2.7, 2.2, 2.2), mat)
  mesh.position.z = -0.05
  g.add(mesh)
  g.visible = false
  return g
}

// ══════════════════════════════════════════
// 【简化模型】— 中量设备（13~40台）：机身+玻璃+主轴+塔灯+LED，约10个Mesh
// ══════════════════════════════════════════
function buildMachineSimple(device: any): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const g = GLW[st] || 0x0
  const root = new THREE.Group()

  // 机身（两段）
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.6, 1.9, 2.0), mat(0x8a8a8a, 0.4, 0.6)))
  root.children[root.children.length - 1].position.y = 1.0
  // 顶部装饰
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.12, 1.2), mat(c, 0.3, 0.8, g, 0.15)))
  root.children[root.children.length - 1].position.y = 2.0
  // 前玻璃
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.0, 0.8, 0.02),
    new THREE.MeshPhysicalMaterial({ color: 0x88ccff, transparent: true, opacity: 0.35, roughness: 0.1, depthWrite: false })))
  root.children[root.children.length - 1].position.set(0.05, 1.2, 1.01)
  // 底座
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.14, 1.6), mat(0x505050, 0.7, 0.7)))
  root.children[root.children.length - 1].position.y = 0.07

  // 主轴（旋转动画）
  const spindle = new THREE.Mesh(
    new THREE.CylinderGeometry(0.08, 0.11, 0.3, 12),
    mat(c, 0.06, 0.98, g, 0.25)
  )
  spindle.position.set(0.05, 0.8, 0.15); root.add(spindle)

  // 塔灯（红黄绿）+ LED
  const tower = new THREE.Group()
  tower.add(new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.04, 0.12, 8), mat(0x666666, 0.3, 0.8)))
  tower.children[0].position.y = 2.02
  const towerLights: THREE.Mesh[] = []
  ;[0xff4444, 0xffcc00, 0x44cc44].forEach((tc, ti) => {
    const seg = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, 0.036, 8),
      new THREE.MeshStandardMaterial({ color: tc, emissive: tc, emissiveIntensity: 0.25, roughness: 0.3 }))
    seg.position.y = 1.94 + ti * 0.045; tower.add(seg); towerLights.push(seg)
  })
  const led = new THREE.Mesh(
    new THREE.SphereGeometry(0.04, 6, 6),
    new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 1.0 })
  )
  led.position.y = 2.1; tower.add(led)
  tower.position.set(-0.6, 0, 0.6); root.add(tower)

  root.userData = { device, spindle, led, label: null, towerLights }
  return root
}

// ══════════════════════════════════════════
// 【极简模型】— 大量设备（>40台）：机身+主轴+LED，约4个Mesh（无标签）
// ══════════════════════════════════════════
function buildMachineMinimal(device: any): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const root = new THREE.Group()

  // 单一机身（含主体色条）
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.5, 1.8, 1.9), mat(0x8a8a8a, 0.45, 0.55)))
  root.children[root.children.length - 1].position.y = 0.95
  const body = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.14, 1.1), mat(c, 0.3, 0.8))
  body.position.y = 1.9; root.add(body)

  // 主轴（旋转动画）
  const spindle = new THREE.Mesh(
    new THREE.CylinderGeometry(0.07, 0.1, 0.25, 8),
    mat(c, 0.1, 0.9)
  )
  spindle.position.set(0.05, 0.75, 0.15); root.add(spindle)

  // LED 状态球
  const led = new THREE.Mesh(
    new THREE.SphereGeometry(0.04, 6, 6),
    new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 1.0 })
  )
  led.position.set(-0.6, 2.1, 0.6); root.add(led)

  root.userData = { device, spindle, led, label: null, towerLights: null }
  return root
}

/**
 * 按设备数量调节渲染质量：
 * ≤12  全特效（阴影+2x分辨率）
 * 13~40 关闭阴影，降低分辨率
 * >40  关闭阴影 + 1x分辨率 + 关闭抗锯齿重绘
 */
function applyQualityByCount(count: number) {
  if (!S) return
  const full = count <= 12
  const medium = count <= 40
  S.webgl.shadowMap.enabled = full
  S.webgl.setPixelRatio(Math.min(window.devicePixelRatio, full ? 2 : medium ? 1.5 : 1))
  S.scene.traverse(c => {
    if (c instanceof THREE.Mesh) {
      c.castShadow = full && (c as any).userData?.castShadowBase !== false
      c.receiveShadow = full
    }
  })
}

// ─── init scene ───
function init() {
  if (!containerRef.value) return
  const dark = themeStore.isDark
  const scene = new THREE.Scene()
  scene.background = new THREE.Color(dark ? 0x2a2a2a : 0xe8e8ed)

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  const camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 80)
  camera.position.set(16, 11, 16)

  const webgl = new THREE.WebGLRenderer({ antialias: true, alpha: false })
  webgl.setSize(w, h)
  webgl.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  webgl.shadowMap.enabled = true
  webgl.shadowMap.type = THREE.PCFSoftShadowMap
  webgl.toneMapping = THREE.ACESFilmicToneMapping
  webgl.toneMappingExposure = 1.1
  webgl.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(webgl.domElement)

  const lbl = new CSS2DRenderer()
  lbl.setSize(w, h)
  lbl.domElement.style.cssText = 'position:absolute;top:0;pointer-events:none;z-index:2'
  containerRef.value.appendChild(lbl.domElement)

  const controls = new OrbitControls(camera, webgl.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.08
  controls.minDistance = 6; controls.maxDistance = 50
  controls.maxPolarAngle = Math.PI / 2.08
  controls.target.set(0, 0.5, 0); controls.update()
  controls.autoRotate = false

  buildLighting(scene, dark)

  const raycaster = new THREE.Raycaster()
  raycaster.params.Line = { threshold: 0.2 }
  const mouse = new THREE.Vector2()
  const clock = new THREE.Clock()

  selectedOutline = makeOutline(0xffcc00)
  selectedOutline.position.set(0, 1.05, 0); scene.add(selectedOutline)
  hoverOutline = makeOutline(0x88aaff)
  hoverOutline.position.set(0, 1.05, 0); scene.add(hoverOutline)

  S = { scene, camera, webgl, lbl, controls, clock, animId: 0, raycaster, mouse }
  animate()
  ready.value = true
}

// ─── refresh devices ───
function refresh(devices: any[]) {
  if (!S) return
  const count = devices?.length || 0
  if (!count) return

  // 按设备数量调节渲染质量（LOD）
  applyQualityByCount(count)

  // rebuild factory for device count if bounds changed
  const bounds = calcGridBounds(count)
  if (bounds.halfW !== lastGridHW || bounds.halfD !== lastGridHD) {
    clearFactory()
    buildFactory(count, themeStore.isDark)
  }

  deviceNodes.forEach(n => {
    n.root.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
    S.scene.remove(n.root)
  })
  deviceNodes = []
  const { ox, oz } = bounds
  devices.forEach((d: any, i: number) => {
    // LOD：≤12 精细 / 13~40 简化 / >40 极简（无标签）
    const root = count > 40 ? buildMachineMinimal(d) : count > 12 ? buildMachineSimple(d) : buildMachine(d)
    const x = ox + (i % GRID_COLS) * SX
    const z = oz + Math.floor(i / GRID_COLS) * SZ
    root.position.set(x, 0, z)
    S.scene.add(root)

    const spindle = (root.userData as any).spindle as THREE.Mesh | undefined
    const led = (root.userData as any).led as THREE.Mesh | undefined
    const label = (root.userData as any).label as CSS2DObject | undefined
    const towerLights = (root.userData as any).towerLights as THREE.Mesh[] | undefined
    deviceNodes.push({ root, spindle: spindle ?? null, led: led ?? null, label: label ?? null, towerLights: towerLights ?? [] })
  })
}

// ─── click / selection ───
function raycastDevice(clientX: number, clientY: number): THREE.Object3D | null {
  if (!S || !containerRef.value) return null
  const rect = containerRef.value.getBoundingClientRect()
  const px = ((clientX - rect.left) / rect.width) * 2 - 1
  const py = -((clientY - rect.top) / rect.height) * 2 + 1

  // multi-ray: cast within a small radius for easier targeting
  const offsets = [[0, 0], [6, 0], [-6, 0], [0, 6], [0, -6]]
  for (const [ox, oy] of offsets) {
    S.mouse.x = px + ox / rect.width * 2
    S.mouse.y = py + oy / rect.height * 2
    S.raycaster.setFromCamera(S.mouse, S.camera)
    S.scene.updateMatrixWorld(true)
    // only intersect device roots, not entire scene
    const deviceRoots = deviceNodes.map(n => n.root)
    const hits = S.raycaster.intersectObjects(deviceRoots, true)
    if (hits.length > 0) {
      let o: THREE.Object3D | null = hits[0].object
      while (o && !(o as any).userData?.device) o = o.parent
      if (o && (o as any).userData?.device) return o
    }
  }
  return null
}

function onSceneClick(e: MouseEvent) {
  if (!S) return
  const o = raycastDevice(e.clientX, e.clientY)
  if (o) {
    const dev = (o as any).userData.device
    selectedDevice.value = dev
    // 选中时若缓冲为空，用当前温度初始化趋势
    const code = dev?.code || dev?.deviceCode
    if (code && !trendBuffer.get(code)?.length && Number(dev?.temperature) > 0) {
      trendBuffer.set(code, [Number(dev.temperature)])
    }
    emit('select', dev)
    if (selectedOutline && o instanceof THREE.Group) {
      const wp = new THREE.Vector3(); o.getWorldPosition(wp)
      selectedOutline.position.copy(wp); selectedOutline.position.y = 1.05; selectedOutline.visible = true
    }
  }
}

function onMouseMove(e: MouseEvent) {
  if (!S || !hoverOutline) return
  const o = raycastDevice(e.clientX, e.clientY)
  if (o && o instanceof THREE.Group) {
    const wp = new THREE.Vector3(); o.getWorldPosition(wp)
    hoverOutline.position.copy(wp); hoverOutline.position.y = 1.05; hoverOutline.visible = true
  } else {
    hoverOutline.visible = false
  }
}

// ─── view presets ───
function flyToView(key: string) {
  if (!S) return
  currentView.value = key
  const targets: Record<string, number[]> = {
    perspective: [16, 11, 16, 0, 0.5, 0],
    top: [0, 22, 0.5, 0, 0, 0],
    front: [0, 3, 18, 0, 0.8, 0],
  }
  const [cx, cy, cz, tx, ty, tz] = targets[key] || targets.perspective
  S.camera.position.set(cx, cy, cz)
  S.controls.target.set(tx, ty, tz)
  S.controls.update()
}

// ─── animate loop ───
let animTime = 0
function animate() {
  S.animId = requestAnimationFrame(animate)
  const dt = Math.min(S.clock.getDelta(), 0.1)
  animTime += dt
  S.controls.update()

  deviceNodes.forEach(n => {
    const dev = (n.root.userData as any).device as any | undefined
    if (!dev) return
    const st = ST(dev.status)
    const temp = Number(dev.temperature) || 0
    const speed = Number(dev.speed) || 0

    // spindle rotation: 转速与真实 speed 联动（运行中越转越快，故障/离线停止）
    if (n.spindle) {
      let rotSpeed = 0.2
      if (st === 'ONLINE') rotSpeed = speed > 0 ? 2 + speed / 150 : 6
      else if (st === 'MAINTENANCE') rotSpeed = 1.5
      n.spindle.rotation.y += dt * rotSpeed
    }

    // LED: 呼吸光效（sin 波），温度越高越亮
    if (n.led) {
      const mat = n.led.material as THREE.MeshStandardMaterial
      const breathe = 0.85 + Math.sin(animTime * 3 + n.root.position.x) * 0.15
      if (st === 'FAULT') mat.emissiveIntensity = 1.0 + Math.sin(animTime * 6) * 0.3
      else if (st === 'ONLINE') mat.emissiveIntensity = (0.6 + temp / 160) * breathe
      else mat.emissiveIntensity = 0.3
    }

    // 塔灯：故障红灯闪烁 / 运行绿灯亮 / 维护黄灯亮
    if (n.towerLights?.length) {
      const [red, yellow, green] = n.towerLights
      const flash = Math.sin(animTime * 6) > 0 ? 1 : 0.12
      if (red) (red.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'FAULT' ? flash : 0.06
      if (yellow) (yellow.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'MAINTENANCE' ? 1 : 0.06
      if (green) (green.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'ONLINE' ? 0.9 + Math.sin(animTime * 2) * 0.1 : 0.06
    }
  })

  S.webgl.render(S.scene, S.camera)
  S.lbl.render(S.scene, S.camera)
}

/**
 * 增量更新设备数据（不重建模型，避免闪烁）：
 * 更新 userData 引用、CSS2D 标签数值、LED 状态色
 */
function updateNodes(devices: any[]) {
  if (!S || !devices?.length) return
  deviceNodes.forEach((n, i) => {
    const dev = devices[i]
    if (!dev) return
    const prev = (n.root.userData as any).device
    n.root.userData.device = dev

    // LED 状态色随状态变化
    const st = ST(dev.status)
    if (n.led) {
      const mat = n.led.material as THREE.MeshStandardMaterial
      const c = CLR[st] || 0x6366f1
      mat.color.setHex(c); mat.emissive.setHex(c)
    }

    // CSS2D 标签实时刷新：名称 + 状态 + 温度 + 转速
    if (n.label) {
      const c = CLR[st] || 0x6366f1
      const hex = c.toString(16).padStart(6, '0')
      const name = dev.name || dev.deviceName || dev.deviceCode || '设备'
      const temp = dev.temperature != null ? Number(dev.temperature).toFixed(1) : '--'
      const speed = dev.speed ?? '--'
      const el = (n.label as any).element as HTMLElement
      if (el) {
        el.innerHTML = `<b>${escHtml(name)}</b><br><span style="color:#${hex}">●</span> ${escHtml(TXT[st] || '')} · ${escHtml(String(temp))}°C · ${escHtml(String(speed))}rpm`
      }
    }

    // 同步选中面板（按 code 匹配最新数据，保持面板数值实时刷新）
    if (selectedDevice.value) {
      const cur = selectedDevice.value.code || selectedDevice.value.deviceCode
      const next = dev.code || dev.deviceCode
      if (cur === next) {
        selectedDevice.value = dev
      }
    }

    // 状态变化时刷新标签边框色（通过 userData 记录的原始 device 对比）
    void prev

    // 收集温度趋势缓冲（选中面板 sparkline 数据源）
    const devCode = dev.code || dev.deviceCode
    if (devCode) {
      const t = Number(dev.temperature)
      if (!isNaN(t) && t > 0) {
        const buf = trendBuffer.get(devCode) || []
        buf.push(t)
        if (buf.length > 40) buf.shift()
        trendBuffer.set(devCode, buf)
      }
    }
  })
}

let resizeObserver: ResizeObserver | null = null

// ─── resize ───
function onResize() {
  if (!containerRef.value || !S) return
  const w = containerRef.value.clientWidth; const h = containerRef.value.clientHeight
  S.camera.aspect = w / h; S.camera.updateProjectionMatrix()
  S.webgl.setSize(w, h); S.lbl.setSize(w, h)
}

// ─── theme watch ───
let lastDeviceCount = 0
watch(() => themeStore.isDark, (dark) => {
  if (!S) return
  clearFactory()
  buildFactory(lastDeviceCount || 5, dark)
  S.scene.traverse(c => {
    if (c instanceof THREE.AmbientLight) c.color.setHex(dark ? 0xbbbbbb : 0xf0f0f0)
    if (c instanceof THREE.HemisphereLight) c.color.setHex(dark ? 0x888888 : 0xaaaaaa)
  })
})

watch(() => props.devices, v => {
  if (!v?.length) return
  lastDeviceCount = v.length
  if (S && deviceNodes.length === v.length) {
    // 数量相同 → 增量更新（不重建模型，保持动画连续）
    updateNodes(v)
  } else {
    refresh(v)
  }
}, { deep: true })

onMounted(() => {
  init()
  if (props.devices?.length) refresh(props.devices)
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => onResize())
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(S?.animId)
  deviceNodes.forEach(n => {
    n.root.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
  })
  S?.webgl?.dispose()
  S?.lbl?.domElement?.remove()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.twins-scene {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 520px;
  border-radius: 0;
  overflow: hidden;
  background: var(--bg-app);
  cursor: grab;
}
.twins-scene:active { cursor: grabbing; }
.twins-loading {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 12px;
  background: rgba(0,0,0,.6); z-index: 10; color: #fff; font-size: 14px;
}
.twins-spinner {
  width: 28px; height: 28px; border: 3px solid rgba(255,255,255,.2); border-top-color: #6366f1; border-radius: 50%;
  animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* HUD badges */
.twins-hud { position: absolute; top: 10px; left: 10px; z-index: 5; display: flex; gap: 4px; }
.hud-row { display: flex; gap: 3px; }
.hud-badge {
  display: flex; align-items: center; gap: 3px; padding: 3px 9px; border-radius: 5px; font-size: 12px; font-weight: 600;
  color: #fff; backdrop-filter: blur(6px);
}
.hud-badge span { font-weight: 400; font-size: 11px; opacity: .85; }
.hud-badge.running { background: var(--success); }
.hud-badge.fault { background: var(--danger); }
.hud-badge.idle { background: var(--info); }
.hud-badge.maintenance { background: var(--warning); }
.twins-views {
  position: absolute; bottom: 10px; right: 10px; z-index: 5; display: flex; gap: 4px;
}
.view-btn {
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,.45); backdrop-filter: blur(6px); border: 1px solid rgba(255,255,255,.12); border-radius: 8px;
  color: #eee; cursor: pointer; font-size: 14px; transition: all .15s;
}
.view-btn:hover { background: rgba(99,102,241,.3); border-color: rgba(99,102,241,.5); }
.view-btn.on { background: rgba(99,102,241,.5); border-color: #6366f1; }
.twins-device-panel {
  position: absolute; bottom: 54px; right: 10px; z-index: 5;
  background: var(--bg-card);
  backdrop-filter: blur(12px); border: 1px solid var(--border-color);
  border-radius: 10px; padding: 10px 14px; min-width: 210px;
  color: var(--text-primary); font-size: 12px;
}
.panel-header { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.panel-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.panel-title-info { flex: 1; min-width: 0; }
.panel-title-info strong { font-size: 12px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.panel-code { font-size: 10px; color: var(--text-muted); }
.panel-status { padding: 1px 6px; border-radius: 4px; font-size: 10px; font-weight: 600; flex-shrink: 0; }
.panel-status.running { background: var(--success-light); color: var(--success); }
.panel-status.fault { background: var(--danger-light); color: var(--danger); }
.panel-status.idle { background: var(--info-light); color: var(--info); }
.panel-status.maintenance { background: var(--warning-light); color: var(--warning); }
.panel-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 4px 8px; }
.panel-grid div { text-align: center; }
.panel-grid label { color: var(--text-muted); font-size: 9px; display: block; margin-bottom: 1px; }
.panel-grid span { font-weight: 600; font-family: monospace; color: var(--text-primary); font-size: 12px; }
.panel-grid span.temp-ok { color: var(--success); }
.panel-grid span.temp-warn { color: var(--warning); }
.panel-grid span.temp-danger { color: var(--danger); }
.panel-bar-wrap { display: flex; align-items: center; gap: 6px; margin: 8px 0; }
.panel-trend { margin: 2px 0 8px; }
.panel-trend-label { display: block; font-size: 10px; color: var(--text-muted); margin-bottom: 2px; }
.panel-trend-svg { display: block; width: 100%; height: 36px; background: var(--bg-hover); border-radius: 4px; }
.panel-trend-empty { display: block; font-size: 10px; color: var(--text-muted); opacity: .7; padding: 8px 4px; }
.panel-bar { flex: 1; height: 4px; background: var(--border-color); border-radius: 2px; overflow: hidden; }
.panel-bar div { height: 100%; background: var(--accent); border-radius: 2px; transition: width .6s; min-width: 2px; }
.panel-bar-label { font-size: 10px; color: var(--text-muted); font-family: monospace; min-width: 30px; text-align: right; }
.panel-actions { display: flex; gap: 4px; padding-top: 6px; border-top: 1px solid var(--border-color); }
.panel-actions button { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: var(--bg-hover); border: 1px solid var(--border-light); border-radius: 5px; color: var(--text-secondary); cursor: pointer; font-size: 11px; transition: all .12s; }
.panel-actions button:hover { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.slide-up-enter-active, .slide-up-leave-active { transition: all .2s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(10px); }
</style>
