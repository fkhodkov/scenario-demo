import { create } from 'zustand'
import {
  addEdge,
  applyNodeChanges,
  applyEdgeChanges,
  Node,
  Edge,
  NodeChange,
  EdgeChange,
  Connection,
} from 'reactflow'
import { EventSpec } from '../api/index'

export type NodeType =
  | 'trigger' | 'sendEmail' | 'sendPush' | 'sendSms'
  | 'waitEvent' | 'delay' | 'condition' | 'end'

export interface NodeData {
  label: string
  eventType?: string
  topic?: string
  timeoutSeconds?: number
  templateId?: string
  channel?: string
  conditionField?: string
  conditionValue?: string
  delaySeconds?: number
}

interface EditorState {
  nodes: Node<NodeData>[]
  edges: Edge[]
  selectedNode: Node<NodeData> | null
  eventCatalog: EventSpec[]

  // actions
  setNodes: (nodes: Node<NodeData>[]) => void
  setEdges: (edges: Edge[]) => void
  onNodesChange: (changes: NodeChange[]) => void
  onEdgesChange: (changes: EdgeChange[]) => void
  onConnect: (connection: Connection) => void
  selectNode: (node: Node<NodeData> | null) => void
  updateNodeData: (id: string, data: Partial<NodeData>) => void
  addNode: (type: NodeType, position: { x: number; y: number }) => void
  loadGraph: (definition: string) => void
  exportGraph: () => string
  setEventCatalog: (catalog: EventSpec[]) => void
  resetGraph: () => void
}

let nodeId = 1
const newId = () => `node_${nodeId++}`

export const useEditorStore = create<EditorState>((set, get) => ({
  nodes: [],
  edges: [],
  selectedNode: null,
  eventCatalog: [],

  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),

  onNodesChange: (changes) =>
    set((s) => ({ nodes: applyNodeChanges(changes, s.nodes) as Node<NodeData>[] })),

  onEdgesChange: (changes) =>
    set((s) => ({ edges: applyEdgeChanges(changes, s.edges) })),

  onConnect: (connection) =>
    set((s) => ({
      edges: addEdge(
        {
          ...connection,
          animated: true,
          style: { stroke: '#6366f1', strokeWidth: 2 },
          data: { edgeType: 'DEFAULT', label: '' },
        },
        s.edges
      ),
    })),

  selectNode: (node) => set({ selectedNode: node }),

  updateNodeData: (id, data) =>
    set((s) => ({
      nodes: s.nodes.map((n) =>
        n.id === id ? { ...n, data: { ...n.data, ...data } } : n
      ),
      selectedNode:
        s.selectedNode?.id === id
          ? { ...s.selectedNode, data: { ...s.selectedNode.data, ...data } }
          : s.selectedNode,
    })),

  addNode: (type, position) => {
    const id = newId()
    const defaults: Record<NodeType, NodeData> = {
      trigger:   { label: 'Trigger', eventType: '', topic: '' },
      sendEmail: { label: 'Send Email', templateId: 'welcome', channel: 'email' },
      sendPush:  { label: 'Send Push', templateId: 'push_01', channel: 'push' },
      sendSms:   { label: 'Send SMS',  templateId: 'sms_01',  channel: 'sms' },
      waitEvent: { label: 'Wait for Event', eventType: '', timeoutSeconds: 3600 },
      delay:     { label: 'Delay', delaySeconds: 3600 },
      condition: { label: 'Condition', conditionField: 'channel', conditionValue: 'email' },
      end:       { label: 'End' },
    }
    const node: Node<NodeData> = {
      id,
      type,
      position,
      data: defaults[type],
    }
    set((s) => ({ nodes: [...s.nodes, node] }))
  },

  loadGraph: (definition) => {
    try {
      const graph = JSON.parse(definition)
      // Map backend node types (UPPER_SNAKE) → camelCase for React Flow
      const typeMap: Record<string, NodeType> = {
        TRIGGER: 'trigger', SEND_EMAIL: 'sendEmail', SEND_PUSH: 'sendPush',
        SEND_SMS: 'sendSms', WAIT_EVENT: 'waitEvent', DELAY: 'delay',
        CONDITION: 'condition', END: 'end',
      }
      const nodes: Node<NodeData>[] = (graph.nodes || []).map((n: any) => ({
        ...n,
        type: typeMap[n.type] ?? n.type,
      }))
      set({ nodes, edges: graph.edges || [] })
    } catch (e) {
      console.error('Failed to load graph', e)
    }
  },

  exportGraph: () => {
    const { nodes, edges } = get()
    const typeMap: Record<string, string> = {
      trigger: 'TRIGGER', sendEmail: 'SEND_EMAIL', sendPush: 'SEND_PUSH',
      sendSms: 'SEND_SMS', waitEvent: 'WAIT_EVENT', delay: 'DELAY',
      condition: 'CONDITION', end: 'END',
    }
    const exportNodes = nodes.map((n) => ({
      ...n,
      type: typeMap[n.type!] ?? n.type,
    }))
    return JSON.stringify({ nodes: exportNodes, edges })
  },

  setEventCatalog: (catalog) => set({ eventCatalog: catalog }),

  resetGraph: () => set({ nodes: [], edges: [], selectedNode: null }),
}))
