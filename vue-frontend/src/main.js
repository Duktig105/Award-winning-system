import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import './assets/css/global.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

axios.interceptors.request.use(config => {
  const token = localStorage.getItem('saims_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const app = createApp(App)

app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
