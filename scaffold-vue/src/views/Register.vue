<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { collaborationApi, authApi, type DepartmentOption, ApiError } from '@/api'
import { registrationErrors } from '@/utils/registration'

const router = useRouter()
const form = reactive({ realName: '', phone: '', deptId: undefined as number | undefined, password: '', confirm: '', captcha: '' })
const departments = ref<DepartmentOption[]>([])
const loading = ref(false), message = ref(''), error = ref('')
const captchaCode = ref(''), captchaUuid = ref(''), captchaLoading = ref(false)
const captchaError = ref(''), departmentError = ref('')
function failureMessage(e: unknown) {
  if (e instanceof ApiError) return e.status === 0 ? '网络连接失败，请检查网络后重试' : e.message
  return '服务器响应异常，请稍后重试；如持续出现，请联系管理员'
}
async function refreshCaptcha() {
  if (captchaLoading.value) return
  captchaLoading.value = true
  captchaError.value = ''; captchaUuid.value = ''; captchaCode.value = ''; form.captcha = ''
  try {
    const data = await authApi.captcha()
    captchaCode.value = data.code; captchaUuid.value = data.uuid
  } catch (e) {
    captchaError.value = `验证码加载失败：${failureMessage(e)}，请点击验证码区域重试`
  } finally { captchaLoading.value = false }
}
async function loadDepartments() {
  departmentError.value = ''
  try {
    departments.value = await collaborationApi.departments()
    if (!departments.value.length) departmentError.value = '暂无可选市县，请联系管理员配置'
  } catch (e) { departmentError.value = `所属市县加载失败：${failureMessage(e)}` }
}
onMounted(() => { void loadDepartments(); void refreshCaptcha() })
async function submit() {
  if (loading.value || message.value) return
  error.value = ''; message.value = ''
  const errors = registrationErrors(form)
  if (!captchaUuid.value) errors.push('验证码尚未加载，请点击验证码区域重试')
  if (errors.length) { error.value = errors.join('；'); return }
  loading.value = true
  try {
    await collaborationApi.register({ realName: form.realName.trim(), phone: form.phone.trim(), deptId: form.deptId!, password: form.password, captcha: form.captcha.trim(), captchaUuid: captchaUuid.value })
    message.value = '注册成功，即将使用手机号登录'
    setTimeout(() => router.push({ path: '/login', query: { username: form.phone.trim() } }), 1200)
  } catch (e) {
    error.value = failureMessage(e)
    await refreshCaptcha()
  } finally { loading.value = false }
}
</script>
<template><div class="login-wrap"><div class="login-brand"><div class="login-brand-mid"><h1>用户注册</h1><p>请选择所属市县，注册成功后手机号即为登录账号。</p></div></div><div class="login-form-side"><div class="login-panel"><div class="login-card-head"><h2>注册账户</h2><p>注册后默认为普通员工，可直接登录并使用基础PPT制作与提交功能。</p></div><el-form label-position="top" @submit.prevent="submit"><el-form-item label="真实姓名"><el-input v-model="form.realName" maxlength="64" /></el-form-item><el-form-item label="手机号（登录账号）"><el-input v-model="form.phone" maxlength="11" /></el-form-item><el-form-item label="所属市县"><el-select v-model="form.deptId" style="width:100%" placeholder="请选择所属市县" filterable><el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" /></el-select></el-form-item><el-form-item label="密码（6–64 位，支持纯数字）"><el-input v-model="form.password" type="password" show-password placeholder="请输入至少 6 位密码" /></el-form-item><el-form-item label="确认密码"><el-input v-model="form.confirm" type="password" show-password /></el-form-item><el-form-item label="图形验证码"><div style="display:flex;gap:10px;width:100%"><el-input v-model="form.captcha" maxlength="4" @keyup.enter="submit"/><button type="button" :disabled="captchaLoading || loading" class="captcha-box" title="点击刷新" @click="refreshCaptcha"><b style="letter-spacing:5px">{{ captchaLoading ? '加载中' : (captchaCode || '重试') }}</b></button></div></el-form-item><el-alert v-if="departmentError" :title="departmentError" type="error" :closable="false"><el-button link @click="loadDepartments">重新加载市县</el-button></el-alert><el-alert v-if="captchaError" :title="captchaError" type="error" :closable="false"/><el-alert v-if="error" :title="error" type="error" :closable="false"/><el-alert v-if="message" :title="message" type="success" :closable="false"/><el-button type="primary" size="large" class="login-submit" :loading="loading" @click="submit">立即注册</el-button><div style="text-align:center;margin-top:16px"><router-link to="/login" class="login-link">返回登录</router-link></div></el-form></div></div></div></template>
