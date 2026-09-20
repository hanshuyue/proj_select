/* ============================================================
   岗位管理 —— 岗位基础信息维护
   ============================================================ */
function PostForm({ initial, onSave, onClose }) {
  const [f, setF] = useState(initial || { postName: '', postCode: '', sort: 9, status: 1, remark: '' });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.postName.trim()) e.postName = '请输入岗位名称';
    if (!f.postCode.trim()) e.postCode = '请输入岗位编码';
    else if (!/^[a-z][a-z0-9_]*$/.test(f.postCode)) e.postCode = '仅限小写字母、数字与下划线';
    setErr(e); if (Object.keys(e).length) return; onSave(f);
  };
  return (
    <Modal title={initial ? '编辑岗位' : '新增岗位'} onClose={onClose}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={save}>保存</Button></>}>
      <div className="form-grid">
        <Field label="岗位名称" required error={err.postName}>
          <Input value={f.postName} invalid={err.postName} placeholder="如：研发工程师" onChange={(e) => set('postName', e.target.value)} />
        </Field>
        <Field label="岗位编码" required error={err.postCode}>
          <Input value={f.postCode} invalid={err.postCode} disabled={!!initial} placeholder="如：dev" onChange={(e) => set('postCode', e.target.value)} />
        </Field>
        <Field label="显示排序"><Input type="number" value={f.sort} onChange={(e) => set('sort', Number(e.target.value))} /></Field>
        <Field label="岗位状态">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, height: 38 }}>
            <Switch checked={f.status === 1} onChange={(v) => set('status', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.status === 1 ? '正常' : '停用'}</span>
          </div>
        </Field>
        <Field label="备注" className="col-2"><textarea className="input" value={f.remark} placeholder="岗位职责说明" onChange={(e) => set('remark', e.target.value)} /></Field>
      </div>
    </Modal>
  );
}

function PostPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(POSTS);
  const [kw, setKw] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selected, setSelected] = useState([]);
  const [sort, setSort] = useState(null);
  const [modal, setModal] = useState(null);
  const [holders, setHolders] = useState(null); // 查看在岗成员

  const filtered = useMemo(() => {
    let r = list.filter(p =>
      (!kw || p.postName.includes(kw) || p.postCode.includes(kw)) &&
      (status === '' || p.status === Number(status)));
    if (sort) r = [...r].sort((a, b) => (a[sort.key] > b[sort.key] ? 1 : -1) * (sort.dir === 'asc' ? 1 : -1));
    return r;
  }, [list, kw, status, sort]);
  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);
  const doSort = (k) => setSort(s => s && s.key === k ? (s.dir === 'asc' ? { key: k, dir: 'desc' } : null) : { key: k, dir: 'asc' });

  const save = (data) => {
    if (modal.row) { setList(l => l.map(p => p.id === modal.row.id ? { ...p, ...data } : p)); toast('岗位已保存', 'ok'); }
    else { setList(l => [{ ...data, id: Math.max(...l.map(p => p.id)) + 1, userCount: 0, createTime: '2026-06-05 10:00' }, ...l]); toast('岗位已新增', 'ok'); }
    setModal(null);
  };
  const doDelete = () => {
    if (modal.batch) { setList(l => l.filter(p => !selected.includes(p.id))); setSelected([]); toast(`已删除 ${selected.length} 个岗位`, 'ok'); }
    else { setList(l => l.filter(p => p.id !== modal.row.id)); toast('岗位已删除', 'ok'); }
    setModal(null);
  };

  const columns = [
    { key: 'postName', title: '岗位', sortable: true, render: (p) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <span className="menu-ico-box" style={{ width: 34, height: 34, background: 'var(--primary-soft)', color: 'var(--primary)' }}><Icon name="post" size={16} /></span>
        <div>
          <div className="cell-strong">{p.postName}</div>
          <div style={{ marginTop: 2 }}><span className="code-chip">{p.postCode}</span></div>
        </div>
      </div>
    )},
    { key: 'userCount', title: '在岗人数', width: 110, sortable: true, render: (p) => (
      p.userCount > 0
        ? <button className="linkcount" onClick={(e) => { e.stopPropagation(); setHolders(p); }} title="查看在岗成员">
            <span className="tnum">{p.userCount}</span><Icon name="users" size={13} />
          </button>
        : <span className="tnum cell-muted">0</span>
    )},
    { key: 'sort', title: '排序', width: 80, sortable: true, render: (p) => <span className="tnum cell-muted">{p.sort}</span> },
    { key: 'remark', title: '备注', render: (p) => <span className="cell-muted" style={{ fontSize: 13 }}>{p.remark || '—'}</span> },
    { key: 'status', title: '状态', width: 90, render: (p) => <StatusTag status={p.status} /> },
    { key: 'createTime', title: '创建时间', width: 160, render: (p) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{p.createTime}</span> },
    { key: 'actions', title: '操作', width: 100, className: 'col-actions', render: (p) => (
      <div className="row-actions">
        <IconBtn name="edit" size={16} tip="编辑" onClick={(e) => { e.stopPropagation(); setModal({ type: 'edit', row: p }); }} />
        <IconBtn name="trash" size={16} tip="删除" onClick={(e) => { e.stopPropagation(); setModal({ type: 'delete', row: p }); }} disabled={p.userCount > 0} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统管理', '岗位管理']} title="岗位管理"
        desc="维护组织岗位信息，作为用户任职与组织建模的基础。仍有在岗人员的岗位不可删除。"
        actions={<>
          <Button variant="quiet" icon="download" onClick={() => toast(`已导出 ${filtered.length} 条岗位数据`, 'ok')}>导出</Button>
          <Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add' })}>新增岗位</Button>
        </>} />

      <Card style={{ marginBottom: 18, padding: '18px 22px' }}>
        <div className="filter-bar">
          <Field label="关键词" className="grow"><div style={{ width: 240 }}>
            <SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="岗位名称 / 编码" round={false} />
          </div></Field>
          <Field label="状态"><Select value={status} onChange={(v) => { setStatus(v); setPage(1); }} width={140}
            options={[{ label: '全部状态', value: '' }, { label: '正常', value: '1' }, { label: '停用', value: '0' }]} /></Field>
          <div style={{ marginLeft: 'auto' }}>
            <Button variant="quiet" icon="refresh" onClick={() => { setKw(''); setStatus(''); setPage(1); }}>重置</Button>
          </div>
        </div>
      </Card>

      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span className="panel-title">岗位列表</span>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 个岗位</span>
          </div>
          <IconBtn name="download" size={17} bordered tip="导出当前结果" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')} />
        </div>
        {selected.length > 0 && (
          <div className="selbar">
            已选择 <b>{selected.length}</b> 项
            <Button variant="text" size="sm" className="danger" onClick={() => setModal({ type: 'delete', batch: true })}>批量删除</Button>
            <div style={{ flex: 1 }} />
            <Button variant="text" size="sm" onClick={() => setSelected([])}>取消选择</Button>
          </div>
        )}
        <Table columns={columns} data={paged} density={tweaks.density} selectable
          selected={selected} onSelectChange={setSelected} sort={sort} onSort={doSort} />
        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>

      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <PostForm initial={modal.row} onSave={save} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除岗位" danger
          message={modal.batch ? <>确认删除选中的 <b>{selected.length}</b> 个岗位吗？此操作不可恢复。</> : <>确认删除岗位 <b>{modal.row.postName}</b> 吗？此操作不可恢复。</>}
          confirmText="删除" onConfirm={doDelete} onClose={() => setModal(null)} />}

      {holders && (
        <Drawer title={`在岗成员 · ${holders.postName}`} sub={`共 ${holders.userCount} 人 · 数据来自用户管理`} width={460} onClose={() => setHolders(null)}
          footer={<Button variant="quiet" onClick={() => setHolders(null)}>关闭</Button>}>
          <div className="holder-list">
            {USERS.filter(u => u.postName === holders.postName).map(u => (
              <div key={u.id} className="holder-row">
                <Avatar name={u.nickname} size={38} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="cell-strong" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>{u.nickname}<span className="code-chip">@{u.username}</span></div>
                  <div className="cell-muted" style={{ fontSize: 12.5, marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
                    <Icon name="dept" size={12} />{u.deptName} · {u.roleName}
                  </div>
                </div>
                <StatusTag status={u.status} />
              </div>
            ))}
          </div>
          <div style={{ marginTop: 16, padding: '11px 14px', background: 'var(--primary-soft)', borderRadius: 'var(--r-sm)', fontSize: 12.5, color: 'var(--primary)', display: 'flex', gap: 8, alignItems: 'center' }}>
            <Icon name="users" size={14} />岗位与用户联动：在岗人数根据用户的任职岗位自动统计。
          </div>
        </Drawer>
      )}
    </div>
  );
}

window.PostPage = PostPage;
