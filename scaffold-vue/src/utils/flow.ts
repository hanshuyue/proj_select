const NODE_INDEX: Record<string, number> = {
  发起: 0,
  提交申请: 0,
  部门审批: 1,
  部门负责人审批: 1,
  财务审核: 2,
  财务复核: 2,
  总经理: 3,
  归档: 3,
}

export function normalizeFlowStep(current: number | string | null | undefined, total: number | string | null | undefined) {
  const max = normalizeNumber(total, 0)
  const numeric = normalizeNumber(current, Number.NaN)
  if (Number.isFinite(numeric)) {
    return clamp(numeric, 0, max)
  }
  const name = String(current ?? '').trim()
  return clamp(NODE_INDEX[name] ?? 0, 0, max)
}

function normalizeNumber(value: number | string | null | undefined, fallback: number) {
  const numberValue = typeof value === 'number' ? value : Number(value)
  return Number.isFinite(numberValue) ? Math.trunc(numberValue) : fallback
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), Math.max(max, min))
}
