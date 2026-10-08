import request from '@/utils/request'

export const studentApi = {
  page(params) {
    return request.get('/students', { params })
  },
  save(data) {
    return request.post('/students', data)
  },
  update(id, data) {
    return request.put(`/students/${id}`, data)
  },
  remove(id) {
    return request.delete(`/students/${id}`)
  },
  toggleStatus(id, status) {
    return request.put(`/students/${id}/status/${status}`)
  },
  resetPassword(id) {
    return request.put(`/students/${id}/reset-password`)
  }
}
