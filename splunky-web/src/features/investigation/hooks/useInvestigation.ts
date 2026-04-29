import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getInvestigation,
  startInvestigation,
} from '../api/investigationApi'
import type { Investigation, StartInvestigationRequest } from '../types'

export const investigationQueryKey = (id: string) => ['investigation', id] as const

export function useInvestigation(investigationId?: string) {
  return useQuery({
    queryKey: investigationQueryKey(investigationId ?? 'missing'),
    queryFn: () => getInvestigation(investigationId as string),
    enabled: Boolean(investigationId),
  })
}

export function useStartInvestigation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: StartInvestigationRequest) => startInvestigation(request),
    onSuccess: (investigation: Investigation) => {
      queryClient.setQueryData(investigationQueryKey(investigation.id), investigation)
    },
  })
}
