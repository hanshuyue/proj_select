import { httpClient } from './http.ts'
import { createAuthApi } from './auth.ts'
import { createSystemApi } from './system.ts'
import { createPortalApi } from './portal.ts'
import { createSelectionApi } from './selection.ts'
import { createInitiationApi } from './initiation.ts'
import { createCollaborationApi } from './collaboration.ts'

export * from './http.ts'
export * from './auth.ts'
export * from './session.ts'
export * from './system.ts'
export * from './portal.ts'
export * from './selection.ts'
export * from './initiation.ts'
export * from './collaboration.ts'

export const authApi = createAuthApi(httpClient)
export const systemApi = createSystemApi(httpClient)
export const portalApi = createPortalApi(httpClient)
export const selectionApi = createSelectionApi(httpClient)
export const initiationApi = createInitiationApi(httpClient)
export const collaborationApi = createCollaborationApi(httpClient)
