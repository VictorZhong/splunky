import { Button, Empty, Table, Tag } from 'antd'
import type { TableColumnsType } from 'antd'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult, TimelineEvent } from '../types'
import { formatDuration, formatTimestamp } from '../utils/formatters'

type TimelineViewProps = {
  result: InvestigationResult
}

export function TimelineView({ result }: TimelineViewProps) {
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)

  const columns: TableColumnsType<TimelineEvent> = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      width: 130,
      render: (timestamp: string) => formatTimestamp(timestamp),
    },
    {
      title: 'Service',
      dataIndex: 'service',
      width: 190,
      render: (service: string) => <Tag>{service}</Tag>,
    },
    {
      title: 'Event Type',
      dataIndex: 'eventType',
      width: 180,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 120,
      render: (status: TimelineEvent['status']) => <StatusTag status={status} />,
    },
    {
      title: 'Duration',
      dataIndex: 'durationMs',
      width: 110,
      render: (durationMs?: number) => formatDuration(durationMs),
    },
    {
      title: 'Message',
      dataIndex: 'message',
    },
    {
      title: 'Action',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button
          type="link"
          onClick={() => openDrawer({ type: 'TIMELINE_EVENT', id: record.id })}
        >
          Inspect
        </Button>
      ),
    },
  ]

  if (result.timeline.length === 0) {
    return <Empty description="No timeline events available." />
  }

  return (
    <Table
      rowKey="id"
      size="middle"
      columns={columns}
      dataSource={result.timeline}
      pagination={false}
      scroll={{ x: 940 }}
      onRow={(record) => ({
        onClick: () => openDrawer({ type: 'TIMELINE_EVENT', id: record.id }),
      })}
      rowClassName={(record) =>
        record.status === 'FAILED' || record.status === 'TIMEOUT'
          ? 'cursor-pointer bg-red-50'
          : 'cursor-pointer'
      }
    />
  )
}
