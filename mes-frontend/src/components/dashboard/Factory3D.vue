<template>
  <div ref="wrapRef" class="factory3d-wrap">
    <canvas ref="canvasRef" class="factory3d-canvas"></canvas>
    <div v-if="hoverDevice" class="f3d-tip" :style="tipStyle">
      <div class="f3d-tip-name">{{ hoverDevice.name }}</div>
      <div class="f3d-tip-meta">
        <span :class="['f3d-status', hoverDevice.status]">{{ statusText(hoverDevice.status) }}</span>
        <span v-if="hoverDevice.temperature != null">{{ Math.round(hoverDevice.temperature) }}°C</span>
        <span v-if="hoverDevice.speed != null">{{ hoverDevice.speed }}rpm</span>
      </div>
      <div class="f3d-tip-hint">点击查看设备详情</div>
    </div>
    <div class="f3d-legend">
      <span class="lg-item"><i class="lg-dot" style="background:#10b981"></i>运行中</span>
      <span class="lg-item"><i class="lg-dot" style="background:#06b6d4"></i>空闲</span>
      <span class="lg-item"><i class="lg-dot" style="background:#ef4444"></i>告警</span>
      <span class="lg-item"><i class="lg-dot" style="background:#f59e0b"></i>维护</span>
      <span class="lg-hint">拖拽旋转 · 滚轮缩放 · 点击设备跳转</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { useThemeStore } from '@/stores/theme'

const props = defineProps<{ devices: any[] }>()

const router = useRouter()
const themeStore = useThemeStore()

const wrapRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const hoverDevice = ref<any>(null)
const tipStyle = ref({ left: '0px', top: '0px' })

const STATUS_COLOR: Record<string, number> = {
  ONLINE: 0x10b981,
  OFFLINE: 0x06b6d4,
  ALARM: 0xef4444,
  MAINTENANCE: 0xf59e0b,
}

const statusText = (s: string) =>
  ({ ONLINE: '运行中', OFFLINE: '空闲', ALARM: '告警', MAINTENANCE: '维护中' } as Record<string, string>)[s] || '未知'

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let controls: OrbitControls | null = null
let rafId = 0
let disposed = false

/** 设备方块 mesh 列表（与 devices 对应） */
const deviceMeshes: { mesh: THREE.Group; baseY: number; userData: any }[] = []

/** 动态装饰对象 */
const conveyorBlocks: THREE.Mesh[] = []
const scanRings: { mesh: THREE.Mesh; base: number }[] = []
let floatParticles: THREE.Points | null = null

const buildScene = () => {
  const wrap = wrapRef.value
  const canvas = canvasRef.value
  if (!wrap || !canvas) return

  const w = wrap.clientWidth
  const h = wrap.clientHeight

  renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.toneMapping = THREE.ACESFilmicToneMapping

  scene = new THREE.Scene()
  scene.fog = new THREE.Fog(0x000000, 40, 120)

  camera = new THREE.PerspectiveCamera(50, w / h, 0.1, 500)
  camera.position.set(34, 26, 34)
  camera.lookAt(0, 0, 0)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.maxPolarAngle = Math.PI / 2.15
  controls.minDistance = 12
  controls.maxDistance = 80
  controls.target.set(0, 0, 0)
  controls.addEventListener('start', () => {
    lastInteract = performance.now()
  })

  // 地面网格
  const grid = new THREE.GridHelper(90, 30, 0x6366f1, 0x33334d)
  ;(grid.material as THREE.Material).transparent = true
  ;(grid.material as THREE.Material).opacity = 0.35
  scene.add(grid)

  // 厂房框架（线框建筑）
  const building = new THREE.LineSegments(
    new THREE.EdgesGeometry(new THREE.BoxGeometry(52, 10, 30)),
    new THREE.LineBasicMaterial({ color: 0x6366f1, transparent: true, opacity: 0.35 })
  )
  building.position.set(0, 5, 0)
  scene.add(building)

  // 厂房玻璃顶棚（半透明面）
  const roof = new THREE.Mesh(
    new THREE.BoxGeometry(52, 0.4, 30),
    new THREE.MeshBasicMaterial({ color: 0x8b5cf6, transparent: true, opacity: 0.08 })
  )
  roof.position.set(0, 10.4, 0)
  scene.add(roof)

  // 中轴传送带示意线
  const belt = new THREE.Line(
    new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(-24, 0.4, 0), new THREE.Vector3(24, 0.4, 0),
    ]),
    new THREE.LineBasicMaterial({ color: 0x8b5cf6, transparent: true, opacity: 0.5 })
  )
  scene.add(belt)

  // 传送带流动块（生产物流流动效果）
  for (let i = 0; i < 8; i++) {
    const block = new THREE.Mesh(
      new THREE.BoxGeometry(0.55, 0.55, 0.55),
      new THREE.MeshBasicMaterial({ color: 0x22d3ee, transparent: true, opacity: 0.9 })
    )
    block.position.set(-24 + i * 6, 0.75, 0)
    scene.add(block)
    conveyorBlocks.push(block)
  }

  // 地面扫描环（雷达扩散效果）
  for (let i = 0; i < 3; i++) {
    const ring = new THREE.Mesh(
      new THREE.RingGeometry(1.4, 1.5, 48),
      new THREE.MeshBasicMaterial({
        color: 0x22d3ee,
        transparent: true,
        opacity: 0.5,
        side: THREE.DoubleSide,
        depthWrite: false,
      })
    )
    ring.rotation.x = -Math.PI / 2
    ring.position.y = 0.06
    scanRings.push({ mesh: ring, base: i * 1.7 })
    scene.add(ring)
  }

  // 漂浮数据粒子
  {
    const count = 70
    const positions = new Float32Array(count * 3)
    for (let i = 0; i < count; i++) {
      positions[i * 3] = (Math.random() - 0.5) * 56
      positions[i * 3 + 1] = 1 + Math.random() * 11
      positions[i * 3 + 2] = (Math.random() - 0.5) * 34
    }
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
    floatParticles = new THREE.Points(
      geo,
      new THREE.PointsMaterial({
        color: 0x8b5cf6,
        size: 0.18,
        transparent: true,
        opacity: 0.55,
        depthWrite: false,
        blending: THREE.AdditiveBlending,
      })
    )
    scene.add(floatParticles)
  }

  // 灯光
  scene.add(new THREE.HemisphereLight(0xffffff, 0x223, 1.1))
  const dir = new THREE.DirectionalLight(0xffffff, 1.6)
  dir.position.set(20, 40, 15)
  scene.add(dir)

  syncDevices()
  window.addEventListener('resize', onResize)
}

const syncDevices = () => {
  if (!scene) return
  // 清掉旧设备
  deviceMeshes.forEach(({ mesh }) => {
    mesh.traverse((o: any) => {
      if (o.geometry) o.geometry.dispose()
      if (o.material) o.material.dispose()
    })
    scene!.remove(mesh)
  })
  deviceMeshes.length = 0

  const list = props.devices.slice(0, 12)
  const cols = Math.min(4, Math.max(2, Math.ceil(Math.sqrt(list.length))))
  const spacingX = 10
  const spacingZ = 8
  const startX = -((cols - 1) * spacingX) / 2
  const startZ = -((Math.ceil(list.length / cols) - 1) * spacingZ) / 2

  list.forEach((d, i) => {
    const col = i % cols
    const row = Math.floor(i / cols)
    const color = STATUS_COLOR[d.status] ?? 0x6366f1

    const group = new THREE.Group()

    // 基座
    const base = new THREE.Mesh(
      new THREE.BoxGeometry(2.6, 0.5, 2.6),
      new THREE.MeshStandardMaterial({ color: 0x222233, roughness: 0.6, metalness: 0.4 })
    )
    base.position.y = 0.25
    group.add(base)

    // 机身（高度随利用率）
    const util = Math.min(1.2, Math.max(0.35, ((d.speed || 0) / 120) + 0.3))
    const bodyH = 1.6 * util + 0.8
    const body = new THREE.Mesh(
      new THREE.BoxGeometry(1.9, bodyH, 1.9),
      new THREE.MeshStandardMaterial({
        color,
        roughness: 0.35,
        metalness: 0.55,
        emissive: color,
        emissiveIntensity: 0.28,
      })
    )
    body.position.y = 0.5 + bodyH / 2
    group.add(body)

    // 顶部状态灯
    const lamp = new THREE.Mesh(
      new THREE.SphereGeometry(0.28, 12, 12),
      new THREE.MeshBasicMaterial({ color })
    )
    lamp.position.y = 0.5 + bodyH + 0.45
    group.add(lamp)

    // 告警浮标（仅 ALARM 设备显示红色感叹号锥）
    const marker = new THREE.Mesh(
      new THREE.ConeGeometry(0.34, 0.6, 4),
      new THREE.MeshBasicMaterial({ color: 0xef4444, transparent: true, opacity: 0.95 })
    )
    marker.rotation.x = Math.PI
    marker.position.y = 0.5 + bodyH + 1.15
    marker.visible = d.status === 'ALARM'
    group.add(marker)

    // 状态光柱（半透明竖线）
    const beam = new THREE.Mesh(
      new THREE.CylinderGeometry(0.06, 0.06, 7, 6, 1, true),
      new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.22 })
    )
    beam.position.y = 4
    group.add(beam)

    group.position.set(startX + col * spacingX, 0, startZ + row * spacingZ)
    group.userData = { device: d, lamp, marker, beam, body, baseY: body.position.y }
    scene!.add(group)
    deviceMeshes.push({ mesh: group, baseY: body.position.y, userData: d })
  })
}

const onResize = () => {
  const wrap = wrapRef.value
  if (!wrap || !renderer || !camera) return
  const w = wrap.clientWidth
  const h = wrap.clientHeight
  camera.aspect = w / h
  camera.updateProjectionMatrix()
  renderer.setSize(w, h)
}

/** 自动旋转 + 告警脉冲 + 数据同步 */
let idleRotate = true
let lastInteract = 0
const animate = (now: number) => {
  if (disposed) return
  rafId = requestAnimationFrame(animate)

  // 交互后暂停自转 5s
  if (performance.now() - lastInteract < 5000) idleRotate = false
  else idleRotate = true

  if (idleRotate && scene) {
    scene.rotation.y += 0.0016
  }

  const t = now / 1000
  deviceMeshes.forEach(({ mesh, baseY }) => {
    const d = mesh.userData.device
    const beam = mesh.userData.beam as THREE.Mesh | undefined
    const lamp = mesh.userData.lamp as THREE.Mesh | undefined
    const marker = mesh.userData.marker as THREE.Mesh | undefined
    if (d.status === 'ALARM') {
      const pulse = 1 + Math.sin(t * 6) * 0.12
      if (lamp) lamp.scale.setScalar(pulse)
      if (lamp) (lamp.material as THREE.MeshBasicMaterial).opacity = 0.5 + Math.sin(t * 6) * 0.5
      if (marker) {
        marker.position.y = mesh.userData.markerY || (marker.position.y + Math.sin(t * 5) * 0.08)
        marker.scale.setScalar(1 + Math.sin(t * 6) * 0.15)
      }
      if (beam) (beam.material as THREE.MeshBasicMaterial).opacity = 0.18 + Math.sin(t * 4) * 0.14
    } else {
      mesh.userData.body.position.y = baseY + Math.sin(t * 2 + mesh.position.x) * 0.06
      if (beam) (beam.material as THREE.MeshBasicMaterial).opacity = 0.12 + Math.sin(t * 1.5) * 0.06
    }
  })

  // 传送带流动块循环
  conveyorBlocks.forEach((b, i) => {
    b.position.x = ((b.position.x + 0.06 + 24) % 48) - 24
    b.rotation.y += 0.03
  })

  // 扫描环扩散
  scanRings.forEach((r, i) => {
    const cycle = ((t + r.base) % 4) / 4
    const s = 0.4 + cycle * 22
    r.mesh.scale.setScalar(s)
    ;(r.mesh.material as THREE.MeshBasicMaterial).opacity = 0.45 * (1 - cycle)
  })

  // 粒子缓慢浮动
  if (floatParticles) {
    floatParticles.rotation.y += 0.0008
    floatParticles.position.y = Math.sin(t * 0.5) * 0.3
  }

  if (controls && renderer && scene && camera) {
    controls.update()
    renderer.render(scene, camera)
  }
}

/* ===== 拾取交互 ===== */
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()

const onPointerMove = (e: MouseEvent) => {
  if (!renderer || !scene || !camera) return
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const hits = raycaster.intersectObjects(
    deviceMeshes.map((m) => m.mesh),
    true
  )
  const entry = hits.length ? deviceMeshes.find((m) => findRoot(hits[0].object) === m.mesh) : null

  if (entry) {
    const d = entry.userData
    hoverDevice.value = {
      name: d.deviceName || d.deviceCode || '设备',
      status: d.status,
      temperature: d.temperature,
      speed: d.speed,
    }
    tipStyle.value = {
      left: `${e.clientX - rect.left + 14}px`,
      top: `${e.clientY - rect.top - 10}px`,
    }
  } else {
    hoverDevice.value = null
  }
}

/** 沿父级链找设备 Group 根节点 */
const findRoot = (obj: THREE.Object3D): THREE.Object3D | null => {
  let cur: THREE.Object3D | null = obj
  while (cur) {
    const entry = deviceMeshes.find((m) => m.mesh === cur)
    if (entry) return entry.mesh
    cur = cur.parent
  }
  return null
}

const onClick = (e: MouseEvent) => {
  if (!renderer || !scene || !camera) return
  const rect = renderer.domElement.getBoundingClientRect()
  pointer.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  pointer.y = -((e.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const hits = raycaster.intersectObjects(
    deviceMeshes.map((m) => m.mesh),
    true
  )
  for (const h of hits) {
    const root = findRoot(h.object)
    const entry = root ? deviceMeshes.find((m) => m.mesh === root) : null
    if (entry) {
      const d = entry.userData
      router.push({ path: '/device', query: { code: d.deviceCode || d.deviceName } })
      return
    }
  }
}

let themeWatcher: (() => void) | null = null
let deviceWatcher: (() => void) | null = null

onMounted(() => {
  buildScene()
  const canvas = canvasRef.value
  if (canvas) {
    canvas.addEventListener('pointermove', onPointerMove)
    canvas.addEventListener('click', onClick)
  }
  // 设备数据实时同步（WS 推送驱动）
  deviceWatcher = watch(
    () => props.devices,
    () => syncDevices(),
    { deep: true }
  )
  // 网格/建筑颜色随主题
  themeWatcher = watch(
    () => themeStore.isDark,
    () => {
      if (!scene) return
      scene.traverse((o: any) => {
        if (o instanceof THREE.GridHelper) {
          ;(o.material as THREE.Material).color.set(themeStore.isDark ? 0x6366f1 : 0x4444aa)
        }
      })
    }
  )
  animate(0)
})

onUnmounted(() => {
  disposed = true
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
  const canvas = canvasRef.value
  if (canvas) {
    canvas.removeEventListener('pointermove', onPointerMove)
    canvas.removeEventListener('click', onClick)
  }
  themeWatcher?.()
  deviceWatcher?.()
  controls?.dispose()
  renderer?.dispose()
  scene = null
  renderer = null
})
</script>

<style scoped>
.factory3d-wrap {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 140px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 100%, rgba(99, 102, 241, 0.08), transparent 60%),
    var(--bg-card);
  border: 1px solid var(--border-light);
  cursor: grab;
}
.factory3d-wrap:active { cursor: grabbing; }
.factory3d-canvas { display: block; width: 100%; height: 100%; }

.f3d-tip {
  position: absolute;
  z-index: 10;
  pointer-events: none;
  background: var(--bg-card);
  border: 1px solid var(--accent);
  border-radius: 10px;
  padding: 8px 12px;
  box-shadow: var(--shadow-lg);
  transform: translate(-50%, -110%);
  white-space: nowrap;
}
.f3d-tip-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}
.f3d-tip-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: var(--text-secondary);
}
.f3d-status {
  font-weight: 600;
}
.f3d-status.ONLINE { color: var(--success); }
.f3d-status.OFFLINE { color: var(--info); }
.f3d-status.ALARM { color: var(--danger); }
.f3d-status.MAINTENANCE { color: var(--warning); }
.f3d-tip-hint {
  margin-top: 4px;
  font-size: 10px;
  color: var(--text-muted);
}

.f3d-legend {
  position: absolute;
  left: 12px;
  bottom: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 11px;
  color: var(--text-muted);
  pointer-events: none;
}
.lg-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.lg-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 6px currentColor;
}
.lg-hint { opacity: 0.6; margin-left: 6px; }
</style>
