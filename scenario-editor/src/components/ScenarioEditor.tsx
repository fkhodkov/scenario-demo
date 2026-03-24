import React, { useCallback } from 'react'
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  BackgroundVariant,
  ReactFlowProvider,
  useReactFlow,
  Node,
} from 'reactflow'
import { useEditorStore, NodeType, NodeData } from '../store/editorStore'
import { nodeTypes } from './FlowNodes'
import { NodePalette } from './NodePalette'
import { NodePropertiesPanel } from './NodePropertiesPanel'

import 'reactflow/dist/style.css'

function FlowCanvas() {
  const {
    nodes, edges,
    onNodesChange, onEdgesChange, onConnect,
    selectNode, addNode,
  } = useEditorStore()

  const { screenToFlowPosition } = useReactFlow()

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node<NodeData>) => {
    selectNode(node)
  }, [selectNode])

  const onPaneClick = useCallback(() => {
    selectNode(null)
  }, [selectNode])

  const onDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    const type = e.dataTransfer.getData('nodeType') as NodeType
    if (!type) return
    const pos = screenToFlowPosition({ x: e.clientX, y: e.clientY })
    addNode(type, pos)
  }, [screenToFlowPosition, addNode])

  const onDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      onNodeClick={onNodeClick}
      onPaneClick={onPaneClick}
      onDrop={onDrop}
      onDragOver={onDragOver}
      fitView
      deleteKeyCode="Delete"
      className="bg-[#0d0d1a]"
    >
      <Background
        variant={BackgroundVariant.Dots}
        gap={20}
        size={1}
        color="#1e293b"
      />
      <Controls />
      <MiniMap
        nodeColor={(n) => {
          const colorMap: Record<string, string> = {
            trigger: '#f59e0b', sendEmail: '#3b82f6', sendPush: '#8b5cf6',
            sendSms: '#06b6d4', waitEvent: '#f97316', delay: '#64748b',
            condition: '#ec4899', end: '#475569',
          }
          return colorMap[n.type!] ?? '#334155'
        }}
        maskColor="#0d0d1a88"
      />
    </ReactFlow>
  )
}

export function ScenarioEditor() {
  return (
    <div className="flex h-full">
      <NodePalette />
      <div className="flex-1 relative">
        <ReactFlowProvider>
          <FlowCanvas />
        </ReactFlowProvider>
      </div>
      <NodePropertiesPanel />
    </div>
  )
}
