<template>
  <div ref="containerRef" class="scene-container" @click="onClick"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { useThemeStore } from '@/stores/theme'
const themeStore = useThemeStore()

const props = defineProps<{ devices: any[] }>()
const emit = defineEmits<{ select: [device: any] }>()
const containerRef = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let animId: number
let deviceGroups: THREE.Group[] = []
let raycaster = new THREE.Raycaster()
let mouse = new THREE.Vector2()

function buildScene() {
  if (!containerRef.value) return
  const isDark = themeStore.isDark
  scene = new THREE.Scene()
  scene.background = new THREE.Color(isDark ? 0x07070f : 0xe8eaed)

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 100)
  camera.position.set(14, 10, 14)

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(w, h)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.0
  renderer.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.minDistance = 6
  controls.maxDistance = 30
  controls.maxPolarAngle = Math.PI / 2.1
  controls.target.set(0, 0.6, 0)
  controls.update()

  const bg = isDark ? 0x0e0e1a : 0xd0d2d8
  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(30, 22),
    new THREE.MeshStandardMaterial({ color: bg, roughness: 0.9, metalness: 0.1 })
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(28, 14, isDark ? 0x1a1a3a : 0xb0b0c0, isDark ? 0x11112a : 0xa0a0b0)
  grid.position.y = 0.01
  scene.add(grid)

  // Ambient
  scene.add(new THREE.AmbientLight(isDark ? 0x222244 : 0x8888aa, 0.6))
  const main = new THREE.DirectionalLight(0xffeedd, 1.5)
  main.position.set(8, 12, 6)
  main.castShadow = true
  main.shadow.mapSize.set(1024, 1024)
  scene.add(main)
  const fill = new THREE.DirectionalLight(isDark ? 0x4444ff : 0x8888dd, 0.3)
  fill.position.set(-6, 4, -8)
  scene.add(fill)

  animate()
}

function createCNC(color: number, emissive: number): THREE.Group {
  const g = new THREE.Group()
  const M = (c: number, r = 0.3, m = 0.85, e = 0, ei = 0) => new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })

  const bodyClr = 0x2a2a45
  const darkClr = 0x181830

  // Base plate
  const base = new THREE.Mesh(new THREE.BoxGeometry(2.0, 0.12, 1.6), M(darkClr, 0.6, 0.3))
  base.position.y = 0.06
  base.castShadow = true; base.receiveShadow = true
  g.add(base)

  // Main body
  const body = new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.2, 1.2), M(color, 0.25, 0.8, emissive, 0.08))
  body.position.y = 0.72
  body.castShadow = true
  g.add(body)

  // Side panels (dark)
  const panel = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.7, 0.5), M(0x1a1a40, 0.5, 0.6))
  panel.position.set(0, 0.72, 0.62)
  g.add(panel)

  // Screen
  const screen = new THREE.Mesh(new THREE.BoxGeometry(1, 0.22, 0.02), new THREE.MeshBasicMaterial({ color: 0x00ffaa }))
  screen.position.set(0, 0.9, 0.63)
  g.add(screen)

  // Spindle housing
  const spindle = new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.38, 0.35, 16), M(color, 0.2, 0.9, emissive, 0.12))
  spindle.position.set(0.4, 1.5, 0)
  spindle.castShadow = true
  g.add(spindle)

  // Spindle shaft
  const shaft = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.5, 8), M(0x8888aa, 0.2, 0.9))
  shaft.position.set(0.4, 1.1, 0)
  g.add(shaft)

  // Tool
  const tool = new THREE.Mesh(new THREE.ConeGeometry(0.05, 0.15, 6), M(0xccccdd, 0.3, 0.8))
  tool.position.set(0.4, 0.85, 0)
  g.add(tool)

  // Column
  const col = new THREE.Mesh(new THREE.BoxGeometry(0.25, 1.0, 0.25), M(0x181830, 0.5, 0.5))
  col.position.set(-0.6, 0.62, 0)
  g.add(col)

  // Status LED
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.06, 8, 8), new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.9 }))
  led.position.set(0.5, 1.55, 0.5)
  g.add(led)

  return g
}

function createArm(color: number): THREE.Group {
  const g = new THREE.Group()
  const M = (c: number, r = 0.3, m = 0.8) => new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m })
  const D = new THREE.MeshStandardMaterial({ color: 0x181830, roughness: 0.6, metalness: 0.4 })

  const base = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.6, 0.15, 16), D)
  base.position.y = 0.08
  g.add(base)

  const turntable = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.4, 0.2, 16), M(0x33335a))
  turntable.position.y = 0.28
  g.add(turntable)

  const lower = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.7, 0.12), M(color))
  lower.position.set(0, 0.7, 0.25)
  lower.rotation.x = -0.3
  g.add(lower)

  const upper = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.6, 0.1), M(0x5555aa))
  upper.position.set(0, 1.2, -0.08)
  upper.rotation.x = 0.35
  g.add(upper)

  const wrist = new THREE.Mesh(new THREE.SphereGeometry(0.06, 6, 6), M(0x7777bb))
  wrist.position.set(0, 1.5, -0.35)
  g.add(wrist)

  for (const s of [-1, 1]) {
    const f = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.08, 0.15), M(0x9999dd))
    f.position.set(s * 0.05, 1.4, -0.45)
    g.add(f)
  }

  const led = new THREE.Mesh(new THREE.SphereGeometry(0.06, 8, 8), new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.7 }))
  led.position.set(0, 0.4, 0.35)
  g.add(led)

  return g
}

function conveyor(color: number): THREE.Group {
  const g = new THREE.Group()
  const M = (c: number, r = 0.4, m = 0.7) => new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m })
  const D = new THREE.MeshStandardMaterial({ color: 0x222240, roughness: 0.9 })
  const dClr = 0x333358

  for (const z of [-0.5, 0.5]) {
    const rail = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.06, 0.06), M(dClr))
    rail.position.set(0, 0.18, z)
    g.add(rail)
  }

  for (let x = -0.9; x <= 0.9; x += 0.45) {
    const roller = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 1.0, 8), M(dClr))
    roller.rotation.z = Math.PI / 2
    roller.position.set(x, 0.18, 0)
    g.add(roller)
  }

  const belt = new THREE.Mesh(new THREE.BoxGeometry(2.0, 0.015, 0.8), D)
  belt.position.set(0, 0.2, 0)
  g.add(belt)

  for (let x = -0.6; x <= 0.6; x += 0.6) {
    const item = new THREE.Mesh(new THREE.BoxGeometry(0.12, 0.06, 0.12), new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.08 }))
    item.position.set(x, 0.24, 0)
    g.add(item)
  }

  for (const x of [-1, 1])
    for (const z of [-0.4, 0.4]) {
      const leg = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.03, 0.15, 6), M(dClr))
      leg.position.set(x, 0.08, z)
      g.add(leg)
    }

  return g
}

function buildDevice(device: any): THREE.Group {
  const st = (device.status || 'ONLINE').toUpperCase()
  const cm: Record<string, number> = { ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b }
  const em: Record<string, number> = { ONLINE: 0x166534, FAULT: 0x7f1d1d, OFFLINE: 0x1f2937, MAINTENANCE: 0x78350f }
  const c = cm[st] || 0x6366f1
  const e = em[st] || 0x312e81
  const code = (device.device_code || device.code || '').toLowerCase()
  let g: THREE.Group
  if (code.includes('cnc') || code.includes('mill') || code.includes('加工')) g = createCNC(c, e)
  else if (code.includes('robot') || code.includes('arm') || code.includes('机械')) g = createArm(c)
  else if (code.includes('conveyor') || code.includes('传送')) g = conveyor(c)
  else g = createCNC(c, e)
  g.userData = { device }
  return g
}

function updateDevices(devices: any[]) {
  deviceGroups.forEach(g => { scene.remove(g); disposeGroup(g) })
  deviceGroups = []
  const cols = 5
  const sx = 4.5; const sz = 4
  const sx2 = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const sz2 = -((Math.ceil(devices.length / cols) - 1) * sz) / 2
  devices.forEach((d, i) => {
    const c = i % cols; const r = Math.floor(i / cols)
    const g = buildDevice(d)
    g.position.set(sx2 + c * sx, 0, sz2 + r * sz)
    scene.add(g)
    deviceGroups.push(g)
  })
}

function disposeGroup(g: THREE.Group) {
  g.traverse(c => { if (c instanceof THREE.Mesh) { c.geometry.dispose(); if (Array.isArray(c.material)) c.material.forEach(m => m.dispose()); else c.material.dispose() } })
}

function onResize() {
  if (!containerRef.value || !camera || !renderer) return
  camera.aspect = containerRef.value.clientWidth / containerRef.value.clientHeight
  camera.updateProjectionMatrix()
  renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight)
}

function onClick(event: MouseEvent) {
  if (!containerRef.value || !renderer) return
  const rect = containerRef.value.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  raycaster.setFromCamera(mouse, camera)
  scene.updateMatrixWorld(true)
  const hits = raycaster.intersectObjects(scene.children, true)
  if (hits.length > 0) {
    let o: THREE.Object3D | null = hits[0].object
    while (o && !(o as any).userData?.device) o = o.parent
    if (o && (o as any).userData?.device) emit('select', (o as any).userData.device)
  }
}

function animate() { animId = requestAnimationFrame(animate); controls.update(); renderer.render(scene, camera) }

watch(() => props.devices, v => { if (v?.length) updateDevices(v) })

watch(() => themeStore.isDark, () => {
  if (!scene) return
  const d = themeStore.isDark
  scene.background = new THREE.Color(d ? 0x07070f : 0xe8eaed)
})

onMounted(() => { buildScene(); if (props.devices?.length) updateDevices(props.devices); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { cancelAnimationFrame(animId); deviceGroups.forEach(g => disposeGroup(g)); renderer?.dispose(); window.removeEventListener('resize', onResize) })
</script>

<style scoped>
.scene-container { width: 100%; height: 100%; min-height: 480px; border-radius: var(--radius-lg); overflow: hidden; }
</style>
