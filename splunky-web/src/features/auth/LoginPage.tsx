import { Alert, Button, Card, Form, Input, Layout, Typography } from 'antd'
import {
  FileSearch,
  GitBranch,
  LockKeyhole,
  LogIn,
  MessagesSquare,
  UserRound,
} from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useSessionStore } from './sessionStore'
import type { LucideIcon } from 'lucide-react'

interface LoginFormValues {
  username: string
  password: string
}

const featureHighlights: Array<{
  title: string
  body: string
  Icon: LucideIcon
}> = [
  {
    title: 'Evidence-first diagnosis',
    body: 'Summary, confidence, linked logs, and recommended actions.',
    Icon: FileSearch,
  },
  {
    title: 'Call path visibility',
    body: 'Timeline, sequence, and service graph show where requests failed.',
    Icon: GitBranch,
  },
  {
    title: 'Follow-up workflow',
    body: 'Ask for expansion, similar errors, or incident-ready summaries.',
    Icon: MessagesSquare,
  },
]

export function LoginPage() {
  const navigate = useNavigate()
  const session = useSessionStore((state) => state.session)
  const login = useSessionStore((state) => state.login)

  if (session) {
    return <Navigate to="/" replace />
  }

  function submit(values: LoginFormValues) {
    login(values.username.trim())
    navigate('/', { replace: true })
  }

  return (
    <Layout className="splunky-shell">
      <main className="mx-auto grid min-h-screen w-full max-w-6xl items-center gap-8 px-5 py-10 lg:grid-cols-[minmax(0,1fr)_420px]">
        <section className="max-w-2xl">
          <Typography.Text className="text-sm font-semibold uppercase tracking-wide text-teal-700">
            Splunky
          </Typography.Text>
          <Typography.Title level={1} className="mt-3">
            Investigate API failures from Splunk evidence
          </Typography.Title>
          <Typography.Paragraph className="text-base text-slate-600">
            Start from a Splunk URL, correlation ID, error payload, or plain
            question. Splunky turns raw Splunk logs into a structured
            investigation workspace.
          </Typography.Paragraph>
          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {featureHighlights.map(({ title, body, Icon }) => (
              <div
                key={title}
                className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
              >
                <Icon size={18} className="mb-2 text-teal-700" />
                <Typography.Text strong>{title}</Typography.Text>
                <Typography.Paragraph className="mb-0 mt-1 text-sm text-slate-500">
                  {body}
                </Typography.Paragraph>
              </div>
            ))}
          </div>
        </section>

        <Card className="border-slate-200 shadow-sm">
          <Typography.Title level={3} className="mb-1">
            Login
          </Typography.Title>
          <Typography.Paragraph className="text-slate-500">
            Use your test Splunk account for the investigation session.
          </Typography.Paragraph>
          <Alert
            className="mb-5"
            type="info"
            showIcon
            title="Splunky queries Splunk as you"
            description="Your password is used only for the current session and is not saved. It expires immediately after logout, and the session also expires after 30 minutes of inactivity."
          />
          <Form<LoginFormValues> layout="vertical" onFinish={submit}>
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
            >
              Start Session
            </Button>
          </Form>
        </Card>
      </main>
    </Layout>
  )
}
