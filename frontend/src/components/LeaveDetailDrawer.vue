<template>
  <el-drawer
    :model-value="modelValue"
    title="请假详情"
    size="620px"
    @update:model-value="$emit('update:modelValue', $event)"
    @open="loadDetail"
  >
    <el-skeleton v-if="loading" :rows="8" animated />
    <template v-else-if="detail">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="请假单号" :span="2">{{ detail.leave.leaveNo }}</el-descriptions-item>
        <el-descriptions-item label="学生">{{ detail.leave.studentName }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ detail.leave.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ detail.leave.className }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ detail.leave.collegeName }}</el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ detail.leave.leaveType }}</el-descriptions-item>
        <el-descriptions-item label="请假时间" :span="2">
          {{ detail.leave.startDate }} ~ {{ detail.leave.endDate }}
          <el-tag size="small" class="ml-1">{{ detail.leave.days }} 天</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态" :span="2">
          <el-tag :type="tagOf(detail.leave.status)">{{ statusText(detail.leave.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请假事由" :span="2">{{ detail.leave.reason }}</el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">{{ detail.leave.applyTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.leave.actualReturnTime" label="销假时间" :span="2">
          {{ detail.leave.actualReturnTime }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">审批流转记录</el-divider>
      <el-timeline v-if="detail.audits.length">
        <el-timeline-item
          v-for="item in detail.audits"
          :key="item.id"
          :timestamp="item.auditTime"
          :type="tagOfAction(item.action)"
        >
          <div class="audit-row">
            <el-tag size="small" :type="tagOfAction(item.action)">
              {{ levelText(item.level) }} · {{ actionText(item.action) }}
            </el-tag>
            <span class="operator">{{ item.operatorName }}</span>
          </div>
          <div v-if="item.comment" class="audit-comment">意见：{{ item.comment }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流转记录" :image-size="60" />

      <template v-if="canOperate && isPending">
        <el-divider content-position="left">审批操作</el-divider>
        <el-input
          v-model="comment"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
          placeholder="请输入审批意见（可选）"
        />
        <div class="operate-btns">
          <el-button type="danger" :loading="submitting === 'reject'" @click="submitAction('reject')">
            驳 回
          </el-button>
          <el-button type="success" :loading="submitting === 'approve'" @click="submitAction('approve')">
            同 意
          </el-button>
        </div>
      </template>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref } from 'vue'
import { leaveApi } from '@/api/leave'
import {
  LEAVE_STATUS,
  LEAVE_STATUS_TAG,
  LEVEL_MAP,
  ACTION_MAP,
  ACTION_TAG
} from '@/utils/dict'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  leaveId: { type: [Number, String], default: null },
  canOperate: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'approve', 'reject'])

const detail = ref(null)
const loading = ref(false)
const comment = ref('')
const submitting = ref('')

const isPending = computed(() =>
  ['PENDING_HEAD', 'PENDING_COUNSELOR', 'PENDING_LEADER'].includes(detail.value?.leave?.status)
)

function statusText(status) {
  return LEAVE_STATUS[status] || status
}
function tagOf(status) {
  return LEAVE_STATUS_TAG[status] || 'info'
}
function levelText(level) {
  return LEVEL_MAP[level] || level
}
function actionText(action) {
  return ACTION_MAP[action] || action
}
function tagOfAction(action) {
  return ACTION_TAG[action] || 'info'
}

async function loadDetail() {
  if (!props.leaveId) return
  loading.value = true
  detail.value = null
  comment.value = ''
  try {
    detail.value = await leaveApi.detail(props.leaveId)
  } finally {
    loading.value = false
  }
}

async function submitAction(type) {
  submitting.value = type
  try {
    emit(type, { id: detail.value.leave.id, comment: comment.value })
  } finally {
    submitting.value = ''
  }
}
</script>

<style scoped>
.ml-1 {
  margin-left: 8px;
}
.audit-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.operator {
  font-size: 13px;
  color: #606266;
}
.audit-comment {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}
.operate-btns {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
