<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="学院名称 / 编码"
        clearable
        style="width: 240px"
        @keyup.enter="load(1)"
        @clear="load(1)"
      />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <div class="flex-1"></div>
      <el-button type="primary" @click="openDialog()">新增学院</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="collegeName" label="学院名称" min-width="160" />
      <el-table-column prop="collegeCode" label="学院编码" width="140" />
      <el-table-column prop="leaderName" label="院领导" width="120">
        <template #default="{ row }">{{ row.leaderName || '未设置' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑学院' : '新增学院'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="学院名称" prop="collegeName">
          <el-input v-model="form.collegeName" placeholder="如：计算机学院" />
        </el-form-item>
        <el-form-item label="学院编码" prop="collegeCode">
          <el-input v-model="form.collegeCode" placeholder="如：JSJ001" />
        </el-form-item>
        <el-form-item label="院领导">
          <el-select
            v-model="form.leaderTeacherId"
            placeholder="请选择院领导（教师需拥有院领导角色）"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="t in leaderOptions"
              :key="t.id"
              :label="`${t.name}（${t.teacherNo}）`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
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
import { collegeApi } from '@/api/college'
import { teacherApi } from '@/api/teacher'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const leaderOptions = ref([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const emptyForm = { id: null, collegeName: '', collegeCode: '', leaderTeacherId: null, description: '' }
const form = reactive({ ...emptyForm })

const rules = {
  collegeName: [{ required: true, message: '请输入学院名称', trigger: 'blur' }],
  collegeCode: [{ required: true, message: '请输入学院编码', trigger: 'blur' }]
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await collegeApi.page(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadLeaders() {
  leaderOptions.value = await teacherApi.options({ roleCode: 'college_leader' })
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
      await collegeApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await collegeApi.save(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除学院“${row.collegeName}”吗？`, '删除确认', { type: 'warning' })
  await collegeApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadLeaders()
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
