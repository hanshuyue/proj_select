/* ============================================================
   主应用 —— 路由 / 登录态 / Tweaks
   ============================================================ */
const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "sidebarTone": "ink",
  "navActive": "pill",
  "listLayout": "table",
  "density": "regular",
  "accent": "#0066cc"
}/*EDITMODE-END*/;

const ACCENTS = {
  '#0066cc': { soft: '#e8f1fb', onDark: '#2997ff', focus: '#0071e3' },
  '#1d1d1f': { soft: '#ededee', onDark: '#bdbdbf', focus: '#3a3a3c' },
  '#1f8a4c': { soft: '#e7f4ec', onDark: '#34c759', focus: '#23a058' },
  '#6b46d9': { soft: '#f0ecfb', onDark: '#a78bfa', focus: '#7c52f0' },
};

function App() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const [authed, setAuthed] = useState(false);
  const [route, setRoute] = useState('dashboard');

  // 应用强调色
  useEffect(() => {
    const a = ACCENTS[t.accent] || ACCENTS['#0066cc'];
    const r = document.documentElement.style;
    r.setProperty('--primary', t.accent);
    r.setProperty('--primary-soft', a.soft);
    r.setProperty('--primary-on-dark', a.onDark);
    r.setProperty('--primary-focus', a.focus);
  }, [t.accent]);

  const nav = (r) => { if (r) setRoute(r); };

  const PAGES = {
    dashboard: <Dashboard onNav={nav} />,
    user: <UsersPage tweaks={t} />,
    role: <RolesPage tweaks={t} />,
    menu: <MenusPage tweaks={t} />,
    dept: <DeptsPage />,
    post: <PostPage tweaks={t} />,
    dict: <DictPage tweaks={t} />,
    config: <ConfigPage tweaks={t} />,
    operlog: <OperLogPage tweaks={t} />,
    loginlog: <LoginLogPage tweaks={t} />,
    model: <WfModelPage tweaks={t} />,
    def: <WfDefPage tweaks={t} />,
    todo: <WfTodoPage />,
    done: <WfDonePage tweaks={t} />,
    gen: <GeneratorPage tweaks={t} />,
  };

  const panel = (
    <TweaksPanel>
      <TweakSection label="侧边栏" />
      <TweakRadio label="底色" value={t.sidebarTone}
        options={[{ label: '墨黑', value: 'ink' }, { label: '纯黑', value: 'black' }, { label: '浅色', value: 'light' }]}
        onChange={(v) => setTweak('sidebarTone', v)} />
      <TweakRadio label="选中样式" value={t.navActive}
        options={[{ label: '胶囊', value: 'pill' }, { label: '侧条', value: 'bar' }, { label: '淡色', value: 'tint' }]}
        onChange={(v) => setTweak('navActive', v)} />

      <TweakSection label="表格 / 列表" />
      <TweakRadio label="用户页排布" value={t.listLayout}
        options={[{ label: '表格', value: 'table' }, { label: '卡片', value: 'cards' }]}
        onChange={(v) => setTweak('listLayout', v)} />
      <TweakRadio label="行密度" value={t.density}
        options={[{ label: '舒展', value: 'comfy' }, { label: '常规', value: 'regular' }, { label: '紧凑', value: 'compact' }]}
        onChange={(v) => setTweak('density', v)} />

      <TweakSection label="主题" />
      <TweakColor label="强调色" value={t.accent}
        options={['#0066cc', '#1d1d1f', '#1f8a4c', '#6b46d9']}
        onChange={(v) => setTweak('accent', v)} />
      <div style={{ fontSize: 11.5, color: 'var(--ink-muted-48)', padding: '2px 2px 0', lineHeight: 1.5 }}>
        设计基线为单一 Action Blue；其余强调色仅供探索。
      </div>
    </TweaksPanel>
  );

  if (!authed) {
    return (<>
      <LoginPage onLogin={() => { setAuthed(true); setRoute('dashboard'); }} />
      {panel}
    </>);
  }

  return (
    <AppShell route={route} onNav={nav} tweaks={t} onLogout={() => setAuthed(false)}>
      {PAGES[route] || <Dashboard onNav={nav} />}
      {panel}
    </AppShell>
  );
}

function Root() {
  return (
    <ToastProvider>
      <App />
    </ToastProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<Root />);
