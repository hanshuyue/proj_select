/* ============================================================
   布局外壳 —— 深色侧边栏 + 顶栏 + 路由
   侧边栏风格 / 选中样式由 tweaks 驱动
   ============================================================ */

/* ---------- 侧边栏 ---------- */
function Sidebar({ route, onNav, collapsed, tweaks, onToast }) {
  const light = tweaks.sidebarTone === 'light';
  const bg = tweaks.sidebarTone === 'black' ? '#000'
           : tweaks.sidebarTone === 'light' ? '#ffffff' : 'var(--side-bg)';
  const inkColor = light ? 'var(--ink)' : 'var(--side-ink)';
  const muted = light ? 'var(--ink-muted-48)' : 'var(--side-ink-muted)';
  const hairline = light ? 'var(--hairline)' : 'var(--side-hairline)';

  const NavItem = ({ item }) => {
    const active = route === item.route;
    const disabled = !item.route;
    // 选中样式
    let rowStyle = {};
    let txtColor = active ? inkColor : muted;
    let icoColor = active ? (light ? 'var(--primary)' : '#fff') : muted;
    if (active) {
      if (tweaks.navActive === 'pill') {
        rowStyle = { background: 'var(--primary)' };
        txtColor = '#fff'; icoColor = '#fff';
      } else if (tweaks.navActive === 'bar') {
        rowStyle = { background: light ? 'var(--primary-soft)' : 'var(--side-active)',
          boxShadow: 'inset 3px 0 0 var(--primary)' };
        txtColor = light ? 'var(--primary)' : '#fff';
        icoColor = light ? 'var(--primary)' : 'var(--primary-on-dark)';
      } else { // tint
        rowStyle = { background: light ? 'var(--primary-soft)' : 'var(--side-active)' };
        txtColor = light ? 'var(--primary)' : '#fff';
        icoColor = light ? 'var(--primary)' : 'var(--primary-on-dark)';
      }
    }
    return (
      <button
        className="nav-item"
        style={{ color: txtColor, ...rowStyle, justifyContent: collapsed ? 'center' : 'flex-start' }}
        onClick={() => disabled ? onToast('「' + item.label + '」本期暂未开放', 'info') : onNav(item.route)}
        title={collapsed ? item.label : ''}>
        <Icon name={item.icon} size={19} style={{ color: icoColor }} />
        {!collapsed && <span className="nav-label">{item.label}</span>}
        {!collapsed && item.badge && <span className="nav-badge">{item.badge}</span>}
        {!collapsed && disabled && <span className="nav-soon">·</span>}
      </button>
    );
  };

  return (
    <aside className="sidebar" style={{ width: collapsed ? 'var(--side-w-collapsed)' : 'var(--side-w)',
      background: bg, borderRight: light ? '1px solid var(--hairline)' : 'none' }}>
      {/* 品牌 */}
      <div className="side-brand" style={{ borderBottom: `1px solid ${hairline}`, justifyContent: collapsed ? 'center' : 'flex-start' }}>
        <span className="brand-mark"><Icon name="command" size={20} /></span>
        {!collapsed && (
          <div style={{ overflow: 'hidden' }}>
            <div className="brand-name" style={{ color: inkColor }}>云原中台</div>
            <div className="brand-sub" style={{ color: muted }}>管理控制台</div>
          </div>
        )}
      </div>

      {/* 导航 */}
      <nav className="side-nav">
        {NAV.map((node, i) => {
          if (node.items) {
            return (
              <div key={i} className="nav-group">
                {!collapsed && <div className="nav-group-label" style={{ color: muted }}>{node.group}</div>}
                {collapsed && i > 0 && <div className="nav-divider" style={{ background: hairline }} />}
                {node.items.map(it => <NavItem key={it.key} item={it} />)}
              </div>
            );
          }
          return <div key={i} className="nav-group"><NavItem item={node} /></div>;
        })}
      </nav>

      {/* 底部用户 */}
      <div className="side-foot" style={{ borderTop: `1px solid ${hairline}` }}>
        <Avatar name="周明远" size={collapsed ? 32 : 36} tint="#0066cc" />
        {!collapsed && (
          <div style={{ overflow: 'hidden', flex: 1 }}>
            <div style={{ fontSize: 13.5, fontWeight: 600, color: inkColor, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>周明远</div>
            <div style={{ fontSize: 12, color: muted }}>超级管理员</div>
          </div>
        )}
        {!collapsed && <Icon name="chevronRight" size={15} style={{ color: muted }} />}
      </div>
    </aside>
  );
}

/* ---------- 顶栏 ---------- */
function Topbar({ collapsed, onToggle, title, onLogout, onToast }) {
  const [bellOpen, setBellOpen] = useState(false);
  const bellRef = useRef(null);
  const [userOpen, setUserOpen] = useState(false);
  const userRef = useRef(null);
  useEffect(() => {
    const h = (e) => {
      if (bellRef.current && !bellRef.current.contains(e.target)) setBellOpen(false);
      if (userRef.current && !userRef.current.contains(e.target)) setUserOpen(false);
    };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);
  return (
    <header className="topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <IconBtn name="panelLeft" size={19} onClick={onToggle} tip={collapsed ? '展开侧栏' : '收起侧栏'} />
        <div className="topbar-search input-wrap has-icon">
          <Icon name="search" size={15} />
          <input className="input input-search" placeholder="搜索菜单、用户、功能…" style={{ width: 260 }}
            onKeyDown={(e) => { if (e.key === 'Enter') onToast('搜索功能演示', 'info'); }} />
          <span className="kbd">⌘K</span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <IconBtn name="grid" size={19} tip="应用" onClick={() => onToast('应用面板演示', 'info')} />
        <IconBtn name="gear" size={19} tip="设置" onClick={() => onToast('系统设置演示', 'info')} />
        <div ref={bellRef} style={{ position: 'relative' }}>
          <button className="icon-btn tip" data-tip="通知" onClick={() => setBellOpen(o => !o)} style={{ position: 'relative' }}>
            <Icon name="bell" size={19} />
            <span className="bell-dot" />
          </button>
          {bellOpen && (
            <div className="pop-menu" style={{ width: 320 }}>
              <div className="pop-head">通知 <span className="tag tag-info" style={{ height: 20 }}>3 条未读</span></div>
              {DASH_TODO.slice(0, 3).map(t => (
                <div key={t.id} className="pop-item" onClick={() => { setBellOpen(false); onToast('查看任务详情', 'info'); }}>
                  <span className="pop-dot" style={{ background: t.level === 'danger' ? 'var(--danger)' : t.level === 'warn' ? 'var(--warn)' : 'var(--primary)' }} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 13, fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{t.title}</div>
                    <div style={{ fontSize: 12, color: 'var(--ink-muted-48)', marginTop: 2 }}>{t.from} · {t.time}</div>
                  </div>
                </div>
              ))}
              <div className="pop-foot" onClick={() => { setBellOpen(false); onToast('跳转待办列表', 'info'); }}>查看全部待办</div>
            </div>
          )}
        </div>
        <div className="topbar-sep" />
        <div ref={userRef} style={{ position: 'relative' }}>
          <button className="user-chip" onClick={() => setUserOpen(o => !o)}>
            <Avatar name="周明远" size={30} tint="#0066cc" />
            <span className="user-chip-name">周明远</span>
            <Icon name="chevronDown" size={14} style={{ color: 'var(--ink-muted-48)' }} />
          </button>
          {userOpen && (
            <div className="pop-menu" style={{ width: 200, right: 0 }}>
              <div className="pop-userhead">
                <Avatar name="周明远" size={40} tint="#0066cc" />
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600 }}>周明远</div>
                  <div style={{ fontSize: 12, color: 'var(--ink-muted-48)' }}>zhoumy@yunyuan.com</div>
                </div>
              </div>
              <div className="pop-line" onClick={() => { setUserOpen(false); onToast('个人中心演示', 'info'); }}><Icon name="user" size={16} />个人中心</div>
              <div className="pop-line" onClick={() => { setUserOpen(false); onToast('账户设置演示', 'info'); }}><Icon name="gear" size={16} />账户设置</div>
              <div className="pop-line" onClick={() => { setUserOpen(false); onToast('密码修改演示', 'info'); }}><Icon name="key" size={16} />修改密码</div>
              <div className="pop-divider" />
              <div className="pop-line danger" onClick={() => { setUserOpen(false); onLogout(); }}><Icon name="logout" size={16} />退出登录</div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}

/* ---------- App 外壳 ---------- */
function AppShell({ route, onNav, tweaks, children, onLogout }) {
  const [collapsed, setCollapsed] = useState(false);
  const toast = useToast();
  return (
    <div className="app-shell">
      <Sidebar route={route} onNav={onNav} collapsed={collapsed} tweaks={tweaks} onToast={toast} />
      <div className="app-main">
        <Topbar collapsed={collapsed} onToggle={() => setCollapsed(c => !c)} onLogout={onLogout} onToast={toast} />
        <div className="app-content" key={route}>
          {children}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { Sidebar, Topbar, AppShell });
