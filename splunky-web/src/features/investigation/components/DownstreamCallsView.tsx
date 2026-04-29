import { Empty, Segmented, Table, Tag } from 'antd'
import type { TableColumnsType } from 'antd'
import { useMemo, useState } from 'react'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { DownstreamCall, InvestigationResult } from '../types'
import { formatDuration } from '../utils/formatters'

type DownstreamCallsViewProps = {
  result: InvestigationResult
}

type CallFilter = 'All' | 'Failed Only' | 'Timeout Only' | 'Group by Downstream'

export function DownstreamCallsView({ result }: DownstreamCallsViewProps) {
  const [filter, setFilter] = useState<CallFilter>('All')
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)

  const data = useMemo(() => {
    if (filter === 'Failed Only') {
      return result.downstreamCalls.filter((call) => call.status !== 'OK')
    }
    if (filter === 'Timeout Only') {
      return result.downstreamCalls.filter((call) => call.status === 'TIMEOUT')
    }
    return result.downstreamCalls
  }, [filter, result.downstreamCalls])

  const columns: TableColumnsType<DownstreamCall> = [
    {
      title: '#',
      width: 60,
      render: (_, __, index) => index + 1,
    },
    {
      title: 'Caller',
      dataIndex: 'caller',
      width: 160,
      render: (caller: string) => <Tag>{caller}</Tag>,
    },
    {
      title: 'Downstream',
      dataIndex: 'downstream',
      width: 230,
      render: (downstream: string, record) => (
        <Tag color={record.status === 'TIMEOUT' ? 'red' : 'default'}>
          {downstream}
        </Tag>
      ),
    },
    {
      title: 'Operation',
      dataIndex: 'operation',
      width: 210,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 120,
      render: (status: DownstreamCall['status']) => <StatusTag status={status} />,
    },
    {
      title: 'Latency',
      dataIndex: 'latencyMs',
      width: 100,
      render: (latencyMs?: number) => formatDuration(latencyMs),
      sorter: (a, b) => (a.latencyMs ?? 0) - (b.latencyMs ?? 0),
    },
    {
      title: 'Error',
      dataIndex: 'errorMessage',
      render: (error?: string) => error ?? '-',
    },
    {
      title: 'Evidence',
      dataIndex: 'relatedLogIds',
      width: 110,
      render: (relatedLogIds: string[]) => `${relatedLogIds.length} logs`,
    },
  ]

  if (result.downstreamCalls.length === 0) {
    return <Empty description="No downstream calls available." />
  }

  return (
    <div className="space-y-3">
      <Segmented<CallFilter>
        value={filter}
        onChange={setFilter}
        options={['All', 'Failed Only', 'Timeout Only', 'Group by Downstream']}
      />
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        pagination={false}
        scroll={{ x: 980 }}
        onRow={(record) => ({
          onClick: () => openDrawer({ type: 'DOWNSTREAM_CALL', id: record.id }),
        })}
        rowClassName={(record) =>
          record.status === 'TIMEOUT' || record.status === 'FAILED'
            ? 'cursor-pointer bg-red-50'
            : 'cursor-pointer'
        }
      />
    </div>
  )
}
