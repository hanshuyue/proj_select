/* ============================================================
   角色管理 —— 列表 + 权限分配抽屉 + 数据权限
   ============================================================ */
const SCOPES = [
  { key: 'all', name: '全部数据权限', desc: '可查看系统内所有部门的数据' },
  { key: 'dept_below', name: '本部门及以下数据权限', desc: '可查看本部门及其所有下级部门数据' },
  { key: 'dept', name: '本部门数据权限', desc: '仅可查看本部门数据' },
  { key: 'self', name: '仅本人数据权限', desc: '仅可查看本人创建的数据' },
  { key: 'custom', name: '自定义数据权限', desc: '手动指定可查看的部门范围' },
];

function flattenMenuIds(nodes, acc = []) {
  nodes.forEach(n => { acc.push(n.id); if (n.children) flattenMenuIds(n.children, acc); });
  return acc;
}

function PermNode({ node, depth, checked, onToggle }) {
  const [open, setOpen] = useState(depth < 1);
  const hasChild = node.children && node.children.length > 0;
  const btns = (node.children || []).filter(c => c.type === 'btn');
  const subs = (node.children || []).filter(c => c.type !== 'btn');
  const isOn = checked.includes(node.id);
  return (
    <div className="perm-node">
      <div className="perm-row" style={{ paddingLeft: 8 + depth * 18 }}>
        <span className={`tree-toggle ${subs.length ? '' : 'leaf'} ${open ? 'open' : ''}`}
          onClick={() => setOpen(o => !o)}><Icon name="chevronRight" size={13} /></span>
        <Checkbox checked={isOn} onChange={() => onToggle(node.id)} />
        {node.icon ? <Icon name={node.icon} size={15} style={{ color: 'var(--ink-muted-48)' }} /> : <span style={{ width: 15 }} />}
        <span style={{ fontSize: 13.5, fontWeight: node.type === 'dir' ? 600 : 450 }}>{node.name}</span>
        {node.type === 'dir' && <Tag tone="neutral">目录</Tag>}
      </div>
      {btns.length > 0 && open && (
        <div className="perm-btns">
          {btns.map(b => (
            <span key={b.id} className={`perm-btn-chip ${checked.includes(b.id) ? 'on' : ''}`} onClick={() => onToggle(b.id)}>
              {checked.includes(b.id) && <Icon name="check" size={12} stroke={2.6} />}{b.name}
            </span>
          ))}
        </div>
      )}
      {subs.length > 0 && open && subs.map(c => (
        <PermNode key={c.id} node={c} depth={depth + 1} checked={checked} onToggle={onToggle} />
      ))}
    </div>
  );
}

function PermDrawer({ role, onClose, onSave }) {
  const toast = useToast();
  const [tab, setTab] = useState('menu');
  const [checked, setChecked] = useState(role.id <= 2 ? flattenMenuIds(MENUS) : [1, 100, 101, 1011, 1012, 102, 104]);
  const [scope, setScope] = useState(role.id === 1 ? 'all' : role.id === 2 ? 'dept_below' : 'dept');
  const allIds = useMemo(() => flattenMenuIds(MENUS), []);
  const toggle = (id) => setChecked(c => c.includes(id) ? c.filter(x => x !== id) : [...c, id]);

  return (
    <Drawer title={`分配权限 · ${role.name}`} sub={`角色编码 ${role.code}`} width={520} onClose={onClose}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={() => { onSave(); toast('权限已保存', 'ok'); }}>保存权限</Button></>}>
      <div style={{ marginBottom: 18 }}>
        <Segmented value={tab} onChange={setTab} options={[{ label: '菜单与按钮', value: 'menu', icon: 'menu' }, { label: '数据权限', value: 'scope', icon: 'shield' }]} />
      </div>

      {tab === 'menu' ? (
        <div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
            <span style={{ fontSize: 13, color: 'var(--ink-muted-48)' }}>已选 <b style={{ color: 'var(--primary)' }}>{checked.length}</b> / {allIds.length} 项</span>
            <div style={{ display: 'flex', gap: 6 }}>
              <Button variant="text" size="sm" onClick={() => setChecked(allIds)}>全选</Button>
              <Button variant="text" size="sm" onClick={() => setChecked([])}>清空</Button>
            </div>
          </div>
          <div className="perm-tree">
            {MENUS.map(n => <PermNode key={n.id} node={n} depth={0} checked={checked} onToggle={toggle} />)}
          </div>
        </div>
      ) : (
        <div>
          <div style={{ fontSize: 13, color: 'var(--ink-muted-48)', marginBottom: 14 }}>选择该角色在业务列表中可查看的数据范围：</div>
          <div className="scope-list">
            {SCOPES.map(s => (
              <div key={s.key} className={`scope-opt ${scope === s.key ? 'on' : ''}`} onClick={() => setScope(s.key)}>
                <span className="scope-radio" />
                <div><div className="scope-name">{s.name}</div><div className="scope-desc">{s.desc}</div></div>
              </div>
            ))}
          </div>
          {scope === 'custom' && (
            <div style={{ marginTop: 14, padding: 16, border: '1px solid var(--hairline)', borderRadius: 'var(--r-md)' }}>
              <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>指定可见部门</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {DEPT_FLAT.map(d => <span key={d.id} className="perm-btn-chip">{d.name}</span>)}
              </div>
            </div>
          )}
        </div>
      )}
    </Drawer>
  );
}

function RoleForm({ initial, onSave, onClose }) {
  const [f, setF] = useState(initial || { name: '', code: '', sort: 9, status: 1, remark: '' });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.name.trim()) e.name = '请输入角色名称';
    if (!f.code.trim()) e.code = '请输入角色编码';
    setErr(e); if (Object.keys(e).length) return; onSave(f);
  };
  return (
    <Modal title={initial ? '编辑角色' : '新增角色'} onClose={onClose}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={save}>保存</Button></>}>
      <div className="form-grid">
        <Field label="角色名称" required error={err.name}><Input value={f.name} invalid={err.name} placeholder="如：业务管理员" onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="角色编码" required error={err.code}><Input value={f.code} invalid={err.code} placeholder="如：biz_admin" onChange={(e) => set('code', e.target.value)} /></Field>
        <Field label="显示排序"><Input type="number" value={f.sort} onChange={(e) => set('sort', Number(e.target.value))} /></Field>
        <Field label="角色状态">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, height: 38 }}>
            <Switch checked={f.status === 1} onChange={(v) => set('status', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.status === 1 ? '正常' : '停用'}</span>
          </div>
        </Field>
        <Field label="备注" className="col-2"><textarea className="input" value={f.remark} placeholder="角色职责说明" onChange={(e) => set('remark', e.target.value)} /></Field>
      </div>
    </Modal>
  );
}

function RolesPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(ROLES);
  const [kw, setKw] = useState('');
  const [modal, setModal] = useState(null);
  const [drawer, setDrawer] = useState(null);
  const filtered = list.filter(r => !kw || r.name.includes(kw) || r.code.includes(kw));

  const columns = [
    { key: 'name', title: '角色', render: (r) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <span className="menu-ico-box" style={{ background: r.builtin ? '#f0ecfb' : 'var(--primary-soft)', color: r.builtin ? '#6b46d9' : 'var(--primary)' }}><Icon name="role" size={16} /></span>
        <div>
          <div className="cell-strong" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>{r.name}{r.builtin && <Tag tone="purple">内置</Tag>}</div>
          <div style={{ marginTop: 2 }}><span className="code-chip">{r.code}</span></div>
        </div>
      </div>
    )},
    { key: 'dataScope', title: '数据权限', render: (r) => <Tag tone="info">{r.dataScope}</Tag> },
    { key: 'userCount', title: '用户数', align: 'left', render: (r) => <span className="tnum cell-strong">{r.userCount}</span> },
    { key: 'createTime', title: '创建时间', render: (r) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{r.createTime}</span> },
    { key: 'status', title: '状态', width: 90, render: (r) => <StatusTag status={r.status} /> },
    { key: 'actions', title: '操作', width: 170, className: 'col-actions', render: (r) => (
      <div className="row-actions">
        <Button variant="text" size="sm" icon="shield" onClick={() => setDrawer(r)} disabled={r.builtin && r.id === 1}>权限</Button>
        <IconBtn name="edit" size={16} tip="编辑" onClick={() => setModal({ type: 'edit', role: r })} />
        <IconBtn name="trash" size={16} tip="删除" onClick={() => setModal({ type: 'delete', role: r })} disabled={r.builtin} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统管理', '角色管理']} title="角色管理"
        desc="维护角色基础信息、菜单权限、按钮权限与数据权限范围。"
        actions={<Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add' })}>新增角色</Button>} />

      <Card>
        <div className="panel-head">
          <div style={{ width: 260 }}><SearchInput value={kw} onChange={setKw} placeholder="搜索角色名称 / 编码" /></div>
          <div style={{ display: 'flex', gap: 8 }}>
            <IconBtn name="refresh" size={17} bordered tip="刷新" onClick={() => toast('已刷新', 'info')} />
            <IconBtn name="download" size={17} bordered tip="导出" onClick={() => toast(`已导出 ${filtered.length} 条角色数据`, 'ok')} />
          </div>
        </div>
        <Table columns={columns} data={filtered} density={tweaks.density} />
        <Pagination total={filtered.length} page={1} pageSize={10} onPage={() => {}} onPageSize={() => {}} />
      </Card>

      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <RoleForm initial={modal.role} onSave={() => { toast(modal.type === 'add' ? '角色已新增' : '角色已保存', 'ok'); setModal(null); }} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除角色" danger message={<>确认删除角色 <b>{modal.role.name}</b> 吗？该角色下 {modal.role.userCount} 名用户将失去对应权限。</>}
          confirmText="删除" onConfirm={() => { setList(l => l.filter(r => r.id !== modal.role.id)); toast('角色已删除', 'ok'); }} onClose={() => setModal(null)} />}
      {drawer && <PermDrawer role={drawer} onClose={() => setDrawer(null)} onSave={() => setDrawer(null)} />}
    </div>
  );
}

window.RolesPage = RolesPage;
