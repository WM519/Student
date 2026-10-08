<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span>菜单采用“目录 + 菜单”两级结构，勾选角色后按角色动态生成侧边栏</span>
      <div class="flex-1"></div>
      <el-button type="primary" @click="openDialog()">新增根目录</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="tree" v-loading="loading" border row-key="id" default-expand-all>
      <el-table-column prop="menuName" label="菜单名称" min-width="180">
        <template #default="{ row }">
          <el-icon v-if="row.icon" class="menu-icon"><component :is="row.icon" /></el-icon>
          {{ row.menuName }}
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.menuType === 'DIR' ? 'primary' : 'success'" size="small">
            {{ row.menuType === 'DIR' ? '目录' : '菜单' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="path" label="路由路径" width="170" />
      <el-table-column prop="component" label="组件路径" min-width="190" />
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column label="显示" width="80">
        <template #default="{ row }">{{ row.visible === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="row.menuType === 'DIR'" size="small" type="primary" @click="openDialog(null, row)">
            新增子菜单
          </el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑菜单' : '新增菜单'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单" prop="parentId">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option :value="0" label="作为根目录" />
            <el-option
              v-for="m in allMenus"
              :key="m.id"
              :value="m.id"
              :label="`${m.menuName}${m.menuType === 'MENU' ? '（菜单）' : '（目录）'}`"
              :disabled="m.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio value="DIR">目录（分组）</el-radio>
            <el-radio value="MENU">菜单（页面）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.menuType === 'MENU'" label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="如 /system/college" />
        </el-form-item>
        <el-form-item v-if="form.menuType === 'MENU'" label="组件路径" prop="component">
          <el-input v-model="form.component" placeholder="相对 src/views，如 system/CollegeManage" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Setting" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="是否显示">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
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
import { menuApi } from '@/api/menu'

const tree = ref([])
const allMenus = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const emptyForm = {
  id: null,
  parentId: 0,
  menuName: '',
  menuType: 'MENU',
  path: '',
  component: '',
  icon: '',
  sortOrder: 1,
  visible: 1
}
const form = reactive({ ...emptyForm })

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级菜单', trigger: 'change' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  component: [{ required: true, message: '请输入组件路径', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    allMenus.value = await menuApi.list()
    tree.value = await menuApi.tree()
  } finally {
    loading.value = false
  }
}

function openDialog(row, parent) {
  Object.assign(form, emptyForm)
  if (row) {
    Object.assign(form, row)
  } else if (parent) {
    form.parentId = parent.id
    form.menuType = 'MENU'
  }
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = { ...form }
    delete payload.children
    if (form.id) {
      await menuApi.update(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await menuApi.save(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除菜单“${row.menuName}”吗？`, '删除确认', { type: 'warning' })
  await menuApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.flex-1 {
  flex: 1;
}
.menu-icon {
  margin-right: 6px;
  vertical-align: -2px;
}
</style>
