/* ============================================================
   代码生成 —— 导入表 / 字段配置 / 预览生成
   ============================================================ */
const GEN_FILES = [
  { name: 'domain/BizOrder.java', label: '实体类' },
  { name: 'mapper/BizOrderMapper.java', label: 'Mapper' },
  { name: 'service/IBizOrderService.java', label: 'Service' },
  { name: 'controller/BizOrderController.java', label: 'Controller' },
  { name: 'api/order.js', label: '前端 API' },
  { name: 'views/order/index.vue', label: '列表页' },
];
const GEN_PREVIEW = `@RestController
@RequestMapping("/biz/order")
public class BizOrderController extends BaseController {

    @Autowired
    private IBizOrderService bizOrderService;

    @PreAuthorize("@ss.hasPermi('biz:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(BizOrder bizOrder) {
        startPage();
        List<BizOrder> list = bizOrderService.selectBizOrderList(bizOrder);
        return getDataTable(list);
    }

    @Log(title = "业务订单", businessType = BusinessType.INSERT)
    @PreAuthorize("@ss.hasPermi('biz:order:add')")
    @PostMapping
    public AjaxResult add(@RequestBody BizOrder bizOrder) {
        return toAjax(bizOrderService.insertBizOrder(bizOrder));
    }
}`;

function ImportModal({ onClose, onImport }) {
  const toast = useToast();
  const candidates = [
    { tableName: 'biz_supplier', tableComment: '供应商表' },
    { tableName: 'biz_payment', tableComment: '付款记录表' },
    { tableName: 'biz_warehouse', tableComment: '仓库信息表' },
    { tableName: 'sys_dept_extra', tableComment: '部门扩展表' },
  ];
  const [sel, setSel] = useState([]);
  const toggle = (n) => setSel(s => s.includes(n) ? s.filter(x => x !== n) : [...s, n]);
  return (
    <Modal title="导入数据库表" onClose={onClose} width={560}
      footer={<>
        <Button variant="quiet" onClick={onClose}>取消</Button>
        <Button variant="primary" disabled={!sel.length} onClick={() => { onImport(candidates.filter(c => sel.includes(c.tableName))); toast(`已导入 ${sel.length} 张表`, 'ok'); }}>导入 {sel.length ? `(${sel.length})` : ''}</Button>
      </>}>
      <div style={{ marginBottom: 12 }}><SearchInput value="" onChange={() => {}} placeholder="搜索数据库表名" /></div>
      <div className="import-list">
        {candidates.map(c => (
          <div key={c.tableName} className={`import-row ${sel.includes(c.tableName) ? 'on' : ''}`} onClick={() => toggle(c.tableName)}>
            <Checkbox checked={sel.includes(c.tableName)} onChange={() => toggle(c.tableName)} />
            <Icon name="database" size={16} style={{ color: 'var(--ink-muted-48)' }} />
            <span className="code-chip">{c.tableName}</span>
            <span style={{ flex: 1, fontSize: 13, color: 'var(--ink-72)' }}>{c.tableComment}</span>
          </div>
        ))}
      </div>
    </Modal>
  );
}

function ConfigDrawer({ table, onClose }) {
  const toast = useToast();
  const [fields, setFields] = useState(GEN_FIELDS);
  const setF = (i, k, v) => setFields(fs => fs.map((f, idx) => idx === i ? { ...f, [k]: v } : f));

  const columns = [
    { key: 'col', title: '字段列名', render: (f) => <span className="code-chip">{f.col}</span> },
    { key: 'comment', title: '字段描述', render: (f, i) => f.comment },
    { key: 'javaType', title: 'Java 类型', width: 110, render: (f) => <span className="cell-muted" style={{ fontSize: 13 }}>{f.javaType}</span> },
    { key: 'insert', title: '插入', width: 56, align: 'center' },
    { key: 'edit', title: '编辑', width: 56, align: 'center' },
    { key: 'list', title: '列表', width: 56, align: 'center' },
    { key: 'query', title: '查询', width: 56, align: 'center' },
    { key: 'formType', title: '表单类型', width: 120 },
  ];

  return (
    <Drawer title={`字段配置 · ${table.tableComment}`} sub={table.tableName} width={860} onClose={onClose}
      footer={<><Button variant="quiet" onClick={onClose}>取消</Button><Button variant="primary" icon="check" onClick={() => { toast('字段配置已保存', 'ok'); onClose(); }}>保存配置</Button></>}>
      <div className="form-grid" style={{ marginBottom: 20 }}>
        <Field label="实体类名称"><Input defaultValue={table.className} /></Field>
        <Field label="生成模块名"><Input defaultValue={table.module} /></Field>
        <Field label="业务名称"><Input defaultValue={table.tableComment.replace('表', '')} /></Field>
        <Field label="生成模板"><Select value="crud" onChange={() => {}} options={[{ label: '单表 CRUD', value: 'crud' }, { label: '树表', value: 'tree' }, { label: '主子表', value: 'sub' }]} /></Field>
      </div>
      <div className="approve-sec-label">字段配置</div>
      <div className="gen-field-table">
        <table className="tbl density-compact">
          <thead><tr>
            {columns.map(c => <th key={c.key} style={{ width: c.width, textAlign: c.align }}>{c.title}</th>)}
          </tr></thead>
          <tbody>
            {fields.map((f, i) => (
              <tr key={f.col}>
                <td><span className="code-chip">{f.col}</span></td>
                <td><input className="input" style={{ height: 30, fontSize: 12.5 }} value={f.comment} onChange={(e) => setF(i, 'comment', e.target.value)} /></td>
                <td><span className="cell-muted" style={{ fontSize: 12.5 }}>{f.javaType}</span></td>
                <td style={{ textAlign: 'center' }}><Checkbox checked={f.insert} onChange={(v) => setF(i, 'insert', v)} /></td>
                <td style={{ textAlign: 'center' }}><Checkbox checked={f.edit} onChange={(v) => setF(i, 'edit', v)} /></td>
                <td style={{ textAlign: 'center' }}><Checkbox checked={f.list} onChange={(v) => setF(i, 'list', v)} /></td>
                <td style={{ textAlign: 'center' }}><Checkbox checked={f.query} onChange={(v) => setF(i, 'query', v)} /></td>
                <td><Select value={f.formType} onChange={(v) => setF(i, 'formType', v)} width={108}
                  options={['文本框','文本域','下拉框','数字框','日期框','单选框','隐藏']} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Drawer>
  );
}

function PreviewModal({ table, onClose }) {
  const toast = useToast();
  const [active, setActive] = useState(GEN_FILES[3].name);
  return (
    <Modal title={`代码预览 · ${table.tableComment}`} onClose={onClose} width={880}
      footer={<>
        <Button variant="quiet" icon="copy" onClick={() => toast('已复制到剪贴板', 'ok')}>复制当前</Button>
        <Button variant="primary" icon="download" onClick={() => { toast('已下载代码压缩包', 'ok'); onClose(); }}>下载全部</Button>
      </>}>
      <div className="preview-wrap">
        <div className="preview-files">
          {GEN_FILES.map(f => (
            <div key={f.name} className={`preview-file ${active === f.name ? 'on' : ''}`} onClick={() => setActive(f.name)}>
              <Icon name="file" size={14} />
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="preview-file-name">{f.name.split('/').pop()}</div>
                <div className="preview-file-label">{f.label}</div>
              </div>
            </div>
          ))}
        </div>
        <pre className="preview-code">{GEN_PREVIEW}</pre>
      </div>
    </Modal>
  );
}

function GeneratorPage({ tweaks }) {
  const toast = useToast();
  const [list, setList] = useState(GEN_TABLES);
  const [kw, setKw] = useState('');
  const [importOpen, setImportOpen] = useState(false);
  const [config, setConfig] = useState(null);
  const [preview, setPreview] = useState(null);
  const [modal, setModal] = useState(null);
  const filtered = list.filter(t => !kw || t.tableName.includes(kw) || t.tableComment.includes(kw));

  const onImport = (tables) => {
    setList(l => [...tables.map((t, i) => ({ ...t, id: Date.now() + i, className: t.tableName.split('_').map(w => w[0].toUpperCase() + w.slice(1)).join(''), module: t.tableName.split('_')[0], synced: true, createTime: '2026-06-05 10:00' })), ...l]);
    setImportOpen(false);
  };

  const columns = [
    { key: 'tableName', title: '表名称', render: (t) => (
      <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
        <span className="menu-ico-box" style={{ width: 34, height: 34, background: 'var(--primary-soft)', color: 'var(--primary)' }}><Icon name="database" size={17} /></span>
        <div>
          <div style={{ marginBottom: 2 }}><span className="code-chip">{t.tableName}</span></div>
          <div className="cell-muted" style={{ fontSize: 12.5 }}>{t.tableComment}</div>
        </div>
      </div>
    )},
    { key: 'className', title: '实体类', render: (t) => <span className="tnum" style={{ fontSize: 13 }}>{t.className}</span> },
    { key: 'module', title: '模块', width: 100, render: (t) => <Tag tone="neutral">{t.module}</Tag> },
    { key: 'synced', title: '同步状态', width: 110, render: (t) => t.synced ? <Tag tone="ok" dot>已同步</Tag> : <Tag tone="warn" dot>待同步</Tag> },
    { key: 'createTime', title: '导入时间', width: 160, render: (t) => <span className="tnum cell-muted" style={{ fontSize: 12.5 }}>{t.createTime}</span> },
    { key: 'actions', title: '操作', width: 230, className: 'col-actions', render: (t) => (
      <div className="row-actions">
        <Button variant="text" size="sm" icon="eye" onClick={() => setPreview(t)}>预览</Button>
        <Button variant="text" size="sm" icon="gear" onClick={() => setConfig(t)}>配置</Button>
        <Button variant="text" size="sm" icon="download" onClick={() => toast(`已生成「${t.tableComment}」代码`, 'ok')}>生成</Button>
        <IconBtn name="trash" size={16} tip="删除" onClick={() => setModal({ type: 'delete', row: t })} />
      </div>
    )},
  ];

  return (
    <div>
      <PageHeader crumbs={['系统工具', '代码生成']} title="代码生成"
        desc="导入数据库表，配置字段查询与表单规则，一键生成前后端 CRUD 代码与菜单权限。"
        actions={<>
          <Button variant="quiet" icon="refresh" onClick={() => toast('已同步表结构', 'ok')}>同步</Button>
          <Button variant="primary" icon="plus" onClick={() => setImportOpen(true)}>导入表</Button>
        </>} />

      <Card>
        <div className="panel-head">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ width: 280 }}><SearchInput value={kw} onChange={setKw} placeholder="搜索表名 / 注释" /></div>
            <span className="cell-muted" style={{ fontSize: 13 }}>共 {filtered.length} 张表</span>
          </div>
          <IconBtn name="download" size={17} bordered tip="导出列表" onClick={() => toast(`已导出 ${filtered.length} 条`, 'ok')} />
        </div>
        <Table columns={columns} data={filtered} density={tweaks.density} />
      </Card>

      {importOpen && <ImportModal onClose={() => setImportOpen(false)} onImport={onImport} />}
      {config && <ConfigDrawer table={config} onClose={() => setConfig(null)} />}
      {preview && <PreviewModal table={preview} onClose={() => setPreview(null)} />}
      {modal && modal.type === 'delete' &&
        <ConfirmModal title="删除代码生成表" danger message={<>确认删除表 <b>{modal.row.tableName}</b> 的生成配置吗？</>}
          confirmText="删除" onConfirm={() => { setList(l => l.filter(t => t.id !== modal.row.id)); toast('已删除', 'ok'); }} onClose={() => setModal(null)} />}
    </div>
  );
}

window.GeneratorPage = GeneratorPage;
