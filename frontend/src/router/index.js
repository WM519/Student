import { createRouter, createWebHistory } from 'vue-router'

const modules = import.meta.glob('../views/**/*.vue')

export function loadView(path) {
  if (!path) return null
  const key = `../views/${path}.vue`
  if (modules[key]) {
    return modules[key]
  }
  console.warn(`未找到视图组件: ${path}，使用 404 兜底`)
  return modules['../views/dashboard/Dashboard.vue']
}

export function buildMenuRoutes(menus) {
  const routes = []
  const walk = (list) => {
    if (!list) return
    list.forEach((m) => {
      if (m.menuType === 'MENU' && m.component) {
        routes.push({
          path: m.path,
          name: `menu-${m.id}`,
          component: loadView(m.component),
          meta: { title: m.menuName, icon: m.icon, menuId: m.id }
        })
      }
      if (m.children && m.children.length) {
        walk(m.children)
      }
    })
  }
  walk(menus)
  return routes
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/',
      name: 'Root',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          // 仪表盘作为静态路由常驻，避免登录前 /dashboard 落入兜底路由造成死循环
          path: '/dashboard',
          name: 'DashboardHome',
          component: () => import('@/views/dashboard/Dashboard.vue'),
          meta: { title: '首页' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

router.beforeEach(async (to) => {
  const { useLoginStore } = await import('@/stores/login')
  const loginStore = useLoginStore()

  if (to.meta?.public) {
    if (loginStore.token && to.path === '/login') {
      return '/'
    }
    return true
  }

  if (!loginStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!loginStore.user) {
    try {
      await loginStore.initUserAndRoutes()
    } catch {
      loginStore.reset()
      return '/login'
    }
  }

  // 刷新后首次导航动态路由可能尚未注册而命中兜底路由：
  // 先完成用户初始化，再判断目标地址是否已可解析，避免无意义的死循环
  const isCatchAll = to.matched.some((r) => r.path === '/:pathMatch(.*)*')
  if (isCatchAll && to.path !== '/') {
    const resolved = router.resolve(to.fullPath)
    const stillCatchAll = resolved.matched.some((r) => r.path === '/:pathMatch(.*)*')
    if (stillCatchAll) {
      return '/dashboard'
    }
    return { path: to.fullPath, replace: true }
  }
  return true
})

export default router
