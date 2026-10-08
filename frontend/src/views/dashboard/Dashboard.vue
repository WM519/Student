<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="12" :md="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <el-icon :size="34" :color="card.color"><component :is="card.icon" /></el-icon>
            <div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col v-if="summary.statusItems && summary.statusItems.length" :xs="24" :md="12">
        <el-card shadow="never" header="请假状态分布">
          <div ref="statusChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col v-if="summary.collegeItems && summary.collegeItems.length" :xs="24" :md="12">
        <el-card shadow="never" header="各学院请假数量">
          <div ref="collegeChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col v-if="summary.typeItems && summary.typeItems.length" :xs="24" :md="12">
        <el-card shadow="never" header="请假类型统计">
          <div ref="typeChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="isTeacher" shadow="never" class="chart-row">
      <template #header>审批待办说明</template>
      <el-empty description="当前没有待办事项，可前往左侧“审批中心”查看详细列表" v-if="summary.pending === 0" />
      <el-table v-else :data="teacherRows" border>
        <el-table-column prop="level" label="审批环节" width="180" />
        <el-table-column prop="count" label="待办数量" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="goApproval(row)">去审批</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api/dashboard'
import { useLoginStore } from '@/stores/login'
import { statusText } from '@/utils/dict'

const router = useRouter()
const loginStore = useLoginStore()
const summary = ref({})
const statusChartRef = ref()
const collegeChartRef = ref()
const typeChartRef = ref()
let charts = []

const isTeacher = computed(() => loginStore.user?.userType === 'TEACHER')
const isStudent = computed(() => loginStore.user?.userType === 'STUDENT')

const cards = computed(() => {
  const s = summary.value || {}
  if (isTeacher.value) {
    return [
      { label: '班主任待办', value: s.headPending || 0, icon: 'UserFilled', color: '#409eff' },
      { label: '辅导员待办', value: s.counselorPending || 0, icon: 'Avatar', color: '#e6a23c' },
      { label: '院领导待办', value: s.leaderPending || 0, icon: 'OfficeBuilding', color: '#67c23a' },
      { label: '待办总数', value: s.pending || 0, icon: 'Bell', color: '#f56c6c' }
    ]
  }
  if (isStudent.value) {
    return [
      { label: '请假总次数', value: s.total || 0, icon: 'Tickets', color: '#409eff' },
      { label: '审批中', value: s.pending || 0, icon: 'Loading', color: '#e6a23c' },
      { label: '已通过', value: s.approved || 0, icon: 'CircleCheck', color: '#67c23a' },
      { label: '已销假', value: s.cancelled || 0, icon: 'Finished', color: '#909399' }
    ]
  }
  return [
    { label: '请假总数', value: s.total || 0, icon: 'Tickets', color: '#409eff' },
    { label: '审批中', value: s.pending || 0, icon: 'Loading', color: '#e6a23c' },
    { label: '已通过', value: s.approved || 0, icon: 'CircleCheck', color: '#67c23a' },
    { label: '已驳回', value: s.rejected || 0, icon: 'CircleClose', color: '#f56c6c' }
  ]
})

const teacherRows = computed(() => {
  const s = summary.value || {}
  return [
    { level: '班主任审批（第一环节）', count: s.headPending || 0, path: '/approval/head' },
    { level: '辅导员审批（第二环节）', count: s.counselorPending || 0, path: '/approval/counselor' },
    { level: '院领导审批（第三环节）', count: s.leaderPending || 0, path: '/approval/leader' }
  ]
})

function goApproval(row) {
  router.push(row.path)
}

function renderPie(el, items) {
  if (!el || !items || !items.length) return
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['36%', '64%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: items.map((i) => ({ name: statusText(i.name) || i.name, value: i.value }))
      }
    ]
  })
  charts.push(chart)
}

function renderBar(el, items, color) {
  if (!el || !items || !items.length) return
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 40 },
    xAxis: {
      type: 'category',
      data: items.map((i) => i.name),
      axisLabel: { interval: 0 }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        barMaxWidth: 42,
        itemStyle: { color: color || '#409eff', borderRadius: [6, 6, 0, 0] },
        data: items.map((i) => i.value)
      }
    ]
  })
  charts.push(chart)
}

async function load() {
  summary.value = await dashboardApi.summary()
  await nextTick()
  charts.forEach((c) => c.dispose())
  charts = []
  if (summary.value.statusItems?.length) renderPie(statusChartRef.value, summary.value.statusItems)
  if (summary.value.collegeItems?.length) renderBar(collegeChartRef.value, summary.value.collegeItems, '#67c23a')
  if (summary.value.typeItems?.length) renderPie(typeChartRef.value, summary.value.typeItems)
}

function handleResize() {
  charts.forEach((c) => c.resize())
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}
.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.stat-label {
  color: #909399;
  font-size: 13px;
}
.chart-row {
  margin-top: 4px;
}
.chart-row .el-card {
  margin-bottom: 16px;
}
.chart {
  height: 320px;
}
</style>
