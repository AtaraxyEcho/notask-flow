import { http } from '../http'
import type {
  MemberTaskLoad,
  PersonalNoteTrend,
  PersonalStats,
  RoleCompletion,
  StatsActivity,
  TaskTrend,
} from '@/types/app'

export const statsService = {
  personal() {
    return http.get<PersonalStats>('/stats/personal')
  },
  personalNoteTrend(days = 7) {
    return http.get<PersonalNoteTrend[]>('/stats/personal/note-trend', { params: { days } })
  },
  load(spaceId: number) {
    return http.get<MemberTaskLoad[]>(`/spaces/${spaceId}/stats/load`)
  },
  trend(spaceId: number, days = 7) {
    return http.get<TaskTrend[]>(`/spaces/${spaceId}/stats/trend`, { params: { days } })
  },
  roleCompletion(spaceId: number) {
    return http.get<RoleCompletion[]>(`/spaces/${spaceId}/stats/role-completion`)
  },
  activities(spaceId: number, limit = 8) {
    return http.get<StatsActivity[]>(`/spaces/${spaceId}/stats/activities`, { params: { limit } })
  },
}
