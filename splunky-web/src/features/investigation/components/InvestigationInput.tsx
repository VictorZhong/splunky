import {
  Alert,
  Button,
  Card,
  Input,
  Progress,
  Segmented,
  Select,
  Space,
  Tag,
  Typography,
} from 'antd'
import { Search, Sparkles } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useStartInvestigation } from '../hooks/useInvestigation'
import type { Environment } from '../types'
import { detectInputType } from '../utils/inputDetection'

const { TextArea } = Input

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
  const [environment, setEnvironment] = useState<Environment>('SIT')
  const [timeRangeLabel, setTimeRangeLabel] = useState('Last 30 min')
  const [apiName, setApiName] = useState('payment-sapi')
  const [market, setMarket] = useState('HK')
  const [stageIndex, setStageIndex] = useState(0)
  const mutation = useStartInvestigation()

  const detectedType = useMemo(() => detectInputType(rawText), [rawText])

  useEffect(() => {
    if (!mutation.isPending) {
      return
    }

    const timer = window.setInterval(() => {
      setStageIndex((current) => Math.min(current + 1, loadingStages.length - 1))
    }, 520)

    return () => window.clearInterval(timer)
  }, [mutation.isPending])

  function submitInvestigation() {
    setStageIndex(0)
    mutation.mutate(
      {
        rawText,
        environment,
        timeRangeLabel,
        apiName: apiName.trim() || undefined,
        market,
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
                onChange={(event) => setRawText(event.target.value)}
                autoSize={{ minRows: 8, maxRows: 12 }}
                placeholder="Paste an error response, correlation ID, API name, field value, or ask what you want to investigate..."
              />

              <div className="flex flex-wrap items-center gap-3">
                <Segmented
                  value={detectedType}
                  options={Object.entries(inputTypeLabels).map(([value, label]) => ({
                    label,
                    value,
                  }))}
                />
                <Tag color="cyan">Detected: {inputTypeLabels[detectedType]}</Tag>
              </div>

              <div className="grid gap-3 md:grid-cols-4">
                <Select
                  value={environment}
                  onChange={setEnvironment}
                  options={[
                    { value: 'SIT', label: 'SIT' },
                    { value: 'UAT', label: 'UAT' },
                    { value: 'NFT', label: 'NFT' },
                    { value: 'LOCAL_MOCK', label: 'Local Mock' },
                  ]}
                />
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
                  value={market}
                  onChange={setMarket}
                  options={['All', 'HK', 'TW', 'PH', 'SG', 'UK'].map((value) => ({
                    value,
                    label: value,
                  }))}
                />
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
          <Card className="border-slate-200 shadow-sm">
            <Typography.Text strong>Mock triggers</Typography.Text>
            <div className="mt-3 flex flex-wrap gap-2">
              <Tag>no-result</Tag>
              <Tag>mock-error</Tag>
              <Tag>def-456</Tag>
            </div>
          </Card>
        </aside>
      </div>
    </main>
  )
}
