/* ============================================================
   工作台 / 仪表盘
   ============================================================ */
function fmt(n) { return n >= 10000 ? (n / 10000).toFixed(1) + ' 万' : n.toLocaleString(); }

function TrendChart({ data, height = 200 }) {
  const w = 720, h = height, pad = 8;
  const max = Math.max(...data), min = Math.min(...data);
  const range = max - min || 1;
  const stepX = (w - pad * 2) / (data.length - 1);
  const pts = data.map((v, i) => [pad + i * stepX, h - pad - ((v - min) / range) * (h - pad * 2 - 16) - 8]);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' ');
  const area = `${line} L${pts[pts.length-1][0].toFixed(1)} ${h} L${pts[0][0].toFixed(1)} ${h} Z`;
  return (
    <svg viewBox={`0 0 ${w} ${h}`} width="100%" height={height} preserveAspectRatio="none" style={{ display: 'block' }}>
      <defs>
        <linearGradient id="tg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#0066cc" stopOpacity="0.16" />
          <stop offset="100%" stopColor="#0066cc" stopOpacity="0" />
        </linearGradient>
      </defs>
      {[0.25, 0.5, 0.75].map(g => (
        <line key={g} x1={pad} y1={h * g} x2={w - pad} y2={h * g} stroke="var(--hairline-soft)" strokeWidth="1" />
      ))}
      <path d={area} fill="url(#tg)" />
      <path d={line} fill="none" stroke="var(--primary)" strokeWidth="2.2" strokeLinejoin="round" strokeLinecap="round" />
      {pts.filter((_, i) => i === pts.length - 1).map((p, i) => (
        <circle key={i} cx={p[0]} cy={p[1]} r="4" fill="#fff" stroke="var(--primary)" strokeWidth="2.4" />
      ))}
    </svg>
  );
}

function DonutChart({ segments, size = 132 }) {
  const total = segments.reduce((s, x) => s + x.value, 0);
  const r = size / 2 - 13, cx = size / 2, cy = size / 2, C = 2 * Math.PI * r;
  let offset = 0;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--hairline)" strokeWidth="13" />
      {segments.map((s, i) => {
        const len = (s.value / total) * C;
        const el = (
          <circle key={i} cx={cx} cy={cy} r={r} fill="none" stroke={s.color} strokeWidth="13"
            strokeDasharray={`${len} ${C - len}`} strokeDashoffset={-offset}
            transform={`rotate(-90 ${cx} ${cy})`} strokeLinecap="round" />
        );
        offset += len;
        return el;
      })}
      <text x={cx} y={cy - 2} textAnchor="middle" fontSize="22" fontWeight="700" fill="var(--ink)" fontFamily="var(--font-num)">{total}</text>
      <text x={cx} y={cy + 16} textAnchor="middle" fontSize="11" fill="var(--ink-muted-48)">角色总数</text>
    </svg>
  );
}

function Dashboard({ onNav }) {
  const toast = useToast();
  const roleSeg = [
    { label: '系统类', value: 13, color: '#0066cc' },
    { label: '业务类', value: 40, color: '#1f8a4c' },
    { label: '只读类', value: 15, color: '#b06d00' },
  ];
  return (
    <div>
      <PageHeader title="工作台"
        desc="欢迎回来，周明远。这里汇总了系统的关键指标与你的待办事项。"
        actions={<>
          <Button variant="quiet" icon="download" onClick={() => toast('报表已导出', 'ok')}>导出概览</Button>
          <Button variant="primary" icon="plus" onClick={() => onNav('user')}>新建用户</Button>
        </>} />

      {/* 统计卡 */}
      <div className="stat-grid">
        {DASH_STATS.map(s => (
          <Card key={s.key} className="stat-card">
            <div className="stat-top">
              <span className="stat-ico"><Icon name={s.icon} size={18} /></span>
              <span className={`stat-delta ${s.up ? 'up' : 'down'}`}>
                <Icon name={s.up ? 'arrowUp' : 'arrowDown'} size={12} stroke={2.4} />{s.delta}%
              </span>
            </div>
            <div className="stat-value tnum">{fmt(s.value)}</div>
            <div className="stat-label">{s.label}</div>
            <div className="stat-sub">{s.sub}</div>
          </Card>
        ))}
      </div>

      {/* 趋势 + 角色分布 */}
      <div className="two-col" style={{ marginTop: 18 }}>
        <Panel title="接口调用趋势" sub="最近 24 小时"
          extra={<Segmented value="24h" onChange={() => {}} options={[{label:'24小时',value:'24h'},{label:'7天',value:'7d'},{label:'30天',value:'30d'}]} />}>
          <div style={{ padding: '20px 22px 10px' }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 8 }}>
              <span className="tnum" style={{ fontSize: 30, fontWeight: 700, letterSpacing: '-0.02em' }}>92,400</span>
              <Tag tone="ok" dot>成功率 99.8%</Tag>
            </div>
            <TrendChart data={DASH_TREND} />
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11.5, color: 'var(--ink-muted-48)', marginTop: 6 }}>
              <span>00:00</span><span>06:00</span><span>12:00</span><span>18:00</span><span>现在</span>
            </div>
          </div>
        </Panel>

        <Panel title="角色构成" sub="按数据权限类型">
          <div style={{ padding: '18px 22px 22px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 18 }}>
            <DonutChart segments={roleSeg} />
            <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 11 }}>
              {roleSeg.map(s => (
                <div key={s.label} style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 13.5 }}>
                  <span style={{ width: 9, height: 9, borderRadius: 3, background: s.color }} />
                  <span style={{ flex: 1, color: 'var(--ink-72)' }}>{s.label}</span>
                  <span className="tnum" style={{ fontWeight: 600 }}>{s.value}</span>
                </div>
              ))}
            </div>
          </div>
        </Panel>
      </div>

      {/* 待办 + 活动 */}
      <div className="two-col" style={{ marginTop: 18 }}>
        <Panel title="我的待办" sub="6 项待处理"
          extra={<Button variant="text" size="sm" iconRight="chevronRight" onClick={() => toast('跳转待办列表', 'info')}>全部</Button>}>
          <div className="todo-list">
            {DASH_TODO.map(t => (
              <div key={t.id} className="todo-item" onClick={() => toast('打开审批详情', 'info')}>
                <span className={`todo-bar ${t.level}`} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="todo-title">{t.title}</div>
                  <div className="todo-meta">
                    <Avatar name={t.from} size={18} />
                    <span>{t.from}</span><span className="todo-dot">·</span><span>{t.type}</span>
                  </div>
                </div>
                <div className="todo-time">{t.time}</div>
                <Button variant="ghost" size="sm" onClick={(e) => { e.stopPropagation(); toast('已通过审批', 'ok'); }}>审批</Button>
              </div>
            ))}
          </div>
        </Panel>

        <Panel title="最近动态" sub="系统操作日志">
          <div className="feed">
            {DASH_ACTIVITY.map((a, i) => (
              <div key={i} className="feed-item">
                <Avatar name={a.who} size={32} tint={a.tint} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div className="feed-text">
                    <b>{a.who}</b> {a.action} <span className="feed-target">{a.target}</span>
                  </div>
                  <div className="feed-time">{a.time}</div>
                </div>
              </div>
            ))}
            <div className="feed-more" onClick={() => toast('跳转操作日志', 'info')}>查看完整日志</div>
          </div>
        </Panel>
      </div>
    </div>
  );
}

window.Dashboard = Dashboard;
