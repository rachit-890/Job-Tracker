import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend,
  LineChart, Line
} from 'recharts'
import {
  fetchAnalyticsSummary,
  fetchResumePerformance,
  fetchCompanyAnalytics,
  fetchTrend,
  fetchStatusFlow,
  fetchDayOfWeek
} from '../api/analytics'
import SankeyChart from '../components/SankeyChart'
import DayOfWeekHeatmap from '../components/DayOfWeekHeatmap'
import { createShareToken } from '../api/applications'

const STATUS_COLORS = {
  APPLIED:   '#3b82f6',
  SCREENING: '#f59e0b',
  INTERVIEW: '#8b5cf6',
  OFFER:     '#10b981',
  REJECTED:  '#ef4444',
  STALE:     '#9ca3af'
}

function StatCard({ label, value, sub }) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-3xl font-bold text-gray-900 dark:text-gray-100 mt-1">{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
    </div>
  )
}

export default function AnalyticsPage() {
  const [shareLink, setShareLink] = useState('')

  const shareMutation = useMutation({
    mutationFn: createShareToken,
    onSuccess: (data) => {
      setShareLink(`${window.location.origin}/shared/${data.token}`)
    }
  })

  const { data: summary, isLoading: loadingSummary } = useQuery({
    queryKey: ['analytics', 'summary'],
    queryFn: fetchAnalyticsSummary
  })

  const { data: resumePerf } = useQuery({
    queryKey: ['analytics', 'resume-performance'],
    queryFn: fetchResumePerformance
  })

  const { data: companyData } = useQuery({
    queryKey: ['analytics', 'company'],
    queryFn: fetchCompanyAnalytics
  })

  const { data: trend } = useQuery({
    queryKey: ['analytics', 'trend'],
    queryFn: () => fetchTrend('30d')
  })

  const { data: statusFlow } = useQuery({
    queryKey: ['analytics', 'status-flow'],
    queryFn: fetchStatusFlow
  })

  const { data: dayOfWeek } = useQuery({
    queryKey: ['analytics', 'day-of-week'],
    queryFn: fetchDayOfWeek
  })

  if (loadingSummary) {
    return <div className="text-center py-16 text-gray-400">Loading analytics...</div>
  }

  const statusBreakdown = summary?.statusBreakdown
    ? Object.entries(summary.statusBreakdown).map(([name, value]) => ({ name, value }))
    : []

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Analytics</h1>
        <button
          onClick={() => shareMutation.mutate()}
          disabled={shareMutation.isPending}
          className="bg-primary-600 hover:bg-primary-700 disabled:opacity-50 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          {shareMutation.isPending ? 'Generating...' : 'Share Dashboard'}
        </button>
      </div>

      {shareLink && (
        <div className="bg-primary-50 dark:bg-primary-900/30 border border-primary-200 dark:border-primary-800 rounded-xl p-4 mb-6 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-primary-800 dark:text-primary-200">Public Dashboard Link Generated</h3>
            <p className="text-xs text-primary-600 dark:text-primary-400 mt-1">{shareLink}</p>
          </div>
          <button
            onClick={() => {
              navigator.clipboard.writeText(shareLink)
              setShareLink('')
              alert('Link copied to clipboard!')
            }}
            className="text-sm bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg"
          >
            Copy Link
          </button>
        </div>
      )}

      {/* Stat cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <StatCard
          label="Total Applications"
          value={summary?.totalApplications ?? 0}
        />
        <StatCard
          label="Response Rate"
          value={`${summary?.responseRate ?? 0}%`}
          sub="Applications that moved past Applied"
        />
        <StatCard
          label="Avg Time to Response"
          value={summary?.avgTimeToResponse != null
            ? `${Math.round(summary.avgTimeToResponse)}d`
            : '—'}
          sub="Average days to first response"
        />
        <StatCard
          label="Active Pipeline"
          value={(summary?.screeningCount ?? 0) + (summary?.interviewCount ?? 0)}
          sub="Screening + Interview"
        />
      </div>

      <div className="grid grid-cols-2 gap-6 mb-6">
        {/* Status Breakdown Pie */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Status Breakdown</h2>
          {statusBreakdown.length > 0 ? (
            <ResponsiveContainer width="100%" height={240}>
              <PieChart>
                <Pie
                  data={statusBreakdown}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={90}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {statusBreakdown.map(entry => (
                    <Cell
                      key={entry.name}
                      fill={STATUS_COLORS[entry.name] ?? '#9ca3af'}
                    />
                  ))}
                </Pie>
                <Tooltip formatter={(value, name) => [value, name]} />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-60 flex items-center justify-center text-gray-300">
              No data yet
            </div>
          )}
          <div className="flex flex-wrap gap-3 mt-2 justify-center">
            {statusBreakdown.map(entry => (
              <div key={entry.name} className="flex items-center gap-1.5 text-xs">
                <div
                  className="w-2.5 h-2.5 rounded-full"
                  style={{ backgroundColor: STATUS_COLORS[entry.name] ?? '#9ca3af' }}
                />
                <span className="text-gray-600 dark:text-gray-400">{entry.name} ({entry.value})</span>
              </div>
            ))}
          </div>
        </div>

        {/* Resume Performance */}
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
          <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Resume Performance</h2>
          {resumePerf?.length > 0 ? (
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={resumePerf} layout="vertical" margin={{ left: 16 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" domain={[0, 100]} tickFormatter={v => `${v}%`} tick={{ fontSize: 11 }} />
                <YAxis type="category" dataKey="resumeVersion" tick={{ fontSize: 11 }} width={50} />
                <Tooltip formatter={(v) => [`${v}%`, 'Callback Rate']} />
                <Bar dataKey="callbackRate" fill="#3b82f6" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-60 flex items-center justify-center text-gray-300">
              No resume data yet
            </div>
          )}
        </div>
      </div>

      {/* Application Flow — Sankey Diagram */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Application Flow</h2>
        <p className="text-xs text-gray-400 mb-3">How applications moved through your pipeline</p>
        <SankeyChart links={statusFlow} />
      </div>

      {/* 30-day Trend */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">30-Day Trend</h2>
        {trend?.length > 0 ? (
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={trend}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="applications"
                stroke="#3b82f6"
                strokeWidth={2}
                dot={false}
                name="Applications"
              />
              <Line
                type="monotone"
                dataKey="responses"
                stroke="#10b981"
                strokeWidth={2}
                dot={false}
                name="Responses"
              />
            </LineChart>
          </ResponsiveContainer>
        ) : (
          <div className="h-52 flex items-center justify-center text-gray-300">
            No trend data yet
          </div>
        )}
      </div>

      {/* Day-of-Week Heatmap */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Application Activity</h2>
        <p className="text-xs text-gray-400 mb-3">Which days of the week you apply most</p>
        <DayOfWeekHeatmap data={dayOfWeek} />
      </div>

      {/* Company Analytics */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
        <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Company Analytics</h2>
        {companyData?.length > 0 ? (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700">
                <th className="text-left py-2 font-medium text-gray-600 dark:text-gray-400">Company</th>
                <th className="text-right py-2 font-medium text-gray-600 dark:text-gray-400">Applications</th>
                <th className="text-right py-2 font-medium text-gray-600 dark:text-gray-400">Response Rate</th>
                <th className="text-right py-2 font-medium text-gray-600 dark:text-gray-400">Avg Days to Response</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {companyData.map(row => (
                <tr key={row.company} className="hover:bg-gray-50 dark:bg-gray-900">
                  <td className="py-2.5 font-medium text-gray-900 dark:text-gray-100">{row.company}</td>
                  <td className="py-2.5 text-right text-gray-600 dark:text-gray-400">{row.applicationCount}</td>
                  <td className="py-2.5 text-right">
                    <span className={
                      row.responseRate >= 50 ? 'text-green-600' :
                      row.responseRate >= 25 ? 'text-yellow-600' : 'text-red-500'
                    }>
                      {row.responseRate}%
                    </span>
                  </td>
                  <td className="py-2.5 text-right text-gray-600 dark:text-gray-400">
                    {row.avgDaysToResponse != null ? `${Math.round(row.avgDaysToResponse)}d` : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <div className="text-center py-8 text-gray-300">No company data yet</div>
        )}
      </div>
    </div>
  )
}