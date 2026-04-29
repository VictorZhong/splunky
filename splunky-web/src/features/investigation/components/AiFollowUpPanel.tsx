import {
  Alert,
  Button,
  Card,
  Collapse,
  Divider,
  Input,
  Space,
  Tag,
  Timeline,
  Typography,
} from 'antd'
import { Bot, Send, UserRound } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFollowUp } from '../hooks/useFollowUp'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { ChatMessage, Investigation, RequeryStatus } from '../types'
import { classifyFollowUp } from '../utils/followUpClassification'
import { formatDateTime } from '../utils/formatters'

type AiFollowUpPanelProps = {
  investigation: Investigation
}

function ChatBubble({ message }: { message: ChatMessage }) {
  const isUser = message.role === 'USER'

  return (
    <div className={`flex gap-2 ${isUser ? 'justify-end' : 'justify-start'}`}>
      {!isUser ? <Bot size={18} className="mt-1 text-teal-700" /> : null}
      <div
        className={`max-w-[86%] rounded-lg px-3 py-2 text-sm ${
          isUser
            ? 'bg-slate-900 text-white'
            : 'border border-slate-200 bg-slate-50 text-slate-700'
        }`}
      >
        <div className="whitespace-pre-line">{message.content}</div>
        <div className={`mt-1 text-[11px] ${isUser ? 'text-slate-300' : 'text-slate-400'}`}>
          {formatDateTime(message.createdAt)}
        </div>
      </div>
      {isUser ? <UserRound size={18} className="mt-1 text-slate-600" /> : null}
    </div>
  )
}

function RequeryStatusCard({
  status,
  onViewSpl,
}: {
  status: RequeryStatus
  onViewSpl: () => void
}) {
  return (
    <Alert
      type="info"
      showIcon
      title={status.title}
      description={
        <div className="space-y-2">
          <ul className="m-0 pl-5">
            {status.changes.map((change) => (
              <li key={change}>{change}</li>
            ))}
          </ul>
          <Button size="small" onClick={onViewSpl}>
            View SPL
          </Button>
        </div>
      }
    />
  )
}

export function AiFollowUpPanel({ investigation }: AiFollowUpPanelProps) {
  const [prompt, setPrompt] = useState('')
  const [pendingRequery, setPendingRequery] = useState<RequeryStatus | null>(null)
  const navigate = useNavigate()
  const setActiveTab = useInvestigationUiStore((state) => state.setActiveTab)
  const mutation = useFollowUp(investigation.id)

  function submit(value: string) {
    const trimmed = value.trim()
    if (!trimmed || mutation.isPending) {
      return
    }

    const actionType = classifyFollowUp(trimmed)
    setPendingRequery(
      actionType === 'RERUN_QUERY'
        ? {
            title: 'Re-running investigation',
            changes: [
              'Time range: Last 1 hour',
              'Added filter: downstream = hub-payment-propose-api',
              'Query type: similar timeout search',
            ],
            queryType: 'similar timeout search',
          }
        : null,
    )

    mutation.mutate(
      { prompt: trimmed },
      {
        onSuccess: (response) => {
          setPrompt('')
          setPendingRequery(response.requeryStatus ?? null)

          if (response.investigation.id !== investigation.id) {
            navigate(`/investigations/${response.investigation.id}`)
          }

          window.setTimeout(() => setPendingRequery(null), 2400)
        },
      },
    )
  }

  return (
    <aside className="min-w-0 space-y-4 xl:sticky xl:top-[80px] xl:h-[calc(100vh-96px)]">
      <Card
        title={
          <Space>
            <Bot size={18} />
            AI Assistant
          </Space>
        }
        className="flex h-full flex-col border-slate-200 shadow-sm"
        styles={{
          body: {
            display: 'flex',
            minHeight: 0,
            flex: 1,
            flexDirection: 'column',
            gap: 14,
          },
        }}
      >
        <div className="min-h-[420px] flex-1 space-y-3 overflow-auto rounded-lg border border-slate-100 bg-white p-3">
          {investigation.conversation.map((message) => (
            <ChatBubble key={message.id} message={message} />
          ))}
          {pendingRequery ? (
            <RequeryStatusCard
              status={pendingRequery}
              onViewSpl={() => setActiveTab('spl')}
            />
          ) : null}
        </div>

        <div>
          <Typography.Text strong>Suggested Follow-ups</Typography.Text>
          <div className="mt-2 flex flex-wrap gap-2">
            {investigation.activeResult.suggestedFollowUps.map((item) => (
              <Tag.CheckableTag
                key={item.id}
                checked={false}
                onChange={() => submit(item.prompt)}
              >
                {item.label}
              </Tag.CheckableTag>
            ))}
          </div>
        </div>

        <Divider className="m-0" />

        <Input.TextArea
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          onPressEnter={(event) => {
            if (!event.shiftKey) {
              event.preventDefault()
              submit(prompt)
            }
          }}
          autoSize={{ minRows: 3, maxRows: 5 }}
          placeholder="Ask follow-up..."
        />
        <Button
          type="primary"
          icon={<Send size={16} />}
          loading={mutation.isPending}
          disabled={!prompt.trim()}
          onClick={() => submit(prompt)}
        >
          Send
        </Button>

        <Collapse
          size="small"
          ghost
          items={[
            {
              key: 'runs',
              label: 'Run History',
              children: (
                <Timeline
                  items={investigation.runs
                    .slice()
                    .reverse()
                    .map((run) => ({
                      color:
                        run.runId === investigation.activeRunId ? 'green' : 'gray',
                      content: (
                        <div>
                          <Typography.Text strong>
                            Run #{run.runNumber}: {run.title}
                          </Typography.Text>
                          <Typography.Paragraph className="m-0 text-xs text-slate-500">
                            {run.summary}
                          </Typography.Paragraph>
                        </div>
                      ),
                    }))}
                />
              ),
            },
          ]}
        />
      </Card>
    </aside>
  )
}
