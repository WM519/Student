// 请假状态字典
export const LEAVE_STATUS = {
  PENDING_HEAD: '待班主任审批',
  PENDING_COUNSELOR: '待辅导员审批',
  PENDING_LEADER: '待院领导审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已销假',
  WITHDRAWN: '已撤销'
}

export const LEAVE_STATUS_TAG = {
  PENDING_HEAD: 'warning',
  PENDING_COUNSELOR: 'warning',
  PENDING_LEADER: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info',
  WITHDRAWN: 'info'
}

// 请假类型
export const LEAVE_TYPES = [
  { label: '事假', value: '事假' },
  { label: '病假', value: '病假' },
  { label: '其他', value: '其他' }
]

// 审批环节
export const LEVEL_MAP = {
  0: '提交',
  1: '班主任',
  2: '辅导员',
  3: '院领导',
  4: '销假'
}

// 审批流水动作
export const ACTION_MAP = {
  SUBMIT: '提交申请',
  APPROVE: '审批通过',
  REJECT: '审批驳回',
  WITHDRAW: '学生撤销',
  CANCEL: '学生销假'
}

export const ACTION_TAG = {
  SUBMIT: 'primary',
  APPROVE: 'success',
  REJECT: 'danger',
  WITHDRAW: 'info',
  CANCEL: 'warning'
}

export function statusText(status) {
  return LEAVE_STATUS[status] || status
}
