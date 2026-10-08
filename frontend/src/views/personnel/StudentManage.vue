<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="学生姓名 / 学号"
        clearable
        style="width: 200px"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-select v-model="collegeFilter" placeholder="学院" clearable style="width: 160px" @change="onCollegeChange">
        <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
      </el-select>
      <el-select v-model="query.classId" placeholder="班级" clearable style="width: 160px" @change="load(1)">
        <el-option v-for="cl in classes" :key="cl.id" :label="cl.className" :value="cl.id" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <div class="flex-1"></div>
      <el-button type="primary" @click="openDialog()">新增学生</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.gender === 'F' ? '女' : row.gender === 'M' ? '男' : '-' }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" width="130" />
      <el-table-column prop="collegeName" label="学院" min-width="140" />
      <el-table-column prop="enrollYear" label="入学年份" width="100" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="username" label="登录账号" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleMore(cmd, row)">
            <el-button size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                <el-dropdown-item command="toggle">{{ row.status === 1 ? '停用账号' : '启用账号' }}</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除学生</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学生' : '新增学生'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="form.studentNo" placeholder="登录账号=学号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio value="M">男</el-radio>
                <el-radio value="F">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属班级" prop="classId">
              <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
                <el-option v-for="cl in allClasses" :key="cl.id" :label="cl.className" :value="cl.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学年份">
              <el-input-number v-model="form.enrollYear" :min="2000" :max="2100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-alert type="info" :closable="false" title="新增学生默认密码 123456，并自动分配“学生”角色" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { studentApi } from '@/api/student'
import { classApi } from '@/api/class'
import { collegeApi } from '@/api/college'

const rows = ref([])
const colleges = ref([])
const classes = ref([])
const allClasses = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const collegeFilter = ref(undefined)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', classId: undefined })
const emptyForm = {
  id: null,
  studentNo: '',
  name: '',
  gender: 'M',
  phone: '',
  email: '',
  classId: null,
  enrollYear: new Date().getFullYear()
}
const form = reactive({ ...emptyForm })

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择所属班级', trigger: 'change' }]
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await studentApi.page(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  colleges.value = await collegeApi.all()
  allClasses.value = await classApi.all()
}

async function onCollegeChange() {
  query.classId = undefined
  classes.value = collegeFilter.value
    ? allClasses.value.filter((c) => c.collegeId === collegeFilter.value)
    : allClasses.value
  load(1)
}

function openDialog(row) {
  Object.assign(form, emptyForm, row ? {
    id: row.id,
    studentNo: row.studentNo,
    name: row.name,
    gender: row.gender || 'M',
    phone: row.phone,
    email: row.email,
    classId: row.classId,
    enrollYear: row.enrollYear || new Date().getFullYear()
  } : {})
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      studentNo: form.studentNo,
      name: form.name,
      gender: form.gender,
      phone: form.phone,
      email: form.email,
      classId: form.classId,
      enrollYear: form.enrollYear
    }
    if (form.id) {
      await studentApi.update(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await studentApi.save(payload)
      ElMessage.success('新增成功，初始密码 123456')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleMore(cmd, row) {
  if (cmd === 'reset') {
    await ElMessageBox.confirm(`将 ${row.name} 的密码重置为 123456？`, '重置密码', { type: 'warning' })
    await studentApi.resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } else if (cmd === 'toggle') {
    const target = row.status === 1 ? 0 : 1
    await studentApi.toggleStatus(row.id, target)
    ElMessage.success(target === 1 ? '账号已启用' : '账号已停用')
    load()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm(`确认删除学生“${row.name}”？`, '删除确认', { type: 'warning' })
    await studentApi.remove(row.id)
    ElMessage.success('删除成功')
    load()
  }
}

onMounted(async () => {
  await loadOptions()
  classes.value = allClasses.value
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.flex-1 {
  flex: 1;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
