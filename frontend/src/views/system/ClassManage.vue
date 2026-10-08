<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="班级名称 / 编码"
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
      <el-button type="primary" @click="openDialog()">新增班级</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="className" label="班级名称" min-width="140" />
      <el-table-column prop="classCode" label="班级编码" width="130" />
      <el-table-column prop="collegeName" label="所属学院" min-width="150" />
      <el-table-column prop="headTeacherName" label="班主任" width="110" />
      <el-table-column prop="counselorTeacherName" label="辅导员" width="110" />
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑班级' : '新增班级'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="班级名称" prop="className">
          <el-input v-model="form.className" placeholder="如：计科2401" />
        </el-form-item>
        <el-form-item label="班级编码" prop="classCode">
          <el-input v-model="form.classCode" placeholder="如：C2401" />
        </el-form-item>
        <el-form-item label="所属学院" prop="collegeId">
          <el-select v-model="form.collegeId" placeholder="请选择学院" style="width: 100%" @change="onCollegeChange">
            <el-option v-for="c in colleges" :key="c.id" :label="c.collegeName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班主任" prop="headTeacherId">
          <el-select
            v-model="form.headTeacherId"
            placeholder="请选择班主任（拥有班主任角色）"
            filterable
            style="width: 100%"
            :disabled="!form.collegeId"
          >
            <el-option v-for="t in headOptions" :key="t.id" :label="`${t.name}（${t.teacherNo}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="辅导员" prop="counselorTeacherId">
          <el-select
            v-model="form.counselorTeacherId"
            placeholder="请选择辅导员（拥有辅导员角色）"
            filterable
            style="width: 100%"
            :disabled="!form.collegeId"
          >
            <el-option v-for="t in counselorOptions" :key="t.id" :label="`${t.name}（${t.teacherNo}）`" :value="t.id" />
          </el-select>
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
import { classApi } from '@/api/class'
import { collegeApi } from '@/api/college'
import { teacherApi } from '@/api/teacher'

const rows = ref([])
const colleges = ref([])
const headOptions = ref([])
const counselorOptions = ref([])
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', collegeId: undefined })
const emptyForm = {
  id: null,
  className: '',
  classCode: '',
  collegeId: null,
  headTeacherId: null,
  counselorTeacherId: null
}
const form = reactive({ ...emptyForm })

const rules = {
  className: [{ required: true, message: '请输入班级名称', trigger: 'blur' }],
  classCode: [{ required: true, message: '请输入班级编码', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择所属学院', trigger: 'change' }],
  headTeacherId: [{ required: true, message: '请选择班主任', trigger: 'change' }],
  counselorTeacherId: [{ required: true, message: '请选择辅导员', trigger: 'change' }]
}

async function load(pageNum) {
  if (pageNum) query.pageNum = pageNum
  loading.value = true
  try {
    const data = await classApi.page(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadColleges() {
  colleges.value = await collegeApi.all()
}

async function loadTeacherOptions(collegeId) {
  headOptions.value = await teacherApi.options({ roleCode: 'head_teacher', collegeId })
  counselorOptions.value = await teacherApi.options({ roleCode: 'counselor', collegeId })
}

function onCollegeChange() {
  form.headTeacherId = null
  form.counselorTeacherId = null
  loadTeacherOptions(form.collegeId)
}

function openDialog(row) {
  Object.assign(form, emptyForm, row || {})
  dialogVisible.value = true
  formRef.value?.clearValidate()
  loadTeacherOptions(form.collegeId || undefined)
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await classApi.update(form.id, form)
      ElMessage.success('修改成功')
    } else {
      await classApi.save(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除班级“${row.className}”吗？`, '删除确认', { type: 'warning' })
  await classApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadColleges()
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
