import { Card, Divider, Space, Typography } from 'antd'
import type { Investigation } from '../types'
import { formatDateTime } from '../utils/formatters'
import { RunHistoryDropdown } from './RunHistoryDropdown'

type ResultContextBarProps = {
  investigation: Investigation
}

export function ResultContextBar({ investigation }: ResultContextBarProps) {
  const context = investigation.activeResult.context
  const timezone = context.timeRange.timezone

  return (
    <Card className="border-slate-200 shadow-sm" styles={{ body: { padding: 16 } }}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0 flex-1">
          <Space wrap separator={<Divider orientation="vertical" />}>
            <Typography.Text strong>
              Investigation {investigation.id}
            </Typography.Text>
            <span>Updated: {formatDateTime(context.lastRunAt)}</span>
            <span>
              {context.timeRange.label} | {timezone.label} {timezone.offset}
            </span>
            {context.correlationId ? (
              <span>Correlation ID: {context.correlationId}</span>
            ) : null}
          </Space>
          <Typography.Paragraph className="mb-0 mt-2 break-words text-sm text-slate-700">
            <Typography.Text className="mr-2 text-slate-500">
              Original input:
            </Typography.Text>
            {investigation.input.rawText}
          </Typography.Paragraph>
        </div>
        <div className="shrink-0">
          <RunHistoryDropdown
            runs={investigation.runs}
            activeRunId={investigation.activeRunId}
          />
        </div>
      </div>
    </Card>
  )
}
