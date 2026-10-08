<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-title">
        <el-icon :size="36" color="#409eff"><School /></el-icon>
        <h2>学生请假管理系统</h2>
        <p>Vue3 + Spring Boot + MySQL</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" clearable />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">
        <template #title>
          <div class="tip">演示账号（密码 123456）：</div>
          <div class="tip">管理员 admin / admin123；班主任 head01 / head03；辅导员 coun01；院领导 leader01；学生 stu01</div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useLoginStore } from '@/stores/login'

const route = useRoute()
const router = useRouter()
const loginStore = useLoginStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await loginStore.login(form)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } catch {
    // 错误信息由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3b73 0%, #2d6cdf 60%, #67c23a 130%);
}
.login-card {
  width: 420px;
  padding: 36px 32px 24px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
}
.login-title h2 {
  margin: 10px 0 4px;
}
.login-title p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.login-btn {
  width: 100%;
}
.tip {
  font-size: 12px;
  line-height: 1.7;
}
</style>
