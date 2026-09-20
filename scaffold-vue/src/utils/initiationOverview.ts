export function yearOnYear(previous: unknown, current: unknown): string {
  const number = (value: unknown) => {
    const text = String(value ?? '').trim().replace(/[,，%％]/g, '')
    return text && Number.isFinite(Number(text)) ? Number(text) : null
  }
  const before = number(previous), after = number(current)
  if (before == null || after == null) return ''
  if (before === 0) return '—'
  return ((after - before) / before * 100).toFixed(2)
}

export function parseMonthRange(value: string): string[] | null {
  const match = String(value || '').match(/^(\d{4})年(\d{1,2})[月]?[-至～](?:(\d{4})年)?(\d{1,2})月$/)
  if (!match) return null
  return [`${match[1]}-${match[2].padStart(2, '0')}`, `${match[3] || match[1]}-${match[4].padStart(2, '0')}`]
}

export function formatMonthRange(value: string[] | null): string {
  if (!value || value.length !== 2) return ''
  const [first, last] = value.map(item => item.split('-'))
  return first[0] === last[0]
    ? `${first[0]}年${Number(first[1])}-${Number(last[1])}月`
    : `${first[0]}年${Number(first[1])}月至${last[0]}年${Number(last[1])}月`
}

export const capabilityDemandOptions = [
  { key: 'IT', label: 'IT平台和设备', placeholder: '填写平台及设备名称、融入的自有能力（如9One）' },
  { key: 'CLOUD', label: '公有云', placeholder: '例如：开通5台云主机，用于业务系统部署' },
  { key: 'NETWORK', label: '数据专线', placeholder: '例如：开通2条100M数据专线，用于连接客户机房' },
]

export function buildCapabilityDemand(selected: string[], details: Record<string, string>, extra = ''): string {
  return [...capabilityDemandOptions.filter(item => selected.includes(item.key))
    .map(item => details[item.key]?.trim() ? `${item.label}：${details[item.key].trim()}` : ''), extra.trim()]
    .filter(Boolean).join('\n')
}
