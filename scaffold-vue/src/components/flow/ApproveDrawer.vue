<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import FlowSteps from './FlowSteps.vue'
import type { WfTodo, WfTrace } from '@/data'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { currentUser } from '@/composables/auth'

const PRIORITY: Record<string, { label: string; tone: string }> = {
  high: { label: '紧急', tone: 'danger' }, mid: { label: '普通', tone: 'info' }, low: { label: '较低', tone: 'neutral' },
}

const props = withDefaults(defineProps<{ task: WfTodo; submitting?: boolean }>(), { submitting: false })
const emit = defineEmits<{ (e: 'close'): void; (e: 'submit', task: WfTodo, resultLabel: string, result: string, comment: string): void }>()

const action = ref<'pass' | 'reject' | 'return' | 'revoke'>('pass')
const comment = ref('')
const trace = ref<WfTrace[]>([])
const canRevoke = computed(() => currentUser.value?.username === props.task.starter)
const actionOptions = computed(() => [
  { label: '通过', value: 'pass', icon: 'check' },
  { label: '驳回', value: 'reject', icon: 'x' },
  { label: '退回', value: 'return', icon: 'undo' },
  ...(canRevoke.value ? [{ label: '撤回', value: 'revoke', icon: 'refresh' }] : []),
])

onMounted(loadTrace)

async function loadTrace() {
  try {
    const rows = props.task.processInstanceId
      ? await portalApi.workflow.instances.trace(props.task.processInstanceId)
      : await portalApi.workflow.tasks.trace(props.task.id)
    trace.value = rows as unknown as WfTrace[]
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '审批记录加载失败', 'danger')
  }
}

function submit() {
  if (props.submitting) return
  if (action.value !== 'pass' && !comment.value.trim()) { pushToast('请填写审批意见', 'danger'); return }
  const map: Record<string, string> = { pass: '已通过', reject: '已驳回', return: '已退回', revoke: '已撤回' }
  emit('submit', props.task, map[action.value], action.value, comment.value)
}
</script>

<template>
  <el-drawer title="审批处理" size="560px" destroy-on-close append-to-body @close="emit('close')">
    <p class="panel-sub">{{ task.procName }}</p>
    <!-- 业务信息 -->
    <div class="approve-biz">
      <div class="approve-biz-title">{{ task.bizTitle }}</div>
      <div class="approve-biz-meta">
        <span><Icon name="user" :size="13" />发起人 {{ task.starter }}</span>
        <span><Icon name="clock" :size="13" />{{ task.createTime }}</span>
        <el-tag :type="(PRIORITY[task.priority]?.tone as any) === 'ok' ? 'success' : (PRIORITY[task.priority]?.tone as any) === 'warn' ? 'warning' : (PRIORITY[task.priority]?.tone as any) === 'danger' ? 'danger' : (PRIORITY[task.priority]?.tone as any) === 'info' || (PRIORITY[task.priority]?.tone as any) === 'purple' ? 'primary' : 'info'" effect="light" round>{{ PRIORITY[task.priority]?.label ?? '普通' }}</el-tag>
      </div>
    </div>

    <!-- 流转进度 -->
    <div class="approve-sec-label">流转进度</div>
    <div class="flow-diagram" style="margin-bottom: 20px">
      <FlowSteps :current="task.node" :total="task.total" :nodes="task.nodes" />
    </div>

    <!-- 流转记录 -->
    <div class="approve-sec-label">审批记录</div>
    <div class="trace-list">
      <div v-for="(t, i) in trace" :key="i" :class="['trace-item', t.current ? 'current' : '']">
        <span :class="['trace-node', t.done ? 'done' : t.current ? 'current' : '']">
          <Icon v-if="t.done" name="check" :size="12" :stroke="3" /><span v-else class="trace-num">{{ i + 1 }}</span>
        </span>
        <div class="trace-body">
          <div class="trace-head">
            <span class="trace-name">{{ t.node }}</span>
            <el-tag :type="(t.tone as any) === 'ok' ? 'success' : (t.tone as any) === 'warn' ? 'warning' : (t.tone as any) === 'danger' ? 'danger' : (t.tone as any) === 'info' || (t.tone as any) === 'purple' ? 'primary' : 'info'" effect="light" round>{{ t.result }}</el-tag>
          </div>
          <div class="trace-meta">{{ t.who }} · {{ t.time }}</div>
          <div v-if="t.comment" class="trace-comment">{{ t.comment }}</div>
        </div>
      </div>
    </div>

    <!-- 审批操作 -->
    <div class="approve-sec-label">我的处理</div>
    <el-segmented v-model="action" :options="actionOptions">
      <template #default="{ item }">
        <span style="display: inline-flex; align-items: center; gap: 5px">
          <Icon v-if="item.icon" :name="item.icon" :size="15" />
          {{ item.label }}
        </span>
      </template>
    </el-segmented>
    <textarea class="input" style="margin-top: 12px; min-height: 90px"
      :placeholder="action === 'pass' ? '审批意见（选填）' : '请填写审批意见（必填）'"
      v-model="comment" />

    <template #footer>
      <el-button plain @click="emit('close')">取消</el-button>
      <el-button :type="action === 'pass' ? 'primary' : 'danger'" :disabled="submitting" @click="submit"><Icon name="send" :size="14" />提交审批</el-button>
    </template>
  </el-drawer>
</template>
