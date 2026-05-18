import { computed } from 'vue'
import { useSpaceStore } from '@/stores/space'

export function usePermission(permission: string) {
  const spaceStore = useSpaceStore()

  return computed(() => {
    if (!permission) {
      return true
    }

    return spaceStore.permissions.includes(permission)
  })
}

