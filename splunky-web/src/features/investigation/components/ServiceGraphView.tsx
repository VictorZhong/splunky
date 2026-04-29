import { Alert, Empty, Space, Tag, Typography } from 'antd'
import {
  Background,
  Controls,
  MarkerType,
  ReactFlow,
  type Edge,
  type Node,
} from '@xyflow/react'
import { useMemo } from 'react'
import { StatusTag } from '../../../shared/components/StatusTag'
import { useInvestigationUiStore } from '../store/investigationUiStore'
import type { InvestigationResult, ServiceEdge, ServiceNode } from '../types'

type ServiceGraphViewProps = {
  result: InvestigationResult
}

const positions: Record<string, { x: number; y: number }> = {
  gateway: { x: 20, y: 170 },
  'payment-sapi': { x: 300, y: 170 },
  'payee-service': { x: 620, y: 30 },
  'limit-service': { x: 620, y: 170 },
  'hub-payment-propose-api': { x: 620, y: 320 },
}

function buildNode(node: ServiceNode): Node {
  const failed = node.status === 'FAILED' || node.status === 'TIMEOUT'

  return {
    id: node.id,
    position: positions[node.id] ?? { x: 0, y: 0 },
    data: {
      label: (
        <div className="text-left">
          <Typography.Text strong>{node.serviceName}</Typography.Text>
          <div className="mt-1 text-xs text-slate-500">{node.platform}</div>
          <div className="mt-2 flex flex-wrap gap-1">
            <StatusTag status={node.status} />
            <Tag className="m-0">Logs: {node.logCount}</Tag>
            <Tag color={node.errorCount > 0 ? 'red' : 'default'} className="m-0">
              Errors: {node.errorCount}
            </Tag>
          </div>
        </div>
      ),
    },
    style: {
      width: 220,
      border: failed ? '2px solid #dc2626' : '1px solid #94a3b8',
      borderRadius: 8,
      background: failed ? '#fff1f2' : '#ffffff',
      boxShadow: '0 10px 20px rgba(15, 23, 42, 0.08)',
      padding: 12,
    },
  }
}

function buildEdge(edge: ServiceEdge): Edge {
  const failed = edge.status === 'FAILED' || edge.status === 'TIMEOUT'

  return {
    id: edge.id,
    source: edge.source,
    target: edge.target,
    label: `${edge.operation ?? edge.label} | ${edge.label}`,
    animated: failed,
    markerEnd: {
      type: MarkerType.ArrowClosed,
      color: failed ? '#dc2626' : '#64748b',
    },
    style: {
      stroke: failed ? '#dc2626' : '#64748b',
      strokeWidth: failed ? 3 : 2,
      strokeDasharray: edge.evidenceType === 'INFERRED' ? '6 4' : undefined,
    },
    labelStyle: {
      fill: failed ? '#991b1b' : '#334155',
      fontWeight: 700,
    },
    labelBgStyle: {
      fill: failed ? '#fff1f2' : '#f8fafc',
    },
  }
}

export function ServiceGraphView({ result }: ServiceGraphViewProps) {
  const openDrawer = useInvestigationUiStore((state) => state.openDrawer)
  const nodes = useMemo(
    () => result.serviceGraph.nodes.map(buildNode),
    [result.serviceGraph.nodes],
  )
  const edges = useMemo(
    () => result.serviceGraph.edges.map(buildEdge),
    [result.serviceGraph.edges],
  )

  if (nodes.length === 0) {
    return <Empty description="No service graph available." />
  }

  return (
    <div className="space-y-3">
      <Alert
        type="error"
        showIcon
        title="Failure path"
        description="payment-sapi -> hub-payment-propose-api | POST /payments/propose | Timeout / 30000ms"
      />
      <div className="h-[560px] overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          fitView
          minZoom={0.55}
          maxZoom={1.4}
          onNodeClick={(_, node) =>
            openDrawer({ type: 'SERVICE_NODE', id: node.id })
          }
          onEdgeClick={(_, edge) =>
            openDrawer({ type: 'SERVICE_EDGE', id: edge.id })
          }
        >
          <Background />
          <Controls />
        </ReactFlow>
      </div>
      <Space wrap>
        <Tag color="green">OK</Tag>
        <Tag color="red">TIMEOUT</Tag>
        <Tag color="blue">INFERRED</Tag>
      </Space>
    </div>
  )
}
