import { defineStore } from 'pinia'
import router, { buildMenuRoutes } from '@/router'
import { loginApi } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useLoginStore = defineStore('login', {
  state: () => ({
    token: getToken(),
    user: null,
    menus: [],
    registeredRouteNames: []
  }),

  getters: {
    isLogin: (state) => !!state.token,
    roleCodes: (state) => (state.user?.roles || []).map((r) => r.code),
    hasRole: (state) => (code) => (state.user?.roles || []).some((r) => r.code === code)
  },

  actions: {
    async login(form) {
      const data = await loginApi.login(form)
      this.token = data.token
      this.user = data.user
      this.menus = data.user?.menus || []
      setToken(this.token)
      this.registerMenus(this.menus)
      return data
    },

    async initUserAndRoutes() {
      const user = await loginApi.info()
      this.user = user
      this.menus = user?.menus || []
      this.registerMenus(this.menus)
    },

    /** 把后端返回的菜单注册为 Layout 的子路由 */
    registerMenus(menus) {
      const routes = buildMenuRoutes(menus)
      const existing = new Set(router.getRoutes().map((r) => r.path))
      routes.forEach((route) => {
        if (!router.hasRoute(route.name) && !existing.has(route.path)) {
          router.addRoute('Root', route)
          existing.add(route.path)
          this.registeredRouteNames.push(route.name)
        }
      })
    },

    clearRoutes() {
      this.registeredRouteNames.forEach((name) => {
        if (router.hasRoute(name)) {
          router.removeRoute(name)
        }
      })
      this.registeredRouteNames = []
    },

    async logout() {
      try {
        await loginApi.logout()
      } catch {
        // 忽略登出接口异常
      }
      this.reset()
      router.push('/login')
    },

    reset() {
      this.clearRoutes()
      this.token = ''
      this.user = null
      this.menus = []
      removeToken()
    }
  }
})
