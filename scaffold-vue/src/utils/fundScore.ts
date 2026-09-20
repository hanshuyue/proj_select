export interface FundItemOption { label: string; value: string; score: number }
export interface FundHistoryRow { label: string; item: string; deduction: string; options: FundItemOption[]; multiple?: boolean }
export interface FundCurrentRow { label: string; sequence: number; requirement: string; item: string; deduction: string; aggregateKey: string; options: FundItemOption[] }

const option = (value: string, score: number, label = value): FundItemOption => ({ label: `${label}（${score ? `扣${score}分` : '不扣分'}）`, value, score })
const noRisk = (label = '不涉及'): FundItemOption => option(label, 0)

export const fundHistoryRows: FundHistoryRow[] = [
  { label: '行业属性分析', item: 'fundHistoryIndustryItem', deduction: 'fundHistoryIndustryDeduction', options: [
    option('教育', 25), option('工业能源', 18.22), option('党政', 17.91), option('交通', 15.51), option('互联网', 14.95),
    option('农商', 14.87), option('执法融合', 12.78), option('医疗', 9.99), option('金融', 2.5),
  ] },
  { label: '客户类型分析', item: 'fundHistoryCustomerItem', deduction: 'fundHistoryCustomerDeduction', options: [
    option('民营企业', 25), option('区县政府及以下或相关（含区县城投）', 21.4), option('事业单位（含学校、医院）', 19.12),
    option('央、国企', 8.9), option('市级及以上政府或相关（含市级城投）', 2.5),
  ] },
  { label: '资金来源分析（多选取平均值）', item: 'fundHistorySourceItem', deduction: 'fundHistorySourceDeduction', multiple: true, options: [
    option('客户自筹资金', 50), option('区县财政（计划内预算）', 49.47), option('财政资金（计划外追加预算）', 49.15),
    option('企业自行募集债券', 47.22), option('专项债', 36.83), option('国债', 35.6), option('奖补资金', 33.48),
    option('市级财政（计划内预算）', 31.2), option('中央财政（计划内预算）', 24.42), option('省级财政（计划内预算）', 20.83),
    option('承兑汇票', 20.48), option('其他（JD、银校合作）', 5),
  ] },
]

const currentRow = (label: string, sequence: number, requirement: string, key: string, aggregateKey: string, options: FundItemOption[]): FundCurrentRow => ({
  label, sequence, requirement, item: `${key}Item`, deduction: `${key}Deduction`, aggregateKey, options,
})
const auditRequirements = [
  '该单位曾因拖欠税款被列入欠税公告名单、因借款被起诉。',
  '该单位曾因企业公示信息弄虚作假被列入经营异常名录、因买卖合同纠纷被起诉。',
  '该单位曾因多起劳务纠纷被起诉。', '该单位存在股权、高管频繁变动。',
]
const asymmetricOptions = [noRisk(), option('1至2次', 20, '起诉非对称支付1至2次'), option('3次及以上', 30, '起诉非对称支付3次及以上')]
const arrearsOptions = [
  noRisk('无欠费或付款比例为100%'), option('单项目付款比例80%-99%', 10), option('单项目付款比例60%-80%', 15),
  option('单项目付款比例30%-60%', 20), option('多项目欠费100万-300万', 10), option('多项目欠费300万-500万', 15),
  option('多项目欠费500万及以上', 20),
]

export const enterpriseFundCurrentRows: FundCurrentRow[] = [
  ...auditRequirements.map((requirement, index) => currentRow('审计审查自查', index + 1, requirement, `fundEnterpriseAudit${index + 1}`, 'fundCurrentAuditDeduction', [noRisk(), option('涉及', 5)])),
  currentRow('历史项目自查', 1, '后向在全省范围内起诉非对称支付；多个后向次数累加。', 'fundEnterpriseHistory1', 'fundCurrentHistoryDeduction', asymmetricOptions),
  currentRow('历史项目自查', 2, '甲方与我公司合作过程中以房抵债。', 'fundEnterpriseHistory2', 'fundCurrentHistoryDeduction', [noRisk(), option('1至2次', 10), option('3次及以上', 20)]),
  currentRow('历史项目自查', 3, '甲方往期履约存在单项目或多项目欠费；同时触发时就高、不重复累计。', 'fundEnterpriseHistory3', 'fundCurrentHistoryDeduction', arrearsOptions),
  currentRow('资金来源自查', 1, '资金来源不明确，或无法提供充分来源证明。', 'fundEnterpriseSource1', 'fundCurrentSourceDeduction', [noRisk(), option('无法提供充分来源证明', 5), option('资金来源不明确', 10)]),
  currentRow('资金来源自查', 2, '项目预算未覆盖全成本或跨界使用预算。', 'fundEnterpriseSource2', 'fundCurrentSourceDeduction', [noRisk(), option('涉及', 10)]),
  currentRow('资金来源自查', 3, '支付节点不合理或存在付款倒挂。', 'fundEnterpriseSource3', 'fundCurrentSourceDeduction', [noRisk(), option('涉及', 10)]),
  currentRow('相对方评价自查', 1, '相对方信用评估结果。', 'fundEnterpriseCounterparty1', 'fundCurrentCounterpartyDeduction', [option('低风险或不涉及', 0), option('中风险', 10), option('高风险', 20)]),
]

export const governmentFundCurrentRows: FundCurrentRow[] = [
  currentRow('历史项目自查', 1, '后向在全省范围内起诉非对称支付；多个后向次数累加。', 'fundGovernmentHistory1', 'fundCurrentHistoryDeduction', asymmetricOptions),
  currentRow('历史项目自查', 2, '甲方与我公司合作过程中以房抵债。', 'fundGovernmentHistory2', 'fundCurrentHistoryDeduction', [noRisk(), option('1至2次', 20), option('3次及以上', 30)]),
  currentRow('历史项目自查', 3, '甲方往期履约存在单项目或多项目欠费；同时触发时就高、不重复累计。', 'fundGovernmentHistory3', 'fundCurrentHistoryDeduction', arrearsOptions),
  currentRow('资金来源自查', 1, '资金来源不明确，或无法提供充分来源证明。', 'fundGovernmentSource1', 'fundCurrentSourceDeduction', [noRisk(), option('无法提供充分来源证明', 10), option('资金来源不明确', 20)]),
  currentRow('资金来源自查', 2, '项目预算未覆盖全成本或跨界使用预算。', 'fundGovernmentSource2', 'fundCurrentSourceDeduction', [noRisk(), option('涉及', 10)]),
  currentRow('资金来源自查', 3, '支付节点不合理或存在付款倒挂。', 'fundGovernmentSource3', 'fundCurrentSourceDeduction', [noRisk(), option('涉及', 10)]),
  currentRow('负债情况自查', 1, '甲方所在区县欠费率排名前20。', 'fundGovernmentDebt1', 'fundCurrentDebtDeduction', [noRisk(), option('排名前20', 20)]),
]

export const fundScoreOptions = [...new Set([...Array.from({ length: 101 }, (_, value) => value), 2.5, 8.9, 9.99, 12.78, 14.87, 14.95, 15.51, 17.91, 18.22, 19.12, 20.48, 20.83, 21.4, 24.42, 31.2, 33.48, 35.6, 36.83, 47.22, 49.15, 49.47])].sort((left, right) => left - right)
export const fundBonusOptions: FundItemOption[] = [
  { label: '无特殊加分', value: '无特殊加分', score: 0 },
  { label: '甲方合作项目均按合同约定付款或有提前付款历史（加5分）', value: '甲方合作项目均按合同约定付款或有提前付款历史', score: 5 },
  { label: '与我司签署战略合作协议（省公司领导出席签署）（加10分）', value: '与我司签署战略合作协议', score: 10 },
  { label: '项目列入省级或其他重点工程等情况（加10分）', value: '项目列入省级或其他重点工程等情况', score: 10 },
  { label: '项目为国家重点扶持大项目等特殊情况（加20分）', value: '项目为国家重点扶持大项目等特殊情况', score: 20 },
  { label: '相对方信用评估为低风险（加5分）', value: '相对方信用评估为低风险', score: 5 },
]

export function mappedFundScore(options: FundItemOption[], value: unknown) { return options.find(item => item.value === value)?.score }
export function mappedFundAverageScore(options: FundItemOption[], value: unknown) {
  const selected = Array.isArray(value) ? value : value ? [value] : []
  const scores = selected.map(item => mappedFundScore(options, item)).filter((item): item is number => item != null)
  return scores.length ? Math.round((scores.reduce((total, item) => total + item, 0) / scores.length) * 100) / 100 : undefined
}
const score = (value: unknown) => Number(value || 0) || 0
const rounded = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100

export function calculateFundScores(sections: Record<string, unknown>, projectType: 'GOVERNMENT' | 'ENTERPRISE' | string) {
  const historyDeduction = rounded(fundHistoryRows.reduce((total, row) => total + score(sections[row.deduction]), 0))
  const currentRows = projectType === 'GOVERNMENT' ? governmentFundCurrentRows : enterpriseFundCurrentRows
  const hasDetailedCurrentValues = currentRows.some(row => sections[row.item] != null && sections[row.item] !== '')
  const currentDeduction = hasDetailedCurrentValues
    ? currentRows.reduce((total, row) => total + score(sections[row.deduction]), 0)
    : [...new Set(currentRows.map(row => row.aggregateKey))].reduce((total, key) => total + score(sections[key]), 0)
  const history = rounded(Math.max(0, 100 - historyDeduction))
  const current = rounded(Math.max(0, 100 - currentDeduction))
  const bonus = score(sections.fundSpecialBonusScore)
  const finalScore = rounded(Math.min(100, history * 0.4 + current * 0.6 + bonus))
  const riskLevel = finalScore < 60 ? '高' : finalScore < 80 ? '中' : '低'
  const riskResult = finalScore < 60 ? '谨慎拓展' : finalScore < 80 ? '适当拓展' : '拓展'
  return { historyDeduction, currentDeduction, history, current, bonus, finalScore, riskLevel, riskResult }
}
