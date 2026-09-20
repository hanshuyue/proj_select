import type { CurrentUser, LoginResponse } from './auth'

const TOKEN_KEY = 'yunyuan.token'
const USER_KEY = 'yunyuan.user'
const EXPIRES_KEY = 'yunyuan.expiresAt'

export interface AuthSessionOptions {
  localStorage?: Storage
  sessionStorage?: Storage
}

export interface StoredUser {
  id?: number
  username: string
  nickname: string
  deptName?: string
  mustChangePassword?: boolean
  roles?: string[]
  permissions?: string[]
}

export function createAuthSession(options: AuthSessionOptions = {}) {
  const local = options.localStorage ?? safeStorage('localStorage')
  const session = options.sessionStorage ?? safeStorage('sessionStorage')

  function activeStorage() {
    return local.getItem(TOKEN_KEY) ? local : session
  }

  function save(login: LoginResponse, user: StoredUser | CurrentUser, remember: boolean) {
    clear()
    const storage = remember ? local : session
    storage.setItem(TOKEN_KEY, login.token)
    storage.setItem(USER_KEY, JSON.stringify(user))
    storage.setItem(EXPIRES_KEY, String(Date.now() + login.expiresIn * 1000))
  }

  function token() {
    const storage = activeStorage()
    const expiresAt = Number(storage.getItem(EXPIRES_KEY) || 0)
    if (expiresAt && expiresAt < Date.now()) {
      clear()
      return null
    }
    return storage.getItem(TOKEN_KEY)
  }

  function user(): StoredUser | null {
    const raw = activeStorage().getItem(USER_KEY)
    return raw ? JSON.parse(raw) as StoredUser : null
  }

  function updateUser(user: StoredUser | CurrentUser) {
    activeStorage().setItem(USER_KEY, JSON.stringify(user))
  }

  function isAuthenticated() {
    return Boolean(token())
  }

  function clear() {
    ;[local, session].forEach(storage => {
      storage.removeItem(TOKEN_KEY)
      storage.removeItem(USER_KEY)
      storage.removeItem(EXPIRES_KEY)
      storage.removeItem('yunyuan.authed')
    })
  }

  return { save, updateUser, token, user, isAuthenticated, clear }
}

function safeStorage(name: 'localStorage' | 'sessionStorage'): Storage {
  if (typeof window !== 'undefined' && window[name]) return window[name]
  const data = new Map<string, string>()
  return {
    get length() { return data.size },
    clear: () => data.clear(),
    getItem: (key: string) => data.get(key) ?? null,
    key: (index: number) => Array.from(data.keys())[index] ?? null,
    removeItem: (key: string) => data.delete(key),
    setItem: (key: string, value: string) => data.set(key, value),
  }
}

export const authSession = createAuthSession()
