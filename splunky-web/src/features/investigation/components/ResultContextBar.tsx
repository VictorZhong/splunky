import { Card, Divider, Space, Tag, Typography } from 'antd'
import type { Investigation } from '../types'
import { formatDateTime } from '../utils/formatters'
import { RunHistoryDropdown } from './RunHistoryDropdown'

type ResultContextBarProps = {
  investigation: Investigation
}

export function ResultContextBar({ investigation }: ResultContextBarProps) {
  const context = investigation.activeResult.context

  return (
    <Card className="border-slate-200 shadow-sm" styles={{ body: { padding: 16 } }}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Space wrap separator={<Divider orientation="vertical" />}>
          <Typography.Text strong>
            Investigation {investigation.id}
          </Typography.Text>
          <span>Input: {investigation.input.detectedTypes.join(', ')}</span>
          <span>
            {context.timeRange.label} | {context.timeRange.timezone.label}{' '}
            {context.timeRange.timezone.offset}
          </span>
          {context.correlationId ? (
            <span>Correlation ID: {context.correlationId}</span>
          ) : null}
          {context.apiName ? <span>API: {context.apiName}</span> : null}
          <Tag>Test environment</Tag>
          <span>Updated: {formatDateTime(context.lastRunAt)}</span>
        </Space>
        <RunHistoryDropdown
          runs={investigation.runs}
          activeRunId={investigation.activeRunId}
        />
      </div>
    </Card>
  )
}
