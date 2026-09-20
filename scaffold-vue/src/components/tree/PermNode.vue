<script setup lang="ts">
import { ref, computed } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Menu } from '@/data'

const props = defineProps<{
  node: Menu
  depth: number
  checked: number[]
  checkedSet?: Set<number>
}>()
const emit = defineEmits<{ (e: 'toggle', id: number): void }>()

const btns = computed(() => (props.node.children || []).filter(c => c.type === 'btn'))
const subs = computed(() => (props.node.children || []).filter(c => c.type !== 'btn'))
const open = ref(props.depth < 2 || btns.value.length > 0)
const checkedLookup = computed(() => props.checkedSet ?? new Set(props.checked))
const descendantIds = computed(() => {
  const ids: number[] = []
  const walk = (nodes: Menu[]) => nodes.forEach(node => {
    ids.push(node.id)
    if (Array.isArray(node.children)) walk(node.children)
  })
  walk(props.node.children || [])
  return ids
})
const nodeIds = computed(() => [props.node.id, ...descendantIds.value])
const isOn = computed(() => nodeIds.value.every(id => checkedLookup.value.has(id)))
const isPartial = computed(() => !isOn.value && nodeIds.value.some(id => checkedLookup.value.has(id)))

function toggleNode() {
  emit('toggle', props.node.id)
}
</script>

<template>
  <div class="perm-node">
    <div class="perm-row" :style="{ paddingLeft: 8 + depth * 18 + 'px' }">
      <span :class="['tree-toggle', subs.length || btns.length ? '' : 'leaf', open ? 'open' : '']" @click="open = !open">
        <Icon name="chevronRight" :size="13" />
      </span>
      <el-checkbox :model-value="isOn" :indeterminate="isPartial" @click.stop @update:model-value="toggleNode" />
      <Icon v-if="node.icon" :name="node.icon" :size="15" style="color: var(--ink-muted-48)" />
      <span v-else style="width: 15px" />
      <span :style="{ fontSize: '13.5px', fontWeight: node.type === 'dir' ? 600 : 450 }">{{ node.name }}</span>
      <el-tag v-if="node.type === 'dir'" type="info" effect="light" round>目录</el-tag>
      <el-tag v-else-if="btns.length" type="primary" effect="light" round>{{ btns.length }} 按钮</el-tag>
    </div>
    <div v-if="btns.length && open" class="perm-btns">
      <span
        v-for="b in btns" :key="b.id"
        :class="['perm-btn-chip', checkedLookup.has(b.id) ? 'on' : '']"
        @click="emit('toggle', b.id)"
      >
        <Icon v-if="checkedLookup.has(b.id)" name="check" :size="12" :stroke="2.6" />{{ b.name }}
      </span>
    </div>
    <template v-if="subs.length && open">
      <PermNode
        v-for="c in subs" :key="c.id"
        :node="c" :depth="depth + 1" :checked="checked" :checked-set="checkedSet"
        @toggle="emit('toggle', $event)"
      />
    </template>
  </div>
</template>
