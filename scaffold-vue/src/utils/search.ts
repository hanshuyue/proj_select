export function includesKeyword(keyword: string | null | undefined, ...values: unknown[]) {
  const key = String(keyword ?? '').trim()
  if (!key) return true
  return values.some(value => String(value ?? '').includes(key))
}
