/* ============================================================
   工作流程 —— 流程模型 / 流程定义 / 我的待办 / 我的已办
   ============================================================ */
const PRIORITY = { high: { label: '紧急', tone: 'danger' }, mid: { label: '普通', tone: 'info' }, low: { label: '较低', tone: 'neutral' } };

/* ---------- 流程节点条（简易流程图） ---------- */
function FlowSteps({ current, total }) {
  const names = ['发起', '部门审批', '财务审核', '总经理', '归档'].slice(0, total + 1);
  return (
    <div className="flow-steps">
      {names.map((n, i) => (
        <React.Fragment key={i}>
          <div className={`flow-step ${i < current ? 'done' : i === current ? 'current' : ''}`}>
            <span className="flow-dot">{i < current ? <Icon name="check" size={12} stroke={3} /> : i + 1}</span>
            <span className="flow-step-label">{n}</span>
          </div>
          {i < names.length - 1 && <span className={`flow-line ${i < current ? 'done' : ''}`} />}
        </React.Fragment>
      ))}
    </div>
  );
}

/* ---------- 流程模型 ---------- */
function WfModelPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(WF_MODELS);
  const [kw, setKw] = useState('');
  const [modal, setModal] = useState(null);
  const filtered = list.filter(m => !kw || m.modelName.includes(kw) || m.modelKey.includes(kw));

  const columns = [
    { key: 'modelName', title: '流程模型', render: (m) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <span className="menu-ico-box" style={{ width: 34, height: 34, background: 'var(--primary-soft)', color: 'var(--primary)' }}><Icon name="flow" size={17} /></span>
        <div>
          <div className="cell-strong">{m.modelName}</div>
          <div style={{ marginTop: 2 }}><span className="code-chip">{m.modelKey}</span></div>
        </div>
      </div>
    )},
    { key: 'category', title: '分类', render: (m) => <Tag tone="neutral">{m.category}</Tag> },
    { key: 'version', title: '版本', width: 80, render: (m) => <span className="code-chip">v{m.version}</span> },
    { key: 'deployed', title: '状态', width: 110, render: (m) => m.deployed ? <Tag tone="ok" dot>已部署</Tag> : <Tag tone="warn" dot>草稿</Tag> },
    { key: 'updateTime', title: '最后更新', width: 160, render: (m) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{m.updateTime}</span> },
    { key: 'actions', title: '操作', width: 200, className: 'col-actions', render: (m) => (
      <div className="row-actions">
        <Button variant="text" size="sm" icon="edit" onClick={() => toast('打开流程设计器（演示）', 'info')}>设计</Button>
        <Button variant="text" size="sm" icon="play" onClick={() => setModal({ type: 'deploy', row: m })}>部署</Button>
        <IconBtn name="trash" size={16} tip="删除" onClick={() => setModal({ type: 'delete', row: m })} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['工作流程', '流程模型']} title="流程模型"
        desc="通过在线设计器维护 BPMN 流程模型，保存后可部署为可发起的流程定义。"
        actions={<Button variant="primary" icon="plus" onClick={() => toast('新建流程模型（演示）', 'info')}>新建模型</Button>} />

      <Card>
        <div className="panel-head">
          <div style={{ width: 280 }}><SearchInput value={kw} onChange={setKw} placeholder="搜索流程名称 / 标识" /></div>
          <IconBtn name="download" size={17} bordered tip="导出" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')} />
        </div>
        <Table columns={columns} data={filtered} density={tweaks.density} />
      </Card>

      {modal && modal.type === 'deploy' &&
        <ConfirmModal title="部署流程模型" message={<>确认将模型 <b>{modal.row.modelName}</b> 部署为流程定义 v{modal.row.version + (modal.row.deployed ? 1 : 0)} 吗？</>}
          confirmText="确认部署" onConfirm={() => { setList(l => l.map(m => m.id === modal.row.id ? { ...m, deployed: true } : m)); toast('流程已部署', 'ok'); }} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除流程模型" danger message={<>确认删除流程模型 <b>{modal.row.modelName}</b> 吗？</>}
          confirmText="删除" onConfirm={() => { setList(l => l.filter(m => m.id !== modal.row.id)); toast('模型已删除', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

/* ---------- 流程定义 ---------- */
function WfDefPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(WF_DEFS);
  const [kw, setKw] = useState('');
  const [drawer, setDrawer] = useState(null);
  const filtered = list.filter(d => !kw || d.processName.includes(kw) || d.processKey.includes(kw));

  const columns = [
    { key: 'processName', title: '流程定义', render: (d) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <span className="menu-ico-box" style={{ width: 34, height: 34, background: d.suspended ? 'var(--parchment)' : 'var(--primary-soft)', color: d.suspended ? 'var(--ink-muted-48)' : 'var(--primary)' }}><Icon name="layers" size={17} /></span>
        <div>
          <div className="cell-strong">{d.processName}</div>
          <div style={{ marginTop: 2 }}><span className="code-chip">{d.processKey}</span></div>
        </div>
      </div>
    )},
    { key: 'category', title: '分类', render: (d) => <Tag tone="neutral">{d.category}</Tag> },
    { key: 'version', title: '版本', width: 80, render: (d) => <span className="code-chip">v{d.version}</span> },
    { key: 'nodes', title: '节点数', width: 80, render: (d) => <span className="tnum cell-muted">{d.nodes}</span> },
    { key: 'suspended', title: '状态', width: 110, render: (d) => d.suspended ? <Tag tone="neutral" dot>已挂起</Tag> : <Tag tone="ok" dot>激活</Tag> },
    { key: 'deployTime', title: '部署时间', width: 160, render: (d) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{d.deployTime}</span> },
    { key: 'actions', title: '操作', width: 200, className: 'col-actions', render: (d) => (
      <div className="row-actions">
        <Button variant="text" size="sm" icon="eye" onClick={() => setDrawer(d)}>流程图</Button>
        {d.suspended
          ? <Button variant="text" size="sm" icon="play" onClick={() => { setList(l => l.map(x => x.id === d.id ? { ...x, suspended: false } : x)); toast('流程已激活', 'ok'); }}>激活</Button>
          : <Button variant="text" size="sm" icon="pause" onClick={() => { setList(l => l.map(x => x.id === d.id ? { ...x, suspended: true } : x)); toast('流程已挂起', 'ok'); }}>挂起</Button>}
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['工作流程', '流程定义']} title="流程定义"
        desc="管理已部署的流程定义，支持挂起 / 激活与流程图查看。挂起后不能发起新流程。"
        actions={<Button variant="quiet" icon="download" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')}>导出</Button>} />

      <Card>
        <div className="panel-head">
          <div style={{ width: 280 }}><SearchInput value={kw} onChange={setKw} placeholder="搜索流程名称 / 标识" /></div>
          <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 条定义</span>
        </div>
        <Table columns={columns} data={filtered} density={tweaks.density} />
      </Card>

      {drawer && (
        <Drawer title={`流程图 · ${drawer.processName}`} sub={`${drawer.processKey} · v${drawer.version}`} width={560} onClose={() => setDrawer(null)}
          footer={<Button variant="quiet" onClick={() => setDrawer(null)}>关闭</Button>}>
          <div className="flow-diagram">
            <FlowSteps current={drawer.nodes} total={drawer.nodes} />
          </div>
          <div style={{ marginTop: 20, fontSize: 13, color: 'var(--ink-muted-48)', lineHeight: 1.6 }}>
            该流程共 {drawer.nodes} 个审批节点，部署于 {drawer.deployTime}。BPMN 图形由 Flowable 渲染，此处为节点流转示意。
          </div>
        </Drawer>
      )}
    </div>
  );
}

/* ---------- 审批详情抽屉 ---------- */
function ApproveDrawer({ task, onClose, onSubmit }) {
  const toast = useToast();
  const [action, setAction] = useState('pass');
  const [comment, setComment] = useState('');
  const submit = () => {
    if (action !== 'pass' && !comment.trim()) { toast('请填写审批意见', 'danger'); return; }
    const map = { pass: '已通过', reject: '已驳回', return: '已退回', revoke: '已撤回' };
    onSubmit(task, map[action]);
  };
  return (
    <Drawer title="审批处理" sub={task.procName} width={560} onClose={onClose}
      footer={<>
        <Button variant="quiet" onClick={onClose}>取消</Button>
        <Button variant={action === 'pass' ? 'primary' : 'danger'} icon="send" onClick={submit}>提交审批</Button>
      </>}>
      {/* 业务信息 */}
      <div className="approve-biz">
        <div className="approve-biz-title">{task.bizTitle}</div>
        <div className="approve-biz-meta">
          <span><Icon name="user" size={13} />发起人 {task.starter}</span>
          <span><Icon name="clock" size={13} />{task.createTime}</span>
          <Tag tone={PRIORITY[task.priority].tone}>{PRIORITY[task.priority].label}</Tag>
        </div>
      </div>

      {/* 流转进度 */}
      <div className="approve-sec-label">流转进度</div>
      <div className="flow-diagram" style={{ marginBottom: 20 }}>
        <FlowSteps current={task.node} total={task.total} />
      </div>

      {/* 流转记录 */}
      <div className="approve-sec-label">审批记录</div>
      <div className="trace-list">
        {WF_TRACE.map((t, i) => (
          <div key={i} className={`trace-item ${t.current ? 'current' : ''}`}>
            <span className={`trace-node ${t.done ? 'done' : t.current ? 'current' : ''}`}>
              {t.done ? <Icon name="check" size={12} stroke={3} /> : <span className="trace-num">{i + 1}</span>}
            </span>
            <div className="trace-body">
              <div className="trace-head">
                <span className="trace-name">{t.node}</span>
                <Tag tone={t.tone}>{t.result}</Tag>
              </div>
              <div className="trace-meta">{t.who} · {t.time}</div>
              {t.comment && <div className="trace-comment">{t.comment}</div>}
            </div>
          </div>
        ))}
      </div>

      {/* 审批操作 */}
      <div className="approve-sec-label">我的处理</div>
      <Segmented value={action} onChange={setAction} options={[
        { label: '通过', value: 'pass', icon: 'check' },
        { label: '驳回', value: 'reject', icon: 'x' },
        { label: '退回', value: 'return', icon: 'undo' },
        { label: '撤回', value: 'revoke', icon: 'refresh' },
      ]} />
      <textarea className="input" style={{ marginTop: 12, minHeight: 90 }}
        placeholder={action === 'pass' ? '审批意见（选填）' : '请填写审批意见（必填）'}
        value={comment} onChange={(e) => setComment(e.target.value)} />
    </Drawer>
  );
}

/* ---------- 我的待办 ---------- */
function WfTodoPage() {
  const toast = useToast();
  const [list, setList] = useState(WF_TODO);
  const [kw, setKw] = useState('');
  const [drawer, setDrawer] = useState(null);
  const filtered = list.filter(t => !kw || t.procName.includes(kw) || t.starter.includes(kw) || t.bizTitle.includes(kw));

  const submit = (task, resultLabel) => {
    setList(l => l.filter(t => t.id !== task.id));
    setDrawer(null);
    toast(`「${task.procName}」${resultLabel}`, 'ok');
  };

  return (
    <div>
      <PageHeader crumbs={['工作流程', '我的待办']} title="我的待办"
        desc="展示需要您处理的审批任务，点击任务进入审批详情，可通过、驳回、退回或撤回。"
        actions={<div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="tag tag-warn" style={{ height: 28, fontSize: 13 }}>{list.length} 项待处理</span>
        </div>} />

      <Card style={{ marginBottom: 18, padding: '16px 22px' }}>
        <div style={{ width: 320 }}><SearchInput value={kw} onChange={setKw} placeholder="搜索流程 / 发起人 / 单据" /></div>
      </Card>

      {filtered.length === 0 ? (
        <Card><div className="tbl-empty"><div className="em-ico"><Icon name="check" size={24} /></div>太棒了，没有待处理的任务</div></Card>
      ) : (
        <div className="todo-grid">
          {filtered.map(t => (
            <div key={t.id} className="todo-card" onClick={() => setDrawer(t)}>
              <div className="todo-card-top">
                <Tag tone="neutral">{t.bizType}</Tag>
                <Tag tone={PRIORITY[t.priority].tone}>{PRIORITY[t.priority].label}</Tag>
              </div>
              <div className="todo-card-title">{t.bizTitle}</div>
              <div className="todo-card-task"><Icon name="flow" size={14} />{t.procName} · {t.taskName}</div>
              <div className="todo-progress"><FlowSteps current={t.node} total={t.total} /></div>
              <div className="todo-card-foot">
                <span className="todo-starter"><Avatar name={t.starter} size={24} />{t.starter}</span>
                <span className="todo-due"><Icon name="clock" size={13} />截止 {t.due.slice(5)}</span>
              </div>
              <div className="todo-card-actions">
                <Button variant="primary" size="sm" icon="check" onClick={(e) => { e.stopPropagation(); setDrawer(t); }}>审批处理</Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {drawer && <ApproveDrawer task={drawer} onClose={() => setDrawer(null)} onSubmit={submit} />}
    </div>
  );
}

/* ---------- 我的已办 ---------- */
function WfDonePage({ tweaks }) {
  const toast = useToast();
  const [kw, setKw] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const filtered = WF_DONE.filter(d => !kw || d.procName.includes(kw) || d.starter.includes(kw) || d.bizTitle.includes(kw));
  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);

  const columns = [
    { key: 'bizTitle', title: '业务单据', render: (d) => (
      <div>
        <div className="cell-strong">{d.bizTitle}</div>
        <div className="cell-muted" style={{ fontSize: 12.5, marginTop: 2, display: 'flex', alignItems: 'center', gap: 6 }}>
          <Icon name="flow" size={13} />{d.procName} · {d.taskName}
        </div>
      </div>
    )},
    { key: 'starter', title: '发起人', width: 120, render: (d) => (
      <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Avatar name={d.starter} size={28} />{d.starter}</span>
    )},
    { key: 'result', title: '审批结果', width: 100, render: (d) => <Tag tone={d.tone} dot>{d.result}</Tag> },
    { key: 'comment', title: '审批意见', render: (d) => <span className="cell-muted" style={{ fontSize: 13 }}>{d.comment}</span> },
    { key: 'approveTime', title: '处理时间', width: 160, render: (d) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{d.approveTime}</span> },
  ];

  return (
    <div>
      <PageHeader crumbs={['工作流程', '我的已办']} title="我的已办"
        desc="展示您已处理过的审批任务，含审批结果与审批意见。"
        actions={<Button variant="quiet" icon="download" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')}>导出</Button>} />

      <Card>
        <div className="panel-head">
          <div style={{ width: 320 }}><SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="搜索流程 / 发起人 / 单据" /></div>
          <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 条已办</span>
        </div>
        <Table columns={columns} data={paged} density={tweaks.density} />
        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>
    </div>
  );
}

Object.assign(window, { WfModelPage, WfDefPage, WfTodoPage, WfDonePage });
