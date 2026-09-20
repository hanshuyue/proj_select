<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox, type UploadFile } from 'element-plus'
import { initiationApi, type InitiationTemplate } from '@/api'
import { downloadBlob } from '@/utils/download'
import { pushToast } from '@/composables/toast'

const rows = ref<InitiationTemplate[]>([])
const templateName = ref('')
const uploadFile = ref<File>()
const uploading = ref(false)
const dialog = ref(false)
const isDefault = (row: InitiationTemplate) => row.defaultFlag === true || Number(row.defaultFlag) === 1
const load = async () => { rows.value = await initiationApi.templates.list() }
function chooseFile(file: UploadFile) {
  uploadFile.value = file.raw
  if (uploadFile.value && !templateName.value.trim()) templateName.value = uploadFile.value.name.replace(/\.pptx$/i, '')
}
async function upload() {
  const file = uploadFile.value
  if (!file || !/\.pptx$/i.test(file.name)) return pushToast('请选择 .pptx 模板文件', 'danger')
  uploading.value = true
  try {
    await initiationApi.templates.upload(templateName.value.trim(), file)
    uploadFile.value = undefined
    templateName.value = ''
    dialog.value = false
    await load()
    pushToast('模板上传成功', 'ok')
  } finally { uploading.value = false }
}
async function activate(row: InitiationTemplate) {
  await initiationApi.templates.setDefault(row.id)
  await load()
  pushToast('默认模板已更新', 'ok')
}
async function download(row: InitiationTemplate) {
  downloadBlob(await initiationApi.templates.download(row.id), row.originalFilename)
}
async function remove(row: InitiationTemplate) {
  await ElMessageBox.confirm(`确认删除模板“${row.templateName}”吗？`, '删除确认', { type: 'warning' })
  await initiationApi.templates.remove(row.id)
  await load()
  pushToast('模板已删除', 'ok')
}
onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div class="page-main">
        <div><h1 class="page-title">PPT 模板管理</h1><div class="page-desc">上传固定 PPTX 模板，系统按项目立项字段、收支明细和附件生成项目立项 PPT。</div></div>
        <el-button type="primary" @click="dialog=true">上传模板</el-button>
      </div>
    </div>
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom:16px">
      <template #title>请基于项目立项标准模板修改，保留页序、章节标题、表格行列和嵌入的收支明细工作簿；可调整配色、字体和标识。切换默认模板只影响后续导出。</template>
    </el-alert>
    <el-card shadow="never">
      <el-table :data="rows" border>
        <el-table-column prop="templateName" label="模板名称" min-width="220" />
        <el-table-column prop="originalFilename" label="文件名" min-width="260" />
        <el-table-column label="大小" width="120"><template #default="{ row }">{{ (row.fileSize / 1024 / 1024).toFixed(2) }} MB</template></el-table-column>
        <el-table-column label="默认" width="90"><template #default="{ row }"><el-tag v-if="isDefault(row)" type="success">默认</el-tag><el-tag v-else type="info">备用</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="220"><template #default="{ row }"><el-button link @click="download(row)">下载</el-button><el-button link type="primary" :disabled="isDefault(row)" @click="activate(row)">设为默认</el-button><el-button link type="danger" :disabled="isDefault(row)" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="上传 PPT 模板" width="520px">
      <el-form label-width="90px">
        <el-form-item label="模板名称"><el-input v-model="templateName" maxlength="100" placeholder="例如：项目立项标准模板" /></el-form-item>
        <el-form-item label="PPTX 文件"><el-upload :auto-upload="false" :limit="1" accept=".pptx" :on-change="chooseFile"><el-button>选择文件</el-button></el-upload></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="uploading" @click="upload">上传</el-button></template>
    </el-dialog>
  </div>
</template>
