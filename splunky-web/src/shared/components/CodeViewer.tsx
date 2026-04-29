import { json } from '@codemirror/lang-json'
import { EditorState } from '@codemirror/state'
import { EditorView, lineNumbers } from '@codemirror/view'
import type { CSSProperties } from 'react'
import { useEffect, useRef } from 'react'

type CodeViewerProps = {
  value: string
  language?: 'json' | 'spl' | 'text'
  minHeight?: number
}

const readonlyTheme = EditorView.theme({
  '&': {
    minHeight: 'var(--viewer-height)',
  },
  '.cm-scroller': {
    fontFamily:
      'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace',
  },
})

export function CodeViewer({
  value,
  language = 'text',
  minHeight = 140,
}: CodeViewerProps) {
  const hostRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!hostRef.current) {
      return
    }

    const extensions = [
      lineNumbers(),
      EditorState.readOnly.of(true),
      EditorView.editable.of(false),
      EditorView.lineWrapping,
      readonlyTheme,
    ]

    if (language === 'json') {
      extensions.push(json())
    }

    const view = new EditorView({
      parent: hostRef.current,
      state: EditorState.create({
        doc: value,
        extensions,
      }),
    })

    return () => {
      view.destroy()
    }
  }, [language, value])

  return (
    <div
      className="code-viewer"
      ref={hostRef}
      style={{ '--viewer-height': `${minHeight}px` } as CSSProperties}
    />
  )
}
