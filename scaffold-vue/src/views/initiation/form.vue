<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { initiationApi, collaborationApi, type FinanceItem, type InitiationAttachment, type InitiationProject } from '@/api'
import { downloadBlob } from '@/utils/download'
import { pushToast } from '@/composables/toast'
import EconomicBenefitCalculator from '@/components/finance/EconomicBenefitCalculator.vue'
import DepreciationScheduleCalculator from '@/components/finance/DepreciationScheduleCalculator.vue'
import EconomicBaseDetailTable from '@/components/finance/EconomicBaseDetailTable.vue'
import ReportingPeriodPicker from '@/components/finance/ReportingPeriodPicker.vue'
import {
  calculateEconomicBenefit,
  createEconomicBenefitInputs,
  economicBenefitOutputRows as economicBenefitRows,
  type EconomicBenefitMatrix,
} from '@/utils/economicBenefit'
import { calculatedBenefitAnalysisValue } from '@/utils/benefitAnalysis'
import { financeAmountExTax, summarizeFinanceItems } from '@/utils/financeSummary'
import { initiationAttachmentCatalog, requiredInitiationAttachments, missingInitiationAttachments } from '@/utils/initiationAttachments'
import { yearOnYear, parseMonthRange, formatMonthRange, capabilityDemandOptions, buildCapabilityDemand } from '@/utils/initiationOverview'

const uploadingEvidence = ref(false)
const finishedPptInput = ref<HTMLInputElement>()
const uploadingFinishedPpt = ref(false)
import {
  calculateDepreciation,
  allocateDepreciationInvestments,
  DEPRECIATION_HORIZON_YEARS,
  createDepreciationSchedule,
  hasDepreciationScheduleInput,
  type DepreciationSchedule,
} from '@/utils/depreciation'
import {
  calculateEconomicBaseDetailTotals,
  economicBaseDetailDefinitions,
  normalizeEconomicBaseDetailValues,
} from '@/utils/economicBaseDetail'
import {
  calculateFundScores,
  enterpriseFundCurrentRows,
  fundBonusOptions,
  fundHistoryRows,
  governmentFundCurrentRows,
  mappedFundAverageScore,
  mappedFundScore,
  type FundCurrentRow,
  type FundHistoryRow,
} from '@/utils/fundScore'

const route = useRoute()
const router = useRouter()
const id = computed(() => Number(route.params.id) || 0)
const saving = ref(false)
const generating = ref(false)
const active = ref('overview')
const loaded = ref(false)
let autoSaveTimer: ReturnType<typeof setTimeout> | undefined

const groupRows = [
  '信息化收入（万元）',
  '其中：通信服务收入',
  '其中：算力服务收入',
  '其中：智能服务收入',
  '成员收入（万元）',
  '成员数量（户）',
  '成员数量份额（%）',
  'V网成员数（户）'
]
const treeRows = [
  '信息化收入（万元）',
  '节点纳管率（%）',
  '专线渗透率（%）',
  '移动云渗透率（%）',
  '成员收入（万元）',
  '成员数量（户）',
  '成员数量份额（%）',
  'V网成员数（户）'
]

const fusionLeftRows = [
  { fusion: '融云', product: '自研I+P产品', key: '融云' },
  { fusion: '融5G', product: '5G专网通信费、功能费，不含管会', key: '融5G' },
  { fusion: '融网', product: '专线或物联网卡', key: '融网' },
  { fusion: '融AI', product: '九天能力', key: '九天能力' },
  { fusion: '融AI', product: '其他AI产品能力', key: '其他AI产品能力' }
]

const fusionRightRows = [
  { fusion: '融中台', product: '三大', key: '三大' },
  { fusion: '融中台', product: '云电脑', key: '云电脑' },
  { fusion: '融中台', product: '集团方案', key: '集团方案' },
  { fusion: '融中台', product: '省内方案', key: '省内方案' },
  { fusion: '融中台', product: '专业公司方案', key: '专业公司方案' },
  { fusion: '融中台', product: '集团中台', key: '集团中台' },
  { fusion: '融中台', product: '省内中台', key: '省内中台' },
  { fusion: '融集成', product: '硬件集成', key: '硬件集成' },
  { fusion: '融集成', product: '软件集成', key: '软件集成' },
  { fusion: '融C', product: '集团成员号卡（保）', key: '集团成员号卡（保）' },
  { fusion: '融C', product: '集团成员号卡（拓）', key: '集团成员号卡（拓）' }
]

const revenueModeOptions = ['投资模式', '合作服务模式', '购销模式', '受托代销模式', '其他收入']
const costPrimaryOptions = ['投资部分', '成本部分', '其他部分']
const cooperationCostOptions = ['合作服务模式', '购销模式', '其他成本']

const vendorRiskDefaultRow = {
  vendorName: '', amountRatio: '', smeFlag: '', spendAmount: '', spendRemark: '',
  qualification: '软件厂商', agentLevel: '厂商', agentEquipment: '-', cooperationCount: '',
  businessRisk: '', evidence: '', remark: '', fixed: true
}
const cashFlowDefaultRow = {
  time: '第1年', revenueExTax: '', revenueIncTax: '', receiptIncTax: '', expenseIncTax: '',
  cashRealizationRate: '', description: '', investmentIncomeIncTax: '',
  investmentConfirmation: '按月确认收入', fixed: true
}
const paymentMethodOptions = [
  '大型企业按对称方式付款', '中小企业按项目节点分期付款', '受托代销模式按对称方式付款'
]
const paymentMethodTemplates: Record<string, string> = {
  '大型企业按对称方式付款': '后向厂家中XX为大型企业，按对称方式付款。',
  '中小企业按项目节点分期付款': '后向厂家为中小型企业的，项目合同签订后X个工作日支付软件合同额的X%；初验结束后X个工作日付至合同额的X%；软件终验结束后X个工作日支付至合同额的X%；否则按台账管理，垫资产生相关资金成本纳入分公司净利润考核、成本上限管控。',
  '受托代销模式按对称方式付款': '受托代销模式：按照受托代销最新合同模板签署，按对称方式付款。'
}
const riskRowDefaults = [
  ['虚假项目', '上下游关联关系'], ['虚假项目', '上下游存在AB/BA关系、循环贸易'],
  ['政策、法律风险', '违规转分包'], ['执行不规范', '甄选流程不规范'], ['执行不规范', '违规拆分立项'],
  ['建设与运作风险', '项目交付风险'], ['建设与运作风险', '项目变更、终止的风险'],
  ['建设与运作风险', '生产方面的安全风险'], ['建设与运作风险', '信息安全风险和数据风险'],
  ['建设与运作风险', '项目运维风险'], ['业务、财务风险', '欠费'], ['业务、财务风险', '税务及发票风险'],
  ['合同风险', '项目执行风险']
].map(([category, riskPoint], index) => ({ key: `risk-${index}`, category, riskPoint, involvedDescription: '', controlMeasures: '', evaluationResult: '', fixed: true }))
const deliveryModeOptions = ['投资模式', '合作服务模式', '受托代销模式']
const existingProviderOptions = ['移动', '联通', '广电']
const investmentRows = [
  { mode: '投资部分', name: 'DICT投资' },
  { mode: '投资部分', name: '政企标准化产品投资' },
  { mode: '投资部分', name: '小计' },
  { mode: '合作服务模式', name: 'ICT-云计算成本' },
  { mode: '合作服务模式', name: 'ICT-5G成本' },
  { mode: '合作服务模式', name: 'ICT-三化成本' },
  { mode: '合作服务模式', name: 'ICT成本' },
  { mode: '合作服务模式', name: '小计' },
  { mode: '购销模式', name: '设备销售成本' },
  { mode: '其他成本', name: '专线装机化成本' },
  { mode: '其他成本', name: '专线维护成本' },
  { mode: '其他成本', name: '业务平台维护支撑费-自主集成成本' },
  { mode: '其他成本', name: 'IDC电费' },
  { mode: '其他成本', name: 'SA酬金' },
  { mode: '其他成本', name: 'AI成本' },
  { mode: '其他成本', name: '小计' },
  { mode: '其他部分', name: '受托代销应付账款' },
  { mode: '合计', name: '' }
]

const revenueRows = [
  { mode: '投资模式', name: '专线收入-新增' },
  { mode: '投资模式', name: '专线收入-存量' },
  { mode: '投资模式', name: 'ICT收入' },
  { mode: '投资模式', name: '私有云收入' },
  { mode: '投资模式', name: '物联网收入' },
  { mode: '投资模式', name: '小计' },
  { mode: '合作服务模式', name: 'ICT-云计算收入' },
  { mode: '合作服务模式', name: 'ICT-5G收入' },
  { mode: '合作服务模式', name: 'ICT-三化收入' },
  { mode: '合作服务模式', name: 'ICT收入' },
  { mode: '合作服务模式', name: '小计' },
  { mode: '购销模式', name: '设备销售收入' },
  { mode: '受托代销模式', name: '受托代销应收账款' },
  { mode: '受托代销模式', name: 'ICT手续费' },
  { mode: '其他收入', name: '专线收入' },
  { mode: '其他收入', name: '公有云收入' },
  { mode: '其他收入', name: 'IDC收入' },
  { mode: '其他收入', name: '物联网收入' },
  { mode: '其他收入', name: 'ICT自主集成收入' },
  { mode: '其他收入', name: '云视讯收入' },
  { mode: '其他收入', name: 'AI收入' },
  { mode: '其他收入', name: '小计' },
  { mode: '合计', name: '' }
]

const benefitRowDefaults = [
  { key: 'row01', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '8%及以上' },
  { key: 'row02', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '底线管控', metric: '净现值率（%）', requirement: '折现率为4%，净现值率要求大于0' },
  { key: 'row03', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '底线管控', metric: '静态回收期（年）', requirement: '小于合同期，原则上不超过5年' },
  { key: 'row04', no: '1.1', mode: '投资模式部分', category: '效益管控', metric: '内部收益率（IRR）', requirement: '内部收益率（IRR）要求4%及以上' },
  { key: 'row05', no: '1.1', mode: '投资模式部分', category: '底线管控', metric: '净现值率（%）', requirement: '折现率为4%，净现值率要求大于0' },
  { key: 'row06', no: '1.1', mode: '投资模式部分', category: '底线管控', metric: '动态回收期（年）', requirement: '小于合同期，原则上不超过5年' },
  { key: 'row07', no: '1.2', mode: '合作部分IT净利润率', category: '效益管控', metric: '净利润率（%）', requirement: '3%及以上' },
  { key: 'row08', no: '1.3', mode: '三化部分', category: '省内投资共研模式效益管控', metric: '净利润率（%）', requirement: '15%及以上' },
  { key: 'row09', no: '1.3', mode: '三化部分', category: '省内框架订单模式效益管控', metric: '净利润率（%）', requirement: '11.25%及以上' },
  { key: 'row10', no: '1.3', mode: '三化部分', category: '省内合作入库模式效益管控', metric: '净利润率（%）', requirement: '8%及以上' },
  { key: 'row11', no: '1.3', mode: '三化部分', category: '外省和专业公司方案效益管控', metric: '净利润率（%）', requirement: '6%及以上' },
  { key: 'row12', no: '1.4', mode: '大视频部分', category: '效益管控', metric: '净利润率（%）', requirement: '13%及以上' },
  { key: 'row13', no: '1.5', mode: '大安全部分', category: '效益管控', metric: '净利润率（%）', requirement: '15%及以上' },
  { key: 'row14', no: '1.6', mode: '专线延伸服务部分', category: '效益管控', metric: '净利润率（%）', requirement: '3%及以上' },
  { key: 'row15', no: '1.7', mode: 'AI部分', category: '效益管控', metric: '净利润率（%）', requirement: '13%及以上' },
  { key: 'row16', no: '', mode: '2.购销模式部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '3%及以上' },
  { key: 'row17', no: '', mode: '2.购销模式部分（见附）', category: '底线管控', metric: '净现值率（%）', requirement: '折现率为4%，净现值率要求大于0' },
  { key: 'row18', no: '', mode: '2.购销模式部分（见附）', category: '底线管控', metric: '静态回收期（年）', requirement: '小于合同期，原则上不超过5年' },
  { key: 'row19', no: '', mode: '3.受托代销模式部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '1%及以上' },
  { key: 'row20', no: '', mode: '4.代理人模式部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '1%及以上' },
  { key: 'row21', no: '', mode: '5.租赁模式部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '1%及以上' },
  { key: 'row22', no: '', mode: '6.自主集成部分（见附）', category: '效益管控', metric: '净利润率（%）', requirement: '18%及以上' },
  { key: 'row23', no: '', mode: '其他收入', category: '效益管控', metric: '净利润率（%）', requirement: '' }
]

const preDecisionRows = [
  { key: 'row01', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '效益管控', metric: '净利润率（%）' },
  { key: 'row02', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '底线管控', metric: '净现值率（%）' },
  { key: 'row03', no: '', mode: '1.投资及合作服务模式部分（见附）', category: '底线管控', metric: '静态回收期（年）' },
  { key: 'row04', no: '1.1', mode: '投资模式部分', category: '效益管控', metric: '内部收益率（IRR）' },
  { key: 'row05', no: '1.1', mode: '投资模式部分', category: '底线管控', metric: '净现值率（%）' },
  { key: 'row06', no: '1.1', mode: '投资模式部分', category: '底线管控', metric: '动态回收期（年）' },
  { key: 'row07', no: '1.2', mode: '合作部分IT净利润率', category: '效益管控', metric: '净利润率（%）' },
  { key: 'row08', no: '', mode: '2.购销模式部分（见附）', category: '效益管控', metric: '净利润率（%）' },
  { key: 'row09', no: '', mode: '2.购销模式部分（见附）', category: '底线管控', metric: '净现值率（%）' },
  { key: 'row10', no: '', mode: '2.购销模式部分（见附）', category: '底线管控', metric: '静态回收期（年）' },
  { key: 'row11', no: '', mode: '3.自主集成部分（见附）', category: '效益管控', metric: '净利润率（%）' },
  { key: 'row12', no: '', mode: '4.AI产品（见附）', category: '效益管控', metric: '净利润率（%）' }
]

const idcMainDefaults = [
  { category: '配套分摊', description: '征地' },
  { category: '配套分摊', description: '土建' },
  { category: '配套分摊', description: '变电站' },
  { category: '配套分摊', description: '大机电' },
  { category: '配套分摊', description: '小机电' },
  { category: '维护费用', description: '维护费' },
  { category: '电费', description: '' },
  { category: '小计', description: '小计' }
]
const idcCabinetDefaults = [
  { no: '1', item: '征地' }, { no: '2', item: '土建' }, { no: '3', item: '变电站' },
  { no: '4', item: '大机电' }, { no: '5', item: '小机电房' }, { no: '小计', item: '小计' }
]
const idcMaintenanceDefaults = [
  { no: '1', item: '维护-动环' }, { no: '2', item: '维护-协维' }, { no: '小计', item: '小计' }
]
const now = new Date()
const form = reactive<InitiationProject>({
  projectName: '',
  opportunityNo: '',
  customerName: '',
  customerType: '政府/事业单位',
  industryType: '',
  departmentName: '政企客户部',
  projectManager: '',
  reportYear: now.getFullYear(),
  reportMonth: now.getMonth() + 1,
  agreementYears: undefined,
  status: 'DRAFT',
  sections: {
    attachmentRequirementSelections: [],
    cityName: '',
    bureauName: '',
    platformName: '',
    branchCompany: '',
    coverApplicant: '',
    projectPaymentDetails: {},
    reportPeriod: '',
    projectSubject: '',
    acquisitionMethod: '投标',
    bidDate: '',
    fundingSource: '财政资金',
    projectOverview: '',
    businessModel: '',
    businessModelPart: '',
    propertyOwnership: '',
    groupName: '',
    groupSummary: '',
    customerTreeSummary: '',
    constructionContent: '',
    capabilitySupply: '',
    capabilityDemand: '',
    sevenFusionSummary: '',
    fusionLeftTable: Object.fromEntries(fusionLeftRows.map(row => [row.key, {}])),
    fusionRightTable: Object.fromEntries(fusionRightRows.map(row => [row.key, {}])),
    revenueModes: [] as string[],
    costPrimaryModes: [] as string[],
    cooperationCostModes: [] as string[],
    groupTable: {},
    treeTable: {},
    groupPeriod1: '',
    groupPeriod2: '',
    treePeriod1: '',
    treePeriod2: '',
    investmentSummary: '',
    indirectRevenue: '',
    drivenOpportunity: '',
    benefitAnalysisRows: [],
    benefitAnalysisValues: {},
    benefitAnalysisSelectedKeys: [],
    benefitAnalysisSelectedModes: [],
    procurementComparisonEnabled: false,
    procurementType: 'PUBLIC',
    procurementPublicDescription: '',
    procurementDirectDescription: '',
    procurementAssessment: '',
    procurementPublicRows: [{}, {}, {}],
    procurementDirectRows: [{}],
    idcEnabled: false,
    idcMainRows: idcMainDefaults.map(row => ({ ...row })),
    idcCabinetRows: idcCabinetDefaults.map(row => ({ ...row })),
    idcMaintenanceRows: idcMaintenanceDefaults.map(row => ({ ...row })),
    idcElectricityRows: [{}],
    economicBenefitValues: Object.fromEntries(economicBenefitRows.map(row => [row.key, Array(11).fill('')])),
    economicBenefitInputs: createEconomicBenefitInputs(10),
    economicBenefitYearCount: 10,
    economicBaseData: { estimatedInvestment: null, directRevenue: null, operatingExpense: null, terminalResources: null },
    economicBaseDetailValues: normalizeEconomicBaseDetailValues(undefined),
    depreciationSchedule: createDepreciationSchedule(10),
    depreciationScheduleActive: false,
    investmentEconomicBenefitEnabled: false,
    investmentEconomicBenefitValues: Object.fromEntries(economicBenefitRows.map(row => [row.key, Array(11).fill('')])),
    investmentEconomicBenefitInputs: createEconomicBenefitInputs(10),
    cooperationEconomicBenefitEnabled: false,
    cooperationEconomicBenefitValues: Object.fromEntries(economicBenefitRows.map(row => [row.key, Array(11).fill('')])),
    cooperationEconomicBenefitInputs: createEconomicBenefitInputs(10),
    modeEconomicBenefitInputs: {},
    modeEconomicBenefitValues: {},
    modeEconomicBenefitDefaults: {},
    modeEconomicBenefitDefaultVersion: {},
    modeEconomicBaseDetailValues: {},
    modeEconomicBaseInitialized: {},
    advancePaymentReviewEnabled: false,
    advancePaymentStrategyConclusion: '',
    advancePaymentPreferredOrderConclusion: '',
    advancePaymentCapabilityConclusion: '',
    selectionCompany: '',
    selectionPlanDecisionDate: '',
    selectionMode: '',
    selectionAmountSummary: '',
    selectionAfterBid: '',
    selectionAfterBidReason: '',
    selectionReviewPanel: '',
    selectionReviewStandard: '',
    selectionResultDecisionDate: '',
    selectionReviewDate: '',
    selectionRecommendation: '',
    selectionWinners: '',
    selectionCandidates: '',
    selectionPublicationDate: '',
    preDecisionApproved: false,
    preDecisionSummary: '',
    preDecisionComparisonRows: [],
    preDecisionValues: {},
    clientGroupName: '', clientEnterpriseNature: '', clientEstablishmentDate: '', clientRegisteredCapital: '', clientPaidInCapital: '', clientInsuredEmployees: '',
    clientIndustryType: '', clientProjectDemand: '', clientSiteVisit: '', clientDeliveryAddress: '', clientPastPerformanceRows: [],
    clientHousingDebtAmount: '', clientMobilePerformance: '', clientOtherPerformance: '', clientAsymmetricCount: '', clientAsymmetricAmount: '', clientAsymmetricRatio: '',
    clientTaxDebtRisk: '', clientAbnormalOperationRisk: '', clientLaborDisputeRisk: '', clientEquityManagementRisk: '',
    fundProjectType: 'GOVERNMENT',
    fundProofMaterialSelections: [],
    fundType: '',
    fundProofMaterials: '',
    fundGovernmentBudgetPublicDescription: '',
    fundGovernmentExpenditureSubjectDescription: '',
    fundGovernmentPerformanceTargetDescription: '',
    fundGovernmentProcurementDescription: '',
    fundEnterpriseInternalApprovalDescription: '',
    fundEnterpriseFinancialStrengthDescription: '',
    fundSource: '',
    fundHistoryIndustryItem: '', fundHistoryIndustryDeduction: '', fundHistoryCustomerItem: '', fundHistoryCustomerDeduction: '', fundHistorySourceItem: '', fundHistorySourceDeduction: '',
    fundCurrentAuditItem: '', fundCurrentAuditDeduction: '', fundCurrentHistoryItem: '', fundCurrentHistoryDeduction: '', fundCurrentSourceItem: '', fundCurrentSourceDeduction: '', fundCurrentDebtItem: '', fundCurrentDebtDeduction: '', fundCurrentCounterpartyItem: '', fundCurrentCounterpartyDeduction: '',
    fundSpecialBonusType: '', fundSpecialBonusScore: '',
    vendorRiskRows: [{ ...vendorRiskDefaultRow }],
    vendorSmeProtection: '', vendorQualificationSummary: '', vendorPerformanceSummary: '', vendorBusinessRiskSummary: '',
    projectReceiptMethods: [],
    projectPaymentMethods: [],
    cashFlowRows: [{ ...cashFlowDefaultRow }],
    riskSummary: '',
    riskAssessmentRows: riskRowDefaults.map(row => ({ ...row })),
    threeLineSelections: [],
    threeLineRows: [
      { level: '红线', nature: '禁止性', satisfaction: '', remark: '', fixed: true },
      { level: '底线', nature: '门槛性', satisfaction: '', remark: '', fixed: true },
      { level: '高线', nature: '引导性', satisfaction: '', remark: '', fixed: true }
    ],
    deliveryPlan: '',
    deliveryResponsibility: '',
    deliveryPartners: '',
    deliverySchedule: '',
    deliveryModes: [],
    deliveryRows: [],
    decisionApproveProject: '', decisionMode: '', decisionCostIncTax: '', decisionTotalRevenueIncTax: '', decisionMainRevenueIncTax: '', decisionOtherRevenueIncTax: '', decisionApproveSelection: '', decisionIncludeSelection: '',
    maintenanceEnabled: false, preDecisionOpinionsEnabled: false, existingBusinessEnabled: false, partnerAppendixEnabled: false,
    maintenanceCustomerName: '', maintenanceCustomerAddress: '', maintenanceContact: '', maintenancePhone: '', maintenanceCustomerLevel: '', maintenanceManager: '',
    maintenanceItems: [], maintenanceFullCoverage: '', maintenanceFullCoverageRemark: '', onsiteService: '', onsiteProvider: '', keySupportService: '', keySupportDescription: '',
    lineProtectionLevel: '', lineDualRoute: '', lineRecoveryHours: '', lineAllowedInterruption: '', lineDailyMaintenance: '', lineSupervisor: '', lineManager: '',
    fiveGProtectionLevel: '', fiveGDualRoute: '', fiveGRecoveryHours: '', fiveGAllowedInterruption: '', fiveGDailyMaintenance: '', fiveGSupervisor: '', fiveGManager: '',
    provincialPreDecisionOpinion: '', cityDecisionOpinion: '', existingProjectDriven: '', existingMerchantNo: '', existingProjectName: '', existingProjectOperation: '', existingCtSummary: '', existingProviders: [], existingBusinessRows: [],
    partnerSelectionRows: [], supplyChainFinanceUsed: '', supplyChainFinanceBank: '', supplyChainFinanceAmount: '', supplyChainFinanceReason: '', supplyChainFinanceDescription: '', appraisalEnabled: false, appraisalExecuted: '', appraisalReductionAmount: '', appraisalReductionRatio: '', appraisalReason: '',
    appraisalRows: [],
    decisionContent: ''
  },
  incomeItems: [],
  costItems: []
})

const attachments = ref<InitiationAttachment[]>([])
const removingAttachment = ref<number>()

function ensureSectionObjects() {
  form.sections ||= {}
  form.sections.attachmentRequirementSelections ||= []
  if (!Array.isArray(form.sections.capabilityDemandSelections)) {
    form.sections.capabilityDemandSelections = []
    form.sections.capabilityDemandExtra = form.sections.capabilityDemand || ''
  }
  form.sections.capabilityDemandDetails ||= {}
  form.sections.groupTable ||= {}
  form.sections.treeTable ||= {}
  form.sections.fusionLeftTable ||= {}
  form.sections.fusionRightTable ||= {}
  form.sections.benefitAnalysisValues ||= {}
  form.sections.benefitAnalysisRows ||= []
  if (!form.sections.benefitAnalysisRows.length) {
    let previousSignature = ''
    let groupKey = ''
    form.sections.benefitAnalysisRows = benefitRowDefaults.map((row, index) => {
      const signature = `${row.no}|${row.mode}`
      if (signature !== previousSignature) groupKey = `benefit-group-${index}`
      previousSignature = signature
      return { ...row, groupKey, projectValue: form.sections.benefitAnalysisValues[row.key] || '' }
    })
  } else {
    let previousSignature = ''
    let inferredGroupKey = ''
    form.sections.benefitAnalysisRows = form.sections.benefitAnalysisRows.map((row: any, index: number) => {
      const signature = `${row.no || ''}|${row.mode || ''}`
      if (signature !== previousSignature) inferredGroupKey = `benefit-group-${Date.now()}-${index}`
      previousSignature = signature
      return {
        key: row.key || `benefit-${Date.now()}-${index}`,
        groupKey: row.groupKey || inferredGroupKey,
        no: row.no || '',
        mode: row.mode || '',
        category: row.category || '',
        metric: row.metric || '',
        projectValue: row.projectValue ?? form.sections.benefitAnalysisValues[row.key] ?? '',
        requirement: row.requirement || ''
      }
    })
  }
  if (!Array.isArray(form.sections.benefitAnalysisSelectedKeys)) {
    form.sections.benefitAnalysisSelectedKeys = form.sections.benefitAnalysisRows
      .filter((row: any) => String(row.projectValue ?? form.sections.benefitAnalysisValues[row.key] ?? '').trim())
      .map((row: any) => row.key)
  }
  const validBenefitKeys = new Set(benefitRowDefaults.map(row => row.key))
  form.sections.benefitAnalysisSelectedKeys = form.sections.benefitAnalysisSelectedKeys.filter((key: string) => validBenefitKeys.has(key))
  if (!Array.isArray(form.sections.benefitAnalysisSelectedModes)
    || (!form.sections.benefitAnalysisSelectedModes.length && form.sections.benefitAnalysisSelectedKeys.length)) {
    const selectedKeys = new Set(form.sections.benefitAnalysisSelectedKeys)
    form.sections.benefitAnalysisSelectedModes = [...new Set(
      form.sections.benefitAnalysisRows
        .filter((row: any) => selectedKeys.has(row.key))
        .map((row: any) => row.groupKey),
    )]
  }
  const validBenefitModes = new Set(form.sections.benefitAnalysisRows.map((row: any) => row.groupKey))
  form.sections.benefitAnalysisSelectedModes = form.sections.benefitAnalysisSelectedModes
    .filter((key: string) => validBenefitModes.has(key))
  syncSelectedBenefitModeRows()
  form.sections.procurementPublicRows ||= []
  form.sections.procurementDirectRows ||= []
  while (form.sections.procurementPublicRows.length < 3) form.sections.procurementPublicRows.push({})
  while (form.sections.procurementDirectRows.length < 1) form.sections.procurementDirectRows.push({})
  form.sections.idcMainRows ||= []
  form.sections.idcCabinetRows ||= []
  form.sections.idcMaintenanceRows ||= []
  form.sections.idcElectricityRows ||= []
  while (form.sections.idcMainRows.length < 8) form.sections.idcMainRows.push({})
  while (form.sections.idcCabinetRows.length < 6) form.sections.idcCabinetRows.push({})
  while (form.sections.idcMaintenanceRows.length < 3) form.sections.idcMaintenanceRows.push({})
  while (form.sections.idcElectricityRows.length < 1) form.sections.idcElectricityRows.push({})
  idcMainDefaults.forEach((defaults, index) => {
    for (const [key, value] of Object.entries(defaults)) {
      if (form.sections.idcMainRows[index][key] == null || form.sections.idcMainRows[index][key] === '') form.sections.idcMainRows[index][key] = value
    }
  })
  idcCabinetDefaults.forEach((defaults, index) => {
    for (const [key, value] of Object.entries(defaults)) {
      if (form.sections.idcCabinetRows[index][key] == null || form.sections.idcCabinetRows[index][key] === '') form.sections.idcCabinetRows[index][key] = value
    }
  })
  idcMaintenanceDefaults.forEach((defaults, index) => {
    for (const [key, value] of Object.entries(defaults)) {
      if (form.sections.idcMaintenanceRows[index][key] == null || form.sections.idcMaintenanceRows[index][key] === '') form.sections.idcMaintenanceRows[index][key] = value
    }
  })
  // The source workbook always evaluates a ten-year horizon. Project years
  // control allocation; depreciation continues through the formula horizon.
  form.sections.economicBenefitYearCount = 10
  form.sections.economicBaseData = {
    estimatedInvestment: null,
    directRevenue: null,
    operatingExpense: null,
    terminalResources: null,
    ...(form.sections.economicBaseData || {}),
  }
  form.sections.economicBaseDetailValues = normalizeEconomicBaseDetailValues(form.sections.economicBaseDetailValues)
  const economicSections = [
    ['economicBenefitInputs', 'economicBenefitValues'],
    ['investmentEconomicBenefitInputs', 'investmentEconomicBenefitValues'],
    ['cooperationEconomicBenefitInputs', 'cooperationEconomicBenefitValues']
  ] as const
  for (const [inputKey, outputKey] of economicSections) {
    const economicValueLength = Math.max(2, Math.min(11, Number(form.sections.economicBenefitYearCount || 10) + 1))
    form.sections[outputKey] ||= {}
    for (const row of economicBenefitRows) {
      const values = form.sections[outputKey][row.key]
      form.sections[outputKey][row.key] = Array.isArray(values)
        ? [...values, ...Array(Math.max(0, economicValueLength - values.length)).fill('')].slice(0, economicValueLength)
        : Array(economicValueLength).fill('')
    }
    const legacyInputs = Object.fromEntries(['initialInvestment', 'revenue', 'expense', 'terminalResources', 'depreciation']
      .map(key => [key, form.sections[outputKey][key]]))
    form.sections[inputKey] = createEconomicBenefitInputs(
      form.sections.economicBenefitYearCount,
      form.sections[inputKey] || legacyInputs
    )
  }
  form.sections.modeEconomicBenefitInputs ||= {}
  form.sections.modeEconomicBenefitValues ||= {}
  form.sections.modeEconomicBenefitDefaults ||= {}
  form.sections.modeEconomicBenefitDefaultVersion ||= {}
  form.sections.modeEconomicBaseDetailValues ||= {}
  form.sections.modeEconomicBaseInitialized ||= {}
  for (const group of selectedEconomicBenefitGroups.value) {
    const legacyInput = group.no === '1.1'
      ? form.sections.investmentEconomicBenefitInputs
      : group.no === '1.2' ? form.sections.cooperationEconomicBenefitInputs : undefined
    form.sections.modeEconomicBenefitInputs[group.key] = createEconomicBenefitInputs(
      form.sections.economicBenefitYearCount,
      form.sections.modeEconomicBenefitInputs[group.key] || legacyInput,
    )
    form.sections.modeEconomicBenefitValues[group.key] ||= Object.fromEntries(
      economicBenefitRows.map(row => [row.key, Array(11).fill('')]),
    )
    ensureModeEconomicBaseDefaults(group.key)
  }
  form.sections.depreciationSchedule = createDepreciationSchedule(
    form.sections.economicBenefitYearCount,
    form.sections.depreciationSchedule,
  )
  form.sections.depreciationScheduleActive ??= hasDepreciationScheduleInput(form.sections.depreciationSchedule)
  recalculateEconomicBenefits()
  form.sections.preDecisionValues ||= {}
  form.sections.preDecisionComparisonRows ||= []
  if (!form.sections.preDecisionComparisonRows.length) {
    let previousSignature = ''
    let groupKey = ''
    form.sections.preDecisionComparisonRows = preDecisionRows.map((row, index) => {
      const signature = `${row.no}|${row.mode}`
      if (signature !== previousSignature) groupKey = `pre-decision-group-${index}`
      previousSignature = signature
      const values = form.sections.preDecisionValues[row.key] || {}
      return { ...row, groupKey, preDecision: values.preDecision || '', initiation: values.initiation || '', trend: values.trend || '' }
    })
  }
  form.sections.fundProjectType ||= 'GOVERNMENT'
  form.sections.fundHistorySourceItem = Array.isArray(form.sections.fundHistorySourceItem)
    ? form.sections.fundHistorySourceItem
    : form.sections.fundHistorySourceItem ? [form.sections.fundHistorySourceItem] : []
  form.sections.fundProofMaterialSelections ||= []
  form.sections.maintenanceEnabled ??= false
  form.sections.preDecisionOpinionsEnabled ??= false
  form.sections.existingBusinessEnabled ??= false
  form.sections.partnerAppendixEnabled ??= false
  form.sections.clientPastPerformanceRows ||= []
  form.sections.vendorRiskRows ||= []
  if (!form.sections.vendorRiskRows.length) form.sections.vendorRiskRows.push({ ...vendorRiskDefaultRow })
  form.sections.vendorRiskRows[0] = { ...vendorRiskDefaultRow, ...form.sections.vendorRiskRows[0], fixed: true }
  form.sections.projectPaymentDetails ||= {}
  form.sections.cashFlowRows ||= []
  if (!form.sections.cashFlowRows.length) form.sections.cashFlowRows.push({ ...cashFlowDefaultRow })
  form.sections.cashFlowRows[0] = { ...cashFlowDefaultRow, ...form.sections.cashFlowRows[0], time: '第1年', fixed: true }
  form.sections.projectReceiptMethods ||= []
  const legacyPaymentMethod = String(form.sections.projectPaymentMethod || '').trim()
  const savedPaymentMethods = Array.isArray(form.sections.projectPaymentMethods)
    ? form.sections.projectPaymentMethods.filter((method: string) => paymentMethodOptions.includes(method))
    : []
  if (!savedPaymentMethods.length && legacyPaymentMethod) {
    const inferredMethod = legacyPaymentMethod.includes('受托代销')
      ? '受托代销模式按对称方式付款'
      : legacyPaymentMethod.includes('中小')
        ? '中小企业按项目节点分期付款'
        : legacyPaymentMethod.includes('大型企业')
          ? '大型企业按对称方式付款'
          : ''
    if (inferredMethod) savedPaymentMethods.push(inferredMethod)
  }
  form.sections.projectPaymentMethods = savedPaymentMethods
  if (savedPaymentMethods.length === 1 && legacyPaymentMethod && !String(form.sections.projectPaymentDetails[savedPaymentMethods[0]] || '').trim()) {
    form.sections.projectPaymentDetails[savedPaymentMethods[0]] = legacyPaymentMethod
  }
  form.sections.riskAssessmentRows ||= []
  if (!form.sections.riskAssessmentRows.length) form.sections.riskAssessmentRows = riskRowDefaults.map(row => ({ ...row }))
  form.sections.threeLineSelections ||= []
  form.sections.threeLineRows ||= []
  form.sections.threeLineRows = [
    { level: '红线', nature: '禁止性', satisfaction: '', remark: '', fixed: true },
    { level: '底线', nature: '门槛性', satisfaction: '', remark: '', fixed: true },
    { level: '高线', nature: '引导性', satisfaction: '', remark: '', fixed: true }
  ].map(row => ({ ...row, ...form.sections.threeLineRows.find((existing: any) => existing.level === row.level) }))
  form.sections.deliveryRows ||= []
  if (form.sections.deliveryRows.length && form.sections.deliveryRows.every((row: any) => row.fixed)) form.sections.deliveryRows = []
  form.sections.deliveryModes ||= [...new Set(form.sections.deliveryRows.map((row: any) => row.mode).filter(Boolean))]
  form.sections.maintenanceItems ||= []
  form.sections.existingBusinessRows ||= []
  form.sections.existingProviders ||= [...new Set(form.sections.existingBusinessRows.map((row: any) => row.provider).filter(Boolean))]
  form.sections.partnerSelectionRows ||= []
  form.sections.appraisalRows ||= []
  for (const row of fusionLeftRows) form.sections.fusionLeftTable[row.key] ||= {}
  for (const row of fusionRightRows) form.sections.fusionRightTable[row.key] ||= {}
  const integrationRemarks = [...new Set(fusionRightRows.slice(7, 9).map(row => String(form.sections.fusionRightTable[row.key].remark || '').trim()).filter(Boolean))]
  form.sections.fusionRightTable[fusionRightRows[7].key].remark = integrationRemarks.join('\n')
  delete form.sections.fusionRightTable[fusionRightRows[8].key].remark
  for (const [rows, table] of [[groupRows, form.sections.groupTable], [treeRows, form.sections.treeTable]] as const) {
    for (const row of rows.slice(4)) table[row + '2025'] ||= table[row + '2026'] || table[row + 'growth'] || ''
  }
  const middlePlatformRemark = fusionRightRows.slice(0, 7)
    .map(row => form.sections.fusionRightTable[row.key].remark)
    .find((value: unknown) => String(value || '').trim())
  if (middlePlatformRemark) {
    form.sections.fusionRightTable[fusionRightRows[0].key].remark = middlePlatformRemark
    for (const row of fusionRightRows.slice(1, 7)) delete form.sections.fusionRightTable[row.key].remark
  }
}

function addVendorRiskRow() {
  form.sections.vendorRiskRows.push({ vendorName: '', amountRatio: '', smeFlag: '', spendAmount: '', spendRemark: '', qualification: '', agentLevel: '', agentEquipment: '', cooperationCount: '', businessRisk: '', evidence: '', remark: '' })
}

function changeEconomicBenefitYears(delta: number) {
  const current = Number(form.agreementYears || 1)
  const next = Math.max(1, Math.min(10, current + delta))
  if (next === current) return
  form.agreementYears = next
  setEconomicBenefitYears(next)
}

function setEconomicBenefitYears(yearCount: number) {
  const next = Math.max(1, Math.min(10, Math.trunc(Number(yearCount) || 1)))
  const formulaYears = 10
  for (const inputKey of ['economicBenefitInputs', 'investmentEconomicBenefitInputs', 'cooperationEconomicBenefitInputs']) {
    form.sections[inputKey] = createEconomicBenefitInputs(formulaYears, form.sections[inputKey])
    for (const key of ['initialInvestment', 'revenue', 'expense', 'terminalResources']) {
      form.sections[inputKey][key] = form.sections[inputKey][key].map((value: unknown, index: number) => index < next ? value : null)
    }
  }
  for (const group of selectedEconomicBenefitGroups.value) {
    const inputs = createEconomicBenefitInputs(formulaYears, form.sections.modeEconomicBenefitInputs?.[group.key])
    for (const key of ['initialInvestment', 'revenue', 'expense', 'terminalResources', 'depreciation']) {
      inputs[key] = inputs[key].map((value, index: number) => index < next ? value : null)
    }
    form.sections.modeEconomicBenefitInputs[group.key] = inputs
  }
  form.sections.depreciationSchedule = createDepreciationSchedule(formulaYears, form.sections.depreciationSchedule)
  form.sections.economicBenefitYearCount = formulaYears
  form.agreementYears = next
  recalculateEconomicBenefits()
}

function readEconomicBaseDataFromDetails() {
  const totals = calculateEconomicBaseDetailTotals(form.sections.economicBaseDetailValues)
  form.sections.economicBaseData.estimatedInvestment = totals.estimatedInvestment
  form.sections.economicBaseData.directRevenue = totals.directRevenue
  form.sections.economicBaseData.operatingExpense = totals.operatingExpense
  syncFinanceSummaries()
  form.contractAmountIncTax = totals.sections.INCOME.contractIncTax
  syncDepreciationInvestmentsFromAnnualInput(totals)
  recalculateEconomicBenefits()
}

const financeDistributionSignature = computed(() => JSON.stringify({
  years: form.agreementYears,
  detail: form.sections.economicBaseDetailValues,
}))

watch(financeDistributionSignature, () => {
  if (loaded.value) readEconomicBaseDataFromDetails()
})

function syncDepreciationInvestmentsFromAnnualInput(existingTotals?: ReturnType<typeof calculateEconomicBaseDetailTotals>) {
  const totals = existingTotals || calculateEconomicBaseDetailTotals(form.sections.economicBaseDetailValues)
  const totalInvestment = Number(totals.estimatedInvestment || 0)
  const projectYears = Math.max(1, Math.min(10, Number(form.agreementYears || 1)))
  const previousPlan = form.sections.economicBenefitInputs?.initialInvestment || []
  form.sections.annualInvestmentManual ??= previousPlan.some((value: unknown) => value != null && value !== '')
  if (!form.sections.annualInvestmentManual) {
    form.sections.economicBenefitInputs.initialInvestment = Array.from({ length: 10 }, (_, index) =>
      index === 0 && totalInvestment > 0 ? totalInvestment : null)
  }
  const annualInvestment = form.sections.economicBenefitInputs.initialInvestment
    .map((value: unknown, index: number) => index < projectYears ? value : null)
  form.sections.depreciationSchedule = allocateDepreciationInvestments(
    totals.investmentCategories, annualInvestment, 10, form.sections.depreciationSchedule)
  form.sections.depreciationScheduleActive = totalInvestment > 0 && annualInvestment.some((value: unknown) => Number(value) > 0)
}

const economicSectionPairs = [
  ['economicBenefitInputs', 'economicBenefitValues'],
  ['investmentEconomicBenefitInputs', 'investmentEconomicBenefitValues'],
  ['cooperationEconomicBenefitInputs', 'cooperationEconomicBenefitValues']
] as const

function economicInputs(key: string) {
  return form.sections[key] as EconomicBenefitMatrix
}

function modeEconomicInputs(groupKey: string) {
  return (form.sections.modeEconomicBenefitInputs?.[groupKey] || createEconomicBenefitInputs(10)) as EconomicBenefitMatrix
}

function modeEconomicOutputs(groupKey: string) {
  return (form.sections.modeEconomicBenefitValues?.[groupKey] || {}) as EconomicBenefitMatrix
}

function modeFinanceItems(groupKey: string) {
  const row = (form.sections.benefitAnalysisRows || []).find((item: any) => item.groupKey === groupKey)
  const income = form.incomeItems || []
  const costs = form.costItems || []
  const byMode = (items: FinanceItem[], mode: string) => items.filter(item => item.mode === mode)
  const byName = (items: FinanceItem[], pattern: RegExp) => items.filter(item => pattern.test(String(item.name || '')))
  switch (row?.key) {
    case 'row04': return { income: byMode(income, '投资模式'), expense: byMode(costs, '投资部分') }
    case 'row07': return { income: byMode(income, '合作服务模式'), expense: byMode(costs, '合作服务模式') }
    case 'row12': return { income: byName(income, /云视讯|大视频|视频/), expense: byName(costs, /云视讯|大视频|视频/) }
    case 'row14': return { income: byName(income, /专线/), expense: byName(costs, /专线/) }
    case 'row15': return { income: byName(income, /AI/i), expense: byName(costs, /AI/i) }
    case 'row16': return { income: byMode(income, '购销模式'), expense: byMode(costs, '购销模式') }
    case 'row19': return { income: byMode(income, '受托代销模式'), expense: byName(costs, /受托代销/) }
    case 'row22': return { income: byName(income, /自主集成/), expense: byName(costs, /自主集成/) }
    default: return { income: byMode(income, row?.mode || ''), expense: byMode(costs, row?.mode || '') }
  }
}

function modeFinanceRows(groupKey: string) {
  const details = modeFinanceItems(groupKey)
  return [
    ...details.income.map(item => ({ ...item, financeType: '预计收入' })),
    ...details.expense.map(item => ({ ...item, financeType: '预计成本' })),
  ]
}

function buildModeEconomicBaseDefaults(groupKey: string) {
  const details = modeFinanceItems(groupKey)
  const values = normalizeEconomicBaseDetailValues(undefined)
  const add = (key: string, amount: number, taxRate?: number) => {
    values[key] ||= {}
    values[key].itIncTax = Number(values[key].itIncTax || 0) + amount
    const definition = economicBaseDetailDefinitions.find(item => item.key === key)
    if (definition?.taxRate == null && Number.isFinite(taxRate)) values[key].taxRate = taxRate
  }
  for (const item of details.income) {
    const name = String(item.name || '')
    const key = /维保|维护/.test(name) ? 'incomeMaintenance'
      : /设备.*销售|销售.*设备/.test(name) ? 'incomeEquipmentSale'
        : /租赁/.test(name) ? 'incomeEquipmentLease'
          : /集成|安装/.test(name) && Number(item.taxRate || 0) === 6 ? 'incomeIntegration' : 'incomeOther'
    add(key, Number(item.amountIncTax || 0), Number(item.taxRate || 0))
  }
  for (const item of details.expense) {
    const name = String(item.name || '')
    const rate = Number(item.taxRate || 0)
    const key = rate === 9 ? 'costConstructionInstallation'
      : rate === 3 ? 'costConstructionIntegration'
        : rate === 1 ? 'costConstructionBid'
      : /维护|维保/.test(name) ? 'costOperationMaintenance'
      : /设备.*更新/.test(name) ? 'costOperationRenewal'
        : /采购|设备/.test(name) ? 'costConstructionEquipment'
          : /安装|建安/.test(name) ? 'costConstructionInstallation'
            : rate === 6 ? 'costConstructionIntegration' : 'costDirectOther'
    add(key, Number(item.amountIncTax || 0), rate)
  }
  return values
}

function modeEconomicBaseValues(groupKey: string) {
  form.sections.modeEconomicBaseDetailValues ||= {}
  return form.sections.modeEconomicBaseDetailValues[groupKey] ||= buildModeEconomicBaseDefaults(groupKey)
}

function ensureModeEconomicBaseDefaults(groupKey: string) {
  form.sections.modeEconomicBaseDetailValues ||= {}
  form.sections.modeEconomicBaseInitialized ||= {}
  if (form.sections.modeEconomicBaseInitialized[groupKey] !== 4) {
    form.sections.modeEconomicBaseDetailValues[groupKey] = buildModeEconomicBaseDefaults(groupKey)
    form.sections.modeEconomicBaseInitialized[groupKey] = 4
  }
  return form.sections.modeEconomicBaseDetailValues[groupKey]
}

function restoreOverallExpectedValues() {
  const inputs = form.sections.economicBenefitInputs
  inputs.revenue[0] = Number(form.sections.economicBaseData.directRevenue || 0) || null
  inputs.expense[0] = Number(form.sections.economicBaseData.operatingExpense || 0) || null
  recalculateEconomicBenefits()
}

function restoreModeStandardTable(groupKey: string) {
  form.sections.modeEconomicBaseDetailValues ||= {}
  form.sections.modeEconomicBaseInitialized ||= {}
  form.sections.modeEconomicBaseDetailValues[groupKey] = buildModeEconomicBaseDefaults(groupKey)
  form.sections.modeEconomicBaseInitialized[groupKey] = 4
  restoreModeExpectedValues(groupKey)
}

function restoreModeExpectedValues(groupKey: string) {
  const totals = calculateEconomicBaseDetailTotals(modeEconomicBaseValues(groupKey))
  const inputs = form.sections.modeEconomicBenefitInputs[groupKey] ||= createEconomicBenefitInputs(10)
  inputs.revenue[0] = totals.directRevenue || null
  inputs.expense[0] = totals.operatingExpense || null
  form.sections.modeEconomicBenefitDefaults[groupKey] = { revenue: totals.directRevenue, expense: totals.operatingExpense }
  form.sections.modeEconomicBenefitDefaultVersion ||= {}
  form.sections.modeEconomicBenefitDefaultVersion[groupKey] = 2
  recalculateEconomicBenefits()
}

function syncModeEconomicBenefitDefaults() {
  form.sections.modeEconomicBenefitInputs ||= {}
  form.sections.modeEconomicBenefitDefaults ||= {}
  form.sections.modeEconomicBenefitDefaultVersion ||= {}
  for (const group of selectedEconomicBenefitGroups.value) {
    const totals = calculateEconomicBaseDetailTotals(ensureModeEconomicBaseDefaults(group.key))
    const next = { revenue: totals.directRevenue, expense: totals.operatingExpense }
    const previous = form.sections.modeEconomicBenefitDefaults[group.key]
    const needsMigration = form.sections.modeEconomicBenefitDefaultVersion[group.key] !== 2
    const inputs = form.sections.modeEconomicBenefitInputs[group.key] ||= createEconomicBenefitInputs(10)
    for (const key of ['revenue', 'expense'] as const) {
      const current = inputs[key][0]
      const stillDefault = previous && Number(current || 0) === Number(previous[key] || 0)
      if (needsMigration || !previous || current == null || current === '' || stillDefault) inputs[key][0] = next[key] || null
    }
    form.sections.modeEconomicBenefitDefaults[group.key] = next
    form.sections.modeEconomicBenefitDefaultVersion[group.key] = 2
  }
}

watch(() => JSON.stringify(form.sections.modeEconomicBaseDetailValues || {}), () => {
  if (!loaded.value) return
  syncModeEconomicBenefitDefaults()
  recalculateEconomicBenefits()
})

function updateEconomicInput(sectionKey: string, payload: { key: string; index: number; value: number | null }) {
  form.sections[sectionKey][payload.key][payload.index] = payload.value
  if (sectionKey === 'economicBenefitInputs' && payload.key === 'initialInvestment') {
    form.sections.annualInvestmentManual = true
    syncDepreciationInvestmentsFromAnnualInput()
  }
  recalculateEconomicBenefits()
}

function updateModeEconomicInput(groupKey: string, payload: { key: string; index: number; value: number | null }) {
  form.sections.modeEconomicBenefitInputs[groupKey] ||= createEconomicBenefitInputs(10)
  form.sections.modeEconomicBenefitInputs[groupKey][payload.key][payload.index] = payload.value
  recalculateEconomicBenefits()
}

function depreciationSchedule() {
  return form.sections.depreciationSchedule as DepreciationSchedule
}

function syncDepreciationToEconomicBenefit() {
  const years = Number(form.sections.economicBenefitYearCount || 10)
  if (!form.sections.depreciationScheduleActive) {
    form.sections.economicBenefitInputs.depreciation = Array(years).fill(null)
    return
  }
  const result = calculateDepreciation(depreciationSchedule(), DEPRECIATION_HORIZON_YEARS)
  // B58:K58 in the economic sheet reference E2:N2, not the 20-year total.
  form.sections.economicBenefitInputs.depreciation = result.totals.slice(0, years).map(value => value || null)
}

function updateDepreciationInput(payload: { categoryKey: string; field: 'commissioningMonths' | 'investments'; index: number; value: number | null }) {
  form.sections.depreciationSchedule[payload.categoryKey][payload.field][payload.index] = payload.value
  form.sections.depreciationScheduleActive = true
  recalculateEconomicBenefits()
}

function recalculateEconomicBenefits() {
  const years = Number(form.sections.economicBenefitYearCount || 10)
  syncDepreciationToEconomicBenefit()
  for (const [inputKey, outputKey] of economicSectionPairs) {
    const result = calculateEconomicBenefit(form.sections[inputKey] || {}, years)
    form.sections[outputKey] = result.values
    if (outputKey === 'economicBenefitValues' && result.netProfitRate != null) {
      form.overallProfitRate = Number((result.netProfitRate * 100).toFixed(2))
    }
  }
  const selectedModeNumbers = new Set(selectedEconomicBenefitGroups.value.map(group => group.no))
  form.sections.investmentEconomicBenefitEnabled = selectedModeNumbers.has('1.1')
  form.sections.cooperationEconomicBenefitEnabled = selectedModeNumbers.has('1.2')
  for (const group of selectedEconomicBenefitGroups.value) {
    const inputs = form.sections.modeEconomicBenefitInputs[group.key]
    form.sections.modeDepreciationAutomatic ||= {}
    if (inputs && modeHasAutomaticDepreciation()) {
      const categories = calculateEconomicBaseDetailTotals(form.sections.economicBaseDetailValues).investmentCategories
      const schedule = allocateDepreciationInvestments(categories, inputs.initialInvestment || [], years, depreciationSchedule())
      inputs.depreciation = calculateDepreciation(schedule, years).totals.map(value => value || null)
      form.sections.modeDepreciationAutomatic[group.key] = true
    } else if (inputs && form.sections.modeDepreciationAutomatic[group.key]) {
      inputs.depreciation = Array(years).fill(null)
      form.sections.modeDepreciationAutomatic[group.key] = false
    }
    const result = calculateEconomicBenefit(form.sections.modeEconomicBenefitInputs[group.key] || {}, years)
    form.sections.modeEconomicBenefitValues[group.key] = result.values
    if (group.no === '1.1') {
      form.sections.investmentEconomicBenefitEnabled = true
      form.sections.investmentEconomicBenefitInputs = form.sections.modeEconomicBenefitInputs[group.key]
      form.sections.investmentEconomicBenefitValues = result.values
    }
    if (group.no === '1.2') {
      form.sections.cooperationEconomicBenefitEnabled = true
      form.sections.cooperationEconomicBenefitInputs = form.sections.modeEconomicBenefitInputs[group.key]
      form.sections.cooperationEconomicBenefitValues = result.values
    }
  }
  syncCalculatedBenefitIndicators()
  if (Array.isArray(form.sections.economicBenefitAppendices)) {
    form.sections.economicBenefitAppendices = selectedEconomicBenefitGroups.value.map(group => ({
      key: group.key, title: `${group.no ? group.no + ' ' : ''}${group.mode}`,
      values: form.sections.modeEconomicBenefitValues[group.key],
    }))
  }
  for (const row of form.sections.preDecisionComparisonRows || []) {
    const calculatedRow = selectedBenefitAnalysisRows.value.find((item: any) => item.key === row.key && item.mode === row.mode)
    if (calculatedRow) row.initiation = calculatedRow.projectValue
  }
}

function syncCalculatedBenefitIndicators() {
  const totalIndex = Number(form.sections.economicBenefitYearCount || 10)
  const sources = {
    overall: form.sections.economicBenefitValues || {},
    investment: form.sections.investmentEconomicBenefitValues || {},
    cooperation: form.sections.cooperationEconomicBenefitValues || {},
  }
  for (const row of form.sections.benefitAnalysisRows || []) {
    const dynamicValues = form.sections.modeEconomicBenefitValues?.[row.groupKey]
    row.projectValue = dynamicValues
      ? calculatedBenefitAnalysisValue(row, { overall: dynamicValues, investment: dynamicValues, cooperation: dynamicValues }, totalIndex)
      : calculatedBenefitAnalysisValue(row, sources, totalIndex)
  }
}

function modeHasAutomaticDepreciation() {
  const totals = calculateEconomicBaseDetailTotals(form.sections.economicBaseDetailValues)
  return Object.values(totals.investmentCategories).some(value => value > 0)
}

const selectedBenefitAnalysisRows = computed(() => {
  const selectedModes = new Set(form.sections.benefitAnalysisSelectedModes || [])
  return (form.sections.benefitAnalysisRows || []).filter((row: any) =>
    selectedModes.has(row.groupKey) && isVisibleBenefitMetric(row),
  )
})

function isVisibleBenefitMetric(row: any) {
  return Boolean(String(row.metric || '').trim())
}

const benefitModeOptions = computed(() => {
  const groups = new Map<string, { key: string; label: string; metricCount: number }>()
  for (const row of form.sections.benefitAnalysisRows || []) {
    if (!row.mode || !row.metric || !isVisibleBenefitMetric(row)) continue
    const key = String(row.groupKey)
    const current = groups.get(key)
    if (current) current.metricCount += 1
    else groups.set(key, {
      key,
      label: [row.no, row.mode].filter(Boolean).join(' ') || row.mode,
      metricCount: 1,
    })
  }
  return [...groups.values()]
})

const overallEconomicBenefitSelected = computed(() => {
  const overallGroupKey = form.sections.benefitAnalysisRows?.find((row: any) => row.key === 'row01')?.groupKey
  return Boolean(overallGroupKey && (form.sections.benefitAnalysisSelectedModes || []).includes(overallGroupKey))
})

function syncSelectedBenefitModeRows() {
  const selectedModes = new Set(form.sections.benefitAnalysisSelectedModes || [])
  form.sections.benefitAnalysisSelectedKeys = (form.sections.benefitAnalysisRows || [])
    .filter((row: any) => selectedModes.has(row.groupKey) && isVisibleBenefitMetric(row))
    .map((row: any) => row.key)
}

const selectedEconomicBenefitGroups = computed(() => {
  const groups = new Map<string, { key: string; no: string; mode: string }>()
  for (const row of selectedBenefitAnalysisRows.value as any[]) {
    // The first overall group is represented by the main project table above.
    if (['row01', 'row02', 'row03'].includes(row.key)) continue
    if (!row.metric) continue
    const key = String(row.groupKey || row.key)
    if (!groups.has(key)) groups.set(key, { key, no: String(row.no || ''), mode: String(row.mode || '未命名模式') })
  }
  return [...groups.values()]
})

watch(() => JSON.stringify(form.sections.benefitAnalysisSelectedModes || []), () => {
  if (!loaded.value) return
  syncSelectedBenefitModeRows()
  form.sections.modeEconomicBenefitInputs ||= {}
  form.sections.modeEconomicBenefitValues ||= {}
  for (const group of selectedEconomicBenefitGroups.value) {
    form.sections.modeEconomicBenefitInputs[group.key] ||= createEconomicBenefitInputs(10)
  }
  recalculateEconomicBenefits()
})

function addClientPastPerformanceRow() {
  form.sections.clientPastPerformanceRows.push({ no: '', projectName: '', opportunityNo: '', signingDate: '', contractAmount: '', payableAmount: '', paidAmount: '', paymentMethod: '', paymentRatio: '', arrearsAmount: '', overdueArrears: '', minimumArrearsPeriod: '', remark: '' })
}

function removeClientPastPerformanceRow(index: number) {
  form.sections.clientPastPerformanceRows.splice(index, 1)
}

function removeVendorRiskRow(index: number) {
  if (index > 0) form.sections.vendorRiskRows.splice(index, 1)
}

function addCashFlowRow() {
  form.sections.cashFlowRows.push({ time: `第${form.sections.cashFlowRows.length + 1}年`, revenueExTax: '', revenueIncTax: '', receiptIncTax: '', expenseIncTax: '', cashRealizationRate: '', description: '', investmentIncomeIncTax: '', investmentConfirmation: '' })
}

function removeCashFlowRow(index: number) {
  if (index > 0) form.sections.cashFlowRows.splice(index, 1)
}

function addRiskRow() {
  form.sections.riskAssessmentRows.push({ key: `risk-${Date.now()}`, category: '', riskPoint: '', involvedDescription: '', controlMeasures: '', evaluationResult: '', fixed: false })
}
function removeRiskRow(index: number) {
  if (!form.sections.riskAssessmentRows[index]?.fixed) form.sections.riskAssessmentRows.splice(index, 1)
}
function addDeliveryRow(mode: string) {
  const sequence = form.sections.deliveryRows.filter((row: any) => form.sections.deliveryModes.includes(row.mode)).length + 1
  form.sections.deliveryRows.push({ no: String(sequence), mode, name: '', unit: '项', priceIncTax: '', ownCapability: '', downstreamUnit: '', implementer: '', constructionMaintenance: '' })
}
function removeDeliveryRow(index: number) {
  form.sections.deliveryRows.splice(index, 1)
  form.sections.deliveryRows.filter((row: any) => form.sections.deliveryModes.includes(row.mode)).forEach((row: any, rowIndex: number) => { row.no = String(rowIndex + 1) })
}

const selectedThreeLineRows = computed(() => form.sections.threeLineRows)
function deliveryRowsFor(mode: string) {
  return form.sections.deliveryRows.filter((row: any) => row.mode === mode)
}
function deliveryRowIndex(row: any) {
  return form.sections.deliveryRows.indexOf(row)
}
function addSectionRow(key: 'maintenanceItems' | 'existingBusinessRows' | 'partnerSelectionRows', defaults: Record<string, any> = {}) { form.sections[key].push({ ...defaults }) }
function removeSectionRow(key: 'maintenanceItems' | 'existingBusinessRows' | 'partnerSelectionRows', index: number) { form.sections[key].splice(index, 1) }
function existingRowsFor(provider: string) { return form.sections.existingBusinessRows.filter((row: any) => row.provider === provider) }
function existingRowIndex(row: any) { return form.sections.existingBusinessRows.indexOf(row) }
function addAppraisalRow() { form.sections.appraisalRows.push({ no: '', content: '', ecosystemQuote: '', reducibleAmount: '', reducibleRatio: '', remark: '' }) }
function removeAppraisalRow(index: number) { form.sections.appraisalRows.splice(index, 1) }
function sumEmbeddedNumbers(value: unknown): number {
  return (String(value ?? '').replace(/[,，]/g, '').match(/[+-]?(?:\d+(?:\.\d+)?|\.\d+)/g) || []).reduce((sum, item) => sum + Number(item), 0)
}
const selectionRecommendation = computed(() => [
  form.sections.selectionWinners ? `中选人：${form.sections.selectionWinners}` : '',
  form.sections.selectionCandidates ? `候选人：${form.sections.selectionCandidates}` : '',
].filter(Boolean).join('\n'))
function cashFlowSummary({ columns, data }: any) {
  const keys: Record<number, string> = { 1: 'revenueExTax', 2: 'revenueIncTax', 3: 'receiptIncTax', 4: 'expenseIncTax', 7: 'investmentIncomeIncTax' }
  return columns.map((_: unknown, index: number) => index === 0 ? '合计' : keys[index] ? data.reduce((sum: number, row: any) => sum + sumEmbeddedNumbers(row[keys[index]]), 0).toFixed(2) : '')
}
function performanceSummary({ columns, data }: any) {
  const keys: Record<number, string> = { 4: 'contractAmount', 5: 'payableAmount', 6: 'paidAmount', 9: 'arrearsAmount' }
  return columns.map((_: unknown, index: number) => index === 0 ? '合计' : keys[index] ? data.reduce((sum: number, row: any) => sum + sumEmbeddedNumbers(row[keys[index]]), 0).toFixed(2) : [7, 8].includes(index) ? '-' : '')
}
const partnerSelectionTotal = computed(() => form.sections.partnerSelectionRows.reduce((sum: number, row: any) => sum + sumEmbeddedNumbers(row.amountExTax), 0).toFixed(2))
const fundCurrentRows = computed(() => form.sections.fundProjectType === 'GOVERNMENT' ? governmentFundCurrentRows : enterpriseFundCurrentRows)
const fundScoreSummary = computed(() => calculateFundScores(form.sections, form.sections.fundProjectType))
const fundHistoryScore = computed(() => fundScoreSummary.value.history)
const fundCurrentScore = computed(() => fundScoreSummary.value.current)
const fundFinalScore = computed(() => fundScoreSummary.value.finalScore.toFixed(2))
const fundRiskConclusion = computed(() => `${fundScoreSummary.value.riskLevel}风险，${fundScoreSummary.value.riskResult}`)

function applyFundHistoryItem(row: FundHistoryRow) {
  const deduction = row.multiple
    ? mappedFundAverageScore(row.options, form.sections[row.item])
    : mappedFundScore(row.options, form.sections[row.item])
  form.sections[row.deduction] = deduction ?? ''
}

function applyFundCurrentItem(row: FundCurrentRow) {
  const deduction = mappedFundScore(row.options, form.sections[row.item])
  form.sections[row.deduction] = deduction ?? ''
  syncFundCurrentAggregates()
}

function syncFundCurrentAggregates() {
  for (const aggregateKey of [...new Set(fundCurrentRows.value.map(item => item.aggregateKey))]) {
    form.sections[aggregateKey] = fundCurrentRows.value
      .filter(item => item.aggregateKey === aggregateKey)
      .reduce((total, item) => total + Number(form.sections[item.deduction] || 0), 0)
  }
}

function applyFundBonusType() {
  const bonus = mappedFundScore(fundBonusOptions, form.sections.fundSpecialBonusType)
  form.sections.fundSpecialBonusScore = bonus ?? ''
}

function ensureInvestmentRows() {
  form.costItems ||= []
  const hasSavedCostSelection = Array.isArray(form.sections.costPrimaryModes)
  form.sections.costPrimaryModes ||= []
  form.sections.cooperationCostModes ||= []
  form.costItems = form.costItems.filter(item => item.name !== '小计' && item.mode !== '合计')
  // Migrate the previous selector value. It represented the whole cost section,
  // although the UI called it "合作服务模式".
  if (form.sections.costPrimaryModes.includes('合作服务模式')) {
    form.sections.costPrimaryModes = form.sections.costPrimaryModes.filter((mode: string) => mode !== '合作服务模式')
    form.sections.costPrimaryModes.push('成本部分')
  }
  if (!hasSavedCostSelection && form.costItems.length) {
    if (form.costItems.some(item => item.mode === '投资部分')) form.sections.costPrimaryModes.push('投资部分')
    if (form.costItems.some(item => cooperationCostOptions.includes(item.mode || ''))) form.sections.costPrimaryModes.push('成本部分')
    if (form.costItems.some(item => item.mode === '其他部分')) form.sections.costPrimaryModes.push('其他部分')
    form.sections.cooperationCostModes = cooperationCostOptions.filter(mode => form.costItems.some(item => item.mode === mode))
  }
  syncCostModes()
}

function selectedCostModes() {
  const result: string[] = []
  if (form.sections.costPrimaryModes.includes('投资部分')) result.push('投资部分')
  if (form.sections.costPrimaryModes.includes('成本部分')) result.push(...form.sections.cooperationCostModes)
  if (form.sections.costPrimaryModes.includes('其他部分')) result.push('其他部分')
  return result
}

function syncCostModes() {
  if (!form.sections.costPrimaryModes.includes('成本部分')) form.sections.cooperationCostModes = []
  const selected = selectedCostModes()
  const existing = [...form.costItems]
  form.costItems = existing.filter(item => selected.includes(item.mode || '') && item.name !== '小计')
  for (const mode of selected) {
    const templates = investmentRows.filter(row => row.mode === mode && row.name !== '小计')
    for (const template of templates) {
      if (!form.costItems.some(item => item.mode === template.mode && item.name === template.name)) form.costItems.push({ ...template })
    }
  }
}

function costItems(mode: string) {
  return form.costItems.filter(item => item.mode === mode && item.name !== '小计')
}

function addCostItem(mode: string) {
  form.costItems.push({ mode, name: '', taxRate: 6 })
}

function removeCostItem(item: FinanceItem) {
  const index = form.costItems.indexOf(item)
  if (index >= 0) form.costItems.splice(index, 1)
}

function financeSubtotal(items: FinanceItem[], field: 'amountIncTax' | 'amountExTax') {
  return summarizeFinanceItems(items)[field]
}

function syncFinanceSummaries() {
  for (const item of [...form.costItems, ...form.incomeItems]) {
    item.amountExTax = financeAmountExTax(item)
  }
  form.totalCostIncTax = summarizeFinanceItems(form.costItems).amountIncTax
  form.totalRevenueIncTax = summarizeFinanceItems(form.incomeItems).amountIncTax
}

watch(() => JSON.stringify([form.costItems, form.incomeItems].map(items =>
  items.map(({ mode, name, amountIncTax, taxRate }) => ({ mode, name, amountIncTax, taxRate }))
)), () => {
  if (!loaded.value) return
  syncFinanceSummaries()
  syncModeEconomicBenefitDefaults()
  recalculateEconomicBenefits()
})

const costDisplayRows = computed<any[]>(() => {
  const rows = selectedCostModes().flatMap(mode => {
    const items = costItems(mode)
    return [...items, {
      mode,
      name: '小计',
      amountIncTax: financeSubtotal(items, 'amountIncTax'),
      amountExTax: financeSubtotal(items, 'amountExTax'),
      __subtotal: true
    }]
  })
  if (!rows.length) return []
  return [...rows, {
    mode: '合计',
    name: '',
    amountIncTax: financeSubtotal(form.costItems, 'amountIncTax'),
    amountExTax: financeSubtotal(form.costItems, 'amountExTax'),
    __total: true
  }]
})

const investmentSummaryPreview = computed(() => {
  const sum = (predicate: (item: FinanceItem) => boolean) => form.costItems
    .filter(predicate)
    .reduce((total, item) => total + Number(item.amountIncTax || 0), 0)
  const investment = sum(item => item.mode === '投资部分')
  const cooperation = sum(item => item.mode === '合作服务模式')
  const sales = sum(item => item.mode === '购销模式')
  const other = sum(item => item.mode === '其他成本')
  const entrusted = sum(item => item.mode === '其他部分')
  const total = investment + cooperation + sales + other + entrusted
  const details = [
    ['涉及投资', investment], ['合作服务成本', cooperation], ['购销模式成本', sales],
    ['其他成本', other], ['受托代销款', entrusted],
  ].filter(([, amount]) => Number(amount) !== 0)
    .map(([label, amount]) => `${label}${Number(amount).toFixed(2)}万元`)
  return `总投入：${total.toFixed(2)}万元${details.length ? `，其中${details.join('、')}` : ''}。`
})

function costSection(mode: string) {
  if (mode === '投资部分') return '投资部分'
  if (cooperationCostOptions.includes(mode)) return '成本部分'
  if (mode === '其他部分') return '其他部分'
  return mode
}

function ensureRevenueRows() {
  form.incomeItems ||= []
  // Historical records may contain persisted subtotal rows. Subtotals are now
  // derived per mode and must never be saved as ordinary detail rows.
  form.incomeItems = form.incomeItems.filter(item => item.name !== '\u5c0f\u8ba1' && item.mode !== '\u5408\u8ba1')
  form.sections.revenueModes ||= []
  if (!form.sections.revenueModes.length && form.incomeItems.length) {
    form.sections.revenueModes = [...new Set(form.incomeItems.map(item => item.mode).filter((mode): mode is string => !!mode && revenueModeOptions.includes(mode)))]
  }
}

function revenueItems(mode: string) {
  return form.incomeItems.filter(item => item.mode === mode && item.name !== '小计')
}

function addRevenueItem(mode: string) {
  form.incomeItems.push({ mode, name: '', taxRate: 6 })
}

function removeRevenueItem(item: FinanceItem) {
  const index = form.incomeItems.indexOf(item)
  if (index >= 0) form.incomeItems.splice(index, 1)
}

function revenueSubtotal(mode: string, field: 'amountIncTax' | 'amountExTax') {
  return financeSubtotal(revenueItems(mode), field).toFixed(2)
}

function revenueSummary(mode: string, { columns }: { columns: Array<{ property?: string }> }) {
  return columns.map((_, index) => {
    if (index === 0) return '小计'
    if (index === 1) return revenueSubtotal(mode, 'amountIncTax')
    if (index === 3) return revenueSubtotal(mode, 'amountExTax')
    return ''
  })
}

function revenueSummaryMethod(mode: string) {
  return (params: { columns: Array<{ property?: string }> }) => revenueSummary(mode, params)
}

const revenueDisplayRows = computed<any[]>(() => {
  const rows = form.sections.revenueModes.flatMap((mode: string) => {
    const items = revenueItems(mode)
    return [...items, {
      mode,
      name: '小计',
      amountIncTax: Number(revenueSubtotal(mode, 'amountIncTax')),
      amountExTax: Number(revenueSubtotal(mode, 'amountExTax')),
      __subtotal: true
    }]
  })
  if (!rows.length) return []
  return [...rows, {
    mode: '合计',
    name: '',
    amountIncTax: financeSubtotal(form.incomeItems, 'amountIncTax'),
    amountExTax: financeSubtotal(form.incomeItems, 'amountExTax'),
    __total: true
  }]
})

function revenueDynamicSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  const rows = revenueDisplayRows.value
  if (rows[rowIndex]?.__total) return columnIndex === 0 ? [1, 2] : (columnIndex === 1 ? [0, 0] : [1, 1])
  if (columnIndex !== 0) return
  const mode = rows[rowIndex]?.mode
  if (!mode) return [1, 1]
  const first = rows.findIndex(row => row.mode === mode)
  if (rowIndex !== first) return [0, 0]
  return [rows.filter(row => row.mode === mode).length, 1]
}

function syncRevenueModes(modes: string[]) {
  form.incomeItems = form.incomeItems.filter(item => modes.includes(item.mode || ''))
  for (const mode of modes) {
    if (!revenueItems(mode).length) addRevenueItem(mode)
  }
  syncRevenueAnalysisModes()
}

function syncRevenueAnalysisModes() {
  const modeRows: Record<string, string> = {
    '投资模式': 'row04', '合作服务模式': 'row07', '购销模式': 'row16',
    '受托代销模式': 'row19', '其他收入': 'row23',
  }
  const otherRevenueRules = [
    { rowKey: 'row12', income: /云视讯|大视频|视频/, cost: /云视讯|大视频|视频/ },
    { rowKey: 'row14', income: /专线/, cost: /专线/ },
    { rowKey: 'row15', income: /AI/i, cost: /AI/i },
    { rowKey: 'row22', income: /自主集成/, cost: /自主集成/ },
  ]
  const rows = form.sections.benefitAnalysisRows || []
  const otherRow = rows.find((row: any) => row.key === 'row23')
  if (otherRow) Object.assign(otherRow, { mode: '其他收入', category: '效益管控', metric: '净利润率（%）' })
  const selectedRowKeys = new Set<string>()
  if ((form.sections.revenueModes || []).length) selectedRowKeys.add('row01')
  for (const mode of form.sections.revenueModes || []) {
    if (mode !== '其他收入') selectedRowKeys.add(modeRows[mode])
  }
  if ((form.sections.revenueModes || []).includes('其他收入')) {
    const positiveOtherIncome = (form.incomeItems || []).filter((item: any) =>
      item.mode === '其他收入' && Number(item.amountExTax || item.amountIncTax || 0) !== 0)
    const matchedNames = new Set<string>()
    for (const rule of otherRevenueRules) {
      const matching = positiveOtherIncome.filter((item: any) => rule.income.test(String(item.name || '')))
      if (!matching.length) continue
      selectedRowKeys.add(rule.rowKey)
      matching.forEach((item: any) => matchedNames.add(String(item.name || '')))
    }
    if (positiveOtherIncome.some((item: any) => !matchedNames.has(String(item.name || '')))) {
      selectedRowKeys.add('row23')
    }
  }
  form.sections.benefitAnalysisSelectedModes = [...new Set(
    [...selectedRowKeys]
      .filter(Boolean)
      .map((rowKey: string) => rows.find((row: any) => row.key === rowKey)?.groupKey)
      .filter(Boolean),
  )]
  syncSelectedBenefitModeRows()
  form.sections.modeEconomicBenefitInputs ||= {}
  form.sections.modeEconomicBenefitValues ||= {}
  for (const group of selectedEconomicBenefitGroups.value) {
    form.sections.modeEconomicBenefitInputs[group.key] ||= createEconomicBenefitInputs(10,
      group.no === '1.1' ? form.sections.investmentEconomicBenefitInputs
        : group.no === '1.2' ? form.sections.cooperationEconomicBenefitInputs : undefined)
    form.sections.modeEconomicBaseDetailValues ||= {}
    ensureModeEconomicBaseDefaults(group.key)
  }
  syncModeEconomicBenefitDefaults()
  recalculateEconomicBenefits()
  form.sections.preDecisionComparisonCache ||= {}
  const signature = (row: any) => `${row.no}|${row.mode}|${row.category}|${row.metric}`
  for (const row of form.sections.preDecisionComparisonRows || []) {
    form.sections.preDecisionComparisonCache[signature(row)] = { ...row }
  }
  form.sections.preDecisionComparisonRows = selectedBenefitAnalysisRows.value.map((row: any) => ({
    ...row,
    preDecision: '', trend: '',
    ...form.sections.preDecisionComparisonCache[signature(row)],
    key: row.key, groupKey: row.groupKey, initiation: row.projectValue,
  }))
  form.sections.economicBenefitAppendices = selectedEconomicBenefitGroups.value.map(group => ({
    key: group.key, title: `${group.no ? group.no + ' ' : ''}${group.mode}`,
    values: form.sections.modeEconomicBenefitValues[group.key],
  }))
}

function hasRequiredFusionC() {
  return fusionRightRows.slice(-2).some(row => {
    const value = form.sections.fusionRightTable?.[row.key] || {}
    return ['canIntegrate', 'integrated', 'amount', 'remark'].some(key => String(value[key] ?? '').trim())
  })
}

function add(items: FinanceItem[]) {
  items.push({ mode: '合作服务模式', name: '' })
}

function amountEx(item: FinanceItem) {
  item.amountExTax = financeAmountExTax(item)
  syncFinanceSummaries()
}

function benefitGroupSpan({ row, rowIndex, columnIndex }: { row: any; rowIndex: number; columnIndex: number }) {
  const rows = selectedBenefitAnalysisRows.value
  const groupStart = rows.findIndex((item: any) => item.groupKey === row.groupKey)
  const groupCount = rows.filter((item: any) => item.groupKey === row.groupKey).length
  if (columnIndex === 0) {
    if (rowIndex !== groupStart) return [0, 0]
    return row.no ? [groupCount, 1] : [groupCount, 2]
  }
  if (columnIndex === 1) {
    if (!row.no || rowIndex !== groupStart) return [0, 0]
    return [groupCount, 1]
  }
  if (columnIndex === 2) {
    let start = rowIndex
    while (start > groupStart && rows[start - 1].category === row.category) start--
    let end = rowIndex
    const groupEnd = groupStart + groupCount - 1
    while (end < groupEnd && rows[end + 1].category === row.category) end++
    return rowIndex === start ? [end - start + 1, 1] : [0, 0]
  }
}

function overviewTableSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (rowIndex < 4) return [1, 1]
  if (columnIndex === 1) return [1, 3]
  if (columnIndex > 1) return [0, 0]
  return [1, 1]
}

function addPreDecisionRow(afterIndex = form.sections.preDecisionComparisonRows.length - 1, inheritGroup = false) {
  const source = inheritGroup ? form.sections.preDecisionComparisonRows[afterIndex] : undefined
  form.sections.preDecisionComparisonRows.splice(afterIndex + 1, 0, {
    key: `pre-decision-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    groupKey: source?.groupKey || `pre-decision-group-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    no: source?.no || '', mode: source?.mode || '', category: '', metric: '',
    preDecision: '', initiation: '', trend: ''
  })
}

function removePreDecisionRow(index: number) {
  form.sections.preDecisionComparisonRows.splice(index, 1)
}

function preDecisionDynamicSpan({ row, rowIndex, columnIndex }: { row: any; rowIndex: number; columnIndex: number }) {
  const rows = form.sections.preDecisionComparisonRows
  const start = rows.findIndex((item: any) => item.groupKey === row.groupKey)
  const count = rows.filter((item: any) => item.groupKey === row.groupKey).length
  if (columnIndex === 0) return rowIndex === start ? (row.no ? [count, 1] : [count, 2]) : [0, 0]
  if (columnIndex === 1) return row.no && rowIndex === start ? [count, 1] : [0, 0]
  if (columnIndex === 2) {
    let categoryStart = rowIndex
    while (categoryStart > start && rows[categoryStart - 1].category === row.category) categoryStart--
    let categoryEnd = rowIndex
    while (categoryEnd < start + count - 1 && rows[categoryEnd + 1].category === row.category) categoryEnd++
    return rowIndex === categoryStart ? [categoryEnd - categoryStart + 1, 1] : [0, 0]
  }
}

function syncPreDecisionGroup(row: any, field: 'no' | 'mode') {
  for (const item of form.sections.preDecisionComparisonRows) if (item.groupKey === row.groupKey) item[field] = row[field]
}

async function editPreDecisionNo(row: any) {
  try {
    const result = await ElMessageBox.prompt('序号可留空；留空时模式将横跨前两列。', '设置模式序号', {
      inputValue: row.no || '', inputPlaceholder: '例如：1.1'
    })
    row.no = result.value.trim()
    syncPreDecisionGroup(row, 'no')
  } catch { /* cancelled */ }
}

function investmentSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  const rows = costDisplayRows.value
  const row = rows[rowIndex]
  if (!row) return
  if (row.__total) return columnIndex === 0 ? [1, 3] : (columnIndex < 3 ? [0, 0] : [1, 1])
  if (columnIndex === 0) {
    const section = costSection(row.mode)
    const first = rows.findIndex(item => !item.__total && costSection(item.mode) === section)
    const count = rows.filter(item => !item.__total && costSection(item.mode) === section).length
    const columnSpan = section === '成本部分' ? 1 : 2
    return rowIndex === first ? [count, columnSpan] : [0, 0]
  }
  if (columnIndex === 1) {
    if (costSection(row.mode) !== '成本部分') return [0, 0]
    const first = rows.findIndex(item => item.mode === row.mode)
    const count = rows.filter(item => item.mode === row.mode).length
    return rowIndex === first ? [count, 1] : [0, 0]
  }
}

function revenueSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (columnIndex !== 0) return
  const groups: Record<number, number> = { 0: 6, 6: 5, 11: 1, 12: 2, 14: 8, 22: 1 }
  if (groups[rowIndex]) return [groups[rowIndex], 1]
  return [0, 0]
}

function benefitSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  const groups: Record<number, number> = { 0: 3, 3: 3, 7: 4, 15: 3 }
  if (columnIndex === 0) {
    if (rowIndex === 0 || rowIndex === 15 || rowIndex >= 18) {
      return [groups[rowIndex] || 1, 2]
    }
    if (rowIndex === 3 || rowIndex === 7) return [groups[rowIndex], 1]
    if ([6, 11, 12, 13, 14].includes(rowIndex)) return [1, 1]
    return [0, 0]
  }
  if (columnIndex === 1) {
    if (rowIndex === 0 || rowIndex === 15 || rowIndex >= 18) return [0, 0]
    if (rowIndex === 3 || rowIndex === 7) return [groups[rowIndex], 1]
    if ([6, 11, 12, 13, 14].includes(rowIndex)) return [1, 1]
    return [0, 0]
  }
}

function preDecisionSpan({ rowIndex, columnIndex }: { rowIndex: number; columnIndex: number }) {
  if (columnIndex === 0) {
    if (rowIndex === 0 || rowIndex === 7) return [3, 2]
    if (rowIndex === 3) return [3, 1]
    if (rowIndex === 6) return [1, 1]
    if (rowIndex >= 10) return [1, 2]
    return [0, 0]
  }
  if (columnIndex === 1) {
    if (rowIndex === 0 || rowIndex === 7 || rowIndex >= 10) return [0, 0]
    if (rowIndex === 3) return [3, 1]
    if (rowIndex === 6) return [1, 1]
    return [0, 0]
  }
}

async function calculate() {
  syncFinanceSummaries()
  const result = await initiationApi.projects.calculate(form)
  syncFinanceSummaries()
  form.overallProfitRate = result.overallProfitRate
  recalculateEconomicBenefits()
}

async function load() {
  if (!id.value) {
    ensureRevenueRows()
    ensureInvestmentRows()
    ensureSectionObjects()
    syncRevenueAnalysisModes()
    loaded.value = true
    readEconomicBaseDataFromDetails()
    return
  }
  Object.assign(form, await initiationApi.projects.detail(id.value))
  ensureSectionObjects()
  ensureInvestmentRows()
  ensureRevenueRows()
  syncRevenueAnalysisModes()
  attachments.value = await initiationApi.projects.attachments(id.value)
  loaded.value = true
  readEconomicBaseDataFromDetails()
}

async function save(status = 'DRAFT', silent = false): Promise<number | undefined> {
  if (status === 'COMPLETED' && !await confirmMissingAttachments('完成填报')) return undefined
  ensureSectionObjects()
  form.customerName = form.sections.projectSubject || ''
  form.sections.capabilityDemand = buildCapabilityDemand(form.sections.capabilityDemandSelections, form.sections.capabilityDemandDetails, form.sections.capabilityDemandExtra || '')
  readEconomicBaseDataFromDetails()
  syncRevenueAnalysisModes()
  syncFundCurrentAggregates()
  form.fundRiskLevel = fundScoreSummary.value.riskLevel
  form.sections.benefitAnalysisValues = Object.fromEntries(
    selectedBenefitAnalysisRows.value.map((row: any) => [row.key, row.projectValue || ''])
  )
  form.sections.preDecisionValues = Object.fromEntries(
    form.sections.preDecisionComparisonRows.map((row: any) => [row.key, {
      preDecision: row.preDecision || '', initiation: row.initiation || '', trend: row.trend || ''
    }])
  )
  if (!hasRequiredFusionC()) {
    if (!silent) pushToast('融C为必填项：集团成员号卡（保）和集团成员号卡（拓）至少填写一项', 'danger')
    active.value = 'capability'
    return undefined
  }
  if (!form.projectName.trim() || !form.opportunityNo.trim() || !form.customerName?.trim()) {
    if (!silent) pushToast('请先填写项目名称、商机编号和项目主体', 'danger')
    return undefined
  }

  saving.value = true
  try {
    await calculate()
    readEconomicBaseDataFromDetails()
    form.status = status
    if (id.value) {
      await initiationApi.projects.update(id.value, form)
      if (!silent) pushToast('立项材料已保存', 'ok')
      if (status === 'COMPLETED') router.push('/initiation')
      return id.value
    }

    const created = await initiationApi.projects.create(form)
    if (created?.id) {
      await router.replace(`/initiation/${created.id}/edit`)
      if (!silent) pushToast('立项材料已保存，当前记录已进入编辑态', 'ok')
      return created.id
    }
    if (!silent) pushToast('立项材料已保存', 'ok')
    return undefined
  } finally {
    saving.value = false
  }
}

async function saveAndExport() {
  if (generating.value) return
  const projectId = await save('DRAFT', true)
  if (!projectId) return
  generating.value = true
  try {
    const issues = await initiationApi.projects.validate(projectId)
    if (issues.length) {
      await ElMessageBox.confirm(`以下内容尚未完善：${issues.join('、')}。仍要导出吗？`, '完整性提示', { type: 'warning' })
    }
    if (!await confirmMissingAttachments('生成PPT')) return
    downloadBlob(await initiationApi.projects.exportPpt(projectId), `项目立项-${form.projectName}.pptx`)
    pushToast('PPT已按最新表单生成', 'ok')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  } finally {
    generating.value = false
  }
}

function chooseFinishedPpt() { finishedPptInput.value?.click() }

async function uploadFinishedPpt(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  let changeSummary = ''
  try {
    const result = await ElMessageBox.prompt('请说明本轮提交相较上一轮做了哪些修改；首次提交可填写“首次提交”。', '本轮修改说明', { inputPlaceholder: '例如：已按审核意见补充预算依据并调整第6页', inputValidator: value => !!value?.trim() || '请填写本轮修改说明', confirmButtonText: '继续提交' })
    changeSummary = result.value.trim()
  } catch { input.value = ''; return }
  const projectId = await save('DRAFT', true)
  if (!projectId) { input.value = ''; return }
  uploadingFinishedPpt.value = true
  try {
    await collaborationApi.uploadFinishedPpt('INITIATION', projectId, form.projectName, file, changeSummary)
    pushToast('PPT已提交审核，可到“PPT提交与审核”查看进度', 'ok')
    await router.push('/documents')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : 'PPT提交失败', 'danger')
  } finally {
    uploadingFinishedPpt.value = false
    input.value = ''
  }
}

async function uploadEvidence(type: string, event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || uploadingEvidence.value) return
  uploadingEvidence.value = true
  try {
  const projectId = id.value || await save('DRAFT', true)
  if (!projectId) {
    pushToast('请先填写并暂存项目，再上传证明材料', 'danger')
    return
  }
  await initiationApi.projects.attachmentUpload(projectId, type, file)
  attachments.value = await initiationApi.projects.attachments(projectId)
  ;(event.target as HTMLInputElement).value = ''
  pushToast('证明材料已上传', 'ok')
  } finally {
    uploadingEvidence.value = false
    input.value = ''
  }
}

async function download(a: InitiationAttachment) {
  downloadBlob(await initiationApi.projects.attachmentDownload(a.id), a.originalFilename)
}

async function removeAttachment(a: InitiationAttachment) {
  removingAttachment.value = a.id
  try {
    await initiationApi.projects.attachmentRemove(a.id)
    attachments.value = attachments.value.filter(item => item.id !== a.id)
    pushToast('附件已删除', 'ok')
  } finally {
    removingAttachment.value = undefined
  }
}

const attachmentLabelMap: Record<string, string> = Object.fromEntries(
  initiationAttachmentCatalog.map(item => [item.type, item.label])
)
function toggleAttachmentRequirement(type: string, checked: unknown) {
  const selected = new Set<string>(form.sections.attachmentRequirementSelections || [])
  if (checked) selected.add(type)
  else selected.delete(type)
  form.sections.attachmentRequirementSelections = [...selected]
}
const requiredUploadItems = computed(() => requiredInitiationAttachments(form.sections))
const uploadedAttachmentTypes = computed(() => new Set(attachments.value.map(item => item.attachmentType)))
const missingUploadItems = computed(() => missingInitiationAttachments(form.sections, attachments.value))
const automaticUploadTypes = computed(() => new Set(requiredInitiationAttachments({
  ...form.sections, attachmentRequirementSelections: [],
}).map(item => item.type)))
const uploadReminderTitle = computed(() => missingUploadItems.value.length
  ? `尚有 ${missingUploadItems.value.length} 类材料未上传：${missingUploadItems.value.map(item => item.label).join('、')}`
  : '当前适用材料均已上传（仅检查文件是否上传，不代表内容已审核）')

async function confirmMissingAttachments(action: string): Promise<boolean> {
  if (uploadingEvidence.value) {
    pushToast('文件正在上传，请完成后再操作', 'danger')
    return false
  }
  if (id.value) attachments.value = await initiationApi.projects.attachments(id.value)
  if (!missingUploadItems.value.length) return true
  try {
    await ElMessageBox.confirm(
      `以下材料尚未上传：\n${missingUploadItems.value.map(item => `• 模板第${item.page}页：${item.label}`).join('\n')}\n仍要${action}吗？`,
      '附件缺失提醒',
      { type: 'warning', confirmButtonText: `仍然${action}`, cancelButtonText: '返回补充材料', customClass: 'attachment-reminder-dialog' }
    )
    return true
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
    document.getElementById('attachment-checklist')?.scrollIntoView({ behavior: 'smooth' })
    return false
  }
}

watch(
  form,
  () => {
    if (!loaded.value || !id.value || saving.value || generating.value) return
    if (autoSaveTimer) clearTimeout(autoSaveTimer)
    autoSaveTimer = setTimeout(() => {
      void save('DRAFT', true)
    }, 1200)
  },
  { deep: true }
)

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div class="page-main">
        <div>
          <h1 class="page-title">{{ id ? '编辑' : '新建' }}项目立项</h1>
          <div class="page-desc">先保存数据并生成 PPT，修改确认后在这里上传成品并提交管理员审核。</div>
        </div>
        <div>
          <el-button @click="router.push('/initiation')">返回</el-button>
          <el-button :loading="saving" @click="save()">暂存</el-button>
          <el-button type="success" :loading="generating" @click="saveAndExport">保存并生成PPT</el-button>
          <el-button type="warning" :loading="uploadingFinishedPpt" @click="chooseFinishedPpt">提交PPT审核</el-button>
          <el-button @click="router.push('/documents')">查看审核进度</el-button>
          <router-link to="/initiation/template">PPT模板管理</router-link>
          <el-button type="primary" :loading="saving" @click="save('COMPLETED')">完成填报</el-button>
        </div>
      </div>
    </div>
    <input ref="finishedPptInput" type="file" accept=".pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation" hidden @change="uploadFinishedPpt" />

    <el-card v-loading="generating" element-loading-text="正在生成PPT，请勿重复点击" shadow="never">
      <el-tabs v-model="active">
        <el-tab-pane label="1 封面与整体介绍" name="overview">
          <el-alert title="封面请分别填写题目中的公司/部门、申报人及汇报年月，标题自动生成；下方整体介绍对应模板第2页。" type="info" :closable="false" style="margin-bottom: 16px" />
          <div class="form-tip" style="margin-bottom: 16px">首页标题预览：关于{{ form.sections.branchCompany || '公司/部门' }}为{{ form.projectName ? (form.projectName.endsWith('项目') ? form.projectName : form.projectName + '项目') : 'XXX项目' }}进行立项的请示</div>
          <el-form label-position="top">
            <h3>封面信息（模板第1页）</h3>
            <el-row :gutter="16">
              <el-col :xs="24" :sm="8"><el-form-item label="项目名称" required><el-input v-model="form.projectName" placeholder="例如：智慧医院信息化平台" /></el-form-item></el-col>
              <el-col :xs="24" :sm="8"><el-form-item label="题目中的公司/部门"><el-input v-model="form.sections.branchCompany" placeholder="例如：济南分公司、政企客户部（用于标题）" /></el-form-item></el-col>
              <el-col :xs="24" :sm="8"><el-form-item label="申报人"><el-input v-model="form.sections.coverApplicant" placeholder="填写封面下方落款的申报人" /></el-form-item></el-col>
              <el-col :xs="24" :sm="8"><el-form-item label="汇报年月"><el-date-picker v-model="form.sections.reportPeriod" type="month" format="YYYY年MM月" value-format="YYYY年MM月" placeholder="选择汇报年月" style="width: 100%" /></el-form-item></el-col>
            </el-row>
            <h3>整体介绍（模板第2页）</h3>
            <div class="form-tip" style="margin-bottom: 12px">按最新模板填写项目主体、获取方式、中标时间、签约金额、资金来源、项目概述和商务模式；商机编号会自动写入项目概述。</div>
            <el-row :gutter="16">
              <el-col :span="6"><el-form-item label="商机编号" required><el-input v-model="form.opportunityNo" /></el-form-item></el-col>

              <el-col :span="24">
                <el-form-item label="项目主体" required>
                  <el-input v-model="form.sections.projectSubject" placeholder="XXXXXX，XXXXX若不是实际项目需求方（尤其针对甲方为“科技类公司”情况），需要写清楚项目获取方式并附证明材料（甲方与实际需求方的合同或中标通知书）。" />
                </el-form-item>
              </el-col>

              <el-col :span="6">
                <el-form-item label="获取方式">
                  <el-select v-model="form.sections.acquisitionMethod" style="width: 100%">
                    <el-option label="投标" value="投标" />
                    <el-option label="洽谈" value="洽谈" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="6"><el-form-item label="中标/获取日期"><el-date-picker v-model="form.sections.bidDate" value-format="YYYY年MM月DD日" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="6">
                <el-form-item label="资金来源">
                  <el-select v-model="form.sections.fundingSource" style="width: 100%">
                    <el-option label="专项债" value="专项债" />
                    <el-option label="财政资金" value="财政资金" />
                    <el-option label="自筹资金" value="自筹资金" />
                    <el-option label="奖补资金" value="奖补资金" />
                    <el-option label="其他" value="其他" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="6"><el-form-item label="签约金额（含税/万元）"><el-input-number v-model="form.contractAmountIncTax" :min="0" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="商务模式"><el-input v-model="form.sections.businessModel" placeholder="例如：合作服务" /></el-form-item></el-col>
              <el-col :span="24">
                <el-form-item label="项目概述">
                  <el-input v-model="form.sections.projectOverview" type="textarea" :rows="5" placeholder="示例：为满足XX医院申请三级医院评审需求，拟对该医院医疗信息化相关系统进行升级改造，为管理者与临床工作提供一体化的信息服务支撑。XX月XX日项目已经通过省公司部门专题会/分管领导专题办公会/总经理专题办公会/绿色通道招投标。（商机编码：XXXXXX)。需写清楚客户背景、项目背景、项目意义或者目的及我公司参与业务内容，不体现具体的功能模块。" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="招标文件">
                  <input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg" @change="uploadEvidence('TENDER_DOCUMENT', $event)" />
                  <div v-for="a in attachments.filter(x => x.attachmentType === 'TENDER_DOCUMENT')" :key="a.id">
                    <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="中标通知书">
                  <input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg" @change="uploadEvidence('AWARD_NOTICE', $event)" />
                  <div v-for="a in attachments.filter(x => x.attachmentType === 'AWARD_NOTICE')" :key="a.id">
                    <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="2 XX集团拓展现状" name="group">
          <el-alert title="对应模板第3页：本集团情况、客户树情况及两张基础情况表。" type="info" :closable="false" style="margin-bottom: 16px" />
          <el-form label-position="top">
            <el-form-item label="本集团情况">
              <el-input v-model="form.sections.groupSummary" type="textarea" :rows="3" placeholder="1-X月，XX集团收入XX万元，同比增长XX万元，增幅XX%，使用我公司XX、XX业务，通信服务收入XX万元，同比增长XX%，算力服务收入XX万元，同比增长XX%，智能服务收入XX万元，同比增长XX%。纳管成员XX万户，成员份额XX%，较上年提升XXpp，其中中高端成员XX户，占比XX%；成员收入XX万元，同比增长XX%。" />
            </el-form-item>
            <el-form-item label="客户树情况">
              <el-input v-model="form.sections.customerTreeSummary" type="textarea" :rows="2" placeholder="归属XX客户树（省/市级树），包括收入情况、成员情况、节点数及纳管情况、网云渗透情况、三大主业渗透情况。" />
            </el-form-item>
          </el-form>
          <el-row :gutter="18">
            <el-col :span="24">
              <h3>本集团基本情况</h3>
              <div class="period-controls">
                <label>上期<ReportingPeriodPicker v-model="form.sections.groupPeriod1" /></label>
                <label>本期<ReportingPeriodPicker v-model="form.sections.groupPeriod2" /></label>
              </div>
              <el-table :data="groupRows" :span-method="overviewTableSpan" border>
                <el-table-column label="项目" min-width="150"><template #default="{ row }">{{ row }}</template></el-table-column>
                <el-table-column min-width="140">
                  <template #header>{{ form.sections.groupPeriod1 || '上期' }}</template>
                  <template #default="{ row }"><el-input v-model="form.sections.groupTable[row + '2025']" /></template>
                </el-table-column>
                <el-table-column min-width="140">
                  <template #header>{{ form.sections.groupPeriod2 || '本期' }}</template>
                  <template #default="{ row }"><el-input v-model="form.sections.groupTable[row + '2026']" /></template>
                </el-table-column>
                <el-table-column label="同比增幅（%）" min-width="120"><template #default="{ row }">{{ yearOnYear(form.sections.groupTable[row + '2025'], form.sections.groupTable[row + '2026']) }}</template></el-table-column>
              </el-table>
            </el-col>
            <el-col :span="24">
              <h3>客户树基本情况</h3>
              <div class="period-controls">
                <label>上期<ReportingPeriodPicker v-model="form.sections.treePeriod1" /></label>
                <label>本期<ReportingPeriodPicker v-model="form.sections.treePeriod2" /></label>
              </div>
              <el-table :data="treeRows" :span-method="overviewTableSpan" border>
                <el-table-column label="项目" min-width="150"><template #default="{ row }">{{ row }}</template></el-table-column>
                <el-table-column min-width="140">
                  <template #header>{{ form.sections.treePeriod1 || '上期' }}</template>
                  <template #default="{ row }"><el-input v-model="form.sections.treeTable[row + '2025']" /></template>
                </el-table-column>
                <el-table-column min-width="140">
                  <template #header>{{ form.sections.treePeriod2 || '本期' }}</template>
                  <template #default="{ row }"><el-input v-model="form.sections.treeTable[row + '2026']" /></template>
                </el-table-column>
                <el-table-column label="同比增幅（%）" min-width="120"><template #default="{ row }">{{ yearOnYear(form.sections.treeTable[row + '2025'], form.sections.treeTable[row + '2026']) }}</template></el-table-column>
              </el-table>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="3 需求分析（建设内容）" name="construction">
          <el-alert title="单独对应模板第4页：填写建设内容，并可上传系统架构图替换模板图示区域。" type="info" :closable="false" style="margin-bottom: 16px" />
          <el-form label-position="top">
            <el-form-item label="建设内容">
              <el-input
                v-model="form.sections.constructionContent"
                type="textarea"
                :rows="9"
                placeholder="建议按以下格式逐行填写：&#10;该项目部署在客户侧机房，主要包括视频解析运算服务。&#10;应用层：填写应用功能和业务能力。&#10;平台层：填写平台部署位置和平台能力。&#10;传输层：填写专线、政务外网等网络能力。&#10;感知层：填写数据来源、设备和感知资源。"
              />
            </el-form-item>
            <el-form-item label="系统架构及图例">
              <input type="file" accept=".png,.jpg,.jpeg" @change="uploadEvidence('ARCHITECTURE_DIAGRAM', $event)" />
              <div class="upload-tip">建议上传横向 PNG/JPG 图片；生成PPT时将自动放入第4页系统架构图区域。</div>
              <div v-for="a in attachments.filter(x => x.attachmentType === 'ARCHITECTURE_DIAGRAM')" :key="a.id">
                <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="4 需求分析（能力供给）" name="capability">
          <el-alert title="填写内容将写入模板第5页，并与七融分析表一并生成。" type="info" :closable="false" style="margin-bottom: 16px" />
          <el-form label-position="top">
            <el-form-item label="根据该项目建设内容，主要包括以下需求：">
              <el-select v-model="form.sections.capabilityDemandSelections" multiple clearable placeholder="选择需求类型（可多选）" style="width:100%">
                <el-option v-for="item in capabilityDemandOptions" :key="item.key" :label="item.label" :value="item.key" />
              </el-select>
            </el-form-item>
            <el-form-item v-for="item in capabilityDemandOptions.filter(item => form.sections.capabilityDemandSelections?.includes(item.key))" :key="item.key" :label="item.label">
              <el-input v-model="form.sections.capabilityDemandDetails[item.key]" type="textarea" :rows="2" :placeholder="item.placeholder" />
            </el-form-item>
            <el-form-item label="补充说明（原有内容保留，可修改或清空）"><el-input v-model="form.sections.capabilityDemandExtra" type="textarea" :rows="2" /></el-form-item>
            <div class="form-tip">仅选中的需求及其已填写内容会生成到PPT；未选需求的模板示例不再显示。</div>
            <el-form-item label="✓ 经“七融解析”后，">
              <el-input
                v-model="form.sections.sevenFusionSummary"
                type="textarea"
                :rows="3"
                placeholder="填写本项目融入移动云、5G专网、专线、AI、自有平台等能力及金额占比分析。"
              />
            </el-form-item>
          </el-form>

          <h3>项目七融分析表（一）</h3>
          <div class="fusion-table-scroll">
            <table class="fusion-table">
              <thead><tr><th>七融</th><th>产品类型</th><th>是否能融</th><th>融了没有</th><th>金额（万元）</th><th>备注</th></tr></thead>
              <tbody>
                <tr v-for="(row, rowIndex) in fusionLeftRows" :key="row.key">
                  <td v-if="rowIndex < 3" class="fusion-name">{{ row.fusion }}</td>
                  <td v-else-if="rowIndex === 3" rowspan="2" class="fusion-name">融AI</td>
                  <td>{{ row.product }}</td>
                  <td><el-select v-model="form.sections.fusionLeftTable[row.key].canIntegrate" placeholder="请选择" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></td>
                  <td><el-select v-model="form.sections.fusionLeftTable[row.key].integrated" placeholder="请选择" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></td>
                  <td><el-input v-model="form.sections.fusionLeftTable[row.key].amount" /></td>
                  <td><el-input v-model="form.sections.fusionLeftTable[row.key].remark" /></td>
                </tr>
              </tbody>
            </table>
          </div>

          <h3 style="margin-top: 22px">项目七融分析表（二）</h3>
          <div class="fusion-table-scroll">
            <table class="fusion-table">
              <thead><tr><th>七融</th><th>产品类型</th><th>是否能融</th><th>融了没有</th><th>金额（万元）</th><th>备注</th></tr></thead>
              <tbody>
                <tr v-for="(row, rowIndex) in fusionRightRows" :key="row.key">
                  <td v-if="rowIndex === 0" rowspan="7" class="fusion-name">融中台</td>
                  <td v-else-if="rowIndex === 7" rowspan="2" class="fusion-name">融集成</td>
                  <td v-else-if="rowIndex === 9" rowspan="2" class="fusion-name">融C</td>
                  <td>{{ row.product }}</td>
                  <td><el-select v-model="form.sections.fusionRightTable[row.key].canIntegrate" placeholder="请选择" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></td>
                  <td><el-select v-model="form.sections.fusionRightTable[row.key].integrated" placeholder="请选择" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></td>
                  <td><el-input v-model="form.sections.fusionRightTable[row.key].amount" /></td>
                  <td v-if="rowIndex === 0" rowspan="7">
                    <el-input v-model="form.sections.fusionRightTable[row.key].remark" type="textarea" :rows="8" />
                  </td>
                  <td v-else-if="rowIndex === 7" rowspan="2"><el-input v-model="form.sections.fusionRightTable[row.key].remark" type="textarea" :rows="3" /></td>
                  <td v-else-if="rowIndex >= 9"><el-input v-model="form.sections.fusionRightTable[row.key].remark" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="5 收支分析" name="finance">
          <section class="finance-template-page">
          <h3>收支分析【投入情况】</h3>
          <el-alert :title="investmentSummaryPreview" type="info" :closable="false" style="margin-bottom: 16px" />
          <div class="form-tip" style="margin-bottom: 16px">请在下方固定结构的“投入明细”表中选择涉及的模式并填写金额；总投入及各类小计自动汇总并写入PPT。仅勾选的模式及其明细、小计会保留在PPT表格中，未勾选的模式不会导出。</div>

          <div class="investment-followup-pages">
          <el-divider content-position="left">投入情况补充页</el-divider>
          <el-form label-position="top">
            <el-form-item label="是否有集采产品差异性对比">
              <el-switch
                v-model="form.sections.procurementComparisonEnabled"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
          </el-form>
          <template v-if="form.sections.procurementComparisonEnabled">
            <div class="table-head">收支分析（集采产品差异性对比）</div>
            <el-form label-position="top">
              <el-form-item label="采购方式">
                <el-radio-group v-model="form.sections.procurementType">
                  <el-radio value="PUBLIC">公开采购</el-radio>
                  <el-radio value="DIRECT">直接采购</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
            <template v-if="form.sections.procurementType === 'PUBLIC'">
              <el-form label-position="top">
              <el-form-item label="公开采购场景说明">
                <el-input v-model="form.sections.procurementPublicDescription" type="textarea" :rows="3" />
              </el-form-item>
              </el-form>
              <el-table :data="form.sections.procurementPublicRows" border>
              <el-table-column label="采购内容" min-width="130"><template #default="{ row }"><el-input v-model="row.content" /></template></el-table-column>
              <el-table-column label="单位" min-width="90"><template #default="{ row }"><el-input v-model="row.unit" /></template></el-table-column>
              <el-table-column label="数量" min-width="90"><template #default="{ row }"><el-input v-model="row.quantity" /></template></el-table-column>
              <el-table-column label="一采产品名称" min-width="160"><template #default="{ row }"><el-input v-model="row.standardProduct" /></template></el-table-column>
              <el-table-column label="集采供应商1" min-width="140"><template #default="{ row }"><el-input v-model="row.supplier1" /></template></el-table-column>
              <el-table-column label="集采供应商2" min-width="140"><template #default="{ row }"><el-input v-model="row.supplier2" /></template></el-table-column>
              <el-table-column label="集采供应商3" min-width="140"><template #default="{ row }"><el-input v-model="row.supplier3" /></template></el-table-column>
              <el-table-column label="其他供应商" min-width="140"><template #default="{ row }"><el-input v-model="row.otherSupplier" /></template></el-table-column>
              <el-table-column label="与一采产品差异" min-width="220"><template #default="{ row }"><el-input v-model="row.difference" /></template></el-table-column>
              <el-table-column label="本次采购需求说明" min-width="220"><template #default="{ row }"><el-input v-model="row.requirement" /></template></el-table-column>
              </el-table>
            </template>

            <template v-else>
              <el-form label-position="top" style="margin-top: 16px">
              <el-form-item label="直接采购场景说明">
                <el-input v-model="form.sections.procurementDirectDescription" type="textarea" :rows="3" />
              </el-form-item>
            </el-form>
            <el-table :data="form.sections.procurementDirectRows" border>
              <el-table-column label="采购内容" min-width="130"><template #default="{ row }"><el-input v-model="row.content" /></template></el-table-column>
              <el-table-column label="单位" min-width="90"><template #default="{ row }"><el-input v-model="row.unit" /></template></el-table-column>
              <el-table-column label="数量" min-width="90"><template #default="{ row }"><el-input v-model="row.quantity" /></template></el-table-column>
              <el-table-column label="一采产品名称" min-width="170"><template #default="{ row }"><el-input v-model="row.standardProduct" /></template></el-table-column>
              <el-table-column label="与一采产品差异" min-width="220"><template #default="{ row }"><el-input v-model="row.difference" /></template></el-table-column>
              <el-table-column label="本次采购需求说明" min-width="240"><template #default="{ row }"><el-input v-model="row.requirement" /></template></el-table-column>
              </el-table>
              <el-form label-position="top" style="margin-top: 16px">
              <el-form-item label="一采非标报备合理性评审">
                <el-input v-model="form.sections.procurementAssessment" type="textarea" :rows="2" />
              </el-form-item>
              </el-form>
            </template>
          </template>

          <el-form label-position="top" style="margin-top: 18px">
            <el-form-item label="是否有IDC支出">
              <el-switch v-model="form.sections.idcEnabled" inline-prompt active-text="是" inactive-text="否" />
            </el-form-item>
          </el-form>
          <template v-if="form.sections.idcEnabled">
            <div class="table-head">收支分析（IDC测算）</div>
            <h4>IDC成分测算合计</h4>
            <el-table :data="form.sections.idcMainRows" border>
              <el-table-column label="计费单元" min-width="150"><template #default="{ row, $index }"><el-input v-if="$index === 0" v-model="row.billingUnit" placeholder="例如：IDC成本（47架5kW机柜）" /></template></el-table-column>
              <el-table-column label="费用类别" min-width="120"><template #default="{ row }">{{ row.category }}</template></el-table-column>
              <el-table-column label="费用说明" min-width="120"><template #default="{ row }">{{ row.description }}</template></el-table-column>
              <el-table-column label="占用机柜数量" min-width="120"><template #default="{ row }"><el-input v-model="row.cabinetCount" /></template></el-table-column>
              <el-table-column label="单价" min-width="140"><template #default="{ row }"><el-input v-model="row.unitPrice" /></template></el-table-column>
              <el-table-column label="合同期（月）" min-width="120"><template #default="{ row }"><el-input v-model="row.contractMonths" /></template></el-table-column>
              <el-table-column label="总成本（万元，不含税）" min-width="170"><template #default="{ row }"><el-input v-model="row.totalCost" /></template></el-table-column>
              <el-table-column label="备注" min-width="220"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
            </el-table>

            <h4>机柜配套部分</h4>
            <el-table :data="form.sections.idcCabinetRows" border>
              <el-table-column label="序号" min-width="70"><template #default="{ row }">{{ row.no }}</template></el-table-column>
              <el-table-column label="项目" min-width="120"><template #default="{ row }">{{ row.item }}</template></el-table-column>
              <el-table-column label="折旧年限" min-width="100"><template #default="{ row }"><el-input v-model="row.depreciationYears" /></template></el-table-column>
              <el-table-column label="投资金额（万元）" min-width="130"><template #default="{ row }"><el-input v-model="row.investmentAmount" /></template></el-table-column>
              <el-table-column label="拟承载机柜数（个）" min-width="140"><template #default="{ row }"><el-input v-model="row.plannedCabinets" /></template></el-table-column>
              <el-table-column label="月均成本（元/架）" min-width="140"><template #default="{ row }"><el-input v-model="row.monthlyCost" /></template></el-table-column>
              <el-table-column label="备注" min-width="200"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
            </el-table>

            <h4>维护成本</h4>
            <el-table :data="form.sections.idcMaintenanceRows" border>
              <el-table-column label="序号" min-width="70"><template #default="{ row }">{{ row.no }}</template></el-table-column>
              <el-table-column label="项目" min-width="120"><template #default="{ row }">{{ row.item }}</template></el-table-column>
              <el-table-column label="期间" min-width="100"><template #default="{ row }"><el-input v-model="row.period" /></template></el-table-column>
              <el-table-column label="维护费（万元）" min-width="120"><template #default="{ row }"><el-input v-model="row.maintenanceFee" /></template></el-table-column>
              <el-table-column label="机柜数（个）" min-width="110"><template #default="{ row }"><el-input v-model="row.cabinetCount" /></template></el-table-column>
              <el-table-column label="月均成本（元/架）" min-width="140"><template #default="{ row }"><el-input v-model="row.monthlyCost" /></template></el-table-column>
              <el-table-column label="备注" min-width="200"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
            </el-table>

            <h4>电费成本测算</h4>
            <el-table :data="form.sections.idcElectricityRows" border>
              <el-table-column label="机柜（个）" min-width="140"><template #default="{ row }"><el-input v-model="row.cabinet" /></template></el-table-column>
              <el-table-column label="电价（元/度）" min-width="120"><template #default="{ row }"><el-input v-model="row.electricityPrice" /></template></el-table-column>
              <el-table-column label="PUE" min-width="90"><template #default="{ row }"><el-input v-model="row.pue" /></template></el-table-column>
              <el-table-column label="电利用率" min-width="100"><template #default="{ row }"><el-input v-model="row.utilization" /></template></el-table-column>
              <el-table-column label="月均成本（元/KW）" min-width="150"><template #default="{ row }"><el-input v-model="row.monthlyCost" /></template></el-table-column>
              <el-table-column label="备注" min-width="200"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
            </el-table>
          </template>
          </div>

          <div class="table-head">投入明细（对应模板表格）</div>
          <el-form label-position="top" class="cost-mode-selector">
            <el-form-item label="投入模式（可多选）">
              <el-checkbox-group v-model="form.sections.costPrimaryModes" @change="syncCostModes">
                <el-checkbox v-for="mode in costPrimaryOptions" :key="mode" :value="mode">{{ mode }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item v-if="form.sections.costPrimaryModes.includes('成本部分')" label="成本分类（可多选）">
              <el-checkbox-group v-model="form.sections.cooperationCostModes" @change="syncCostModes">
                <el-checkbox v-for="mode in cooperationCostOptions" :key="mode" :value="mode">{{ mode }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <el-empty v-if="!costDisplayRows.length" description="请选择投入模式后新增明细" :image-size="72" />
          <el-table v-else :data="costDisplayRows" :span-method="investmentSpan" border>
            <el-table-column label="模式" min-width="120">
              <template #default="{ row }">
                <strong>{{ row.__total ? '合计' : costSection(row.mode) }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="" min-width="130"><template #default="{ row }"><strong v-if="costSection(row.mode) === '成本部分'">{{ row.mode }}</strong></template></el-table-column>
            <el-table-column label="明细" min-width="190"><template #default="{ row }"><strong v-if="row.__subtotal">小计</strong><el-input v-else-if="!row.__total" v-model="row.name" /></template></el-table-column>
            <el-table-column label="含税支出（万元）" min-width="145"><template #default="{ row }"><strong v-if="row.__subtotal || row.__total">{{ Number(row.amountIncTax || 0).toFixed(2) }}</strong><el-input v-else v-model="row.amountIncTax" inputmode="decimal" @input="amountEx(row)" /></template></el-table-column>
            <el-table-column label="税率（%）" min-width="110"><template #default="{ row }"><el-input v-if="!row.__subtotal && !row.__total" v-model="row.taxRate" inputmode="decimal" @input="amountEx(row)" /></template></el-table-column>
            <el-table-column label="不含税支出（万元，自动）" min-width="175"><template #default="{ row }"><strong>{{ Number(row.amountExTax || 0).toFixed(2) }}</strong></template></el-table-column>
            <el-table-column label="说明" min-width="220"><template #default="{ row }"><el-input v-if="!row.__subtotal && !row.__total" v-model="row.description" /></template></el-table-column>
            <el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.__subtotal" type="primary" link @click="addCostItem(row.mode)">新增</el-button><el-button v-else-if="!row.__total" type="danger" link @click="removeCostItem(row)">删除</el-button></template></el-table-column>
          </el-table>
          </section>

          <section class="finance-template-page finance-revenue-page">
          <h3>收支分析【收益情况】</h3>
          <el-alert :title="`总收益：${summarizeFinanceItems(form.incomeItems).amountIncTax.toFixed(2)}万元`" type="success" :closable="false" style="margin-bottom: 16px" />
          <div class="form-tip">总收益由下方已选择模式的收益明细自动合计；不含税金额按含税金额和税率计算。此处汇总与PPT收益情况页使用同一份明细，不计入单独描述的间接收益。</div>
          <el-form label-position="top">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="总收益（含税/万元，自动汇总）"><el-input :model-value="summarizeFinanceItems(form.incomeItems).amountIncTax.toFixed(2)" readonly /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="间接收益"><el-input v-model="form.sections.indirectRevenue" placeholder="例如：如带动专线/物联网卡收入，共计XX万元。" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="项目年限/协议期（年）"><el-input-number v-model="form.agreementYears" :min="1" :max="10" :precision="0" :controls="false" style="width:100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="带动商机"><el-input v-model="form.sections.drivenOpportunity" placeholder="填写带动的新商机、建设内容、预计采购时间和签约额。" /></el-form-item></el-col>
            </el-row>
          </el-form>
          <div class="table-head">收益明细</div>
          <el-form-item label="收益模式（可多选）">
            <el-checkbox-group v-model="form.sections.revenueModes" @change="syncRevenueModes">
              <el-checkbox v-for="mode in revenueModeOptions" :key="mode" :value="mode">{{ mode }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <div class="table-head"><span>收益明细</span></div>
          <el-empty v-if="!revenueDisplayRows.length" description="请选择收益模式后新增明细" :image-size="72" />
          <el-table v-else :data="revenueDisplayRows" :span-method="revenueDynamicSpan" border>
            <el-table-column label="模式" min-width="130"><template #default="{ row }"><strong>{{ row.mode }}</strong></template></el-table-column>
            <el-table-column label="明细" min-width="190"><template #default="{ row }"><strong v-if="row.__subtotal">小计</strong><el-input v-else-if="!row.__total" v-model="row.name" /></template></el-table-column>
            <el-table-column label="含税收益（万元）" min-width="145"><template #default="{ row }"><strong v-if="row.__subtotal || row.__total">{{ Number(row.amountIncTax || 0).toFixed(2) }}</strong><el-input v-else v-model="row.amountIncTax" inputmode="decimal" @input="amountEx(row)" /></template></el-table-column>
            <el-table-column label="税率（%）" min-width="110"><template #default="{ row }"><el-input v-if="!row.__subtotal && !row.__total" v-model="row.taxRate" inputmode="decimal" @input="amountEx(row)" /></template></el-table-column>
            <el-table-column label="不含税收益（万元，自动）" min-width="175"><template #default="{ row }"><strong>{{ Number(row.amountExTax || 0).toFixed(2) }}</strong></template></el-table-column>
            <el-table-column label="说明" min-width="220"><template #default="{ row }"><el-input v-if="!row.__subtotal && !row.__total" v-model="row.description" /></template></el-table-column>
            <el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="row.__subtotal" type="primary" link @click="addRevenueItem(row.mode)">新增</el-button><el-button v-else-if="!row.__total" type="danger" link @click="removeRevenueItem(row)">删除</el-button></template></el-table-column>
          </el-table>
          </section>

          <el-divider />
          <div v-if="false" v-for="mode in form.sections.revenueModes" :key="mode" class="revenue-mode-block">
            <div class="table-head"><span>{{ mode }}</span><el-button type="primary" link @click="addRevenueItem(mode)">新增明细</el-button></div>
            <el-table :data="revenueItems(mode)" border show-summary :summary-method="revenueSummaryMethod(mode)">
              <el-table-column label="明细" min-width="190"><template #default="{ row }"><el-input v-model="row.name" /></template></el-table-column>
              <el-table-column label="含税收益（万元）" min-width="145"><template #default="{ row }"><el-input-number v-model="row.amountIncTax" :controls="false" style="width:100%" @change="amountEx(row)" /></template></el-table-column>
              <el-table-column label="税率（%）" min-width="110"><template #default="{ row }"><el-input-number v-model="row.taxRate" :controls="false" style="width:100%" @change="amountEx(row)" /></template></el-table-column>
              <el-table-column label="不含税收益（万元）" min-width="155"><template #default="{ row }"><el-input-number v-model="row.amountExTax" :controls="false" style="width:100%" /></template></el-table-column>
              <el-table-column label="说明" min-width="220"><template #default="{ row }"><el-input v-model="row.description" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button type="danger" link @click="removeRevenueItem(row)">删除</el-button></template></el-table-column>
            </el-table>
          </div>
          <el-table v-if="false" :data="form.incomeItems" :span-method="revenueSpan" border>
            <el-table-column label="模式" min-width="130"><template #default="{ row }"><strong>{{ row.mode }}</strong></template></el-table-column>
            <el-table-column label="明细" min-width="190"><template #default="{ row }">{{ row.name }}</template></el-table-column>
            <el-table-column label="含税收益（万元）" min-width="145"><template #default="{ row }"><el-input-number v-model="row.amountIncTax" :controls="false" style="width:100%" @change="amountEx(row)" /></template></el-table-column>
            <el-table-column label="税率（%）" min-width="110"><template #default="{ row }"><el-input-number v-model="row.taxRate" :controls="false" style="width:100%" @change="amountEx(row)" /></template></el-table-column>
            <el-table-column label="不含税收益（万元）" min-width="155"><template #default="{ row }"><el-input-number v-model="row.amountExTax" :controls="false" style="width:100%" /></template></el-table-column>
            <el-table-column label="说明" min-width="240"><template #default="{ row }"><el-input v-model="row.description" /></template></el-table-column>
          </el-table>

          <h3 style="margin-top: 28px">收支分析（收益分析）</h3>
          <div class="table-head benefit-table-head">
            <span>收益测算指标明细</span>
          </div>
          <el-form label-position="top">
            <el-form-item label="收益分析模式（自动跟随上方收益情况）">
              <el-checkbox-group v-model="form.sections.benefitAnalysisSelectedModes" class="benefit-mode-selector" disabled>
                <el-checkbox v-for="mode in benefitModeOptions.filter(item => form.sections.benefitAnalysisSelectedModes.includes(item.key))" :key="mode.key" :value="mode.key" border>
                  {{ mode.label }}<small>（{{ mode.metricCount }}项指标）</small>
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <div class="form-tip" style="margin-bottom: 10px">请在上方“收益情况”选择模式；收益分析、招投标对比和附表同步联动。每种模式均配套收入/支出明细表和经济效益测算表，未选择模式不生成。</div>
          <el-empty v-if="!selectedBenefitAnalysisRows.length" description="请先勾选本项目涉及的收益分析模式" :image-size="72" />
          <el-table v-else :data="selectedBenefitAnalysisRows" :span-method="benefitGroupSpan" border>
            <el-table-column label="序号" min-width="90"><template #default="{ row }">{{ row.no || row.mode }}</template></el-table-column>
            <el-table-column prop="mode" label="模式" min-width="220" />
            <el-table-column prop="category" label="分类" min-width="190" />
            <el-table-column prop="metric" label="指标" min-width="170" />
            <el-table-column prop="projectValue" label="该项目（自动计算）" min-width="170" />
            <el-table-column prop="requirement" label="管控要求" min-width="280" />
          </el-table>

          <h3 style="margin-top: 28px">招投标项目对比</h3>
          <el-form label-position="top">
            <el-form-item label="是否为已完成招投标的项目">
              <el-radio-group v-model="form.sections.preDecisionApproved">
                <el-radio :value="true">是</el-radio>
                <el-radio :value="false">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template v-if="form.sections.preDecisionApproved">
            <el-form label-position="top">
              <el-form-item label="招投标至立项变化说明">
                <el-input
                  v-model="form.sections.preDecisionSummary"
                  type="textarea"
                  :rows="3"
                  placeholder="填写项目整体净利润率变化及主要变化原因。"
                />
              </el-form-item>
            </el-form>
            <div class="table-head benefit-table-head"><span>招投标指标对比明细（跟随收益情况模式）</span></div>
            <el-table :data="form.sections.preDecisionComparisonRows" :span-method="preDecisionDynamicSpan" border>
              <el-table-column label="序号" min-width="80"><template #default="{ row }">{{ row.no || row.mode }}</template></el-table-column>
              <el-table-column prop="mode" label="模式" min-width="210" />
              <el-table-column prop="category" label="分类" min-width="130" />
              <el-table-column prop="metric" label="指标" min-width="160" />
              <el-table-column label="招投标环节" min-width="150">
                <template #default="{ row }"><el-input v-model="row.preDecision" /></template>
              </el-table-column>
              <el-table-column label="立项环节" min-width="150">
                <template #default="{ row }">{{ row.initiation }}</template>
              </el-table-column>
              <el-table-column label="趋势" min-width="100">
                <template #default="{ row }">
                  <el-select v-model="row.trend" clearable>
                    <el-option label="上升 ↑" value="↑" />
                    <el-option label="下降 ↓" value="↓" />
                    <el-option label="持平 —" value="—" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <section class="economic-detail-panel">
            <div class="table-head calculator-head">
              <div>
                <h3>基础数据（标准收支测算表）</h3>
                <div class="form-tip">CT、IT 含税金额可填写；模板已给出的税率固定不变，模板留空的税率可按实际情况填写。合同金额、增值税、不含税金额及各级小计均自动计算。</div>
              </div>
            </div>
            <EconomicBaseDetailTable v-model="form.sections.economicBaseDetailValues" />
          </section>

          <section class="economic-base-panel">
            <div class="table-head calculator-head">
              <div>
                <h3>基础数据自动测算</h3>
                <div class="form-tip">根据上方标准基础数据表汇总预计金额。各年度初始投资、收入、支出由填表人按实际计划填写，不再自动平均分配。</div>
              </div>
              <div class="calculator-actions">
                <el-button type="primary" @click="readEconomicBaseDataFromDetails">重新汇总</el-button>
              </div>
            </div>
            <div class="economic-year-control">
              <span>项目年限</span>
              <el-input-number v-model="form.agreementYears" :min="1" :max="10" :precision="0" :controls="false" class="base-year-input" />
              <small>年（决定黄色填写年度；公式继续展示至第10年）</small>
            </div>
            <div class="economic-summary-scroll">
              <table class="economic-summary-table">
                <tbody><tr>
                  <th>预计投资金额</th><td>{{ Number(form.sections.economicBaseData.estimatedInvestment || 0).toFixed(2) }}</td>
                  <th>预计收入金额</th><td>{{ Number(form.sections.economicBaseData.directRevenue || 0).toFixed(2) }}</td>
                  <th>预计成本金额</th><td>{{ Number(form.sections.economicBaseData.operatingExpense || 0).toFixed(2) }}</td>
                </tr></tbody>
              </table>
            </div>
            <div class="form-tip">预计投资=投资小计；预计收入=项目收入小计；预计成本=成本小计。以上均取标准基础数据表中的不含税金额（万元）。</div>
            <el-alert :title="`请在下方明黄色单元格中填写第1至第 ${Math.max(1, Math.min(10, Number(form.agreementYears || 1)))} 年的实际计划金额；公式结果会立即联动。`" type="info" :closable="false" show-icon />
          </section>

          <DepreciationScheduleCalculator
            :year-count="DEPRECIATION_HORIZON_YEARS"
            :category-totals="calculateEconomicBaseDetailTotals(form.sections.economicBaseDetailValues).investmentCategories"
            :project-year-count="Number(form.agreementYears || 1)"
            :schedule="depreciationSchedule()"
            @update-input="updateDepreciationInput"
          />

          <EconomicBenefitCalculator
            title="项目整体经济效益测算（与基础数据配套生成）"
            :year-count="form.sections.economicBenefitYearCount"
            :inputs="economicInputs('economicBenefitInputs')"
            :outputs="economicInputs('economicBenefitValues')"
            :project-year-count="Number(form.agreementYears || 1)"
            :calculated-input-keys="['depreciation']"
            allow-year-change
            restore-label="恢复预计收入/成本"
            @update-input="payload => updateEconomicInput('economicBenefitInputs', payload)"
            @change-years="changeEconomicBenefitYears"
            @restore="restoreOverallExpectedValues"
            @calculate="recalculateEconomicBenefits"
          />

          <el-alert
            v-if="selectedEconomicBenefitGroups.length"
            title="以下内容按已勾选模式生成；每个模式先列收入/支出明细，再独立测算，并自动回填对应指标及PPT。"
            type="success"
            :closable="false"
            show-icon
            style="margin-top: 28px"
          />
          <template v-for="group in selectedEconomicBenefitGroups" :key="group.key">
            <section class="mode-finance-source">
              <div class="table-head calculator-head">
                <div>
                  <h3>{{ `${group.no ? `${group.no} ` : ''}${group.mode}收入/支出表` }}</h3>
                  <div class="form-tip">数据取自前面的预计收入和预计成本明细；测算表首次生成时默认带入不含税合计，之后可在测算表中自行调整，不会被强制覆盖。</div>
                </div>
              </div>
              <el-table :data="modeFinanceRows(group.key)" border empty-text="该模式暂无收入或成本明细">
                <el-table-column prop="financeType" label="类别" width="110" />
                <el-table-column prop="mode" label="模式" min-width="145" />
                <el-table-column prop="name" label="明细" min-width="210" />
                <el-table-column label="含税金额（万元）" min-width="145"><template #default="{ row }">{{ Number(row.amountIncTax || 0).toFixed(2) }}</template></el-table-column>
                <el-table-column label="税率（%）" width="105"><template #default="{ row }">{{ Number(row.taxRate || 0).toFixed(2) }}</template></el-table-column>
                <el-table-column label="不含税金额（万元）" min-width="155"><template #default="{ row }">{{ Number(row.amountExTax || 0).toFixed(2) }}</template></el-table-column>
              </el-table>
            </section>
            <section class="mode-standard-table">
              <div class="table-head calculator-head">
                <div>
                  <h3>{{ `${group.no ? `${group.no} ` : ''}${group.mode}标准收支测算表` }}</h3>
                  <div class="form-tip">默认按上方该模式预计收入和预计成本生成。CT、IT 含税金额可继续填写；小计和税额自动计算。</div>
                </div>
                <el-button @click="restoreModeStandardTable(group.key)">恢复预计收支明细</el-button>
              </div>
              <EconomicBaseDetailTable v-model="form.sections.modeEconomicBaseDetailValues[group.key]" />
            </section>
            <EconomicBenefitCalculator
              :title="`收支分析（${group.no ? `${group.no} ` : ''}${group.mode}经济效益评估）`"
              :year-count="form.sections.economicBenefitYearCount"
              :inputs="modeEconomicInputs(group.key)"
              :outputs="modeEconomicOutputs(group.key)"
              :calculated-input-keys="modeHasAutomaticDepreciation() ? ['depreciation'] : []"
              :project-year-count="Number(form.agreementYears || 1)"
              restore-label="恢复标准表收入/成本"
              @update-input="payload => updateModeEconomicInput(group.key, payload)"
              @restore="restoreModeExpectedValues(group.key)"
              @calculate="recalculateEconomicBenefits"
            />
          </template>

          <el-form label-position="top" style="margin-top: 28px">
            <el-form-item label="是否经过垫资项目“三要素”评审">
              <el-switch
                v-model="form.sections.advancePaymentReviewEnabled"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </el-form-item>
            <template v-if="form.sections.advancePaymentReviewEnabled">
              <h3>收支分析（垫资项目“三要素”评审）</h3>
              <div class="form-tip" style="margin-bottom: 10px">
                适用于投资、非对称付款项目。填写以下三项评审结论后写入PPT。
              </div>
              <el-form-item label="需符合战略卡位场景">
                <el-input
                  v-model="form.sections.advancePaymentStrategyConclusion"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：该项目为智慧城市项目，经“一部两中心”综合评估符合要求。"
                />
              </el-form-item>
              <el-form-item label="需符合集团优单标准">
                <el-input
                  v-model="form.sections.advancePaymentPreferredOrderConclusion"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：该项目净现值率大于12%，符合高收益优单场景。"
                />
              </el-form-item>
              <el-form-item label="需符合能力沉淀要求">
                <el-input
                  v-model="form.sections.advancePaymentCapabilityConclusion"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：该项目符合沉淀中台能力场景，合作伙伴同意将能力上台签入合同。"
                />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="8">
                  <el-form-item label="战略卡位场景图片">
                    <input
                      type="file"
                      accept=".png,.jpg,.jpeg"
                      @change="uploadEvidence('ADVANCE_REVIEW_STRATEGY_IMAGE', $event)"
                    />
                    <div class="upload-tip">用于替换PPT第15页左侧图片区域。</div>
                    <div v-for="a in attachments.filter(x => x.attachmentType === 'ADVANCE_REVIEW_STRATEGY_IMAGE')" :key="a.id">
                      <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                      <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="集团优单标准图片">
                    <input
                      type="file"
                      accept=".png,.jpg,.jpeg"
                      @change="uploadEvidence('ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE', $event)"
                    />
                    <div class="upload-tip">用于替换PPT第15页中间图片区域。</div>
                    <div v-for="a in attachments.filter(x => x.attachmentType === 'ADVANCE_REVIEW_PREFERRED_ORDER_IMAGE')" :key="a.id">
                      <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                      <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="能力沉淀要求图片">
                    <input
                      type="file"
                      accept=".png,.jpg,.jpeg"
                      @change="uploadEvidence('ADVANCE_REVIEW_CAPABILITY_IMAGE', $event)"
                    />
                    <div class="upload-tip">用于替换PPT第15页右侧图片区域。</div>
                    <div v-for="a in attachments.filter(x => x.attachmentType === 'ADVANCE_REVIEW_CAPABILITY_IMAGE')" :key="a.id">
                      <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                      <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                    </div>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="“三要素”评审附件">
                <input
                  type="file"
                  accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.zip,.rar,.7z,.png,.jpg,.jpeg"
                  @change="uploadEvidence('ADVANCE_REVIEW_ATTACHMENT', $event)"
                />
                <div class="upload-tip">可上传评分表、签字表、优单标准、能力沉淀说明等证明材料。</div>
                <div v-for="a in attachments.filter(x => x.attachmentType === 'ADVANCE_REVIEW_ATTACHMENT')" :key="a.id">
                  <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                  <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                </div>
              </el-form-item>
            </template>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="6 生态甄选" name="risk">
          <el-form label-position="top">
            <h3>甄选方案决策</h3>
            <el-alert title="对应最新模板第16页；只填写本项目实际甄选信息，空项不会覆盖模板固定标题。" type="info" :closable="false" style="margin-bottom:16px" />
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="公司名称"><el-input v-model="form.sections.selectionCompany" placeholder="例如：XX公司" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="甄选方案决策日期"><el-date-picker v-model="form.sections.selectionPlanDecisionDate" type="date" value-format="YYYY年MM月DD日" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="12">
                <el-form-item label="甄选模式">
                  <el-select v-model="form.sections.selectionMode" style="width: 100%" clearable>
                    <el-option label="竞争性甄选" value="竞争性甄选" />
                    <el-option label="定向甄选" value="定向甄选" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否标后甄选">
                  <el-select v-model="form.sections.selectionAfterBid" style="width: 100%" clearable>
                    <el-option label="是" value="是" />
                    <el-option label="否" value="否" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24"><el-form-item label="甄选金额"><el-input v-model="form.sections.selectionAmountSummary" type="textarea" :rows="2" placeholder="例如：A包预算金额XX万元（含税），手续费XX万元；B包预算金额XX万元（含税），手续费XX万元。" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="标后甄选原因"><el-input v-model="form.sections.selectionAfterBidReason" type="textarea" :rows="2" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="评审小组"><el-input v-model="form.sections.selectionReviewPanel" type="textarea" :rows="2" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="评审标准"><el-input v-model="form.sections.selectionReviewStandard" type="textarea" :rows="2" /></el-form-item></el-col>
            </el-row>

            <h3 style="margin-top: 24px">甄选结果决策</h3>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="甄选结果决策日期"><el-date-picker v-model="form.sections.selectionResultDecisionDate" type="date" value-format="YYYY年MM月DD日" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="甄选评审日期"><el-date-picker v-model="form.sections.selectionReviewDate" type="date" value-format="YYYY年MM月DD日" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="评审委员会推荐意见"><el-input :model-value="selectionRecommendation" type="textarea" :rows="3" readonly placeholder="根据下方中选人和候选人自动生成" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="中选人"><el-input v-model="form.sections.selectionWinners" type="textarea" :rows="2" placeholder="例如：1包：XX、2包：XX（PPT按模板缩进显示）" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="候选人"><el-input v-model="form.sections.selectionCandidates" type="textarea" :rows="2" placeholder="例如：1包：XX、2包：XX（PPT按模板缩进显示）" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="甄选结果公示日期"><el-date-picker v-model="form.sections.selectionPublicationDate" type="date" value-format="YYYY年MM月DD日" style="width: 100%" /></el-form-item></el-col>
            </el-row>

            <h3 style="margin-top: 24px">甄选材料</h3>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="需求文件">
                  <input type="file" accept=".zip,.rar,.7z,.pdf,.doc,.docx,.xls,.xlsx" @change="uploadEvidence('SELECTION_REQUIREMENT_FILE', $event)" />
                  <div v-for="a in attachments.filter(x => x.attachmentType === 'SELECTION_REQUIREMENT_FILE')" :key="a.id">
                    <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                    <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="甄选过程">
                  <input type="file" accept=".zip,.rar,.7z,.pdf,.doc,.docx,.xls,.xlsx" @change="uploadEvidence('SELECTION_PROCESS_FILE', $event)" />
                  <div v-for="a in attachments.filter(x => x.attachmentType === 'SELECTION_PROCESS_FILE')" :key="a.id">
                    <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                    <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="甄选结果">
                  <input type="file" accept=".zip,.rar,.7z,.pdf,.doc,.docx,.xls,.xlsx" @change="uploadEvidence('SELECTION_RESULT_FILE', $event)" />
                  <div v-for="a in attachments.filter(x => x.attachmentType === 'SELECTION_RESULT_FILE')" :key="a.id">
                    <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                    <el-button link type="danger" :loading="removingAttachment === a.id" @click="removeAttachment(a)">删除</el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="7 资金保障与现金流" name="fund">
          <el-form label-position="top">
            <h3>资金分析【甲方情况分析】</h3>
            <h4>客户尽责调查情况</h4>
            <el-form-item label="客户尽责调查表（保留上传入口）">
              <input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg" @change="uploadEvidence('CLIENT_DUE_DILIGENCE_FILE', $event)" />
              <div v-for="a in attachments.filter(x => x.attachmentType === 'CLIENT_DUE_DILIGENCE_FILE')" :key="a.id">
                <el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button>
                <el-button link type="danger" @click="removeAttachment(a)">删除</el-button>
              </div>
            </el-form-item>
            <h4>甲方资信情况</h4>
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="集团名称"><el-input v-model="form.sections.clientGroupName" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="企业性质"><el-select v-model="form.sections.clientEnterpriseNature" clearable><el-option v-for="item in ['政府机关','事业单位','央企','国企','民企','其他']" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="成立时间"><el-date-picker v-model="form.sections.clientEstablishmentDate" type="date" value-format="YYYY年MM月DD日" style="width:100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="注册资本（万元）"><el-input v-model="form.sections.clientRegisteredCapital" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="实收注册资金（万元）"><el-input v-model="form.sections.clientPaidInCapital" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="参保人数"><el-input v-model="form.sections.clientInsuredEmployees" /></el-form-item></el-col>
            </el-row>
            <h4>项目信息情况</h4>
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="行业类型"><el-input v-model="form.sections.clientIndustryType" placeholder="例如：党政、执法、工业能源" /></el-form-item></el-col>
              <el-col :span="16"><el-form-item label="项目需求"><el-input v-model="form.sections.clientProjectDemand" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="客户实地拜访"><el-select v-model="form.sections.clientSiteVisit" clearable><el-option label="有" value="有" /><el-option label="无" value="无" /></el-select></el-form-item></el-col>
              <el-col :span="16"><el-form-item label="项目交付详细地址"><el-input v-model="form.sections.clientDeliveryAddress" /></el-form-item></el-col>
            </el-row>

            <div class="table-head" style="margin-top:24px"><span>往期履约情况</span><el-button type="primary" link @click="addClientPastPerformanceRow">新增项目</el-button></div>
            <el-form-item label="与移动合作履约情况"><el-input v-model="form.sections.clientMobilePerformance" type="textarea" :rows="2" placeholder="填写前期合作项目、是否正常履约及异常原因；未合作可填写未合作过DICT项目" /></el-form-item>
            <el-table :data="form.sections.clientPastPerformanceRows" border show-summary :summary-method="performanceSummary">
              <el-table-column label="序号" width="75"><template #default="{row}"><el-input v-model="row.no" /></template></el-table-column><el-table-column label="项目名称" min-width="150"><template #default="{row}"><el-input v-model="row.projectName" /></template></el-table-column><el-table-column label="商机编号" width="120"><template #default="{row}"><el-input v-model="row.opportunityNo" /></template></el-table-column><el-table-column label="签约日期" width="135"><template #default="{row}"><el-input v-model="row.signingDate" /></template></el-table-column><el-table-column label="合同总额（万元）" width="140"><template #default="{row}"><el-input v-model="row.contractAmount" /></template></el-table-column><el-table-column label="应付款（万元）" width="130"><template #default="{row}"><el-input v-model="row.payableAmount" /></template></el-table-column><el-table-column label="已付款（万元）" width="130"><template #default="{row}"><el-input v-model="row.paidAmount" /></template></el-table-column><el-table-column label="付款方式" width="120"><template #default="{row}"><el-select v-model="row.paymentMethod" clearable><el-option label="对称" value="对称" /><el-option label="非对称" value="非对称" /></el-select></template></el-table-column><el-table-column label="付款比例" width="110"><template #default="{row}"><el-input v-model="row.paymentRatio" /></template></el-table-column><el-table-column label="欠费金额（万元）" width="140"><template #default="{row}"><el-input v-model="row.arrearsAmount" /></template></el-table-column><el-table-column label="逾期欠费" width="110"><template #default="{row}"><el-input v-model="row.overdueArrears" /></template></el-table-column><el-table-column label="最小欠费账期" width="130"><template #default="{row}"><el-input v-model="row.minimumArrearsPeriod" /></template></el-table-column><el-table-column label="备注" min-width="130"><template #default="{row}"><el-input v-model="row.remark" /></template></el-table-column><el-table-column label="操作" fixed="right" width="80"><template #default="{$index}"><el-button link type="danger" @click="removeClientPastPerformanceRow($index)">删除</el-button></template></el-table-column>
            </el-table>
            <h4>其他甲方情况</h4><el-row :gutter="16"><el-col :span="8"><el-form-item label="以房抵债金额（万元）"><el-input v-model="form.sections.clientHousingDebtAmount" /></el-form-item></el-col><el-col :span="24"><el-form-item label="与其他单位合作履约情况"><el-input v-model="form.sections.clientOtherPerformance" type="textarea" :rows="2" /></el-form-item></el-col><el-col :span="8"><el-form-item label="非对称支付次数"><el-input v-model="form.sections.clientAsymmetricCount" /></el-form-item></el-col><el-col :span="8"><el-form-item label="非对称支付金额（万元）"><el-input v-model="form.sections.clientAsymmetricAmount" /></el-form-item></el-col><el-col :span="8"><el-form-item label="占全省非对称支付总额（%）"><el-input v-model="form.sections.clientAsymmetricRatio" /></el-form-item></el-col></el-row>
            <template v-if="form.sections.fundProjectType !== 'GOVERNMENT'">
              <h4>项目风险核查（政府单位不生成此部分）</h4>
              <el-row :gutter="16"><el-col :span="12"><el-form-item label="欠税公告、借款诉讼"><el-select v-model="form.sections.clientTaxDebtRisk" clearable><el-option label="曾因" value="曾因" /><el-option label="未因" value="未因" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="经营异常、合同纠纷"><el-select v-model="form.sections.clientAbnormalOperationRisk" clearable><el-option label="曾因" value="曾因" /><el-option label="未因" value="未因" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="劳务纠纷"><el-select v-model="form.sections.clientLaborDisputeRisk" clearable><el-option label="曾因" value="曾因" /><el-option label="未因" value="未因" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="股权、高管变动"><el-select v-model="form.sections.clientEquityManagementRisk" clearable><el-option label="存在" value="存在" /><el-option label="不存在" value="不存在" /></el-select></el-form-item></el-col></el-row>
            </template>

            <h3>资金保障分析</h3>
            <el-form-item label="项目类别">
              <el-radio-group v-model="form.sections.fundProjectType">
                <el-radio-button value="GOVERNMENT">政府类</el-radio-button>
                <el-radio-button value="ENTERPRISE">企业类</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="资金类型"><el-input v-model="form.sections.fundType" placeholder="例如：中央财政资金、奖补资金、自筹资金" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="资金风险等级"><el-select v-model="form.fundRiskLevel" clearable style="width: 100%"><el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" /></el-select></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="资金保障评估"><el-input v-model="form.sections.fundSource" type="textarea" :rows="4" placeholder="只填写本项目实际评估结论，生成PPT时替换模板示例文字" /></el-form-item></el-col>
            </el-row>
            <el-row v-if="form.sections.fundProjectType === 'GOVERNMENT'" :gutter="16">
              <el-col :span="12"><el-form-item label="政府部门预算公开文件说明"><el-input v-model="form.sections.fundGovernmentBudgetPublicDescription" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="项目对应支出科目说明"><el-input v-model="form.sections.fundGovernmentExpenditureSubjectDescription" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="项目支出绩效目标表说明"><el-input v-model="form.sections.fundGovernmentPerformanceTargetDescription" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="政府采购单等说明"><el-input v-model="form.sections.fundGovernmentProcurementDescription" /></el-form-item></el-col>
            </el-row>
            <el-row v-else :gutter="16">
              <el-col :span="24"><el-alert title="企业类项目：请填写客户内部立项、资金实力，并完成下方审计审查、相对方评价等企业类扣分项。" type="info" :closable="false" style="margin-bottom:12px" /></el-col>
              <el-col :span="12"><el-form-item label="客户内部立项文件说明"><el-input v-model="form.sections.fundEnterpriseInternalApprovalDescription" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="客户资金实力研判说明"><el-input v-model="form.sections.fundEnterpriseFinancialStrengthDescription" /></el-form-item></el-col>
            </el-row>
            <h4>资金证明材料</h4>
            <el-form-item label="请选择需要提供的证明材料（可多选）"><el-checkbox-group v-model="form.sections.fundProofMaterialSelections"><el-checkbox v-for="file in (form.sections.fundProjectType === 'GOVERNMENT' ? [{type:'FUND_BUDGET_PUBLIC_FILE',label:'政府部门预算公开文件'},{type:'FUND_EXPENDITURE_SUBJECT_FILE',label:'项目对应支出科目'},{type:'FUND_PERFORMANCE_TARGET_FILE',label:'项目支出绩效目标表'},{type:'FUND_PROCUREMENT_FILE',label:'政府采购单等'}] : [{type:'FUND_INTERNAL_APPROVAL_FILE',label:'客户内部立项文件'},{type:'FUND_FINANCIAL_STRENGTH_FILE',label:'客户资金实力证明'}])" :key="file.type" :value="file.type">{{ file.label }}</el-checkbox></el-checkbox-group></el-form-item>
            <el-row :gutter="16"><el-col v-for="file in (form.sections.fundProjectType === 'GOVERNMENT' ? [{type:'FUND_BUDGET_PUBLIC_FILE',label:'政府部门预算公开文件'},{type:'FUND_EXPENDITURE_SUBJECT_FILE',label:'项目对应支出科目'},{type:'FUND_PERFORMANCE_TARGET_FILE',label:'项目支出绩效目标表'},{type:'FUND_PROCUREMENT_FILE',label:'政府采购单等'}] : [{type:'FUND_INTERNAL_APPROVAL_FILE',label:'客户内部立项文件'},{type:'FUND_FINANCIAL_STRENGTH_FILE',label:'客户资金实力证明'}]).filter(file => form.sections.fundProofMaterialSelections.includes(file.type))" :key="file.type" :span="12"><el-form-item :label="file.label"><input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.zip" @change="uploadEvidence(file.type, $event)" /><div v-for="a in attachments.filter(x => x.attachmentType === file.type)" :key="a.id"><el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button><el-button link type="danger" @click="removeAttachment(a)">删除</el-button></div></el-form-item></el-col><el-col :span="12"><el-form-item label="资金分析模型及其他证明"><input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.zip" @change="uploadEvidence('FUND_ANALYSIS_MODEL_FILE', $event)" /><div v-for="a in attachments.filter(x => x.attachmentType === 'FUND_ANALYSIS_MODEL_FILE')" :key="a.id"><el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button><el-button link type="danger" @click="removeAttachment(a)">删除</el-button></div></el-form-item></el-col></el-row>

            <h4>历史项目欠收分析模型</h4>
            <div class="form-tip" style="margin-bottom: 10px">选择事项后自动带出对应扣分；“其他”事项可在扣分下拉框中自行选择。</div>
            <el-table :data="fundHistoryRows" border>
              <el-table-column prop="label" label="指标项" min-width="180" />
              <el-table-column label="事项" min-width="280">
                <template #default="{ row }">
                  <el-select
                    v-model="form.sections[row.item]"
                    :multiple="row.multiple"
                    filterable
                    clearable
                    placeholder="请选择事项"
                    style="width: 100%"
                    @change="applyFundHistoryItem(row)"
                  >
                    <el-option v-for="option in row.options" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分数项" min-width="160">
                <template #default="{ row }">
                  <el-input :model-value="`${Number(form.sections[row.deduction] || 0)} 分`" readonly class="auto-score" />
                </template>
              </el-table-column>
            </el-table>
            <div class="table-total">本项得分：100 - {{ fundScoreSummary.historyDeduction }} = {{ fundHistoryScore }} 分</div>

            <h4>当前项目资金风险分析模型</h4>
            <div class="form-tip" style="margin-bottom: 10px">选择风险事项后自动带出扣分，本项得分和最终得分同步更新。</div>
            <el-table :data="fundCurrentRows" border>
              <el-table-column prop="label" label="指标项" min-width="170" />
              <el-table-column prop="sequence" label="序号" width="70" align="center" />
              <el-table-column prop="requirement" label="扣分要求" min-width="460" />
              <el-table-column label="选择扣分情形" min-width="300">
                <template #default="{ row }">
                  <el-select v-model="form.sections[row.item]" filterable clearable placeholder="请选择是否涉及" style="width: 100%" @change="applyFundCurrentItem(row)">
                    <el-option v-for="option in row.options" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分数项" min-width="130">
                <template #default="{ row }"><el-input :model-value="`${Number(form.sections[row.deduction] || 0)} 分`" readonly class="auto-score" /></template>
              </el-table-column>
            </el-table>
            <div class="table-total">本项得分：100 - {{ fundScoreSummary.currentDeduction }} = {{ fundCurrentScore }} 分</div>

            <h4>特殊情况加分</h4>
            <div class="form-tip" style="margin-bottom: 10px">选择加分类型后自动带出分值；多个情况就高、不重复加分。</div>
            <el-table :data="[{ key: 'special' }]" border>
              <el-table-column label="加分类型（就高，不重复加分）" min-width="420">
                <template #default>
                  <el-select v-model="form.sections.fundSpecialBonusType" filterable clearable placeholder="请选择加分类型" style="width: 100%" @change="applyFundBonusType">
                    <el-option v-for="option in fundBonusOptions" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分数项" min-width="180">
                <template #default>
                  <el-input :model-value="`${Number(form.sections.fundSpecialBonusScore || 0)} 分`" readonly class="auto-score" />
                </template>
              </el-table-column>
            </el-table>
            <div class="table-total">本项得分：{{ fundScoreSummary.bonus }} 分</div>
            <el-alert :title="`最终得分：${fundHistoryScore} × 40% + ${fundCurrentScore} × 60% + ${fundScoreSummary.bonus} = ${fundFinalScore} 分`" type="success" :closable="false" />
            <el-alert :title="`评估结果：${fundRiskConclusion}（最终得分最高100分）`" type="info" :closable="false" style="margin-top: 10px" />

            <div class="table-head" style="margin-top: 24px"><span>后向厂家支出风险保障</span><el-button type="primary" link @click="addVendorRiskRow">新增厂家</el-button></div>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="中小企业保障"><el-input v-model="form.sections.vendorSmeProtection" placeholder="例如：收到款项后优先支付中小企业厂商" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="资质能力情况"><el-input v-model="form.sections.vendorQualificationSummary" placeholder="资质、代理级别及信用查询结论" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="往期履约情况"><el-input v-model="form.sections.vendorPerformanceSummary" placeholder="合作履约、诉讼及违约情况" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="经营风险情况"><el-input v-model="form.sections.vendorBusinessRiskSummary" placeholder="经营风险及内部诉讼核查结论" /></el-form-item></el-col>
            </el-row>
            <el-alert title="第一行固定保留；第二行起可新增、编辑、删除。" type="info" :closable="false" style="margin-bottom: 12px" />
            <el-table :data="form.sections.vendorRiskRows" border>
              <el-table-column label="后向单位名称" min-width="150"><template #default="{ row }"><el-input v-model="row.vendorName" /></template></el-table-column>
              <el-table-column label="金额占比（%）" width="120"><template #default="{ row }"><el-input v-model="row.amountRatio" /></template></el-table-column>
              <el-table-column label="是否中小企业" width="130"><template #default="{ row }"><el-select v-model="row.smeFlag" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></template></el-table-column>
              <el-table-column label="支出金额（含税）" width="140"><template #default="{ row }"><el-input v-model="row.spendAmount" /></template></el-table-column>
              <el-table-column label="支出备注" min-width="160"><template #default="{ row }"><el-input v-model="row.spendRemark" /></template></el-table-column>
              <el-table-column label="资质" width="120"><template #default="{ row }"><el-input v-model="row.qualification" /></template></el-table-column>
              <el-table-column label="代理级别" width="120"><template #default="{ row }"><el-input v-model="row.agentLevel" /></template></el-table-column>
              <el-table-column label="代理设备" width="120"><template #default="{ row }"><el-input v-model="row.agentEquipment" /></template></el-table-column>
              <el-table-column label="合作次数" width="100"><template #default="{ row }"><el-input v-model="row.cooperationCount" /></template></el-table-column>
              <el-table-column label="经营风险" min-width="140"><template #default="{ row }"><el-input v-model="row.businessRisk" /></template></el-table-column>
              <el-table-column label="证明截图说明" min-width="150"><template #default="{ row }"><el-input v-model="row.evidence" /></template></el-table-column>
              <el-table-column label="备注" min-width="130"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
              <el-table-column label="操作" fixed="right" width="80"><template #default="{ $index }"><el-button link type="danger" :disabled="$index === 0" @click="removeVendorRiskRow($index)">删除</el-button></template></el-table-column>
            </el-table>
            <el-row :gutter="16" style="margin-top:16px"><el-col v-for="file in [{type:'VENDOR_CREDIT_CHINA_FILE',label:'信用中国截图'},{type:'VENDOR_PAST_PERFORMANCE_FILE',label:'往期合作履约情况'},{type:'VENDOR_QUALIFICATION_FILE',label:'相关资质证明'}]" :key="file.type" :span="8"><el-form-item :label="file.label"><input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.zip" @change="uploadEvidence(file.type,$event)" /><div v-for="a in attachments.filter(x => x.attachmentType === file.type)" :key="a.id"><el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button><el-button link type="danger" @click="removeAttachment(a)">删除</el-button></div></el-form-item></el-col></el-row>

            <h3 style="margin-top: 24px">项目收付款方式与现金流向</h3>
            <div class="form-tip" style="margin-bottom: 12px">以下输入内容将原样写入PPT；选项和输入框中的示例提示不会自动写入。</div>
            <el-form-item label="项目收款方式"><el-input v-model="form.sections.projectReceiptMethod" type="textarea" :rows="2" placeholder="填写合同签订、初验、终验等收付款节点、工作日和比例" /></el-form-item>
            <el-form-item label="项目付款方式（先选择类型）">
              <el-select v-model="form.sections.projectPaymentMethods" multiple clearable placeholder="请选择适用的付款方式类型" style="width: 100%">
                <el-option v-for="item in paymentMethodOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item v-for="method in form.sections.projectPaymentMethods.filter((item: string) => paymentMethodTemplates[item])" :key="method" :label="method + '（具体条款）'">
              <el-input v-model="form.sections.projectPaymentDetails[method]" type="textarea" :rows="3" :placeholder="paymentMethodTemplates[method]" />
            </el-form-item>
            <div class="table-head"><span>项目协议期内现金流</span><el-button type="primary" link @click="addCashFlowRow">新增年度</el-button></div>
            <el-alert title="第1年固定保留；后续年度可新增、编辑、删除。" type="info" :closable="false" style="margin-bottom: 12px" />
            <el-table :data="form.sections.cashFlowRows" border show-summary :summary-method="cashFlowSummary">
              <el-table-column label="时间" width="100"><template #default="{ row, $index }"><el-input v-model="row.time" :disabled="$index === 0" /></template></el-table-column>
              <el-table-column label="项目收入（不含税，万元）" width="180"><template #default="{ row }"><el-input v-model="row.revenueExTax" /></template></el-table-column>
              <el-table-column label="项目收入（含税，万元）" width="170"><template #default="{ row }"><el-input v-model="row.revenueIncTax" /></template></el-table-column>
              <el-table-column label="项目收款（含税，万元）" width="170"><template #default="{ row }"><el-input v-model="row.receiptIncTax" /></template></el-table-column>
              <el-table-column label="项目支出（含税，万元）" width="170"><template #default="{ row }"><el-input v-model="row.expenseIncTax" /></template></el-table-column>
              <el-table-column label="当年收现率（%）" width="140"><template #default="{ row }"><el-input v-model="row.cashRealizationRate" /></template></el-table-column>
              <el-table-column label="说明" min-width="140"><template #default="{ row }"><el-input v-model="row.description" /></template></el-table-column>
              <el-table-column label="投资部分收入（含税，万元）" width="200"><template #default="{ row }"><el-input v-model="row.investmentIncomeIncTax" /></template></el-table-column>
              <el-table-column label="投资部分收入确认情况说明" min-width="210"><template #default="{ row }"><el-input v-model="row.investmentConfirmation" /></template></el-table-column>
              <el-table-column label="操作" fixed="right" width="80"><template #default="{ $index }"><el-button link type="danger" :disabled="$index === 0" @click="removeCashFlowRow($index)">删除</el-button></template></el-table-column>
            </el-table>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="8 风险评估与项目交付" name="delivery">
          <el-form label-position="top">
            <h3>风险评估</h3>
            <el-form-item label="风险评估总述"><el-input v-model="form.sections.riskSummary" type="textarea" :rows="2" placeholder="例如：经评估，本项目存在交付、运维风险点，通过管控手段均可控。" /></el-form-item>
            <div class="table-head"><span>风险明细（黄色字段固定）</span><el-button type="primary" link @click="addRiskRow">新增风险点</el-button></div>
            <el-table :data="form.sections.riskAssessmentRows" border>
              <el-table-column label="风险类别" width="160"><template #default="{ row }"><el-input v-model="row.category" :disabled="row.fixed" /></template></el-table-column>
              <el-table-column label="风险点" min-width="190"><template #default="{ row }"><el-input v-model="row.riskPoint" :disabled="row.fixed" /></template></el-table-column>
              <el-table-column label="本项目是否涉及该点以及风险描述" min-width="280"><template #default="{ row }"><el-input v-model="row.involvedDescription" type="textarea" :rows="2" /></template></el-table-column>
              <el-table-column label="管控手段" min-width="230"><template #default="{ row }"><el-input v-model="row.controlMeasures" type="textarea" :rows="2" /></template></el-table-column>
              <el-table-column label="评估结果" width="120"><template #default="{ row }"><el-select v-model="row.evaluationResult" clearable><el-option label="可控" value="可控" /><el-option label="不可控" value="不可控" /></el-select></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row, $index }"><el-button link type="danger" :disabled="row.fixed" @click="removeRiskRow($index)">删除</el-button></template></el-table-column>
            </el-table>

            <h3 style="margin-top: 24px">项目三线判定</h3>
            <el-form-item label="判定结果（可多选）"><el-checkbox-group v-model="form.sections.threeLineSelections"><el-checkbox value="红线">红线</el-checkbox><el-checkbox value="底线">底线</el-checkbox><el-checkbox value="高线">高线</el-checkbox></el-checkbox-group></el-form-item>
            <div class="form-tip">红线、底线、高线均需填写评价条件满足情况；不涉及的请填写“不涉及”。上方判定结果仅用于结论，不控制表格显示。</div>
            <el-table :data="selectedThreeLineRows" border>
              <el-table-column prop="level" label="判定层级" width="120" /><el-table-column prop="nature" label="性质" width="120" />
              <el-table-column label="本项目评价条件满足情况" min-width="320"><template #default="{ row }"><el-input v-model="row.satisfaction" type="textarea" :rows="2" /></template></el-table-column>
              <el-table-column label="备注" min-width="220"><template #default="{ row }"><el-input v-model="row.remark" type="textarea" :rows="2" /></template></el-table-column>
            </el-table>

            <h3 style="margin-top: 24px">项目交付</h3>
            <el-form-item label="交付责任"><el-input v-model="form.sections.deliveryResponsibility" type="textarea" :rows="3" placeholder="填写责任单位、交付经理及跟踪管理安排" /></el-form-item>
            <el-form-item label="合作伙伴"><el-input v-model="form.sections.deliveryPartners" type="textarea" :rows="3" placeholder="填写合作伙伴数量、总集成单位及甄选完成情况" /></el-form-item>
            <el-form-item label="工期情况"><el-input v-model="form.sections.deliverySchedule" type="textarea" :rows="4" placeholder="填写验收节点、延期责任及合同条款安排" /></el-form-item>
            <div class="table-head"><span>交付明细</span></div>
            <el-form-item label="交付模式（可多选）"><el-checkbox-group v-model="form.sections.deliveryModes"><el-checkbox v-for="mode in deliveryModeOptions" :key="mode" :value="mode">{{ mode }}</el-checkbox></el-checkbox-group></el-form-item>
            <el-empty v-if="!form.sections.deliveryModes.length" description="请先选择交付模式" :image-size="64" />
            <section v-for="mode in form.sections.deliveryModes" :key="mode" style="margin-bottom: 24px">
              <div class="table-head"><span>{{ mode }}</span><el-button type="primary" link @click="addDeliveryRow(mode)">新增明细</el-button></div>
              <el-empty v-if="!deliveryRowsFor(mode).length" description="暂无明细，请点击新增" :image-size="56" />
              <el-table v-else :data="deliveryRowsFor(mode)" border>
              <el-table-column label="序号" width="75"><template #default="{ row }"><el-input v-model="row.no" disabled /></template></el-table-column>
              <el-table-column label="名称" min-width="150"><template #default="{ row }"><el-input v-model="row.name" /></template></el-table-column>
              <el-table-column label="单位" width="90"><template #default="{ row }"><el-input v-model="row.unit" /></template></el-table-column>
              <el-table-column label="价格万元（含税）" width="140"><template #default="{ row }"><el-input v-model="row.priceIncTax" /></template></el-table-column>
              <el-table-column label="自有能力" width="130"><template #default="{ row }"><el-input v-model="row.ownCapability" /></template></el-table-column>
              <el-table-column label="后向单位" min-width="140"><template #default="{ row }"><el-input v-model="row.downstreamUnit" /></template></el-table-column>
              <el-table-column label="实际实施方" min-width="140"><template #default="{ row }"><el-input v-model="row.implementer" /></template></el-table-column>
              <el-table-column label="是否包施工、包维保" width="160"><template #default="{ row }"><el-input v-model="row.constructionMaintenance" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="danger" @click="removeDeliveryRow(deliveryRowIndex(row))">删除</el-button></template></el-table-column>
            </el-table>
            </section>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="9 决策与附录" name="appendix">
          <el-alert
            :title="uploadReminderTitle"
            :type="missingUploadItems.length ? 'warning' : 'success'"
            :closable="false"
            show-icon
            style="margin-bottom: 18px"
          >
            <template #default>
              PPT模板和收益率测算公式已内置，无需上传；测算输入均使用当前项目手工填写的数据。这里只提醒项目证明材料。已上传 {{ uploadedAttachmentTypes.size }} 类附件。
            </template>
          </el-alert>
          <el-form label-position="top">
            <h3>需决策事项</h3>
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="决策商务模式"><el-select v-model="form.sections.decisionMode" clearable><el-option v-for="mode in deliveryModeOptions" :key="mode" :label="mode" :value="mode" /></el-select></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="成本/投资/应付账款（万元，含税）"><el-input v-model="form.sections.decisionCostIncTax" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="项目总收入（万元，含税）"><el-input v-model="form.sections.decisionTotalRevenueIncTax" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="主营业务收入（万元，含税）"><el-input v-model="form.sections.decisionMainRevenueIncTax" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="其他业务收入（万元，含税）"><el-input v-model="form.sections.decisionOtherRevenueIncTax" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="是否一同决策标后甄选结果"><el-select v-model="form.sections.decisionIncludeSelection" clearable><el-option label="是，需要一同决策" value="是" /><el-option label="否，不添加此项" value="否" /></el-select></el-form-item></el-col>
            </el-row>

            <h3 style="margin-top:24px">附录1：维护要求</h3><el-switch v-model="form.sections.maintenanceEnabled" active-text="需要本附录" inactive-text="不需要" /><template v-if="form.sections.maintenanceEnabled">
            <el-row :gutter="16"><el-col :span="8"><el-form-item label="客户名称"><el-input v-model="form.sections.maintenanceCustomerName" /></el-form-item></el-col><el-col :span="8"><el-form-item label="客户详细地址"><el-input v-model="form.sections.maintenanceCustomerAddress" /></el-form-item></el-col><el-col :span="8"><el-form-item label="客户联系人"><el-input v-model="form.sections.maintenanceContact" /></el-form-item></el-col><el-col :span="8"><el-form-item label="客户联系电话"><el-input v-model="form.sections.maintenancePhone" /></el-form-item></el-col><el-col :span="8"><el-form-item label="客户级别"><el-input v-model="form.sections.maintenanceCustomerLevel" /></el-form-item></el-col><el-col :span="8"><el-form-item label="客户经理姓名"><el-input v-model="form.sections.maintenanceManager" /></el-form-item></el-col></el-row>
            <div class="table-head"><span>合同已购维保服务的软硬件清单</span><el-button type="primary" link @click="addSectionRow('maintenanceItems', { businessMode: '', itemName: '', quantityUnit: '', agreementYears: '', managementDepartment: '', maintenanceUnit: '' })">新增明细</el-button></div>
            <el-table :data="form.sections.maintenanceItems" border><el-table-column label="商务模式" width="150"><template #default="{row}"><el-select v-model="row.businessMode"><el-option v-for="mode in deliveryModeOptions" :key="mode" :label="mode" :value="mode" /></el-select></template></el-table-column><el-table-column label="软硬件名称" min-width="180"><template #default="{row}"><el-input v-model="row.itemName" /></template></el-table-column><el-table-column label="数量/单位" width="120"><template #default="{row}"><el-input v-model="row.quantityUnit" /></template></el-table-column><el-table-column label="协议期（年）" width="120"><template #default="{row}"><el-input v-model="row.agreementYears" /></template></el-table-column><el-table-column label="管理部门" min-width="160"><template #default="{row}"><el-input v-model="row.managementDepartment" /></template></el-table-column><el-table-column label="实际维护单位" min-width="160"><template #default="{row}"><el-input v-model="row.maintenanceUnit" /></template></el-table-column><el-table-column label="操作" width="80"><template #default="{$index}"><el-button link type="danger" @click="removeSectionRow('maintenanceItems',$index)">删除</el-button></template></el-table-column></el-table>
            <el-row :gutter="16" style="margin-top:16px"><el-col :span="8"><el-form-item label="是否全量购买维保"><el-select v-model="form.sections.maintenanceFullCoverage"><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item></el-col><el-col v-if="form.sections.maintenanceFullCoverage === '否'" :span="16"><el-form-item label="非全量说明"><el-input v-model="form.sections.maintenanceFullCoverageRemark" /></el-form-item></el-col><el-col :span="8"><el-form-item label="驻场服务"><el-select v-model="form.sections.onsiteService"><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item></el-col><el-col :span="16"><el-form-item label="驻场服务提供方"><el-input v-model="form.sections.onsiteProvider" /></el-form-item></el-col><el-col :span="8"><el-form-item label="重保服务"><el-select v-model="form.sections.keySupportService"><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item></el-col><el-col :span="16"><el-form-item label="重保服务要求及提供方说明"><el-input v-model="form.sections.keySupportDescription" /></el-form-item></el-col></el-row>
            <el-row :gutter="16"><el-col v-for="prefix in ['line','fiveG']" :key="prefix" :span="24"><h4>{{ prefix === 'line' ? '维护标准表-专线' : '维护标准表-5G专网' }}</h4><el-row :gutter="16"><el-col :span="12"><el-form-item label="保障等级"><el-input v-model="form.sections[prefix+'ProtectionLevel']" /></el-form-item></el-col><el-col :span="12"><el-form-item label="双路由/双机要求"><el-input v-model="form.sections[prefix+'DualRoute']" /></el-form-item></el-col><el-col :span="12"><el-form-item label="业务恢复时限（小时）"><el-input v-model="form.sections[prefix+'RecoveryHours']" /></el-form-item></el-col><el-col :span="12"><el-form-item label="允许阻断时长（分钟/年）"><el-input v-model="form.sections[prefix+'AllowedInterruption']" /></el-form-item></el-col><el-col :span="24"><el-form-item label="日常维护需求"><el-input v-model="form.sections[prefix+'DailyMaintenance']" /></el-form-item></el-col><el-col :span="12"><el-form-item label="网络部维护主管"><el-input v-model="form.sections[prefix+'Supervisor']" /></el-form-item></el-col><el-col :span="12"><el-form-item label="网络部经理"><el-input v-model="form.sections[prefix+'Manager']" /></el-form-item></el-col></el-row></el-col></el-row></template>

            <h3 style="margin-top:24px">附录2：省市招投标意见</h3><el-switch v-model="form.sections.preDecisionOpinionsEnabled" active-text="需要本附录" inactive-text="不需要" /><template v-if="form.sections.preDecisionOpinionsEnabled"><el-form-item label="省公司招投标意见"><el-input v-model="form.sections.provincialPreDecisionOpinion" type="textarea" :rows="4" /></el-form-item><el-form-item label="市公司招投标意见"><el-input v-model="form.sections.cityDecisionOpinion" type="textarea" :rows="4" /></el-form-item></template>
            <h3 style="margin-top:24px">附录3：存量业务情况</h3><el-switch v-model="form.sections.existingBusinessEnabled" active-text="需要本附录" inactive-text="不需要" /><template v-if="form.sections.existingBusinessEnabled"><el-row :gutter="16"><el-col :span="8"><el-form-item label="本次立项项目是否由存量项目带动"><el-select v-model="form.sections.existingProjectDriven" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item></el-col><template v-if="form.sections.existingProjectDriven === '是'"><el-col :span="8"><el-form-item label="存量商机编号"><el-input v-model="form.sections.existingMerchantNo" /></el-form-item></el-col><el-col :span="8"><el-form-item label="项目名称"><el-input v-model="form.sections.existingProjectName" /></el-form-item></el-col></template></el-row><el-form-item label="存量项目运营补充说明"><el-input v-model="form.sections.existingProjectOperation" type="textarea" :rows="2" /></el-form-item><el-form-item label="存量CT业务情况"><el-input v-model="form.sections.existingCtSummary" type="textarea" :rows="2" /></el-form-item>
            <div class="table-head"><span>存量业务明细</span></div>
            <el-form-item label="业务提供方（可多选）"><el-checkbox-group v-model="form.sections.existingProviders"><el-checkbox v-for="p in existingProviderOptions" :key="p" :value="p">{{ p }}</el-checkbox></el-checkbox-group></el-form-item>
            <div v-for="provider in form.sections.existingProviders" :key="provider" style="margin-bottom:16px"><div class="table-head"><span>{{ provider }}</span><el-button type="primary" link @click="addSectionRow('existingBusinessRows',{provider,businessName:'',quantity:'',unit:'',monthlyFee:'',annualRevenue:'',remark:''})">新增明细</el-button></div><el-table :data="existingRowsFor(provider)" border><el-table-column label="业务名称"><template #default="{row}"><el-input v-model="row.businessName" /></template></el-table-column><el-table-column label="数量" width="100"><template #default="{row}"><el-input v-model="row.quantity" /></template></el-table-column><el-table-column label="单位" width="100"><template #default="{row}"><el-input v-model="row.unit" /></template></el-table-column><el-table-column label="月资费（元）" width="130"><template #default="{row}"><el-input v-model="row.monthlyFee" /></template></el-table-column><el-table-column label="年收入（元）" width="130"><template #default="{row}"><el-input v-model="row.annualRevenue" /></template></el-table-column><el-table-column label="备注"><template #default="{row}"><el-input v-model="row.remark" /></template></el-table-column><el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="danger" @click="removeSectionRow('existingBusinessRows',existingRowIndex(row))">删除</el-button></template></el-table-column></el-table></div></template>

            <h3 style="margin-top:24px">附录4：合作伙伴情况</h3><el-switch v-model="form.sections.partnerAppendixEnabled" active-text="需要本附录" inactive-text="不需要" /><template v-if="form.sections.partnerAppendixEnabled"><div class="table-head"><span>合作伙伴甄选结果</span><el-button type="primary" link @click="addSectionRow('partnerSelectionRows',{packageNo:String(form.sections.partnerSelectionRows.length+1)})">新增包</el-button></div><el-table :data="form.sections.partnerSelectionRows" border><el-table-column label="包号" width="100"><template #default="{row}"><el-input v-model="row.packageNo" /></template></el-table-column><el-table-column label="中选人"><template #default="{row}"><el-input v-model="row.winner" /></template></el-table-column><el-table-column label="不含税中选金额（万元）"><template #default="{row}"><el-input v-model="row.amountExTax" /></template></el-table-column><el-table-column label="操作" width="80"><template #default="{$index}"><el-button link type="danger" @click="removeSectionRow('partnerSelectionRows',$index)">删除</el-button></template></el-table-column></el-table><div class="table-total">合计：{{ partnerSelectionTotal }} 万元</div><h4>供应链金融合作</h4><div class="form-tip">生成时按模板输出“若使用供应链金融”或“若未使用供应链金融”对应内容。</div><el-row :gutter="16" style="margin-top:16px"><el-col :span="8"><el-form-item label="是否使用供应链金融"><el-select v-model="form.sections.supplyChainFinanceUsed" clearable><el-option label="是" value="是" /><el-option label="否" value="否" /></el-select></el-form-item></el-col><template v-if="form.sections.supplyChainFinanceUsed === '是'"><el-col :span="8"><el-form-item label="合作银行"><el-input v-model="form.sections.supplyChainFinanceBank" /></el-form-item></el-col><el-col :span="8"><el-form-item label="合作金额（万元）"><el-input v-model="form.sections.supplyChainFinanceAmount" /></el-form-item></el-col></template><el-col v-if="form.sections.supplyChainFinanceUsed === '否'" :span="16"><el-form-item label="未使用原因（自行填写）"><el-input v-model="form.sections.supplyChainFinanceReason" placeholder="填写未与银行合作供应链金融业务的具体原因" /></el-form-item></el-col><el-col :span="24"><el-form-item label="其他补充说明（自行填写）"><el-input v-model="form.sections.supplyChainFinanceDescription" /></el-form-item></el-col></el-row></template>

            <h3 style="margin-top:24px">附录5：审价议价执行情况</h3><el-switch v-model="form.sections.appraisalEnabled" active-text="需要本附录" inactive-text="不需要" /><template v-if="form.sections.appraisalEnabled"><div class="table-head" style="margin-top:12px"><span>审核明细</span><el-button type="primary" link @click="addAppraisalRow">新增费用</el-button></div><el-table :data="form.sections.appraisalRows" border><el-table-column label="序号" width="75"><template #default="{row}"><el-input v-model="row.no" /></template></el-table-column><el-table-column label="内容" min-width="180"><template #default="{row}"><el-input v-model="row.content" /></template></el-table-column><el-table-column label="生态报价（元）"><template #default="{row}"><el-input v-model="row.ecosystemQuote" /></template></el-table-column><el-table-column label="可压降金额"><template #default="{row}"><el-input v-model="row.reducibleAmount" /></template></el-table-column><el-table-column label="可压降比例"><template #default="{row}"><el-input v-model="row.reducibleRatio" /></template></el-table-column><el-table-column label="备注"><template #default="{row}"><el-input v-model="row.remark" /></template></el-table-column><el-table-column label="操作" width="80"><template #default="{$index}"><el-button link type="danger" @click="removeAppraisalRow($index)">删除</el-button></template></el-table-column></el-table><el-form-item label="执行情况（下面情况二选一）"><el-radio-group v-model="form.sections.appraisalExecuted"><el-radio value="是">按照审核结果与生态议价</el-radio><el-radio value="否">未按照审核结果与生态议价</el-radio></el-radio-group></el-form-item><el-row v-if="form.sections.appraisalExecuted === '是'" :gutter="16"><el-col :span="12"><el-form-item label="相对生态报价压降金额（元）"><el-input v-model="form.sections.appraisalReductionAmount" /></el-form-item></el-col><el-col :span="12"><el-form-item label="压降比例（%）"><el-input v-model="form.sections.appraisalReductionRatio" /></el-form-item></el-col></el-row><el-form-item v-if="form.sections.appraisalExecuted === '否'" label="未议价原因"><el-input v-model="form.sections.appraisalReason" /></el-form-item><el-form-item label="硬件设备及相关服务费评估表"><input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.zip" @change="uploadEvidence('APPRAISAL_SUPPORT_FILE',$event)" /><div v-for="a in attachments.filter(x => x.attachmentType === 'APPRAISAL_SUPPORT_FILE')" :key="a.id"><el-button link type="primary" @click="download(a)">{{ a.originalFilename }}</el-button><el-button link type="danger" @click="removeAttachment(a)">删除</el-button></div></el-form-item></template>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card id="attachment-checklist" shadow="never" style="margin-top: 16px">
      <template #header><div class="table-head"><span>模板材料清单与上传入口</span><span>本项目涉及 {{ requiredUploadItems.length }} 类，未上传 {{ missingUploadItems.length }} 类</span></div></template>
      <el-alert :title="uploadReminderTitle" :type="missingUploadItems.length ? 'warning' : 'success'" :closable="false" show-icon />
      <p class="form-tip">按原始31页模板列出，导出删除可选页后页码可能变化。自动勾选项跟随上方项目设置；其他按需材料请勾选“涉及”，完成填报或生成PPT时会提醒缺失项。表格中已填写并由系统生成的测算数据无需重复上传。证明文件在项目中保存，上传成功不代表已审核或所有格式都会嵌入PPT。</p>
      <el-table :data="initiationAttachmentCatalog" border max-height="660">
        <el-table-column prop="page" label="模板页码" width="95" />
        <el-table-column prop="label" label="材料名称" min-width="225" />
        <el-table-column prop="hint" label="适用情况/说明" min-width="255" />
        <el-table-column label="本项目涉及" width="125"><template #default="{ row }">
          <el-checkbox :model-value="automaticUploadTypes.has(row.type) || form.sections.attachmentRequirementSelections?.includes(row.type)" :disabled="automaticUploadTypes.has(row.type)" @change="toggleAttachmentRequirement(row.type, $event)">{{ automaticUploadTypes.has(row.type) ? '自动' : '涉及' }}</el-checkbox>
        </template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }">
          <el-tag :type="uploadedAttachmentTypes.has(row.type) ? 'success' : requiredUploadItems.some(item => item.type === row.type) ? 'warning' : 'info'">{{ uploadedAttachmentTypes.has(row.type) ? '已上传' : requiredUploadItems.some(item => item.type === row.type) ? '未上传' : '按需提供' }}</el-tag>
        </template></el-table-column>
        <el-table-column label="上传/已上传文件" min-width="275"><template #default="{ row }">
          <input type="file" :disabled="uploadingEvidence" :accept="row.imageOnly ? '.png,.jpg,.jpeg' : '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.png,.jpg,.jpeg,.zip,.rar,.7z'" @change="uploadEvidence(row.type, $event)" />
          <div v-for="file in attachments.filter(item => item.attachmentType === row.type)" :key="file.id">
            <el-button link type="primary" @click="download(file)">{{ file.originalFilename }}</el-button>
            <el-button link type="danger" :loading="removingAttachment === file.id" @click="removeAttachment(file)">删除</el-button>
          </div>
        </template></el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <div class="table-head">
          <span>已上传文件</span>
          <span class="attachment-count">共 {{ attachments.length }} 个</span>
        </div>
      </template>
      <el-empty v-if="!attachments.length" description="暂无已上传文件" :image-size="72" />
      <el-table v-else :data="attachments" border>
        <el-table-column prop="originalFilename" label="文件名" min-width="280" />
        <el-table-column label="材料类型" min-width="200"><template #default="{ row }">{{ attachmentLabelMap[row.attachmentType] || row.attachmentType }}</template></el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="180" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="download(row)">下载</el-button>
            <el-button link type="danger" :loading="removingAttachment === row.id" @click="removeAttachment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.period-controls { display: flex; flex-wrap: wrap; gap: 16px 24px; margin-bottom: 16px; }
.period-controls label { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.period-controls :deep(.el-date-editor) { width: 300px; max-width: 100%; }
.finance-template-page { display: flex; flex-direction: column; padding: 18px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; background: var(--el-bg-color); }
.finance-template-page > h3:first-child { margin-top: 0 !important; }
.investment-followup-pages { order: 20; margin-top: 24px; }
.finance-revenue-page { margin-top: 24px; }
:global(.attachment-reminder-dialog .el-message-box__message) {
  white-space: pre-line;
  max-height: 55vh;
  overflow-y: auto;
}
.table-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  margin: 0 0 8px;
}
.attachment-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  font-weight: 400;
}
.table-total {
  padding: 10px 16px;
  text-align: right;
  font-weight: 600;
  border: 1px solid var(--el-border-color-lighter);
  border-top: 0;
}
.upload-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.form-tip {
  width: 100%;
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}
.fusion-table-scroll {
  width: 100%;
  overflow-x: auto;
}
.fusion-table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  table-layout: fixed;
}
.fusion-table th,
.fusion-table td {
  border: 1px solid var(--el-border-color);
  padding: 8px;
  text-align: center;
  vertical-align: middle;
}
.fusion-table th {
  color: #0070c0;
  font-weight: 700;
  background: var(--el-fill-color-light);
}
.fusion-table th:nth-child(1) { width: 90px; }
.fusion-table th:nth-child(2) { width: 190px; }
.fusion-table th:nth-child(3),
.fusion-table th:nth-child(4) { width: 120px; }
.fusion-table th:nth-child(5) { width: 130px; }
.fusion-name {
  font-weight: 700;
  color: var(--el-text-color-primary);
}
.revenue-mode-block { margin-top: 18px; }
.revenue-mode-block .table-head { display: flex; align-items: center; justify-content: space-between; }
.revenue-subtotal { padding: 10px 14px; text-align: right; font-weight: 700; background: var(--el-fill-color-light); border: 1px solid var(--el-border-color); border-top: 0; }
.economic-detail-panel { margin-top: 28px; }
.economic-detail-panel h3 { margin: 0; }
.economic-base-panel { margin-top: 28px; padding: 18px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; background: var(--el-fill-color-extra-light); }
.economic-base-panel .calculator-head { gap: 18px; align-items: flex-end; }
.economic-base-panel h3 { margin: 0; }
.economic-base-panel .calculator-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.economic-year-control { display: flex; gap: 10px; align-items: center; margin: 14px 0 12px; color: var(--el-text-color-regular); }
.economic-year-control small { color: var(--el-text-color-secondary); }
.base-year-input { width: 130px; }
.base-year-input :deep(.el-input__wrapper) { background: #fff200; box-shadow: 0 0 0 1px #d6b800 inset; }
.economic-summary-scroll { overflow-x: auto; }
.economic-summary-table { width: 100%; min-width: 760px; border-collapse: collapse; table-layout: fixed; }
.economic-summary-table th, .economic-summary-table td { border: 1px solid var(--el-border-color); padding: 14px 16px; text-align: center; }
.economic-summary-table th { background: var(--el-fill-color-light); color: var(--el-text-color-regular); }
.economic-summary-table td { background: #f8fafc; color: var(--el-color-primary); font-size: 18px; font-weight: 700; font-variant-numeric: tabular-nums; }
.economic-base-panel > .el-alert { margin-top: 12px; }
.benefit-mode-selector { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; width: 100%; }
.benefit-mode-selector .el-checkbox { margin: 0; height: auto; min-height: 42px; padding: 10px 14px; white-space: normal; }
.benefit-mode-selector small { color: var(--el-text-color-secondary); margin-left: 4px; }
</style>
