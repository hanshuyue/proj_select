/* ============================================================
   系统监控 —— 操作日志 + 登录日志
   ============================================================ */
const METHOD_TONE = { GET: 'neutral', POST: 'ok', PUT: 'info', DELETE: 'danger' };

function OperLogDetail({ row, onClose }) {
  return (
    <Drawer title="操作日志详情" sub={`日志 ID #${row.id}`} width={500} onClose={onClose}
      footer={<Button variant="quiet" onClick={onClose}>关闭</Button>}>
      <div className="kv-grid" style={{ marginBottom: 18 }}>
        <div className="kv-cell"><div className="kv-label">操作模块</div><div className="kv-value">{row.module}</div></div>
        <div className="kv-cell"><div className="kv-label">操作类型</div><div className="kv-value"><Tag tone={row.tone}>{row.action}</Tag></div></div>
        <div className="kv-cell"><div className="kv-label">操作人员</div><div className="kv-value">{row.operName}</div></div>
        <div className="kv-cell"><div className="kv-label">所属部门</div><div className="kv-value">{row.dept}</div></div>
        <div className="kv-cell"><div className="kv-label">请求方式</div><div className="kv-value"><span className="code-chip">{row.method}</span></div></div>
        <div className="kv-cell"><div className="kv-label">操作结果</div><div className="kv-value">{row.result ? <Tag tone="ok" dot>成功</Tag> : <Tag tone="danger" dot>失败</Tag>}</div></div>
        <div className="kv-cell"><div className="kv-label">主机地址</div><div className="kv-value tnum">{row.ip}</div></div>
        <div className="kv-cell"><div className="kv-label">操作地点</div><div className="kv-value">{row.location}</div></div>
        <div className="kv-cell"><div className="kv-label">消耗时间</div><div className="kv-value tnum">{row.cost} ms</div></div>
        <div className="kv-cell"><div className="kv-label">操作时间</div><div className="kv-value tnum" style={{ fontSize: 13 }}>{row.operTime}</div></div>
      </div>
      <div className="log-field">
        <div className="log-field-label">请求地址</div>
        <pre className="log-pre">{row.method} {row.url}</pre>
      </div>
      <div className="log-field">
        <div className="log-field-label">请求参数</div>
        <pre className="log-pre">{row.params}</pre>
      </div>
      {!row.result && (
        <div className="log-field">
          <div className="log-field-label" style={{ color: 'var(--danger)' }}>异常信息</div>
          <pre className="log-pre danger">{row.errorMsg}</pre>
        </div>
      )}
    </Drawer>
  );
}

function OperLogPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(OPER_LOGS);
  const [kw, setKw] = useState('');
  const [mod, setMod] = useState('');
  const [result, setResult] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selected, setSelected] = useState([]);
  const [sort, setSort] = useState({ key: 'operTime', dir: 'desc' });
  const [detail, setDetail] = useState(null);
  const [modal, setModal] = useState(null);

  const filtered = useMemo(() => {
    let r = list.filter(o =>
      (!kw || o.operName.includes(kw) || o.url.includes(kw)) &&
      (!mod || o.module === mod) &&
      (result === '' || o.result === Number(result)));
    if (sort) r = [...r].sort((a, b) => (a[sort.key] > b[sort.key] ? 1 : -1) * (sort.dir === 'asc' ? 1 : -1));
    return r;
  }, [list, kw, mod, result, sort]);
  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);
  const doSort = (k) => setSort(s => s && s.key === k ? (s.dir === 'asc' ? { key: k, dir: 'desc' } : null) : { key: k, dir: 'asc' });

  const columns = [
    { key: 'module', title: '模块', width: 100, render: (o) => <span className="cell-strong">{o.module}</span> },
    { key: 'action', title: '类型', width: 80, render: (o) => <Tag tone={o.tone}>{o.action}</Tag> },
    { key: 'url', title: '请求', render: (o) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Tag tone={METHOD_TONE[o.method]}>{o.method}</Tag>
        <span className="tnum cell-muted" style={{ fontSize: 12.5, overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 200, whiteSpace: 'nowrap' }}>{o.url}</span>
      </div>
    )},
    { key: 'operName', title: '操作人', width: 100, render: (o) => <span>{o.operName}</span> },
    { key: 'ip', title: '主机 / 地点', render: (o) => (
      <div><div className="tnum" style={{ fontSize: 13 }}>{o.ip}</div><div className="cell-muted" style={{ fontSize: 12 }}>{o.location}</div></div>
    )},
    { key: 'cost', title: '耗时', width: 80, sortable: true, render: (o) => <span className="tnum cell-muted">{o.cost} ms</span> },
    { key: 'result', title: '结果', width: 80, render: (o) => o.result ? <Tag tone="ok" dot>成功</Tag> : <Tag tone="danger" dot>失败</Tag> },
    { key: 'operTime', title: '操作时间', width: 160, sortable: true, render: (o) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{o.operTime}</span> },
    { key: 'actions', title: '', width: 60, className: 'col-actions', render: (o) => (
      <div className="row-actions"><IconBtn name="eye" size={16} tip="详情" onClick={(e) => { e.stopPropagation(); setDetail(o); }} /></div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统监控', '操作日志']} title="操作日志"
        desc="记录后台关键操作，便于审计与问题排查。点击行查看请求参数与异常信息。"
        actions={<>
          <Button variant="quiet" icon="trash" onClick={() => setModal('clear')}>清空</Button>
          <Button variant="quiet" icon="download" onClick={() => toast(`已导出 ${filtered.length} 条操作日志`, 'ok')}>导出</Button>
        </>} />

      <Card style={{ marginBottom: 18, padding: '18px 22px' }}>
        <div className="filter-bar">
          <Field label="关键词" className="grow"><div style={{ width: 220 }}>
            <SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="操作人 / 请求地址" round={false} />
          </div></Field>
          <Field label="操作模块"><Select value={mod} onChange={(v) => { setMod(v); setPage(1); }} width={140}
            options={[{ label: '全部模块', value: '' }, ...OPER_MODULES.map(m => ({ label: m, value: m }))]} /></Field>
          <Field label="操作结果"><Select value={result} onChange={(v) => { setResult(v); setPage(1); }} width={120}
            options={[{ label: '全部', value: '' }, { label: '成功', value: '1' }, { label: '失败', value: '0' }]} /></Field>
          <div style={{ marginLeft: 'auto' }}>
            <Button variant="quiet" icon="refresh" onClick={() => { setKw(''); setMod(''); setResult(''); setPage(1); }}>重置</Button>
          </div>
        </div>
      </Card>

      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span className="panel-title">操作记录</span>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 条</span>
          </div>
          <IconBtn name="download" size={17} bordered tip="导出当前结果" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')} />
        </div>
        {selected.length > 0 && (
          <div className="selbar">
            已选择 <b>{selected.length}</b> 项
            <Button variant="text" size="sm" className="danger" onClick={() => { setList(l => l.filter(o => !selected.includes(o.id))); setSelected([]); toast('已删除所选日志', 'ok'); }}>批量删除</Button>
            <div style={{ flex: 1 }} />
            <Button variant="text" size="sm" onClick={() => setSelected([])}>取消选择</Button>
          </div>
        )}
        <Table columns={columns} data={paged} density={tweaks.density} selectable
          selected={selected} onSelectChange={setSelected} sort={sort} onSort={doSort} onRowClick={setDetail} />
        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>

      {detail && <OperLogDetail row={detail} onClose={() => setDetail(null)} />}
      {modal === 'clear' &&
        <ConfirmModal title="清空操作日志" danger message="确认清空全部操作日志吗？此操作不可恢复。"
          confirmText="清空" onConfirm={() => { setList([]); toast('操作日志已清空', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

function LoginLogPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(LOGIN_LOGS);
  const [kw, setKw] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selected, setSelected] = useState([]);
  const [sort, setSort] = useState({ key: 'loginTime', dir: 'desc' });
  const [modal, setModal] = useState(null);

  const filtered = useMemo(() => {
    let r = list.filter(o =>
      (!kw || o.username.includes(kw) || o.ip.includes(kw)) &&
      (status === '' || o.status === Number(status)));
    if (sort) r = [...r].sort((a, b) => (a[sort.key] > b[sort.key] ? 1 : -1) * (sort.dir === 'asc' ? 1 : -1));
    return r;
  }, [list, kw, status, sort]);
  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);
  const doSort = (k) => setSort(s => s && s.key === k ? (s.dir === 'asc' ? { key: k, dir: 'desc' } : null) : { key: k, dir: 'asc' });

  const columns = [
    { key: 'username', title: '登录账号', render: (o) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <Avatar name={o.username} size={32} />
        <span className="code-chip">{o.username}</span>
      </div>
    )},
    { key: 'ip', title: '登录 IP', render: (o) => <span className="tnum" style={{ fontSize: 13 }}>{o.ip}</span> },
    { key: 'location', title: '登录地点', width: 110, render: (o) => <span>{o.location}</span> },
    { key: 'browser', title: '浏览器 / 系统', render: (o) => (
      <div><div style={{ fontSize: 13 }}>{o.browser}</div><div className="cell-muted" style={{ fontSize: 12 }}>{o.os}</div></div>
    )},
    { key: 'status', title: '状态', width: 90, render: (o) => o.status ? <Tag tone="ok" dot>成功</Tag> : <Tag tone="danger" dot>失败</Tag> },
    { key: 'msg', title: '描述', width: 110, render: (o) => <span className="cell-muted" style={{ fontSize: 13 }}>{o.msg}</span> },
    { key: 'loginTime', title: '登录时间', width: 160, sortable: true, render: (o) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{o.loginTime}</span> },
  ];

  return (
    <div>
      <PageHeader crumbs={['系统监控', '登录日志']} title="登录日志"
        desc="记录用户登录与退出行为，含登录 IP、地点、终端环境与结果。"
        actions={<>
          <Button variant="quiet" icon="trash" onClick={() => setModal('clear')}>清空</Button>
          <Button variant="quiet" icon="download" onClick={() => toast(`已导出 ${filtered.length} 条登录日志`, 'ok')}>导出</Button>
        </>} />

      <Card style={{ marginBottom: 18, padding: '18px 22px' }}>
        <div className="filter-bar">
          <Field label="关键词" className="grow"><div style={{ width: 240 }}>
            <SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="登录账号 / IP" round={false} />
          </div></Field>
          <Field label="登录状态"><Select value={status} onChange={(v) => { setStatus(v); setPage(1); }} width={140}
            options={[{ label: '全部状态', value: '' }, { label: '成功', value: '1' }, { label: '失败', value: '0' }]} /></Field>
          <div style={{ marginLeft: 'auto' }}>
            <Button variant="quiet" icon="refresh" onClick={() => { setKw(''); setStatus(''); setPage(1); }}>重置</Button>
          </div>
        </div>
      </Card>

      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span className="panel-title">登录记录</span>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 条</span>
          </div>
          <IconBtn name="download" size={17} bordered tip="导出当前结果" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')} />
        </div>
        {selected.length > 0 && (
          <div className="selbar">
            已选择 <b>{selected.length}</b> 项
            <Button variant="text" size="sm" className="danger" onClick={() => { setList(l => l.filter(o => !selected.includes(o.id))); setSelected([]); toast('已删除所选日志', 'ok'); }}>批量删除</Button>
            <div style={{ flex: 1 }} />
            <Button variant="text" size="sm" onClick={() => setSelected([])}>取消选择</Button>
          </div>
        )}
        <Table columns={columns} data={paged} density={tweaks.density} selectable
          selected={selected} onSelectChange={setSelected} sort={sort} onSort={doSort} />
        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>

      {modal === 'clear' &&
        <ConfirmModal title="清空登录日志" danger message="确认清空全部登录日志吗？此操作不可恢复。"
          confirmText="清空" onConfirm={() => { setList([]); toast('登录日志已清空', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

Object.assign(window, { OperLogPage, LoginLogPage });
