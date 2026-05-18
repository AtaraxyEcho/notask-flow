import type { RouteLocationNormalizedLoaded } from 'vue-router'
import type { Space } from '@/types/app'

export function getLandingPath(space?: Space | null) {
  if (!space) {
    return '/app/notes'
  }

  return space.type === 'TEAM' ? '/app/projects' : '/app/notes'
}

export function getSpaceAccent(space?: Space | null) {
  return space?.type === 'TEAM' ? 'var(--primary-container)' : '#ff8a65'
}

export function needsTeamRedirect(route: RouteLocationNormalizedLoaded, nextSpace?: Space | null) {
  return Boolean(route.meta.teamOnly) && nextSpace?.type !== 'TEAM'
}

