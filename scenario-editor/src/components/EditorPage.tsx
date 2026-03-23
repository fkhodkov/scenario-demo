import React, { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { scenarioApi, eventApi } from '../api'
import { useEditorStore } from '../store/editorStore'
import { ScenarioEditor } from './ScenarioEditor'
import { SimulatorPanel } from './simulator/SimulatorPanel'
import {
  ArrowLeft, Save, Play, Pause, Zap, FlaskConical,
  ChevronRight, RefreshCw, Users,
} from 'lucide-react'
import toast from 'react-hot-toast'

const STARTER_GRAPH = JSON.stringify({
  nodes: [
    {
      id: 'node_1',
      type: 'TRIGGER',
      position: { x: 80, y: 200 },
      data: { label: 'User Registered', eventType: 'USER_REGISTERED', topic: 'user.registered' },
    },
    {
      id: 'node_2',
      type: 'SEND_EMAIL',
      position: { x: 340, y: 200 },
      data: { label: 'Send Welcome Email', templateId: 'welcome_email', channel: 'email' },
    },
    {
      id: 'node_3',
      type: 'WAIT_EVENT',
      position: { x: 620, y: 110 },
      data: { label: 'Wait: Email Opened', eventType: 'EMAIL_OPENED', timeoutSeconds: 86400 },
    },
    {
      id: 'node_4',
      type: 'SEND_EMAIL',
      position: { x: 900, y: 80 },
      data: { label: 'Send Follow-up', templateId: 'followup_email', channel: 'email' },
    },
    {
      id: 'node_5',
      type: 'SEND_PUSH',
      position: { x: 900, y: 240 },
      data: { label: 'Send Push Reminder', templateId: 'reminder_push', channel: 'push' },
    },
    {
      id: 'node_6',
      type: 'END',
      position: { x: 1160, y: 160 },
      data: { label: 'End' },
    },
    {
      id: 'node_7',
      type: 'END',
      position: { x: 620, y: 340 },
      data: { label: 'End (failed)' },
    },
  ],
  edges: [
    { id: 'e1', source: 'node_1', target: 'node_2', sourceHandle: null, animated: true, style: { stroke: '#6366f1', strokeWidth: 2 } },
    { id: 'e2', source: 'node_2', target: 'node_3', sourceHandle: 'delivered', animated: true, style: { stroke: '#22c55e', strokeWidth: 2 }, data: { label: 'delivered' } },
    { id: 'e3', source: 'node_2', target: 'node_7', sourceHandle: 'failed',    animated: true, style: { stroke: '#ef4444', strokeWidth: 2 }, data: { label: 'failed' } },
    { id: 'e4', source: 'node_3', target: 'node_4', sourceHandle: 'event',     animated: true, style: { stroke: '#f97316', strokeWidth: 2 }, data: { label: 'opened' } },
    { id: 'e5', source: 'node_3', target: 'node_5', sourceHandle: 'timeout',   animated: true, style: { stroke: '#94a3b8', strokeWidth: 2 }, data: { label: 'timeout' } },
    { id: 'e6', source: 'node_4', target: 'node_6', sourceHandle: null,        animated: true, style: { stroke: '#6366f1', strokeWidth: 2 } },
    { id: 'e7', source: 'node_5', target: 'node_6', sourceHandle: 'delivered', animated: true, style: { stroke: '#22c55e', strokeWidth: 2 } },
  ],
})

export function EditorPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const isNew = id === 'new'

  const { loadGraph, exportGraph, resetGraph, setEventCatalog } = useEditorStore()

  const [name, setName]         = useState('New Scenario')
  const [desc, setDesc]         = useState('')
  const [status, setStatus]     = useState<string>('DRAFT')
  const [saving, setSaving]     = useState(false)
  const [showSim, setShowSim]   = useState(false)
  const [execCount, setExecCount] = useState(0)

  useEffect(() => {
    // Load event catalog
    eventApi.list().then(setEventCatalog).catch(() => {})

    if (isNew) {
      resetGraph()
      loadGraph(STARTER_GRAPH)
      return
    }
    scenarioApi.get(id!).then(s => {
      setName(s.name)
      setDesc(s.description || '')
      setStatus(s.status)
      loadGraph(s.definition)
    }).catch(() => toast.error('Failed to load scenario'))

    scenarioApi.executions(id!).then(exs => setExecCount(exs.length)).catch(() => {})
  }, [id])

  const save = async () => {
    setSaving(true)
    try {
      const definition = exportGraph()
      if (isNew) {
        const s = await scenarioApi.create({ name, description: desc, definition })
        toast.success('Scenario created')
        navigate(`/editor/${s.id}`, { replace: true })
      } else {
        await scenarioApi.update(id!, { name, description: desc, definition })
        toast.success('Saved')
      }
    } catch (e: any) {
      toast.error(e.response?.data?.message ?? 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const toggleActive = async () => {
    if (isNew) { toast.error('Save first'); return }
    try {
      if (status === 'ACTIVE') {
        await scenarioApi.pause(id!)
        setStatus('PAUSED')
        toast.success('Paused')
      } else {
        await scenarioApi.activate(id!)
        setStatus('ACTIVE')
        toast.success('Activated — scenario is now listening for events')
      }
    } catch (e: any) {
      toast.error(e.message)
    }
  }

  return (
    <div className="flex flex-col h-screen bg-[#0f1117]">
      {/* Topbar */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-slate-700 bg-slate-900 flex-shrink-0">
        <button
          onClick={() => navigate('/')}
          className="p-1.5 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-white transition"
        >
          <ArrowLeft size={16} />
        </button>
        <ChevronRight size={14} className="text-slate-600" />

        {/* Name */}
        <input
          value={name}
          onChange={e => setName(e.target.value)}
          className="bg-transparent border-none text-white font-semibold text-sm focus:outline-none
                     hover:bg-slate-800 focus:bg-slate-800 px-2 py-1 rounded-lg transition w-48"
        />
        <input
          value={desc}
          onChange={e => setDesc(e.target.value)}
          placeholder="Description…"
          className="bg-transparent border-none text-slate-400 text-xs focus:outline-none
                     hover:bg-slate-800 focus:bg-slate-800 px-2 py-1 rounded-lg transition w-64"
        />

        {/* Status badge */}
        <div className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
          status === 'ACTIVE'  ? 'bg-green-500/20 text-green-400 border-green-500/30' :
          status === 'PAUSED'  ? 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30' :
          'bg-slate-700 text-slate-400 border-slate-600'
        }`}>
          {status}
        </div>

        <div className="flex-1" />

        {/* Executions count */}
        {!isNew && (
          <div className="flex items-center gap-1.5 text-xs text-slate-500 mr-2">
            <Users size={13} />
            <span>{execCount} runs</span>
          </div>
        )}

        {/* Actions */}
        <button
          onClick={() => setShowSim(v => !v)}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            showSim
              ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
              : 'bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white border border-slate-700'
          }`}
        >
          <FlaskConical size={13} />
          Simulator
        </button>

        <button
          onClick={save}
          disabled={saving}
          className="flex items-center gap-2 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700
                     rounded-lg text-xs font-semibold text-white transition disabled:opacity-40"
        >
          {saving ? <RefreshCw size={13} className="animate-spin" /> : <Save size={13} />}
          Save
        </button>

        <button
          onClick={toggleActive}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
            status === 'ACTIVE'
              ? 'bg-yellow-500/20 hover:bg-yellow-500/30 text-yellow-400 border border-yellow-500/30'
              : 'bg-green-500/20 hover:bg-green-500/30 text-green-400 border border-green-500/30'
          }`}
        >
          {status === 'ACTIVE' ? <><Pause size={13} /> Pause</> : <><Play size={13} /> Activate</>}
        </button>
      </div>

      {/* Main */}
      <div className="flex-1 flex overflow-hidden">
        <div className={`flex-1 overflow-hidden ${showSim ? 'w-0' : ''}`}>
          <ScenarioEditor />
        </div>
        {showSim && (
          <div className="w-[480px] flex-shrink-0 border-l border-slate-700 overflow-hidden">
            <SimulatorPanel />
          </div>
        )}
      </div>
    </div>
  )
}
