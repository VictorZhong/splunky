import { Button, Dropdown, Space, Typography } from 'antd'
import { History } from 'lucide-react'
import type { InvestigationRunSummary } from '../types'
import { formatDateTime } from '../utils/formatters'

type RunHistoryDropdownProps = {
  runs: InvestigationRunSummary[]
  activeRunId: string
}

export function RunHistoryDropdown({
  runs,
  activeRunId,
}: RunHistoryDropdownProps) {
  const activeRun = runs.find((run) => run.runId === activeRunId) ?? runs[0]

  return (
    <Dropdown
      trigger={['click']}
      menu={{
        items: runs
          .slice()
          .reverse()
          .map((run) => ({
            key: run.runId,
            label: (
              <div className="max-w-80">
                <Typography.Text strong>
                  Run #{run.runNumber}: {run.title}
                </Typography.Text>
                <Typography.Paragraph className="m-0 text-xs text-slate-500">
                  {run.summary}
                </Typography.Paragraph>
                <Typography.Text className="text-xs text-slate-400">
                  {formatDateTime(run.createdAt)}
                </Typography.Text>
              </div>
            ),
          })),
      }}
    >
      <Button icon={<History size={15} />}>
        <Space>
          Run #{activeRun?.runNumber ?? 1}
          <span className="hidden sm:inline">{activeRun?.title}</span>
        </Space>
      </Button>
    </Dropdown>
  )
}
