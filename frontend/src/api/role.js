import request from '@/utils/request'

export const roleApi = {
  page(params) {
    return request.get('/roles', { params })
  },
  all() {
    return request.get('/roles/all')
  },
  save(data) {
    return request.post('/roles', data)
  },
  update(id, data) {
    return request.put(`/roles/${id}`, data)
  },
  remove(id) {
    return request.delete(`/roles/${id}`)
  },
  menuIds(id) {
    return request.get(`/roles/${id}/menus`)
  },
  assignMenus(id, menuIds) {
    return request.put(`/roles/${id}/menus`, { roleId: id, menuIds })
  }
}
