export type EconomicBaseSection = 'INCOME' | 'INVESTMENT' | 'COST'

export interface EconomicBaseDetailDefinition {
  key: string
  section: EconomicBaseSection
  sectionLabel: string
  group: string
  item: string
  taxRate: number | null
  description: string
  depreciationCategory?: 'platform' | 'transmission' | 'fiberPipeline' | 'software' | 'other'
}

export interface EconomicBaseDetailValue {
  ctIncTax?: number | null
  itIncTax?: number | null
  taxRate?: number | null
}

export type EconomicBaseDetailValues = Record<string, EconomicBaseDetailValue>

const income = (key: string, item: string, taxRate: number | null, description: string): EconomicBaseDetailDefinition => ({
  key, section: 'INCOME', sectionLabel: '直接收入', group: '项目收入（万元）', item, taxRate, description,
})
const investment = (key: string, group: string, item: string, taxRate: number | null, description: string, depreciationCategory: EconomicBaseDetailDefinition['depreciationCategory']): EconomicBaseDetailDefinition => ({
  key, section: 'INVESTMENT', sectionLabel: '直接支出', group, item, taxRate, description, depreciationCategory,
})
const cost = (key: string, group: string, item: string, taxRate: number | null, description: string): EconomicBaseDetailDefinition => ({
  key, section: 'COST', sectionLabel: '直接支出', group, item, taxRate, description,
})

export const economicBaseDetailDefinitions: EconomicBaseDetailDefinition[] = [
  income('incomeIntegration', '1）集成费', 6, '系统集成方案咨询、设计、软硬件安装、通信工程服务、调测开通等一次性项目实施服务费。'),
  income('incomeMaintenance', '2）维保费', 6, 'ICT项目建成后提供维护保障服务而收取的维保费。'),
  income('incomeTelecomBasic', '3）通信业务收入（基础电信服务）', 9, '语音、短彩信、无线上网、专线、物联网等通信服务费。'),
  income('incomeTelecomValueAdded', '4）通信业务收入（增值电信服务）', 6, 'ICT项目建成后客户使用互联网业务等产生的通信服务费。'),
  income('incomeFunction', '5）功能费收入', 6, '标准化集团信息化产品按月产生的功能费收入，如视频监控等。'),
  income('incomeEquipmentLease', '6）设备租赁收入', 13, 'ICT项目中出租设备产生的收入。'),
  income('incomeEquipmentSale', '7）设备（含第三方软件）销售收入', 13, 'ICT项目中销售设备及相关第三方软件产生的收入。'),
  income('incomeTerminalSale', '8）终端（含第三方软件）销售收入', 13, 'ICT项目中销售终端及相关第三方软件产生的收入。'),
  income('incomeTerminalLease', '9）终端租赁收入', 13, 'ICT项目中出租终端产生的收入。'),
  income('incomeOther', '10）其他收入', null, '项目带来的其他收入。'),

  investment('investmentPlatformHardware', '1、平台类投资', '1）硬件设备类投资', 13, '项目建成后资产归属于我公司的平台设备采购投资。', 'platform'),
  investment('investmentPlatformSoftware', '1、平台类投资', '2）定制软件类投资', 6, '项目建成后资产归属于我公司的平台定制软件采购投资。', 'software'),
  investment('investmentPlatformIntegration', '1、平台类投资', '3）集成服务费', 6, '资产归属于我公司的平台软硬件设备集成费用。', 'platform'),
  investment('investmentPlatformSupporting', '1、平台类投资', '4）工程配套费', null, '平台软硬件设备所需线缆、走线架等配套费用。', 'platform'),
  investment('investmentPlatformInstallation', '1、平台类投资', '5）建安费用', null, '平台软硬件设备建设安装工程费。', 'platform'),
  investment('investmentPlatformDesign', '1、平台类投资', '6）设计监理费', 6, '投资项目相关的设计、可研及监理费用。', 'platform'),
  investment('investmentTransmissionPipeline', '2、传输网络配套投资', '1）管道类投资', 13, '末端接入需要的管道、杆路等主材、配套和工程服务投资。', 'fiberPipeline'),
  investment('investmentTransmissionCable', '2、传输网络配套投资', '2）光缆类投资', 13, '末端接入需要的光缆等主材、配套和工程服务投资。', 'fiberPipeline'),
  investment('investmentTransmissionEquipment', '2、传输网络配套投资', '3）设备类投资', 13, '末端接入需要的传输主设备、配套和工程服务投资。', 'transmission'),
  investment('investmentOtherConstruction', '3、其他网络配套投资', '其他建设费用（如有需按税率区分）', null, '项目建设中涉及的其他网络配套投资。', 'other'),

  cost('costConstructionEquipment', '1、建设费用', '1）设备采购支出', 13, '项目建成后资产不归属于我公司的平台设备采购支出。'),
  cost('costConstructionIntegration', '1、建设费用', '2）集成建设费用', 6, '完成系统集成方案所需的项目集成建设费用。'),
  cost('costConstructionBid', '1、建设费用', '3）中标服务费', 6, '招标代理机构提供招标、评标、定标及合同协调等服务收取的费用。'),
  cost('costConstructionInstallation', '1、建设费用', '4）建安费用', 9, '建设安装工程费。'),
  cost('costConstructionOther', '1、建设费用', '5）其他建设费用', null, '项目建设过程中的其他费用。'),
  cost('costOperationMaintenance', '2、运行维护费用', '1）维护费用', 6, '运营维护过程中发生的设备维护支出及费用。'),
  cost('costOperationRenewal', '2、运行维护费用', '2）设备更新费用', 13, '项目运行过程中设备更新发生的支出及费用。'),
  cost('costOperationOther', '2、运行维护费用', '3）其他运行费用（电费、耗材等）', null, '电费、耗材、场租、水费等其他运行费用。'),
  cost('costDirectMarketing', '3、其他直接支出', '1）项目营销成本', 6, '项目营销相关费用。'),
  cost('costDirectCommission', '3、其他直接支出', '2）代理渠道酬金支出', 6, '项目合作代理渠道的酬金支出。'),
  cost('costDirectTelecomSettlement', '3、其他直接支出', '3）通信业务结算支出', 6, '专线、语音、短彩信、物联网、互联网等通信业务的结算成本。'),
  cost('costDirectOther', '3、其他直接支出', '4）其他直接支出', null, '其他项目建设、运行和维护过程中的成本。'),
  cost('costNetworkAllocation', '4、专线类项目对大网资源的分摊成本', '大网资源分摊成本', null, 'ICT项目占用传输资源的上层网络资源分摊成本。'),
  cost('costGroupProduct', '5、集团信息化产品成本', '集团信息化产品成本', null, '按相关业务管理办法计算。'),
]

const numberValue = (value: unknown) => Number.isFinite(Number(value)) ? Math.max(0, Number(value)) : 0
export function normalizeEconomicBaseDetailValues(values: EconomicBaseDetailValues | null | undefined): EconomicBaseDetailValues {
  const result: EconomicBaseDetailValues = { ...(values || {}) }
  for (const definition of economicBaseDetailDefinitions) {
    result[definition.key] = {
      ctIncTax: result[definition.key]?.ctIncTax ?? null,
      itIncTax: result[definition.key]?.itIncTax ?? null,
      taxRate: definition.taxRate ?? result[definition.key]?.taxRate ?? null,
    }
  }
  return result
}

export function calculateEconomicBaseRow(definition: EconomicBaseDetailDefinition, values: EconomicBaseDetailValues) {
  const value = values[definition.key] || {}
  const ctIncTax = numberValue(value.ctIncTax)
  const itIncTax = numberValue(value.itIncTax)
  const taxRate = definition.taxRate ?? numberValue(value.taxRate)
  const taxRateDecimal = taxRate / 100
  // Exact workbook formulas: F/(1+I)*I and G/(1+I)*I.
  const ctVat = ctIncTax / (1 + taxRateDecimal) * taxRateDecimal
  const itVat = itIncTax / (1 + taxRateDecimal) * taxRateDecimal
  const contractIncTax = ctIncTax + itIncTax
  return {
    ctIncTax,
    itIncTax,
    contractIncTax,
    taxRate,
    ctVat,
    itVat,
    amountExTax: contractIncTax - ctVat - itVat,
  }
}

export function calculateEconomicBaseDetailTotals(values: EconomicBaseDetailValues) {
  const totals = {
    INCOME: { ctIncTax: 0, itIncTax: 0, contractIncTax: 0, ctVat: 0, itVat: 0, amountExTax: 0 },
    INVESTMENT: { ctIncTax: 0, itIncTax: 0, contractIncTax: 0, ctVat: 0, itVat: 0, amountExTax: 0 },
    COST: { ctIncTax: 0, itIncTax: 0, contractIncTax: 0, ctVat: 0, itVat: 0, amountExTax: 0 },
  }
  const investmentCategories = { platform: 0, transmission: 0, fiberPipeline: 0, software: 0, other: 0 }
  for (const definition of economicBaseDetailDefinitions) {
    const row = calculateEconomicBaseRow(definition, values)
    const total = totals[definition.section]
    for (const key of Object.keys(total) as Array<keyof typeof total>) total[key] += row[key]
    if (definition.section === 'INVESTMENT' && definition.depreciationCategory) investmentCategories[definition.depreciationCategory] += row.amountExTax
  }
  return {
    sections: totals,
    investmentCategories,
    estimatedInvestment: totals.INVESTMENT.amountExTax,
    directRevenue: totals.INCOME.amountExTax,
    operatingExpense: totals.COST.amountExTax,
  }
}
