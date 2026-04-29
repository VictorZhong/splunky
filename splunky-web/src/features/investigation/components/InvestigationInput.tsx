import {
  Alert,
  Button,
  Card,
  DatePicker,
  Input,
  Progress,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd'
import type { Dayjs } from 'dayjs'
import { Search, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useStartInvestigation } from '../hooks/useInvestigation'
import type { InvestigationInputType, TimezoneOption } from '../types'
import {
  buildTimeRange,
  defaultTimezone,
  detectInputTypes,
  timezoneOptions,
} from '../utils/inputDetection'

const { TextArea } = Input
const { RangePicker } = DatePicker

const loadingStages = [
  'Understanding input',
  'Selecting query templates',
  'Querying Splunk logs',
  'Building investigation timeline',
  'Analyzing evidence',
]

const examples = [
  'Find logs for correlation ID abc-123',
  'Why did this payment propose fail?',
  'Check 5xx spike for payment-sapi in last 30 minutes',
]

const inputTypeLabels = {
  CORRELATION_ID: 'Correlation ID',
  ERROR_RESPONSE: 'Error Response',
  API_NAME_OR_FIELD: 'API Name',
  NATURAL_LANGUAGE: 'Natural Lang',
}

export function InvestigationInput() {
  const navigate = useNavigate()
  const [rawText, setRawText] = useState('correlation id abc-123')
  const [timeRangeLabel, setTimeRangeLabel] = useState('Last 30 min')
  const [timezone, setTimezone] = useState<TimezoneOption>(defaultTimezone)
  const [customRange, setCustomRange] = useState<[Dayjs, Dayjs] | null>(null)
  const [apiName, setApiName] = useState('payment-sapi')
  const [selectedInputTypes, setSelectedInputTypes] = useState<
    InvestigationInputType[]
  >(detectInputTypes(rawText))
  const [stageIndex, setStageIndex] = useState(0)
  const mutation = useStartInvestigation()

  const detectedTypes = useMemo(() => detectInputTypes(rawText), [rawText])

  useEffect(() => {
    if (!mutation.isPending) {
      return
    }

    const timer = window.setInterval(() => {
      setStageIndex((current) => Math.min(current + 1, loadingStages.length - 1))
    }, 520)

    return () => window.clearInterval(timer)
  }, [mutation.isPending])

  function updateRawText(value: string) {
    setRawText(value)
    setSelectedInputTypes(detectInputTypes(value))
  }

  function toggleInputType(type: InvestigationInputType, checked: boolean) {
    setSelectedInputTypes((current) => {
      if (checked) {
        return current.includes(type) ? current : [...current, type]
      }

      const next = current.filter((item) => item !== type)
      return next.length > 0 ? next : current
    })
  }

  function submitInvestigation() {
    setStageIndex(0)
    mutation.mutate(
      {
        rawText,
        selectedInputTypes,
        timeRange: buildTimeRange(
          timeRangeLabel,
          timezone,
          customRange ?? undefined,
        ),
        apiName: apiName.trim() || undefined,
      },
      {
        onSuccess: (investigation) => {
          navigate(`/investigations/${investigation.id}`)
        },
      },
    )
  }

  const progress = mutation.isPending
    ? Math.round(((stageIndex + 1) / loadingStages.length) * 100)
    : 0
  const requiresCustomRange = timeRangeLabel === 'Custom'
  const customRangeReady = !requiresCustomRange || customRange !== null

  return (
    <main className="mx-auto flex min-h-[calc(100vh-56px)] max-w-6xl items-center px-5 py-10">
      <div className="grid w-full gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="min-w-0">
          <div className="mb-5">
            <Tag color="processing" className="mb-3">
              Frontend mock mode
            </Tag>
            <Typography.Title level={1} className="m-0 max-w-3xl">
              AI-assisted API log investigation
            </Typography.Title>
            <Typography.Paragraph className="mt-3 max-w-2xl text-base text-slate-600">
              Paste an error response, correlation ID, API name, field value, or
              natural-language question.
            </Typography.Paragraph>
          </div>

          <Card className="border-slate-200 shadow-sm">
            <Space orientation="vertical" size={16} className="w-full">
              <TextArea
                value={rawText}
                onChange={(event) => updateRawText(event.target.value)}
                autoSize={{ minRows: 8, maxRows: 12 }}
                placeholder="Paste an error response, correlation ID, API name, field value, or ask what you want to investigate..."
              />

              <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
                <Typography.Text className="text-sm font-medium text-slate-700">
                  Input signals
                </Typography.Text>
                <div className="mt-2 flex flex-wrap gap-2">
                  {Object.entries(inputTypeLabels).map(([value, label]) => {
                    const type = value as InvestigationInputType
                    const detected = detectedTypes.includes(type)
                    return (
                      <Tag.CheckableTag
                        key={value}
                        checked={selectedInputTypes.includes(type)}
                        onChange={(checked) => toggleInputType(type, checked)}
                        className={
                          detected
                            ? 'border border-teal-200 bg-teal-50'
                            : 'border border-slate-200 bg-white'
                        }
                      >
                        {label}
                      </Tag.CheckableTag>
                    )
                  })}
                </div>
              </div>

              <div className="grid gap-3 md:grid-cols-[180px_minmax(220px,1fr)_170px]">
                <Select
                  value={timeRangeLabel}
                  onChange={setTimeRangeLabel}
                  options={[
                    { value: 'Last 15 min', label: 'Last 15 min' },
                    { value: 'Last 30 min', label: 'Last 30 min' },
                    { value: 'Last 1 hour', label: 'Last 1 hour' },
                    { value: 'Last 4 hours', label: 'Last 4 hours' },
                    { value: 'Custom', label: 'Custom' },
                  ]}
                />
                <Input
                  value={apiName}
                  onChange={(event) => setApiName(event.target.value)}
                  placeholder="API name"
                />
                <Select
                  value={`${timezone.label}|${timezone.offset}`}
                  onChange={(value) => {
                    const [label, offset] = value.split('|')
                    setTimezone({ label, offset })
                  }}
                  options={timezoneOptions.map((item) => ({
                    value: `${item.label}|${item.offset}`,
                    label: `${item.label} (${item.offset})`,
                  }))}
                />
              </div>

              {requiresCustomRange ? (
                <RangePicker
                  showTime={{ format: 'HH:mm' }}
                  format="YYYY-MM-DD HH:mm"
                  className="w-full"
                  onChange={(value) => {
                    if (value?.[0] && value[1]) {
                      setCustomRange([value[0], value[1]])
                    } else {
                      setCustomRange(null)
                    }
                  }}
                />
              ) : null}

              {mutation.isError ? (
                <Alert
                  type="error"
                  title="Investigation failed"
                  description={mutation.error.message}
                  showIcon
                />
              ) : null}

              {mutation.isPending ? (
                <Alert
                  type="info"
                  showIcon
                  title={loadingStages[stageIndex]}
                  description={<Progress percent={progress} size="small" />}
                />
              ) : null}

              <Button
                type="primary"
                size="large"
                icon={<Search size={18} />}
                loading={mutation.isPending}
                disabled={!rawText.trim() || !customRangeReady}
                onClick={submitInvestigation}
              >
                Investigate
              </Button>
            </Space>
          </Card>
        </section>

        <aside className="space-y-4">
          <Card title="Examples" className="border-slate-200 shadow-sm">
            <Space orientation="vertical" className="w-full">
              {examples.map((example) => (
                <Button
                  key={example}
                  className="h-auto justify-start whitespace-normal text-left"
                  icon={<Sparkles size={15} />}
                  onClick={() => setRawText(example)}
                >
                  {example}
                </Button>
              ))}
            </Space>
          </Card>
        </aside>
      </div>
    </main>
  )
}
