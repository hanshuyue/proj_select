/* ============================================================
   Mock 数据 —— 全中文，贴合 PRD 字段
   ============================================================ */

// —— 部门树 ——
const DEPTS = [
  { id: 1, parentId: 0, name: '云原科技集团', leader: '周明远', phone: '010-8800-1000', email: 'hq@yunyuan.com', sort: 1, status: 1,
    children: [
      { id: 2, parentId: 1, name: '研发中心', leader: '陈思远', phone: '010-8800-1010', email: 'rd@yunyuan.com', sort: 1, status: 1,
        children: [
          { id: 5, parentId: 2, name: '平台架构部', leader: '李维', phone: '010-8800-1011', email: 'arch@yunyuan.com', sort: 1, status: 1 },
          { id: 6, parentId: 2, name: '应用开发部', leader: '吴敏', phone: '010-8800-1012', email: 'app@yunyuan.com', sort: 2, status: 1 },
          { id: 7, parentId: 2, name: '测试质量部', leader: '韩雪', phone: '010-8800-1013', email: 'qa@yunyuan.com', sort: 3, status: 1 },
        ] },
      { id: 3, parentId: 1, name: '产品中心', leader: '赵晴', phone: '010-8800-1020', email: 'pm@yunyuan.com', sort: 2, status: 1,
        children: [
          { id: 8, parentId: 3, name: '产品设计部', leader: '孙浩', phone: '010-8800-1021', email: 'design@yunyuan.com', sort: 1, status: 1 },
          { id: 9, parentId: 3, name: '用户研究部', leader: '马玲', phone: '010-8800-1022', email: 'ux@yunyuan.com', sort: 2, status: 0 },
        ] },
      { id: 4, parentId: 1, name: '职能中心', leader: '钱伟', phone: '010-8800-1030', email: 'hr@yunyuan.com', sort: 3, status: 1,
        children: [
          { id: 10, parentId: 4, name: '人力资源部', leader: '冯婷', phone: '010-8800-1031', email: 'people@yunyuan.com', sort: 1, status: 1 },
          { id: 11, parentId: 4, name: '财务部', leader: '蒋文', phone: '010-8800-1032', email: 'finance@yunyuan.com', sort: 2, status: 1 },
        ] },
    ] },
];

// —— 角色 ——
const ROLES = [
  { id: 1, name: '超级管理员', code: 'super_admin', dataScope: '全部数据权限', userCount: 2, sort: 1, status: 1, builtin: true, createTime: '2024-01-08 09:12', remark: '系统最高权限，不可删除' },
  { id: 2, name: '系统管理员', code: 'sys_admin', dataScope: '本部门及以下', userCount: 5, sort: 2, status: 1, builtin: false, createTime: '2024-02-14 10:33', remark: '负责系统日常配置与维护' },
  { id: 3, name: '业务管理员', code: 'biz_admin', dataScope: '本部门数据权限', userCount: 12, sort: 3, status: 1, builtin: false, createTime: '2024-03-02 14:20', remark: '维护业务数据与审批流程' },
  { id: 4, name: '研发工程师', code: 'developer', dataScope: '仅本人数据权限', userCount: 28, sort: 4, status: 1, builtin: false, createTime: '2024-03-19 16:45', remark: '使用代码生成与开发模块' },
  { id: 5, name: '审批专员', code: 'approver', dataScope: '自定义数据权限', userCount: 8, sort: 5, status: 1, builtin: false, createTime: '2024-04-07 11:08', remark: '处理待办审批任务' },
  { id: 6, name: '只读访客', code: 'guest', dataScope: '仅本人数据权限', userCount: 15, sort: 6, status: 0, builtin: false, createTime: '2024-05-21 08:50', remark: '仅可查看授权报表' },
];

// —— 用户 ——
const FIRST = ['陈','李','王','张','刘','杨','黄','赵','周','吴','徐','孙','马','朱','胡','郭','林','何','高','罗'];
const GIVEN = ['思远','维','敏','雪','浩','玲','文','婷','晴','明远','子honestly','航','悦','宁','哲','楠','璐','岩','蕊','涛'];
const AVATAR_TINTS = ['#0066cc','#1f8a4c','#b06d00','#7a5ae0','#c4321f','#0a84b0','#5a6470'];

const DEPT_FLAT = [
  { id: 5, name: '平台架构部' }, { id: 6, name: '应用开发部' }, { id: 7, name: '测试质量部' },
  { id: 8, name: '产品设计部' }, { id: 9, name: '用户研究部' }, { id: 10, name: '人力资源部' }, { id: 11, name: '财务部' },
];
const ROLE_NAMES = ['超级管理员','系统管理员','业务管理员','研发工程师','审批专员','只读访客'];
const POST_NAMES = ['董事长','总经理','技术总监','产品经理','架构师','研发工程师','测试工程师','人事专员','财务专员'];
// 部门 → 该部门常见岗位（用户岗位据此分配，保证联动合理）
const DEPT_POSTS = {
  '平台架构部': ['技术总监','架构师','研发工程师'],
  '应用开发部': ['研发工程师','架构师'],
  '测试质量部': ['测试工程师','研发工程师'],
  '产品设计部': ['产品经理'],
  '用户研究部': ['产品经理'],
  '人力资源部': ['人事专员'],
  '财务部': ['财务专员'],
};

function genUsers() {
  const fixed = [
    { id: 1, username: 'admin', nickname: '周明远', phone: '138-0011-8800', email: 'zhoumy@yunyuan.com', deptName: '平台架构部', postName: '技术总监', roleName: '超级管理员', status: 1, createTime: '2024-01-08 09:12', lastLogin: '2026-06-05 08:41' },
    { id: 2, username: 'chensy', nickname: '陈思远', phone: '139-0022-1010', email: 'chensy@yunyuan.com', deptName: '平台架构部', postName: '架构师', roleName: '系统管理员', status: 1, createTime: '2024-02-14 10:33', lastLogin: '2026-06-04 19:22' },
    { id: 3, username: 'wumin', nickname: '吴敏', phone: '137-0033-1012', email: 'wumin@yunyuan.com', deptName: '应用开发部', postName: '研发工程师', roleName: '业务管理员', status: 1, createTime: '2024-03-02 14:20', lastLogin: '2026-06-05 07:55' },
    { id: 4, username: 'hanxue', nickname: '韩雪', phone: '136-0044-1013', email: 'hanxue@yunyuan.com', deptName: '测试质量部', postName: '测试工程师', roleName: '研发工程师', status: 1, createTime: '2024-03-19 16:45', lastLogin: '2026-06-03 13:10' },
    { id: 5, username: 'zhaoq', nickname: '赵晴', phone: '135-0055-1020', email: 'zhaoq@yunyuan.com', deptName: '产品设计部', postName: '产品经理', roleName: '审批专员', status: 0, createTime: '2024-04-07 11:08', lastLogin: '2026-05-28 09:30' },
    { id: 6, username: 'sunhao', nickname: '孙浩', phone: '134-0066-1021', email: 'sunhao@yunyuan.com', deptName: '产品设计部', postName: '产品经理', roleName: '研发工程师', status: 1, createTime: '2024-04-22 15:40', lastLogin: '2026-06-05 09:02' },
    { id: 7, username: 'maling', nickname: '马玲', phone: '133-0077-1022', email: 'maling@yunyuan.com', deptName: '用户研究部', postName: '产品经理', roleName: '只读访客', status: 1, createTime: '2024-05-11 09:18', lastLogin: '2026-06-02 16:48' },
    { id: 8, username: 'fengt', nickname: '冯婷', phone: '132-0088-1031', email: 'fengt@yunyuan.com', deptName: '人力资源部', postName: '人事专员', roleName: '业务管理员', status: 1, createTime: '2024-05-21 08:50', lastLogin: '2026-06-04 11:25' },
  ];
  const out = [...fixed];
  for (let i = 9; i <= 56; i++) {
    const fn = FIRST[i % FIRST.length];
    const gn = GIVEN[(i * 3) % GIVEN.length].replace('honestly','宇');
    const dept = DEPT_FLAT[i % DEPT_FLAT.length];
    const posts = DEPT_POSTS[dept.name] || ['研发工程师'];
    out.push({
      id: i,
      username: 'user' + String(i).padStart(3, '0'),
      nickname: fn + gn,
      phone: '1' + (30 + i % 9) + '-' + String(1000 + i * 7).slice(0,4) + '-' + String(2000 + i * 13).slice(0,4),
      email: 'user' + i + '@yunyuan.com',
      deptName: dept.name,
      postName: posts[i % posts.length],
      roleName: ROLE_NAMES[(i + 2) % ROLE_NAMES.length],
      status: i % 7 === 0 ? 0 : 1,
      createTime: '2024-' + String(1 + i % 9).padStart(2,'0') + '-' + String(1 + i % 27).padStart(2,'0') + ' 10:' + String(i % 60).padStart(2,'0'),
      lastLogin: '2026-0' + (1 + i % 5) + '-' + String(1 + i % 27).padStart(2,'0') + ' ' + String(8 + i % 11).padStart(2,'0') + ':' + String(i*2 % 60).padStart(2,'0'),
    });
  }
  return out;
}
const USERS = genUsers();

// —— 菜单树（目录 / 菜单 / 按钮）——
const MENUS = [
  { id: 1, name: '工作台', type: 'menu', icon: 'dashboard', path: '/dashboard', component: 'dashboard/index', permission: '', sort: 1, visible: 1, status: 1 },
  { id: 100, name: '系统管理', type: 'dir', icon: 'gear', path: '/system', component: '', permission: '', sort: 2, visible: 1, status: 1, children: [
    { id: 101, name: '用户管理', type: 'menu', icon: 'users', path: '/system/user', component: 'system/user/index', permission: 'system:user:list', sort: 1, visible: 1, status: 1, children: [
      { id: 1011, name: '用户查询', type: 'btn', icon: '', path: '', component: '', permission: 'system:user:query', sort: 1, visible: 1, status: 1 },
      { id: 1012, name: '用户新增', type: 'btn', icon: '', path: '', component: '', permission: 'system:user:add', sort: 2, visible: 1, status: 1 },
      { id: 1013, name: '用户修改', type: 'btn', icon: '', path: '', component: '', permission: 'system:user:edit', sort: 3, visible: 1, status: 1 },
      { id: 1014, name: '用户删除', type: 'btn', icon: '', path: '', component: '', permission: 'system:user:remove', sort: 4, visible: 1, status: 1 },
      { id: 1015, name: '重置密码', type: 'btn', icon: '', path: '', component: '', permission: 'system:user:resetPwd', sort: 5, visible: 1, status: 1 },
    ]},
    { id: 102, name: '角色管理', type: 'menu', icon: 'role', path: '/system/role', component: 'system/role/index', permission: 'system:role:list', sort: 2, visible: 1, status: 1, children: [
      { id: 1021, name: '角色查询', type: 'btn', icon: '', path: '', component: '', permission: 'system:role:query', sort: 1, visible: 1, status: 1 },
      { id: 1022, name: '角色新增', type: 'btn', icon: '', path: '', component: '', permission: 'system:role:add', sort: 2, visible: 1, status: 1 },
      { id: 1023, name: '角色修改', type: 'btn', icon: '', path: '', component: '', permission: 'system:role:edit', sort: 3, visible: 1, status: 1 },
    ]},
    { id: 103, name: '菜单管理', type: 'menu', icon: 'menu', path: '/system/menu', component: 'system/menu/index', permission: 'system:menu:list', sort: 3, visible: 1, status: 1 },
    { id: 104, name: '部门管理', type: 'menu', icon: 'dept', path: '/system/dept', component: 'system/dept/index', permission: 'system:dept:list', sort: 4, visible: 1, status: 1 },
    { id: 105, name: '字典管理', type: 'menu', icon: 'dict', path: '/system/dict', component: 'system/dict/index', permission: 'system:dict:list', sort: 5, visible: 1, status: 1 },
    { id: 106, name: '参数配置', type: 'menu', icon: 'gear', path: '/system/config', component: 'system/config/index', permission: 'system:config:list', sort: 6, visible: 0, status: 1 },
  ]},
  { id: 200, name: '系统监控', type: 'dir', icon: 'log', path: '/monitor', component: '', permission: '', sort: 3, visible: 1, status: 1, children: [
    { id: 201, name: '操作日志', type: 'menu', icon: 'file', path: '/monitor/oper-log', component: 'monitor/operlog/index', permission: 'monitor:operlog:list', sort: 1, visible: 1, status: 1 },
    { id: 202, name: '登录日志', type: 'menu', icon: 'lock', path: '/monitor/login-log', component: 'monitor/loginlog/index', permission: 'monitor:loginlog:list', sort: 2, visible: 1, status: 1 },
  ]},
  { id: 300, name: '工作流程', type: 'dir', icon: 'flow', path: '/workflow', component: '', permission: '', sort: 4, visible: 1, status: 1, children: [
    { id: 301, name: '流程模型', type: 'menu', icon: 'grid', path: '/workflow/model', component: 'workflow/model/index', permission: 'workflow:model:list', sort: 1, visible: 1, status: 1 },
    { id: 302, name: '流程定义', type: 'menu', icon: 'file', path: '/workflow/definition', component: 'workflow/def/index', permission: 'workflow:def:list', sort: 2, visible: 1, status: 1 },
    { id: 303, name: '我的待办', type: 'menu', icon: 'bell', path: '/workflow/todo', component: 'workflow/todo/index', permission: 'workflow:todo:list', sort: 3, visible: 1, status: 1 },
    { id: 304, name: '我的已办', type: 'menu', icon: 'check', path: '/workflow/done', component: 'workflow/done/index', permission: 'workflow:done:list', sort: 4, visible: 1, status: 1 },
  ]},
  { id: 400, name: '系统工具', type: 'dir', icon: 'tool', path: '/tool', component: '', permission: '', sort: 5, visible: 1, status: 1, children: [
    { id: 401, name: '代码生成', type: 'menu', icon: 'command', path: '/tool/generator', component: 'tool/gen/index', permission: 'tool:gen:list', sort: 1, visible: 1, status: 1 },
  ]},
];

// —— 侧边栏导航（精简自菜单，仅本期实现的页面可点）——
const NAV = [
  { key: 'dashboard', label: '工作台', icon: 'dashboard', route: 'dashboard' },
  { group: '系统管理', items: [
    { key: 'user', label: '用户管理', icon: 'users', route: 'user' },
    { key: 'role', label: '角色管理', icon: 'role', route: 'role' },
    { key: 'menu', label: '菜单管理', icon: 'menu', route: 'menu' },
    { key: 'dept', label: '部门管理', icon: 'dept', route: 'dept' },
    { key: 'post', label: '岗位管理', icon: 'post', route: 'post' },
    { key: 'dict', label: '字典管理', icon: 'dict', route: 'dict' },
    { key: 'config', label: '参数配置', icon: 'gear', route: 'config' },
  ]},
  { group: '系统监控', items: [
    { key: 'operlog', label: '操作日志', icon: 'file', route: 'operlog' },
    { key: 'loginlog', label: '登录日志', icon: 'lock', route: 'loginlog' },
  ]},
  { group: '工作流程', items: [
    { key: 'model', label: '流程模型', icon: 'flow', route: 'model' },
    { key: 'def', label: '流程定义', icon: 'layers', route: 'def' },
    { key: 'todo', label: '我的待办', icon: 'bell', route: 'todo', badge: 6 },
    { key: 'done', label: '我的已办', icon: 'check', route: 'done' },
  ]},
  { group: '系统工具', items: [
    { key: 'gen', label: '代码生成', icon: 'command', route: 'gen' },
  ]},
];

// —— 仪表盘 ——
const DASH_STATS = [
  { key: 'users', label: '系统用户', value: 1284, delta: 4.2, up: true, icon: 'users', sub: '本月新增 52' },
  { key: 'online', label: '在线会话', value: 86, delta: 1.1, up: true, icon: 'eye', sub: '峰值 124' },
  { key: 'todo', label: '待办任务', value: 37, delta: 2.8, up: false, icon: 'bell', sub: '逾期 3' },
  { key: 'api', label: '今日接口调用', value: 92400, delta: 6.5, up: true, icon: 'trend', sub: '成功率 99.8%' },
];

const DASH_TREND = [42,48,40,55,60,52,68,72,64,78,82,76,88,95,90,102,98,110,118,112,126,120,134,128];

const DASH_TODO = [
  { id: 1, title: '采购申请审批 · 研发服务器扩容', from: '陈思远', type: '采购流程', time: '12 分钟前', level: 'warn' },
  { id: 2, title: '报销单审批 · 4 月差旅费用', from: '吴敏', type: '报销流程', time: '38 分钟前', level: 'info' },
  { id: 3, title: '请假申请 · 调休 2 天', from: '韩雪', type: '人事流程', time: '1 小时前', level: 'info' },
  { id: 4, title: '合同审批 · 云服务年度续约', from: '赵晴', type: '合同流程', time: '2 小时前', level: 'danger' },
  { id: 5, title: '用印申请 · 投标授权书', from: '孙浩', type: '行政流程', time: '3 小时前', level: 'info' },
];

const DASH_ACTIVITY = [
  { who: '周明远', action: '新增了角色', target: '审批专员', time: '09:41', tint: '#0066cc' },
  { who: '陈思远', action: '修改了用户', target: '吴敏 的部门', time: '08:55', tint: '#1f8a4c' },
  { who: '系统', action: '部署了流程定义', target: '采购流程 v3', time: '08:30', tint: '#7a5ae0' },
  { who: '韩雪', action: '导出了表格', target: '用户管理（48 条）', time: '昨天 19:22', tint: '#b06d00' },
  { who: '马玲', action: '重置了密码', target: 'user032', time: '昨天 16:48', tint: '#c4321f' },
];

// —— 字典管理 ——
const DICT_TYPES = [
  { id: 1, dictName: '用户性别', dictType: 'sys_user_sex', status: 1, createTime: '2024-01-08 09:12', remark: '用户性别列表' },
  { id: 2, dictName: '菜单状态', dictType: 'sys_show_hide', status: 1, createTime: '2024-01-08 09:14', remark: '菜单显示 / 隐藏' },
  { id: 3, dictName: '系统开关', dictType: 'sys_normal_disable', status: 1, createTime: '2024-01-09 10:20', remark: '系统正常 / 停用状态' },
  { id: 4, dictName: '任务状态', dictType: 'sys_job_status', status: 1, createTime: '2024-02-02 14:30', remark: '定时任务运行状态' },
  { id: 5, dictName: '审批结果', dictType: 'wf_approve_result', status: 1, createTime: '2024-03-11 16:05', remark: '工作流审批结果' },
  { id: 6, dictName: '业务类型', dictType: 'wf_business_type', status: 1, createTime: '2024-03-11 16:08', remark: '审批业务单据类型' },
  { id: 7, dictName: '通知类型', dictType: 'sys_notice_type', status: 0, createTime: '2024-04-20 11:42', remark: '通知公告类型' },
  { id: 8, dictName: '操作类型', dictType: 'sys_oper_type', status: 1, createTime: '2024-05-06 09:30', remark: '操作日志业务类型' },
];

const DICT_DATA = {
  sys_user_sex: [
    { id: 1, label: '男', value: '0', sort: 1, status: 1, tone: 'info', def: true },
    { id: 2, label: '女', value: '1', sort: 2, status: 1, tone: 'purple', def: false },
    { id: 3, label: '未知', value: '2', sort: 3, status: 1, tone: 'neutral', def: false },
  ],
  sys_show_hide: [
    { id: 4, label: '显示', value: '0', sort: 1, status: 1, tone: 'ok', def: true },
    { id: 5, label: '隐藏', value: '1', sort: 2, status: 1, tone: 'neutral', def: false },
  ],
  sys_normal_disable: [
    { id: 6, label: '正常', value: '0', sort: 1, status: 1, tone: 'ok', def: true },
    { id: 7, label: '停用', value: '1', sort: 2, status: 1, tone: 'danger', def: false },
  ],
  sys_job_status: [
    { id: 8, label: '运行中', value: '0', sort: 1, status: 1, tone: 'ok', def: true },
    { id: 9, label: '已暂停', value: '1', sort: 2, status: 1, tone: 'warn', def: false },
  ],
  wf_approve_result: [
    { id: 10, label: '通过', value: 'pass', sort: 1, status: 1, tone: 'ok', def: true },
    { id: 11, label: '驳回', value: 'reject', sort: 2, status: 1, tone: 'danger', def: false },
    { id: 12, label: '退回', value: 'return', sort: 3, status: 1, tone: 'warn', def: false },
    { id: 13, label: '撤回', value: 'revoke', sort: 4, status: 1, tone: 'neutral', def: false },
  ],
  wf_business_type: [
    { id: 14, label: '采购流程', value: 'purchase', sort: 1, status: 1, tone: 'info', def: false },
    { id: 15, label: '报销流程', value: 'expense', sort: 2, status: 1, tone: 'info', def: false },
    { id: 16, label: '人事流程', value: 'hr', sort: 3, status: 1, tone: 'info', def: false },
    { id: 17, label: '合同流程', value: 'contract', sort: 4, status: 1, tone: 'info', def: false },
  ],
  sys_notice_type: [
    { id: 18, label: '通知', value: '1', sort: 1, status: 1, tone: 'info', def: true },
    { id: 19, label: '公告', value: '2', sort: 2, status: 1, tone: 'purple', def: false },
  ],
  sys_oper_type: [
    { id: 20, label: '新增', value: '1', sort: 1, status: 1, tone: 'ok', def: false },
    { id: 21, label: '修改', value: '2', sort: 2, status: 1, tone: 'info', def: false },
    { id: 22, label: '删除', value: '3', sort: 3, status: 1, tone: 'danger', def: false },
    { id: 23, label: '导出', value: '4', sort: 4, status: 1, tone: 'warn', def: false },
  ],
};

// —— 参数配置 ——
const CONFIGS = [
  { id: 1, name: '主框架页-默认皮肤样式', key: 'sys.index.skinName', value: 'skin-blue', type: 'Y', builtin: true, createTime: '2024-01-08 09:12', remark: '蓝色 skin-blue、绿色 skin-green' },
  { id: 2, name: '用户管理-账号初始密码', key: 'sys.user.initPassword', value: 'Yy@123456', type: 'Y', builtin: true, createTime: '2024-01-08 09:12', remark: '初始化密码 Yy@123456' },
  { id: 3, name: '主框架页-侧边栏主题', key: 'sys.index.sideTheme', value: 'theme-dark', type: 'Y', builtin: true, createTime: '2024-01-08 09:13', remark: '深色 theme-dark、浅色 theme-light' },
  { id: 4, name: '账号自助-验证码开关', key: 'sys.account.captchaEnabled', value: 'true', type: 'Y', builtin: true, createTime: '2024-01-09 10:20', remark: '是否开启登录验证码' },
  { id: 5, name: '账号自助-是否开启注册', key: 'sys.account.registerUser', value: 'false', type: 'Y', builtin: false, createTime: '2024-02-02 14:30', remark: '是否开启用户注册功能' },
  { id: 6, name: '用户登录-黑名单列表', key: 'sys.login.blackIPList', value: '', type: 'N', builtin: false, createTime: '2024-02-14 10:33', remark: '设置登录 IP 黑名单，多个用 ; 分隔' },
  { id: 7, name: '文件上传-单文件大小上限', key: 'sys.file.maxSize', value: '20MB', type: 'N', builtin: false, createTime: '2024-03-02 14:20', remark: '单个上传文件大小上限' },
  { id: 8, name: '工作流-审批超时提醒小时', key: 'wf.task.remindHours', value: '24', type: 'N', builtin: false, createTime: '2024-03-19 16:45', remark: '待办任务超时提醒阈值' },
];

// —— 操作日志 ——
const OPER_MODULES = ['用户管理','角色管理','菜单管理','部门管理','字典管理','参数配置','代码生成','流程定义'];
const OPER_ACTIONS = [
  { label: '新增', tone: 'ok', method: 'POST' }, { label: '修改', tone: 'info', method: 'PUT' },
  { label: '删除', tone: 'danger', method: 'DELETE' }, { label: '导出', tone: 'warn', method: 'POST' },
  { label: '查询', tone: 'neutral', method: 'GET' },
];
const OPER_USERS = ['周明远','陈思远','吴敏','韩雪','赵晴','孙浩','马玲','冯婷'];
const OPER_IPS = ['192.168.1.','10.20.30.','172.16.5.'];
function genOperLogs() {
  const out = [];
  for (let i = 1; i <= 42; i++) {
    const act = OPER_ACTIONS[(i * 3) % OPER_ACTIONS.length];
    const mod = OPER_MODULES[i % OPER_MODULES.length];
    const ok = i % 11 !== 0;
    const u = OPER_USERS[i % OPER_USERS.length];
    out.push({
      id: 1000 + i,
      module: mod,
      action: act.label, tone: act.tone, method: act.method,
      url: '/system/' + ['user','role','menu','dept','dict','config'][i % 6] + (act.method === 'GET' ? '/list' : act.method === 'DELETE' ? '/' + (i + 3) : ''),
      operName: u,
      dept: DEPT_FLAT[i % DEPT_FLAT.length].name,
      ip: OPER_IPS[i % 3] + (20 + i % 200),
      location: ['北京市','上海市','深圳市','杭州市','成都市'][i % 5],
      cost: 8 + (i * 13) % 320,
      result: ok ? 1 : 0,
      errorMsg: ok ? '' : '请求参数校验失败：roleCode 已存在',
      params: '{"pageNum":1,"pageSize":10,"' + (act.method === 'GET' ? 'keyword":"' : 'id":"') + (i) + '"}',
      operTime: '2026-06-0' + (1 + i % 5) + ' ' + String(8 + i % 11).padStart(2,'0') + ':' + String(i * 2 % 60).padStart(2,'0') + ':' + String(i * 7 % 60).padStart(2,'0'),
    });
  }
  return out;
}
const OPER_LOGS = genOperLogs();

// —— 登录日志 ——
const BROWSERS = ['Chrome 125','Safari 17','Edge 124','Firefox 126'];
const OS_LIST = ['macOS 14','Windows 11','Windows 10','iOS 17'];
function genLoginLogs() {
  const out = [];
  const msgs = [
    { ok: 1, msg: '登录成功' }, { ok: 1, msg: '登录成功' }, { ok: 1, msg: '登录成功' },
    { ok: 0, msg: '验证码错误' }, { ok: 0, msg: '密码错误' }, { ok: 1, msg: '退出成功' },
  ];
  for (let i = 1; i <= 36; i++) {
    const m = msgs[(i * 2) % msgs.length];
    const u = ['admin','chensy','wumin','hanxue','zhaoq','sunhao','maling'][i % 7];
    out.push({
      id: 2000 + i,
      username: u,
      ip: OPER_IPS[i % 3] + (20 + i % 200),
      location: ['北京市','上海市','深圳市','杭州市','成都市','广州市'][i % 6],
      browser: BROWSERS[i % BROWSERS.length],
      os: OS_LIST[i % OS_LIST.length],
      status: m.ok,
      msg: m.msg,
      loginTime: '2026-06-0' + (1 + i % 5) + ' ' + String(8 + i % 11).padStart(2,'0') + ':' + String(i * 3 % 60).padStart(2,'0') + ':' + String(i * 5 % 60).padStart(2,'0'),
    });
  }
  return out;
}
const LOGIN_LOGS = genLoginLogs();

// —— 工作流：流程模型 ——
const WF_MODELS = [
  { id: 1, modelKey: 'purchase', modelName: '采购申请流程', category: '采购管理', version: 3, deployed: true, createTime: '2024-03-02 14:20', updateTime: '2026-05-18 10:30', desc: '物资 / 服务采购审批，部门负责人 → 财务 → 总经理' },
  { id: 2, modelKey: 'expense', modelName: '费用报销流程', category: '财务管理', version: 5, deployed: true, createTime: '2024-03-05 09:10', updateTime: '2026-04-22 16:45', desc: '差旅 / 日常费用报销，直属上级 → 财务复核' },
  { id: 3, modelKey: 'leave', modelName: '请假申请流程', category: '人事管理', version: 2, deployed: true, createTime: '2024-03-19 16:45', updateTime: '2026-03-30 11:08', desc: '员工请假，按天数决定审批层级' },
  { id: 4, modelKey: 'contract', modelName: '合同审批流程', category: '法务管理', version: 4, deployed: true, createTime: '2024-04-07 11:08', updateTime: '2026-05-01 09:30', desc: '合同会签，法务 → 财务 → 分管领导' },
  { id: 5, modelKey: 'seal', modelName: '用印申请流程', category: '行政管理', version: 1, deployed: false, createTime: '2024-05-21 08:50', updateTime: '2026-05-21 08:50', desc: '公章 / 合同章使用申请，草稿待部署' },
];

// —— 工作流：流程定义 ——
const WF_DEFS = [
  { id: 1, processKey: 'purchase', processName: '采购申请流程', category: '采购管理', version: 3, deployTime: '2026-05-18 10:30', suspended: false, nodes: 4 },
  { id: 2, processKey: 'purchase', processName: '采购申请流程', category: '采购管理', version: 2, deployTime: '2025-11-12 14:20', suspended: true, nodes: 3 },
  { id: 3, processKey: 'expense', processName: '费用报销流程', category: '财务管理', version: 5, deployTime: '2026-04-22 16:45', suspended: false, nodes: 3 },
  { id: 4, processKey: 'leave', processName: '请假申请流程', category: '人事管理', version: 2, deployTime: '2026-03-30 11:08', suspended: false, nodes: 3 },
  { id: 5, processKey: 'contract', processName: '合同审批流程', category: '法务管理', version: 4, deployTime: '2026-05-01 09:30', suspended: false, nodes: 4 },
];

// —— 工作流：我的待办 ——
const WF_TODO = [
  { id: 1, taskName: '部门负责人审批', procName: '采购申请流程', bizTitle: '研发服务器扩容采购 ¥186,000', bizType: '采购流程', starter: '陈思远', createTime: '2026-06-05 09:20', due: '2026-06-06 18:00', priority: 'high', node: 2, total: 4 },
  { id: 2, taskName: '财务复核', procName: '费用报销流程', bizTitle: '4 月差旅费用报销 ¥8,420', bizType: '报销流程', starter: '吴敏', createTime: '2026-06-05 08:40', due: '2026-06-07 12:00', priority: 'mid', node: 2, total: 3 },
  { id: 3, taskName: '直属上级审批', procName: '请假申请流程', bizTitle: '韩雪 调休申请 2 天', bizType: '人事流程', starter: '韩雪', createTime: '2026-06-04 17:10', due: '2026-06-06 17:10', priority: 'low', node: 1, total: 3 },
  { id: 4, taskName: '法务会签', procName: '合同审批流程', bizTitle: '云服务年度续约合同 ¥420,000', bizType: '合同流程', starter: '赵晴', createTime: '2026-06-04 15:30', due: '2026-06-05 18:00', priority: 'high', node: 1, total: 4 },
  { id: 5, taskName: '行政审批', procName: '用印申请流程', bizTitle: '投标授权书用印申请', bizType: '行政流程', starter: '孙浩', createTime: '2026-06-04 11:05', due: '2026-06-06 11:05', priority: 'mid', node: 1, total: 2 },
  { id: 6, taskName: '总经理审批', procName: '采购申请流程', bizTitle: '设计部 Mac 设备采购 ¥58,000', bizType: '采购流程', starter: '马玲', createTime: '2026-06-03 16:48', due: '2026-06-05 16:48', priority: 'mid', node: 3, total: 4 },
];

// —— 工作流：我的已办 ——
const WF_DONE = [
  { id: 101, taskName: '部门负责人审批', procName: '采购申请流程', bizTitle: '测试环境云主机采购 ¥32,000', starter: '韩雪', result: '通过', tone: 'ok', comment: '预算合理，同意采购。', approveTime: '2026-06-04 10:22' },
  { id: 102, taskName: '财务复核', procName: '费用报销流程', bizTitle: '3 月市场推广费用 ¥15,600', starter: '马玲', result: '驳回', tone: 'danger', comment: '发票信息不完整，请补充后重新提交。', approveTime: '2026-06-03 14:05' },
  { id: 103, taskName: '直属上级审批', procName: '请假申请流程', bizTitle: '孙浩 年假申请 3 天', starter: '孙浩', result: '通过', tone: 'ok', comment: '准假，注意工作交接。', approveTime: '2026-06-03 09:18' },
  { id: 104, taskName: '法务会签', procName: '合同审批流程', bizTitle: '办公租赁合同续签 ¥240,000', starter: '冯婷', result: '退回', tone: 'warn', comment: '退回发起人补充租赁面积条款。', approveTime: '2026-06-02 16:40' },
  { id: 105, taskName: '总经理审批', procName: '采购申请流程', bizTitle: '研发团队培训采购 ¥45,000', starter: '陈思远', result: '通过', tone: 'ok', comment: '同意，提升团队能力。', approveTime: '2026-06-02 11:30' },
];

// —— 审批流转记录（详情用）——
const WF_TRACE = [
  { node: '发起申请', who: '陈思远', time: '2026-06-05 09:18', result: '提交', tone: 'info', comment: '研发服务器资源不足，申请扩容。', done: true },
  { node: '部门负责人审批', who: '周明远', time: '当前节点', result: '处理中', tone: 'warn', comment: '', done: false, current: true },
  { node: '财务审核', who: '冯婷', time: '—', result: '待处理', tone: 'neutral', comment: '', done: false },
  { node: '总经理审批', who: '周明远', time: '—', result: '待处理', tone: 'neutral', comment: '', done: false },
];

// —— 代码生成 ——
const GEN_TABLES = [
  { id: 1, tableName: 'biz_order', tableComment: '业务订单表', className: 'BizOrder', module: 'biz', synced: true, createTime: '2026-05-28 10:12' },
  { id: 2, tableName: 'biz_customer', tableComment: '客户信息表', className: 'BizCustomer', module: 'biz', synced: true, createTime: '2026-05-28 10:15' },
  { id: 3, tableName: 'biz_product', tableComment: '产品资料表', className: 'BizProduct', module: 'biz', synced: false, createTime: '2026-05-29 14:30' },
  { id: 4, tableName: 'sys_notice', tableComment: '通知公告表', className: 'SysNotice', module: 'system', synced: true, createTime: '2026-05-30 09:08' },
  { id: 5, tableName: 'biz_contract', tableComment: '合同台账表', className: 'BizContract', module: 'biz', synced: false, createTime: '2026-06-01 16:45' },
  { id: 6, tableName: 'biz_inventory', tableComment: '库存流水表', className: 'BizInventory', module: 'biz', synced: true, createTime: '2026-06-02 11:20' },
];
const GEN_FIELDS = [
  { col: 'order_id', javaField: 'orderId', javaType: 'Long', jdbcType: 'BIGINT', comment: '订单 ID', insert: false, edit: false, list: true, query: false, queryType: '=', formType: '隐藏', required: false },
  { col: 'order_no', javaField: 'orderNo', javaType: 'String', jdbcType: 'VARCHAR', comment: '订单编号', insert: true, edit: true, list: true, query: true, queryType: 'LIKE', formType: '文本框', required: true },
  { col: 'customer_name', javaField: 'customerName', javaType: 'String', jdbcType: 'VARCHAR', comment: '客户名称', insert: true, edit: true, list: true, query: true, queryType: 'LIKE', formType: '文本框', required: true },
  { col: 'amount', javaField: 'amount', javaType: 'BigDecimal', jdbcType: 'DECIMAL', comment: '订单金额', insert: true, edit: true, list: true, query: false, queryType: '=', formType: '数字框', required: true },
  { col: 'status', javaField: 'status', javaType: 'String', jdbcType: 'CHAR', comment: '订单状态', insert: true, edit: true, list: true, query: true, queryType: '=', formType: '下拉框', required: true },
  { col: 'remark', javaField: 'remark', javaType: 'String', jdbcType: 'VARCHAR', comment: '备注', insert: true, edit: true, list: false, query: false, queryType: '=', formType: '文本域', required: false },
  { col: 'create_time', javaField: 'createTime', javaType: 'Date', jdbcType: 'DATETIME', comment: '创建时间', insert: false, edit: false, list: true, query: true, queryType: 'BETWEEN', formType: '日期框', required: false },
];

// —— 岗位管理（在岗人数由用户数据派生，保证与用户管理联动）——
const postCount = (name) => USERS.filter(u => u.postName === name).length;
const POSTS = [
  { id: 1, postCode: 'ceo', postName: '董事长', sort: 1, status: 1, userCount: postCount('董事长'), createTime: '2024-01-08 09:12', remark: '集团最高管理岗位' },
  { id: 2, postCode: 'gm', postName: '总经理', sort: 2, status: 1, userCount: postCount('总经理'), createTime: '2024-01-08 09:13', remark: '分管业务线总经理' },
  { id: 3, postCode: 'tech_dir', postName: '技术总监', sort: 3, status: 1, userCount: postCount('技术总监'), createTime: '2024-02-14 10:33', remark: '研发中心负责人' },
  { id: 4, postCode: 'pm', postName: '产品经理', sort: 4, status: 1, userCount: postCount('产品经理'), createTime: '2024-03-02 14:20', remark: '负责产品规划与设计' },
  { id: 5, postCode: 'arch', postName: '架构师', sort: 5, status: 1, userCount: postCount('架构师'), createTime: '2024-03-19 16:45', remark: '平台架构设计岗位' },
  { id: 6, postCode: 'dev', postName: '研发工程师', sort: 6, status: 1, userCount: postCount('研发工程师'), createTime: '2024-04-07 11:08', remark: '前后端研发岗位' },
  { id: 7, postCode: 'qa', postName: '测试工程师', sort: 7, status: 1, userCount: postCount('测试工程师'), createTime: '2024-04-22 15:40', remark: '质量保障岗位' },
  { id: 8, postCode: 'hr', postName: '人事专员', sort: 8, status: 1, userCount: postCount('人事专员'), createTime: '2024-05-11 09:18', remark: '人力资源岗位' },
  { id: 9, postCode: 'finance', postName: '财务专员', sort: 9, status: 0, userCount: postCount('财务专员'), createTime: '2024-05-21 08:50', remark: '财务核算岗位' },
];

Object.assign(window, {
  DEPTS, ROLES, USERS, MENUS, NAV, DEPT_FLAT, ROLE_NAMES, POST_NAMES, POSTS,
  DASH_STATS, DASH_TREND, DASH_TODO, DASH_ACTIVITY, AVATAR_TINTS,
  DICT_TYPES, DICT_DATA, CONFIGS, OPER_LOGS, LOGIN_LOGS,
  WF_MODELS, WF_DEFS, WF_TODO, WF_DONE, WF_TRACE, GEN_TABLES, GEN_FIELDS,
});
