import {
  Alert,
  Button,
  Card,
  DatePicker,
  Input,
  Progress,
  Segmented,
  Select,
  Space,
  Typography,
} from 'antd'
import { Clock3, FileSearch, Search, Sparkles } from 'lucide-react'
import dayjs, { type Dayjs } from 'dayjs'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useStartInvestigation } from '../hooks/useInvestigation'
import {
  buildTimeRange,
  defaultTimezone,
  detectInputTypes,
  isSplunkUrl,
  timezoneOptions,
} from '../utils/inputDetection'

const { TextArea } = Input
const { RangePicker } = DatePicker

type TimePreset =
  | 'From Splunk URL'
  | 'Last 15 min'
  | 'Last 30 min'
  | 'Last 1 hour'
  | 'Last 4 hours'
  | 'Custom'

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

const recentInvestigations = [
  'payment-sapi HUB timeout',
  'payee-service validation warning',
  'limit-service latency spike',
]

const baseTimeOptions: TimePreset[] = [
  'Last 15 min',
  'Last 30 min',
  'Last 1 hour',
  'Last 4 hours',
  'Custom',
]

function timezoneKey(timezone: typeof defaultTimezone) {
  return `${timezone.label}:${timezone.offset}`
}

export function InvestigationInput() {
  const navigate = useNavigate()
  const [rawText, setRawText] = useState('correlation id abc-123')
  const [timePreset, setTimePreset] = useState<TimePreset>('Last 30 min')
  const [timezone, setTimezone] = useState(defaultTimezone)
  const [customRange, setCustomRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'minute'),
    dayjs(),
  ])
  const [stageIndex, setStageIndex] = useState(0)
  const mutation = useStartInvestigation()

  const detectedTypes = useMemo(() => detectInputTypes(rawText), [rawText])
  const splunkUrlDetected = useMemo(() => isSplunkUrl(rawText), [rawText])
  const timeOptions = useMemo(
    () =>
      splunkUrlDetected
        ? ['From Splunk URL' as const, ...baseTimeOptions]
        : baseTimeOptions,
    [splunkUrlDetected],
  )

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
    const wasSplunkUrl = isSplunkUrl(rawText)
    const nextIsSplunkUrl = isSplunkUrl(value)

    setRawText(value)

    if (nextIsSplunkUrl && !wasSplunkUrl) {
      setTimePreset('From Splunk URL')
    }

    if (!nextIsSplunkUrl && wasSplunkUrl && timePreset === 'From Splunk URL') {
      setTimePreset('Last 30 min')
    }
  }

  function submitInvestigation() {
    setStageIndex(0)
    mutation.mutate(
      {
        rawText,
        selectedInputTypes: detectedTypes,
        timeRange: buildTimeRange(
          timePreset,
          timezone,
          timePreset === 'Custom' ? customRange : undefined,
        ),
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
  const selectedTimezoneKey = timezoneKey(timezone)

  return (
    <main className="mx-auto flex min-h-[calc(100vh-56px)] max-w-6xl items-center px-5 py-10">
      <div className="grid w-full gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <section className="min-w-0">
          <div className="mb-5">
            <Typography.Title level={1} className="m-0 max-w-3xl">
              AI-assisted API log investigation
            </Typography.Title>
            <Typography.Paragraph className="mt-3 max-w-2xl text-base text-slate-600">
              Paste an error response, correlation ID, Splunk search URL, or
              a natural-language question. Splunky will extract time range,
              SPL, APIs, and evidence from the input.
            </Typography.Paragraph>
          </div>

          <Card className="border-slate-200 shadow-sm">
            <Space orientation="vertical" size={16} className="w-full">
              <TextArea
                value={rawText}
                onChange={(event) => updateRawText(event.target.value)}
                autoSize={{ minRows: 10, maxRows: 16 }}
                placeholder="Paste a Splunk URL, error response, correlation ID, or ask what you want to investigate..."
              />

              <div className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3 lg:grid-cols-[minmax(0,1fr)_180px]">
                <div className="min-w-0">
                  <Typography.Text strong>Time range</Typography.Text>
                  <Segmented
                    block
                    value={timePreset}
                    className="mt-2 w-full"
                    options={timeOptions.map((option) => ({
                      label: option,
                      value: option,
                    }))}
                    onChange={(value) => setTimePreset(value as TimePreset)}
                  />
                  {timePreset === 'Custom' ? (
                    <RangePicker
                      showTime
                      allowClear={false}
                      value={customRange}
                      format="YYYY-MM-DD HH:mm"
                      className="mt-3 w-full"
                      onChange={(value) => {
                        if (value?.[0] && value[1]) {
                          setCustomRange([value[0], value[1]])
                        }
                      }}
                    />
                  ) : null}
                  {timePreset === 'From Splunk URL' ? (
                    <Typography.Text className="mt-2 block text-xs text-slate-500">
                      SPL and time window will be imported from the supplied
                      Splunk URL when backend integration is ready.
                    </Typography.Text>
                  ) : null}
                </div>
                <div>
                  <Typography.Text strong>Timezone</Typography.Text>
                  <Select
                    className="mt-2 w-full"
                    value={selectedTimezoneKey}
                    options={timezoneOptions.map((option) => ({
                      value: timezoneKey(option),
                      label: `${option.label} ${option.offset}`,
                    }))}
                    onChange={(value) => {
                      const selected =
                        timezoneOptions.find(
                          (option) => timezoneKey(option) === value,
                        ) ?? defaultTimezone
                      setTimezone(selected)
                    }}
                  />
                </div>
              </div>

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
                disabled={!rawText.trim()}
                onClick={submitInvestigation}
              >
                Investigate
              </Button>
            </Space>
          </Card>
        </section>

        <aside className="space-y-4">
          <Card title="Examples" className="border-slate-200 shadow-sm">
            <div className="space-y-2">
              {examples.map((example) => (
                <button
                  key={example}
                  type="button"
                  className="flex w-full items-start gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-left text-sm text-slate-700 transition hover:border-teal-300 hover:bg-teal-50"
                  onClick={() => updateRawText(example)}
                >
                  <Sparkles size={15} className="mt-0.5 shrink-0 text-teal-700" />
                  <span className="min-w-0 whitespace-normal break-words leading-5">
                    {example}
                  </span>
                </button>
              ))}
            </div>
          </Card>
          <Card title="Recent Investigations" className="border-slate-200 shadow-sm">
            <div className="space-y-2">
              {recentInvestigations.map((item) => (
                <button
                  key={item}
                  type="button"
                  className="flex w-full items-start gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-left text-sm text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
                  onClick={() => updateRawText(item)}
                >
                  <Clock3 size={15} className="mt-0.5 shrink-0 text-slate-500" />
                  <span className="min-w-0 whitespace-normal break-words leading-5">
                    {item}
                  </span>
                </button>
              ))}
            </div>
            <div className="mt-3 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-500">
              <FileSearch size={14} className="mr-1 inline align-[-2px]" />
              Saved history will connect to backend storage later.
            </div>
          </Card>
        </aside>
      </div>
    </main>
  )
}
