import { Tag } from 'antd'
import type {
  Confidence,
  EventStatus,
  InvestigationStatus,
} from '../../features/investigation/types'

type StatusTagProps = {
  status: EventStatus | InvestigationStatus | Confidence | string
}

const statusColor: Record<string, string> = {
  OK: 'green',
  SUCCESS: 'green',
  FAILED: 'red',
  TIMEOUT: 'volcano',
  WARNING: 'gold',
  INFERRED: 'blue',
  PARTIAL: 'gold',
  NO_RESULT: 'default',
  HIGH: 'green',
  MEDIUM: 'gold',
  LOW: 'orange',
  UNKNOWN: 'default',
}

export function StatusTag({ status }: StatusTagProps) {
  return (
    <Tag color={statusColor[status] ?? 'default'} className="m-0 font-medium">
      {status.replaceAll('_', ' ')}
    </Tag>
  )
}
