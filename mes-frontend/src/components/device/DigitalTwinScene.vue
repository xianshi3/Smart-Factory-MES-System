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
      <button v-for="v in viewPresets" :key="v.key" class="view-btn" :class="{ on: currentView === v.key }" @click="flyToView(v.key)" :title="v.label">
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
            <span :class="tempClass(selectedDevice.temperature)">{{ selectedDevice.temperature ?? '--' }}°C</span>
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
        <div class="panel-actions">
          <button @click="emit('action', { type:'predict', device: selectedDevice })" title="AI预测"><Cpu /></button>
          <button @click="emit('action', { type:'spc', device: selectedDevice })" title="SPC分析"><Histogram /></button>
          <button @click="emit('action', { type:'energy', device: selectedDevice })" title="能耗优化"><Lightning /></button>
          <button @click="emit('action', { type:'llm', device: selectedDevice })" title="AI建议"><ChatLineRound /></button>
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
import { Top, View, Connection } from '@element-plus/icons-vue'
import { Cpu, Histogram, Lightning, ChatLineRound } from '@element-plus/icons-vue'
const themeStore = useThemeStore()

const props = defineProps<{ devices: any[] }>()
const emit = defineEmits<{ select: [device: any]; action: [payload: { type: string; device: any }] }>()
const containerRef = ref<HTMLElement>()
const ready = ref(false)
const selectedDevice = ref<any>(null)

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
let deviceNodes: { root: THREE.Group; spindle?: THREE.Mesh | null; led: THREE.Mesh | null; pulseRing: THREE.Mesh | null; glowRing: THREE.Mesh | null }[] = []
let selectedOutline: THREE.Group | null = null
let hoverOutline: THREE.Group | null = null
let runTime = 0
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

  const bg = dark ? 0x14141a : 0xe8e8ed
  S.scene.background = new THREE.Color(bg)
  S.scene.fog = new THREE.Fog(bg, floorW * 0.8, floorW * 2.2)

  // floor with tile pattern
  const floorGeo = new THREE.PlaneGeometry(floorW, floorD)
  const floorCanvas = document.createElement('canvas')
  floorCanvas.width = 512; floorCanvas.height = Math.round(512 * floorD / floorW)
  const ctx = floorCanvas.getContext('2d')!
  ctx.fillStyle = dark ? '#1a1a22' : '#d8d8e0'
  ctx.fillRect(0, 0, floorCanvas.width, floorCanvas.height)
  ctx.strokeStyle = dark ? '#22222e' : '#c0c0cc'
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
  const colMat = mat(dark ? 0x2a2a38 : 0xccccd8, 0.5, 0.6)
  const colCX = halfW - 0.3; const colCZ = halfD - 0.3
  ;[[-colCX, 1.5, -colCZ], [colCX, 1.5, -colCZ], [-colCX, 1.5, colCZ], [colCX, 1.5, colCZ]].forEach(([x, y, z]) => {
    const col = new THREE.Mesh(colGeo, colMat)
    col.position.set(x, y, z); col.castShadow = true; factoryGroup.add(col)
  })

  // ceiling truss lines — semi-transparent, unobtrusive
  const trussGeoX = new THREE.BoxGeometry(0.02, 0.02, halfD * 2 + 2)
  const trussGeoZ = new THREE.BoxGeometry(halfW * 2 + 2, 0.02, 0.02)
  const trussMat = new THREE.MeshStandardMaterial({ color: dark ? 0x2a2a38 : 0xb8b8c8, roughness: 0.5, metalness: 0.3, transparent: true, opacity: 0.25 })
  const trussY = 3.2
  ;[-halfW + 1, halfW - 1, -halfW + 3, halfW - 3].forEach(x => {
    const t = new THREE.Mesh(trussGeoX, trussMat); t.position.set(x, trussY, 0); t.renderOrder = 1; factoryGroup.add(t)
  })
  ;[-halfD + 1, halfD - 1, -halfD + 3, halfD - 3].forEach(z => {
    const t = new THREE.Mesh(trussGeoZ, trussMat); t.position.set(0, trussY, z); t.renderOrder = 1; factoryGroup.add(t)
  })

  // low walls around perimeter
  const wallH = 1.5
  const wallMat = mat(dark ? 0x1e1e2c : 0xb8b8c8, 0.8, 0.1)
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
  s.add(new THREE.AmbientLight(dark ? 0x33334a : 0xbbbbdd, 0.5))
  const key = new THREE.DirectionalLight(dark ? 0xffeedd : 0xffffff, 1.8)
  key.position.set(14, 20, 10)
  key.castShadow = true
  key.shadow.mapSize.set(2048, 2048)
  key.shadow.camera.near = 0.5; key.shadow.camera.far = 60
  key.shadow.camera.left = -20; key.shadow.camera.right = 20
  key.shadow.camera.top = 15; key.shadow.camera.bottom = -15
  key.shadow.bias = -0.0001
  s.add(key)
  const fill = new THREE.DirectionalLight(0x8899cc, 0.35)
  fill.position.set(-10, 8, -8)
  s.add(fill)
  s.add(new THREE.HemisphereLight(dark ? 0x4466aa : 0xaaccff, dark ? 0x222238 : 0x8888aa, 0.25))
}

// ─── CNC machining center (vertical mill) ───
function buildMachine(device: any): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const g = GLW[st] || 0x0
  const root = new THREE.Group()

  // ── base ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.6, 0.14, 2.0), mat(0x707070, 0.4, 0.65)))
  root.children[root.children.length - 1].position.y = 0.07

  // ── main cabinet (silver-grey industrial) ──
  const body = new THREE.Mesh(new THREE.BoxGeometry(2.4, 1.8, 1.7), mat(0xc0c4cc, 0.08, 0.7))
  body.position.y = 1.04; body.castShadow = true; root.add(body)

  // ── top accent band ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.38, 0.06, 1.68), mat(0x3a3a3a, 0.2, 0.75)))
  root.children[root.children.length - 1].position.set(0, 1.96, 0)

  // ── rear cabinet ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.0, 1.4, 0.3), mat(0x8a8a8a, 0.2, 0.65)))
  root.children[root.children.length - 1].position.set(0.05, 0.95, -0.95)

  // vent slats
  for (let i = 0; i < 5; i++) {
    root.add(new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.015, 0.015), mat(0x555555, 0.5, 0.5)))
    root.children[root.children.length - 1].position.set(0.05, 0.5 + i * 0.18, -1.1)
  }

  // ── front door ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.9, 1.5, 0.015), mat(0x4a4a4a, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(0.05, 0.95, 0.86)

  // door border
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.92, 1.52, 0.01), mat(0xa0a0a0, 0.1, 0.9)))
  root.children[root.children.length - 1].position.set(0.05, 0.95, 0.85)

  // glass window
  const glass = new THREE.Mesh(
    new THREE.BoxGeometry(1.3, 1.0, 0.005),
    new THREE.MeshPhysicalMaterial({ color: 0x88ccff, transparent: true, opacity: 0.3, roughness: 0.02, metalness: 0.05 })
  )
  glass.position.set(0.05, 1.0, 0.87); root.add(glass)

  // door handles
  ;[-0.5, 0.6].forEach(hx => {
    const h = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.02, 0.6, 8), mat(0xbcc0c8, 0.1, 0.92))
    h.position.set(hx, 0.92, 0.88); root.add(h)
  })

  // ── interior ──
  // table
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.0, 0.06, 0.5), mat(0x707680, 0.2, 0.95)))
  root.children[root.children.length - 1].position.set(0.05, 0.5, 0.55)
  // vise
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.45, 0.07, 0.18), mat(0x808690, 0.15, 0.93)))
  root.children[root.children.length - 1].position.set(0.05, 0.56, 0.55)
  // workpiece
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.14, 0.16), mat(0xeeaa55, 0.5, 0.3)))
  root.children[root.children.length - 1].position.set(0.05, 0.68, 0.55)

  // spindle (animated)
  const spindle = new THREE.Mesh(
    new THREE.CylinderGeometry(0.08, 0.11, 0.3, 12),
    mat(c, 0.06, 0.98, g, 0.25)
  )
  spindle.position.set(0.05, 0.82, 0.38); spindle.castShadow = true; root.add(spindle)
  // tool
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.055, 0.12, 8), mat(0xa8acb4, 0.1, 0.9)))
  root.children[root.children.length - 1].position.set(0.05, 0.62, 0.38)

  // ── control panel (right side, attached to cabinet) ──
  const pnl = new THREE.Group()
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.7, 0.06), mat(0x4a4a4a, 0.2, 0.75)))
  // screen
  pnl.add(new THREE.Mesh(new THREE.BoxGeometry(0.32, 0.24, 0.005), new THREE.MeshBasicMaterial({ color: 0x20d050 })))
  pnl.children[pnl.children.length - 1].position.set(0, 0.18, 0.035)
  // button row
  ;[0xff4444, 0xffcc00, 0x44cc44].forEach((bc, bi) => {
    const btn = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.02, 0.01, 8), new THREE.MeshStandardMaterial({ color: bc, roughness: 0.2, emissive: bc, emissiveIntensity: 0.3 }))
    btn.position.set(-0.1 + bi * 0.1, -0.1, 0.035); btn.rotation.x = Math.PI / 2; pnl.add(btn)
  })
  pnl.position.set(1.26, 1.0, 0.5); root.add(pnl)

  // ── stack light ──
  const tower = new THREE.Group()
  tower.add(new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.04, 0.12, 8), mat(0x4a4a4a, 0.3, 0.82)))
  tower.children[0].position.y = 2.02
  ;[0xff4444, 0xffcc00, 0x44cc44].forEach((tc, ti) => {
    const seg = new THREE.Mesh(new THREE.CylinderGeometry(0.035, 0.035, 0.036, 8),
      new THREE.MeshStandardMaterial({ color: tc, emissive: tc, emissiveIntensity: 0.25, roughness: 0.3 }))
    seg.position.y = 1.94 + ti * 0.045; tower.add(seg)
  })
  const led = new THREE.Mesh(
    new THREE.SphereGeometry(0.04, 8, 8),
    new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 1.0 })
  )
  led.position.y = 2.1; tower.add(led)
  tower.position.set(-0.6, 0, 0.6); root.add(tower)

  // ── side window (tool magazine) ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.01, 0.6, 0.5),
    new THREE.MeshPhysicalMaterial({ color: 0x8899cc, transparent: true, opacity: 0.3, roughness: 0.05 })))
  root.children[root.children.length - 1].position.set(-1.2, 1.02, 0)
  // magazine dots
  for (let ti = 0; ti < 6; ti++) {
    const a = (ti / 6) * Math.PI * 2
    const dot = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.018, 0.02, 6), mat(0xb4b8c0, 0.08, 0.92))
    dot.position.set(-1.18, 1.02 + Math.sin(a) * 0.24, Math.cos(a) * 0.18); root.add(dot)
  }

  // ── chip drawer ──
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.8, 0.06, 0.35), mat(0x767a82, 0.45, 0.7)))
  root.children[root.children.length - 1].position.set(0, 0.15, 0.75)
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.0, 0.012, 0.04), mat(0xaeb2b8, 0.12, 0.88)))
  root.children[root.children.length - 1].position.set(0, 0.2, 0.93)

  // ── feet ──
  const padGeo = new THREE.CylinderGeometry(0.1, 0.13, 0.05, 8)
  ;[[-1.1, 0.025, -0.85], [1.1, 0.025, -0.85], [-1.1, 0.025, 0.85], [1.1, 0.025, 0.85]].forEach(([px, py, pz]) => {
    root.add(new THREE.Mesh(padGeo, mat(0x5a5a5a, 0.5, 0.7)))
    root.children[root.children.length - 1].position.set(px, py, pz)
  })

  // ── data label ──
  const devName = device.name || device.deviceName || device.deviceCode || '设备'
  const dataDiv = labelHtml(
    `<b>${devName}</b><br><span style="color:#${c.toString(16).padStart(6, '0')}">●</span> ${TXT[st] || ''}`,
    '#' + c.toString(16).padStart(6, '0')
  )
  dataDiv.position.set(0, 2.3, 0); root.add(dataDiv)

  root.userData = { device, spindle, led }
  return root
}

// ─── selection glow outline ───
function makeOutline(color: number): THREE.Group {
  const g = new THREE.Group()
  const mat = new THREE.MeshBasicMaterial({ color, wireframe: true, transparent: true, opacity: 0.5 })
  const geo = new THREE.BoxGeometry(2.6, 2.1, 2.3)
  const mesh = new THREE.Mesh(geo, mat)
  g.add(mesh)
  g.visible = false
  return g
}

// ─── init scene ───
function init() {
  if (!containerRef.value) return
  const dark = themeStore.isDark
  const scene = new THREE.Scene()
  scene.background = new THREE.Color(dark ? 0x14141a : 0xe8e8ed)

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
  selectedOutline.position.set(0, 0.9, 0); scene.add(selectedOutline)
  hoverOutline = makeOutline(0x88aaff)
  hoverOutline.position.set(0, 0.9, 0); scene.add(hoverOutline)

  S = { scene, camera, webgl, lbl, controls, clock, animId: 0, raycaster, mouse }
  animate()
  ready.value = true
}

// ─── refresh devices ───
function refresh(devices: any[]) {
  if (!S) return
  const count = devices?.length || 0
  if (!count) return

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
    const root = buildMachine(d)
    const x = ox + (i % GRID_COLS) * SX
    const z = oz + Math.floor(i / GRID_COLS) * SZ
    root.position.set(x, 0, z)
    S.scene.add(root)

    const spindle = (root.userData as any).spindle as THREE.Mesh | undefined
    const led = (root.userData as any).led as THREE.Mesh | undefined
    const conveyor = (root.userData as any).conveyor as THREE.Group | undefined

    deviceNodes.push({ root, spindle: spindle ?? null, led: led ?? null, pulseRing: null, glowRing: null })

    // pulse ring for fault machines
    const st = ST(d.status)
    if (st === 'FAULT') {
      const ringGeo = new THREE.TorusGeometry(1.3, 0.03, 16, 32)
      const ringMat = new THREE.MeshBasicMaterial({ color: 0xff3b30, transparent: true, opacity: 0.6 })
      const ring = new THREE.Mesh(ringGeo, ringMat)
      ring.rotation.x = -Math.PI / 2
      ring.position.y = 0.15
      root.add(ring)
      deviceNodes[deviceNodes.length - 1].pulseRing = ring
    }

    // glow ring
    const glowGeo = new THREE.TorusGeometry(1.35, 0.02, 8, 32)
    const glowMat = new THREE.MeshBasicMaterial({ color: CLR[st] || 0x6366f1, transparent: true, opacity: 0.3 })
    const glow = new THREE.Mesh(glowGeo, glowMat)
    glow.rotation.x = -Math.PI / 2
    glow.position.y = 0.12
    root.add(glow)
    deviceNodes[deviceNodes.length - 1].glowRing = glow
  })
  runTime = 0
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
    emit('select', dev)
    if (selectedOutline && o instanceof THREE.Group) {
      const wp = new THREE.Vector3(); o.getWorldPosition(wp)
      selectedOutline.position.copy(wp); selectedOutline.position.y = 1.0; selectedOutline.visible = true
    }
  }
}

function onMouseMove(e: MouseEvent) {
  if (!S || !hoverOutline) return
  const o = raycastDevice(e.clientX, e.clientY)
  if (o && o instanceof THREE.Group) {
    const wp = new THREE.Vector3(); o.getWorldPosition(wp)
    hoverOutline.position.copy(wp); hoverOutline.position.y = 1.0; hoverOutline.visible = true
  } else {
    hoverOutline.visible = false
  }
}

// ─── view presets ───
function flyToView(key: string) {
  if (!S) return
  currentView.value = key
  const targets: Record<string, [number, number, number, number, number, number]> = {
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
function animate() {
  S.animId = requestAnimationFrame(animate)
  const dt = Math.min(S.clock.getDelta(), 0.1)
  S.controls.update()

  deviceNodes.forEach(n => {
    const dev = (n.root.userData as any).device as any | undefined
    if (!dev) return
    const st = ST(dev.status)

    // spindle rotation (running = fast, others = slow/stop)
    if (n.spindle) {
      const speed = st === 'ONLINE' ? 8 : st === 'MAINTENANCE' ? 1.5 : 0.2
      n.spindle.rotation.y += dt * speed
    }

    // LED pulse
    if (n.led) {
      const mat = n.led.material as THREE.MeshStandardMaterial
      const pulse = st === 'FAULT' ? 0.5 + Math.sin(runTime * 6) * 0.5 : st === 'ONLINE' ? 0.7 + Math.sin(runTime * 2) * 0.3 : 0.4
      mat.emissiveIntensity = pulse
    }

    // pulse ring for fault
    if (n.pulseRing) {
      const ringMat = n.pulseRing.material as THREE.MeshBasicMaterial
      ringMat.opacity = 0.3 + Math.sin(runTime * 4) * 0.3
      n.pulseRing.scale.setScalar(1 + Math.sin(runTime * 3) * 0.08)
    }

    // glow ring
    if (n.glowRing && !n.pulseRing) {
      const gMat = n.glowRing.material as THREE.MeshBasicMaterial
      if (st === 'ONLINE') gMat.opacity = 0.2 + Math.sin(runTime * 1.5) * 0.1
    }
  })

  S.webgl.render(S.scene, S.camera)
  S.lbl.render(S.scene, S.camera)
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
    if (c instanceof THREE.AmbientLight) c.color.setHex(dark ? 0x33334a : 0xbbbbdd)
    if (c instanceof THREE.HemisphereLight) c.color.setHex(dark ? 0x4466aa : 0xaaccff)
  })
})

watch(() => props.devices, v => {
  if (v?.length) { lastDeviceCount = v.length; refresh(v) }
})

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
.panel-bar { flex: 1; height: 4px; background: var(--border-color); border-radius: 2px; overflow: hidden; }
.panel-bar div { height: 100%; background: var(--accent); border-radius: 2px; transition: width .6s; min-width: 2px; }
.panel-bar-label { font-size: 10px; color: var(--text-muted); font-family: monospace; min-width: 30px; text-align: right; }
.panel-actions { display: flex; gap: 4px; padding-top: 6px; border-top: 1px solid var(--border-color); }
.panel-actions button { width: 26px; height: 26px; display: flex; align-items: center; justify-content: center; background: var(--bg-hover); border: 1px solid var(--border-light); border-radius: 5px; color: var(--text-secondary); cursor: pointer; font-size: 11px; transition: all .12s; }
.panel-actions button:hover { background: var(--accent-light); border-color: var(--accent); color: var(--accent); }
.slide-up-enter-active, .slide-up-leave-active { transition: all .2s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(10px); }
</style>
