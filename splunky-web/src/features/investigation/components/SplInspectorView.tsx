import { Button, Card, Descriptions, Empty, Space, Tag, Typography } from 'antd'
import { ExternalLink } from 'lucide-react'
import { CodeViewer } from '../../../shared/components/CodeViewer'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult, SplQueryRecord } from '../types'
import { formatDuration } from '../utils/formatters'

type SplInspectorViewProps = {
  result: InvestigationResult
}

function QueryCard({ query }: { query: SplQueryRecord }) {
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)

  return (
    <Card
      className="border-slate-200"
      title={
        <Space wrap>
          <Typography.Text strong>{query.templateName}</Typography.Text>
          <StatusTag status={query.status} />
        </Space>
      }
      extra={
        <Space>
          <Button type="link" onClick={() => openDrawer({ type: 'SPL_QUERY', id: query.id })}>
            Details
          </Button>
          <Button icon={<ExternalLink size={15} />} href={query.splunkUrl} target="_blank">
            Open in Splunk
          </Button>
        </Space>
      }
    >
      <Descriptions size="small" column={{ xs: 1, md: 2, xl: 4 }} className="mb-3">
        <Descriptions.Item label="Query ID">{query.id}</Descriptions.Item>
        <Descriptions.Item label="Result Count">
          <Tag color="blue">{query.resultCount}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Execution">
          {formatDuration(query.executionDurationMs)}
        </Descriptions.Item>
        <Descriptions.Item label="Time Range">
          {query.timeRange.label}
        </Descriptions.Item>
      </Descriptions>
      <Typography.Paragraph className="text-slate-600">
        {query.reason}
      </Typography.Paragraph>
      <CodeViewer language="spl" value={query.spl} minHeight={120} />
    </Card>
  )
}

export function SplInspectorView({ result }: SplInspectorViewProps) {
  if (result.queries.length === 0) {
    return <Empty description="No SPL queries available." />
  }

  return (
    <div className="space-y-4">
      {result.queries.map((query) => (
        <QueryCard key={query.id} query={query} />
      ))}
    </div>
  )
}
