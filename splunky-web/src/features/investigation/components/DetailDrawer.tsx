import {
  Button,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Space,
  Tag,
  Typography,
} from 'antd'
import { Copy, ExternalLink } from 'lucide-react'
import type { ReactNode } from 'react'
import { JsonViewer } from '../../../shared/components/JsonViewer'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type {
  DrawerSelection,
  EventStatus,
  Investigation,
  RawLogEntry,
} from '../types'
import { formatDateTime, formatDuration } from '../utils/formatters'

type DetailDrawerProps = {
  investigation: Investigation
}

type DetailView = {
  title: string
  status?: EventStatus | string
  timestamp?: string
  service?: string
  overview: Record<string, ReactNode>
  relatedLogIds: string[]
  rawData: unknown
  splunkUrl?: string
}

function logsByIds(logs: RawLogEntry[], ids: string[]) {
  const idSet = new Set(ids)
  return logs.filter((log) => idSet.has(log.id))
}

function findDetail(
  investigation: Investigation,
  selection: DrawerSelection | null,
): DetailView | null {
  if (!selection) {
    return null
  }

  const result = investigation.activeResult

  if (selection.type === 'TIMELINE_EVENT') {
    const event = result.timeline.find((item) => item.id === selection.id)
    if (!event) {
      return null
    }
    return {
      title: event.message,
      status: event.status,
      timestamp: event.timestamp,
      service: event.service,
      overview: {
        'Event Type': event.eventType,
        Duration: formatDuration(event.durationMs),
        'Downstream Call': event.downstreamCallId ?? '-',
      },
      relatedLogIds: event.relatedLogIds,
      rawData: event,
    }
  }

  if (selection.type === 'SERVICE_NODE') {
    const node = result.serviceGraph.nodes.find((item) => item.id === selection.id)
    if (!node) {
      return null
    }
    const relatedLogIds = result.rawLogs
      .filter((log) => log.service === node.serviceName)
      .map((log) => log.id)
    return {
      title: node.serviceName,
      status: node.status,
      service: node.serviceName,
      overview: {
        Platform: node.platform ?? '-',
        Logs: node.logCount,
        Errors: node.errorCount,
      },
      relatedLogIds,
      rawData: node,
    }
  }

  if (selection.type === 'SERVICE_EDGE') {
    const edge = result.serviceGraph.edges.find((item) => item.id === selection.id)
    if (!edge) {
      return null
    }
    return {
      title: `${edge.source} -> ${edge.target}`,
      status: edge.status,
      overview: {
        Operation: edge.operation ?? '-',
        Label: edge.label,
        Latency: formatDuration(edge.latencyMs),
        Evidence: edge.evidenceType,
      },
      relatedLogIds: edge.relatedLogIds,
      rawData: edge,
    }
  }

  if (selection.type === 'DOWNSTREAM_CALL') {
    const call = result.downstreamCalls.find((item) => item.id === selection.id)
    if (!call) {
      return null
    }
    return {
      title: `${call.caller} -> ${call.downstream}`,
      status: call.status,
      service: call.caller,
      overview: {
        Operation: call.operation,
        Endpoint: call.endpoint ?? '-',
        Latency: formatDuration(call.latencyMs),
        Error: call.errorMessage ?? '-',
      },
      relatedLogIds: call.relatedLogIds,
      rawData: call,
    }
  }

  if (selection.type === 'RAW_LOG') {
    const log = result.rawLogs.find((item) => item.id === selection.id)
    if (!log) {
      return null
    }
    return {
      title: log.message,
      status: log.level,
      timestamp: log.timestamp,
      service: log.service,
      overview: {
        Level: log.level,
        'Event Type': log.eventType ?? '-',
        'Splunk Link': log.splunkUrl ? 'Available' : '-',
      },
      relatedLogIds: [log.id],
      rawData: log,
      splunkUrl: log.splunkUrl,
    }
  }

  if (selection.type === 'SPL_QUERY') {
    const query = result.queries.find((item) => item.id === selection.id)
    if (!query) {
      return null
    }
    return {
      title: query.templateName,
      status: query.status,
      overview: {
        Reason: query.reason,
        'Result Count': query.resultCount,
        Execution: formatDuration(query.executionDurationMs),
        'Time Range': query.timeRange.label,
      },
      relatedLogIds: [],
      rawData: query,
      splunkUrl: query.splunkUrl,
    }
  }

  if (selection.type === 'EVIDENCE') {
    const evidence = result.summary.evidence.find((item) => item.id === selection.id)
    if (!evidence) {
      return null
    }
    return {
      title: evidence.title,
      timestamp: evidence.timestamp,
      service: evidence.service,
      overview: {
        Description: evidence.description,
        'Timeline Events': evidence.relatedTimelineEventIds.join(', '),
      },
      relatedLogIds: evidence.relatedLogIds,
      rawData: evidence,
    }
  }

  const message = result.sequence.messages.find((item) => item.id === selection.id)
  if (!message) {
    return null
  }

  return {
    title: message.label,
    status: message.status,
    timestamp: message.timestamp,
    overview: {
      From: message.from,
      To: message.to,
      Duration: formatDuration(message.durationMs),
      'Downstream Call': message.downstreamCallId ?? '-',
    },
    relatedLogIds: message.relatedLogIds,
    rawData: message,
  }
}

export function DetailDrawer({ investigation }: DetailDrawerProps) {
  const selection = useInvestigationUiStore((state) => state.drawerSelection)
  const closeDrawer = useInvestigationUiStore((state) => state.closeDrawer)
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)
  const detail = findDetail(investigation, selection)
  const relatedLogs = detail
    ? logsByIds(investigation.activeResult.rawLogs, detail.relatedLogIds)
    : []

  function copyJson() {
    if (detail) {
      void navigator.clipboard.writeText(JSON.stringify(detail.rawData, null, 2))
    }
  }

  function copyId() {
    if (selection) {
      void navigator.clipboard.writeText(selection.id)
    }
  }

  return (
    <Drawer
      size={560}
      open={Boolean(selection)}
      onClose={closeDrawer}
      title={
        detail ? (
          <Space orientation="vertical" size={4}>
            <Typography.Text strong>{detail.title}</Typography.Text>
            <Space wrap>
              {detail.status ? <StatusTag status={detail.status} /> : null}
              {detail.service ? <Tag>{detail.service}</Tag> : null}
              {detail.timestamp ? <Tag>{formatDateTime(detail.timestamp)}</Tag> : null}
            </Space>
          </Space>
        ) : (
          'Detail'
        )
      }
      extra={
        <Space>
          <Button icon={<Copy size={15} />} onClick={copyId}>
            Copy ID
          </Button>
          <Button icon={<Copy size={15} />} onClick={copyJson}>
            Copy JSON
          </Button>
        </Space>
      }
    >
      {detail ? (
        <div className="space-y-4">
          <section>
            <Typography.Title level={5}>Overview</Typography.Title>
            <Descriptions bordered size="small" column={1}>
              {Object.entries(detail.overview).map(([label, value]) => (
                <Descriptions.Item key={label} label={label}>
                  {value}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </section>

          <section>
            <Typography.Title level={5}>Related Logs</Typography.Title>
            {relatedLogs.length > 0 ? (
              <div className="divide-y divide-slate-100">
                {relatedLogs.map((log) => (
                  <div
                    key={log.id}
                    className="flex items-start justify-between gap-3 py-3 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <Typography.Text strong>
                        {formatDateTime(log.timestamp)} | {log.service}
                      </Typography.Text>
                      <Typography.Paragraph className="m-0 text-slate-600">
                        {log.message}
                      </Typography.Paragraph>
                    </div>
                    <Button
                      type="link"
                      onClick={() => openDrawer({ type: 'RAW_LOG', id: log.id })}
                    >
                      Inspect
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="No linked logs." />
            )}
          </section>

          <section>
            <Typography.Title level={5}>Raw Data</Typography.Title>
            <JsonViewer value={detail.rawData} />
          </section>

          <Divider />
          <Space>
            {detail.splunkUrl ? (
              <Button
                icon={<ExternalLink size={15} />}
                href={detail.splunkUrl}
                target="_blank"
              >
                Open in Splunk
              </Button>
            ) : null}
          </Space>
        </div>
      ) : (
        <Empty description="Detail not found." />
      )}
    </Drawer>
  )
}
