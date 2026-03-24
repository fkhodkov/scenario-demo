import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { scenarioApi, Scenario } from '../api/index'
import { Plus, Play, Pause, Edit3, Zap, Clock, CheckCircle, Archive, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'

function StatusBadge({ status }: { status: Scenario['status'] }) {
  const map = {
    DRAFT:    'bg-slate-700 text-slate-300',
    ACTIVE:   'bg-green-500/20 text-green-400 border border-green-500/30',
    PAUSED:   'bg-yellow-500/20 text-yellow-400 border border-yellow-500/30',
    ARCHIVED: 'bg-slate-800 text-slate-500',
  }
  return <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${map[status]}`}>{status}</span>
}

export function ScenarioListPage() {
  const [scenarios, setScenarios] = useState<Scenario[]>([])
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  const load = () => {
    setLoading(true)
    scenarioApi.list()
      .then(setScenarios)
      .catch(() => toast.error('Could not load scenarios'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const activate = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    await scenarioApi.activate(id)
    toast.success('Scenario activated')
    load()
  }

  const pause = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    await scenarioApi.pause(id)
    toast.success('Scenario paused')
    load()
  }

  const createNew = () => navigate('/editor/new')

  return (
    <div className="min-h-screen bg-[#0f1117] p-8">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-white">Scenarios</h1>
            <p className="text-slate-400 text-sm mt-1">Communication automation workflows</p>
          </div>
          <div className="flex gap-3">
            <button
              onClick={load}
              className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
            >
              <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            </button>
            <button
              onClick={createNew}
              className="flex items-center gap-2 px-4 py-2 bg-brand hover:bg-brand-dark rounded-lg text-sm font-semibold text-white transition"
            >
              <Plus size={16} /> New Scenario
            </button>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          {[
            { label: 'Total',    value: scenarios.length,                               icon: <Zap size={18} />,          color: '#6366f1' },
            { label: 'Active',   value: scenarios.filter(s => s.status === 'ACTIVE').length,   icon: <CheckCircle size={18} />,   color: '#22c55e' },
            { label: 'Draft',    value: scenarios.filter(s => s.status === 'DRAFT').length,    icon: <Edit3 size={18} />,         color: '#f59e0b' },
            { label: 'Paused',   value: scenarios.filter(s => s.status === 'PAUSED').length,   icon: <Pause size={18} />,         color: '#64748b' },
          ].map(s => (
            <div key={s.label} className="bg-slate-800/60 border border-slate-700 rounded-xl p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-lg" style={{ background: `${s.color}22`, color: s.color }}>
                  {s.icon}
                </div>
                <div>
                  <p className="text-2xl font-bold text-white">{s.value}</p>
                  <p className="text-xs text-slate-500">{s.label}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* List */}
        {loading ? (
          <div className="text-center py-20 text-slate-500">
            <RefreshCw size={28} className="animate-spin mx-auto mb-3" />
            Loading scenarios…
          </div>
        ) : scenarios.length === 0 ? (
          <div className="text-center py-20 border-2 border-dashed border-slate-700 rounded-2xl">
            <Zap size={40} className="mx-auto text-slate-600 mb-4" />
            <p className="text-slate-400 text-lg font-semibold">No scenarios yet</p>
            <p className="text-slate-600 text-sm mb-6">Create your first communication workflow</p>
            <button
              onClick={createNew}
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-brand hover:bg-brand-dark rounded-xl text-sm font-semibold text-white"
            >
              <Plus size={16} /> Create Scenario
            </button>
          </div>
        ) : (
          <div className="space-y-3">
            {scenarios.map(s => (
              <div
                key={s.id}
                onClick={() => navigate(`/editor/${s.id}`)}
                className="group flex items-center gap-4 px-5 py-4 bg-slate-800/60 hover:bg-slate-800
                           border border-slate-700 hover:border-slate-500 rounded-xl cursor-pointer transition"
              >
                <div className="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0"
                     style={{ background: '#6366f122', color: '#6366f1' }}>
                  <Zap size={18} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <p className="font-semibold text-white">{s.name}</p>
                    <StatusBadge status={s.status} />
                  </div>
                  <p className="text-xs text-slate-500 truncate">{s.description || 'No description'}</p>
                  {s.triggerTopic && (
                    <p className="text-xs text-slate-600 mt-1">
                      Trigger: <code className="text-slate-500">{s.triggerTopic}</code>
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition">
                  <button
                    onClick={e => navigate(`/editor/${s.id}`) }
                    className="px-3 py-1.5 bg-slate-700 hover:bg-slate-600 rounded-lg text-xs text-slate-300 font-semibold"
                  >
                    Edit
                  </button>
                  {s.status !== 'ACTIVE' ? (
                    <button
                      onClick={e => activate(s.id, e)}
                      className="px-3 py-1.5 bg-green-500/20 hover:bg-green-500/30 border border-green-500/30 rounded-lg text-xs text-green-400 font-semibold"
                    >
                      Activate
                    </button>
                  ) : (
                    <button
                      onClick={e => pause(s.id, e)}
                      className="px-3 py-1.5 bg-yellow-500/20 hover:bg-yellow-500/30 border border-yellow-500/30 rounded-lg text-xs text-yellow-400 font-semibold"
                    >
                      Pause
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
