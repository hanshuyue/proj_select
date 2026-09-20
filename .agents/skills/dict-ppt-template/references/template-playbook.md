# DICT PPT generation playbook

## Architecture and source-of-truth rules

- The Vue form owns user selections and values. Save derived selection keys before persistence.
- `normalizeFinanceDetails` removes unselected modes and zero-value placeholders before totals or slides are built.
- The PPTX owns typography, spacing, geometry, labels, and merges. Copy styles from a nearby template run/cell.
- Template examples are placeholders. Remove unused examples rather than exporting blank or zero rows.
- Dynamic output comes from selected form data. Cached row keys are only a legacy fallback when group selection is absent.

## Dynamic tables and selected modes

- Detect tables by stable headers and column structure; fixed page numbers are a fallback.
- Resize to exactly header + selected rows + optional total. Clear stale DrawingML merges before row changes, then rebuild merges.
- Preserve form ordering and group by stable keys.
- 收益分析 filters by `benefitAnalysisSelectedModes`/`groupKey` and includes every visible metric in each selected group. Use `benefitAnalysisSelectedKeys` only for old records without group selection.
- 收益分析的主表始终先展示“项目整体”三项指标，再展示每个实际收益小类。`其他收入`不能作为最终分类：按非零明细名称拆分为专线、大视频、AI、自主集成等已定义小类；只有无法识别的非零明细才保留“其他收入”。
- Per-mode appendices clone one source slide per selected mode, remove copied custom-data relationships, fill mode-specific values, and assert one appendix per mode.
- Do not display zero-value finance details or unselected modes. Recalculate totals from surviving rows.

## Text, typography, and overflow

- Edit existing runs where possible. Keep label and value in separate runs.
- 项目信息情况 labels and values are bold; 甄选模式/甄选金额 are regular; ordinary values inherit template body typography.
- For multi-part sections, create/update one paragraph per classification instead of placing newline text in one run.
- Keep dynamic tables inside their original anchor rectangle, or move all following shapes together.
- Constrain long text above tables and use shrink-to-fit. Verify realistic long values.
- Remove empty optional paragraphs individually; never clear a whole shape because one paragraph is inapplicable.

## Template-specific rules

- 收支明细统一使用 `收支明细.xlsx` 的七列结构：支出区块为 A2:G21，收益区块为 B26:G50；保留“投资部分/成本部分”的两级模式、合并单元格、各模式小计和总计公式。网站 `costItems` / `incomeItems` 是唯一数据源，按模式顺序写入模板预留行，不能重建成通用的“类型、模式、明细”扁平表。
- PPT 必须直接呈现收支明细：标题含“投入情况”的页面显示支出表，标题含“收益情况”的页面显示收益表；完整七列表工作簿作为 Excel 对象嵌入投入情况页。新甄选模板通过 `含税支出`、`含税收益` 和 `总投入` 等可见文本语义识别这些插槽，不依赖固定页码。
- 网站输入金额与税率按原小数位写入 Excel，单元格使用与原输入小数位一致的数字格式；Excel 公式保留完整计算精度。PPT 表格和页面汇总统一四舍五入显示两位小数。

- Member metrics use one descriptive cell spanning the three period/growth columns. The website's last two field names do not match the legacy template labels (`成员数量份额` maps to the template's `V网成员`, and `V网成员数` maps to `全球通成员`), so map these four descriptive rows by stable row position. Clear merges, rebuild the merge first, then write only the surviving left-most cell; merging after writing can silently discard the updated value.
- “经‘七融解析’后” has a leading check mark.
- 能力供给条目使用“蓝色勾选符/类别标签 + 黑色回答”的分段 run；“经‘七融解析’后，”标签保持常规字重，解析结论为黑色正文。
- 项目交付页的“交付责任、合作伙伴、工期情况”每个小标题都必须重建蓝色方框，标题蓝色加粗，后面的回答保持模板黑色常规字重；大标题“七、项目交付”不额外添加方框。
- 附录4供应链金融合作 uses a blue square main heading and a black arrow-bullet subcategory. Keep the subcategories “若使用供应链金融” and “若未使用供应链金融”; populate only the selected category, keep its label bold and body regular in the same paragraph, and keep supplemental text separate.
- 供应链金融“未使用原因”可能是短语，也可能已经是完整句。完整句直接输出；短语才套用“该项目因…原因，未与银行合作…”句式，避免重复前后缀。
- 收付款页三个小标题统一带模板方框，收款/付款正文的每一条内容使用模板原生的蓝色 Wingdings 项目符号（字体 `Wingdings`、字符 `ü`），不能把 Unicode `✓` 直接拼进正文。兼容模板将收款与付款放在同一文本框或拆成两个独立文本框的两种结构，不能只在合并文本框分支添加符号。投资收入为零时移除投资部分两列和模板红色说明，只保留七列项目现金流表；历史记录残留的投资确认说明不能单独触发投资列。
- 收款方式是直接填写字段 `projectReceiptMethod`；付款方式先选择适用分类，再填写各分类的实际条款 `projectPaymentDetails[已选分类]`。分类名称和 placeholder 只是编辑提示，不能作为项目数据输出；某分类只选择但具体条款为空时，不得自动补模板示例、分类名或默认句子。旧记录的 `projectPaymentMethod` 仅在没有任何分类条款时作为后端兼容回退；若旧记录已有且仅有一个有效分类，前端加载时把旧文本迁移到该分类条款，不能将旧文本误当成分类，也不能同时输出新旧字段造成重复。
- 决策事项先选择商务模式，并按模式生成“合作服务/购销模式投入成本”“投资模式投资”或“受托代销应付账款”。金额字段可能已经包含“万元”，拼句前先去掉尾部单位；其他业务收入为空时按 0 输出，结尾统一使用句号。标后甄选结果问题只在“需要一同决策”被选中时追加，且保持问句，不输出表单选择值作为决策结论。

## Template replacement checklist

1. Locate source slides by visible titles and confirm slide count.
2. Inventory shape types, anchors, marker text, table dimensions/merges, and representative run typography.
3. Compare service markers with the new template before changing indexes.
4. Verify optional-slide removal and appendix insertion order.
5. Confirm cloned slides have no invalid `p:custDataLst` relationships or duplicate shape IDs.
6. Generate zero/one/many rows, one/many modes, government/enterprise branches, and long text.
7. Render/open affected slides and inspect at normal zoom.
8. Confirm the new template contains editable native tables with `含税支出` and `含税收益` headers, and a package/OLE relationship on the `总投入` slide for the complete `收支明细.xlsx` workbook.

## Known failures

- Missing 收益分析 rows: cached row keys overrode selected groups. Filter by group and include all metrics.
- Incorrect 收益分析 summary: only top-level revenue modes were mapped, so `ICT自主集成收入` appeared as generic “其他收入” and the overall group disappeared. Always include the overall group and derive known subcategories from non-zero revenue detail names.
- Zero rows: named placeholders with amount zero passed filtering. Filter before subtotals.
- Broken groups: stale `rowSpan`/`vMerge` survived row removal. Clear and rebuild merges.
- Missing 甲方 text: whole multi-paragraph shapes were cleared. Mutate matching paragraphs only.
- POI `XmlValueDisconnectedException`: paragraph XML was removed and cached wrappers were read afterward. Finish all paragraph reads/writes first, then remove empty paragraph XML once.
- The same exception also occurs when reading font properties from a run after `shape.clearText()`. Snapshot family, size, color, and weight into ordinary values before clearing.
- Overlap: text outgrew its anchor or tables expanded without reflow. Shrink/constrain text and retain table geometry.
- 甲方往期履约表增加数据行后，PowerPoint 会按单元格最小高度扩张表格；仅调用 `setAnchor`/平均分配行高不足以避免遮挡。应删除没有数据的“以房抵债/非对称付款”区块，并把表后说明、风险标题和风险正文作为一个纵向布局整体重新定位。
- 收付款页不能把标题和正文都复制为模板标题格式。分别创建标题 run（蓝色加粗）与正文 run（黑色常规），固定上方文本区域、现金流表和页脚说明的三个不相交区域，再启用文本自适应。
- 新模板把“项目收款方式”和“项目付款方式”拆成独立文本框时，旧的单框判断会走 `setTitleAndBody` 分支并丢失方框/对号。单框和双框结构都应调用统一的符号化段落写入方法，并按换行拆分正文，为每行分别添加对号。
- 直接把字符 `✓` 拼到收付款正文中会使用正文或回退字体，形状、缩进和颜色都与模板不一致。模板的对号实际是 Wingdings 项目符号 `ü`；生成时应重建段落项目符号属性，并让正文保持微软雅黑黑色常规字重。
- 付款方式曾同时保留分类条款和旧版自由填写字段，生成时会把两者追加后造成重复。新版表单只显示“选择付款类型 + 填写该类型条款”；后端优先输出已选分类的实际条款，仅在分类条款全部为空时回退旧字段。
- 合作伙伴页的小分类标题和对应正文必须写在同一段的两个 run 中，避免 PowerPoint 自动换行后看起来像无关内容混排；只生成当前适用的分类。
- 风险评估摘要在模板中含示例换行/空项目符号；只改首段会留下第二个空方框。重建摘要文本框为单一非项目符号段落，并把模板方框作为普通字符写入。
- Wrong font: new runs inherited theme defaults. Copy the nearest template run, then apply explicit weight rules.
- Repaired PPTX: cloned metadata referenced missing relationships or IDs collided. Remove custom data and normalize IDs.
- PowerPoint repair dialog after dynamic appendix cloning: POI `importContent()` can copy a malformed notes page and also leave an invalid direct `PresentationPart -> NotesSlidePart` relationship. Dynamic appendix pages do not need speaker notes, so remove their copied notes entirely, then remove every notes-slide relationship from the presentation package part before writing. Keep valid notes on original template slides. Assert the root-relationship invariant after the byte-array round trip. POI can reopen the invalid package, so POI-only validation will miss this failure; verify a real multi-mode export with desktop PowerPoint.

## Verification

- In `scaffold-vue`: `npm run build`.
- In `scaffold-system`: `mvn -pl scaffold-system -am clean compile`.
- For generation changes, run the template-backed test with `-Ddict.template.path=<absolute-template-path>` and visually inspect affected slides.
- A POI round-trip only proves that POI can parse its own output. On Windows, also open a generated test deck through PowerPoint COM in read-only/hidden mode; a successful open without a repair dialog is the stronger OOXML compatibility check.
