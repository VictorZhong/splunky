import { Card, Layout, Spin, Typography } from 'antd'
import { useParams } from 'react-router-dom'
import { DetailDrawer } from '../../features/investigation/components/DetailDrawer'
import { RawLogsView } from '../../features/investigation/components/RawLogsView'
import { ResultContextBar } from '../../features/investigation/components/ResultContextBar'
import { SplInspectorView } from '../../features/investigation/components/SplInspectorView'
import { SummaryView } from '../../features/investigation/components/SummaryView'
import { useInvestigation } from '../../features/investigation/hooks/useInvestigation'
import { AppHeader } from '../../shared/components/AppHeader'

export function InvestigationPage() {
  const { investigationId } = useParams()
  const query = useInvestigation(investigationId)

  if (query.isLoading) {
    return (
      <Layout className="splunky-shell">
        <AppHeader showNewSearch />
        <div className="flex min-h-[calc(100vh-56px)] items-center justify-center">
          <Spin size="large" description="Loading investigation" />
        </div>
      </Layout>
    )
  }

  if (query.isError || !query.data) {
    return (
      <Layout className="splunky-shell">
        <AppHeader showNewSearch />
        <main className="mx-auto w-full max-w-4xl px-5 py-10">
          <Card className="border-red-200">
            <Typography.Title level={4} className="!mb-2">
              Investigation unavailable
            </Typography.Title>
            <Typography.Paragraph className="!mb-0 text-slate-600">
              {query.error?.message ?? 'The investigation was not found.'}
            </Typography.Paragraph>
          </Card>
        </main>
      </Layout>
    )
  }

  const investigation = query.data
  const result = investigation.activeResult

  return (
    <Layout className="splunky-shell">
      <AppHeader showNewSearch />
      <main className="mx-auto w-full max-w-7xl px-4 py-4">
        <div className="space-y-4">
          <ResultContextBar investigation={investigation} />

          <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <SummaryView result={result} />
          </section>

          <Card
            className="border-slate-200 shadow-sm"
            title="Splunk logs sent to AI"
            extra={
              <Typography.Text className="text-sm text-slate-500">
                {result.rawLogs.length} preview rows
              </Typography.Text>
            }
          >
            <RawLogsView result={result} />
          </Card>

          <Card
            className="border-slate-200 shadow-sm"
            title="Executed SPL"
            extra={
              <Typography.Text className="text-sm text-slate-500">
                {result.queries.length} query
                {result.queries.length === 1 ? '' : 'ies'}
              </Typography.Text>
            }
          >
            <SplInspectorView result={result} />
          </Card>
        </div>
      </main>
      <DetailDrawer investigation={investigation} />
    </Layout>
  )
}
