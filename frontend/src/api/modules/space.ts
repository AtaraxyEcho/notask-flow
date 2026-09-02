import { http } from '../http'
import type { Space, SpaceEventTicketResponse, SpaceInvite, SpaceInvitePreview, SpaceMember } from '@/types/app'

export const spaceService = {
  list() {
    return http.get<Space[]>('/spaces')
  },
  detail(spaceId: number) {
    return http.get<Space>(`/spaces/${spaceId}`)
  },
  create(payload: { name: string }) {
    return http.post<Space>('/spaces', payload)
  },
  update(spaceId: number, payload: { name: string }) {
    return http.put<Space>(`/spaces/${spaceId}`, payload)
  },
  delete(spaceId: number) {
    return http.delete<void>(`/spaces/${spaceId}`)
  },
  permissions(spaceId: number) {
    return http.get<string[]>(`/spaces/${spaceId}/permissions`)
  },
  createSpaceEventTicket(spaceId: number) {
    return http.post<SpaceEventTicketResponse>(`/spaces/${spaceId}/events-ticket`, undefined, {
      silentError: true,
    })
  },
  members(spaceId: number) {
    return http.get<SpaceMember[]>(`/spaces/${spaceId}/members`)
  },
  heartbeatMember(spaceId: number, clientId: string) {
    return http.post<void>(`/spaces/${spaceId}/members/heartbeat`, { clientId }, { silentError: true })
  },
  offlineMember(spaceId: number, clientId: string) {
    return http.post<void>(`/spaces/${spaceId}/members/offline`, { clientId }, { silentError: true })
  },
  sendOfflineMember(spaceId: number, tokenValue: string, clientId: string) {
    const baseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined) || ''
    return fetch(`${baseUrl}/spaces/${spaceId}/members/offline`, {
      method: 'POST',
      keepalive: true,
      headers: {
        'Content-Type': 'application/json',
        ...(tokenValue ? { Authorization: `Bearer ${tokenValue}` } : {}),
      },
      body: JSON.stringify({ clientId }),
    }).catch(() => undefined)
  },
  addMember(spaceId: number, payload: { userId: number; roleCode: string }) {
    return http.post<SpaceMember>(`/spaces/${spaceId}/members`, payload)
  },
  updateMemberRole(spaceId: number, userId: number, roleCode: string) {
    return http.put<SpaceMember>(`/spaces/${spaceId}/members/${userId}`, { roleCode })
  },
  removeMember(spaceId: number, userId: number) {
    return http.delete<void>(`/spaces/${spaceId}/members/${userId}`)
  },
  leave(spaceId: number) {
    return http.delete<void>(`/spaces/${spaceId}/members/me`)
  },
  createInvite(spaceId: number, payload: { roleCode: string; expireMinutes: number }) {
    return http.post<SpaceInvite>(`/spaces/${spaceId}/invites`, payload)
  },
  previewInvite(code: string) {
    return http.get<SpaceInvitePreview>(`/spaces/invites/${code}`)
  },
  joinInvite(code: string) {
    return http.post<SpaceMember>(`/spaces/invites/${code}/join`)
  },
}
