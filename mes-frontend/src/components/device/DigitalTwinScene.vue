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
let lbl: CSS2DRenderer
let controls: OrbitControls
let animId: number
let deviceRoots: THREE.Group[] = []
let raycaster = new THREE.Raycaster()
let mouse = new THREE.Vector2()

const ST = (s: string) => (s || 'ONLINE').toUpperCase()
const CLR: Record<string, number> = { ONLINE: 0x34c759, FAULT: 0xff3b30, OFFLINE: 0x8e8e93, MAINTENANCE: 0xff9500 }
const GLW: Record<string, number> = { ONLINE: 0x1a7a3a, FAULT: 0x7a1a1a, OFFLINE: 0x0, MAINTENANCE: 0x7a3a00 }
const TXT: Record<string, string> = { ONLINE: '正常', FAULT: '故障', OFFLINE: '离线', MAINTENANCE: '维护' }

function mat(c: number, r = 0.3, m = 0.4, e = 0, ei = 0) {
  return new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
}

function label(text: string, border: string): CSS2DObject {
  const d = document.createElement('div')
  d.textContent = text
  d.style.cssText = `color:#fff;font-size:11px;font-weight:500;font-family:-apple-system,BlinkMacSystemFont,sans-serif;
background:rgba(0,0,0,.7);backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);
padding:3px 10px;border-radius:6px;border:1px solid ${border};white-space:nowrap;pointer-events:none;
box-shadow:0 2px 8px rgba(0,0,0,.3)`
  return new CSS2DObject(d)
}

function buildMachine(device: any): THREE.Group {
  const st = ST(device.status)
  const c = CLR[st] || 0x6366f1
  const g = GLW[st] || 0x0
  const hex = '#' + c.toString(16).padStart(6, '0')

  const root = new THREE.Group()
  const metal = 0x3a3a4c
  const dark = 0x2a2a3a
  const light = 0x4a4a5c

  // Base
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.0, 0.08, 1.6), mat(dark, 0.6, 0.5)))
  root.children[0].position.y = 0.04

  // Body
  const body = new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.1, 1.2), mat(metal, 0.4, 0.6))
  body.position.y = 0.63; body.castShadow = true; root.add(body)

  // Front trim
  root.add(new THREE.Mesh(new THREE.BoxGeometry(1.3, 0.02, 0.01), mat(light)))
  root.children[root.children.length - 1].position.set(0, 0.85, 0.61)

  // Door panel
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.7, 0.01), mat(0x323248)))
  root.children[root.children.length - 1].position.set(0, 0.58, 0.61)

  // Window
  const win = new THREE.Mesh(new THREE.BoxGeometry(0.55, 0.35, 0.01),
    new THREE.MeshPhysicalMaterial({ color: 0xaaccff, transparent: true, opacity: 0.25, roughness: 0, metalness: 0, clearcoat: 0.1 }))
  win.position.set(0, 0.6, 0.615); root.add(win)

  // Handle
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.015, 0.025), mat(0x666688, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(0.4, 0.6, 0.62)

  // Control panel
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.45, 0.01), mat(0x1e1e32)))
  root.children[root.children.length - 1].position.set(0.55, 0.58, 0.55)

  // Screen
  const scr = new THREE.Mesh(new THREE.BoxGeometry(0.25, 0.12, 0.005), new THREE.MeshBasicMaterial({ color: 0x30d158 }))
  scr.position.set(0.55, 0.65, 0.555); root.add(scr)

  // Red button
  root.add(new THREE.Mesh(new THREE.CircleGeometry(0.025, 8), mat(0xff3b30)))
  root.children[root.children.length - 1].position.set(0.47, 0.52, 0.555)

  // Green button
  root.add(new THREE.Mesh(new THREE.CircleGeometry(0.025, 8), mat(0x34c759)))
  root.children[root.children.length - 1].position.set(0.55, 0.52, 0.555)

  // Spindle head
  const sp = new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.28, 0.25, 12), mat(c, 0.2, 0.7, g, 0.05))
  sp.position.set(0.35, 1.3, 0); sp.castShadow = true; root.add(sp)

  // Shaft
  root.add(new THREE.Mesh(new THREE.CylinderGeometry(0.04, 0.04, 0.35, 6), mat(0x8888aa, 0.2, 0.8)))
  root.children[root.children.length - 1].position.set(0.35, 1.0, 0)

  // Column
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.85, 0.18), mat(0x222238)))
  root.children[root.children.length - 1].position.set(-0.55, 0.48, 0)

  // LED
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.04, 8, 8), new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.9 }))
  led.position.set(0.45, 1.45, 0.35); root.add(led)

  // Label
  const lblObj = label(
    (device.deviceName || device.deviceCode || '设备') + ' · ' + (TXT[st] || ''),
    hex
  )
  lblObj.position.set(0, 1.9, 0)
  root.add(lblObj)

  root.userData = { device }
  return root
}

function init() {
  if (!containerRef.value) return
  const d = themeStore.isDark
  scene = new THREE.Scene()
  scene.background = new THREE.Color(d ? 0x1c1c1e : 0xf2f2f7)

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera = new THREE.PerspectiveCamera(30, w / h, 0.1, 100)
  camera.position.set(18, 12, 18)

  webgl = new THREE.WebGLRenderer({ antialias: true })
  webgl.setSize(w, h)
  webgl.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  webgl.shadowMap.enabled = true
  webgl.shadowMap.type = THREE.PCFSoftShadowMap
  webgl.toneMapping = THREE.ACESFilmicToneMapping
  webgl.toneMappingExposure = 1.0
  webgl.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(webgl.domElement)

  lbl = new CSS2DRenderer()
  lbl.setSize(w, h)
  lbl.domElement.style.cssText = 'position:absolute;top:0;pointer-events:none'
  containerRef.value.appendChild(lbl.domElement)

  controls = new OrbitControls(camera, webgl.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.05
  controls.minDistance = 8; controls.maxDistance = 40
  controls.maxPolarAngle = Math.PI / 2.1
  controls.target.set(0, 0.5, 0); controls.update()

  const bg = d ? 0x1c1c1e : 0xf2f2f7
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(32, 24), mat(bg, 0.9, 0))
  floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; scene.add(floor)
  const grid = new THREE.GridHelper(30, 15, d ? 0x38383a : 0xc7c7cc, d ? 0x2c2c2e : 0xd1d1d6)
  grid.position.y = 0.01; scene.add(grid)

  scene.add(new THREE.AmbientLight(d ? 0x3a3a3c : 0xccccdd, 0.5))
  const main = new THREE.DirectionalLight(themeStore.isDark ? 0xffeedd : 0xffffff, 1.5)
  main.position.set(12, 18, 10); main.castShadow = true; main.shadow.mapSize.set(2048, 2048); scene.add(main)
  const rim = new THREE.DirectionalLight(0x8888ff, 0.4)
  rim.position.set(-8, 6, -10); scene.add(rim)
  scene.add(new THREE.HemisphereLight(d ? 0x4444aa : 0xaaaaff, d ? 0x222244 : 0x8888aa, 0.3))

  animate()
}

function refresh(devices: any[]) {
  deviceRoots.forEach(g => {
    g.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
    scene.remove(g)
  })
  deviceRoots = []
  const cols = 5; const sx = 4.5; const sz = 4
  const ox = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const oz = -((Math.ceil(devices.length / cols) - 1) * sz) / 2
  devices.forEach((d, i) => {
    const g = buildMachine(d)
    g.position.set(ox + (i % cols) * sx, 0, oz + Math.floor(i / cols) * sz)
    scene.add(g); deviceRoots.push(g)
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
  lbl.render(scene, camera)
}

function onResize() {
  if (!containerRef.value || !camera || !webgl) return
  const w = containerRef.value.clientWidth; const h = containerRef.value.clientHeight
  camera.aspect = w / h; camera.updateProjectionMatrix()
  webgl.setSize(w, h); lbl.setSize(w, h)
}

watch(() => props.devices, v => { if (v?.length) refresh(v) })
watch(() => themeStore.isDark, () => {
  if (scene) scene.background = new THREE.Color(themeStore.isDark ? 0x1c1c1e : 0xf2f2f7)
})

onMounted(() => { init(); if (props.devices?.length) refresh(props.devices); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  deviceRoots.forEach(g => {
    g.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
  })
  webgl?.dispose(); lbl?.domElement?.remove(); window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.scene-container { position: relative; width: 100%; height: 100%; min-height: 500px; border-radius: 12px; overflow: hidden; }
</style>
