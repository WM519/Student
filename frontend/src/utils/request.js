import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截器：自动携带 token
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function toLogin() {
  removeToken()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// 响应拦截器：解包 Result {code,message,data}
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 401) {
        toLogin()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    if (status === 401) {
      ElMessage.error(message || '登录已过期，请重新登录')
      toLogin()
    } else {
      ElMessage.error(message || error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
