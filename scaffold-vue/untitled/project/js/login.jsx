/* ============================================================
   登录页 —— 左侧近黑品牌区 + 右侧纯白表单
   ============================================================ */
function CaptchaBox({ code, onRefresh }) {
  // 简易验证码：字符 + 干扰线（纯展示）
  return (
    <button type="button" className="captcha-box" onClick={onRefresh} title="点击刷新">
      <svg width="100" height="40" viewBox="0 0 100 40">
        <rect width="100" height="40" fill="#f5f5f7" />
        {[0,1,2,3].map(i => (
          <line key={i} x1={Math.random()*100} y1={Math.random()*40} x2={Math.random()*100} y2={Math.random()*40}
            stroke="#d2d2d7" strokeWidth="1" />
        ))}
        {code.split('').map((c, i) => (
          <text key={i} x={14 + i * 21} y={28} fontSize="22" fontWeight="700"
            fill={['#0066cc','#1d1d1f','#1f8a4c','#b06d00'][i % 4]}
            transform={`rotate(${(i % 2 ? 1 : -1) * (6 + i * 2)} ${14 + i * 21} 22)`}
            fontFamily="SF Pro Display, sans-serif">{c}</text>
        ))}
      </svg>
    </button>
  );
}

function LoginPage({ onLogin }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [captcha, setCaptcha] = useState('');
  const [remember, setRemember] = useState(true);
  const [code, setCode] = useState('A8K3');
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  const refresh = () => setCode(Math.random().toString(36).slice(2, 6).toUpperCase());

  const submit = (e) => {
    e.preventDefault();
    const er = {};
    if (!username.trim()) er.username = '请输入登录账号';
    if (!password) er.password = '请输入登录密码';
    if (!captcha.trim()) er.captcha = '请输入验证码';
    setErrors(er);
    if (Object.keys(er).length) return;
    setLoading(true);
    setTimeout(() => { setLoading(false); onLogin(); }, 800);
  };

  return (
    <div className="login-wrap">
      {/* 左：品牌区 */}
      <div className="login-brand">
        <div className="login-brand-top">
          <span className="brand-mark" style={{ width: 40, height: 40 }}><Icon name="command" size={24} /></span>
          <div>
            <div style={{ fontSize: 17, fontWeight: 600, color: '#fff', letterSpacing: '-0.02em' }}>云原中台</div>
            <div style={{ fontSize: 12.5, color: 'var(--side-ink-muted)' }}>Enterprise Admin Platform</div>
          </div>
        </div>

        <div className="login-brand-mid">
          <h1>统一的企业<br/>后台管理平台</h1>
          <p>权限管理 · 数据权限 · 系统配置 · 代码生成 · 工作流审批，一套底座支撑全部业务系统快速搭建。</p>
          <div className="login-feat">
            {[['shield','RBAC 权限'],['dept','数据权限'],['command','代码生成'],['flow','工作流']].map(([ic, t]) => (
              <div key={t} className="login-feat-item">
                <span><Icon name={ic} size={16} /></span>{t}
              </div>
            ))}
          </div>
        </div>

        <div className="login-brand-foot">© 2026 云原科技集团 · 版本 v1.0.0（MVP）</div>
        <div className="login-orb" />
      </div>

      {/* 右：表单 */}
      <div className="login-form-side">
        <form className="login-card" onSubmit={submit}>
          <div className="login-card-head">
            <h2>欢迎回来</h2>
            <p>登录以继续访问管理控制台</p>
          </div>

          <Field label="登录账号" error={errors.username}>
            <div className="input-wrap has-icon">
              <Icon name="user" size={16} />
              <input className={`input ${errors.username ? 'invalid' : ''}`} value={username} placeholder="请输入账号"
                style={{ paddingLeft: 38 }} onChange={(e) => setUsername(e.target.value)} />
            </div>
          </Field>

          <Field label="登录密码" error={errors.password}>
            <div className="input-wrap has-icon">
              <Icon name="lock" size={16} />
              <input type="password" className={`input ${errors.password ? 'invalid' : ''}`} value={password} placeholder="请输入密码"
                style={{ paddingLeft: 38 }} onChange={(e) => setPassword(e.target.value)} />
            </div>
          </Field>

          <Field label="验证码" error={errors.captcha}>
            <div style={{ display: 'flex', gap: 12 }}>
              <input className={`input ${errors.captcha ? 'invalid' : ''}`} value={captcha} placeholder="请输入右侧字符"
                onChange={(e) => setCaptcha(e.target.value)} style={{ flex: 1 }} />
              <CaptchaBox code={code} onRefresh={refresh} />
            </div>
          </Field>

          <div className="login-row">
            <label className="login-remember" onClick={() => setRemember(r => !r)}>
              <Checkbox checked={remember} onChange={setRemember} />
              <span>记住登录状态</span>
            </label>
            <a className="login-link" onClick={(e) => e.preventDefault()} href="#">忘记密码？</a>
          </div>

          <Button variant="primary" size="lg" type="submit" className="login-submit" disabled={loading}>
            {loading ? '登录中…' : '登 录'}
          </Button>

          <div className="login-demo">
            <Icon name="key" size={13} />
            演示账号 <b>admin</b> / 密码 <b>admin123</b>，验证码任意填写
          </div>
        </form>
      </div>
    </div>
  );
}

window.LoginPage = LoginPage;
