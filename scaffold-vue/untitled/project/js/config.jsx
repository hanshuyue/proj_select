/* ============================================================
   参数配置 —— 系统参数维护
   ============================================================ */
function ConfigForm({ initial, onSave, onClose }) {
  const [f, setF] = useState(initial || { name: '', key: '', value: '', type: 'N', remark: '' });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.name.trim()) e.name = '请输入参数名称';
    if (!f.key.trim()) e.key = '请输入参数键名';
    setErr(e); if (Object.keys(e).length) return; onSave(f);
  };
  return (
    <Modal title={initial ? '编辑参数' : '新增参数'} onClose={onClose} width={560}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" onClick={save}>保存</Button></>}>
      <div className="form-grid">
        <Field label="参数名称" required error={err.name} className="col-2">
          <Input value={f.name} invalid={err.name} placeholder="如：账号初始密码" onChange={(e) => set('name', e.target.value)} />
        </Field>
        <Field label="参数键名" required error={err.key} className="col-2">
          <Input value={f.key} invalid={err.key} disabled={initial && initial.builtin} placeholder="如：sys.user.initPassword" onChange={(e) => set('key', e.target.value)} />
        </Field>
        <Field label="参数键值" className="col-2">
          <Input value={f.value} placeholder="参数值" onChange={(e) => set('value', e.target.value)} />
        </Field>
        <Field label="系统内置" className="col-2">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Switch checked={f.type === 'Y'} onChange={(v) => set('type', v ? 'Y' : 'N')} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.type === 'Y' ? '是 · 内置参数不可删除' : '否 · 业务参数'}</span>
          </div>
        </Field>
        <Field label="备注" className="col-2"><textarea className="input" value={f.remark} placeholder="参数用途说明" onChange={(e) => set('remark', e.target.value)} /></Field>
      </div>
    </Modal>
  );
}

function ConfigPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(CONFIGS);
  const [kw, setKw] = useState('');
  const [type, setType] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modal, setModal] = useState(null);

  const filtered = list.filter(c =>
    (!kw || c.name.includes(kw) || c.key.includes(kw)) &&
    (type === '' || c.type === type));
  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);

  const save = (data) => {
    if (modal.type === 'edit') { setList(l => l.map(c => c.id === modal.row.id ? { ...c, ...data, builtin: data.type === 'Y' } : c)); toast('参数已保存', 'ok'); }
    else { setList(l => [{ ...data, id: Math.max(...l.map(c => c.id)) + 1, builtin: data.type === 'Y', createTime: '2026-06-05 10:00' }, ...l]); toast('参数已新增', 'ok'); }
    setModal(null);
  };

  const columns = [
    { key: 'name', title: '参数名称', render: (c) => (
      <div>
        <div className="cell-strong">{c.name}</div>
        <div style={{ marginTop: 3 }}><span className="code-chip">{c.key}</span></div>
      </div>
    )},
    { key: 'value', title: '参数键值', render: (c) => c.value ? <span className="tnum" style={{ fontSize: 13 }}>{c.value}</span> : <span className="cell-muted">（空）</span> },
    { key: 'type', title: '系统内置', width: 110, render: (c) => c.type === 'Y' ? <Tag tone="purple">内置</Tag> : <Tag tone="neutral">自定义</Tag> },
    { key: 'remark', title: '备注', render: (c) => <span className="cell-muted" style={{ fontSize: 13 }}>{c.remark || '—'}</span> },
    { key: 'createTime', title: '创建时间', render: (c) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{c.createTime}</span> },
    { key: 'actions', title: '操作', width: 100, className: 'col-actions', render: (c) => (
      <div className="row-actions">
        <IconBtn name="edit" size={16} tip="编辑" onClick={() => setModal({ type: 'edit', row: c })} />
        <IconBtn name="trash" size={16} tip="删除" onClick={() => setModal({ type: 'delete', row: c })} disabled={c.builtin} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统管理', '参数配置']} title="参数配置"
        desc="维护系统运行参数，内置参数由系统初始化，不可删除。"
        actions={<>
          <Button variant="quiet" icon="refresh" onClick={() => toast('缓存已刷新', 'ok')}>刷新缓存</Button>
          <Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add' })}>新增参数</Button>
        </>} />

      <Card style={{ marginBottom: 18, padding: '18px 22px' }}>
        <div className="filter-bar">
          <Field label="关键词" className="grow"><div style={{ width: 240 }}>
            <SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="参数名称 / 键名" round={false} />
          </div></Field>
          <Field label="参数类型"><Select value={type} onChange={(v) => { setType(v); setPage(1); }} width={150}
            options={[{ label: '全部类型', value: '' }, { label: '系统内置', value: 'Y' }, { label: '自定义', value: 'N' }]} /></Field>
          <div style={{ marginLeft: 'auto' }}>
            <Button variant="quiet" icon="refresh" onClick={() => { setKw(''); setType(''); setPage(1); }}>重置</Button>
          </div>
        </div>
      </Card>

      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span className="panel-title">参数列表</span>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 项</span>
          </div>
          <IconBtn name="download" size={17} bordered tip="导出当前结果" onClick={() => toast(`已导出 ${filtered.length} 条参数`, 'ok')} />
        </div>
        <Table columns={columns} data={paged} density={tweaks.density} />
        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>

      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <ConfigForm initial={modal.row} onSave={save} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除参数" danger message={<>确认删除参数 <b>{modal.row.name}</b> 吗？</>}
          confirmText="删除" onConfirm={() => { setList(l => l.filter(c => c.id !== modal.row.id)); toast('参数已删除', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

window.ConfigPage = ConfigPage;
