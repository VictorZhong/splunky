import { Alert, Button, Card, Form, Input, Layout, Typography } from 'antd'
import { LockKeyhole, LogIn, ShieldCheck, UserRound } from 'lucide-react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useSessionStore } from './sessionStore'

interface LoginFormValues {
  username: string
  password: string
}

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
            Sign in with your Splunk credentials
          </Typography.Title>
          <Typography.Paragraph className="text-base text-slate-600">
            Splunky uses these credentials only to query Splunk during this
            browser session. The password is not saved by the frontend mock.
          </Typography.Paragraph>
          <div className="mt-6 grid gap-3 sm:grid-cols-3">
            {[
              ['Session scoped', 'A session ID is generated after login.'],
              ['No password storage', 'Credentials are not persisted.'],
              ['401 aware', 'Invalid credentials will prompt re-login.'],
            ].map(([title, body]) => (
              <div
                key={title}
                className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
              >
                <ShieldCheck size={18} className="mb-2 text-teal-700" />
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
            title="Credentials are session-only"
            description="The frontend sends a generated session ID with mock API calls. Backend credential validation will be added later."
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
