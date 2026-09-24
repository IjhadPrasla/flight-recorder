import { useEffect, useMemo, useState } from 'react'
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { getAlerts, getSessions, getTelemetry } from './api'
import type {
  Alert,
  Session,
  TelemetryReading,
} from './types'
import './App.css'

function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

function formatAlertType(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(' ')
}

function App() {
  const [sessions, setSessions] = useState<Session[]>([])
  const [selectedSessionId, setSelectedSessionId] = useState('')
  const [telemetry, setTelemetry] = useState<TelemetryReading[]>([])
  const [alerts, setAlerts] = useState<Alert[]>([])
  const [vehicleFilter, setVehicleFilter] = useState('all')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadSessions() {
    try {
      setError('')
      const loadedSessions = await getSessions()
      setSessions(loadedSessions)

      if (loadedSessions.length > 0) {
        setSelectedSessionId((current) =>
          current || loadedSessions[0].id
        )
      } else {
        setLoading(false)
      }
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to load sessions',
      )
      setLoading(false)
    }
  }

  async function loadSessionData(sessionId: string) {
    try {
      setLoading(true)
      setError('')

      const [loadedTelemetry, loadedAlerts] = await Promise.all([
        getTelemetry(sessionId),
        getAlerts(sessionId),
      ])

      setTelemetry(loadedTelemetry)
      setAlerts(loadedAlerts)
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : 'Unable to load telemetry',
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadSessions()
  }, [])

  useEffect(() => {
    if (selectedSessionId) {
      setVehicleFilter('all')
      void loadSessionData(selectedSessionId)
    }
  }, [selectedSessionId])

  const selectedSession = sessions.find(
    (session) => session.id === selectedSessionId,
  )

  const vehicleIds = useMemo(
    () => [...new Set(telemetry.map((reading) => reading.vehicleId))],
    [telemetry],
  )

  const displayedTelemetry = useMemo(
    () =>
      vehicleFilter === 'all'
        ? telemetry
        : telemetry.filter(
            (reading) => reading.vehicleId === vehicleFilter,
          ),
    [telemetry, vehicleFilter],
  )

  const metrics = useMemo(() => {
    if (displayedTelemetry.length === 0) {
      return {
        averageSpeed: 0,
        minimumBattery: 0,
        maximumTemperature: 0,
      }
    }

    const totalSpeed = displayedTelemetry.reduce(
      (sum, reading) => sum + reading.speedKph,
      0,
    )

    return {
      averageSpeed: totalSpeed / displayedTelemetry.length,
      minimumBattery: Math.min(
        ...displayedTelemetry.map(
          (reading) => reading.batteryPercent,
        ),
      ),
      maximumTemperature: Math.max(
        ...displayedTelemetry.map(
          (reading) => reading.motorTemperatureCelsius,
        ),
      ),
    }
  }, [displayedTelemetry])

  const chartData = displayedTelemetry.map((reading) => ({
    time: Number((reading.offsetMillis / 1000).toFixed(2)),
    speed: Number(reading.speedKph.toFixed(1)),
    battery: Number(reading.batteryPercent.toFixed(1)),
    temperature: Number(
      reading.motorTemperatureCelsius.toFixed(1),
    ),
  }))

  const visibleAlerts =
    vehicleFilter === 'all'
      ? alerts
      : alerts.filter(
          (alert) => alert.vehicleId === vehicleFilter,
        )

  async function refreshDashboard() {
    await loadSessions()

    if (selectedSessionId) {
      await loadSessionData(selectedSessionId)
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Telemetry Operations</p>
          <h1>Flight Recorder</h1>
        </div>

        <div className="topbar-actions">
          <span className="connection-status">
            <span className="status-dot" />
            Backend connected
          </span>

          <button
            className="refresh-button"
            onClick={() => void refreshDashboard()}
            type="button"
          >
            Refresh data
          </button>
        </div>
      </header>

      <main>
        <section className="control-panel">
          <div>
            <label htmlFor="session">Recording session</label>
            <select
              id="session"
              value={selectedSessionId}
              onChange={(event) =>
                setSelectedSessionId(event.target.value)
              }
            >
              {sessions.length === 0 && (
                <option value="">No sessions available</option>
              )}

              {sessions.map((session) => (
                <option key={session.id} value={session.id}>
                  {session.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="vehicle">Vehicle</label>
            <select
              id="vehicle"
              value={vehicleFilter}
              onChange={(event) =>
                setVehicleFilter(event.target.value)
              }
              disabled={vehicleIds.length === 0}
            >
              <option value="all">All vehicles</option>

              {vehicleIds.map((vehicleId) => (
                <option key={vehicleId} value={vehicleId}>
                  {vehicleId}
                </option>
              ))}
            </select>
          </div>

          <div className="session-summary">
            <span className="summary-label">Session status</span>
            <strong>{selectedSession?.status ?? 'Unavailable'}</strong>
            <span>
              {selectedSession
                ? formatDate(selectedSession.startedAt)
                : 'No recording selected'}
            </span>
          </div>
        </section>

        {error && <div className="error-banner">{error}</div>}

        <section className="metric-grid">
          <article className="metric-card">
            <span>Telemetry readings</span>
            <strong>{displayedTelemetry.length}</strong>
            <small>{vehicleIds.length} vehicles detected</small>
          </article>

          <article className="metric-card">
            <span>Average speed</span>
            <strong>{metrics.averageSpeed.toFixed(1)} km/h</strong>
            <small>Across displayed readings</small>
          </article>

          <article className="metric-card">
            <span>Minimum battery</span>
            <strong>{metrics.minimumBattery.toFixed(1)}%</strong>
            <small>Lowest recorded charge</small>
          </article>

          <article className="metric-card">
            <span>Peak motor temperature</span>
            <strong>
              {metrics.maximumTemperature.toFixed(1)}°C
            </strong>
            <small>Highest recorded temperature</small>
          </article>
        </section>

        <section className="dashboard-grid">
          <article className="panel chart-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Replay timeline</p>
                <h2>Vehicle telemetry</h2>
              </div>

              <span>{displayedTelemetry.length} readings</span>
            </div>

            <div className="chart-container">
              {loading ? (
                <div className="empty-state">Loading telemetry…</div>
              ) : chartData.length === 0 ? (
                <div className="empty-state">
                  Run the simulator to generate telemetry.
                </div>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData}>
                    <CartesianGrid
                      strokeDasharray="4 4"
                      stroke="#27344a"
                    />
                    <XAxis
                      dataKey="time"
                      stroke="#91a0b8"
                      unit="s"
                    />
                    <YAxis stroke="#91a0b8" />
                    <Tooltip />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="speed"
                      name="Speed (km/h)"
                      stroke="#38bdf8"
                      dot={false}
                      strokeWidth={2}
                    />
                    <Line
                      type="monotone"
                      dataKey="battery"
                      name="Battery (%)"
                      stroke="#34d399"
                      dot={false}
                      strokeWidth={2}
                    />
                    <Line
                      type="monotone"
                      dataKey="temperature"
                      name="Motor temperature (°C)"
                      stroke="#f59e0b"
                      dot={false}
                      strokeWidth={2}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </div>
          </article>

          <article className="panel alert-panel">
            <div className="panel-heading">
              <div>
                <p className="eyebrow">Safety monitoring</p>
                <h2>Triggered alerts</h2>
              </div>

              <span className="alert-count">
                {visibleAlerts.length}
              </span>
            </div>

            <div className="alert-list">
              {visibleAlerts.length === 0 ? (
                <div className="empty-state">
                  No alerts for this selection.
                </div>
              ) : (
                visibleAlerts.map((alert) => (
                  <div className="alert-item" key={alert.id}>
                    <div className="alert-icon">!</div>

                    <div>
                      <strong>
                        {formatAlertType(alert.alertType)}
                      </strong>
                      <p>{alert.message}</p>
                      <small>
                        {alert.vehicleId} ·{' '}
                        {formatDate(alert.triggeredAt)}
                      </small>
                    </div>
                  </div>
                ))
              )}
            </div>
          </article>
        </section>
      </main>
    </div>
  )
}

export default App