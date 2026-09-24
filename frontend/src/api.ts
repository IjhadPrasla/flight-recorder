import type { Alert, Session, TelemetryReading } from './types'

async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`)
  }

  return response.json() as Promise<T>
}

export function getSessions(): Promise<Session[]> {
  return getJson<Session[]>('/api/sessions')
}

export function getTelemetry(
  sessionId: string,
  vehicleId?: string,
): Promise<TelemetryReading[]> {
  const params = new URLSearchParams({
    limit: '1000',
  })

  if (vehicleId) {
    params.set('vehicleId', vehicleId)
  }

  return getJson<TelemetryReading[]>(
    `/api/sessions/${encodeURIComponent(sessionId)}/telemetry/replay?${params}`,
  )
}

export function getAlerts(sessionId: string): Promise<Alert[]> {
  return getJson<Alert[]>(
    `/api/sessions/${encodeURIComponent(sessionId)}/alerts?limit=100`,
  )
}