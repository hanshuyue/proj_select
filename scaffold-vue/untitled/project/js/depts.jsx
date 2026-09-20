/* ============================================================
   部门管理 —— 左树 + 右详情
   ============================================================ */
function DeptTreeNode({ node, depth, activeId, onSelect, expanded, toggle }) {
  const hasChild = node.children && node.children.length > 0;
  const open = expanded.includes(node.id);
  return (
    <div>
      <div className={`tree-node-row ${activeId === node.id ? 'active' : ''}`}
        style={{ paddingLeft: 8 + depth * 18 }} onClick={() => onSelect(node)}>
        <span className={`tree-caret ${hasChild ? '' : 'leaf'} ${open ? 'open' : ''}`}
          onClick={(e) => { e.stopPropagation(); toggle(node.id); }}>
          <Icon name="chevronRight" size={14} />
        </span>
        <Icon name={hasChild ? 'folder' : 'dept'} size={16} style={{ color: activeId === node.id ? 'var(--primary)' : 'var(--ink-muted-48)' }} />
        <span className="tree-label" style={{ flex: 1 }}>{node.name}</span>
        {!node.status && <Tag tone="neutral">停用</Tag>}
      </div>
      {hasChild && open && node.children.map(c => (
        <DeptTreeNode key={c.id} node={c} depth={depth + 1} activeId={activeId} onSelect={onSelect} expanded={expanded} toggle={toggle} />
      ))}
    </div>
  );
}

function DeptForm({ initial, parentName, onSave, onClose }) {
  const [f, setF] = useState(initial || { name: '', leader: '', phone: '', email: '', sort: 1, status: 1 });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.name.trim()) e.name = '请输入部门名称';
    setErr(e); if (Object.keys(e).length) return;
    onSave(f);
  };
  return (
    <Modal title={initial ? '编辑部门' : '新增部门'} onClose={onClose}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={save}>保存</Button></>}>
      {parentName && <div style={{ marginBottom: 18, fontSize: 13, color: 'var(--ink-muted-48)' }}>上级部门：<span className="code-chip">{parentName}</span></div>}
      <div className="form-grid">
        <Field label="部门名称" required error={err.name} className="col-2">
          <Input value={f.name} invalid={err.name} placeholder="如：平台架构部" onChange={(e) => set('name', e.target.value)} />
        </Field>
        <Field label="负责人"><Input value={f.leader} placeholder="姓名" onChange={(e) => set('leader', e.target.value)} /></Field>
        <Field label="联系电话"><Input value={f.phone} placeholder="010-0000-0000" onChange={(e) => set('phone', e.target.value)} /></Field>
        <Field label="邮箱"><Input value={f.email} placeholder="dept@company.com" onChange={(e) => set('email', e.target.value)} /></Field>
        <Field label="显示排序"><Input type="number" value={f.sort} onChange={(e) => set('sort', Number(e.target.value))} /></Field>
        <Field label="部门状态" className="col-2">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Switch checked={f.status === 1} onChange={(v) => set('status', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.status === 1 ? '正常' : '停用'}</span>
          </div>
        </Field>
      </div>
    </Modal>
  );
}

function DeptsPage() {
  const toast = useToast();
  const [active, setActive] = useState(DEPTS[0]);
  const [expanded, setExpanded] = useState([1, 2, 3, 4]);
  const [modal, setModal] = useState(null);
  const toggle = (id) => setExpanded(e => e.includes(id) ? e.filter(x => x !== id) : [...e, id]);
  const expandAll = () => setExpanded([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11]);
  const collapseAll = () => setExpanded([1]);

  const memberCount = active ? (active.children ? active.children.length * 4 + 6 : 8) : 0;

  return (
    <div>
      <PageHeader crumbs={['系统管理', '部门管理']} title="部门管理"
        desc="维护组织架构，为用户归属与数据权限提供基础。点击左侧部门查看详情。"
        actions={<Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add', parent: active })}>新增部门</Button>} />

      <div className="tree-layout">
        {/* 左：树 */}
        <Card className="dept-tree-card">
          <div className="tree-toolbar">
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink-72)', flex: 1, paddingLeft: 4 }}>组织架构</span>
            <IconBtn name="arrowDown" size={15} tip="展开全部" onClick={expandAll} />
            <IconBtn name="arrowUp" size={15} tip="收起全部" onClick={collapseAll} />
          </div>
          {DEPTS.map(n => (
            <DeptTreeNode key={n.id} node={n} depth={0} activeId={active && active.id} onSelect={setActive} expanded={expanded} toggle={toggle} />
          ))}
        </Card>

        {/* 右：详情 */}
        {active ? (
          <Card>
            <div className="panel-head">
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span className="menu-ico-box" style={{ width: 36, height: 36, background: 'var(--primary-soft)', color: 'var(--primary)' }}><Icon name="dept" size={19} /></span>
                <div>
                  <div className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>{active.name}<StatusTag status={active.status} /></div>
                  <div className="panel-sub">部门 ID #{active.id} · {memberCount} 名成员</div>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button variant="quiet" size="sm" icon="plus" onClick={() => setModal({ type: 'add', parent: active })}>子部门</Button>
                <Button variant="quiet" size="sm" icon="edit" onClick={() => setModal({ type: 'edit', dept: active })}>编辑</Button>
                {active.id !== 1 && <IconBtn name="trash" size={17} bordered tip="删除" onClick={() => setModal({ type: 'delete', dept: active })} />}
              </div>
            </div>
            <div style={{ padding: 22 }}>
              <div className="kv-grid">
                <div className="kv-cell"><div className="kv-label">部门名称</div><div className="kv-value">{active.name}</div></div>
                <div className="kv-cell"><div className="kv-label">负责人</div><div className="kv-value">{active.leader || '—'}</div></div>
                <div className="kv-cell"><div className="kv-label">联系电话</div><div className="kv-value tnum">{active.phone || '—'}</div></div>
                <div className="kv-cell"><div className="kv-label">邮箱</div><div className="kv-value">{active.email || '—'}</div></div>
                <div className="kv-cell"><div className="kv-label">显示排序</div><div className="kv-value tnum">{active.sort}</div></div>
                <div className="kv-cell"><div className="kv-label">子部门数</div><div className="kv-value tnum">{active.children ? active.children.length : 0}</div></div>
              </div>

              {active.children && active.children.length > 0 && (
                <div style={{ marginTop: 22 }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--ink-72)', marginBottom: 12 }}>下级部门</div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))', gap: 10 }}>
                    {active.children.map(c => (
                      <div key={c.id} className="ucard-row" style={{ padding: '11px 14px', border: '1px solid var(--hairline)', borderRadius: 'var(--r-sm)', cursor: 'pointer' }}
                        onClick={() => { setActive(c); if (!expanded.includes(active.id)) toggle(active.id); }}>
                        <Icon name="dept" size={15} />
                        <span style={{ flex: 1, fontWeight: 500, color: 'var(--ink)' }}>{c.name}</span>
                        <Icon name="chevronRight" size={14} />
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </Card>
        ) : (
          <Card><div className="dept-detail-empty"><div className="em-ico"><Icon name="dept" size={28} /></div>请选择左侧部门查看详情</div></Card>
        )}
      </div>

      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <DeptForm initial={modal.dept} parentName={modal.type === 'add' && modal.parent ? modal.parent.name : null}
          onSave={() => { toast(modal.type === 'add' ? '部门已新增' : '部门已保存', 'ok'); setModal(null); }} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除部门" danger message={<>确认删除部门 <b>{modal.dept.name}</b> 吗？其下级部门与关联用户需先行处理。</>}
          confirmText="删除" onConfirm={() => { toast('部门已删除', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

window.DeptsPage = DeptsPage;
