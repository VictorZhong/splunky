import { Avatar, Button, Dropdown, Layout, Space, Typography } from 'antd'
import { BookOpen, LogOut, Plus } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useSessionStore } from '../../features/auth/sessionStore'
import { logoutSession } from '../../features/auth/sessionApi'
import { buildAppUrl } from '../../app/config'

const { Header } = Layout

type AppHeaderProps = {
  showNewSearch?: boolean
}

export function AppHeader({ showNewSearch = false }: AppHeaderProps) {
  const navigate = useNavigate()
  const session = useSessionStore((state) => state.session)
  const logout = useSessionStore((state) => state.logout)
  const initials = session?.username.slice(0, 2).toUpperCase() ?? 'SP'

  function openNewSearchTab() {
    window.open(buildAppUrl('/'), '_blank')?.focus()
  }

  function handleLogout() {
    if (session?.sessionId) {
      void logoutSession(session.sessionId).catch(() => undefined)
    }
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <Header className="sticky top-0 z-30 flex h-16 items-center justify-between px-5 shadow-sm">
      <Space align="center" size={18}>
        <button
          className="flex items-center gap-3 border-0 bg-transparent p-0 text-left text-white"
          type="button"
          onClick={() => navigate('/')}
        >
          <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-teal-500 text-sm font-bold text-slate-950">
            S
          </span>
          <span>
            <Typography.Text className="block text-lg font-semibold leading-5 text-white">
              Splunky
            </Typography.Text>
            <Typography.Text className="hidden text-xs leading-4 text-slate-400 sm:block">
              Testing log investigation
            </Typography.Text>
          </span>
        </button>
        <Button type="text" className="text-slate-300" icon={<BookOpen size={16} />}>
          User Guide
        </Button>
      </Space>

      <Space align="center" size={12}>
        {showNewSearch ? (
          <Button icon={<Plus size={16} />} type="primary" onClick={openNewSearchTab}>
            New Search
          </Button>
        ) : null}
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              {
                key: 'user',
                disabled: true,
                label: session?.username ?? 'Unknown user',
              },
              {
                key: 'logout',
                icon: <LogOut size={15} />,
                label: 'Logout',
                onClick: handleLogout,
              },
            ],
          }}
        >
          <button
            type="button"
            className="flex items-center gap-2 border-0 bg-transparent p-0 text-white"
          >
            <Avatar className="bg-teal-600 text-white">{initials}</Avatar>
          </button>
        </Dropdown>
      </Space>
    </Header>
  )
}
