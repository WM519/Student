import request from '@/utils/request'

export const loginApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  info() {
    return request.get('/auth/info')
  },
  logout() {
    return request.post('/auth/logout')
  }
}
