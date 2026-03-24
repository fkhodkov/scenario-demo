import React from 'react'
import { Handle, Position, NodeProps } from 'reactflow'
import { NodeData } from '../store/editorStore'
import {
  Zap, Mail, Bell, MessageSquare, Clock, GitBranch,
  Timer, Square, CheckCircle, XCircle,
} from 'lucide-react'

// ── Shared wrapper ────────────────────────────────────────────────────────────

interface WrapperProps {
  selected?: boolean
  accent: string
  icon: React.ReactNode
  title: string
  subtitle?: string
  children?: React.ReactNode
  handles?: React.ReactNode
}

function NodeWrapper({ selected, accent, icon, title, subtitle, children, handles }: WrapperProps) {
  return (
    <div
      className="relative rounded-xl shadow-2xl min-w-[180px] max-w-[220px]"
      style={{
        background: '#1a1a2e',
        border: `2px solid ${selected ? accent : '#334155'}`,
        boxShadow: selected ? `0 0 0 3px ${accent}33` : '0 4px 24px #0008',
        transition: 'border-color 0.15s, box-shadow 0.15s',
      }}
    >
      {/* Header */}
      <div
        className="flex items-center gap-2 px-3 py-2 rounded-t-xl"
        style={{ background: `${accent}22`, borderBottom: `1px solid ${accent}44` }}
      >
        <span style={{ color: accent }}>{icon}</span>
        <span className="text-xs font-semibold text-white truncate">{title}</span>
      </div>
      {/* Body */}
      {(subtitle || children) && (
        <div className="px-3 py-2 text-xs text-slate-400">
          {subtitle && <div className="truncate">{subtitle}</div>}
          {children}
        </div>
      )}
      {handles}
    </div>
  )
}

// Helper handles
const InLeft  = ({ id = 'in' }: { id?: string }) =>
  <Handle type="target" position={Position.Left} id={id} />
const OutRight = ({ id = 'out' }: { id?: string }) =>
  <Handle type="source" position={Position.Right} id={id} />

function LabeledHandle({
  type, position, id, label, color, offsetY = 0,
}: {
  type: 'source' | 'target'
  position: Position
  id: string
  label: string
  color: string
  offsetY?: number
}) {
  const isRight = position === Position.Right
  return (
    <div
      style={{
        position: 'absolute',
        right: isRight ? -6 : undefined,
        left: !isRight ? -6 : undefined,
        top: `calc(50% + ${offsetY}px)`,
        transform: 'translateY(-50%)',
        display: 'flex',
        alignItems: 'center',
        gap: 4,
        flexDirection: isRight ? 'row-reverse' : 'row',
      }}
    >
      <Handle
        type={type}
        position={position}
        id={id}
        style={{
          position: 'relative',
          transform: 'none',
          top: 'auto',
          right: 'auto',
          left: 'auto',
          borderColor: color,
          background: '#1a1a2e',
        }}
      />
      <span style={{ fontSize: 9, color, fontWeight: 600, letterSpacing: '0.05em' }}>
        {label}
      </span>
    </div>
  )
}

// ── Individual node types ─────────────────────────────────────────────────────

export function TriggerNode({ data, selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#f59e0b"
      icon={<Zap size={13} />}
      title="Trigger"
      subtitle={data.eventType || 'Select event…'}
      handles={<OutRight />}
    />
  )
}

export function SendEmailNode({ data, selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#3b82f6"
      icon={<Mail size={13} />}
      title="Send Email"
      subtitle={`Template: ${data.templateId || 'none'}`}
      handles={
        <>
          <InLeft />
          <LabeledHandle type="source" position={Position.Right} id="delivered" label="✓ delivered" color="#22c55e" offsetY={-14} />
          <LabeledHandle type="source" position={Position.Right} id="failed"    label="✗ failed"    color="#ef4444" offsetY={14} />
        </>
      }
    />
  )
}

export function SendPushNode({ data, selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#8b5cf6"
      icon={<Bell size={13} />}
      title="Send Push"
      subtitle={`Template: ${data.templateId || 'none'}`}
      handles={
        <>
          <InLeft />
          <LabeledHandle type="source" position={Position.Right} id="delivered" label="✓ delivered" color="#22c55e" offsetY={-14} />
          <LabeledHandle type="source" position={Position.Right} id="failed"    label="✗ failed"    color="#ef4444" offsetY={14} />
        </>
      }
    />
  )
}

export function SendSmsNode({ data, selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#06b6d4"
      icon={<MessageSquare size={13} />}
      title="Send SMS"
      subtitle={`Template: ${data.templateId || 'none'}`}
      handles={
        <>
          <InLeft />
          <LabeledHandle type="source" position={Position.Right} id="delivered" label="✓ delivered" color="#22c55e" offsetY={-14} />
          <LabeledHandle type="source" position={Position.Right} id="failed"    label="✗ failed"    color="#ef4444" offsetY={14} />
        </>
      }
    />
  )
}

export function WaitEventNode({ data, selected }: NodeProps<NodeData>) {
  const timeout = data.timeoutSeconds
    ? data.timeoutSeconds >= 3600
      ? `${(data.timeoutSeconds / 3600).toFixed(1)}h`
      : `${data.timeoutSeconds}s`
    : '1h'
  return (
    <NodeWrapper
      selected={selected}
      accent="#f97316"
      icon={<Clock size={13} />}
      title="Wait for Event"
      subtitle={data.eventType || 'Any event'}
      handles={
        <>
          <InLeft />
          <LabeledHandle type="source" position={Position.Right} id="event"   label="event"   color="#f97316" offsetY={-14} />
          <LabeledHandle type="source" position={Position.Right} id="timeout" label={`⏱ ${timeout}`} color="#94a3b8" offsetY={14} />
        </>
      }
    />
  )
}

export function DelayNode({ data, selected }: NodeProps<NodeData>) {
  const d = data.delaySeconds ?? 3600
  const label = d >= 86400 ? `${(d / 86400).toFixed(1)}d`
    : d >= 3600 ? `${(d / 3600).toFixed(1)}h`
    : `${d}s`
  return (
    <NodeWrapper
      selected={selected}
      accent="#64748b"
      icon={<Timer size={13} />}
      title="Delay"
      subtitle={label}
      handles={<><InLeft /><OutRight /></>}
    />
  )
}

export function ConditionNode({ data, selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#ec4899"
      icon={<GitBranch size={13} />}
      title="Condition"
      subtitle={data.conditionField ? `${data.conditionField} = ${data.conditionValue}` : 'Define…'}
      handles={
        <>
          <InLeft />
          <LabeledHandle type="source" position={Position.Right} id="true"  label="true"  color="#22c55e" offsetY={-14} />
          <LabeledHandle type="source" position={Position.Right} id="false" label="false" color="#ef4444" offsetY={14} />
        </>
      }
    />
  )
}

export function EndNode({ selected }: NodeProps<NodeData>) {
  return (
    <NodeWrapper
      selected={selected}
      accent="#475569"
      icon={<Square size={13} />}
      title="End"
      handles={<InLeft />}
    />
  )
}

// ── Node type map (passed to ReactFlow) ──────────────────────────────────────

export const nodeTypes = {
  trigger:   TriggerNode,
  sendEmail:  SendEmailNode,
  sendPush:   SendPushNode,
  sendSms:    SendSmsNode,
  waitEvent:  WaitEventNode,
  delay:      DelayNode,
  condition:  ConditionNode,
  end:        EndNode,
}
