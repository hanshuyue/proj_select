<script setup lang="ts">
import { computed,onMounted,ref } from 'vue'
import { collaborationApi,type Registration,ApiError } from '@/api'
import { currentUser } from '@/composables/auth'
import { pushToast } from '@/composables/toast'
const rows=ref<Registration[]>([]),loading=ref(false),updating=ref<number|null>(null)
const superAdmin=computed(()=>currentUser.value?.roles?.includes('super_admin'))
async function load(){loading.value=true;try{rows.value=await collaborationApi.registrations()}catch(e){pushToast(e instanceof ApiError?e.message:'注册结果加载失败','danger')}finally{loading.value=false}}
async function toggle(row:Registration,value:boolean){updating.value=row.id;try{await collaborationApi.setAdministrator(row.id,value);pushToast(value?'已设置为管理员':'已取消管理员','ok');await load()}catch(e){pushToast(e instanceof ApiError?e.message:'设置失败','danger')}finally{updating.value=null}}
onMounted(load)
</script>
<template><div class="page"><div class="page-head"><div><h1>管理员设置</h1><p>{{superAdmin?'查看全平台账号，并设置或取消管理员身份。':'仅超级管理员可以任免管理员。'}}</p></div></div><el-alert v-if="superAdmin" title="打开开关即设为管理员；关闭开关即恢复为普通用户。超级管理员账号不会出现在此列表。" type="info" :closable="false" style="margin-bottom:16px"/><el-card shadow="never"><el-table v-loading="loading" :data="rows" border><el-table-column prop="realName" label="姓名"/><el-table-column prop="phone" label="手机号"/><el-table-column prop="deptName" label="所属市县"/><el-table-column prop="createTime" label="注册/创建时间" width="180"/><el-table-column label="账户状态" width="100"><template #default="s"><el-tag :type="Number(s.row.status)===1?'success':'info'">{{Number(s.row.status)===1?'正常':'停用'}}</el-tag></template></el-table-column><el-table-column label="当前身份" width="120"><template #default="s"><el-tag :type="s.row.administrator?'warning':'info'">{{s.row.administrator?'管理员':'普通用户'}}</el-tag></template></el-table-column><el-table-column v-if="superAdmin" label="设为管理员" width="170"><template #default="s"><el-switch :model-value="!!s.row.administrator" :loading="updating===s.row.id" active-text="管理员" inactive-text="普通用户" @change="toggle(s.row,!!$event)"/></template></el-table-column></el-table></el-card></div></template>
