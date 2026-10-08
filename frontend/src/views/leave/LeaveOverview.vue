<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>请假总览（全部记录）</span>
        <div class="filters">
          <el-input
            v-model="query.keyword"
            placeholder="学生姓名 / 学号 / 单号"
            clearable
            style="width: 220px"
            @keyup.enter="load(1)"
            @clear="load(1)"
          />
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 150px" @change="load(1)">
            <el-option v-for="(label, value) in LEAVE_STATUS" :key="value" :label="label" :value="value" />
          </el-select>
          <el-select v-model="query.collegeId" placeholder="学院" clearable style="width: 160px" @change="load(1)">
            <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
          </el-select>
          <el-select v-model="query.leaveType" placeholder="类型" clearable style="width: 120px" @change="load(1)">
            <el-option v-for="t in LEAVE_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="leaveNo" label="请假单号" width="180" />
      <el-table-column prop="studentName" label="学生" width="100" />
      <el-table-column prop="studentNo" label="学号" width="110" />
      <el-table-column prop="className" label="班级" width="120" />
      <el-table-column prop="collegeName" label="学院" min-width="130" show-overflow-tooltip />
      <el-table-column prop="leaveType" label="类型" width="70" />
      <el-table-column label="请假时间" min-width="200">
        <template #default="{ row }">
          {{ row.startDate }} ~ {{ row.endDate }}
          <el-tag size="small" type="warning">{{ row.days }} 天</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="tagOf(row.status)">{{ textOf(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
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
import { Search } from '@element-plus/icons-vue'
import { leaveApi } from '@/api/leave'
import { collegeApi } from '@/api/college'
import { LEAVE_STATUS, LEAVE_STATUS_TAG, LEAVE_TYPES } from '@/utils/dict'
import LeaveDetailDrawer from '@/components/LeaveDetailDrawer.vue'

const rows = ref([])
const colleges = ref([])
const total = ref(0)
const loading = ref(false)
const drawerVisible = ref(false)
const currentRow = ref(null)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  collegeId: undefined,
  leaveType: ''
})

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
    const data = await leaveApi.all({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      collegeId: query.collegeId,
      leaveType: query.leaveType || undefined
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

onMounted(async () => {
  colleges.value = await collegeApi.all()
  load()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.filters {
  display: flex;
  gap: 10px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
