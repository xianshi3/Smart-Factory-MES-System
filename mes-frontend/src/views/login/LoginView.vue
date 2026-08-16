<template>
  <div class="login-page">
    <!-- ===== 左侧品牌展示区 ===== -->
    <div class="login-left">
      <div class="left-bg">
        <div class="bg-circle circle-1"></div>
        <div class="bg-circle circle-2"></div>
        <div class="bg-circle circle-3"></div>
        <div class="bg-grid"></div>
        <div class="bg-particle p1"></div>
        <div class="bg-particle p2"></div>
        <div class="bg-particle p3"></div>
        <div class="bg-particle p4"></div>
      </div>

      <div class="brand-content">
        <div class="brand-logo">
          <img v-if="logoOk" src="/logo.png" alt="logo" @error="logoOk = false" />
          <el-icon v-else :size="40"><Cpu /></el-icon>
        </div>

        <h1>Virtual Path MES</h1>
        <p class="brand-slogan">虚拟路径MES系统</p>

        <div class="feature-list">
          <div class="feature-item">
            <span class="fi-icon"><el-icon><Monitor /></el-icon></span>
            <div class="fi-text">
              <b>数字孪生监控</b>
              <span>3D 工厂实时孪生 · 设备状态一屏总览</span>
            </div>
          </div>
          <div class="feature-item">
            <span class="fi-icon"><el-icon><MagicStick /></el-icon></span>
            <div class="fi-text">
              <b>AI 智能洞察</b>
              <span>生产风险预测 · 告警根因分析 · 智能问答</span>
            </div>
          </div>
          <div class="feature-item">
            <span class="fi-icon"><el-icon><Connection /></el-icon></span>
            <div class="fi-text">
              <b>全链路追溯</b>
              <span>工单 · 工艺 · 质检 · 报表闭环管理</span>
            </div>
          </div>
          <div class="feature-item">
            <span class="fi-icon"><el-icon><DataAnalysis /></el-icon></span>
            <div class="fi-text">
              <b>实时数据流</b>
              <span>MQTT · Kafka · WebSocket 秒级推送</span>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <p>© 2026 Virtual Path MES · MES v1.0.50</p>
      </div>
    </div>

    <!-- ===== 右侧登录区 ===== -->
    <div class="login-right">
      <div class="login-box">
        <div class="login-header">
          <h2>系统登录</h2>
          <p>Virtual Path MES 制造执行系统</p>
        </div>

        <!-- 系统状态提示 -->
        <div class="sys-status" :class="sysOnline ? 'ok' : 'down'">
          <i></i>
          <span v-if="sysChecking">正在检测系统状态…</span>
          <span v-else-if="sysOnline">系统服务正常 · 响应 {{ sysLatency }}ms</span>
          <span v-else>后端服务未启动或不可达，请先启动 mes-auth</span>
        </div>

        <!-- 企业公告 -->
        <div class="notice-bar">
          <el-icon><Bell /></el-icon>
          <div class="notice-scroll">
            <span class="notice-text">{{ currentNotice }}</span>
          </div>
        </div>

        <el-form
          ref="loginFormRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="工号 / 用户名" size="large" autocomplete="username">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              autocomplete="current-password"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-form-item prop="captcha">
            <div class="captcha-row">
              <el-input
                v-model="form.captcha"
                placeholder="验证码"
                size="large"
                class="captcha-input"
              >
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
              <canvas
                ref="captchaRef"
                class="captcha-canvas"
                width="112"
                height="40"
                title="点击刷新验证码"
                @click="drawCaptcha"
              ></canvas>
            </div>
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <span class="admin-tip">账号由管理员统一开通</span>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form>

        <!-- 演示账号快捷填充 -->
        <div class="demo-section">
          <div class="demo-label">
            <span class="demo-line"></span>
            演示账号 · 点击填充
            <span class="demo-line"></span>
          </div>
          <div class="demo-chips">
            <span
              v-for="acc in demoAccounts"
              :key="acc.username"
              class="demo-chip"
              :class="{ active: form.username === acc.username }"
              @click="fillDemo(acc)"
            >
              {{ acc.label }} <em>{{ acc.username }}</em>
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, FormInstance } from 'element-plus'
import { Cpu, Monitor, MagicStick, Connection, DataAnalysis, User, Lock, Key, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)
const logoOk = ref(true)
const captchaRef = ref<HTMLCanvasElement>()
const captchaText = ref('')

const form = reactive({ username: '', password: '', captcha: '' })

const demoAccounts = [
  { label: '管理员', username: 'admin' },
  { label: '生产主管', username: 'zhangsan' },
  { label: '操作员', username: 'lisi' },
  { label: '质检员', username: 'wangwu' },
  { label: '工程师', username: 'zhaoliu' },
]

/* ===== 企业公告（轮播） ===== */
const notices = [
  '【通知】本周五 18:00 进行系统例行维护，请提前保存工作数据',
  '【安全】禁止将账号借予他人使用，账号操作全程留痕审计',
  '【生产】8 月排产计划已下发，请各班组在排产看板确认',
  '【质量】上月一次合格率 98.6%，较上月提升 0.4 个百分点',
]
const noticeIndex = ref(0)
const currentNotice = computed(() => notices[noticeIndex.value])
let noticeTimer: number | null = null

/* ===== 图形验证码（canvas） ===== */
const drawCaptcha = () => {
  const canvas = captchaRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  let text = ''
  for (let i = 0; i < 4; i++) {
    text += chars[Math.floor(Math.random() * chars.length)]
  }
  captchaText.value = text

  const w = canvas.width
  const h = canvas.height
  ctx.clearRect(0, 0, w, h)

  const dark = themeStore.isDark
  ctx.fillStyle = dark ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.02)'
  ctx.fillRect(0, 0, w, h)

  // 干扰线
  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(${dark ? '139,92,246' : '99,102,241'}, ${0.25 + Math.random() * 0.3})`
    ctx.beginPath()
    ctx.moveTo(Math.random() * w, Math.random() * h)
    ctx.lineTo(Math.random() * w, Math.random() * h)
    ctx.stroke()
  }

  // 字符
  for (let i = 0; i < text.length; i++) {
    ctx.save()
    ctx.font = `bold ${22 + Math.random() * 4}px Consolas, monospace`
    ctx.fillStyle = dark ? '#c7d2fe' : '#4338ca'
    ctx.translate(18 + i * 24, h / 2 + 2)
    ctx.rotate((Math.random() - 0.5) * 0.5)
    ctx.fillText(text[i], -6, 7)
    ctx.restore()
  }

  // 噪点
  for (let i = 0; i < 24; i++) {
    ctx.fillStyle = `rgba(${dark ? '255,255,255' : '0,0,0'}, ${0.05 + Math.random() * 0.08})`
    ctx.fillRect(Math.random() * w, Math.random() * h, 1.6, 1.6)
  }
}

/* ===== 系统状态检测 ===== */
const sysOnline = ref(false)
const sysChecking = ref(true)
const sysLatency = ref(0)
let sysTimer: number | null = null

const probeSystem = async () => {
  sysChecking.value = true
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 4000)
  const t0 = performance.now()
  try {
    await fetch('/auth/actuator/health', { signal: controller.signal, cache: 'no-store' })
    sysOnline.value = true
    sysLatency.value = Math.round(performance.now() - t0)
  } catch {
    sysOnline.value = false
  } finally {
    clearTimeout(timer)
    sysChecking.value = false
  }
}

/* ===== 校验 ===== */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度 2-20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 4, max: 32, message: '长度 4-32 个字符', trigger: 'blur' },
  ],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

const fillDemo = (acc: { username: string }) => {
  form.username = acc.username
  form.password = 'admin123'
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    // 验证码校验（忽略大小写）
    if (form.captcha.trim().toUpperCase() !== captchaText.value.toUpperCase()) {
      ElMessage.error('验证码错误')
      form.captcha = ''
      drawCaptcha()
      return
    }

    loading.value = true
    try {
      await userStore.login(form.username, form.password, rememberMe.value)
      ElMessage.success('登录成功')
      localStorage.setItem('mes-last-username', form.username)
      router.push('/')
    } catch (error: any) {
      ElMessage.error(error?.response?.data?.message || error?.message || '登录失败')
      drawCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  drawCaptcha()
  probeSystem()
  sysTimer = window.setInterval(probeSystem, 15000)
  noticeTimer = window.setInterval(() => {
    noticeIndex.value = (noticeIndex.value + 1) % notices.length
  }, 6000)
  const last = localStorage.getItem('mes-last-username')
  if (last) form.username = last
})

onUnmounted(() => {
  if (sysTimer) clearInterval(sysTimer)
  if (noticeTimer) clearInterval(noticeTimer)
})
</script>

<style scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: var(--bg-app);
}

/* ===== 左侧品牌区（固定暗色科技风） ===== */
.login-left {
  flex: 0 0 46%;
  width: 46%;
  background: linear-gradient(160deg, #101828 0%, #0b1222 55%, #090d18 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 48px 56px;
  position: relative;
  overflow: hidden;
}
.left-bg { position: absolute; inset: 0; pointer-events: none; }
.bg-circle { position: absolute; border-radius: 50%; }
.circle-1 {
  width: 460px; height: 460px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.22) 0%, transparent 70%);
  top: -160px; right: -120px;
  animation: drift 18s ease-in-out infinite alternate;
}
.circle-2 {
  width: 340px; height: 340px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.16) 0%, transparent 70%);
  bottom: -120px; left: -90px;
  animation: drift 22s ease-in-out infinite alternate-reverse;
}
.circle-3 {
  width: 220px; height: 220px;
  background: radial-gradient(circle, rgba(34, 211, 238, 0.12) 0%, transparent 70%);
  top: 42%; left: 18%;
  animation: drift 16s ease-in-out infinite alternate;
}
@keyframes drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(26px, -20px) scale(1.08); }
}
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(99, 102, 241, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99, 102, 241, 0.06) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at 40% 50%, black 30%, transparent 75%);
  -webkit-mask-image: radial-gradient(ellipse at 40% 50%, black 30%, transparent 75%);
}
.bg-particle {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(139, 92, 246, 0.8);
  filter: blur(1px);
  animation: float-up 9s ease-in-out infinite;
}
.p1 { left: 15%; bottom: 8%; animation-delay: 0s; }
.p2 { left: 32%; bottom: 20%; animation-delay: 2.2s; background: rgba(34, 211, 238, 0.7); }
.p3 { left: 55%; bottom: 12%; animation-delay: 4.5s; }
.p4 { left: 72%; bottom: 26%; animation-delay: 6.8s; background: rgba(34, 211, 238, 0.7); }
@keyframes float-up {
  0%, 100% { transform: translateY(0); opacity: 0.35; }
  50% { transform: translateY(-46px); opacity: 0.9; }
}

.brand-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  z-index: 1;
}
.brand-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 22px;
}
.brand-logo img {
  width: 68px; height: 68px;
  border-radius: 16px;
  object-fit: cover;
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.45);
}
.brand-logo .el-icon {
  width: 68px; height: 68px;
  border-radius: 16px;
  background: var(--gradient-primary);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.45);
}
.login-left h1 {
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  text-align: center;
  letter-spacing: 1px;
  background: linear-gradient(120deg, #fff 30%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.brand-slogan {
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-size: 14px;
  letter-spacing: 3px;
  margin-top: 8px;
  margin-bottom: 34px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 0 40px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.fi-icon {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 11px;
  background: rgba(99, 102, 241, 0.16);
  border: 1px solid rgba(99, 102, 241, 0.35);
  color: #818cf8;
  flex-shrink: 0;
  font-size: 18px;
}
.feature-item:nth-child(2) .fi-icon { color: #c084fc; border-color: rgba(192, 132, 252, 0.35); background: rgba(192, 132, 252, 0.14); }
.feature-item:nth-child(3) .fi-icon { color: #22d3ee; border-color: rgba(34, 211, 238, 0.35); background: rgba(34, 211, 238, 0.12); }
.feature-item:nth-child(4) .fi-icon { color: #34d399; border-color: rgba(52, 211, 153, 0.35); background: rgba(52, 211, 153, 0.12); }
.fi-text { display: flex; flex-direction: column; gap: 2px; }
.fi-text b { color: rgba(255, 255, 255, 0.92); font-size: 14px; font-weight: 600; }
.fi-text span { color: rgba(255, 255, 255, 0.45); font-size: 12px; }

.brand-footer p {
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  text-align: center;
  position: relative;
  z-index: 1;
}

/* ===== 右侧登录区（主题兼容） ===== */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}
.login-box { width: 400px; }

.login-header { margin-bottom: 18px; }
.login-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.login-header p { font-size: 13px; color: var(--text-muted); }

.sys-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 12px;
  margin-bottom: 12px;
}
.sys-status i { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; }
.sys-status.ok { background: var(--success-light); color: var(--success); }
.sys-status.ok i { background: var(--success); box-shadow: 0 0 8px var(--success); animation: pulse-green 2s ease-in-out infinite; }
.sys-status.down { background: var(--warning-light); color: var(--warning); }
.sys-status.down i { background: var(--warning); }
@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 企业公告 */
.notice-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--accent-light);
  border: 1px dashed var(--accent);
  margin-bottom: 18px;
  overflow: hidden;
}
.notice-bar .el-icon { color: var(--accent); flex-shrink: 0; }
.notice-scroll { flex: 1; overflow: hidden; }
.notice-text {
  display: inline-block;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  animation: notice-in 0.5s ease;
}
@keyframes notice-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-form :deep(.el-form-item) { margin-bottom: 18px; }
.login-form :deep(.el-input__wrapper) {
  border-radius: 9px;
  padding: 3px 14px;
  background: var(--bg-card);
}
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
}
.login-form :deep(.el-input__inner) { height: 42px; color: var(--text-primary); }

/* 验证码 */
.captcha-row { display: flex; gap: 10px; width: 100%; }
.captcha-input { flex: 1; }
.captcha-canvas {
  width: 112px;
  height: 40px;
  border-radius: 9px;
  border: 1px solid var(--border-light);
  background: var(--bg-card);
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color var(--transition-fast);
}
.captcha-canvas:hover { border-color: var(--accent); }

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.form-options :deep(.el-checkbox__label) { color: var(--text-muted); font-size: 13px; }
.admin-tip { font-size: 12px; color: var(--text-muted); }

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  border-radius: 9px;
  background: var(--gradient-primary);
  border: none;
  transition: all var(--transition-normal);
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 26px rgba(99, 102, 241, 0.4);
}

/* 演示账号 */
.demo-section { margin-top: 26px; }
.demo-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 10px;
}
.demo-line { flex: 1; height: 1px; background: var(--border-light); }
.demo-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.demo-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 11px;
  border-radius: 16px;
  font-size: 11.5px;
  color: var(--text-secondary);
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;
}
.demo-chip em { font-style: normal; color: var(--text-muted); font-family: Consolas, monospace; }
.demo-chip:hover {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-light);
  transform: translateY(-1px);
}
.demo-chip.active {
  color: var(--accent);
  border-color: var(--accent);
  background: var(--accent-light);
  font-weight: 600;
}

@media (max-width: 1024px) {
  .login-page { flex-direction: column; }
  .login-left { flex: none; width: 100%; min-height: 240px; padding: 32px 24px; }
  .feature-list { display: none; }
  .brand-content { flex-direction: row; align-items: center; gap: 18px; justify-content: center; }
  .brand-logo { margin-bottom: 0; }
  .brand-logo img { width: 48px; height: 48px; }
  .brand-slogan { display: none; }
  .brand-footer { display: none; }
  .login-right { padding: 28px 20px; }
}
</style>
