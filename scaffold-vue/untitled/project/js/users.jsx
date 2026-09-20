/* ============================================================
   用户管理
   ============================================================ */
function UserForm({ initial, onSave, onClose }) {
  const [f, setF] = useState(initial || { username: '', nickname: '', phone: '', email: '', deptName: '平台架构部', postName: '研发工程师', roleName: '研发工程师', status: 1 });
  const [err, setErr] = useState({});
  const set = (k, v) => setF(s => ({ ...s, [k]: v }));
  const save = () => {
    const e = {};
    if (!f.username.trim()) e.username = '请输入登录账号';
    if (!f.nickname.trim()) e.nickname = '请输入用户昵称';
    if (f.phone && !/^[\d-]{7,}$/.test(f.phone)) e.phone = '手机号格式不正确';
    if (f.email && !/^\S+@\S+\.\S+$/.test(f.email)) e.email = '邮箱格式不正确';
    setErr(e);
    if (Object.keys(e).length) return;
    onSave(f);
  };
  return (
    <Modal title={initial ? '编辑用户' : '新增用户'} onClose={onClose} width={580}
      footer={<>
        <Button variant="quiet" onClick={onClose}>取消</Button>
        <Button variant="primary" onClick={save}>{initial ? '保存修改' : '确认新增'}</Button>
      </>}>
      <div className="form-grid">
        <Field label="登录账号" required error={err.username}>
          <Input value={f.username} invalid={err.username} disabled={!!initial}
            placeholder="字母 / 数字组合" onChange={(e) => set('username', e.target.value)} />
        </Field>
        <Field label="用户昵称" required error={err.nickname}>
          <Input value={f.nickname} invalid={err.nickname} placeholder="真实姓名" onChange={(e) => set('nickname', e.target.value)} />
        </Field>
        <Field label="手机号码" error={err.phone}>
          <Input value={f.phone} invalid={err.phone} placeholder="13800000000" onChange={(e) => set('phone', e.target.value)} />
        </Field>
        <Field label="电子邮箱" error={err.email}>
          <Input value={f.email} invalid={err.email} placeholder="name@company.com" onChange={(e) => set('email', e.target.value)} />
        </Field>
        <Field label="所属部门" required>
          <Select value={f.deptName} onChange={(v) => set('deptName', v)} options={DEPT_FLAT.map(d => d.name)} />
        </Field>
        <Field label="任职岗位" required>
          <Select value={f.postName} onChange={(v) => set('postName', v)} options={POST_NAMES} />
        </Field>
        <Field label="用户角色" required className="col-2">
          <Select value={f.roleName} onChange={(v) => set('roleName', v)} options={ROLE_NAMES} />
        </Field>
        <Field label="账号状态" className="col-2">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Switch checked={f.status === 1} onChange={(v) => set('status', v ? 1 : 0)} />
            <span style={{ fontSize: 13.5, color: 'var(--ink-72)' }}>{f.status === 1 ? '正常 · 允许登录系统' : '停用 · 禁止登录系统'}</span>
          </div>
        </Field>
      </div>
      {!initial && (
        <div style={{ marginTop: 18, padding: '11px 14px', background: 'var(--primary-soft)', borderRadius: 'var(--r-sm)', fontSize: 12.5, color: 'var(--primary)', display: 'flex', gap: 8, alignItems: 'center' }}>
          <Icon name="key" size={14} />新用户初始密码为 <b style={{ margin: '0 2px' }}>Yy@123456</b>，首次登录需修改。
        </div>
      )}
    </Modal>
  );
}

function UserCard({ u, onEdit, onMenu }) {
  return (
    <div className="ucard">
      <div className="ucard-top">
        <Avatar name={u.nickname} size={46} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="ucard-name">{u.nickname}{u.username === 'admin' && <Tag tone="purple">超管</Tag>}</div>
          <div className="ucard-user">@{u.username}</div>
        </div>
        <StatusTag status={u.status} />
      </div>
      <div className="ucard-rows">
        <div className="ucard-row"><Icon name="dept" size={14} /><span>{u.deptName}</span></div>
        <div className="ucard-row"><Icon name="post" size={14} /><span>{u.postName}</span></div>
        <div className="ucard-row"><Icon name="role" size={14} /><span>{u.roleName}</span></div>
        <div className="ucard-row"><Icon name="phone" size={14} /><span className="tnum">{u.phone}</span></div>
        <div className="ucard-row"><Icon name="mail" size={14} /><span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{u.email}</span></div>
      </div>
      <div className="ucard-foot">
        <span className="cell-muted" style={{ fontSize: 12 }}>登录 {u.lastLogin}</span>
        <div style={{ display: 'flex', gap: 2 }}>
          <IconBtn name="edit" size={16} tip="编辑" onClick={() => onEdit(u)} />
          <IconBtn name="key" size={16} tip="重置密码" onClick={() => onMenu('reset', u)} />
          <IconBtn name="trash" size={16} tip="删除" onClick={() => onMenu('delete', u)} />
        </div>
      </div>
    </div>
  );
}

function UsersPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(USERS);
  const [kw, setKw] = useState('');
  const [dept, setDept] = useState('');
  const [post, setPost] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [selected, setSelected] = useState([]);
  const [sort, setSort] = useState(null);
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState(null); // {type, user}
  const layout = tweaks.listLayout || 'table';

  const filtered = useMemo(() => {
    let r = list.filter(u =>
      (!kw || u.nickname.includes(kw) || u.username.includes(kw) || u.phone.includes(kw)) &&
      (!dept || u.deptName === dept) &&
      (!post || u.postName === post) &&
      (!role || u.roleName === role) &&
      (status === '' || u.status === Number(status)));
    if (sort) {
      r = [...r].sort((a, b) => {
        const av = a[sort.key], bv = b[sort.key];
        return (av > bv ? 1 : av < bv ? -1 : 0) * (sort.dir === 'asc' ? 1 : -1);
      });
    }
    return r;
  }, [list, kw, dept, post, role, status, sort]);

  const paged = filtered.slice((page - 1) * pageSize, page * pageSize);
  const reload = () => { setLoading(true); setTimeout(() => setLoading(false), 500); };
  const doSort = (k) => setSort(s => s && s.key === k ? (s.dir === 'asc' ? { key: k, dir: 'desc' } : null) : { key: k, dir: 'asc' });

  const save = (data) => {
    if (modal.user) {
      setList(l => l.map(u => u.id === modal.user.id ? { ...u, ...data } : u));
      toast('用户已更新', 'ok');
    } else {
      setList(l => [{ ...data, id: Math.max(...l.map(u => u.id)) + 1, createTime: '2026-06-05 10:00', lastLogin: '—' }, ...l]);
      toast('用户已新增', 'ok');
    }
    setModal(null);
  };
  const onMenu = (type, u) => setModal({ type, user: u });
  const doDelete = () => {
    if (modal.batch) { setList(l => l.filter(u => !selected.includes(u.id))); setSelected([]); toast(`已删除 ${selected.length} 个用户`, 'ok'); }
    else { setList(l => l.filter(u => u.id !== modal.user.id)); toast('用户已删除', 'ok'); }
    setModal(null);
  };
  const exportCsv = () => toast(`已导出 ${filtered.length} 条用户数据（含中文表头）`, 'ok');

  const columns = [
    { key: 'nickname', title: '用户', sortable: true, render: (u) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <Avatar name={u.nickname} size={36} />
        <div>
          <div className="cell-strong" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {u.nickname}{u.username === 'admin' && <Tag tone="purple">超管</Tag>}
          </div>
          <div className="cell-muted" style={{ fontSize: 12.5, marginTop: 1 }}>@{u.username}</div>
        </div>
      </div>
    )},
    { key: 'deptName', title: '部门', render: (u) => <span>{u.deptName}</span> },
    { key: 'postName', title: '岗位', render: (u) => <span className="cell-muted">{u.postName}</span> },
    { key: 'roleName', title: '角色', render: (u) => <Tag tone={u.roleName.includes('管理员') ? 'info' : 'neutral'}>{u.roleName}</Tag> },
    { key: 'phone', title: '手机号', render: (u) => <span className="tnum cell-muted">{u.phone}</span> },
    { key: 'status', title: '状态', width: 90, render: (u) => <StatusTag status={u.status} /> },
    { key: 'lastLogin', title: '最近登录', sortable: true, render: (u) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{u.lastLogin}</span> },
    { key: 'actions', title: '操作', width: 132, className: 'col-actions', render: (u) => (
      <div className="row-actions">
        <IconBtn name="edit" size={16} tip="编辑" onClick={(e) => { e.stopPropagation(); setModal({ type: 'edit', user: u }); }} />
        <IconBtn name="key" size={16} tip="重置密码" onClick={(e) => { e.stopPropagation(); onMenu('reset', u); }} />
        <IconBtn name="trash" size={16} tip="删除" onClick={(e) => { e.stopPropagation(); onMenu('delete', u); }} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统管理', '用户管理']} title="用户管理"
        desc="维护后台系统用户信息与用户角色关系，支持按部门、角色、状态筛选。"
        actions={<>
          <Button variant="quiet" icon="download" onClick={exportCsv}>导出</Button>
          <Button variant="primary" icon="plus" onClick={() => setModal({ type: 'add' })}>新增用户</Button>
        </>} />

      {/* 筛选 */}
      <Card style={{ marginBottom: 18, padding: '18px 22px' }}>
        <div className="filter-bar">
          <Field label="关键词" className="grow"><div style={{ width: 220 }}>
            <SearchInput value={kw} onChange={(v) => { setKw(v); setPage(1); }} placeholder="昵称 / 账号 / 手机号" round={false} />
          </div></Field>
          <Field label="部门"><Select value={dept} onChange={(v) => { setDept(v); setPage(1); }} width={150}
            options={[{ label: '全部部门', value: '' }, ...DEPT_FLAT.map(d => ({ label: d.name, value: d.name }))]} /></Field>
          <Field label="岗位"><Select value={post} onChange={(v) => { setPost(v); setPage(1); }} width={140}
            options={[{ label: '全部岗位', value: '' }, ...POST_NAMES.map(p => ({ label: p, value: p }))]} /></Field>
          <Field label="角色"><Select value={role} onChange={(v) => { setRole(v); setPage(1); }} width={140}
            options={[{ label: '全部角色', value: '' }, ...ROLE_NAMES.map(r => ({ label: r, value: r }))]} /></Field>
          <Field label="状态"><Select value={status} onChange={(v) => { setStatus(v); setPage(1); }} width={120}
            options={[{ label: '全部状态', value: '' }, { label: '正常', value: '1' }, { label: '停用', value: '0' }]} /></Field>
          <div style={{ display: 'flex', gap: 8, marginLeft: 'auto' }}>
            <Button variant="quiet" icon="refresh" onClick={() => { setKw(''); setDept(''); setPost(''); setRole(''); setStatus(''); setPage(1); reload(); }}>重置</Button>
          </div>
        </div>
      </Card>

      {/* 列表 */}
      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span className="panel-title">用户列表</span>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 个用户</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <IconBtn name="refresh" size={17} bordered tip="刷新" onClick={reload} />
            <IconBtn name="download" size={17} bordered tip="导出当前结果" onClick={exportCsv} />
          </div>
        </div>

        {selected.length > 0 && (
          <div className="selbar">
            已选择 <b>{selected.length}</b> 项
            <Button variant="text" size="sm" onClick={() => toast('批量启用成功', 'ok')}>批量启用</Button>
            <Button variant="text" size="sm" onClick={() => toast('批量停用成功', 'ok')}>批量停用</Button>
            <Button variant="text" size="sm" className="danger" onClick={() => setModal({ type: 'delete', batch: true })}>批量删除</Button>
            <div style={{ flex: 1 }} />
            <Button variant="text" size="sm" onClick={() => setSelected([])}>取消选择</Button>
          </div>
        )}

        {layout === 'cards' ? (
          loading ? <div style={{ padding: 40, textAlign: 'center', color: 'var(--ink-muted-48)' }}>加载中…</div> :
          paged.length === 0 ? <div className="tbl-empty"><div className="em-ico"><Icon name="search" size={24} /></div>暂无数据</div> :
          <div className="ucard-grid">
            {paged.map(u => <UserCard key={u.id} u={u} onEdit={(x) => setModal({ type: 'edit', user: x })} onMenu={onMenu} />)}
          </div>
        ) : (
          <Table columns={columns} data={paged} density={tweaks.density} selectable
            selected={selected} onSelectChange={setSelected} sort={sort} onSort={doSort} loading={loading} />
        )}

        <Pagination total={filtered.length} page={page} pageSize={pageSize}
          onPage={setPage} onPageSize={(s) => { setPageSize(s); setPage(1); }} />
      </Card>

      {/* 弹窗 */}
      {modal && (modal.type === 'add' || modal.type === 'edit') &&
        <UserForm initial={modal.user} onSave={save} onClose={() => setModal(null)} />}
      {modal && modal.type === 'reset' &&
        <ConfirmModal title="重置密码" message={<>确认将用户 <b>{modal.user.nickname}</b> 的密码重置为初始密码 <b>Yy@123456</b> 吗？</>}
          confirmText="确认重置" onConfirm={() => toast('密码已重置', 'ok')} onClose={() => setModal(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除用户" danger
          message={modal.batch ? <>确认删除选中的 <b>{selected.length}</b> 个用户吗？此操作不可恢复。</> : <>确认删除用户 <b>{modal.user.nickname}</b> 吗？此操作不可恢复。</>}
          confirmText="删除" onConfirm={doDelete} onClose={() => setModal(null)} />}
    </div>
  );
}

window.UsersPage = UsersPage;
