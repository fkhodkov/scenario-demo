import axios from 'axios'

export const api = axios.create({ baseURL: '/api' })
export const sim = axios.create({ baseURL: '/sim' })

// ── Types ────────────────────────────────────────────────────────────────────

export interface EventSpec {
  eventType: string
  topic: string
  description: string
}

export interface Scenario {
  id: string
  name: string
  description: string
  status: 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'ARCHIVED'
  definition: string
  triggerTopic: string
  triggerEventType: string
  createdAt: string
  updatedAt: string
}

export interface Execution {
  id: string
  userId: string
  workflowId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  currentNodeId: string
  startedAt: string
  finishedAt?: string
}

export interface OutboundMessage {
  id: string
  channel: string
  userId: string
  messageId: string
  templateId: string
  timestamp: string
  status: 'PENDING' | 'DELIVERED' | 'FAILED'
  rawPayload: string
}

// ── Scenario API ────────────────────────────────────────────────────────────

export const scenarioApi = {
  list:     ()             => api.get<Scenario[]>('/scenarios').then(r => r.data),
  get:      (id: string)   => api.get<Scenario>(`/scenarios/${id}`).then(r => r.data),
  create:   (body: object) => api.post<Scenario>('/scenarios', body).then(r => r.data),
  update:   (id: string, body: object) => api.put<Scenario>(`/scenarios/${id}`, body).then(r => r.data),
  activate: (id: string)   => api.post<Scenario>(`/scenarios/${id}/activate`).then(r => r.data),
  pause:    (id: string)   => api.post<Scenario>(`/scenarios/${id}/pause`).then(r => r.data),
  executions: (id: string) => api.get<Execution[]>(`/scenarios/${id}/executions`).then(r => r.data),
  execute:  (id: string, userId: string) =>
    api.post<Execution>(`/scenarios/${id}/execute`, { userId }).then(r => r.data),
}

export const eventApi = {
  list: () => api.get<EventSpec[]>('/events').then(r => r.data),
}

// ── Simulator API ────────────────────────────────────────────────────────────

export const simulatorApi = {
  catalog:  ()             => sim.get<EventSpec[]>('/events').then(r => r.data),
  publish:  (body: object) => sim.post('/publish', body).then(r => r.data),
  outbound: ()             => sim.get<OutboundMessage[]>('/outbound').then(r => r.data),
  log:      ()             => sim.get<string[]>('/log').then(r => r.data),
  deliver:  (msgId: string) => sim.post(`/outbound/${msgId}/deliver`).then(r => r.data),
  fail:     (msgId: string) => sim.post(`/outbound/${msgId}/fail`).then(r => r.data),
}
