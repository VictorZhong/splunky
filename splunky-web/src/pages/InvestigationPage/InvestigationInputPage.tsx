import { Layout } from 'antd'
import { InvestigationInput } from '../../features/investigation/components/InvestigationInput'
import { AppHeader } from '../../shared/components/AppHeader'

export function InvestigationInputPage() {
  return (
    <Layout className="splunky-shell">
      <AppHeader />
      <InvestigationInput />
    </Layout>
  )
}
