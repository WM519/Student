import request from '@/utils/request'

export const collegeApi = {
  page(params) {
    return request.get('/colleges', { params })
  },
  all() {
    return request.get('/colleges/all')
  },
  save(data) {
    return request.post('/colleges', data)
  },
  update(id, data) {
    return request.put(`/colleges/${id}`, data)
  },
  remove(id) {
    return request.delete(`/colleges/${id}`)
  }
}
