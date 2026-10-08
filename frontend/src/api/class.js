import request from '@/utils/request'

export const classApi = {
  page(params) {
    return request.get('/classes', { params })
  },
  all(params) {
    return request.get('/classes/all', { params })
  },
  save(data) {
    return request.post('/classes', data)
  },
  update(id, data) {
    return request.put(`/classes/${id}`, data)
  },
  remove(id) {
    return request.delete(`/classes/${id}`)
  }
}
