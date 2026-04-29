import { useMutation, useQueryClient } from '@tanstack/react-query'
import { submitFollowUp } from '../api/investigationApi'
import { investigationQueryKey } from './useInvestigation'
import type { FollowUpRequest, FollowUpResponse } from '../types'

export function useFollowUp(investigationId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: FollowUpRequest) =>
      submitFollowUp(investigationId, request),
    onSuccess: (response: FollowUpResponse) => {
      queryClient.setQueryData(
        investigationQueryKey(response.investigation.id),
        response.investigation,
      )
    },
  })
}
