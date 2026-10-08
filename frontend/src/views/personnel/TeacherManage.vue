<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="教师姓名 / 工号"
        clearable
        style="width: 220px"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-select v-model="query.collegeId" placeholder="所属学院" clearable style="width: 180px" @change="load(1)">
        <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <div class="flex-1"></div>
      <el-button type="primary" @click="openDialog()">新增教师</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="teacherNo" label="工号" width="110" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }">{{ row.gender === 'F' ? '女' : row.gender === 'M' ? '男' : '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="collegeName" label="所属学院" min-width="140" />
      <el-table-column prop="username" label="登录账号" width="110" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="name in row.roleNames" :key="name" size="small" class="role-tag">{{ name }}</el-tag>
          <span v-if="!row.roleNames?.length" class="gray">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-dropdown trigger="click" @command="(cmd) => handleMore(cmd, row)">
            <el-button size="small">更多<el-icon><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="reset">重置密码</el-dropdown-item>
                <el-dropdown-item command="toggle">{{ row.status === 1 ? '停用账号' : '启用账号' }}</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除教师</el-dropdown-item>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑教师' : '新增教师'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="form.teacherNo" placeholder="登录账号=工号" />
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
            <el-form-item label="所属学院" prop="collegeId">
              <el-select v-model="form.collegeId" placeholder="请选择" style="width: 100%">
                <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分配角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="可多选（班主任/辅导员/院领导等）" style="width: 100%">
                <el-option v-for="r in teacherRoles" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <el-alert type="info" :closable="false" title="新增教师默认密码 123456，可在列表“更多 → 重置密码”中恢复" />
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
import { teacherApi } from '@/api/teacher'
import { collegeApi } from '@/api/college'
import { roleApi } from '@/api/role'

const rows = ref([])
const colleges = ref([])
const teacherRoles = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', collegeId: undefined })
const emptyForm = {
  id: null,
  teacherNo: '',
  name: '',
  gender: 'M',
  phone: '',
  email: '',
  collegeId: null,
  roleIds: []
}
const form = reactive({ ...emptyForm })

const rules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }]
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await teacherApi.page(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  const [cols, roles] = await Promise.all([collegeApi.all(), roleApi.all()])
  colleges.value = cols
  teacherRoles.value = roles.filter((r) => !['student', 'super_admin'].includes(r.roleCode))
}

function openDialog(row) {
  Object.assign(form, emptyForm, row ? {
    id: row.id,
    teacherNo: row.teacherNo,
    name: row.name,
    gender: row.gender || 'M',
    phone: row.phone,
    email: row.email,
    collegeId: row.collegeId,
    roleIds: row.roleIds ? [...row.roleIds] : []
  } : {})
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = {
      teacherNo: form.teacherNo,
      name: form.name,
      gender: form.gender,
      phone: form.phone,
      email: form.email,
      collegeId: form.collegeId,
      roleIds: form.roleIds
    }
    if (form.id) {
      await teacherApi.update(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await teacherApi.save(payload)
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
    await teacherApi.resetPassword(row.id)
    ElMessage.success('密码已重置为 123456')
  } else if (cmd === 'toggle') {
    const target = row.status === 1 ? 0 : 1
    await teacherApi.toggleStatus(row.id, target)
    ElMessage.success(target === 1 ? '账号已启用' : '账号已停用')
    load()
  } else if (cmd === 'delete') {
    await ElMessageBox.confirm(`确认删除教师“${row.name}”？删除将同时移除其登录账号。`, '删除确认', { type: 'warning' })
    await teacherApi.remove(row.id)
    ElMessage.success('删除成功')
    load()
  }
}

onMounted(() => {
  load()
  loadOptions()
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
.role-tag {
  margin-right: 4px;
}
.gray {
  color: #c0c4cc;
}
</style>
