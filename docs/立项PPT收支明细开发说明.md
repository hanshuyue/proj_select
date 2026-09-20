# 立项 PPT 收支明细开发说明

## 目标

立项表单是收支数据的唯一来源。系统保存项目后，生成与 `收支明细.xlsx` 一致的完整工作簿，并在 PPT 中直接展示支出和收益明细。

## 数据来源与筛选

- 支出读取项目根节点的 `costItems`。
- 收益读取项目根节点的 `incomeItems`。
- 支出模式由 `sections.costPrimaryModes` 和 `sections.cooperationCostModes` 控制。
- 收益模式由 `sections.revenueModes` 控制。
- 不导出未选择模式、空名称、小计/合计持久化行和零金额模板占位行。
- 旧数据没有选择字段时，兼容已有的有效明细。

## Excel 模板契约

资源文件：`scaffold-system/scaffold-system/src/main/resources/templates/initiation-finance-detail-workbook.xlsx`。

- 工作表必须包含 `sheet1`。
- 支出标题为 `支出`，表头和明细位于 A2:G21。
- 收入标题为 `收入`，表头和明细位于 B26:G50。
- 七列依次表达一级模式、二级模式、明细、含税金额、税率、不含税金额和说明。
- 必须保留原模板的合并单元格、列宽、字体、边框和底色。
- 不含税金额使用 `含税金额/(1+税率)`；小计和合计按模板模式边界求和。
- 金额和税率按照网站原输入的小数位设置 Excel 数字格式，不统一截断为两位。

## PPT 模板契约

- 标题含“投入情况”的页面必须包含原生可编辑表格，表头包含 `含税支出`，用于呈现 `costItems`。
- 标题含“收益情况”的页面必须包含原生可编辑表格，表头包含 `含税收益`，用于呈现 `incomeItems`。
- 标题或摘要包含 `总投入` 的页面必须包含一个 Excel package/OLE 对象。生成时只替换其工作簿内容，保持模板中的位置和尺寸。
- PPT 中金额统一显示两位小数；完整精度保存在嵌入 Excel 中。
- 明细行按网站顺序生成，模板示例行必须删除或覆盖；行数不足时动态增加 PPT 表格行。

## 新甄选模板接入检查

1. 用可见标题确认投入情况页和收益情况页。
2. 确认两个页面分别存在 `含税支出`、`含税收益` 语义表头。
3. 确认投入情况页存在 Excel package/OLE 关系。
4. 使用包含多个支出模式、多个收益模式和长说明的项目生成 PPT。
5. 检查 PPT 表格为两位小数，嵌入 Excel 保留原输入位数。
6. 使用桌面 PowerPoint 只读打开生成文件，确认没有修复提示。

## 主要实现

- `InitiationPptService.normalizeFinanceDetails`：按网站选择清理明细并计算完整精度的不含税金额。
- `InitiationPptService.fillFinanceTable`：填充 PPT 原生支出/收益表并以两位小数显示。
- `InitiationPptService.fillFullFinanceDetailSheet`：按原七列表结构填充完整 Excel。
- `InitiationPptService.replaceEmbeddedWorkbooks`：将完整收支工作簿写入投入情况页的嵌入对象。
