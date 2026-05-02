import { Button, Card, Empty, Space, Statistic, Tag, Typography } from 'antd'
import { Bot, FileText, ShieldAlert, Waypoints } from 'lucide-react'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult } from '../types'

type SummaryViewProps = {
  result: InvestigationResult
}

export function SummaryView({ result }: SummaryViewProps) {
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)

  if (result.summary.status === 'NO_RESULT') {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="No matching logs found for the selected input and time range."
      />
    )
  }

  const relatedServices = Array.from(
    new Set(
      [...result.summary.affectedServices, ...result.rawLogs.map((log) => log.service)].filter(
        Boolean,
      ),
    ),
  )

  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <Card className="border-slate-200">
          <Statistic
            title="Final Status"
            value={result.summary.status}
            prefix={<ShieldAlert size={18} />}
            styles={{ content: { color: '#dc2626', fontSize: 20 } }}
          />
          <div className="mt-2">
            <StatusTag status={result.summary.status} />
          </div>
        </Card>
        <Card className="border-slate-200">
          <Statistic
            title="Failure Point"
            value={result.summary.failurePoint ?? 'Unknown'}
            prefix={<Waypoints size={18} />}
            styles={{ content: { fontSize: 18 } }}
          />
        </Card>
        <Card className="border-slate-200">
          <Statistic
            title="Confidence"
            value={result.summary.confidence}
            styles={{ content: { color: '#0f766e', fontSize: 20 } }}
          />
          <div className="mt-2">
            <StatusTag status={result.summary.confidence} />
          </div>
        </Card>
        <Card className="border-slate-200">
          <Statistic
            title="Logs Analyzed"
            value={result.summary.logsFound}
            prefix={<FileText size={18} />}
            styles={{ content: { color: '#2563eb', fontSize: 20 } }}
          />
          <div className="mt-2">
            <Tag color="blue">{relatedServices.length} services</Tag>
          </div>
        </Card>
      </div>

      <Card className="overflow-hidden border-0 bg-[linear-gradient(135deg,#0f172a_0%,#164e63_52%,#f0fdfa_140%)] shadow-lg shadow-slate-300/30">
        <div className="space-y-4">
          <Space wrap size={[8, 8]}>
            <Tag color="gold">AI Summary</Tag>
            <Tag color="cyan">Summary-only MVP</Tag>
          </Space>
          <div className="max-w-4xl">
            <Typography.Title level={3} className="!mb-3 !text-white">
              {result.summary.rootCauseHypothesis ?? 'No conclusive root-cause hypothesis yet.'}
            </Typography.Title>
            <Typography.Paragraph className="!mb-0 text-sm leading-7 !text-slate-200">
              Splunky queried Splunk, previewed {result.rawLogs.length} log rows, and rendered
              this summary from the returned evidence. Use the evidence, raw logs, and executed
              SPL sections below to validate or refine the diagnosis.
            </Typography.Paragraph>
          </div>
          <Space wrap size={[8, 8]}>
            {relatedServices.map((service) => (
              <Tag key={service} className="rounded-full border-0 bg-white/12 px-3 py-1 text-white">
                {service}
              </Tag>
            ))}
          </Space>
        </div>
      </Card>

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1.1fr)_minmax(280px,0.9fr)]">
        <Card title="Evidence from Splunk logs" className="border-slate-200">
          {result.summary.evidence.length === 0 ? (
            <Empty description="No structured evidence items were returned." />
          ) : (
            <div className="divide-y divide-slate-100">
              {result.summary.evidence.map((item, index) => (
                <div key={item.id} className="flex gap-3 py-4 first:pt-0 last:pb-0">
                  <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-100 text-sm font-semibold text-slate-600">
                    {index + 1}
                  </span>
                  <div className="min-w-0 flex-1">
                    <button
                      type="button"
                      className="border-0 bg-transparent p-0 text-left font-medium text-slate-900"
                      onClick={() => openDrawer({ type: 'EVIDENCE', id: item.id })}
                    >
                      {item.title}
                    </button>
                    <Typography.Paragraph className="mb-0 mt-1 text-sm leading-6 text-slate-600">
                      {item.description}
                    </Typography.Paragraph>
                    <Space wrap size={[6, 6]} className="mt-3">
                      {item.service ? <Tag>{item.service}</Tag> : null}
                      {item.timestamp ? <Tag color="default">{item.timestamp}</Tag> : null}
                      <Tag color="blue">{item.relatedLogIds.length} linked logs</Tag>
                    </Space>
                  </div>
                  <Button
                    type="link"
                    onClick={() => openDrawer({ type: 'EVIDENCE', id: item.id })}
                  >
                    Details
                  </Button>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Space direction="vertical" size={16} className="w-full">
          <Card title="Recommended next actions" className="border-slate-200">
            {result.summary.recommendedActions.length === 0 ? (
              <Typography.Text className="text-slate-500">
                No recommended actions were returned.
              </Typography.Text>
            ) : (
              <div className="space-y-3">
                {result.summary.recommendedActions.map((item, index) => (
                  <div key={item} className="flex gap-3 text-sm leading-6 text-slate-700">
                    <span className="font-semibold text-teal-700">{index + 1}.</span>
                    <span>{item}</span>
                  </div>
                ))}
              </div>
            )}
          </Card>

          <Card title="Analysis scope" className="border-slate-200">
            <div className="space-y-3 text-sm text-slate-600">
              <div className="flex items-start gap-3">
                <Bot size={16} className="mt-1 text-teal-700" />
                <div>
                  <Typography.Text strong className="block text-slate-900">
                    AI input
                  </Typography.Text>
                  <span>
                    {result.rawLogs.length} preview rows from {relatedServices.length} services,
                    plus {result.queries.length} executed Splunk query.
                  </span>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                {relatedServices.length > 0 ? (
                  relatedServices.map((service) => (
                    <Tag key={service} color="cyan">
                      {service}
                    </Tag>
                  ))
                ) : (
                  <Typography.Text className="text-slate-500">
                    No service names were extracted.
                  </Typography.Text>
                )}
              </div>
            </div>
          </Card>
        </Space>
      </div>
    </div>
  )
}
