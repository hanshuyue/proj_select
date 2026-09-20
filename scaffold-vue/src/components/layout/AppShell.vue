<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from './Sidebar.vue'
import Topbar from './Topbar.vue'
import { logout as doLogout } from '@/composables/auth'

const router = useRouter()
const currentRoute = useRoute()
const collapsed = ref(false)

const initiationPages = {
  list: defineAsyncComponent(() => import('@/views/initiation/index.vue')),
  form: defineAsyncComponent(() => import('@/views/initiation/form.vue')),
  template: defineAsyncComponent(() => import('@/views/initiation/template.vue')),
}

const fallbackComponent = computed(() => {
  if (currentRoute.path === '/initiation') return initiationPages.list
  if (currentRoute.path === '/initiation/form') return initiationPages.form
  if (currentRoute.path === '/initiation/template') return initiationPages.template
  return null
})

async function onLogout() {
  await doLogout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <Sidebar :collapsed="collapsed" />
    <div class="app-main">
      <Topbar :collapsed="collapsed" @toggle="collapsed = !collapsed" @logout="onLogout" />
      <div class="app-content">
        <router-view v-slot="{ Component, route }">
          <transition name="page" mode="out-in">
            <component :is="Component || fallbackComponent" :key="route.fullPath" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-enter-active { transition: opacity .26s ease; }
.page-enter-from { opacity: 0; }
</style>
