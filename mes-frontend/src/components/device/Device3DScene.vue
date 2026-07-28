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

function M(c: number, r = 0.3, m = 0.7, e = 0x0, ei = 0) {
  return new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
}

function buildScene() {
  if (!containerRef.value) return
  const d = themeStore.isDark
  scene = new THREE.Scene()
  scene.background = new THREE.Color(d ? 0x07070f : 0xe8eaed)

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
  controls.enableDamping = true; controls.dampingFactor = 0.06
  controls.minDistance = 6; controls.maxDistance = 30
  controls.maxPolarAngle = Math.PI / 2.1
  controls.target.set(0, 0.6, 0); controls.update()

  const bg = d ? 0x0e0e1a : 0xd0d2d8
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(30, 22), M(bg, 0.9, 0.1))
  floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; scene.add(floor)
  const grid = new THREE.GridHelper(28, 14, d ? 0x1a1a3a : 0xb0b0c0, d ? 0x11112a : 0xa0a0b0)
  grid.position.y = 0.01; scene.add(grid)

  scene.add(new THREE.AmbientLight(d ? 0x222244 : 0x8888aa, 0.6))
  const main = new THREE.DirectionalLight(0xffeedd, 1.5)
  main.position.set(8, 12, 6); main.castShadow = true; main.shadow.mapSize.set(1024, 1024); scene.add(main)
  const fill = new THREE.DirectionalLight(d ? 0x4444ff : 0x8888dd, 0.3)
  fill.position.set(-6, 4, -8); scene.add(fill)

  animate()
}

function machineCNC(c: number, e: number): THREE.Group {
  const g = new THREE.Group()
  const dark = 0x1a1a30
  // Base plate
  g.add(new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.1, 1.8), M(dark, 0.6, 0.3)))
  // Main chassis
  const body = new THREE.Mesh(new THREE.BoxGeometry(1.8, 1.3, 1.4), M(c, 0.2, 0.85, e, 0.06))
  body.position.y = 0.7; body.castShadow = true; g.add(body)
  // Control panel (front face)
  g.add(new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.7, 0.6), M(0x222240)))
  const panel = g.children[g.children.length - 1]; panel.position.set(0, 0.7, 0.73)
  // Screen
  const scr = new THREE.Mesh(new THREE.BoxGeometry(0.45, 0.2, 0.02), new THREE.MeshBasicMaterial({ color: 0x00ff88 }))
  scr.position.set(0, 0.85, 0.75); g.add(scr)
  // Spindle housing
  const spindle = new THREE.Mesh(new THREE.CylinderGeometry(0.25, 0.32, 0.3, 16), M(c, 0.15, 0.9, e, 0.1))
  spindle.position.set(0.4, 1.5, 0); spindle.castShadow = true; g.add(spindle)
  // Shaft
  g.add(new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 0.4, 8), M(0x8888aa, 0.2, 0.9)))
  g.children[g.children.length - 1].position.set(0.4, 1.15, 0)
  // Tool
  g.add(new THREE.Mesh(new THREE.ConeGeometry(0.04, 0.12, 6), M(0xccccdd, 0.3, 0.8)))
  g.children[g.children.length - 1].position.set(0.4, 0.88, 0)
  // Column
  const col = new THREE.Mesh(new THREE.BoxGeometry(0.2, 1.0, 0.2), M(0x181838, 0.5, 0.5))
  col.position.set(-0.65, 0.55, 0); g.add(col)
  // LED lamp
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.05, 6, 6), new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.9 }))
  led.position.set(0.55, 1.6, 0.5); g.add(led)
  return g
}

function robotArm(c: number): THREE.Group {
  const g = new THREE.Group()
  const dark = 0x1a1a30
  g.add(new THREE.Mesh(new THREE.CylinderGeometry(0.45, 0.55, 0.12, 16), M(dark, 0.6, 0.4)))
  g.add(new THREE.Mesh(new THREE.CylinderGeometry(0.32, 0.38, 0.18, 16), M(0x2a2a50)))
  g.children[g.children.length - 1].position.y = 0.16
  const lower = new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.6, 0.1), M(c))
  lower.position.set(0, 0.55, 0.2); lower.rotation.x = -0.3; g.add(lower)
  const upper = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.5, 0.08), M(0x5555aa))
  upper.position.set(0, 0.95, -0.06); upper.rotation.x = 0.35; g.add(upper)
  const wrist = new THREE.Mesh(new THREE.SphereGeometry(0.05, 6, 6), M(0x7777bb))
  wrist.position.set(0, 1.2, -0.28); g.add(wrist)
  for (const s of [-1, 1]) {
    const f = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.06, 0.12), M(0x9999dd))
    f.position.set(s * 0.04, 1.1, -0.36); g.add(f)
  }
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.05, 6, 6), new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.7 }))
  led.position.set(0, 0.3, 0.3); g.add(led)
  return g
}

function makeDevice(device: any): THREE.Group {
  const st = (device.status || 'ONLINE').toUpperCase()
  const cm: Record<string, number> = { ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b }
  const em: Record<string, number> = { ONLINE: 0x166534, FAULT: 0x7f1d1d, OFFLINE: 0x0, MAINTENANCE: 0x78350f }
  const c = cm[st] || 0x6366f1; const e = em[st] || 0x312e81
  const code = (device.device_code || device.code || '').toLowerCase()
  const g = code.includes('robot') || code.includes('arm') || code.includes('机械') ? robotArm(c) : machineCNC(c, e)
  g.userData = { device }
  return g
}

function updateDevices(devices: any[]) {
  deviceGroups.forEach(g => { scene.remove(g) })
  deviceGroups = []
  const cols = 5; const sx = 4.5; const sz = 4
  const ox = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const oz = -((Math.ceil(devices.length / cols) - 1) * sz) / 2
  devices.forEach((d, i) => {
    const g = makeDevice(d)
    g.position.set(ox + (i % cols) * sx, 0, oz + Math.floor(i / cols) * sz)
    scene.add(g); deviceGroups.push(g)
  })
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
function onResize() {
  if (!containerRef.value || !camera || !renderer) return
  camera.aspect = containerRef.value.clientWidth / containerRef.value.clientHeight
  camera.updateProjectionMatrix(); renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight)
}

watch(() => props.devices, v => { if (v?.length) updateDevices(v) })
watch(() => themeStore.isDark, () => { if (scene) scene.background = new THREE.Color(themeStore.isDark ? 0x07070f : 0xe8eaed) })

onMounted(() => { buildScene(); if (props.devices?.length) updateDevices(props.devices); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { cancelAnimationFrame(animId); renderer?.dispose(); window.removeEventListener('resize', onResize) })
</script>

<style scoped>
.scene-container { width: 100%; height: 100%; min-height: 480px; border-radius: var(--radius-lg); overflow: hidden; }
</style>
