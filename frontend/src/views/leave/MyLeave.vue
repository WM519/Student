<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>我的请假</span>
        <div class="right">
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="load(1)">
            <el-option v-for="(label, value) in LEAVE_STATUS" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button type="primary" @click="$router.push('/leave/apply')">发起请假</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="leaveNo" label="请假单号" width="180" />
      <el-table-column prop="leaveType" label="类型" width="80" />
      <el-table-column label="请假时间" min-width="210">
        <template #default="{ row }">
          {{ row.startDate }} ~ {{ row.endDate }}
          <el-tag size="small" type="warning">{{ row.days }} 天</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="事由" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="140">
        <template #default="{ row }">
          <el-tag :type="tagOf(row.status)">{{ textOf(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.status === 'PENDING_HEAD'"
            size="small"
            type="warning"
            @click="withdraw(row)"
          >
            撤销
          </el-button>
          <el-button
            v-if="canCancel(row)"
            size="small"
            type="success"
            @click="cancelLeave(row)"
          >
            销假
          </el-button>
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

    <leave-detail-drawer v-model="drawerVisible" :leave-id="currentRow?.id" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { leaveApi } from '@/api/leave'
import { LEAVE_STATUS, LEAVE_STATUS_TAG } from '@/utils/dict'
import LeaveDetailDrawer from '@/components/LeaveDetailDrawer.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const statusFilter = ref('')
const drawerVisible = ref(false)
const currentRow = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10 })

function textOf(status) {
  return LEAVE_STATUS[status] || status
}
function tagOf(status) {
  return LEAVE_STATUS_TAG[status] || 'info'
}

// 已通过且请假已开始（今天 ≥ 开始日期）才允许销假
function canCancel(row) {
  return row.status === 'APPROVED' && !dayjs(row.startDate).isAfter(dayjs().format('YYYY-MM-DD'))
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await leaveApi.my({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      status: statusFilter.value || undefined
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

async function withdraw(row) {
  await ElMessageBox.confirm(`确认撤销请假单 ${row.leaveNo} 吗？`, '撤销申请', { type: 'warning' })
  await leaveApi.withdraw(row.id)
  ElMessage.success('已撤销')
  load()
}

async function cancelLeave(row) {
  await ElMessageBox.confirm(
    `确认办理销假吗？销假后将结束请假单 ${row.leaveNo}。`,
    '销假',
    { type: 'success' }
  )
  await leaveApi.cancel(row.id)
  ElMessage.success('销假成功')
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
.right {
  display: flex;
  gap: 10px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
