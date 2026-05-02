import { Alert, Button, Card, Form, Input, Layout, Select, Tag, Typography } from 'antd'
import { useState } from 'react'
import {
  Bot,
  LockKeyhole,
  LogIn,
  SearchCheck,
  ShieldCheck,
  UserRound,
} from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import type { LucideIcon } from 'lucide-react'
import { useSessionStore } from './sessionStore'
import { loginSession, SessionApiError } from './sessionApi'
import { splunkEnvironmentOptions } from './splunkEnvironments'

interface LoginFormValues {
  username: string
  password: string
  environment: 'DEV' | 'PROD_ON_PREM' | 'PROD_AWS'
}

const featureHighlights: Array<{
  title: string
  body: string
  Icon: LucideIcon
}> = [
  {
    title: 'Logs to summary',
    body: 'Pull raw Splunk rows, send the evidence to AI, and return one readable diagnosis.',
    Icon: Bot,
  },
  {
    title: 'Evidence first',
    body: 'Keep executed SPL, linked log lines, and recommended next actions together.',
    Icon: SearchCheck,
  },
  {
    title: 'Session scoped',
    body: 'Use your Splunk identity for the current run only, without storing the password.',
    Icon: ShieldCheck,
  },
]

export function LoginPage() {
  const navigate = useNavigate()
  const [form] = Form.useForm<LoginFormValues>()
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const session = useSessionStore((state) => state.session)
  const setSession = useSessionStore((state) => state.setSession)
  const selectedEnvironment = Form.useWatch('environment', form) ?? 'DEV'

  if (session) {
    return <Navigate to="/" replace />
  }

  async function submit(values: LoginFormValues) {
    setSubmitting(true)
    setSubmitError(null)
    try {
      const response = await loginSession({
        splunkUsername: values.username.trim(),
        splunkPassword: values.password,
        environment: values.environment,
      })
      setSession({
        sessionId: response.sessionId,
        username: response.splunkUsername,
        environment: response.environment,
        createdAt: new Date().toISOString(),
      })
      navigate('/', { replace: true })
    } catch (error) {
      if (error instanceof SessionApiError) {
        setSubmitError(error.message)
      } else {
        setSubmitError('Failed to login. Please retry.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Layout className="splunky-shell">
      <main className="mx-auto grid min-h-screen w-full max-w-6xl items-center gap-8 px-5 py-10 lg:grid-cols-[minmax(0,1.05fr)_430px]">
        <section className="min-w-0">
          <Typography.Text className="text-sm font-semibold uppercase tracking-[0.28em] text-teal-700">
            Splunky
          </Typography.Text>
          <Typography.Title level={1} className="mb-3 mt-4 max-w-3xl">
            Turn Splunk logs into one useful investigation summary.
          </Typography.Title>
          <Typography.Paragraph className="max-w-2xl text-base text-slate-600">
            The first release stays narrow on purpose: choose the Splunk environment,
            log in with your session credential, fetch the logs, and let Splunky render
            a structured AI summary with evidence and executed SPL.
          </Typography.Paragraph>

          <div className="mt-8 grid gap-3 sm:grid-cols-3">
            {featureHighlights.map(({ title, body, Icon }) => (
              <div
                key={title}
                className="rounded-2xl border border-slate-200 bg-white/85 p-5 shadow-sm backdrop-blur"
              >
                <div className="mb-3 inline-flex rounded-xl bg-teal-50 p-2 text-teal-700">
                  <Icon size={18} />
                </div>
                <Typography.Text strong className="block text-slate-900">
                  {title}
                </Typography.Text>
                <Typography.Paragraph className="mb-0 mt-2 text-sm leading-6 text-slate-500">
                  {body}
                </Typography.Paragraph>
              </div>
            ))}
          </div>

          <div className="mt-8">
            <div className="mb-3 flex items-center justify-between gap-3">
              <Typography.Title level={4} className="!mb-0">
                Splunk Environments
              </Typography.Title>
              <Tag color="cyan">Session target</Tag>
            </div>
            <div className="grid gap-3">
              {splunkEnvironmentOptions.map((environment) => {
                const selected = selectedEnvironment === environment.code

                return (
                  <button
                    key={environment.code}
                    type="button"
                    className={`rounded-2xl border p-4 text-left transition ${
                      selected
                        ? 'border-teal-500 bg-teal-950 text-white shadow-lg shadow-teal-950/10'
                        : 'border-slate-200 bg-white/80 text-slate-900 shadow-sm hover:border-teal-300 hover:bg-white'
                    }`}
                    onClick={() => form.setFieldValue('environment', environment.code)}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Typography.Text
                          strong
                          className={selected ? '!text-white' : '!text-slate-900'}
                        >
                          {environment.label}
                        </Typography.Text>
                        <Typography.Paragraph
                          className={`mb-0 mt-1 text-sm ${
                            selected ? 'text-teal-50/90' : 'text-slate-500'
                          }`}
                        >
                          {environment.description}
                        </Typography.Paragraph>
                      </div>
                      <Tag color={selected ? 'gold' : 'default'}>{environment.code}</Tag>
                    </div>
                    <div
                      className={`mt-3 rounded-xl px-3 py-2 font-mono text-xs ${
                        selected
                          ? 'bg-white/10 text-teal-50'
                          : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {environment.url}
                    </div>
                  </button>
                )
              })}
            </div>
          </div>
        </section>

        <Card
          className="overflow-hidden border-slate-200 shadow-xl shadow-slate-200/70"
          styles={{ body: { padding: 28 } }}
        >
          <Typography.Title level={3} className="mb-1">
            Start Session
          </Typography.Title>
          <Typography.Paragraph className="mb-5 text-slate-500">
            Pick the target environment first, then enter the Splunk username and password
            for this investigation session.
          </Typography.Paragraph>

          <Alert
            className="mb-5"
            type="info"
            showIcon
            message="Splunk password is session-only"
            description="The backend uses your password to log in to Splunk and query logs on your behalf. It is not persisted, and the session expires after 30 minutes of inactivity."
          />

          {submitError ? (
            <Alert className="mb-4" type="error" showIcon message={submitError} />
          ) : null}

          <Form<LoginFormValues>
            form={form}
            layout="vertical"
            onFinish={submit}
            initialValues={{ environment: 'DEV' }}
          >
            <Form.Item
              label="Environment"
              name="environment"
              rules={[{ required: true, message: 'Select target environment.' }]}
            >
              <Select
                options={splunkEnvironmentOptions.map((environment) => ({
                  label: environment.label,
                  value: environment.code,
                }))}
              />
            </Form.Item>
            <Form.Item
              label="Username"
              name="username"
              rules={[{ required: true, message: 'Enter your Splunk username.' }]}
            >
              <Input
                prefix={<UserRound size={16} />}
                autoComplete="username"
                placeholder="e.g. zhong.zc"
              />
            </Form.Item>
            <Form.Item
              label="Password"
              name="password"
              rules={[{ required: true, message: 'Enter your Splunk password.' }]}
            >
              <Input.Password
                prefix={<LockKeyhole size={16} />}
                autoComplete="current-password"
                placeholder="Session-only password"
              />
            </Form.Item>
            <Button
              block
              type="primary"
              htmlType="submit"
              icon={<LogIn size={16} />}
              size="large"
              loading={submitting}
            >
              Start Session
            </Button>
          </Form>
        </Card>
      </main>
    </Layout>
  )
}
