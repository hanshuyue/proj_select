<script setup lang="ts">
import { appearance, ACCENTS } from '@/composables/appearance'

const emit = defineEmits<{ (e: 'close'): void }>()

const sidebarTones = [{ label: '墨黑', value: 'ink' }, { label: '纯黑', value: 'black' }, { label: '浅色', value: 'light' }]
const navActives = [{ label: '胶囊', value: 'pill' }, { label: '侧条', value: 'bar' }, { label: '淡色', value: 'tint' }]
const layouts = [{ label: '表格', value: 'table' }, { label: '卡片', value: 'cards' }]
const densities = [{ label: '舒展', value: 'comfy' }, { label: '常规', value: 'regular' }, { label: '紧凑', value: 'compact' }]
const accents = Object.keys(ACCENTS)
</script>

<template>
  <el-dialog title="外观设置" width="460px" destroy-on-close append-to-body @close="emit('close')">
    <div class="appearance-section">
      <div class="appearance-label">侧边栏底色</div>
      <div class="seg-pills">
        <button
          v-for="o in sidebarTones" :key="o.value"
          :class="['seg-pill', appearance.sidebarTone === o.value ? 'on' : '']"
          @click="appearance.sidebarTone = o.value as any"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="appearance-section">
      <div class="appearance-label">菜单选中样式</div>
      <div class="seg-pills">
        <button
          v-for="o in navActives" :key="o.value"
          :class="['seg-pill', appearance.navActive === o.value ? 'on' : '']"
          @click="appearance.navActive = o.value as any"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="appearance-section">
      <div class="appearance-label">用户页排布</div>
      <div class="seg-pills">
        <button
          v-for="o in layouts" :key="o.value"
          :class="['seg-pill', appearance.listLayout === o.value ? 'on' : '']"
          @click="appearance.listLayout = o.value as any"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="appearance-section">
      <div class="appearance-label">表格行密度</div>
      <div class="seg-pills">
        <button
          v-for="o in densities" :key="o.value"
          :class="['seg-pill', appearance.density === o.value ? 'on' : '']"
          @click="appearance.density = o.value as any"
        >{{ o.label }}</button>
      </div>
    </div>

    <div class="appearance-section">
      <div class="appearance-label">强调色</div>
      <div class="accent-swatches">
        <button
          v-for="a in accents" :key="a"
          :class="['accent-swatch', appearance.accent === a ? 'on' : '']"
          :style="{ background: a }"
          @click="appearance.accent = a"
        />
      </div>
      <div class="appearance-hint">设计基线为单一 Action Blue；其余强调色仅供探索。</div>
    </div>

    <template #footer>
      <el-button type="primary" @click="emit('close')">完成</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.appearance-section { margin-bottom: 20px; }
.appearance-section:last-of-type { margin-bottom: 0; }
.appearance-label { font-size: 13px; font-weight: 600; color: var(--ink-72); margin-bottom: 10px; }
.seg-pills { display: flex; gap: 8px; }
.seg-pill {
  flex: 1; height: 36px; border-radius: var(--r-sm); border: 1px solid var(--border-input);
  background: var(--canvas); color: var(--ink-72); font-size: 13px; cursor: pointer;
  transition: all .14s;
}
.seg-pill:hover { border-color: var(--ink-muted-30); }
.seg-pill.on { border-color: var(--primary); background: var(--primary-soft); color: var(--primary); font-weight: 600; }
.accent-swatches { display: flex; gap: 12px; }
.accent-swatch {
  width: 34px; height: 34px; border-radius: 50%; border: 2px solid transparent; cursor: pointer;
  transition: transform .12s; position: relative;
}
.accent-swatch:active { transform: scale(0.92); }
.accent-swatch.on { box-shadow: 0 0 0 2px var(--canvas), 0 0 0 4px var(--primary); }
.appearance-hint { font-size: 11.5px; color: var(--ink-muted-48); margin-top: 12px; line-height: 1.5; }
</style>
