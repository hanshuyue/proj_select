<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { currentUser, logout } from '@/composables/auth'

const router = useRouter()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (!form.oldPassword || form.newPassword.length < 8) {
    error.value = '请输入当前密码，新密码至少 8 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  loading.value = true
  try {
    await authApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    if (currentUser.value) currentUser.value.mustChangePassword = false
    await logout()
    await router.replace({ name: 'login' })
  } catch (e) {
    error.value = e instanceof Error ? e.message : '密码修改失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page" style="max-width:560px;margin:60px auto">
    <el-card shadow="never">
      <template #header><div style="font-weight:600">首次登录，请修改初始密码</div></template>
      <el-alert title="为保障账号安全，修改成功后请使用新密码重新登录。完成前无法访问其他功能。" type="warning" :closable="false" style="margin-bottom:20px" />
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="当前密码"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="form.confirmPassword" type="password" show-password @keyup.enter="submit" /></el-form-item>
        <el-alert v-if="error" :title="error" type="error" :closable="false" style="margin-bottom:16px" />
        <el-button type="primary" :loading="loading" style="width:100%" @click="submit">修改密码并重新登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>
