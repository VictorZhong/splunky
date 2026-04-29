import { Card, Empty, Table, Tag } from 'antd'
import type { TableColumnsType } from 'antd'
import { useEffect, useMemo, useState } from 'react'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult, SequenceMessage } from '../types'
import { formatDuration, formatTimestamp } from '../utils/formatters'

type SequenceViewProps = {
  result: InvestigationResult
}

type MermaidDiagramProps = {
  chart: string
  diagramId: string
}

function sanitizeMermaidLabel(value: string) {
  return value.replace(/:/g, '-').replace(/\n/g, ' ')
}

function MermaidDiagram({ chart, diagramId }: MermaidDiagramProps) {
  const [svg, setSvg] = useState('')

  useEffect(() => {
    let active = true

    void import('mermaid')
      .then(({ default: mermaid }) => {
        mermaid.initialize({
          startOnLoad: false,
          theme: 'base',
          securityLevel: 'strict',
          themeVariables: {
            primaryColor: '#f8fafc',
            primaryBorderColor: '#94a3b8',
            lineColor: '#334155',
            actorBorder: '#0f766e',
            actorBkg: '#ecfdf5',
            signalColor: '#334155',
            signalTextColor: '#0f172a',
          },
        })

        return mermaid.render(diagramId, chart)
      })
      .then(({ svg: renderedSvg }) => {
        if (active) {
          setSvg(renderedSvg)
        }
      })
      .catch(() => {
        if (active) {
          setSvg('')
        }
      })

    return () => {
      active = false
    }
  }, [chart, diagramId])

  return svg ? (
    <div
      className="mermaid overflow-auto"
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  ) : null
}

function buildSequenceDiagram(result: InvestigationResult) {
  const participantLines = result.sequence.participants.map(
    (participant) => `participant ${participant.id} as ${participant.label}`,
  )

  const messageLines = result.sequence.messages.map((message) => {
    const arrow = message.status === 'FAILED' || message.status === 'TIMEOUT' ? '--x' : '->>'
    return `${message.from}${arrow}${message.to}: ${sanitizeMermaidLabel(message.label)}`
  })

  return ['sequenceDiagram', ...participantLines, ...messageLines].join('\n')
}

export function SequenceView({ result }: SequenceViewProps) {
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)
  const chart = useMemo(() => buildSequenceDiagram(result), [result])
  const diagramId = `sequence-${result.investigationId}-${result.runId}`

  const columns: TableColumnsType<SequenceMessage> = [
    {
      title: 'Time',
      dataIndex: 'timestamp',
      width: 130,
      render: (timestamp: string) => formatTimestamp(timestamp),
    },
    {
      title: 'From',
      dataIndex: 'from',
      width: 140,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: 'To',
      dataIndex: 'to',
      width: 140,
      render: (value: string) => <Tag>{value}</Tag>,
    },
    {
      title: 'Message',
      dataIndex: 'label',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 120,
      render: (status: SequenceMessage['status']) => <StatusTag status={status} />,
    },
    {
      title: 'Duration',
      dataIndex: 'durationMs',
      width: 110,
      render: (durationMs?: number) => formatDuration(durationMs),
    },
  ]

  if (result.sequence.messages.length === 0) {
    return <Empty description="No sequence data available." />
  }

  return (
    <div className="space-y-4">
      <Card className="border-slate-200">
        <MermaidDiagram chart={chart} diagramId={diagramId} />
      </Card>
      <Table
        rowKey="id"
        size="middle"
        columns={columns}
        dataSource={result.sequence.messages}
        pagination={false}
        scroll={{ x: 760 }}
        onRow={(record) => ({
          onClick: () => openDrawer({ type: 'SEQUENCE_MESSAGE', id: record.id }),
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
