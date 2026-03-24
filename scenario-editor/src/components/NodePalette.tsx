import React from 'react'
import { NodeType, useEditorStore } from '../store/editorStore'
import { Zap, Mail, Bell, MessageSquare, Clock, Timer, GitBranch, Square } from 'lucide-react'

const PALETTE: { type: NodeType; label: string; icon: React.ReactNode; accent: string; description: string }[] = [
  { type: 'trigger',   label: 'Trigger',     icon: <Zap size={14} />,           accent: '#f59e0b', description: 'Start on event' },
  { type: 'sendEmail', label: 'Send Email',  icon: <Mail size={14} />,           accent: '#3b82f6', description: 'Email communication' },
  { type: 'sendPush',  label: 'Send Push',   icon: <Bell size={14} />,           accent: '#8b5cf6', description: 'Push notification' },
  { type: 'sendSms',   label: 'Send SMS',    icon: <MessageSquare size={14} />,  accent: '#06b6d4', description: 'SMS message' },
  { type: 'waitEvent', label: 'Wait Event',  icon: <Clock size={14} />,          accent: '#f97316', description: 'Block until event/timeout' },
  { type: 'delay',     label: 'Delay',       icon: <Timer size={14} />,          accent: '#64748b', description: 'Fixed duration wait' },
  { type: 'condition', label: 'Condition',   icon: <GitBranch size={14} />,      accent: '#ec4899', description: 'Branch on field value' },
  { type: 'end',       label: 'End',         icon: <Square size={14} />,         accent: '#475569', description: 'Terminal node' },
]

export function NodePalette() {
  const { addNode } = useEditorStore()

  const onDragStart = (e: React.DragEvent, type: NodeType) => {
    e.dataTransfer.setData('nodeType', type)
    e.dataTransfer.effectAllowed = 'copy'
  }

  return (
    <div className="w-52 border-r border-slate-700 bg-slate-900 flex flex-col">
      <div className="px-4 py-3 border-b border-slate-700">
        <p className="text-xs uppercase tracking-wider text-slate-500 font-semibold">Node Palette</p>
        <p className="text-xs text-slate-600 mt-1">Drag onto canvas</p>
      </div>
      <div className="flex-1 overflow-y-auto p-3 space-y-1">
        {PALETTE.map(item => (
          <div
            key={item.type}
            draggable
            onDragStart={e => onDragStart(e, item.type)}
            onClick={() => addNode(item.type, { x: 250 + Math.random() * 100, y: 150 + Math.random() * 100 })}
            className="flex items-center gap-3 px-3 py-2 rounded-lg cursor-grab active:cursor-grabbing
                       hover:bg-slate-800 border border-transparent hover:border-slate-600
                       transition group select-none"
          >
            <div
              className="w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0"
              style={{ background: `${item.accent}22`, color: item.accent }}
            >
              {item.icon}
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold text-white">{item.label}</p>
              <p className="text-[10px] text-slate-500 leading-tight">{item.description}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="px-4 py-3 border-t border-slate-700">
        <p className="text-[10px] text-slate-600 leading-relaxed">
          📌 Connect <span className="text-green-400">delivered</span> /&nbsp;
          <span className="text-red-400">failed</span> handles for delivery branching.
          <br />
          ⏱ <span className="text-orange-400">timeout</span> handle for fallback paths.
        </p>
      </div>
    </div>
  )
}
