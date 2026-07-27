<template>
  <div ref="containerRef" class="scene-container" @click="onClick" :style="{ '--bg': isDark ? '#0a0a0f' : '#f0f2f5', '--floor': isDark ? '#1a1a2e' : '#e0e0e8', '--grid': isDark ? '#2a2a4e' : '#c0c0d0' }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { useThemeStore } from '@/stores/theme'

const props = defineProps<{ devices: any[] }>()
const emit = defineEmits<{ select: [device: any] }>()

const containerRef = ref<HTMLElement>()

let scene: THREE.Scene
let camera: THREE.PerspectiveCamera
let renderer: THREE.WebGLRenderer
let controls: OrbitControls
let animId: number
let deviceMeshes: Map<string, THREE.Mesh> = new Map()
let raycaster = new THREE.Raycaster()
let mouse = new THREE.Vector2()

function createFactoryFloor() {
  const floor = new THREE.Mesh(
    new THREE.PlaneGeometry(40, 30),
    new THREE.MeshStandardMaterial({
      color: 0x1a1a2e,
      roughness: 0.8,
      metalness: 0.2,
      transparent: true,
      opacity: 0.9
    })
  )
  floor.rotation.x = -Math.PI / 2
  floor.receiveShadow = true
  scene.add(floor)

  const grid = new THREE.GridHelper(40, 20, 0x2a2a4e, 0x1a1a3e)
  grid.position.y = 0.02
  scene.add(grid)

  for (let i = -18; i <= 18; i += 6) {
    for (let j = -12; j <= 12; j += 6) {
      const marker = new THREE.Mesh(
        new THREE.PlaneGeometry(5.2, 5.2),
        new THREE.MeshBasicMaterial({
          color: 0x0d0d1a,
          transparent: true,
          opacity: 0.5,
          side: THREE.DoubleSide
        })
      )
      marker.rotation.x = -Math.PI / 2
      marker.position.set(i, 0.01, j)
      scene.add(marker)
    }
  }
}

function createDeviceMesh(device: any): THREE.Mesh {
  const status = (device.status || 'ONLINE').toUpperCase()
  const colorMap: Record<string, number> = {
    ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b
  }
  const emissiveMap: Record<string, number> = {
    ONLINE: 0x166534, FAULT: 0x7f1d1d, OFFLINE: 0x1f2937, MAINTENANCE: 0x78350f
  }
  const color = colorMap[status] || 0x6366f1
  const emissive = emissiveMap[status] || 0x312e81

  const group = new THREE.Group()

  // Base
  const base = new THREE.Mesh(
    new THREE.BoxGeometry(1.6, 0.3, 1.2),
    new THREE.MeshStandardMaterial({ color: 0x2d2d4a, roughness: 0.6, metalness: 0.4 })
  )
  base.position.y = 0.15
  base.castShadow = true
  group.add(base)

  // Main body
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(1.4, 1.0, 1.0),
    new THREE.MeshStandardMaterial({
      color, roughness: 0.3, metalness: 0.7, emissive, emissiveIntensity: 0.15
    })
  )
  body.position.y = 0.8
  body.castShadow = true
  group.add(body)

  // Top light
  const light = new THREE.Mesh(
    new THREE.SphereGeometry(0.15),
    new THREE.MeshStandardMaterial({ color: 0xffffff, emissive: color, emissiveIntensity: 0.8 })
  )
  light.position.y = 1.5
  group.add(light)

  // Name label position (above the device)
  const labelPos = new THREE.Vector3(0, 2.2, 0)

  group.userData = { device, color, labelPos }
  const mesh = group as unknown as THREE.Mesh
  return mesh
}

function buildScene() {
  if (!containerRef.value) return

  scene = new THREE.Scene()
  scene.background = new THREE.Color(isDark.value ? 0x0a0a0f : 0xf0f2f5)
  scene.fog = new THREE.Fog(isDark.value ? 0x0a0a0f : 0xf0f2f5, 30, 50)

  camera = new THREE.PerspectiveCamera(45, containerRef.value.clientWidth / containerRef.value.clientHeight, 0.1, 100)
  camera.position.set(12, 10, 12)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(containerRef.value.clientWidth, containerRef.value.clientHeight)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.shadowMap.enabled = true
  renderer.shadowMap.type = THREE.PCFSoftShadowMap
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.2
  containerRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.08
  controls.minDistance = 5
  controls.maxDistance = 30
  controls.maxPolarAngle = Math.PI / 2.2
  controls.target.set(0, 1, 0)

  createFactoryFloor(isDark.value ? 0x1a1a2e : 0xe0e0e8, isDark.value ? 0x2a2a4e : 0xc0c0d0)

  // Lights
  const ambient = new THREE.AmbientLight(0x222244, 0.5)
  scene.add(ambient)

  const dirLight = new THREE.DirectionalLight(0xffffff, 1.5)
  dirLight.position.set(10, 15, 10)
  dirLight.castShadow = true
  dirLight.shadow.mapSize.width = 1024
  dirLight.shadow.mapSize.height = 1024
  scene.add(dirLight)

  const fillLight = new THREE.DirectionalLight(0x4444ff, 0.3)
  fillLight.position.set(-10, 5, -10)
  scene.add(fillLight)

  const hemiLight = new THREE.HemisphereLight(0x4444ff, 0x000022, 0.6)
  scene.add(hemiLight)

  // Ceiling lights
  for (let x = -12; x <= 12; x += 8) {
    for (let z = -8; z <= 8; z += 8) {
      const spot = new THREE.SpotLight(0x8888ff, 0.3, 20, Math.PI / 6, 0.5, 1)
      spot.position.set(x, 6, z)
      scene.add(spot)
    }
  }

  animate()
}

function animate() {
  animId = requestAnimationFrame(animate)
  controls.update()
  renderer.render(scene, camera)
}

function updateDevices(devices: any[]) {
  // Remove old
  deviceMeshes.forEach((mesh) => { scene.remove(mesh) })
  deviceMeshes.clear()

  // Place new in a grid
  const cols = 5
  const spacingX = 4
  const spacingZ = 4
  const startX = -((Math.min(devices.length, cols) - 1) * spacingX) / 2
  const startZ = -((Math.ceil(devices.length / cols) - 1) * spacingZ) / 2

  devices.forEach((device, i) => {
    const col = i % cols
    const row = Math.floor(i / cols)
    const mesh = createDeviceMesh(device)
    mesh.position.set(startX + col * spacingX, 0, startZ + row * spacingZ)
    scene.add(mesh)
    deviceMeshes.set(device.device_code || device.id, mesh)
  })
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
  const intersects = raycaster.intersectObjects(scene.children, true)
  if (intersects.length > 0) {
    let obj = intersects[0].object
    while (obj.parent && !(obj as any).userData?.device) obj = obj.parent
    const deviceData = (obj as any).userData?.device
    if (deviceData) emit('select', deviceData)
  }
}

watch(() => props.devices, (val) => {
  if (val?.length) updateDevices(val)
}, { deep: true })

onMounted(() => {
  buildScene()
  if (props.devices?.length) updateDevices(props.devices)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  renderer?.dispose()
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.scene-container { width: 100%; height: 100%; min-height: 500px; border-radius: var(--radius-lg); overflow: hidden; background: #0a0a0f; }
</style>
