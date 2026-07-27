<template>
  <div ref="containerRef" class="scene-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, shallowRef } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
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
let loadedModels: Map<string, THREE.Group> = new Map()
let loader: GLTFLoader

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
  controls.enableDamping = true
  controls.dampingFactor = 0.06
  controls.minDistance = 6
  controls.maxDistance = 30
  controls.maxPolarAngle = Math.PI / 2.1
  controls.target.set(0, 0.6, 0)
  controls.update()

  const bg = d ? 0x0e0e1a : 0xd0d2d8
  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(30, 22),
    new THREE.MeshStandardMaterial({ color: bg, roughness: 0.9, metalness: 0.1 })
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(28, 14, d ? 0x1a1a3a : 0xb0b0c0, d ? 0x11112a : 0xa0a0b0)
  grid.position.y = 0.01
  scene.add(grid)

  scene.add(new THREE.AmbientLight(d ? 0x222244 : 0x8888aa, 0.6))
  const main = new THREE.DirectionalLight(0xffeedd, 1.5)
  main.position.set(8, 12, 6)
  main.castShadow = true
  main.shadow.mapSize.set(1024, 1024)
  scene.add(main)
  const fill = new THREE.DirectionalLight(d ? 0x4444ff : 0x8888dd, 0.3)
  fill.position.set(-6, 4, -8)
  scene.add(fill)

  loader = new GLTFLoader()
  animate()
}

function loadDeviceModel(device: any, index: number): Promise<THREE.Group> {
  const st = (device.status || 'ONLINE').toUpperCase()
  const cm: Record<string, number> = { ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b }
  const c = cm[st] || 0x6366f1

  // Try loading a glTF model by device type
  const code = (device.device_code || device.code || '').toLowerCase()
  let modelPath = ''
  if (code.includes('cnc') || code.includes('mill') || code.includes('加工')) modelPath = '/models/cnc_machine.glb'
  else if (code.includes('robot') || code.includes('arm') || code.includes('机械')) modelPath = '/models/robot_arm.glb'
  else modelPath = '/models/cnc_machine.glb'

  return new Promise((resolve) => {
    if (loadedModels.has(modelPath)) {
      const g = loadedModels.get(modelPath)!.clone()
      g.traverse((child) => {
        if (child instanceof THREE.Mesh && child.material) {
          child.material = (child.material as THREE.MeshStandardMaterial).clone()
          child.material.color.setHex(c)
          child.material.emissive?.setHex(c)
          child.material.emissiveIntensity = 0.1
        }
      })
      g.userData = { device }
      resolve(g)
    } else {
      // Fallback geometry if model not found
      const g = new THREE.Group()
      const M = (clr: number, r = 0.3, m = 0.85) => new THREE.MeshStandardMaterial({ color: clr, roughness: r, metalness: m })
      const body = new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.4, 1.2), M(c))
      body.position.y = 0.75; body.castShadow = true; g.add(body)
      const base = new THREE.Mesh(new THREE.BoxGeometry(2.0, 0.12, 1.6), M(0x181830, 0.6, 0.3))
      base.position.y = 0.06; g.add(base)
      const led = new THREE.Mesh(new THREE.SphereGeometry(0.08, 8, 8), new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.8 }))
      led.position.set(0.6, 1.6, 0.5); g.add(led)
      g.userData = { device }
      resolve(g)
    }
  })
}

async function updateDevices(devices: any[]) {
  deviceGroups.forEach(g => { scene.remove(g) })
  deviceGroups = []
  const cols = 5; const sx = 4.5; const sz = 4
  const sx2 = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const sz2 = -((Math.ceil(devices.length / cols) - 1) * sz) / 2

  for (let i = 0; i < devices.length; i++) {
    const c = i % cols; const r = Math.floor(i / cols)
    const g = await loadDeviceModel(devices[i], i)
    g.position.set(sx2 + c * sx, 0, sz2 + r * sz)
    scene.add(g)
    deviceGroups.push(g)
  }
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
  scene.background = new THREE.Color(themeStore.isDark ? 0x07070f : 0xe8eaed)
})

onMounted(() => { buildScene(); if (props.devices?.length) updateDevices(props.devices); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => { cancelAnimationFrame(animId); deviceGroups.forEach(g => disposeGroup(g)); renderer?.dispose(); window.removeEventListener('resize', onResize) })

function disposeGroup(g: THREE.Group) {
  g.traverse(c => { if (c instanceof THREE.Mesh) { c.geometry.dispose(); if (Array.isArray(c.material)) c.material.forEach(m => m.dispose()); else c.material.dispose() } })
}
</script>

<style scoped>
.scene-container { width: 100%; height: 100%; min-height: 480px; border-radius: var(--radius-lg); overflow: hidden; }
</style>
