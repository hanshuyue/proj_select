export interface AttachmentRequirement {
  type: string
  label: string
  page: string
  hint: string
  imageOnly?: boolean
}

const entry = (type: string, label: string, page: string, hint: string, imageOnly = false): AttachmentRequirement => ({ type, label, page, hint, imageOnly })

// Page numbers refer to the 31-page source template, before optional pages are removed.
export const initiationAttachmentCatalog = [
  entry('TENDER_DOCUMENT', '招标文件', '2', '投标获取项目时提供'),
  entry('AWARD_NOTICE', '中标通知书', '2', '投标获取项目时提供'),
  entry('REQUESTER_CONTRACT_FILE', '甲方与实际需求方的合同或中标通知书', '2', '甲方不是实际项目需求方时，勾选本项目涉及'),
  entry('ARCHITECTURE_DIAGRAM', '系统架构图', '4', '标注感知层、传输层、平台层、应用层及自有能力', true),
  entry('INVESTMENT_NECESSITY_FILE', '投资必要性说明及支撑材料', '6', '使用云网ICT或标准化产品投资时，可补充说明附件'),
  entry('NONSTANDARD_PROCUREMENT_FILE', '一采非标报备及采购依据', '6–7', '涉及非标采购时，提供报备材料、客户指定参数等'),
  entry('PROCUREMENT_DIFFERENCE_FILE', '集采产品差异性对比附件', '7', '网站表格可填写；补充供应商参数、客户指定依据等'),
  entry('PROCUREMENT_REVIEW_FILE', '一采非标报备合理性评审材料', '7', '涉及评审时提供相关部门评估意见'),
  entry('IDC_CALCULATION_FILE', 'IDC测算原始支撑材料', '8', '按需提供原始测算依据，网站填写表格不需重复上传'),
  entry('BENEFIT_CALCULATION_FILE', '收益测算补充材料', '10–14', '系统自动生成测算表；仅补充外部依据时上传'),
  entry('BENEFIT_APPROVAL_FILE', '效益未达标审批材料', '10', '三化等效益低于对应标准且需要审批时勾选'),
  entry('ADVANCE_REVIEW_STRATEGY_IMAGE', '垫资评审：战略卡位场景', '15', '上传需写入PPT的评审图片', true),
  entry('ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE', '垫资评审：集团优单标准', '15', '上传需写入PPT的评审图片', true),
  entry('ADVANCE_REVIEW_CAPABILITY_IMAGE', '垫资评审：能力沉淀要求', '15', '上传需写入PPT的评审图片', true),
  entry('ADVANCE_REVIEW_ATTACHMENT', '垫资项目打分签字表及评审附件', '15', '包含省公司综合评估打分签字表'),
  entry('SELECTION_REQUIREMENT_FILE', '甄选需求及方案决策材料', '16', '甄选方案、需求及决策依据'),
  entry('SELECTION_PROCESS_FILE', '甄选过程材料', '16', '应答、评审记录及推荐意见'),
  entry('SELECTION_RESULT_FILE', '甄选结果及公示材料', '16', '甄选结果决策、公示等'),
  entry('CLIENT_DUE_DILIGENCE_FILE', '客户尽责调查表', '17', '模板明确要求附客户尽责调查表'),
  entry('CLIENT_PERFORMANCE_FILE', '甲方往期履约及风险核查证明', '18', '按需补充欠费、诉讼、以房抵债等核查依据'),
  entry('FUND_BUDGET_PUBLIC_FILE', '政府部门预算公开文件', '19–20', '财政计划内预算证明'),
  entry('FUND_EXPENDITURE_SUBJECT_FILE', '项目对应支出科目', '19–20', '项目支出预算情况表或政府采购预算表'),
  entry('FUND_PERFORMANCE_TARGET_FILE', '项目支出绩效目标表', '19–20', '对应项目的截图或文档'),
  entry('FUND_PROCUREMENT_FILE', '政府采购单等', '19–20', '政采招标网站对应项目材料'),
  entry('FUND_LEADER_APPROVAL_FILE', '市政府领导签批材料', '19–20', '财政计划外追加预算'),
  entry('FUND_USER_APPROVAL_FILE', '使用单位申请和审批材料', '19–20', '财政计划外追加预算'),
  entry('FUND_FINANCE_APPROVAL_FILE', '财政局审批材料', '19–20', '财政计划外追加预算'),
  entry('FUND_BOND_APPLICATION_FILE', '债券申报书', '19–20', '专项债、国债、企业债等对应申报材料'),
  entry('FUND_IMPLEMENTATION_PLAN_FILE', '项目实施方案', '19–20', '债券资金对应项目实施方案'),
  entry('FUND_BOND_ISSUANCE_FILE', '债券发行情况', '19–20', '债券资金发行证明'),
  entry('FUND_BANK_STATEMENT_FILE', '资金到账或银行流水', '19–20', '债券资金到账或自筹资金流水'),
  entry('FUND_SUBSIDY_ALLOCATION_FILE', '奖补资金分配通知或方案', '19–20', '奖补资金对应的分配材料'),
  entry('FUND_SUBSIDY_APPLICATION_FILE', '奖补申报或准入材料', '19–20', '项目相关的奖补申报或准入证明'),
  entry('FUND_ACCOUNT_BALANCE_FILE', '客户账户余额证明', '19–20', '自筹资金账户余额'),
  entry('FUND_INTERNAL_APPROVAL_FILE', '客户内部立项文件', '19–20', '内部公文、立项采购文件、预算凭据等'),
  entry('FUND_FINANCIAL_STRENGTH_FILE', '客户资金实力及支付能力证明', '19–20', '资金实力查询、固定资产清单、支付能力评估等'),
  entry('FUND_ANALYSIS_MODEL_FILE', '资金分析模型及其他证明', '19–20', '网站模型自动计算；外部分析或其他资金类型按需补充'),
  entry('FUND_BONUS_PROOF_FILE', '特殊情况加分证明', '19–20', '选择加分项时提供对应证明材料'),
  entry('VENDOR_CREDIT_CHINA_FILE', '后向厂家信用中国截图', '21', '覆盖项目涉及的后向厂家'),
  entry('VENDOR_PAST_PERFORMANCE_FILE', '后向厂家往期合作履约情况', '21', '覆盖项目涉及的后向厂家'),
  entry('VENDOR_QUALIFICATION_FILE', '后向厂家相关资质证明', '21', '资质、代理级别、授权等'),
  entry('VENDOR_RISK_PROOF_FILE', '后向厂家经营风险证明截图', '21', '经营风险核查相关证明'),
  entry('RISK_ASSESSMENT_FILE', '风险评估补充附件', '23', '网站表格可填写；如有外部评估附件，可补充'),
  entry('DELIVERY_SOLUTION_FILE', '特殊交付场景解决方案', '25', '存在难以交付的特殊场景时勾选'),
  entry('MAINTENANCE_LIST_FILE', '详细维护清单', '27', '维护内容较多、需要附件说明时勾选'),
  entry('CITY_DECISION_FILE', '市公司决策签报或会议纪要', '28', '需体现法务人员意见'),
  entry('PROVINCE_DECISION_FILE', '省公司招投标决策意见材料', '28', '省公司相关决策意见依据'),
  entry('SELECTION_RECHECK_IMAGE', '商机管理平台甄选结果复核通过截图', '30', '模板明确要求附复核通过截图'),
  entry('APPRAISAL_SUPPORT_FILE', '硬件设备及相关服务费评估表', '31', '符合审价议价项目特征时提供'),
]

export function requiredInitiationAttachments(sections: Record<string, any>): AttachmentRequirement[] {
  const types = new Set<string>(['ARCHITECTURE_DIAGRAM', 'CLIENT_DUE_DILIGENCE_FILE'])
  const add = (...values: string[]) => values.forEach(value => types.add(value))
  if (String(sections.acquisitionMethod || '').includes('投标')) add('TENDER_DOCUMENT', 'AWARD_NOTICE')
  for (const type of sections.attachmentRequirementSelections || []) types.add(type)
  for (const type of sections.fundProofMaterialSelections || []) types.add(type)
  if (sections.procurementComparisonEnabled) add('NONSTANDARD_PROCUREMENT_FILE')
  if (sections.advancePaymentReviewEnabled) add('ADVANCE_REVIEW_STRATEGY_IMAGE', 'ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE', 'ADVANCE_REVIEW_CAPABILITY_IMAGE', 'ADVANCE_REVIEW_ATTACHMENT')
  if (sections.selectionCompany || sections.selectionMode || sections.selectionWinners) add('SELECTION_REQUIREMENT_FILE', 'SELECTION_PROCESS_FILE', 'SELECTION_RESULT_FILE')
  if ((sections.vendorRiskRows || []).some((row: any) => row.vendorName || Number(row.spendAmount))) add('VENDOR_CREDIT_CHINA_FILE', 'VENDOR_PAST_PERFORMANCE_FILE', 'VENDOR_QUALIFICATION_FILE', 'VENDOR_RISK_PROOF_FILE')
  if (Number(sections.fundSpecialBonusScore) > 0) add('FUND_BONUS_PROOF_FILE')
  if (sections.preDecisionOpinionsEnabled) add('CITY_DECISION_FILE', 'PROVINCE_DECISION_FILE')
  if (sections.partnerAppendixEnabled) add('SELECTION_RECHECK_IMAGE')
  if (sections.appraisalEnabled) add('APPRAISAL_SUPPORT_FILE')
  const funding = String(sections.fundType || sections.fundingSource || '')
  if (/计划外|追加预算/.test(funding)) add('FUND_LEADER_APPROVAL_FILE', 'FUND_USER_APPROVAL_FILE', 'FUND_FINANCE_APPROVAL_FILE')
  else if (/债/.test(funding)) add('FUND_BOND_APPLICATION_FILE', 'FUND_IMPLEMENTATION_PLAN_FILE', 'FUND_BOND_ISSUANCE_FILE', 'FUND_BANK_STATEMENT_FILE')
  else if (/奖补/.test(funding)) add('FUND_SUBSIDY_ALLOCATION_FILE', 'FUND_SUBSIDY_APPLICATION_FILE')
  else if (/自筹/.test(funding)) add('FUND_BANK_STATEMENT_FILE', 'FUND_ACCOUNT_BALANCE_FILE', 'FUND_INTERNAL_APPROVAL_FILE', 'FUND_FINANCIAL_STRENGTH_FILE')
  else if (/财政/.test(funding)) {
    add('FUND_BUDGET_PUBLIC_FILE', 'FUND_EXPENDITURE_SUBJECT_FILE', 'FUND_PROCUREMENT_FILE')
    if (sections.fundProjectType === 'GOVERNMENT') add('FUND_PERFORMANCE_TARGET_FILE')
  }
  if (sections.fundProjectType === 'ENTERPRISE') add('FUND_INTERNAL_APPROVAL_FILE', 'FUND_FINANCIAL_STRENGTH_FILE')
  return initiationAttachmentCatalog.filter(item => types.has(item.type))
}

export function missingInitiationAttachments(sections: Record<string, any>, attachments: { attachmentType: string }[]) {
  const uploaded = new Set(attachments.map(item => item.attachmentType))
  return requiredInitiationAttachments(sections).filter(item => !uploaded.has(item.type))
}
