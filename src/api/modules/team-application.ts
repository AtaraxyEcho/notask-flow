import { http } from '../http'
import type { SpaceJoinRequest } from '@/types/app'

export const teamApplicationService = {
  apply(payload: { supervisorAccount: string; teamName?: string; remark?: string }) {
    return http.post<SpaceJoinRequest>('/team-applications', payload)
  },
  mine() {
    return http.get<SpaceJoinRequest[]>('/team-applications/mine')
  },
  pending() {
    return http.get<SpaceJoinRequest[]>('/team-applications/pending')
  },
  approve(requestId: number, payload: { spaceId: number; roleCode: string }) {
    return http.post<SpaceJoinRequest>(`/team-applications/${requestId}/approve`, payload)
  },
  reject(requestId: number, payload: { reason?: string }) {
    return http.post<SpaceJoinRequest>(`/team-applications/${requestId}/reject`, payload)
  },
}
