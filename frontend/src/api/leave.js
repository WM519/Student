import request from '@/utils/request'

export const leaveApi = {
  apply(data) {
    return request.post('/leaves/apply', data)
  },
  my(params) {
    return request.get('/leaves/my', { params })
  },
  all(params) {
    return request.get('/leaves/all', { params })
  },
  approval(params) {
    return request.get('/leaves/approval', { params })
  },
  detail(id) {
    return request.get(`/leaves/${id}/detail`)
  },
  approve(id, data) {
    return request.post(`/leaves/${id}/approve`, data)
  },
  reject(id, data) {
    return request.post(`/leaves/${id}/reject`, data)
  },
  withdraw(id) {
    return request.post(`/leaves/${id}/withdraw`)
  },
  cancel(id) {
    return request.post(`/leaves/${id}/cancel`)
  }
}
