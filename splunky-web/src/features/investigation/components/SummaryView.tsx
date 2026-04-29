import { Alert, Button, Card, Empty, Space, Statistic, Tag, Typography } from 'antd'
import { FileText, Network, ShieldAlert } from 'lucide-react'
import {
  CartesianGrid,
  Line,
  LineChart,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult } from '../types'

type SummaryViewProps = {
  result: InvestigationResult
}

const baseTrend = [
  { time: '-25m', errors: 0 },
  { time: '-20m', errors: 1 },
  { time: '-15m', errors: 0 },
  { time: '-10m', errors: 1 },
  { time: '-5m', errors: 3 },
  { time: 'now', errors: 5 },
]

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

  const failedDownstream = result.downstreamCalls.find(
    (call) => call.status === 'TIMEOUT' || call.status === 'FAILED',
  )
  const trend =
    result.runNumber > 1
      ? baseTrend.map((point, index) => ({
          ...point,
          errors: point.errors + (index > 2 ? 2 : 0),
        }))
      : baseTrend

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
            prefix={<Network size={18} />}
            styles={{ content: { fontSize: 18 } }}
          />
          <div className="mt-2">
            <StatusTag status="TIMEOUT" />
          </div>
        </Card>
        <Card className="border-slate-200">
          <Statistic
            title="Confidence"
            value={result.summary.confidence}
            styles={{ content: { color: '#15803d', fontSize: 20 } }}
          />
          <div className="mt-2">
            <StatusTag status={result.summary.confidence} />
          </div>
        </Card>
        <Card className="border-slate-200">
          <Statistic
            title="Logs Found"
            value={result.summary.logsFound}
            prefix={<FileText size={18} />}
            styles={{ content: { color: '#2563eb', fontSize: 20 } }}
          />
          <div className="mt-2">
            <Tag color="blue">{result.summary.affectedServices.length} services</Tag>
          </div>
        </Card>
      </div>

      <Alert
        type="error"
        showIcon
        title="Diagnosis Summary"
        description={result.summary.rootCauseHypothesis}
      />

      <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <Card title="Evidence" className="border-slate-200">
          <div className="divide-y divide-slate-100">
            {result.summary.evidence.map((item, index) => (
              <div
                key={item.id}
                className="flex gap-3 py-3 first:pt-0 last:pb-0"
              >
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
                  <Typography.Paragraph className="mb-0 mt-1 text-slate-600">
                    {item.description}
                  </Typography.Paragraph>
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
        </Card>

        <Space orientation="vertical" size={16} className="w-full">
          <Card title="Recommended Actions" className="border-slate-200">
            <div className="space-y-2">
              {result.summary.recommendedActions.map((item, index) => (
                <div key={item} className="flex gap-2 text-sm text-slate-700">
                  <span className="font-semibold text-slate-500">{index + 1}.</span>
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </Card>
          <Card title="HUB Timeout Trend" className="border-slate-200">
            <div className="overflow-hidden">
              <LineChart
                width={320}
                height={176}
                data={trend}
                margin={{ top: 8, right: 8, bottom: 0, left: -20 }}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                <XAxis dataKey="time" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Line
                  type="monotone"
                  dataKey="errors"
                  stroke="#dc2626"
                  strokeWidth={2}
                  dot={{ r: 3 }}
                />
              </LineChart>
            </div>
          </Card>
        </Space>
      </div>

      <Card title="Affected Services" className="border-slate-200">
        <Space wrap>
          {result.summary.affectedServices.map((service) => (
            <Tag
              key={service}
              color={service === failedDownstream?.downstream ? 'red' : 'default'}
            >
              {service}
            </Tag>
          ))}
        </Space>
      </Card>
    </div>
  )
}
