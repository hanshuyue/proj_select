# 政企项目材料标准化与智能生成平台

系统标识：`gems-platform`。本项目用于政企项目数据标准化管理、收支及经济效益测算、模板管理和项目材料智能生成。

Apple 设计语言风格的企业后台管理脚手架，基于 **Vue 3 + Vite + TypeScript**。
由 Claude Design 的 HTML/CSS/JS 原型（`untitled/`）像素级还原而来。

## 技术栈

- Vue 3（`<script setup>` + Composition API）
- Vite 6
- TypeScript（strict）
- Vue Router 4（动态路由 + 登录守卫）
- 零第三方 UI 库 —— 全部组件与 Apple 风设计令牌均为自实现 CSS
- 真实后端接口：默认通过 Vite `/api` 代理接入 `scaffold-system`，可用 `VITE_API_PROXY_TARGET` 覆盖后端地址。

## 快速开始

```bash
npm install
npm run dev      # 启动开发服务器（默认 http://localhost:5173）
npm run build    # 类型检查 + 生产构建
npm run preview  # 预览生产构建
```

演示账号：`admin` / 密码 `admin123`（验证码任意填写）。

## 目录结构

```
src/
  main.ts                 入口，按原型顺序加载全部样式
  App.vue                 根组件（router-view + Toast 容器）
  router/index.ts         路由表与登录守卫
  styles/                 设计令牌与全部 CSS（移植自原型 + 补齐 dashboard.css）
  data/                   TS 类型、导航配置、头像色板与图标库
  composables/            auth（登录态）/ toast / appearance（外观设置）
  components/
    Icon.vue              线性图标组件
    ui/                   通用组件库：Button/DataTable/Modal/Drawer/Select…
    layout/               AppShell / Sidebar / Topbar / AppearanceModal
    tree/                 部门树 / 权限树（递归组件）
    flow/                 工作流节点条 / 审批抽屉
    charts/               趋势折线 / 角色环形图（纯 SVG）
  views/                  16 个业务页面
```

## 功能页面（16 个）

工作台 · 用户管理 · 角色管理（含权限/数据权限抽屉）· 菜单管理（树形表格）·
部门管理（左树右详情）· 岗位管理 · 字典管理 · 参数配置 ·
操作日志 · 登录日志 · 流程模型 · 流程定义 · 我的待办（审批）· 我的已办 · 代码生成。

## 关于原始 handoff 包的两处缺口

原型 `untitled/project/` 在 HTML 中引用了两个文件，但包内缺失，已在本项目补齐：

- `css/dashboard.css` —— 依据 Dashboard 组件用到的类名按设计系统重写（`src/styles/dashboard.css`）。
- `js/dict.jsx` —— 字典管理页原型缺失，已依据 `DICT_TYPES` / `DICT_DATA` 数据与
  `pages.css` 中既有的 `.dict-*` 样式从零实现（`src/views/Dict.vue`）。

此外，原型用于探索风格的浮层 Tweaks 面板（`tweaks-panel.jsx`，属 claude.ai/design 工具件，
非产品 UI）未移植，其能力固化为顶栏「外观设置」弹层（侧栏底色 / 选中样式 / 行密度 / 强调色），
并通过 `localStorage` 持久化。

> 说明：当前正式页面已接入 `scaffold-system` 真实接口，认证、系统管理、监控日志、工作台、工作流和代码生成均通过 `src/api/` 调用后端。
> `src/data/` 主要保留类型、导航和展示配置；历史静态原型仍位于 `untitled/`，仅作为视觉与交互参考。
