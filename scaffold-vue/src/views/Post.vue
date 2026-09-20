<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Post, User } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import type { Column, SortState } from '@/types/table'
import { avatarColor, avatarText } from '@/utils/avatar'

type ModalState = { type: 'add' | 'edit' | 'delete'; row?: Post; batch?: boolean } | null

interface PostForm extends Partial<Post> {
  postName?: string
  postCode?: string
  sort?: number
  remark?: string
}

const list = ref<Post[]>([])
const holderUsers = ref<User[]>([])
const kw = ref('')
const status = ref('')
const appliedFilters = reactive({ kw: '', status: '' })

const page = ref(1)
const pageSize = ref(10)
const selected = ref<(string | number)[]>([])
const sort = ref<SortState | null>(null)

const loading = ref(false)
const saving = ref(false)
const modal = ref<ModalState>(null)
const holders = ref<Post | null>(null)

const form = reactive<PostForm>({})
const err = reactive<Record<string, string>>({})

const filtered = computed(() => {
  let rows = list.value.filter(item =>
    (!appliedFilters.kw || item.postName.includes(appliedFilters.kw) || item.postCode.includes(appliedFilters.kw)) &&
    (appliedFilters.status === '' || item.status === Number(appliedFilters.status)),
  )

  if (sort.value) {
    const current = sort.value
    rows = [...rows].sort((a, b) => {
      const av = (a as Record<string, unknown>)[current.key]
      const bv = (b as Record<string, unknown>)[current.key]
      const result = av === bv ? 0 : av! > bv! ? 1 : -1
      return current.dir === 'asc' ? result : -result
    })
  }

  return rows
})

const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const columns: Column[] = [
  { key: 'postName', title: '岗位', sortable: true },
  { key: 'userCount', title: '在岗人数', width: 110, sortable: true },
  { key: 'sort', title: '排序', width: 80, sortable: true },
  { key: 'remark', title: '备注' },
  { key: 'status', title: '状态', width: 90 },
  { key: 'createTime', title: '创建时间', width: 160 },
  { key: 'actions', title: '操作', width: 100, className: 'col-actions' },
]

function holdersOf(row: Post) {
  return holderUsers.value.filter(user => user.postName === row.postName)
}

function clearErr() {
  Object.keys(err).forEach(key => delete err[key])
}

function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

async function reload() {
  loading.value = true
  try {
    const data = await systemApi.posts.list({
      pageNum: 1,
      pageSize: 200,
      keyword: appliedFilters.kw || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
    })
    list.value = data.rows as unknown as Post[]
    selected.value = []
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '岗位列表加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function exportPosts() {
  try {
    await systemApi.posts.export({ keyword: appliedFilters.kw || undefined })
    pushToast('岗位数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '岗位导出失败', 'danger')
  }
}

function applyFilters() {
  appliedFilters.kw = kw.value.trim()
  appliedFilters.status = status.value
  page.value = 1
  reload()
}

function resetFilters() {
  kw.value = ''
  status.value = ''
  appliedFilters.kw = ''
  appliedFilters.status = ''
  page.value = 1
  reload()
}

function openAdd() {
  Object.assign(form, {
    postName: '',
    postCode: '',
    sort: 10,
    status: 1,
    remark: '',
  })
  clearErr()
  modal.value = { type: 'add' }
}

function openEdit(row: Post) {
  Object.assign(form, { ...row })
  clearErr()
  modal.value = { type: 'edit', row }
}

async function openHolders(row: Post) {
  holders.value = row
  try {
    const data = await systemApi.users.list({ pageNum: 1, pageSize: 200 })
    holderUsers.value = data.rows as User[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '在岗成员加载失败', 'danger')
  }
}

async function saveForm() {
  clearErr()

  if (!form.postName?.trim()) err.postName = '请输入岗位名称'
  if (!form.postCode?.trim()) err.postCode = '请输入岗位编码'
  else if (!/^[a-z][a-z0-9_]*$/.test(form.postCode)) err.postCode = '仅支持小写字母、数字和下划线'
  if (Object.keys(err).length) return

  saving.value = true
  try {
    const payload = {
      postCode: form.postCode,
      postName: form.postName,
      sort: form.sort,
      status: form.status,
      remark: form.remark,
    }

    if (modal.value?.type === 'edit' && modal.value.row) {
      await systemApi.posts.update(modal.value.row.id, payload)
      pushToast('岗位已更新', 'ok')
    } else {
      await systemApi.posts.create(payload)
      pushToast('岗位已新增', 'ok')
    }

    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '岗位保存失败', 'danger')
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  try {
    if (modal.value?.batch) {
      const ids = selected.value.map(Number)
      await Promise.all(ids.map(id => systemApi.posts.remove(id)))
      selected.value = []
      pushToast(`已删除 ${ids.length} 个岗位`, 'ok')
    } else if (modal.value?.row) {
      await systemApi.posts.remove(modal.value.row.id)
      pushToast('岗位已删除', 'ok')
    }

    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '岗位删除失败', 'danger')
  }
}

onMounted(reload)
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">岗位管理</h1>
          <div class="page-desc">维护组织岗位信息，支持查看在岗成员，仍有关联人员的岗位不能直接删除。</div>
        </div>
        <div class="page-actions">
          <el-button plain @click="exportPosts">
            <Icon name="download" :size="16" />
            导出
          </el-button>
          <el-button type="primary" @click="openAdd">
            <Icon name="plus" :size="16" />
            新增岗位
          </el-button>
        </div>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form class="filter-form" :inline="true" label-position="right" @submit.prevent>
        <el-form-item label="关键词" class="filter-item filter-item-keyword">
          <el-input
            v-model="kw"
            clearable
            style="width: 240px"
            placeholder="岗位名称 / 编码"
            @keyup.enter="applyFilters"
          />
        </el-form-item>
        <el-form-item label="状态" class="filter-item">
          <el-select v-model="status" filterable style="width: 140px">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" @click="applyFilters">
            <Icon name="search" :size="16" />
            查询
          </el-button>
          <el-button plain @click="resetFilters">
            <Icon name="refresh" :size="16" />
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="display: flex; align-items: center; gap: 12px">
          <span class="panel-title">岗位列表</span>
          <span class="cell-muted" style="font-size: 13px">共 {{ filtered.length }} 个岗位</span>
        </div>
        <el-tooltip content="导出当前结果" placement="top">
          <el-button aria-label="导出当前结果" title="导出当前结果" plain circle @click="exportPosts">
            <Icon name="download" :size="17" />
          </el-button>
        </el-tooltip>
      </div>

      <div v-if="selected.length" class="selbar">
        已选择 <b>{{ selected.length }}</b> 项
        <el-button text size="small" class="danger" @click="modal = { type: 'delete', batch: true }">批量删除</el-button>
        <div style="flex: 1" />
        <el-button text size="small" @click="selected = []">取消选择</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="paged"
        :size="appearance.density === 'compact' ? 'small' : appearance.density === 'comfy' ? 'large' : 'default'"
        row-key="id"
        border
        style="width: 100%"
        @sort-change="onSortChange"
        @selection-change="selected = $event.map((row: any) => row.id)"
      >
        <el-table-column type="selection" width="46" reserve-selection />
        <el-table-column
          v-for="col in columns"
          :key="col.key"
          :prop="col.key"
          :label="col.title"
          :width="col.width"
          :align="col.align"
          :class-name="col.className"
          :sortable="col.sortable ? 'custom' : false"
        >
          <template #default="{ row }">
            <template v-if="col.key === 'postName'">
              <div style="display: flex; align-items: center; gap: 11px">
                <span class="menu-ico-box" style="width: 34px; height: 34px; background: var(--primary-soft); color: var(--primary)">
                  <Icon name="post" :size="16" />
                </span>
                <div>
                  <div class="cell-strong">{{ row.postName }}</div>
                  <div style="margin-top: 2px"><span class="code-chip">{{ row.postCode }}</span></div>
                </div>
              </div>
            </template>
            <template v-else-if="col.key === 'userCount'">
              <button v-if="row.userCount > 0" class="linkcount" title="查看在岗成员" @click="openHolders(row)">
                <span class="tnum">{{ row.userCount }}</span>
                <Icon name="users" :size="13" />
              </button>
              <span v-else class="tnum cell-muted">0</span>
            </template>
            <template v-else-if="col.key === 'sort'">
              <span class="tnum cell-muted">{{ row.sort }}</span>
            </template>
            <template v-else-if="col.key === 'remark'">
              <span class="cell-muted" style="font-size: 13px">{{ row.remark || '—' }}</span>
            </template>
            <template v-else-if="col.key === 'status'">
              <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="light" round>
                <span class="el-tag-dot-inline" />
                {{ Number(row.status) === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
            <template v-else-if="col.key === 'createTime'">
              <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.createTime }}</span>
            </template>
            <template v-else-if="col.key === 'actions'">
              <div class="row-actions">
                <el-tooltip content="编辑" placement="top">
                  <el-button aria-label="编辑" title="编辑" text circle @click="openEdit(row)">
                    <Icon name="edit" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button
                    aria-label="删除"
                    title="删除"
                    text
                    circle
                    :disabled="row.userCount > 0"
                    @click="modal = { type: 'delete', row }"
                  >
                    <Icon name="trash" :size="16" />
                  </el-button>
                </el-tooltip>
              </div>
            </template>
            <template v-else>{{ row[col.key] }}</template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filtered.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @update:page-size="page = 1"
        />
      </div>
    </el-card>

    <el-dialog :model-value="true"
      v-if="modal && (modal.type === 'add' || modal.type === 'edit')"
      :title="modal.type === 'edit' ? '编辑岗位' : '新增岗位'"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="form-grid">
        <el-form-item label="岗位名称" required :error="err.postName">
          <input v-model="form.postName" :class="['input', err.postName ? 'invalid' : '']" placeholder="如：研发工程师" />
        </el-form-item>
        <el-form-item label="岗位编码" required :error="err.postCode">
          <input
            v-model="form.postCode"
            :class="['input', err.postCode ? 'invalid' : '']"
            :disabled="modal.type === 'edit'"
            placeholder="如：dev_engineer"
          />
        </el-form-item>
        <el-form-item label="显示排序">
          <input v-model.number="form.sort" class="input" type="number" />
        </el-form-item>
        <el-form-item label="岗位状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ form.status === 1 ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注" class="col-2">
          <textarea v-model="form.remark" class="input" placeholder="岗位职责说明" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true"
      v-if="modal && modal.type === 'delete'"
      title="删除岗位"
      width="420px"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="confirm-body">
        <div v-if="modal.batch">
          确认删除选中的 <b>{{ selected.length }}</b> 个岗位吗？此操作不可恢复。
        </div>
        <div v-else-if="modal.row">
          确认删除岗位 <b>{{ modal.row.postName }}</b> 吗？此操作不可恢复。
        </div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>

    <el-drawer :model-value="true"
      v-if="holders"
      :title="`在岗成员 · ${holders.postName}`"
      size="460px"
      destroy-on-close
      append-to-body
      @close="holders = null"
    >
      <p class="panel-sub">共 {{ holders.userCount }} 人，数据来自用户管理</p>
      <div class="holder-list">
        <div v-for="user in holdersOf(holders)" :key="user.id" class="holder-row">
          <el-avatar :size="38" :style="{ background: avatarColor(user.nickname), fontSize: `${38 * 0.4}px` }">
            {{ avatarText(user.nickname) }}
          </el-avatar>
          <div style="flex: 1; min-width: 0">
            <div class="cell-strong" style="display: flex; align-items: center; gap: 6px">
              {{ user.nickname }}
              <span class="code-chip">@{{ user.username }}</span>
            </div>
            <div class="cell-muted" style="font-size: 12.5px; margin-top: 2px; display: flex; align-items: center; gap: 6px">
              <Icon name="dept" :size="12" />
              {{ user.deptName }} · {{ user.roleName }}
            </div>
          </div>
          <el-tag :type="Number(user.status) === 1 ? 'success' : 'info'" effect="light" round>
            <span class="el-tag-dot-inline" />
            {{ Number(user.status) === 1 ? '正常' : '停用' }}
          </el-tag>
        </div>
      </div>
      <div
        style="margin-top: 16px; padding: 11px 14px; background: var(--primary-soft); border-radius: var(--r-sm); font-size: 12.5px; color: var(--primary); display: flex; gap: 8px; align-items: center"
      >
        <Icon name="users" :size="14" />
        在岗人数根据用户当前任职岗位自动统计。
      </div>
      <template #footer>
        <el-button plain @click="holders = null">关闭</el-button>
      </template>
    </el-drawer>
  </div>
</template>
