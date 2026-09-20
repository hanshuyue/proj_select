import assert from 'node:assert/strict'
import test from 'node:test'
import { calculatedBenefitAnalysisValue } from '../benefitAnalysis.ts'

const sources = {
  overall: {
    netProfitRate: ['', '8.00%'],
    staticPayback: ['', '2.00'],
  },
  investment: {
    netCashInflow: ['', '14.00%'],
    discountedCumulativeNetInflow: ['', '5.00%'],
    dynamicPayback: ['', '3.00'],
  },
  cooperation: {
    netProfitRate: ['', '4.88%'],
  },
}

test('benefit analysis rows use their corresponding economic tables', () => {
  assert.equal(calculatedBenefitAnalysisValue({ mode: '1.投资及合作服务模式部分', metric: '净利润率（%）' }, sources, 1), '8.00%')
  assert.equal(calculatedBenefitAnalysisValue({ no: '1.1', mode: '投资模式部分', metric: '内部收益率（IRR）' }, sources, 1), '14.00%')
  assert.equal(calculatedBenefitAnalysisValue({ no: '1.1', mode: '投资模式部分', metric: '动态回收期（年）' }, sources, 1), '3.00年')
  assert.equal(calculatedBenefitAnalysisValue({ no: '1.2', mode: '合作部分IT净利润率', metric: '净利润率（%）' }, sources, 1), '4.88%')
})
