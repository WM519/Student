<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>{{ pageTitle }}</span>
        <el-input
          v-model="query.keyword"
          placeholder="搜索学生姓名 / 请假单号"
          clearable
          style="width: 260px"
          @keyup.enter="load(1)"
          @clear="load(1)"
        >
          <template #append>
            <el-button :icon="Search" @click="load(1)" />
          </template>
        </el-input>
      </div>
    </template>

    <el-tabs v-model="activeTab" @tab-change="load(1)">
      <el-tab-pane label="待我审批" name="pending" />
      <el-tab-pane label="我已处理" name="done" />
    </el-tabs>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="leaveNo" label="请假单号" width="170" />
      <el-table-column prop="studentName" label="学生" width="110" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="className" label="班级" width="130" />
      <el-table-column prop="collegeName" label="学院" width="140" />
      <el-table-column prop="leaveType" label="类型" width="80" />
      <el-table-column label="请假时间" min-width="210">
        <template #default="{ row }">
          {{ row.startDate }} ~ {{ row.endDate }}
          <el-tag size="small" type="warning">{{ row.days }} 天</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="tagOf(row.status)">{{ textOf(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openDetail(row)">查看 / 处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      @size-change="load(1)"
      @current-change="load()"
    />

    <leave-detail-drawer
      v-model="drawerVisible"
      :leave-id="currentRow?.id"
      :can-operate="activeTab === 'pending'"
      @approve="handleApprove"
      @reject="handleReject"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { leaveApi } from '@/api/leave'
import { LEAVE_STATUS, LEAVE_STATUS_TAG } from '@/utils/dict'
import LeaveDetailDrawer from '@/components/LeaveDetailDrawer.vue'

const route = useRoute()
const activeTab = ref('pending')
const level = computed(() => {
  if (route.path.includes('counselor')) return 2
  if (route.path.includes('leader')) return 3
  return 1
})
const pageTitle = computed(() => {
  if (level.value === 2) return '辅导员审批'
  if (level.value === 3) return '院领导审批'
  return '班主任审批'
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)
const currentRow = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

function textOf(status) {
  return LEAVE_STATUS[status] || status
}
function tagOf(status) {
  return LEAVE_STATUS_TAG[status] || 'info'
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await leaveApi.approval({
      level: level.value,
      done: activeTab.value === 'done',
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  currentRow.value = row
  drawerVisible.value = true
}

async function handleApprove({ id, comment }) {
  await ElMessageBox.confirm('确认同意该请假申请？', '审批', { type: 'warning' })
  await leaveApi.approve(id, { comment })
  ElMessage.success('已同意')
  drawerVisible.value = false
  load()
}

async function handleReject({ id, comment }) {
  await ElMessageBox.confirm('确认驳回该请假申请？', '审批', { type: 'warning' })
  await leaveApi.reject(id, { comment })
  ElMessage.success('已驳回')
  drawerVisible.value = false
  load()
}

onMounted(() => load())
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
