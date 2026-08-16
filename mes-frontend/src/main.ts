/**
 * Virtual Path MES - Main Entry
 * 虚拟路径MES系统入口文件
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import router from './router'
import App from './App.vue'
import './styles/global.scss'
import './styles/table-modern.scss'
import './utils/echarts'
import { vPermission } from './directives/permission'

const app = createApp(App)
const pinia = createPinia()

app.component('VChart', VChart)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.directive('permission', vPermission)
app.mount('#app')