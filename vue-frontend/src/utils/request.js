import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: 'http://localhost:9998',
  timeout: 30000
})

/**
 * 请求拦截器
 * 在发送请求前对请求配置进行统一处理
 * 功能：设置请求头、添加认证信息等
 */
request.interceptors.request.use(config => {
  const token = localStorage.getItem('saims_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  // 只在未设置Content-Type时才设置为JSON格式
  // 避免覆盖multipart/form-data等其他类型
  if (!config.headers['Content-Type']) {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';
  }
  return config;
}, error => {
  // 请求配置出错时的处理
  return Promise.reject(error);
})

request.interceptors.response.use(
  response => {
    let res = response.data
    if (typeof res === 'string') {
      res = res ? JSON.parse(res) : res
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('saims_token')
      localStorage.removeItem('saims_user')
      if (window.location.pathname !== '/login') window.location.href = '/login'
    }
    if (error.response && error.response.status === 404) {
      ElMessage.error('未找到请求接口')
    } else if (error.response && error.response.status === 500) {
      ElMessage.error('系统异常')
    } else {
      console.error(error.message)
    }
    return Promise.reject(error)
  }
)

export default request
