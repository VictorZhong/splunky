import { CodeViewer } from './CodeViewer'

type JsonViewerProps = {
  value: unknown
}

export function JsonViewer({ value }: JsonViewerProps) {
  return (
    <CodeViewer
      language="json"
      value={JSON.stringify(value, null, 2)}
      minHeight={180}
    />
  )
}
