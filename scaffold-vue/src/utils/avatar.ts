import { AVATAR_TINTS } from '@/data'

export function avatarText(name?: string | null) {
  return String(name || '?').slice(-2)
}

export function avatarColor(name?: string | null, tint?: string) {
  if (tint) return tint
  const displayName = String(name || '?')
  return AVATAR_TINTS[displayName.length % AVATAR_TINTS.length]
}
