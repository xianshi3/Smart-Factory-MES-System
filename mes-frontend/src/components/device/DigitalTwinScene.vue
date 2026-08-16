<template>
  <div ref="containerRef" class="twins-scene" @click="onSceneClick" @mousemove="onMouseMove">
    <div v-if="!ready" class="twins-loading">
      <div class="twins-spinner" />
      <span>加载数字孪生...</span>
    </div>
    <!-- HUD -->
    <div class="twins-hud">
      <div class="hud-badge running">{{ statusCount.running }}<span>运行</span></div>
      <div class="hud-badge fault">{{ statusCount.fault }}<span>故障</span></div>
      <div class="hud-badge idle">{{ statusCount.idle }}<span>离线</span></div>
      <div class="hud-badge maintenance">{{ statusCount.maintenance }}<span>维护</span></div>
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
          <span class="panel-close" title="关闭" @click.stop="selectedDevice = null"><el-icon><Close /></el-icon></span>
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
import { RoundedBoxGeometry } from 'three/examples/jsm/geometries/RoundedBoxGeometry.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'
import { useThemeStore } from '@/stores/theme'

import { Cpu, Histogram, Lightning, ChatLineRound, Close } from '@element-plus/icons-vue'
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
const lastTrendX = (_arr: number[]) => trendW
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
  clock: THREE.Timer
  animId: number
  raycaster: THREE.Raycaster
  mouse: THREE.Vector2
}

let S: SceneVars
let pmrem: THREE.PMREMGenerator | null = null
let deviceNodes: { root: THREE.Group; spindle?: THREE.Mesh | null; led: THREE.Mesh | null; band?: THREE.Mesh | null; label?: CSS2DObject | null; towerLights?: THREE.Mesh[]; rot: number }[] = []
let selectedRing: THREE.Mesh | null = null
let hoverRing: THREE.Mesh | null = null
let factoryGroup = new THREE.Group()
let bgTex: THREE.CanvasTexture | null = null
let lastGridHW = 0; let lastGridHD = 0
let lastDevices: any[] = []
const currentView = ref('perspective')

// ─── 状态字典（与 MES 全局色板一致） ───
const ST = (s: string) => {
  const up = (s || 'ONLINE').toUpperCase()
  const m: Record<string, string> = { RUNNING: 'ONLINE', IDLE: 'OFFLINE', FAULT: 'FAULT', ALARM: 'FAULT', MAINTENANCE: 'MAINTENANCE', ONLINE: 'ONLINE', OFFLINE: 'OFFLINE' }
  return m[up] || up
}
// ─── 读取系统主题 CSS 变量 → THREE 颜色（与全局主题完全联动） ───
function cssVar(name: string, fallback: number): number {
  const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  if (!v) return fallback
  const hexM = v.match(/^#([0-9a-fA-F]{6})$/)
  if (hexM) return parseInt(hexM[1], 16)
  const rgbM = v.match(/^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/)
  if (rgbM) return (parseInt(rgbM[1]) << 16) | (parseInt(rgbM[2]) << 8) | parseInt(rgbM[3])
  return fallback
}

// 状态色直接取自系统主题变量（--success/--danger/--info/--warning）
const CLR: Record<string, number> = {
  ONLINE: cssVar('--success', 0x10b981),
  FAULT: cssVar('--danger', 0xef4444),
  OFFLINE: cssVar('--info', 0x06b6d4),
  MAINTENANCE: cssVar('--warning', 0xf59e0b),
}
const ACCENT = () => cssVar('--accent', 0x6366f1)
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

// ─── 主题调色板（深/浅主题全套材质色） ───
function makePalette(dark: boolean) {
  return dark ? {
    bgTop: 0x15171f, bgBottom: 0x0a0c11,
    floorCenter: 0x232833, floorEdge: 0x0a0c11,
    floorGrid: 'rgba(255,255,255,0.05)', floorGridMajor: 'rgba(255,255,255,0.09)', floorGlow: 0.10,
    pad: 0x2a303c,
    body: 0x3f4654, bodyDark: 0x2c303b, trim: 0x576070,
    metal: 0xaeb6c4, metalDark: 0x79828f, rubber: 0x14161c,
    glass: 0x9fc8e8, glassOp: 0.14, screen: 0x22d3ee,
  } : {
    bgTop: 0xd8dce7, bgBottom: 0xb8c0cf,
    floorCenter: 0x98a1b2, floorEdge: 0x7e8796,
    floorGrid: 'rgba(90,105,130,0.11)', floorGridMajor: 'rgba(90,105,130,0.22)', floorGlow: 0.05,
    pad: 0xd8dce6,
    body: 0x727c8d, bodyDark: 0x5b6474, trim: 0x4e5869,
    metal: 0xb4bdcb, metalDark: 0x7d8899, rubber: 0x2b313c,
    glass: 0xa8d4ee, glassOp: 0.26, screen: 0x22d3ee,
  }
}
type Palette = ReturnType<typeof makePalette>

const hex = (c: number) => '#' + c.toString(16).padStart(6, '0')

// ─── materials ───
function mat(c: number, r = 0.35, m = 0.45, e = 0, ei = 0, env = 0.6) {
  const mm = new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
  mm.envMapIntensity = env
  return mm
}

/** 漆面机身材质：清漆涂层 + 环境反射 */
function paint(c: number, r = 0.36, env = 0.55) {
  const mm = new THREE.MeshPhysicalMaterial({ color: c, roughness: r, metalness: 0.15, clearcoat: 0.75, clearcoatRoughness: 0.18 })
  mm.envMapIntensity = env
  return mm
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

function labelHtml(text: string, border: string, dark: boolean): CSS2DObject {
  const div = document.createElement('div')
  div.innerHTML = text
  div.style.cssText = `color:${dark ? '#eef0f6' : '#1c2333'};font-size:10px;font-weight:500;
font-family:-apple-system,'PingFang SC','Microsoft YaHei','Helvetica Neue',sans-serif;
background:${dark ? 'rgba(15,17,23,.78)' : 'rgba(255,255,255,.88)'};backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);
padding:2px 8px;border-radius:5px;border:1px solid ${border};white-space:nowrap;pointer-events:none;
box-shadow:0 1px 6px rgba(0,0,0,.18)`
  div.style.borderLeft = `2px solid ${border}`
  return new CSS2DObject(div)
}

// ─── 资源释放 ───
function disposeObject(root: THREE.Object3D) {
  root.traverse((o: any) => {
    if (o.isMesh) {
      o.geometry?.dispose?.()
      const m = o.material
      if (Array.isArray(m)) m.forEach(mm => { mm?.map?.dispose?.(); mm?.dispose?.() })
      else { m?.map?.dispose?.(); m?.dispose?.() }
    }
  })
}

// ════════════════════════════════════════════════════════════
//  空间布局（单位:米）
//   · 设备按 5 列网格排布，间距 X=5 / Z=4.5，四周留 3.5 边距
//   · 单机占地 2.4×1.9，主体高 ≈2.06，塔灯顶 ≈2.44，标签悬浮 y=2.75
//   · 地面为径向渐变 + 细网格，远端经雾融入背景渐变色
// ════════════════════════════════════════════════════════════
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
  disposeObject(factoryGroup)
  S.scene.remove(factoryGroup)
  factoryGroup = new THREE.Group()
}

// ─── 场景环境：渐变背景 + 径向渐变地板 + 细网格（简约、主题感知） ───
function buildFactory(deviceCount: number, dark: boolean) {
  clearFactory()
  const P = makePalette(dark)
  const bounds = calcGridBounds(deviceCount)
  const { halfW, halfD } = bounds
  lastGridHW = halfW; lastGridHD = halfD
  const floorW = halfW * 2 + 6; const floorD = halfD * 2 + 6

  // 背景：竖向柔和渐变
  bgTex?.dispose()
  const bgCnv = document.createElement('canvas')
  bgCnv.width = 16; bgCnv.height = 256
  const bctx = bgCnv.getContext('2d')!
  const lg = bctx.createLinearGradient(0, 0, 0, 256)
  lg.addColorStop(0, hex(P.bgTop)); lg.addColorStop(1, hex(P.bgBottom))
  bctx.fillStyle = lg; bctx.fillRect(0, 0, 16, 256)
  // 顶部主题色柔光（影棚背景感）
  const bgAccent = ACCENT()
  const gg = bctx.createRadialGradient(8, 80, 2, 8, 80, 190)
  gg.addColorStop(0, hex(bgAccent) + (dark ? '3d' : '14')); gg.addColorStop(1, 'rgba(0,0,0,0)')
  bctx.fillStyle = gg; bctx.fillRect(0, 0, 16, 256)
  bgTex = new THREE.CanvasTexture(bgCnv)
  bgTex.colorSpace = THREE.SRGBColorSpace
  S.scene.background = bgTex
  S.scene.fog = new THREE.Fog(P.bgBottom, floorW * 0.9, floorW * 2.6)

  // 地板：径向渐变（中心略亮，边缘融入雾色）+ 主色柔光 + 主次网格 + 安全区虚线周界
  const size = 1024
  const cnv = document.createElement('canvas')
  cnv.width = size; cnv.height = size
  const ctx = cnv.getContext('2d')!
  const rg = ctx.createRadialGradient(size / 2, size / 2, size * 0.04, size / 2, size / 2, size * 0.55)
  rg.addColorStop(0, hex(P.floorCenter)); rg.addColorStop(1, hex(P.floorEdge))
  ctx.fillStyle = rg; ctx.fillRect(0, 0, size, size)
  // 主色柔光（舞台光感，静态克制）
  const accent = ACCENT()
  const ag = ctx.createRadialGradient(size / 2, size / 2, size * 0.02, size / 2, size / 2, size * 0.42)
  ag.addColorStop(0, hex(accent) + (P.floorGlow > 0.08 ? '26' : '12')); ag.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = ag; ctx.fillRect(0, 0, size, size)
  // 边缘暗角（聚焦中心）
  const vg = ctx.createRadialGradient(size / 2, size / 2, size * 0.3, size / 2, size / 2, size * 0.72)
  vg.addColorStop(0, 'rgba(0,0,0,0)'); vg.addColorStop(1, dark ? 'rgba(0,0,0,0.28)' : 'rgba(70,80,100,0.18)')
  ctx.fillStyle = vg; ctx.fillRect(0, 0, size, size)
  // 主次网格（1 单位小格 / 5 单位大格，XZ 按各自尺寸缩放避免变形）
  const stepX = size / floorW; const stepY = size / floorD
  const gridLines = (every: number, style: string, lw: number) => {
    const sx = stepX * every; const sy = stepY * every
    ctx.strokeStyle = style; ctx.lineWidth = lw
    ctx.beginPath()
    for (let x = sx / 2; x < size; x += sx) { ctx.moveTo(x, 0); ctx.lineTo(x, size) }
    for (let y = sy / 2; y < size; y += sy) { ctx.moveTo(0, y); ctx.lineTo(size, y) }
    ctx.stroke()
  }
  gridLines(1, P.floorGrid, 1)
  gridLines(5, P.floorGridMajor, 1.8)
  // 安全区虚线周界（主题色描边，强化工厂规划感）
  const mx = (x: number) => (x + floorW / 2) * stepX
  const my = (y: number) => (y + floorD / 2) * stepY
  const hw2 = halfW + 0.7; const hd2 = halfD + 0.7
  ctx.setLineDash([14, 10])
  ctx.strokeStyle = hex(accent) + (dark ? '2e' : '30')
  ctx.lineWidth = 2
  ctx.strokeRect(mx(-hw2), my(-hd2), (hw2 * 2) * stepX, (hd2 * 2) * stepY)
  const floorTex = new THREE.CanvasTexture(cnv)
  floorTex.colorSpace = THREE.SRGBColorSpace
  floorTex.anisotropy = S.webgl.capabilities.getMaxAnisotropy()
  const floorMat = new THREE.MeshStandardMaterial({ map: floorTex, roughness: 0.5, metalness: 0.05 })
  floorMat.envMapIntensity = dark ? 0.75 : 0.45
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(floorW, floorD), floorMat)
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  factoryGroup.add(floor)

  S.scene.add(factoryGroup)
}

// ─── lighting ───
let lights: { amb?: THREE.AmbientLight; hemi?: THREE.HemisphereLight; key?: THREE.DirectionalLight; fill?: THREE.DirectionalLight } = {}
function buildLighting(s: THREE.Scene, dark: boolean) {
  lights.amb = new THREE.AmbientLight(0xffffff, 0.1); s.add(lights.amb)
  lights.key = new THREE.DirectionalLight(0xffffff, 2.0)
  lights.key.position.set(12, 18, 9)
  lights.key.castShadow = true
  lights.key.shadow.mapSize.set(2048, 2048)
  lights.key.shadow.camera.near = 0.5; lights.key.shadow.camera.far = 60
  lights.key.shadow.camera.left = -20; lights.key.shadow.camera.right = 20
  lights.key.shadow.camera.top = 15; lights.key.shadow.camera.bottom = -15
  lights.key.shadow.bias = -0.0001
  s.add(lights.key)
  lights.fill = new THREE.DirectionalLight(0x8899bb, 0.4)
  lights.fill.position.set(-10, 8, -10)
  s.add(lights.fill)
  lights.hemi = new THREE.HemisphereLight(0xffffff, 0x444444, 0.6)
  s.add(lights.hemi)
  tuneLights(dark)
}
function tuneLights(dark: boolean) {
  if (lights.amb) { lights.amb.color.setHex(dark ? 0x9aa2b5 : 0xffffff); lights.amb.intensity = dark ? 0.25 : 0.18 }
  if (lights.key) { lights.key.intensity = dark ? 2.0 : 2.3 }
  if (lights.fill) { lights.fill.color.setHex(dark ? 0x8899bb : 0xaabbdd); lights.fill.intensity = dark ? 0.4 : 0.35 }
  if (lights.hemi) {
    lights.hemi.color.setHex(dark ? 0x8b93a5 : 0xffffff)
    lights.hemi.groundColor.setHex(dark ? 0x23262e : 0xb6bdcb)
    lights.hemi.intensity = dark ? 0.45 : 0.55
  }
}

// ─── 零件工具 ───
function part(geo: THREE.BufferGeometry, m: THREE.Material, x = 0, y = 0, z = 0, cast = false, recv = false): THREE.Mesh {
  const ms = new THREE.Mesh(geo, m)
  ms.position.set(x, y, z)
  if (cast) ms.castShadow = true
  if (recv) ms.receiveShadow = true
  return ms
}
const rbox = (w: number, h: number, d: number, r = 0.04) => new RoundedBoxGeometry(w, h, d, 3, r)

/** 圆角矩形地台（平面，XOZ） */
function padGeo(w: number, d: number, r: number): THREE.ShapeGeometry {
  const s = new THREE.Shape()
  const hw = w / 2, hd = d / 2
  s.moveTo(-hw + r, -hd)
  s.lineTo(hw - r, -hd); s.absarc(hw - r, -hd + r, r, -Math.PI / 2, 0, false)
  s.lineTo(hw, hd - r); s.absarc(hw - r, hd - r, r, 0, Math.PI / 2, false)
  s.lineTo(-hw + r, hd); s.absarc(-hw + r, hd - r, r, Math.PI / 2, Math.PI, false)
  s.lineTo(-hw, -hd + r); s.absarc(-hw + r, -hd + r, r, Math.PI, Math.PI * 1.5, false)
  const g = new THREE.ShapeGeometry(s, 6)
  g.rotateX(-Math.PI / 2)
  return g
}

/** HMI 操作屏贴图（迷你控制界面，静态克制） */
function screenTexture(accent: number): THREE.CanvasTexture {
  const w = 96, h = 64
  const cv = document.createElement('canvas'); cv.width = w; cv.height = h
  const c = cv.getContext('2d')!
  c.fillStyle = '#0d1117'; c.fillRect(0, 0, w, h)
  c.fillStyle = '#1b2637'; c.fillRect(0, 0, w, 10)
  c.fillStyle = hex(accent); c.fillRect(4, 3, 10, 4)
  c.strokeStyle = hex(accent); c.lineWidth = 2; c.beginPath()
  for (let i = 0; i < w - 24; i += 3) {
    const y = 28 + Math.sin(i / 8) * 11
    if (i === 0) c.moveTo(8 + i, y); else c.lineTo(8 + i, y)
  }
  c.stroke()
  c.fillStyle = '#22d3ee'; c.fillRect(w - 28, 14, 12, 8)
  ;[0x10b981, 0x22d3ee, 0xf59e0b, accent].forEach((bc, bi) => {
    c.fillStyle = hex(bc)
    const bh = 7 + ((bi * 6) % 12)
    c.fillRect(10 + bi * 20, h - 16 - bh, 13, bh)
  })
  c.fillStyle = '#42526b'
  for (let i = 0; i < 3; i++) c.fillRect(6, h - 8 + i * 2, 34, 1)
  const t = new THREE.CanvasTexture(cv)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

/** 品牌铭牌贴图（深色底 + MES 字标 + 主题色块） */
function plateTexture(accent: number): THREE.CanvasTexture {
  const w = 256, h = 56
  const cv = document.createElement('canvas'); cv.width = w; cv.height = h
  const c = cv.getContext('2d')!
  c.fillStyle = '#171a20'; c.fillRect(0, 0, w, h)
  c.strokeStyle = 'rgba(255,255,255,0.14)'; c.lineWidth = 2
  c.strokeRect(3, 3, w - 6, h - 6)
  c.fillStyle = '#eef0f6'
  c.font = '600 24px -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif'
  c.textBaseline = 'middle'
  c.fillText('MES', 18, h / 2)
  c.fillStyle = hex(accent)
  c.fillRect(w - 36, h / 2 - 9, 20, 18)
  const t = new THREE.CanvasTexture(cv)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

/** 软接触阴影贴图（径向黑渐变，贴地锚定设备） */
function shadowDecal(): THREE.CanvasTexture {
  const s = 256
  const cv = document.createElement('canvas'); cv.width = s; cv.height = s
  const c = cv.getContext('2d')!
  const g = c.createRadialGradient(s / 2, s / 2, s * 0.04, s / 2, s / 2, s / 2)
  g.addColorStop(0, 'rgba(0,0,0,0.45)')
  g.addColorStop(0.65, 'rgba(0,0,0,0.18)')
  g.addColorStop(1, 'rgba(0,0,0,0)')
  c.fillStyle = g; c.fillRect(0, 0, s, s)
  const t = new THREE.CanvasTexture(cv)
  t.colorSpace = THREE.SRGBColorSpace
  return t
}

// ══════════════════════════════════════════
//  CNC 加工中心 — 简约工业风
//  tier: 0 精细(≤12台) / 1 简化(13~40台) / 2 极简(>40台)
// ══════════════════════════════════════════
function buildMachine(device: any, dark: boolean, tier: number): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const P = makePalette(dark)
  const root = new THREE.Group()
  const full = tier === 0
  const minimal = tier === 2

  // 共享材质（漆面 / 金属 / 橡胶，含环境反射强度）
  const mBody   = paint(P.body)
  const mDark   = mat(P.bodyDark, 0.6, 0.3, 0, 0, 0.35)
  const mTrim   = mat(P.trim, 0.45, 0.5, 0, 0, 0.5)
  const mMetal  = mat(P.metal, 0.22, 0.9, 0, 0, 1.0)
  const mMetalD = mat(P.metalDark, 0.32, 0.8, 0, 0, 0.9)
  const mRub    = mat(P.rubber, 0.85, 0.15, 0, 0, 0.15)
  const mBand   = new THREE.MeshStandardMaterial({ color: c, roughness: 0.4, metalness: 0.2, emissive: c, emissiveIntensity: 0.55 })
  mBand.envMapIntensity = 0.4

  // ── 地台（落影 + 点击热区） ──
  const pad = new THREE.Mesh(padGeo(2.9, 2.4, 0.14), mat(P.pad, 0.9, 0.05))
  pad.position.y = 0.012; pad.receiveShadow = true
  root.add(pad)
  // 软接触阴影（贴地，视觉锚定设备）
  const sd = new THREE.Mesh(new THREE.PlaneGeometry(2.7, 2.3),
    new THREE.MeshBasicMaterial({ map: shadowDecal(), transparent: true, opacity: dark ? 0.5 : 0.42, depthWrite: false }))
  sd.rotation.x = -Math.PI / 2
  sd.position.y = 0.016
  sd.renderOrder = 1
  root.add(sd)

  if (minimal) {
    // 【极简】机身 + 顶盖 + 状态灯带 + 状态球
    root.add(part(rbox(2.2, 1.86, 1.7, 0.06), mBody, 0, 1.13, 0, true))
    root.add(part(rbox(2.34, 0.14, 1.84, 0.05), mDark, 0, 2.13, 0, true))
    root.add(part(new THREE.BoxGeometry(1.9, 0.05, 0.03), mBand, 0, 1.86, 0.86))
    const led0 = part(new THREE.SphereGeometry(0.045, 10, 10),
      new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.9, roughness: 0.3 }), -0.8, 2.28, 0.5)
    root.add(led0)
    root.userData = { device, spindle: null, led: led0, band: root.children[2], label: null, towerLights: null }
    return root
  }

  // ── 底座 ──
  root.add(part(rbox(2.4, 0.18, 1.9, 0.04), mDark, 0, 0.11, 0, true, true))
  // ── 主体外壳（五面板 + 开放前脸，玻璃门内可见舱体） ──
  ;[[2.2, 0.08, 1.66, 0, 0.24, 0, true, true],   // 底板
   [2.2, 0.08, 1.66, 0, 1.88, 0, true, false],  // 顶板
   [0.08, 1.56, 1.66, -1.06, 1.06, 0, true, true],// 左侧板（顶/底让位，避免与顶板共面闪屏）
   [0.08, 1.56, 1.66, 1.06, 1.06, 0, true, true], // 右侧板
   [2.2, 1.8, 0.08, 0, 1.06, -0.79, true, true]] // 背板
    .forEach(([w, h, d, x, y, z, cs, rs]) =>
      root.add(part(new THREE.BoxGeometry(w, h, d), mBody, x, y, z, cs, rs)))
  // ── 顶盖（略外挑） ──
  root.add(part(rbox(2.34, 0.14, 1.84, 0.05), mDark, 0, 1.99, 0, true))

  // ── 前脸：视窗边框 + 大玻璃（真透明，可透视舱内） ──
  const fz = 0.85 // 主体前面 z
// 视窗开口 x±1.02 / y 0.28..1.84；四边深色门框（顶/底/左右互不共面，角部无闪屏）
  ;[[0, 1.76, 2.1, 0.16], [0, 0.33, 2.1, 0.12], [-0.99, 1.035, 0.12, 1.29], [0.99, 1.035, 0.12, 1.29]]
    .forEach(([x, y, w, h]) => root.add(part(new THREE.BoxGeometry(w, h, 0.06), mTrim, x, y, fz)))
  const glassMat = new THREE.MeshPhysicalMaterial({
    color: P.glass, transparent: true, opacity: P.glassOp,
    roughness: 0.12, metalness: 0, clearcoat: 0.25, clearcoatRoughness: 0.3,
    side: THREE.DoubleSide, depthWrite: false,
  })
  glassMat.envMapIntensity = 0.45
  const glass = part(new THREE.BoxGeometry(1.9, 1.28, 0.015), glassMat, 0, 1.04, fz + 0.02)
  glass.renderOrder = 999
  root.add(glass)
  if (full) {
    // 中梃（双开门感）+ 拉手
    root.add(part(new THREE.BoxGeometry(0.05, 1.28, 0.035), mTrim, 0, 1.04, fz + 0.035))
    root.add(part(new THREE.BoxGeometry(0.025, 0.28, 0.03), mMetalD, -0.09, 1.04, fz + 0.05))
    root.add(part(new THREE.BoxGeometry(0.025, 0.28, 0.03), mMetalD, 0.09, 1.04, fz + 0.05))
  }

// ══ 状态灯带（细条，贴门框顶边正面、与门同宽，随状态变色） ══
  const band = part(new THREE.BoxGeometry(2.06, 0.05, 0.006), mBand, 0, 1.705, fz + 0.032)
  root.add(band)

  // ── 系统主色饰条（主题联动点缀，与全局 --accent 一致） ──
  const accentC = ACCENT()
  root.add(part(new THREE.BoxGeometry(1.5, 0.018, 0.02), mat(accentC, 0.35, 0.1, accentC, 0.28, 0.6), 0, 0.46, fz + 0.015))

  // ── 品牌铭牌（顶盖前缘，MES 字标 + 主题色块，避开灯带/门框） ──
  if (full) {
    const ptex = plateTexture(accentC)
    root.add(part(new THREE.BoxGeometry(0.52, 0.13, 0.012),
      new THREE.MeshStandardMaterial({ map: ptex, roughness: 0.4, metalness: 0.5, emissive: 0xffffff, emissiveMap: ptex, emissiveIntensity: 0.3 }),
      -0.45, 1.96, 0.926))
  }

  // ══ 舱内（透过玻璃可见）：Y 轴进给台 + Z 轴主轴头（状态联动机械动画） ══
  // 舱内背板（浅色，增加纵深）
  if (full) root.add(part(new THREE.BoxGeometry(1.5, 1.0, 0.03), mat(P.metal, 0.5, 0.35, 0, 0, 0.5), 0, 1.08, -0.42))
  // Y 轴进给台：工作台 + 工件（运行时节拍进给）
  const tableG = new THREE.Group()
  tableG.add(part(new THREE.BoxGeometry(0.95, 0.07, 0.55), mMetal, 0, 0.62, 0.1))
  if (full) tableG.add(part(new THREE.BoxGeometry(0.16, 0.1, 0.14), mMetalD, -0.18, 0.71, 0.1))
  root.add(tableG)
  // Z 轴主轴头：头座 + 旋转主轴 + 刀尖（运行时分段进刀/抬刀）
  const headG = new THREE.Group()
  headG.add(part(rbox(0.36, 0.42, 0.36, 0.04), mMetalD, 0, 0, 0))
  const spindle = part(new THREE.CylinderGeometry(0.055, 0.085, 0.34, 14), mMetal, 0, -0.37, 0)
  headG.add(spindle)
  if (full) headG.add(part(new THREE.CylinderGeometry(0.02, 0.035, 0.12, 8), mMetalD, 0, -0.6, 0))
  headG.position.set(0.05, 1.32, 0.05)
  root.add(headG)

  // ── 舱内照明：工作灯条 + 点光源（玻璃透出舱内细节） ──
  const wl = part(new THREE.PlaneGeometry(1.3, 0.16),
    new THREE.MeshBasicMaterial({ color: 0xf0f8ff, transparent: true, opacity: 0.5 }), 0, 1.5, 0.42)
  wl.rotation.x = -Math.PI / 2 + 0.42
  wl.renderOrder = 998
  root.add(wl)
  if (full) {
    const pl = new THREE.PointLight(0xfff2dd, 2.2, 2.6)
    pl.position.set(0.05, 1.32, 0.05)
    root.add(pl)
  }

  // ── 右侧通风槽 ──
  if (full) {
    for (let i = 0; i < 4; i++) {
      root.add(part(new THREE.BoxGeometry(0.012, 0.22, 0.045), mRub, 1.098, 1.15, -0.55 + i * 0.13))
    }
  }

  // ── 操作面板（立柱 + 箱体 + 屏幕） ──
  root.add(part(new THREE.CylinderGeometry(0.02, 0.025, 0.5, 8), mMetalD, 1.14, 0.8, 0.5))
  const pnl = new THREE.Group()
  pnl.add(part(rbox(0.34, 0.46, 0.08, 0.03), mDark, 0, 0, 0, true))
  const stex = screenTexture(ACCENT())
  pnl.add(part(new THREE.BoxGeometry(0.24, 0.15, 0.02),
    new THREE.MeshStandardMaterial({ map: stex, roughness: 0.35, emissive: 0xffffff, emissiveMap: stex, emissiveIntensity: 0.85 }), 0, 0.11, 0.045))
  if (full) {
    // 急停 + 两个操作钮
    const eStop = part(new THREE.CylinderGeometry(0.028, 0.028, 0.02, 10),
      new THREE.MeshStandardMaterial({ color: 0xef4444, roughness: 0.35, emissive: 0xef4444, emissiveIntensity: 0.25 }), 0.08, -0.12, 0.045)
    eStop.rotation.x = Math.PI / 2
    pnl.add(eStop)
    ;[[-0.04, 0x10b981], [-0.12, 0x94a3b8]].forEach(([bx, bc]) => {
      const b = part(new THREE.CylinderGeometry(0.016, 0.016, 0.014, 8),
        new THREE.MeshStandardMaterial({ color: bc as number, roughness: 0.4, emissive: bc as number, emissiveIntensity: 0.2 }), bx as number, -0.12, 0.045)
      b.rotation.x = Math.PI / 2
      pnl.add(b)
    })
  }
  pnl.position.set(1.3, 1.1, 0.5)
  root.add(pnl)

  // ── 塔灯（红/黄/绿 + 顶部状态球），置于顶盖前左角 ──
  const tower = new THREE.Group()
  tower.add(part(new THREE.CylinderGeometry(0.05, 0.06, 0.03, 10), mRub, 0, 2.075, 0))
  tower.add(part(new THREE.CylinderGeometry(0.012, 0.012, 0.16, 8), mMetalD, 0, 2.16, 0))
  const towerLights: THREE.Mesh[] = []
  ;[0xef4444, 0xf59e0b, 0x10b981].forEach((tc, ti) => {
    const seg = part(new THREE.CylinderGeometry(0.032, 0.032, 0.05, 10),
      new THREE.MeshStandardMaterial({ color: tc, emissive: tc, emissiveIntensity: 0.08, roughness: 0.35 }), 0, 2.27 + ti * 0.055, 0)
    tower.add(seg); towerLights.push(seg)
  })
  const led = part(new THREE.SphereGeometry(0.036, 10, 10),
    new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.9, roughness: 0.3 }), 0, 2.44, 0)
  tower.add(led)
  tower.position.set(-0.8, 0, 0.5)
  root.add(tower)

  // ── 地脚 ──
  if (full) {
    const footGeo = new THREE.CylinderGeometry(0.07, 0.09, 0.06, 10)
    ;[[-1.02, -0.82], [1.02, -0.82], [-1.02, 0.82], [1.02, 0.82]].forEach(([fx, fz2]) => {
      root.add(part(footGeo, mRub, fx, 0.03, fz2))
    })
  }

  // ── 数据标签（主题感知，updateNode 实时刷新） ──
  const devName = device.name || device.deviceName || device.deviceCode || '设备'
  const dataDiv = labelHtml(
    `<b>${escHtml(devName)}</b><br><span style="color:${hex(c)}">●</span> ${escHtml(TXT[st] || '')}`,
    hex(c), dark
  )
  dataDiv.position.set(0, 2.75, 0)
  root.add(dataDiv)

  root.userData = { device, spindle, led, band, label: dataDiv, towerLights, headG, tableG }
  return root
}

// ─── 选中 / 悬停：地面圆环（简约） ───
function makeRing(color: number, opacity: number): THREE.Mesh {
  const geo = new THREE.RingGeometry(1.58, 1.72, 56)
  geo.rotateX(-Math.PI / 2)
  const m = new THREE.Mesh(geo, new THREE.MeshBasicMaterial({ color, transparent: true, opacity, side: THREE.DoubleSide, depthWrite: false }))
  m.position.y = 0.025
  m.renderOrder = 3
  m.visible = false
  return m
}

/**
 * 按设备数量调节渲染质量：
 * ≤12  全特效（阴影+2x分辨率）
 * 13~40 关闭阴影，降低分辨率
 * >40  关闭阴影 + 1x分辨率
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

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  const camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 80)
  camera.position.set(15, 10.5, 15)

  const webgl = new THREE.WebGLRenderer({ antialias: true, alpha: false })
  webgl.setSize(w, h)
  webgl.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  webgl.shadowMap.enabled = true
  webgl.shadowMap.type = THREE.PCFShadowMap
  webgl.toneMapping = THREE.ACESFilmicToneMapping
  webgl.toneMappingExposure = 1.1
  webgl.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(webgl.domElement)

  // 环境反射（室内影棚环境光，金属/漆面/玻璃获得真实高光）
  pmrem = new THREE.PMREMGenerator(webgl)
  scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture

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
  const clock = new THREE.Timer()

  selectedRing = makeRing(ACCENT(), 0.9); scene.add(selectedRing)
  hoverRing = makeRing(ACCENT(), 0.4); scene.add(hoverRing)

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
    buildFactory(count, themeStore.isDark)
  }

  deviceNodes.forEach(n => {
    n.root.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
    disposeObject(n.root)
    S.scene.remove(n.root)
  })
  deviceNodes = []
  const { ox, oz } = bounds
  const dark = themeStore.isDark
  devices.forEach((d: any, i: number) => {
    // LOD：≤12 精细 / 13~40 简化 / >40 极简（无标签）
    const root = buildMachine(d, dark, count > 40 ? 2 : count > 12 ? 1 : 0)
    const x = ox + (i % GRID_COLS) * SX
    const z = oz + Math.floor(i / GRID_COLS) * SZ
    root.position.set(x, 0, z)
    S.scene.add(root)

    const u = root.userData as any
    deviceNodes.push({
      root,
      spindle: (u.spindle as THREE.Mesh) ?? null,
      led: (u.led as THREE.Mesh) ?? null,
      band: (u.band as THREE.Mesh) ?? null,
      label: (u.label as CSS2DObject) ?? null,
      towerLights: (u.towerLights as THREE.Mesh[]) ?? [],
      rot: 0,
    })
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
    if (selectedRing && o instanceof THREE.Group) {
      const wp = new THREE.Vector3(); o.getWorldPosition(wp)
      selectedRing.position.set(wp.x, 0.025, wp.z); selectedRing.visible = true
    }
  }
}

function onMouseMove(e: MouseEvent) {
  if (!S || !hoverRing) return
  const o = raycastDevice(e.clientX, e.clientY)
  if (o && o instanceof THREE.Group) {
    const wp = new THREE.Vector3(); o.getWorldPosition(wp)
    hoverRing.position.set(wp.x, 0.025, wp.z); hoverRing.visible = true
  } else {
    hoverRing.visible = false
  }
}

// ─── view presets ───
function flyToView(key: string) {
  if (!S) return
  currentView.value = key
  const targets: Record<string, number[]> = {
    perspective: [15, 10.5, 15, 0, 0.5, 0],
    top: [0, 22, 0.5, 0, 0, 0],
    front: [0, 3, 18, 0, 0.8, 0],
  }
  const [cx, cy, cz, tx, ty, tz] = targets[key] || targets.perspective
  S.camera.position.set(cx, cy, cz)
  S.controls.target.set(tx, ty, tz)
  S.controls.update()
}

// ─── animate loop（动效克制：仅主轴联动旋转 + 故障慢闪） ───
let animTime = 0
function animate() {
  S.animId = requestAnimationFrame(animate)
  S.clock.update()
  const dt = Math.min(S.clock.getDelta(), 0.1)
  animTime += dt
  S.controls.update()

  deviceNodes.forEach(n => {
    const dev = (n.root.userData as any).device as any | undefined
    if (!dev) return
    const st = ST(dev.status)
    const speed = Number(dev.speed) || 0

    // 主轴旋转：与真实转速联动（加减速平滑，故障/离线停止）
    if (n.spindle) {
      let target = 0.2
      if (st === 'ONLINE') target = speed > 0 ? 2 + speed / 150 : 6
      else if (st === 'MAINTENANCE') target = 1.5
      else target = 0
      n.rot += (target - n.rot) * Math.min(1, dt * 3)
      n.spindle.rotation.y += dt * n.rot
    }
    // Z 轴主轴头：运行→进刀/抬刀加工节拍；非运行→停在高位（平滑过渡）
    const hg = (n.root.userData as any).headG as THREE.Group | undefined
    if (hg) {
      const targetY = st === 'ONLINE' ? 1.39 + Math.sin(animTime * 0.8 + n.root.position.x) * 0.06 : 1.4
      hg.position.y += (targetY - hg.position.y) * Math.min(1, dt * 2.5)
    }
    // Y 轴进给台：运行→横向往复进给；非运行→回中
    const tg = (n.root.userData as any).tableG as THREE.Group | undefined
    if (tg) {
      const targetX = st === 'ONLINE' ? Math.sin(animTime * 0.55 + n.root.position.x * 1.7) * 0.05 : 0
      tg.position.x += (targetX - tg.position.x) * Math.min(1, dt * 2)
    }

    // 顶部状态球：常亮，故障慢闪（克制）
    if (n.led) {
      const lm = n.led.material as THREE.MeshStandardMaterial
      if (st === 'FAULT') lm.emissiveIntensity = Math.sin(animTime * 4) > 0 ? 1.0 : 0.25
      else if (st === 'ONLINE') lm.emissiveIntensity = 0.9
      else if (st === 'MAINTENANCE') lm.emissiveIntensity = 0.7
      else lm.emissiveIntensity = 0.25
    }

    // 前脸灯带：常亮，故障缓慢明暗
    if (n.band) {
      const bm = n.band.material as THREE.MeshStandardMaterial
      bm.emissiveIntensity = st === 'FAULT' ? 0.45 + 0.3 * (0.5 + 0.5 * Math.sin(animTime * 3)) : 0.55
    }

    // 塔灯：故障红灯闪 / 运行绿灯亮 / 维护黄灯亮
    if (n.towerLights?.length) {
      const [red, yellow, green] = n.towerLights
      const flash = Math.sin(animTime * 4) > 0 ? 0.9 : 0.15
      if (red) (red.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'FAULT' ? flash : 0.08
      if (yellow) (yellow.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'MAINTENANCE' ? 0.85 : 0.08
      if (green) (green.material as THREE.MeshStandardMaterial).emissiveIntensity = st === 'ONLINE' ? 0.85 : 0.08
    }
  })

  S.webgl.render(S.scene, S.camera)
  S.lbl.render(S.scene, S.camera)
}

/**
 * 增量更新设备数据（不重建模型，避免闪烁）：
 * 更新 userData 引用、CSS2D 标签数值、LED/灯带状态色
 */
function updateNodes(devices: any[]) {
  if (!S || !devices?.length) return
  deviceNodes.forEach((n, i) => {
    const dev = devices[i]
    if (!dev) return
    n.root.userData.device = dev

    const st = ST(dev.status)
    const c = CLR[st] || 0x6366f1

    // 状态球色
    if (n.led) {
      const m = n.led.material as THREE.MeshStandardMaterial
      m.color.setHex(c); m.emissive.setHex(c)
    }
    // 前脸灯带色
    if (n.band) {
      const m = n.band.material as THREE.MeshStandardMaterial
      m.color.setHex(c); m.emissive.setHex(c)
    }

    // CSS2D 标签实时刷新：名称 + 状态 + 温度 + 转速
    if (n.label) {
      const hx = hex(c)
      const name = dev.name || dev.deviceName || dev.deviceCode || '设备'
      const temp = dev.temperature != null ? Number(dev.temperature).toFixed(1) : '--'
      const speed = dev.speed ?? '--'
      const el = (n.label as any).element as HTMLElement
      if (el) {
        el.innerHTML = `<b>${escHtml(name)}</b><br><span style="color:${hx}">●</span> ${escHtml(TXT[st] || '')} · ${escHtml(String(temp))}°C · ${escHtml(String(speed))}rpm`
        el.style.borderColor = hx
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

// ─── theme watch：重建环境 + 灯光 + 设备模型（全套材质随主题切换） ───
watch(() => themeStore.isDark, (dark) => {
  if (!S) return
  buildFactory(lastDevices.length || 5, dark)
  tuneLights(dark)
  if (lastDevices.length) refresh(lastDevices)
})

watch(() => props.devices, v => {
  if (!v?.length) return
  lastDevices = v
  if (S && deviceNodes.length === v.length) {
    // 数量相同 → 增量更新（不重建模型，保持动画连续）
    updateNodes(v)
  } else {
    refresh(v)
  }
}, { deep: true })

onMounted(() => {
  init()
  if (props.devices?.length) {
    lastDevices = props.devices
    refresh(props.devices)
  }
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => onResize())
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(S?.animId)
  deviceNodes.forEach(n => {
    n.root.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
    disposeObject(n.root)
  })
  disposeObject(factoryGroup)
  bgTex?.dispose()
  pmrem?.dispose()
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
  background: var(--bg-app); opacity: .92; z-index: 10; color: var(--text-secondary); font-size: 14px;
}
.twins-spinner {
  width: 28px; height: 28px; border: 3px solid var(--border-color); border-top-color: var(--accent); border-radius: 50%;
  animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* HUD 状态徽章（分组玻璃胶囊 + 细分隔线，主题感知） */
.twins-hud { position: absolute; top: 14px; left: 14px; z-index: 5; display: flex; gap: 0; padding: 3px; border-radius: 999px; background: color-mix(in srgb, var(--bg-card) 72%, transparent); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px); border: 1px solid var(--border-color); box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.hud-badge {
  position: relative; display: flex; align-items: center; gap: 6px; padding: 5px 13px; border-radius: 999px;
  font-size: 13px; font-weight: 700; font-variant-numeric: tabular-nums; letter-spacing: .2px;
  color: var(--text-secondary); transition: background .15s ease;
}
.hud-badge:hover { background: var(--bg-hover); }
.hud-badge + .hud-badge::after { content: ''; position: absolute; left: 0; top: 22%; bottom: 22%; width: 1px; background: var(--border-color); }
.hud-badge::before { content: ''; width: 7px; height: 7px; border-radius: 50%; background: currentColor; opacity: .9; flex-shrink: 0; }
.hud-badge span { font-weight: 400; font-size: 11px; opacity: .75; }
.hud-badge.running { color: var(--success); }
.hud-badge.fault { color: var(--danger); }
.hud-badge.idle { color: var(--info); }
.hud-badge.maintenance { color: var(--warning); }

/* 视角切换（底部居中 dock，玻璃质感，不与设备面板重叠） */
.twins-views { position: absolute; bottom: 14px; left: 50%; transform: translateX(-50%); z-index: 5; display: flex; flex-direction: row; gap: 7px; }
.view-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, var(--bg-card) 80%, transparent);
  backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--border-color); border-radius: 11px;
  color: var(--text-secondary); cursor: pointer; font-size: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
  transition: all .15s;
}
.view-btn:hover { color: var(--accent); border-color: var(--accent); transform: translateY(-1px); }
.view-btn.on { background: var(--accent); border-color: transparent; color: #fff; box-shadow: 0 4px 14px color-mix(in srgb, var(--accent) 40%, transparent); }

/* 设备信息面板（玻璃卡片，主题感知） */
.twins-device-panel {
  position: absolute; bottom: 62px; right: 14px; z-index: 5; width: 282px;
  background: color-mix(in srgb, var(--bg-card) 88%, transparent);
  backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-color); border-radius: 16px;
  padding: 14px 16px 12px;
  color: var(--text-primary); font-size: 12.5px;
  box-shadow: 0 12px 32px rgba(0,0,0,.16), 0 2px 8px rgba(0,0,0,.08);
}
.panel-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; padding-left: 9px; position: relative; }
.panel-header::before { content: ''; position: absolute; left: 0; top: 3px; bottom: 3px; width: 3.5px; border-radius: 2px; background: var(--accent); }
.panel-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.panel-title-info { flex: 1; min-width: 0; }
.panel-title-info strong { font-size: 14px; display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.panel-code { font-size: 11px; color: var(--text-muted); font-family: monospace; }
.panel-status { padding: 2.5px 9px; border-radius: 999px; font-size: 10.5px; font-weight: 600; flex-shrink: 0; }
.panel-status.running { background: color-mix(in srgb, var(--success) 14%, transparent); color: var(--success); }
.panel-status.fault { background: color-mix(in srgb, var(--danger) 14%, transparent); color: var(--danger); }
.panel-status.idle { background: color-mix(in srgb, var(--info) 14%, transparent); color: var(--info); }
.panel-status.maintenance { background: color-mix(in srgb, var(--warning) 14%, transparent); color: var(--warning); }
.panel-close { display: flex; align-items: center; justify-content: center; width: 20px; height: 20px; border-radius: 6px; color: var(--text-muted); cursor: pointer; flex-shrink: 0; transition: all .12s; }
.panel-close:hover { background: var(--bg-hover); color: var(--text-primary); }
.panel-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; }
.panel-grid div { text-align: center; background: var(--bg-hover); border-radius: 10px; padding: 7px 4px 8px; }
.panel-grid label { color: var(--text-muted); font-size: 10px; display: block; margin-bottom: 3px; }
.panel-grid span { font-weight: 600; font-family: monospace; color: var(--text-primary); font-size: 14px; }
.panel-grid span.temp-ok { color: var(--success); }
.panel-grid span.temp-warn { color: var(--warning); }
.panel-grid span.temp-danger { color: var(--danger); }
.panel-bar-wrap { display: flex; align-items: center; gap: 6px; margin: 11px 0 6px; }
.panel-trend { margin: 5px 0 10px; }
.panel-trend-label { display: block; font-size: 10.5px; color: var(--text-muted); margin-bottom: 5px; }
.panel-trend-svg { display: block; width: 100%; height: 40px; background: var(--bg-hover); border: 1px solid var(--border-light); border-radius: 9px; }
.panel-trend-empty { display: block; font-size: 10.5px; color: var(--text-muted); opacity: .7; padding: 10px 4px; }
.panel-bar { flex: 1; height: 6px; background: var(--border-color); border-radius: 3px; overflow: hidden; }
.panel-bar div { height: 100%; background: var(--gradient-primary); border-radius: 3px; transition: width .6s; min-width: 2px; }
.panel-bar-label { font-size: 10.5px; color: var(--text-muted); font-family: monospace; min-width: 32px; text-align: right; }
.panel-actions { display: flex; gap: 6px; padding-top: 10px; border-top: 1px solid var(--border-color); }
.panel-actions button { flex: 1; height: 32px; display: flex; align-items: center; justify-content: center; background: var(--bg-hover); border: 1px solid var(--border-light); border-radius: 9px; color: var(--text-secondary); cursor: pointer; font-size: 15px; transition: all .12s; }
.panel-actions button :deep(svg) { width: 15px; height: 15px; }
.panel-actions button:hover { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.slide-up-enter-active, .slide-up-leave-active { transition: all .2s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(10px); }
</style>
