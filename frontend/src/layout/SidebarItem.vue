<template>
  <el-sub-menu v-if="hasChildren" :index="String(item.id)">
    <template #title>
      <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
      <span>{{ item.menuName }}</span>
    </template>
    <sidebar-item v-for="child in item.children" :key="child.id" :item="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="item.path">
    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
    <template #title>{{ item.menuName }}</template>
  </el-menu-item>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  item: { type: Object, required: true }
})

const hasChildren = computed(
  () => props.item.children && props.item.children.length > 0
)
</script>
