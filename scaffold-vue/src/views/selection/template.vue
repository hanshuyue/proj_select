<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessageBox, type UploadFile } from 'element-plus'
import { selectionApi, type SelectionTemplate } from '@/api'
import { pushToast } from '@/composables/toast'
import { downloadBlob } from '@/utils/download'

const rows = ref<SelectionTemplate[]>([])
const file = ref<File>()
const name = ref('')
const uploading = ref(false)
const dialog = ref(false)
async function load() { rows.value = await selectionApi.templates.list() }
function choose(upload: UploadFile) { file.value = upload.raw; if (!name.value) name.value = upload.name.replace(/\.pptx$/i, '') }
async function upload() {
  if (!file.value) return pushToast('请选择 .pptx 文件', 'danger')
  uploading.value = true
  try { await selectionApi.templates.upload(name.value, file.value); pushToast('模板上传成功', 'ok'); dialog.value=false; file.value=undefined;name.value='';await load() }
  finally { uploading.value=false }
}
async function setDefault(row: SelectionTemplate) { await selectionApi.templates.setDefault(row.id); pushToast('默认模板已更新','ok');await load() }
async function download(row: SelectionTemplate) { downloadBlob(await selectionApi.templates.download(row.id), row.originalFilename) }
async function remove(row: SelectionTemplate) {
  await ElMessageBox.confirm(`确认删除模板“${row.templateName}”吗？`, '删除确认', {type:'warning'})
  await selectionApi.templates.remove(row.id); await load()
}
onMounted(load)
</script>
<template>
  <div>
    <div class="page-head"><div class="page-main"><div><h1 class="page-title">PPT 模板管理</h1><div class="page-desc">上传固定 PPTX 模板，系统按占位符填充甄选数据。</div></div><el-button type="primary" @click="dialog=true">上传模板</el-button></div></div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom:16px">
      <template #title><span v-pre>文本占位符采用 {{projectName}}；动态表格行采用 {{#bidders}}，并在同行单元格放置 bidderName、serviceExTax、totalScore、ranking 等字段。</span></template>
    </el-alert>
    <el-card shadow="never">
      <el-table :data="rows" border>
        <el-table-column prop="templateName" label="模板名称" min-width="180" />
        <el-table-column prop="originalFilename" label="原文件名" min-width="220" />
        <el-table-column label="文件大小" width="110"><template #default="{row}">{{ (row.fileSize/1024/1024).toFixed(2) }} MB</template></el-table-column>
        <el-table-column label="已识别占位符" min-width="220"><template #default="{row}">{{ row.placeholderKeys || '兼容固定模板（未使用标签）' }}</template></el-table-column>
        <el-table-column label="默认" width="80"><template #default="{row}"><el-tag v-if="row.defaultFlag" type="success">默认</el-tag></template></el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="160" />
        <el-table-column label="操作" width="220"><template #default="{row}"><el-button link @click="download(row)">下载</el-button><el-button link type="primary" :disabled="!!row.defaultFlag" @click="setDefault(row)">设为默认</el-button><el-button link type="danger" :disabled="!!row.defaultFlag" @click="remove(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialog" title="上传 PPT 模板" width="520px">
      <el-form label-width="90px">
        <el-form-item label="模板名称"><el-input v-model="name" placeholder="例如：甄选结果标准模板" /></el-form-item>
        <el-form-item label="PPTX 文件"><el-upload :auto-upload="false" :limit="1" accept=".pptx" :on-change="choose"><el-button>选择文件</el-button></el-upload></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="uploading" @click="upload">上传</el-button></template>
    </el-dialog>
  </div>
</template>
