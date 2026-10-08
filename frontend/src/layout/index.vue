<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo">
        <el-icon :size="24" color="#409eff"><School /></el-icon>
        <span v-show="!collapsed" class="logo-text">请假管理系统</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          background-color="#001529"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <sidebar-item v-for="item in loginStore.menus" :key="item.id" :item="item" />
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path || item.title">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="handleCommand">
          <div class="user-box">
            <el-avatar :size="30" style="background:#409eff">
              {{ (loginStore.user?.realName || '用')[0] }}
            </el-avatar>
            <span class="user-name">{{ loginStore.user?.realName }}</span>
            <el-tag size="small" type="primary" class="role-tag">
              {{ (loginStore.user?.roles || []).map((r) => r.name).join('、') }}
            </el-tag>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>账号：{{ loginStore.user?.username }}</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useLoginStore } from '@/stores/login'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const loginStore = useLoginStore()
const collapsed = ref(false)

const breadcrumbs = computed(() => {
  const matched = route.matched
    .filter((r) => r.meta?.title)
    .map((r) => ({ path: r.path, title: r.meta.title }))
  if (!matched.length) {
    matched.push({ title: '首页' })
  }
  return matched
})

function handleCommand(command) {
  if (command === 'logout') {
    loginStore.logout()
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.layout-aside {
  background-color: #001529;
  transition: width 0.2s;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
}
.logo-text {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.layout-aside :deep(.el-menu) {
  border-right: none;
}
.layout-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}
.user-name {
  font-size: 14px;
  color: #303133;
}
.role-tag {
  max-width: 180px;
}
.layout-main {
  background: #f0f2f5;
  padding: 16px;
  overflow: auto;
}
</style>
