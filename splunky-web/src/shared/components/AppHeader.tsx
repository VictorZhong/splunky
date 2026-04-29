import { Button, Layout, Select, Space, Typography } from 'antd'
import { RotateCcw } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { Environment } from '../../features/investigation/types'
import { formatEnvironment } from '../../features/investigation/utils/formatters'

const { Header } = Layout

type AppHeaderProps = {
  environment?: Environment
}

const environmentOptions: Environment[] = ['SIT', 'UAT', 'NFT', 'LOCAL_MOCK']

export function AppHeader({ environment = 'SIT' }: AppHeaderProps) {
  const navigate = useNavigate()

  return (
    <Header className="sticky top-0 z-30 flex h-14 items-center justify-between px-5 shadow-sm">
      <Space align="center" size={18}>
        <button
          className="border-0 bg-transparent p-0 text-left text-white"
          type="button"
          onClick={() => navigate('/')}
        >
          <Typography.Text className="text-lg font-semibold text-white">
            Splunky
          </Typography.Text>
        </button>
        <Select
          value={environment}
          onChange={() => undefined}
          options={environmentOptions.map((value) => ({
            value,
            label: formatEnvironment(value),
          }))}
          variant="filled"
          size="small"
          className="min-w-32"
        />
        <Typography.Text className="hidden text-sm text-slate-300 sm:inline">
          User: zhong.zc
        </Typography.Text>
      </Space>

      <Button
        icon={<RotateCcw size={16} />}
        type="primary"
        onClick={() => navigate('/')}
      >
        New Investigation
      </Button>
    </Header>
  )
}
