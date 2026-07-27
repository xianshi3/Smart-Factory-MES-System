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

const CLR: Record<string, number> = { ONLINE: 0x22c55e, FAULT: 0xef4444, OFFLINE: 0x6b7280, MAINTENANCE: 0xf59e0b }
const GLW: Record<string, number> = { ONLINE: 0x166534, FAULT: 0x7f1d1d, OFFLINE: 0x0, MAINTENANCE: 0x78350f }
const STXT: Record<string, string> = { ONLINE: '运行中', FAULT: '故障', OFFLINE: '离线', MAINTENANCE: '维护中' }

function mat(c: number, r = 0.3, m = 0.7, e = 0, ei = 0) {
  return new THREE.MeshStandardMaterial({ color: c, roughness: r, metalness: m, emissive: e, emissiveIntensity: ei })
}

function label(text: string, border: string): CSS2DObject {
  const d = document.createElement('div')
  d.textContent = text
  d.style.cssText = `color:${border};font-size:12px;font-weight:600;font-family:-apple-system,sans-serif;
text-shadow:0 1px 4px rgba(0,0,0,.8);background:rgba(0,0,0,.65);padding:2px 8px;border-radius:4px;
border:1px solid ${border};white-space:nowrap;pointer-events:none`
  return new CSS2DObject(d)
}

function build(device: any): THREE.Group {
  const st = (device.status || 'ONLINE').toUpperCase()
  const c = CLR[st] || 0x6366f1
  const g = GLW[st] || 0x312e81
  const hex = '#' + c.toString(16).padStart(6, '0')

  const root = new THREE.Group()
  const dark = 0x161630
  const bodyC = 0x2a2a48

  // Base
  root.add(new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.1, 2.0), mat(dark, 0.6, 0.3)))
  root.children[0].position.y = 0.05

  // Main chassis
  const body = new THREE.Mesh(new THREE.BoxGeometry(2.0, 1.4, 1.6), mat(bodyC, 0.3, 0.75))
  body.position.y = 0.75; body.castShadow = true; root.add(body)

  // Door cutout (darker panel on front)
  const door = new THREE.Mesh(new THREE.BoxGeometry(1.2, 0.9, 0.02), mat(0x1a1a35, 0.5, 0.5))
  door.position.set(0, 0.7, 0.81); root.add(door)

  // Window on door
  const win = new THREE.Mesh(new THREE.BoxGeometry(0.8, 0.5, 0.01), new THREE.MeshPhysicalMaterial({
    color: 0x88ccff, transparent: true, opacity: 0.3, roughness: 0, metalness: 0, envMapIntensity: 0
  }))
  win.position.set(0, 0.7, 0.82); root.add(win)

  // Handle
  const hdl = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.02, 0.04), mat(0x8888aa, 0.2, 0.9))
  hdl.position.set(0.55, 0.7, 0.83); root.add(hdl)

  // Control panel right side
  const panel = new THREE.Mesh(new THREE.BoxGeometry(0.04, 0.6, 0.5), mat(0x222245, 0.3, 0.6))
  panel.position.set(0.75, 0.7, 0.5); root.add(panel)

  // Screen
  const scr = new THREE.Mesh(new THREE.BoxGeometry(0.35, 0.15, 0.01), new THREE.MeshBasicMaterial({ color: 0x00ff88 }))
  scr.position.set(0.75, 0.8, 0.52); root.add(scr)

  // Buttons on panel
  for (let i = 0; i < 3; i++) {
    const btn = new THREE.Mesh(new THREE.CircleGeometry(0.03, 6), mat(i === 0 ? 0xef4444 : 0x444466))
    btn.position.set(0.68 + i * 0.07, 0.65, 0.52); root.add(btn)
  }

  // Spindle head
  const sh = new THREE.Mesh(new THREE.CylinderGeometry(0.3, 0.4, 0.35, 16), mat(c, 0.15, 0.9, g, 0.08))
  sh.position.set(0.5, 1.55, 0); sh.castShadow = true; root.add(sh)

  // Spindle shaft
  const shaft = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.5, 6), mat(0x8888aa, 0.2, 0.9))
  shaft.position.set(0.5, 1.15, 0); root.add(shaft)

  // Tool
  root.add(new THREE.Mesh(new THREE.ConeGeometry(0.04, 0.12, 4), mat(0xccccdd)))
  root.children[root.children.length - 1].position.set(0.5, 0.88, 0)

  // Column
  root.add(new THREE.Mesh(new THREE.BoxGeometry(0.25, 1.0, 0.25), mat(dark, 0.5, 0.5)))
  root.children[root.children.length - 1].position.set(-0.75, 0.55, 0)

  // Ventilation slots on side
  for (let i = 0; i < 4; i++) {
    const slot = new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.04, 0.15), mat(dark))
    slot.position.set(1.01, 0.35 + i * 0.12, 0); root.add(slot)
  }

  // Status LED
  const led = new THREE.Mesh(new THREE.SphereGeometry(0.06, 8, 8), new THREE.MeshStandardMaterial({ color: c, emissive: c, emissiveIntensity: 0.9 }))
  led.position.set(0.6, 1.65, 0.55); root.add(led)

  // Name label
  const lblObj = label(
    (device.deviceName || device.deviceCode || '设备') + ' ' + STXT[st] || '',
    hex
  )
  lblObj.position.set(0, 2.2, 0)
  root.add(lblObj)

  root.userData = { device }
  return root
}

function init() {
  if (!containerRef.value) return
  const d = themeStore.isDark
  scene = new THREE.Scene()
  scene.background = new THREE.Color(d ? 0x080818 : 0xe8eaed)

  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera = new THREE.PerspectiveCamera(35, w / h, 0.1, 100)
  camera.position.set(16, 10, 16)

  webgl = new THREE.WebGLRenderer({ antialias: true })
  webgl.setSize(w, h)
  webgl.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  webgl.shadowMap.enabled = true
  webgl.shadowMap.type = THREE.PCFSoftShadowMap
  webgl.toneMapping = THREE.ACESFilmicToneMapping
  webgl.toneMappingExposure = 0.9
  webgl.outputColorSpace = THREE.SRGBColorSpace
  containerRef.value.appendChild(webgl.domElement)

  lbl = new CSS2DRenderer()
  lbl.setSize(w, h)
  lbl.domElement.style.cssText = 'position:absolute;top:0;pointer-events:none'
  containerRef.value.appendChild(lbl.domElement)

  controls = new OrbitControls(camera, webgl.domElement)
  controls.enableDamping = true; controls.dampingFactor = 0.06
  controls.minDistance = 6; controls.maxDistance = 35
  controls.maxPolarAngle = Math.PI / 2.1
  controls.target.set(0, 0.6, 0); controls.update()

  const bg = d ? 0x0e0e1a : 0xd0d2d8
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(30, 22), mat(bg, 0.9, 0.1))
  floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; scene.add(floor)
  const grid = new THREE.GridHelper(28, 14, d ? 0x1a1a3a : 0xb0b0c0, d ? 0x11112a : 0xa0a0b0)
  grid.position.y = 0.01; scene.add(grid)

  scene.add(new THREE.AmbientLight(d ? 0x222244 : 0x8888aa, 0.6))
  const main = new THREE.DirectionalLight(0xffeedd, 1.8)
  main.position.set(10, 15, 8); main.castShadow = true; main.shadow.mapSize.set(1024, 1024); scene.add(main)
  const fill = new THREE.DirectionalLight(d ? 0x4444ff : 0x8888dd, 0.3)
  fill.position.set(-8, 6, -10); scene.add(fill)

  animate()
}

function refresh(devices: any[]) {
  // Clean old labels first (CSS2DObject DOM needs manual removal)
  deviceRoots.forEach(g => {
    g.traverse(c => {
      if (c instanceof CSS2DObject && c.element?.parentNode) {
        c.element.parentNode.removeChild(c.element)
      }
    })
    scene.remove(g)
  })
  deviceRoots = []

  const cols = 5; const sx = 4.5; const sz = 4
  const ox = -((Math.min(devices.length, cols) - 1) * sx) / 2
  const oz = -((Math.ceil(devices.length / cols) - 1) * sz) / 2
  devices.forEach((d, i) => {
    const g = build(d)
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
  const w = containerRef.value.clientWidth
  const h = containerRef.value.clientHeight
  camera.aspect = w / h; camera.updateProjectionMatrix()
  webgl.setSize(w, h); lbl.setSize(w, h)
}

watch(() => props.devices, v => { if (v?.length) refresh(v) })
watch(() => themeStore.isDark, () => {
  if (scene) scene.background = new THREE.Color(themeStore.isDark ? 0x080818 : 0xe8eaed)
})

onMounted(() => { init(); if (props.devices?.length) refresh(props.devices); window.addEventListener('resize', onResize) })
onBeforeUnmount(() => {
  cancelAnimationFrame(animId)
  deviceRoots.forEach(g => {
    g.traverse(c => { if (c instanceof CSS2DObject && c.element?.parentNode) c.element.parentNode.removeChild(c.element) })
  })
  webgl?.dispose()
  lbl?.domElement?.remove()
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.scene-container { position: relative; width: 100%; height: 100%; min-height: 500px; border-radius: var(--radius-lg); overflow: hidden; }
</style>
