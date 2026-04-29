import { Alert, Layout, Spin, Tabs } from 'antd'
import { lazy, Suspense, type ReactNode } from 'react'
import { useParams } from 'react-router-dom'
import { AiFollowUpPanel } from '../../features/investigation/components/AiFollowUpPanel'
import { DetailDrawer } from '../../features/investigation/components/DetailDrawer'
import { ResultContextBar } from '../../features/investigation/components/ResultContextBar'
import { useInvestigation } from '../../features/investigation/hooks/useInvestigation'
import {
  type InvestigationTabKey,
  useInvestigationUiStore,
} from '../../features/investigation/store/investigationUiStore'
import { AppHeader } from '../../shared/components/AppHeader'

const SummaryView = lazy(() =>
  import('../../features/investigation/components/SummaryView').then((module) => ({
    default: module.SummaryView,
  })),
)
const TimelineView = lazy(() =>
  import('../../features/investigation/components/TimelineView').then((module) => ({
    default: module.TimelineView,
  })),
)
const ServiceGraphView = lazy(() =>
  import('../../features/investigation/components/ServiceGraphView').then(
    (module) => ({
      default: module.ServiceGraphView,
    }),
  ),
)
const SequenceView = lazy(() =>
  import('../../features/investigation/components/SequenceView').then((module) => ({
    default: module.SequenceView,
  })),
)
const DownstreamCallsView = lazy(() =>
  import('../../features/investigation/components/DownstreamCallsView').then(
    (module) => ({
      default: module.DownstreamCallsView,
    }),
  ),
)
const RawLogsView = lazy(() =>
  import('../../features/investigation/components/RawLogsView').then((module) => ({
    default: module.RawLogsView,
  })),
)
const SplInspectorView = lazy(() =>
  import('../../features/investigation/components/SplInspectorView').then(
    (module) => ({
      default: module.SplInspectorView,
    }),
  ),
)

function TabContent({ children }: { children: ReactNode }) {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-80 items-center justify-center">
          <Spin />
        </div>
      }
    >
      {children}
    </Suspense>
  )
}

export function InvestigationPage() {
  const { investigationId } = useParams()
  const query = useInvestigation(investigationId)
  const activeTab = useInvestigationUiStore((state) => state.activeTab)
  const setActiveTab = useInvestigationUiStore((state) => state.setActiveTab)

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
          <Alert
            type="error"
            showIcon
            title="Investigation unavailable"
            description={query.error?.message ?? 'The mock investigation was not found.'}
          />
        </main>
      </Layout>
    )
  }

  const investigation = query.data
  const result = investigation.activeResult

  return (
    <Layout className="splunky-shell">
      <AppHeader showNewSearch />
      <main className="grid min-h-[calc(100vh-56px)] gap-4 p-4 2xl:grid-cols-[minmax(0,1fr)_480px] xl:grid-cols-[minmax(0,1fr)_440px]">
        <section className="min-w-0 space-y-4">
          <ResultContextBar investigation={investigation} />
          <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <Tabs
              activeKey={activeTab}
              onChange={(key) => setActiveTab(key as InvestigationTabKey)}
              items={[
                {
                  key: 'summary',
                  label: 'Summary',
                  children: (
                    <TabContent>
                      <SummaryView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'timeline',
                  label: 'Timeline',
                  children: (
                    <TabContent>
                      <TimelineView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'graph',
                  label: 'Service Graph',
                  children: (
                    <TabContent>
                      <ServiceGraphView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'sequence',
                  label: 'Sequence',
                  children: (
                    <TabContent>
                      <SequenceView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'calls',
                  label: 'Downstream Calls',
                  children: (
                    <TabContent>
                      <DownstreamCallsView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'logs',
                  label: 'Raw Logs',
                  children: (
                    <TabContent>
                      <RawLogsView result={result} />
                    </TabContent>
                  ),
                },
                {
                  key: 'spl',
                  label: 'SPL',
                  children: (
                    <TabContent>
                      <SplInspectorView result={result} />
                    </TabContent>
                  ),
                },
              ]}
            />
          </div>
        </section>

        <AiFollowUpPanel investigation={investigation} />
      </main>
      <DetailDrawer investigation={investigation} />
    </Layout>
  )
}
