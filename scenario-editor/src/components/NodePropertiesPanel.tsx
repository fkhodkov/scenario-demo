import React from 'react'
import { useEditorStore } from '../store/editorStore'
import { X } from 'lucide-react'

const Label = ({ children }: { children: React.ReactNode }) => (
  <label className="block text-xs font-medium text-slate-400 mb-1">{children}</label>
)

const Input = (props: React.InputHTMLAttributes<HTMLInputElement>) => (
  <input
    {...props}
    className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white
               focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand transition"
  />
)

const Select = (props: React.SelectHTMLAttributes<HTMLSelectElement>) => (
  <select
    {...props}
    className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-sm text-white
               focus:outline-none focus:border-brand transition"
  />
)

const Field = ({ label, children }: { label: string; children: React.ReactNode }) => (
  <div className="mb-4">
    <Label>{label}</Label>
    {children}
  </div>
)

export function NodePropertiesPanel() {
  const { selectedNode, updateNodeData, selectNode, eventCatalog } = useEditorStore()

  if (!selectedNode) {
    return (
      <div className="w-72 border-l border-slate-700 bg-slate-900 flex items-center justify-center">
        <p className="text-slate-500 text-sm text-center px-4">
          Click a node to edit its properties
        </p>
      </div>
    )
  }

  const d = selectedNode.data
  const update = (patch: Partial<typeof d>) => updateNodeData(selectedNode.id, patch)

  return (
    <div className="w-72 border-l border-slate-700 bg-slate-900 flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700">
        <div>
          <p className="text-xs text-slate-500 uppercase tracking-wider">Properties</p>
          <p className="text-sm font-semibold text-white capitalize">
            {selectedNode.type?.replace(/([A-Z])/g, ' $1')}
          </p>
        </div>
        <button
          onClick={() => selectNode(null)}
          className="p-1 rounded-lg hover:bg-slate-700 text-slate-400 hover:text-white transition"
        >
          <X size={14} />
        </button>
      </div>

      {/* Fields */}
      <div className="flex-1 overflow-y-auto p-4">
        <Field label="Label">
          <Input value={d.label || ''} onChange={e => update({ label: e.target.value })} />
        </Field>

        {/* TRIGGER */}
        {selectedNode.type === 'trigger' && (
          <>
            <Field label="Trigger Event">
              <Select value={d.eventType || ''} onChange={e => {
                const spec = eventCatalog.find(ev => ev.eventType === e.target.value)
                update({ eventType: e.target.value, topic: spec?.topic || d.topic })
              }}>
                <option value="">— Select event —</option>
                {eventCatalog.map(ev => (
                  <option key={ev.eventType} value={ev.eventType}>{ev.eventType}</option>
                ))}
              </Select>
            </Field>
            <Field label="Kafka Topic">
              <Input value={d.topic || ''} onChange={e => update({ topic: e.target.value })}
                     placeholder="e.g. user.registered" />
            </Field>
          </>
        )}

        {/* SEND_* nodes */}
        {(selectedNode.type === 'sendEmail' || selectedNode.type === 'sendPush' || selectedNode.type === 'sendSms') && (
          <>
            <Field label="Template ID">
              <Input value={d.templateId || ''} onChange={e => update({ templateId: e.target.value })}
                     placeholder="e.g. welcome_email" />
            </Field>
            <div className="p-3 rounded-lg bg-slate-800 border border-slate-700 text-xs text-slate-400">
              <p className="font-semibold text-slate-300 mb-1">Delivery branches</p>
              <p className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
                <strong>delivered</strong> — confirmation received
              </p>
              <p className="flex items-center gap-2 mt-1">
                <span className="w-2 h-2 rounded-full bg-red-500 inline-block" />
                <strong>failed</strong> — bounce, unsubscribed, or timeout
              </p>
            </div>
          </>
        )}

        {/* WAIT_EVENT */}
        {selectedNode.type === 'waitEvent' && (
          <>
            <Field label="Expected Event">
              <Select value={d.eventType || ''} onChange={e => {
                const spec = eventCatalog.find(ev => ev.eventType === e.target.value)
                update({ eventType: e.target.value, topic: spec?.topic })
              }}>
                <option value="">— Any event —</option>
                {eventCatalog.map(ev => (
                  <option key={ev.eventType} value={ev.eventType}>{ev.eventType}</option>
                ))}
              </Select>
            </Field>
            <Field label="Timeout (seconds)">
              <Input type="number" value={d.timeoutSeconds ?? 3600}
                     onChange={e => update({ timeoutSeconds: Number(e.target.value) })} />
            </Field>
            <div className="p-3 rounded-lg bg-slate-800 border border-slate-700 text-xs text-slate-400">
              <p className="font-semibold text-slate-300 mb-1">Output handles</p>
              <p><span className="text-orange-400 font-semibold">event</span> — event received</p>
              <p className="mt-1"><span className="text-slate-400 font-semibold">timeout</span> — timer expired</p>
            </div>
          </>
        )}

        {/* DELAY */}
        {selectedNode.type === 'delay' && (
          <Field label="Delay (seconds)">
            <Input type="number" value={d.delaySeconds ?? 3600}
                   onChange={e => update({ delaySeconds: Number(e.target.value) })} />
            <p className="text-xs text-slate-500 mt-1">
              {(() => {
                const s = d.delaySeconds ?? 3600
                if (s >= 86400) return `≈ ${(s / 86400).toFixed(1)} days`
                if (s >= 3600)  return `≈ ${(s / 3600).toFixed(1)} hours`
                return `${s} seconds`
              })()}
            </p>
          </Field>
        )}

        {/* CONDITION */}
        {selectedNode.type === 'condition' && (
          <>
            <Field label="Field to check">
              <Input value={d.conditionField || ''} onChange={e => update({ conditionField: e.target.value })}
                     placeholder="e.g. channel" />
            </Field>
            <Field label="Expected value">
              <Input value={d.conditionValue || ''} onChange={e => update({ conditionValue: e.target.value })}
                     placeholder="e.g. email" />
            </Field>
            <div className="p-3 rounded-lg bg-slate-800 border border-slate-700 text-xs text-slate-400">
              <p><span className="text-green-400 font-semibold">true</span> — condition matches</p>
              <p className="mt-1"><span className="text-red-400 font-semibold">false</span> — condition doesn't match</p>
            </div>
          </>
        )}

        {/* Node ID (read-only) */}
        <div className="mt-4 pt-4 border-t border-slate-700">
          <p className="text-xs text-slate-600">Node ID: <code className="text-slate-500">{selectedNode.id}</code></p>
        </div>
      </div>
    </div>
  )
}
