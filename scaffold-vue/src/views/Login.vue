<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import Icon from '@/components/Icon.vue'
import { ApiError, authApi } from '@/api'
import { dynamicRoutes, login as doLogin } from '@/composables/auth'
import { injectRoutes } from '@/router'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  captcha: '',
})
const remember = ref(true)
const code = ref('')
const captchaUuid = ref('')
const loading = ref(false)
const serverError = ref('')

const lines = Array.from({ length: 4 }).map(() => ({
  x1: Math.random() * 100,
  y1: Math.random() * 40,
  x2: Math.random() * 100,
  y2: Math.random() * 40,
}))
const charColors = ['#0066cc', '#1d1d1f', '#1f8a4c', '#b06d00']
const features = [
  { icon: 'shield', text: '权限控制' },
  { icon: 'dept', text: '组织管理' },
  { icon: 'command', text: '运维中心' },
  { icon: 'flow', text: '流程协同' },
]

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
  ],
}

async function refresh() {
  form.captcha = ''
  try {
    const res = await authApi.captcha()
    code.value = res.code
    captchaUuid.value = res.uuid
  } catch {
    code.value = ''
    serverError.value = '验证码加载失败，请点击验证码区域重试'
    captchaUuid.value = ''
  }
}

async function submit() {
  if (loading.value) return
  if (!captchaUuid.value) { await refresh(); return }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  serverError.value = ''
  loading.value = true

  try {
    const user = await doLogin({
      username: form.username.trim(),
      password: form.password,
      captcha: form.captcha.trim(),
      captchaUuid: captchaUuid.value,
    }, remember.value)

    if (user.mustChangePassword) {
      await router.replace({ name: 'changePasswordRequired' })
      return
    }
    injectRoutes(dynamicRoutes.value)

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    if (redirect) {
      await router.replace(redirect)
      return
    }

    const firstRoute = dynamicRoutes.value[0]
    const firstPath = firstRoute?.path ? `/${String(firstRoute.path).replace(/^\//, '')}` : '/'
    await router.replace({ path: firstPath })
  } catch (error) {
    serverError.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
    await refresh()
  } finally {
    loading.value = false
  }
}

onMounted(refresh)
onMounted(() => {
  if (typeof route.query.username === 'string') form.username = route.query.username
})
</script>

<template>
  <div class="login-wrap">
    <div class="login-brand">
      <div class="login-brand-top">
        <span class="brand-mark" style="width: 40px; height: 40px">
          <Icon name="command" :size="24" />
        </span>
        <div>
          <div style="font-size: 17px; font-weight: 600; color: #fff; letter-spacing: -0.02em">政企项目材料平台</div>
          <div style="font-size: 12.5px; color: var(--side-ink-muted)">标准化与智能生成</div>
        </div>
      </div>

      <div class="login-brand-mid">
        <h1>欢迎使用政企项目材料标准化与智能生成平台</h1>
        <p>统一管理项目立项、甄选结果、模板和收支测算，规范材料编制流程并智能生成项目文档。</p>
        <div class="login-feat">
          <div v-for="item in features" :key="item.text" class="login-feat-item">
            <span><Icon :name="item.icon" :size="16" /></span>
            {{ item.text }}
          </div>
        </div>
      </div>

      <div class="login-brand-foot">© 2026 政企项目材料标准化与智能生成平台 · 版本 V1.0</div>
      <div class="login-orb" />
    </div>

    <div class="login-form-side">
      <div class="login-panel">
        <div class="login-card-head">
          <h2>账号登录</h2>
          <p>登录后继续访问系统业务页面。</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          @keyup.enter="submit"
        >
          <el-form-item label="手机号" prop="username">
            <el-input v-model="form.username" placeholder="请输入中国大陆手机号" maxlength="11" clearable>
              <template #prefix>
                <Icon name="user" :size="16" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入登录密码" show-password>
              <template #prefix>
                <Icon name="lock" :size="16" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="验证码" prop="captcha">
            <div class="login-captcha">
              <el-input v-model="form.captcha" placeholder="请输入验证码" class="login-captcha-input" clearable />
              <button type="button" class="captcha-box" title="点击刷新验证码" @click="refresh">
                <svg width="100" height="40" viewBox="0 0 100 40">
                  <rect width="100" height="40" fill="#f5f5f7" />
                  <line
                    v-for="(line, i) in lines"
                    :key="i"
                    :x1="line.x1"
                    :y1="line.y1"
                    :x2="line.x2"
                    :y2="line.y2"
                    stroke="#d2d2d7"
                    stroke-width="1"
                  />
                  <text
                    v-for="(char, i) in code.split('')"
                    :key="'c' + i"
                    :x="14 + i * 21"
                    :y="28"
                    font-size="22"
                    font-weight="700"
                    :fill="charColors[i % 4]"
                    :transform="`rotate(${(i % 2 ? 1 : -1) * (6 + i * 2)} ${14 + i * 21} 22)`"
                    font-family="SF Pro Display, sans-serif"
                  >{{ char }}</text>
                </svg>
              </button>
            </div>
          </el-form-item>

          <div class="login-row">
            <el-checkbox v-model="remember">记住登录状态</el-checkbox>
            <button type="button" class="login-link">联系管理员重置密码</button>
          </div>

          <div style="text-align:center;margin-top:-4px;margin-bottom:14px">
            <router-link to="/register" class="login-link">注册账户</router-link>
          </div>

          <el-button type="primary" size="large" class="login-submit" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>

        <div
          v-if="serverError"
          class="login-demo login-demo-error"
        >
          <Icon name="lock" :size="13" />
          {{ serverError }}
        </div>

      </div>
    </div>
  </div>
</template>
