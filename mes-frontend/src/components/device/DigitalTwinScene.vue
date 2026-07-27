<template>
  <div ref="containerRef" class="scene-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { useThemeStore } from '@/stores/theme'
const themeStore = useThemeStore()

const props = defineProps<{ devices: any[] }>()
const emit = defineEmits<{ select: [device: any] }>()
const containerRef = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let webgl: THREE.WebGLRenderer
let labelRenderer: CSS2DRenderer
let controls: OrbitControls
let animId: number
let deviceObjects: THREE.Group[] = []
let labelObjects: CSS2DObject[] = []
let raycaster = new THREE.Raycaster()
let mouse = new THREE.Vector2()

const deviceColors: Record<string, number> = {
  ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b
}
const deviceGlow: Record<string, number> = {
  ONLINE: 0x166534, FAULT: 0x7f1d1d, OFFLINE: 0x0, MAINTENANCE: 0x78350f
}
const deviceNames: Record<string, string> = {
  ONLINE: '运行', FAULT: '故障', OFFLINE: '离线', MAINTENANCE: '维护'
}

function M(c: number, r = 0.3, m = 0.7, e = 0, ei = 0) {
  return new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
}

function createLabel(text: string, color: string): CSS2DObject {
  const div = document.createElement('div')
  div.textContent = text
  div.style.color = color
  div.style.fontSize = '12px'
  div.style.fontWeight = '600'
  div.style.fontFamily = '-apple-system, BlinkMacSystemFont, sans-serif'
  div.style.textShadow = '0 1px 4px rgba(0,0,0,0.8)'
  div.style.background = 'rgba(0,0,0,0.6)'
  div.style.padding = '2px 8px'
  div.style.borderRadius = '4px'
  div.style.border = '1px solid ' + color
  div.style.whiteSpace = 'nowrap'
  div.style.pointerEvents = 'none'
  return new CSS2DObject(div)
}

function buildDevice(device: any, index: number): THREE.Group {
  const st = (device.status || 'ONLINE').toUpperCase()
  const color = deviceColors[st] || 0x6366f1
  const glow = deviceGlow[st] || 0x312e81
  const statusColor = deviceNames[st] || '运行'
  const hexColor = '#' + color.toString(16).padStart(6, '0')

  const g = new THREE.Group()
  const dark = 0x1a1a30

  // Base platform
  const base = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.12, 1.8), M(dark, 0.6, 0.3))
  base.position.y = 0.06; base.receiveShadow = true; g.add(base)

  // Main body
  const body = new THREE.Mesh(new THREE.BoxGeometry(1.8, 1.3, 1.4), M(color, 0.2, 0.85, glow, 0.06))
  body.position.y = 0.72; body.castShadow = true; g.add(body)

  // Panel
  g.add(new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.7, 0.55), M(0x222240)))
  g.children[g.children.length - 1].position.set(0, 0.72, 0.72)

  // Screen
  const scr = new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.18, 0.02), new THREE.MeshBasicMaterial({ color: 0x00ff88 }))
  scr.position.set(0, 0.85, 0.74); g.add(scr)

  // Spindle
  const sp = new THREE.Mesh(new THREE.CylinderGeometry(0.22, 0.28, 0.28, 12), M(color, 0.15, 0.9, glow, 0.1))
  sp.position.set(0.45, 1.45, 0); sp.castShadow = true; g.add(sp)

  // Shaft
  const sh = new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 0.45, 6), M(0x8888aa, 0.2, 0.9))
  sh.position.set(0.45, 1.1, 0); g.add(sh)

  // Tool
  const tl = new THREE.Mesh(new THREE.ConeGeometry(0.03, 0.1, 4), M(0xccccdd))
  tl.position.set(0.45, 0.86, 0); g.add(tl)

  // Column
  const col = new THREE.Mesh(new THREE.BoxGeometry(0.22, 1.0, 0.22), M(0x181838, 0.5, 0.5))
  col.position.set(-0.65, 0.56, 0); g.add(col)

  // Status lamp (top)
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.06, 8, 8),
    new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.9 }))
  led.position.set(0.55, 1.55, 0.5); g.add(led)

  // Name label
  const label = createLabel(
    (device.deviceName || device.deviceCode || '设备') + ' - ' + statusColor,
    hexColor
  )
  label.position.set(0, 2.0, 0)
  g.add(label)

  g.userData = { device }
  return g
}

function init() {
  if (!containerRef.value) return
  const d = themeStore.isDark
  scene = new THREE.Scene()
  scene.background = new THREE.Color(d ? 0x07070f : 0xe8eaed)

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 100)
  camera.position.set(14, 12, 14)

  webgl = new THREE.WebGLRenderer({ antialias: true })
  webgl.setSize(w, h)
  webgl.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  webgl.shadowMap.enabled = true
  webgl.shadowMap.type = THREE.PCFSoftShadowMap
  webgl.toneMapping = THREE.ACESFilmicToneMapping
  webgl.toneMappingExposure = 0.9
  webgl.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(webgl.domElement)

  labelRenderer = new CSS2DRenderer()
  labelRenderer.setSize(w, h)
  labelRenderer.domElement.style.position = 'absolute'
  labelRenderer.domElement.style.top = '0'
  labelRenderer.domElement.style.pointerEvents = 'none'
  containerRef.value.appendChild(labelRenderer.domElement)

  controls = new OrbitControls(camera, webgl.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.06
  controls.minDistance = 6; controls.maxDistance = 35
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

  scene.add(new THREE.HemisphereLight(d ? 0x3333ff : 0x8888ff, d ? 0x000022 : 0x444466, 0.4))

  animate()
}

function updateScene(devices: any[]) {
  deviceObjects.forEach(g => { scene.remove(g) })
  deviceObjects = []
  const cols = 5; const sx = 4.5; const sz = 4
  const ox = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const oz = -((Math.ceil(devices.length / cols) - 1) * sz) / 2
  devices.forEach((d, i) => {
    const g = buildDevice(d, i)
    g.position.set(ox + (i % cols) * sx, 0, oz + Math.floor(i / cols) * sz)
    scene.add(g); deviceObjects.push(g)
  })
}

function onClick(event: MouseEvent) {
  if (!containerRef.value || !webgl) return
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

function animate() {
  animId = requestAnimationFrame(animate)
  controls.update()
  webgl.render(scene, camera)
  labelRenderer.render(scene, camera)
}

function onResize() {
  if (!containerRef.value || !camera || !webgl) return
  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera.aspect = w / h; camera.updateProjectionMatrix()
  webgl.setSize(w, h); labelRenderer.setSize(w, h)
}

watch(() => props.devices, v => { if (v?.length) updateScene(v) })
watch(() => themeStore.isDark, () => {
  if (scene) scene.background = new THREE.Color(themeStore.isDark ? 0x07070f : 0xe8eaed)
})

onMounted(() => {
  init()
  if (props.devices?.length) updateScene(props.devices)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  webgl?.dispose()
  labelRenderer?.domElement?.remove()
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.scene-container { position: relative; width: 100%; height: 100%; min-height: 480px; border-radius: var(--radius-lg); overflow: hidden; }
.scene-container :deep(canvas) { display: block; }
</style>
