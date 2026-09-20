import type { RouteItem } from './auth'

export const MOCK_MENU_TREE: RouteItem[] = [
  {
    id: 1000,
    name: '系统管理',
    icon: 'gear',
    children: [
      { id: 1001, name: '用户管理', path: 'user', icon: 'users', component: 'Users', permission: 'system:user:list' },
      { id: 1002, name: '角色管理', path: 'role', icon: 'role', component: 'Roles', permission: 'system:role:list' },
      { id: 1003, name: '菜单管理', path: 'menu', icon: 'menu', component: 'Menus', permission: 'system:menu:list' },
      { id: 1004, name: '部门管理', path: 'dept', icon: 'dept', component: 'Depts', permission: 'system:dept:list' },
      { id: 1005, name: '岗位管理', path: 'post', icon: 'post', component: 'Post', permission: 'system:post:list' },
      { id: 1006, name: '字典管理', path: 'dict', icon: 'dict', component: 'Dict', permission: 'system:dict:list' },
      { id: 1007, name: '参数配置', path: 'config', icon: 'gear', component: 'Config', permission: 'system:config:list' },
      { id: 1009, name: '通知通道', path: 'systemNoticeChannel', icon: 'bell', component: 'Config', permission: 'system:notice:list' },
    ],
  },
  {
    id: 1100,
    name: '系统监控',
    icon: 'monitor',
    children: [
      { id: 1101, name: '操作日志', path: 'operlog', icon: 'log', component: 'OperLog', permission: 'system:log:oper' },
      { id: 1102, name: '登录日志', path: 'loginlog', icon: 'lock', component: 'LoginLog', permission: 'system:log:login' },
    ],
  },
  {
    id: 1200,
    name: '工作流',
    icon: 'flow',
    children: [
      { id: 1201, name: '模型管理', path: 'model', icon: 'flow', component: 'WfModel', permission: 'workflow:model:list' },
      { id: 1202, name: '流程定义', path: 'def', icon: 'layers', component: 'WfDef', permission: 'workflow:def:list' },
      { id: 1203, name: '我的待办', path: 'todo', icon: 'bell', component: 'WfTodo', permission: 'workflow:task:todo' },
      { id: 1204, name: '已办任务', path: 'done', icon: 'check', component: 'WfDone', permission: 'workflow:task:done' },
    ],
  },
  {
    id: 1300,
    name: '开发工具',
    icon: 'database',
    children: [
      { id: 1301, name: '代码生成', path: 'gen', icon: 'database', component: 'Generator', permission: 'tool:generator:list' },
    ],
  },
]
