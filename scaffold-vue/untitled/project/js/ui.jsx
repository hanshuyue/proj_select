/* ============================================================
   共享 UI 组件
   ============================================================ */
/* ---------- 按钮 ---------- */
function Button({ variant = 'quiet', size = 'md', icon, iconRight, children, className = '', ...rest }) {
  return (
    <button className={`btn btn-${variant} btn-${size} ${className}`} {...rest}>
      {icon && <Icon name={icon} size={size === 'sm' ? 15 : 16} />}
      {children}
      {iconRight && <Icon name={iconRight} size={size === 'sm' ? 15 : 16} />}
    </button>
  );
}
function IconBtn({ name, size = 18, bordered = false, tip, className = '', ...rest }) {
  return (
    <button className={`icon-btn ${bordered ? 'bordered' : ''} ${tip ? 'tip' : ''} ${className}`}
      data-tip={tip} {...rest}>
      <Icon name={name} size={size} />
    </button>
  );
}

/* ---------- 输入 ---------- */
function Field({ label, required, hint, error, children, className = '' }) {
  return (
    <div className={`field ${className}`}>
      {label && <label className="field-label">{label}{required && <span className="req">*</span>}</label>}
      {children}
      {error ? <span className="field-error"><Icon name="x" size={12} />{error}</span>
             : hint ? <span className="field-hint">{hint}</span> : null}
    </div>
  );
}
function Input({ invalid, className = '', ...rest }) {
  return <input className={`input ${invalid ? 'invalid' : ''} ${className}`} {...rest} />;
}
function SearchInput({ value, onChange, placeholder = '搜索…', onEnter, className = '', round = true }) {
  return (
    <div className="input-wrap has-icon" style={{ width: '100%' }}>
      <Icon name="search" size={15} />
      <input className={`input ${round ? 'input-search' : ''} ${className}`}
        value={value} placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter' && onEnter) onEnter(); }} />
    </div>
  );
}

/* ---------- 自定义下拉 ---------- */
function Select({ value, onChange, options, placeholder = '请选择', className = '', width }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);
  useEffect(() => {
    const h = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', h);
    return () => document.removeEventListener('mousedown', h);
  }, []);
  const norm = options.map(o => typeof o === 'string' ? { label: o, value: o } : o);
  const cur = norm.find(o => o.value === value);
  return (
    <div ref={ref} className={className} style={{ position: 'relative', width: width || 'auto' }}>
      <div className={`select-trigger ${open ? 'open' : ''}`} onClick={() => setOpen(o => !o)} style={{ width: '100%' }}>
        <span className={cur ? '' : 'placeholder'} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {cur ? cur.label : placeholder}
        </span>
        <Icon name="chevronDown" size={15} />
      </div>
      {open && (
        <div className="select-menu">
          {norm.map(o => (
            <div key={o.value} className={`select-option ${o.value === value ? 'selected' : ''}`}
              onClick={() => { onChange(o.value); setOpen(false); }}>
              <span>{o.label}</span>
              {o.value === value && <Icon name="check" size={15} className="check" />}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

/* ---------- 开关 / 复选 ---------- */
function Switch({ checked, onChange, disabled }) {
  return (
    <button className={`switch ${checked ? 'on' : ''}`} disabled={disabled}
      onClick={() => !disabled && onChange(!checked)} role="switch" aria-checked={checked}>
      <span className="knob" />
    </button>
  );
}
function Checkbox({ checked, indeterminate, onChange }) {
  return (
    <span className={`checkbox ${checked ? 'checked' : ''} ${indeterminate ? 'indeterminate' : ''}`}
      onClick={(e) => { e.stopPropagation(); onChange(!checked); }} role="checkbox" aria-checked={checked}>
      {indeterminate ? <Icon name="dot" size={12} stroke={4} /> : checked ? <Icon name="check" size={13} stroke={2.6} /> : null}
    </span>
  );
}

/* ---------- 状态标签 ---------- */
function Tag({ tone = 'neutral', dot, children }) {
  return <span className={`tag tag-${tone}`}>{dot && <span className="dot" />}{children}</span>;
}
function StatusTag({ status }) {
  return status ? <Tag tone="ok" dot>正常</Tag> : <Tag tone="neutral" dot>停用</Tag>;
}

/* ---------- 头像 ---------- */
function Avatar({ name, size = 34, tint }) {
  const ch = (name || '?').slice(-2);
  const color = tint || AVATAR_TINTS[(name || '').length % AVATAR_TINTS.length];
  return (
    <span className="avatar" style={{ width: size, height: size, fontSize: size * 0.4, background: color }}>{ch}</span>
  );
}

/* ---------- 卡片 / 面板 ---------- */
function Card({ children, className = '', style }) {
  return <div className={`card ${className}`} style={style}>{children}</div>;
}
function Panel({ title, sub, extra, children, className = '', bodyStyle }) {
  return (
    <div className={`card ${className}`}>
      {(title || extra) && (
        <div className="panel-head">
          <div>
            <div className="panel-title">{title}</div>
            {sub && <div className="panel-sub">{sub}</div>}
          </div>
          {extra}
        </div>
      )}
      <div style={bodyStyle}>{children}</div>
    </div>
  );
}

/* ---------- 表格 ---------- */
function Table({ columns, data, density = 'regular', rowKey = 'id', selectable, selected = [], onSelectChange,
                sort, onSort, loading, emptyText = '暂无数据', onRowClick }) {
  const allChecked = data.length > 0 && selected.length === data.length;
  const someChecked = selected.length > 0 && selected.length < data.length;
  const toggleAll = () => onSelectChange(allChecked ? [] : data.map(r => r[rowKey]));
  const toggleRow = (k) => onSelectChange(selected.includes(k) ? selected.filter(x => x !== k) : [...selected, k]);

  return (
    <div className="tablewrap">
      <table className={`tbl density-${density}`}>
        <thead>
          <tr>
            {selectable && (
              <th style={{ width: 46, paddingRight: 0 }}>
                <Checkbox checked={allChecked} indeterminate={someChecked} onChange={toggleAll} />
              </th>
            )}
            {columns.map(col => (
              <th key={col.key} className={col.sortable ? 'sortable' : ''} style={{ width: col.width, textAlign: col.align }}
                onClick={() => col.sortable && onSort && onSort(col.key)}>
                <span className="th-in">
                  {col.title}
                  {col.sortable && (
                    <span className="sort-ico" style={{ color: sort && sort.key === col.key ? 'var(--primary)' : '' }}>
                      <Icon name={sort && sort.key === col.key && sort.dir === 'asc' ? 'arrowUp' : 'arrowDown'} size={12} />
                    </span>
                  )}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading ? (
            Array.from({ length: 6 }).map((_, i) => (
              <tr key={i}>
                {selectable && <td><span className="skeleton" style={{ width: 18, height: 18, display: 'block' }} /></td>}
                {columns.map(c => <td key={c.key}><span className="skeleton" style={{ width: `${40 + (i * 7 + c.key.length * 5) % 50}%`, height: 13, display: 'block' }} /></td>)}
              </tr>
            ))
          ) : data.length === 0 ? (
            <tr>
              <td colSpan={columns.length + (selectable ? 1 : 0)}>
                <div className="tbl-empty">
                  <div className="em-ico"><Icon name="search" size={24} /></div>
                  <div>{emptyText}</div>
                </div>
              </td>
            </tr>
          ) : (
            data.map(row => {
              const k = row[rowKey];
              const isSel = selected.includes(k);
              return (
                <tr key={k} className={isSel ? 'selected' : ''} onClick={() => onRowClick && onRowClick(row)}
                  style={{ cursor: onRowClick ? 'pointer' : 'default' }}>
                  {selectable && (
                    <td style={{ paddingRight: 0 }} onClick={(e) => e.stopPropagation()}>
                      <Checkbox checked={isSel} onChange={() => toggleRow(k)} />
                    </td>
                  )}
                  {columns.map(col => (
                    <td key={col.key} className={col.className} style={{ textAlign: col.align }}>
                      {col.render ? col.render(row) : row[col.key]}
                    </td>
                  ))}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}

/* ---------- 分页 ---------- */
function Pagination({ total, page, pageSize, onPage, onPageSize }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));
  const start = total === 0 ? 0 : (page - 1) * pageSize + 1;
  const end = Math.min(total, page * pageSize);
  const nums = useMemo(() => {
    const arr = []; const win = 1;
    for (let i = 1; i <= pages; i++) {
      if (i === 1 || i === pages || (i >= page - win && i <= page + win)) arr.push(i);
      else if (arr[arr.length - 1] !== '…') arr.push('…');
    }
    return arr;
  }, [pages, page]);
  return (
    <div className="pager">
      <div className="pager-info">共 <b>{total}</b> 条　第 <b>{start}-{end}</b> 条</div>
      <div className="pager-ctrl">
        <Select value={pageSize} onChange={onPageSize} width={96}
          options={[{label:'10 条/页',value:10},{label:'20 条/页',value:20},{label:'50 条/页',value:50}]} />
        <div style={{ width: 8 }} />
        <button className="pg-btn" disabled={page === 1} onClick={() => onPage(page - 1)}><Icon name="chevronLeft" size={15} /></button>
        {nums.map((n, i) => n === '…'
          ? <span key={'d'+i} className="pg-dots">…</span>
          : <button key={n} className={`pg-btn ${n === page ? 'active' : ''}`} onClick={() => onPage(n)}>{n}</button>)}
        <button className="pg-btn" disabled={page === pages} onClick={() => onPage(page + 1)}><Icon name="chevronRight" size={15} /></button>
      </div>
    </div>
  );
}

/* ---------- 弹窗 ---------- */
function Modal({ title, children, footer, onClose, width = 540 }) {
  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', h);
    return () => document.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal" style={{ maxWidth: width }}>
        <div className="modal-head">
          <span className="modal-title">{title}</span>
          <IconBtn name="x" size={18} onClick={onClose} />
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

/* ---------- 抽屉 ---------- */
function Drawer({ title, sub, children, footer, onClose, width = 480 }) {
  useEffect(() => {
    const h = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', h);
    return () => document.removeEventListener('keydown', h);
  }, [onClose]);
  return (
    <div className="drawer-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="drawer" style={{ width }}>
        <div className="drawer-head">
          <div>
            <div className="modal-title">{title}</div>
            {sub && <div className="panel-sub">{sub}</div>}
          </div>
          <IconBtn name="x" size={18} onClick={onClose} />
        </div>
        <div className="drawer-body">{children}</div>
        {footer && <div className="drawer-foot">{footer}</div>}
      </div>
    </div>
  );
}

/* ---------- 分段控件 ---------- */
function Segmented({ value, onChange, options }) {
  return (
    <div className="segmented">
      {options.map(o => {
        const opt = typeof o === 'string' ? { label: o, value: o } : o;
        return (
          <button key={opt.value} className={opt.value === value ? 'active' : ''} onClick={() => onChange(opt.value)}>
            {opt.icon && <Icon name={opt.icon} size={15} />}{opt.label}
          </button>
        );
      })}
    </div>
  );
}

/* ---------- 页眉 ---------- */
function PageHeader({ crumbs = [], title, desc, actions }) {
  return (
    <div className="page-head">
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 20, flexWrap: 'wrap' }}>
        <div>
          {crumbs.length > 0 && (
            <div className="crumb">
              {crumbs.map((c, i) => (
                <React.Fragment key={i}>
                  {i > 0 && <Icon name="chevronRight" size={12} />}
                  <span>{c}</span>
                </React.Fragment>
              ))}
            </div>
          )}
          <div className="page-title">{title}</div>
          {desc && <div className="page-desc">{desc}</div>}
        </div>
        {actions && <div style={{ display: 'flex', gap: 10 }}>{actions}</div>}
      </div>
    </div>
  );
}

/* ---------- Toast ---------- */
const ToastCtx = createContext(null);
function ToastProvider({ children }) {
  const [list, setList] = useState([]);
  const push = useCallback((msg, tone = 'ok') => {
    const id = Date.now() + Math.random();
    setList(l => [...l, { id, msg, tone }]);
    setTimeout(() => setList(l => l.filter(t => t.id !== id)), 2600);
  }, []);
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div className="toast-stack">
        {list.map(t => (
          <div key={t.id} className={`toast ${t.tone}`}>
            <span className="t-ico"><Icon name={t.tone === 'danger' ? 'x' : 'check'} size={12} stroke={3} /></span>
            {t.msg}
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}
function useToast() { return useContext(ToastCtx); }

/* ---------- 确认弹窗 ---------- */
function ConfirmModal({ title = '确认操作', message, confirmText = '确定', danger, onConfirm, onClose }) {
  return (
    <Modal title={title} onClose={onClose} width={420}
      footer={<>
        <Button variant="quiet" onClick={onClose}>取消</Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={() => { onConfirm(); onClose(); }}>{confirmText}</Button>
      </>}>
      <div style={{ display: 'flex', gap: 14, alignItems: 'flex-start' }}>
        <span style={{ width: 38, height: 38, borderRadius: 10, flexShrink: 0,
          background: danger ? 'var(--danger-soft)' : 'var(--primary-soft)',
          color: danger ? 'var(--danger)' : 'var(--primary)',
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
          <Icon name={danger ? 'trash' : 'bell'} size={18} />
        </span>
        <div style={{ fontSize: 14.5, color: 'var(--ink-72)', lineHeight: 1.55, paddingTop: 2 }}>{message}</div>
      </div>
    </Modal>
  );
}

Object.assign(window, {
  Button, IconBtn, Field, Input, SearchInput, Select, Switch, Checkbox,
  Tag, StatusTag, Avatar, Card, Panel, Table, Pagination, Modal, Drawer,
  Segmented, PageHeader, ToastProvider, useToast, ConfirmModal,
});
