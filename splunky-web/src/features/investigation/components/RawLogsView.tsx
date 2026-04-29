import { Empty, Input, Select, Space, Switch, Table, Tag } from 'antd'
import type { TableColumnsType } from 'antd'
import { useMemo, useState } from 'react'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult, RawLogEntry } from '../types'
import { formatTimestamp } from '../utils/formatters'

type RawLogsViewProps = {
  result: InvestigationResult
}

export function RawLogsView({ result }: RawLogsViewProps) {
  const [service, setService] = useState<string>('All')
  const [level, setLevel] = useState<string>('All')
  const [eventType, setEventType] = useState<string>('All')
  const [keyword, setKeyword] = useState('')
  const [failedOnly, setFailedOnly] = useState(false)
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)

  const services = useMemo(
    () => ['All', ...Array.from(new Set(result.rawLogs.map((log) => log.service)))],
    [result.rawLogs],
  )
  const eventTypes = useMemo(
    () => [
      'All',
      ...Array.from(
        new Set(result.rawLogs.map((log) => log.eventType).filter(Boolean)),
      ),
    ],
    [result.rawLogs],
  )

  const data = useMemo(() => {
    const keywordLower = keyword.toLowerCase()

    return result.rawLogs.filter((log) => {
      const matchesService = service === 'All' || log.service === service
      const matchesLevel = level === 'All' || log.level === level
      const matchesEvent = eventType === 'All' || log.eventType === eventType
      const matchesFailure =
        !failedOnly ||
        log.level === 'ERROR' ||
        String(log.fields.status).toUpperCase().includes('TIMEOUT') ||
        Number(log.fields.status) >= 500
      const matchesKeyword =
        !keywordLower ||
        log.message.toLowerCase().includes(keywordLower) ||
        JSON.stringify(log.fields).toLowerCase().includes(keywordLower)

      return (
        matchesService &&
        matchesLevel &&
        matchesEvent &&
        matchesFailure &&
        matchesKeyword
      )
    })
  }, [eventType, failedOnly, keyword, level, result.rawLogs, service])

  const columns: TableColumnsType<RawLogEntry> = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      width: 130,
      render: (timestamp: string) => formatTimestamp(timestamp),
    },
    {
      title: 'Level',
      dataIndex: 'level',
      width: 90,
      render: (value: RawLogEntry['level']) => (
        <Tag color={value === 'ERROR' ? 'red' : value === 'WARN' ? 'gold' : 'blue'}>
          {value}
        </Tag>
      ),
    },
    {
      title: 'Service',
      dataIndex: 'service',
      width: 190,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: 'Event Type',
      dataIndex: 'eventType',
      width: 190,
    },
    {
      title: 'Message',
      dataIndex: 'message',
    },
  ]

  if (result.rawLogs.length === 0) {
    return <Empty description="No raw logs available." />
  }

  return (
    <div className="space-y-3">
      <div className="grid gap-3 xl:grid-cols-[180px_150px_220px_minmax(180px,1fr)_120px]">
        <Select
          value={service}
          onChange={setService}
          options={services.map((value) => ({ value, label: value }))}
        />
        <Select
          value={level}
          onChange={setLevel}
          options={['All', 'DEBUG', 'INFO', 'WARN', 'ERROR'].map((value) => ({
            value,
            label: value,
          }))}
        />
        <Select
          value={eventType}
          onChange={setEventType}
          options={eventTypes.map((value) => ({ value, label: value }))}
        />
        <Input.Search
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="Keyword"
          allowClear
        />
        <Space>
          <Switch checked={failedOnly} onChange={setFailedOnly} />
          <span>Failed</span>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        pagination={{ pageSize: 8, showSizeChanger: false }}
        scroll={{ x: 900 }}
        onRow={(record) => ({
          onClick: () => openDrawer({ type: 'RAW_LOG', id: record.id }),
        })}
        rowClassName={(record) =>
          record.level === 'ERROR' ? 'cursor-pointer bg-red-50' : 'cursor-pointer'
        }
      />
    </div>
  )
}
