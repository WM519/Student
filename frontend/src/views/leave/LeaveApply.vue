<template>
  <el-card shadow="never" style="max-width: 760px">
    <template #header>发起请假申请</template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="请假类型" prop="leaveType">
        <el-radio-group v-model="form.leaveType">
          <el-radio-button v-for="t in LEAVE_TYPES" :key="t.value" :value="t.value">
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="请假时间" prop="range">
        <el-date-picker
          v-model="form.range"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :disabled-date="(d) => d < dayjs().startOf('day')"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="请假天数">
        <el-tag v-if="days" type="warning" size="large">{{ days }} 天</el-tag>
        <span v-else class="gray">选择日期后自动计算（按自然日，结束日期包含在内）</span>
      </el-form-item>
      <el-alert
        v-if="days"
        class="chain-tip"
        type="info"
        :closable="false"
        :title="`按规则：${chainTip}审批`"
      />
      <el-form-item label="请假事由" prop="reason">
        <el-input
          v-model="form.reason"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请详细说明请假事由"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
        <el-button @click="$router.push('/leave/my')">查看我的请假</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { leaveApi } from '@/api/leave'
import { LEAVE_TYPES } from '@/utils/dict'

const formRef = ref()
const submitting = ref(false)
const form = reactive({
  leaveType: '事假',
  range: [],
  reason: ''
})

const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  range: [{ required: true, message: '请选择请假起止日期', trigger: 'change' }],
  reason: [
    { required: true, message: '请填写请假事由', trigger: 'blur' },
    { min: 3, message: '事由不少于 3 个字', trigger: 'blur' }
  ]
}

const days = computed(() => {
  if (!form.range || form.range.length !== 2) return 0
  return dayjs(form.range[1]).diff(dayjs(form.range[0]), 'day') + 1
})

const chainTip = computed(() => {
  const d = days.value
  if (d <= 3) return '班主任'
  if (d <= 7) return '班主任 → 辅导员'
  return '班主任 → 辅导员 → 院领导'
})

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await leaveApi.apply({
      leaveType: form.leaveType,
      startDate: form.range[0],
      endDate: form.range[1],
      reason: form.reason
    })
    ElMessage.success('请假申请提交成功')
    form.reason = ''
    form.range = []
  } catch {
    // 错误由拦截器提示
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.gray {
  color: #909399;
  font-size: 13px;
}
.chain-tip {
  margin-bottom: 18px;
}
</style>
