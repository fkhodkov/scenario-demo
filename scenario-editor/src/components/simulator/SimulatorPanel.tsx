import React, { useState, useEffect, useCallback } from 'react'
import { simulatorApi, EventSpec, OutboundMessage } from '../../api'
import { Send, RefreshCw, CheckCircle, XCircle, Clock, Zap, Inbox } from 'lucide-react'
import toast from 'react-hot-toast'

function Badge({ status }: { status: string }) {
  const map: Record<string, string> = {
    PENDING:   'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
    DELIVERED: 'bg-green-500/20 text-green-400 border-green-500/30',
    FAILED:    'bg-red-500/20 text-red-400 border-red-500/30',
    RUNNING:   'bg-blue-500/20 text-blue-400 border-blue-500/30',
    COMPLETED: 'bg-green-500/20 text-green-400 border-green-500/30',
  }
  return (
    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${map[status] ?? 'bg-slate-700 text-slate-400 border-slate-600'}`}>
      {status}
    </span>
  )
}

export function SimulatorPanel() {
  const [catalog, setCatalog]   = useState<EventSpec[]>([])
  const [outbound, setOutbound] = useState<OutboundMessage[]>([])
  const [log, setLog]           = useState<string[]>([])
  const [userId, setUserId]     = useState('user_001')
  const [eventType, setEventType] = useState('')
  const [extraJson, setExtraJson] = useState('{}')
  const [busy, setBusy]         = useState(false)
  const [tab, setTab]           = useState<'publish' | 'outbound' | 'log'>('publish')
  const [polling, setPolling]   = useState(false)

  const loadData = useCallback(async () => {
    try {
      const [ob, lg] = await Promise.all([simulatorApi.outbound(), simulatorApi.log()])
      setOutbound(ob)
      setLog(lg)
    } catch {}
  }, [])

  useEffect(() => {
    simulatorApi.catalog().then(setCatalog).catch(() => {})
    loadData()
  }, [loadData])

  useEffect(() => {
    if (!polling) return
    const id = setInterval(loadData, 2000)
    return () => clearInterval(id)
  }, [polling, loadData])

  const publish = async () => {
    if (!eventType || !userId) return
    setBusy(true)
    try {
      let extra: object = {}
      try { extra = JSON.parse(extraJson) } catch {}
      await simulatorApi.publish({ eventType, userId, extra })
      toast.success(`Published ${eventType}`)
      await loadData()
    } catch (e: any) {
      toast.error(e.message ?? 'Publish failed')
    } finally {
      setBusy(false)
    }
  }

  const ackMsg = async (msgId: string, deliver: boolean) => {
    try {
      await (deliver ? simulatorApi.deliver(msgId) : simulatorApi.fail(msgId))
      toast.success(deliver ? 'Marked as delivered' : 'Marked as failed')
      await loadData()
    } catch (e: any) {
      toast.error(e.message)
    }
  }

  return (
    <div className="flex flex-col h-full bg-slate-900">
      {/* Header */}
      <div className="px-5 py-4 border-b border-slate-700 flex items-center justify-between">
        <div>
          <h2 className="text-base font-bold text-white">Event Simulator</h2>
          <p className="text-xs text-slate-500">Publish events · ACK/NACK outbound communications</p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-xs text-slate-400 cursor-pointer">
            <input
              type="checkbox"
              checked={polling}
              onChange={e => setPolling(e.target.checked)}
              className="accent-brand"
            />
            Auto-refresh
          </label>
          <button
            onClick={loadData}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
          >
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-700 px-5">
        {(['publish', 'outbound', 'log'] as const).map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-3 text-xs font-semibold capitalize transition border-b-2 -mb-px ${
              tab === t
                ? 'text-brand border-brand'
                : 'text-slate-500 border-transparent hover:text-slate-300'
            }`}
          >
            {t === 'outbound' ? `Outbound (${outbound.filter(m => m.status === 'PENDING').length} pending)` : t}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto p-5">

        {/* ── Publish ── */}
        {tab === 'publish' && (
          <div className="space-y-4 max-w-lg">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">User ID</label>
              <input
                value={userId}
                onChange={e => setUserId(e.target.value)}
                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-brand"
                placeholder="user_001"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Event Type</label>
              <select
                value={eventType}
                onChange={e => setEventType(e.target.value)}
                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-brand"
              >
                <option value="">— Select event —</option>
                {catalog.map(ev => (
                  <option key={ev.eventType} value={ev.eventType}>
                    {ev.eventType} · {ev.topic}
                  </option>
                ))}
              </select>
              {eventType && catalog.find(e => e.eventType === eventType) && (
                <p className="text-xs text-slate-500 mt-1">
                  Topic: <code className="text-slate-400">{catalog.find(e => e.eventType === eventType)!.topic}</code>
                  &nbsp;·&nbsp;{catalog.find(e => e.eventType === eventType)!.description}
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Extra fields (JSON)</label>
              <textarea
                value={extraJson}
                onChange={e => setExtraJson(e.target.value)}
                rows={3}
                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white font-mono focus:outline-none focus:border-brand resize-none"
                placeholder='{"orderId": "ord_123"}'
              />
            </div>

            <button
              onClick={publish}
              disabled={busy || !eventType || !userId}
              className="flex items-center gap-2 px-5 py-2.5 bg-brand hover:bg-brand-dark rounded-lg text-sm font-semibold text-white
                         disabled:opacity-40 transition"
            >
              {busy ? <RefreshCw size={14} className="animate-spin" /> : <Send size={14} />}
              Publish Event
            </button>

            {/* Quick fire presets */}
            <div className="pt-4 border-t border-slate-700">
              <p className="text-xs font-semibold text-slate-400 mb-3">Quick presets</p>
              <div className="grid grid-cols-2 gap-2">
                {[
                  { label: '👤 Register user', event: 'USER_REGISTERED', extra: { email: 'test@example.com', channel: 'email' } },
                  { label: '💳 Purchase', event: 'PURCHASE_COMPLETED', extra: { orderId: 'ord_001', amount: 99.99 } },
                  { label: '📧 Email opened', event: 'EMAIL_OPENED', extra: { messageId: 'msg_001' } },
                  { label: '🔗 Link clicked', event: 'LINK_CLICKED', extra: { messageId: 'msg_001', url: 'https://example.com' } },
                  { label: '🚫 Unsubscribe', event: 'USER_UNSUBSCRIBED', extra: { channel: 'email' } },
                ].map(p => (
                  <button
                    key={p.event}
                    onClick={() => {
                      setEventType(p.event)
                      setExtraJson(JSON.stringify(p.extra, null, 2))
                    }}
                    className="text-left px-3 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 hover:border-slate-500
                               rounded-lg text-xs text-slate-300 transition"
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* ── Outbound ── */}
        {tab === 'outbound' && (
          <div className="space-y-2">
            {outbound.length === 0 && (
              <div className="flex flex-col items-center justify-center py-16 text-slate-600">
                <Inbox size={32} className="mb-3" />
                <p className="text-sm">No outbound messages yet</p>
                <p className="text-xs mt-1">Start a scenario and send communications</p>
              </div>
            )}
            {outbound.map(msg => (
              <div
                key={msg.id}
                className="flex items-center gap-3 px-4 py-3 bg-slate-800 border border-slate-700 rounded-xl"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-bold text-white uppercase">{msg.channel}</span>
                    <Badge status={msg.status} />
                  </div>
                  <p className="text-xs text-slate-400 truncate">user: {msg.userId}</p>
                  <p className="text-xs text-slate-500 truncate">msg: {msg.messageId}</p>
                  <p className="text-[10px] text-slate-600">{msg.timestamp}</p>
                </div>
                {msg.status === 'PENDING' && (
                  <div className="flex flex-col gap-1 flex-shrink-0">
                    <button
                      onClick={() => ackMsg(msg.messageId, true)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-green-500/20 hover:bg-green-500/30
                                 border border-green-500/30 rounded-lg text-xs text-green-400 font-semibold transition"
                    >
                      <CheckCircle size={12} /> Deliver
                    </button>
                    <button
                      onClick={() => ackMsg(msg.messageId, false)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/20 hover:bg-red-500/30
                                 border border-red-500/30 rounded-lg text-xs text-red-400 font-semibold transition"
                    >
                      <XCircle size={12} /> Fail
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* ── Log ── */}
        {tab === 'log' && (
          <div className="space-y-1 font-mono">
            {log.length === 0 && (
              <p className="text-slate-600 text-xs py-8 text-center">No events logged yet</p>
            )}
            {log.map((entry, i) => (
              <div key={i} className="text-[11px] text-slate-400 py-0.5 border-b border-slate-800">
                {entry}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
