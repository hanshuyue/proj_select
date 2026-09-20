/* ============================================================
   菜单管理 —— 树形表格
   ============================================================ */
const MENU_TYPE_TAG = { dir: ['neutral', '目录'], menu: ['info', '菜单'], btn: ['purple', '按钮'] };

function MenuForm({ initial, parentName, onSave, onClose }) {
  const [f, setF] = useState(initial || { name: '', type: 'menu', icon: 'file', path: '', component: '', permission: '', sort: 1, visible: 1, status: 1 });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.name.trim()) e.name = '请输入菜单名称';
    if (f.type !== 'btn' && !f.path.trim()) e.path = '请输入路由路径';
    if (f.type === 'btn' && !f.permission.trim()) e.permission = '请输入权限标识';
    setErr(e); if (Object.keys(e).length) return; onSave(f);
  };
  const ICONS = ['dashboard', 'gear', 'users', 'role', 'menu', 'dept', 'dict', 'file', 'flow', 'grid', 'command', 'tool', 'bell', 'lock'];
  return (
    <Modal title={initial ? '编辑菜单' : '新增菜单'} onClose={onClose} width={580}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={save}>保存</Button></>}>
      {parentName && <div style={{ marginBottom: 18, fontSize: 13, color: 'var(--ink-muted-48)' }}>上级菜单：<span className="code-chip">{parentName}</span></div>}
      <Field label="菜单类型" className="col-2" >
        <div style={{ marginBottom: 4 }}>
          <Segmented value={f.type} onChange={(v) => set('type', v)} options={[{ label: '目录', value: 'dir' }, { label: '菜单', value: 'menu' }, { label: '按钮', value: 'btn' }]} />
        </div>
      </Field>
      <div className="form-grid" style={{ marginTop: 16 }}>
        <Field label="菜单名称" required error={err.name}><Input value={f.name} invalid={err.name} placeholder="如：用户管理" onChange={(e) => set('name', e.target.value)} /></Field>
        <Field label="显示排序"><Input type="number" value={f.sort} onChange={(e) => set('sort', Number(e.target.value))} /></Field>
        {f.type !== 'btn' && <>
          <Field label="路由路径" required error={err.path}><Input value={f.path} invalid={err.path} placeholder="/system/user" onChange={(e) => set('path', e.target.value)} /></Field>
          <Field label="图标">
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {ICONS.map(ic => (
                <span key={ic} onClick={() => set('icon', ic)} className="menu-ico-box"
                  style={{ cursor: 'pointer', width: 32, height: 32, background: f.icon === ic ? 'var(--primary)' : 'var(--parchment)', color: f.icon === ic ? '#fff' : 'var(--ink-72)' }}>
                  <Icon name={ic} size={16} />
                </span>
              ))}
            </div>
          </Field>
        </>}
        {f.type === 'menu' && <Field label="组件路径" className="col-2"><Input value={f.component} placeholder="system/user/index" onChange={(e) => set('component', e.target.value)} /></Field>}
        {f.type !== 'dir' && <Field label="权限标识" required={f.type === 'btn'} error={err.permission} className="col-2"><Input value={f.permission} invalid={err.permission} placeholder="system:user:list" onChange={(e) => set('permission', e.target.value)} /></Field>}
        <Field label="显示状态">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, height: 38 }}>
            <Switch checked={f.visible === 1} onChange={(v) => set('visible', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.visible ? '显示' : '隐藏'}</span>
          </div>
        </Field>
        <Field label="菜单状态">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, height: 38 }}>
            <Switch checked={f.status === 1} onChange={(v) => set('status', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.status ? '正常' : '停用'}</span>
          </div>
        </Field>
      </div>
    </Modal>
  );
}

function MenusPage({ tweaks }) {
  const toast = useToast();
  const [expanded, setExpanded] = useState([100, 200, 300, 400, 101, 102]);
  const [modal, setModal] = useState(null);
  const [visMap, setVisMap] = useState({});
  const toggle = (id) => setExpanded(e => e.includes(id) ? e.filter(x => x !== id) : [...e, id]);

  // 扁平化为可见行
  const rows = useMemo(() => {
    const out = [];
    const walk = (nodes, depth) => {
      nodes.forEach(n => {
        out.push({ node: n, depth });
        if (n.children && expanded.includes(n.id)) walk(n.children, depth + 1);
      });
    };
    walk(MENUS, 0);
    return out;
  }, [expanded]);

  const allIds = useMemo(() => { const a = []; const w = (ns) => ns.forEach(n => { if (n.children) { a.push(n.id); w(n.children); } }); w(MENUS); return a; }, []);

  return (
    <div>
      <PageHeader crumbs={['系统管理', '菜单管理']} title="菜单管理"
        desc="维护前端菜单、页面路由与按钮权限。支持目录、菜单、按钮三类节点的树形维护。"
        actions={<>
          <Button variant="quiet" icon="download" onClick={() => toast('已导出菜单（含层级字段）', 'ok')}>导出</Button>
          <Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add' })}>新增菜单</Button>
        </>} />

      <Card>
        <div className="panel-head">
          <span className="panel-title">菜单树</span>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button variant="quiet" size="sm" icon="arrowDown" onClick={() => setExpanded(allIds)}>展开全部</Button>
            <Button variant="quiet" size="sm" icon="arrowUp" onClick={() => setExpanded([])}>收起全部</Button>
          </div>
        </div>
        <div className="tablewrap">
          <table className={`tbl density-${tweaks.density}`}>
            <thead>
              <tr>
                <th style={{ minWidth: 240 }}>菜单名称</th>
                <th style={{ width: 80 }}>类型</th>
                <th style={{ width: 180 }}>路由 / 权限标识</th>
                <th style={{ width: 70 }}>排序</th>
                <th style={{ width: 80 }}>显示</th>
                <th style={{ width: 90 }}>状态</th>
                <th style={{ width: 150 }} className="col-actions">操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ node, depth }) => {
                const hasChild = node.children && node.children.length > 0;
                const open = expanded.includes(node.id);
                const [tone, label] = MENU_TYPE_TAG[node.type];
                const vis = visMap[node.id] !== undefined ? visMap[node.id] : node.visible === 1;
                return (
                  <tr key={node.id}>
                    <td>
                      <div className="menu-name-cell">
                        <span className="tree-indent" style={{ width: depth * 22 }} />
                        <span className={`tree-toggle ${hasChild ? '' : 'leaf'} ${open ? 'open' : ''}`} onClick={() => hasChild && toggle(node.id)}>
                          <Icon name="chevronRight" size={14} />
                        </span>
                        {node.type !== 'btn'
                          ? <span className="menu-ico-box"><Icon name={node.icon || 'file'} size={15} /></span>
                          : <span style={{ width: 28, display: 'inline-flex', justifyContent: 'center', color: 'var(--ink-muted-30)' }}><Icon name="dot" size={14} stroke={4} /></span>}
                        <span style={{ fontWeight: node.type === 'dir' ? 600 : 500 }}>{node.name}</span>
                      </div>
                    </td>
                    <td><Tag tone={tone}>{label}</Tag></td>
                    <td>{node.type === 'btn'
                      ? <span className="code-chip">{node.permission}</span>
                      : <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{node.path}</span>}</td>
                    <td><span className="tnum cell-muted">{node.sort}</span></td>
                    <td>{node.type !== 'btn'
                      ? <Switch checked={vis} onChange={(v) => { setVisMap(m => ({ ...m, [node.id]: v })); toast(v ? '已显示' : '已隐藏', 'info'); }} />
                      : <span className="cell-muted">—</span>}</td>
                    <td><StatusTag status={node.status} /></td>
                    <td className="col-actions">
                      <div className="row-actions">
                        {node.type !== 'btn' && <IconBtn name="plus" size={16} tip="新增子项" onClick={() => setModal({ type: 'add', parent: node })} />}
                        <IconBtn name="edit" size={16} tip="编辑" onClick={() => setModal({ type: 'edit', menu: node })} />
                        <IconBtn name="trash" size={16} tip="删除" onClick={() => setModal({ type: 'delete', menu: node })} />
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Card>

      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <MenuForm initial={modal.menu} parentName={modal.parent ? modal.parent.name : null}
          onSave={() => { toast(modal.type === 'add' ? '菜单已新增' : '菜单已保存', 'ok'); setModal(null); }} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除菜单" danger message={<>确认删除 <b>{modal.menu.name}</b> 吗？{modal.menu.children ? '其下级菜单将一并删除。' : ''}</>}
          confirmText="删除" onConfirm={() => toast('菜单已删除', 'ok')} onClose={() => setModal(null)} />}
    </div>
  );
}

window.MenusPage = MenusPage;
