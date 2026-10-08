<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="角色名称 / 编码"
        clearable
        style="width: 240px"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <div class="flex-1"></div>
      <el-button type="primary" @click="openDialog()">新增角色</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="roleName" label="角色名称" width="140" />
      <el-table-column prop="roleCode" label="角色编码" width="160">
        <template #default="{ row }"><el-tag size="small">{{ row.roleCode }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="openAssign(row)">分配菜单</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
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

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="英文编码，如 head_teacher" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="assignVisible" :title="`分配菜单：${currentRole?.roleName || ''}`" width="520px">
      <el-alert type="info" :closable="false" title="勾选后，拥有该角色的用户登录时只显示这些菜单" class="tip" />
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
      />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { roleApi } from '@/api/role'
import { menuApi } from '@/api/menu'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const assigning = ref(false)
const dialogVisible = ref(false)
const assignVisible = ref(false)
const formRef = ref()
const menuTreeRef = ref()
const menuTree = ref([])
const currentRole = ref(null)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const emptyForm = { id: null, roleName: '', roleCode: '', description: '', status: 1 }
const form = reactive({ ...emptyForm })

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_]*$/, message: '小写字母/数字/下划线，字母开头', trigger: 'blur' }
  ]
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await roleApi.page(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  Object.assign(form, emptyForm, row || {})
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await roleApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await roleApi.save(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除角色“${row.roleName}”吗？`, '删除确认', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

async function openAssign(row) {
  currentRole.value = row
  assignVisible.value = true
  if (!menuTree.value.length) {
    menuTree.value = await menuApi.tree()
  }
  const ids = await roleApi.menuIds(row.id)
  await new Promise((r) => setTimeout(r, 0))
  menuTreeRef.value?.setCheckedKeys(ids)
}

async function submitAssign() {
  assigning.value = true
  try {
    const checked = menuTreeRef.value.getCheckedKeys()
    const half = menuTreeRef.value.getHalfCheckedKeys()
    await roleApi.assignMenus(currentRole.value.id, [...checked, ...half])
    ElMessage.success('菜单分配成功')
    assignVisible.value = false
  } finally {
    assigning.value = false
  }
}

onMounted(load)
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
.tip {
  margin-bottom: 12px;
}
</style>
