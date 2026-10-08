import request from '@/utils/request'

export const menuApi = {
  tree() {
    return request.get('/menus/tree')
  },
  list() {
    return request.get('/menus')
  },
  save(data) {
    return request.post('/menus', data)
  },
  update(id, data) {
    return request.put(`/menus/${id}`, data)
  },
  remove(id) {
    return request.delete(`/menus/${id}`)
  }
}
