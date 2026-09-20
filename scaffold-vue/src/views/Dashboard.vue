<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import { portalApi } from '@/api'
import { currentUser } from '@/composables/auth'
import { pushToast } from '@/composables/toast'
import { downloadCsv } from '@/utils/csv'
import { avatarText, avatarColor } from '@/utils/avatar'

const router = useRouter()

const trendRange = ref('24h')
const stats = ref<Record<string, any>[]>([])
const trend = ref<number[]>(Array.from({ length: 24 }, () => 0))
const todos = ref<Record<string, any>[]>([])
const activities = ref<Record<string, any>[]>([])
const trendTotal = ref(0)
const successRate = ref(100)
const roleDist = ref<{ label: string; value: number }[]>([])

const ROLE_COLORS = ['#0066cc', '#1f8a4c', '#b06d00', '#7a5ae0', '#c4321f', '#0a84b0']

const roleSeg = computed(() => roleDist.value.map((r, i) => ({
  label: r.label || '未分配',
  value: Number(r.value) || 0,
  color: ROLE_COLORS[i % ROLE_COLORS.length],
})))

const statItems = computed(() => stats.value)
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '用户')

const todoItems = computed(() => todos.value.map(t => ({
  id: t.id,
  title: t.bizTitle,
  from: t.starter,
  type: t.procName,
  time: String(t.due ?? '').slice(5),
  level: t.priority === 'high' ? 'danger' : t.priority === 'mid' ? 'warn' : 'info',
})))

onMounted(loadSummary)
watch(trendRange, loadSummary)

async function loadSummary() {
  try {
    const data = await portalApi.dashboard.summary({ range: trendRange.value })
    stats.value = data.stats
    trend.value = data.trend.length ? data.trend : trend.value
    todos.value = data.todos
    activities.value = data.activities
    trendTotal.value = data.trendTotal ?? 0
    successRate.value = data.successRate ?? 100
    roleDist.value = data.roleDist ?? []
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '工作台数据加载失败', 'danger')
  }
}

function fmt(n: number) {
  return n >= 10000 ? `${(n / 10000).toFixed(1)} 万` : n.toLocaleString()
}

function exportOverview() {
  const rows = [
    ...statItems.value.map(item => ({ section: '指标', name: item.label, value: item.value, desc: item.sub })),
    ...todoItems.value.map(item => ({ section: '待办', name: item.title, value: item.type, desc: `${item.from} / ${item.time}` })),
    ...activities.value.map(item => ({ section: '动态', name: item.who, value: item.action, desc: `${item.target} / ${item.time}` })),
  ]
  downloadCsv('dashboard-overview.csv', [
    { title: '分类', key: 'section' },
    { title: '名称', key: 'name' },
    { title: '数值/类型', key: 'value' },
    { title: '说明', key: 'desc' },
  ], rows)
  pushToast('工作台概览已导出', 'ok')
}
</script>

<template>
  <div>
    <div class="page-head">
      <div class="page-main">
        <div>
          <h1 class="page-title">工作台</h1>
          <div class="page-desc">{{ `欢迎回来，${displayName}。这里汇总了系统关键指标和你的待办事项。` }}</div>
        </div>
        <div class="page-actions">
          <el-button plain @click="exportOverview"><Icon name="download" :size="14" />导出概览</el-button>
          <el-button type="primary" @click="router.push({ name: 'user' })"><Icon name="plus" :size="14" />新增用户</el-button>
        </div>
      </div>
    </div>

    <div class="stat-grid">
      <el-card v-for="s in statItems" :key="s.key" class="stat-card" shadow="never">
        <div class="stat-top">
          <span class="stat-ico"><Icon :name="s.icon" :size="18" /></span>
          <span :class="`stat-delta ${s.up ? 'up' : 'down'}`">
            <Icon :name="s.up ? 'arrowUp' : 'arrowDown'" :size="12" :stroke="2.4" />{{ s.delta }}%
          </span>
        </div>
        <div class="stat-value tnum">{{ fmt(s.value) }}</div>
        <div class="stat-label">{{ s.label }}</div>
        <div class="stat-sub">{{ s.sub }}</div>
      </el-card>
    </div>

    <div class="two-col" style="margin-top: 18px">
      <el-card shadow="never">
        <template #header>
          <div class="panel-head panel-head--plain">
            <div>
              <div class="panel-title">接口调用趋势</div>
              <div class="panel-sub">{{ trendRange === '24h' ? '最近 24 小时' : trendRange === '7d' ? '最近 7 天' : '最近 30 天' }}</div>
            </div>
            <el-segmented v-model="trendRange" :options="[{ label: '24小时', value: '24h' }, { label: '7天', value: '7d' }, { label: '30天', value: '30d' }]" />
          </div>
        </template>
        <div style="padding: 20px 22px 10px">
          <div style="display: flex; align-items: baseline; gap: 12px; margin-bottom: 8px">
            <span class="tnum" style="font-size: 30px; font-weight: 700; letter-spacing: -0.02em">{{ fmt(trendTotal) }}</span>
            <el-tag type="success" effect="light" round><span class="el-tag-dot-inline" />成功率 {{ successRate }}%</el-tag>
          </div>
          <TrendChart :data="trend" />
          <div v-if="trendRange === '24h'" style="display: flex; justify-content: space-between; font-size: 11.5px; color: var(--ink-muted-48); margin-top: 6px">
            <span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>现在</span>
          </div>
          <div v-else style="display: flex; justify-content: space-between; font-size: 11.5px; color: var(--ink-muted-48); margin-top: 6px">
            <span>{{ trendRange === '7d' ? '7天前' : '30天前' }}</span><span>今天</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="panel-head panel-head--plain">
            <div>
              <div class="panel-title">角色构成</div>
              <div class="panel-sub">按数据权限类型统计</div>
            </div>
          </div>
        </template>
        <div style="padding: 18px 22px 22px; display: flex; flex-direction: column; align-items: center; gap: 18px">
          <DonutChart :segments="roleSeg" />
          <div style="width: 100%; display: flex; flex-direction: column; gap: 11px">
            <div v-for="s in roleSeg" :key="s.label" style="display: flex; align-items: center; gap: 10px; font-size: 13.5px">
              <span :style="{ width: '9px', height: '9px', borderRadius: '3px', background: s.color }" />
              <span style="flex: 1; color: var(--ink-72)">{{ s.label }}</span>
              <span class="tnum" style="font-weight: 600">{{ s.value }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="two-col" style="margin-top: 18px">
      <el-card shadow="never">
        <template #header>
          <div class="panel-head panel-head--plain">
            <div>
              <div class="panel-title">我的待办</div>
              <div class="panel-sub">{{ `${todoItems.length} 项待处理` }}</div>
            </div>
            <div class="page-actions">
              <el-button text size="small" @click="router.push({ name: 'todo' })">全部<Icon name="chevronRight" :size="14" /></el-button>
            </div>
          </div>
        </template>
        <div class="todo-list">
          <div v-for="t in todoItems" :key="t.id" class="todo-item" @click="router.push({ name: 'todo' })">
            <span :class="`todo-bar ${t.level}`" />
            <div style="flex: 1; min-width: 0">
              <div class="todo-title">{{ t.title }}</div>
              <div class="todo-meta">
                <el-avatar :size="18" :style="{ background: avatarColor(t.from), fontSize: Number(18) * 0.4 + 'px' }">{{ avatarText(t.from) }}</el-avatar>
                <span>{{ t.from }}</span><span class="todo-dot">/</span><span>{{ t.type }}</span>
              </div>
            </div>
            <div class="todo-time">{{ t.time }}</div>
            <el-button plain size="small" @click.stop="router.push({ name: 'todo' })">审批</el-button>
          </div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="panel-head panel-head--plain">
            <div>
              <div class="panel-title">最近动态</div>
              <div class="panel-sub">系统操作日志</div>
            </div>
          </div>
        </template>
        <div class="feed">
          <div v-for="(a, i) in activities" :key="i" class="feed-item">
            <el-avatar :size="32" :style="{ background: avatarColor(a.who, a.tint), fontSize: Number(32) * 0.4 + 'px' }">{{ avatarText(a.who) }}</el-avatar>
            <div style="flex: 1; min-width: 0">
              <div class="feed-text">
                <b>{{ a.who }}</b> {{ a.action }} <span class="feed-target">{{ a.target }}</span>
              </div>
              <div class="feed-time">{{ a.time }}</div>
            </div>
          </div>
          <div class="feed-more" @click="router.push({ name: 'operlog' })">查看完整日志</div>
        </div>
      </el-card>
    </div>
  </div>
</template>
