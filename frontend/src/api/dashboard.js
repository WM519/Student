import request from '@/utils/request'

export const dashboardApi = {
  summary() {
    return request.get('/dashboard/summary')
  }
}
