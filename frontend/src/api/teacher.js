import request from '@/utils/request'

export const teacherApi = {
  page(params) {
    return request.get('/teachers', { params })
  },
  options(params) {
    return request.get('/teachers/options', { params })
  },
  save(data) {
    return request.post('/teachers', data)
  },
  update(id, data) {
    return request.put(`/teachers/${id}`, data)
  },
  remove(id) {
    return request.delete(`/teachers/${id}`)
  },
  toggleStatus(id, status) {
    return request.put(`/teachers/${id}/status/${status}`)
  },
  resetPassword(id) {
    return request.put(`/teachers/${id}/reset-password`)
  }
}
