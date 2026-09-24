export interface Session {
  id: string
  name: string
  startedAt: string
  endedAt: string | null
  status: string
}

export interface TelemetryReading {
  eventId: string
  sessionId: string
  vehicleId: string
  recordedAt: string
  receivedAt: string
  sequenceNumber: number
  latitude: number
  longitude: number
  speedKph: number
  batteryPercent: number
  motorTemperatureCelsius: number
  offsetMillis: number
}

export interface Alert {
  id: string
  eventId: string
  sessionId: string
  vehicleId: string
  alertType: string
  message: string
  triggeredAt: string
}