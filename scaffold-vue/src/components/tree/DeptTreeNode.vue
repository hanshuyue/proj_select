<script setup lang="ts">
import Icon from '@/components/Icon.vue'
import type { Dept } from '@/data'

defineProps<{
  node: Dept
  depth: number
  activeId: number | null
  expanded: number[]
}>()
const emit = defineEmits<{
  (e: 'select', node: Dept): void
  (e: 'toggle', id: number): void
}>()
</script>

<template>
  <div>
    <div
      :class="['tree-node-row', activeId === node.id ? 'active' : '']"
      :style="{ paddingLeft: 8 + depth * 18 + 'px' }"
      @click="emit('select', node)"
    >
      <span
        :class="['tree-caret', node.children && node.children.length ? '' : 'leaf', expanded.includes(node.id) ? 'open' : '']"
        @click.stop="emit('toggle', node.id)"
      >
        <Icon name="chevronRight" :size="14" />
      </span>
      <Icon
        :name="node.children && node.children.length ? 'folder' : 'dept'"
        :size="16"
        :style="{ color: activeId === node.id ? 'var(--primary)' : 'var(--ink-muted-48)' }"
      />
      <span class="tree-label" style="flex: 1">{{ node.name }}</span>
      <el-tag v-if="!node.status" type="info" effect="light" round>停用</el-tag>
    </div>
    <template v-if="node.children && node.children.length && expanded.includes(node.id)">
      <DeptTreeNode
        v-for="c in node.children" :key="c.id"
        :node="c" :depth="depth + 1" :active-id="activeId" :expanded="expanded"
        @select="emit('select', $event)" @toggle="emit('toggle', $event)"
      />
    </template>
  </div>
</template>
