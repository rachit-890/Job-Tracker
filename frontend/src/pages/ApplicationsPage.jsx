import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchApplications } from '../api/applications'
import StatusBadge from '../components/StatusBadge'
import CreateApplicationModal from '../components/CreateApplicationModal'
import { format } from 'date-fns'

const STATUSES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE']

export default function ApplicationsPage() {
  const [showModal, setShowModal] = useState(false)
  const [filters, setFilters] = useState({
    status: '',
    company: '',
    appliedDateFrom: '',
    appliedDateTo: ''
  })
  const [page, setPage] = useState(0)

  const params = {
    page,
    size: 20,
    sort: 'appliedDate,desc',
    ...(filters.status && { status: filters.status }),
    ...(filters.company && { company: filters.company }),
    ...(filters.appliedDateFrom && { appliedDateFrom: filters.appliedDateFrom }),
    ...(filters.appliedDateTo && { appliedDateTo: filters.appliedDateTo })
  }

  const { data, isLoading, isError } = useQuery({
    queryKey: ['applications', params],
    queryFn: () => fetchApplications(params)
  })

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }))
    setPage(0)
  }

  const clearFilters = () => {
    setFilters({ status: '', company: '', appliedDateFrom: '', appliedDateTo: '' })
    setPage(0)
  }

  const applications = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (

    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Applications</h1>
          {!isLoading && (
              <p className="text-sm text-gray-500 mt-1">
                {totalElements} application{totalElements !== 1 ? 's' : ''} total
              </p>
          )}
        </div>

        <div className="flex items-center gap-2">
          <a
              href="/api/v1/export/csv"
              className="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Export CSV
          </a>

          <a
              href="/api/v1/export/pdf"
              className="px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Export PDF
          </a>

          <button
              onClick={() => setShowModal(true)}
              className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            + Add Application
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 mb-6">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <select
            value={filters.status}
            onChange={e => handleFilterChange('status', e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="">All Statuses</option>
            {STATUSES.map(s => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>

          <input
            type="text"
            placeholder="Filter by company..."
            value={filters.company}
            onChange={e => handleFilterChange('company', e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
          />

          <input
            type="date"
            value={filters.appliedDateFrom}
            onChange={e => handleFilterChange('appliedDateFrom', e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="From date"
          />

          <input
            type="date"
            value={filters.appliedDateTo}
            onChange={e => handleFilterChange('appliedDateTo', e.target.value)}
            className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            placeholder="To date"
          />
        </div>

        {Object.values(filters).some(Boolean) && (
          <button
            onClick={clearFilters}
            className="mt-3 text-sm text-gray-500 hover:text-gray-700 underline"
          >
            Clear filters
          </button>
        )}
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="text-center py-16 text-gray-400">Loading...</div>
      ) : isError ? (
        <div className="text-center py-16 text-red-500">Failed to load applications</div>
      ) : applications.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-400 text-lg">No applications found</p>
          <p className="text-gray-300 text-sm mt-1">Add your first application to get started</p>
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 bg-gray-50">
                <th className="text-left px-6 py-3 font-medium text-gray-600">Company</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600">Role</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600">Status</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600">Applied</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600">Match Score</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600">Resume</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {applications.map(app => (
                <tr key={app.id} className="hover:bg-gray-50 transition-colors">
                  <td className="px-6 py-4">
                    <Link
                      to={`/applications/${app.id}`}
                      className="font-medium text-gray-900 hover:text-primary-600 transition-colors"
                    >
                      {app.company}
                    </Link>
                  </td>
                  <td className="px-6 py-4 text-gray-600">{app.role}</td>
                  <td className="px-6 py-4">
                    <StatusBadge status={app.currentStatus} />
                  </td>
                  <td className="px-6 py-4 text-gray-500">
                    {format(new Date(app.appliedDate), 'MMM d, yyyy')}
                  </td>
                  <td className="px-6 py-4">
                    {app.matchScore != null ? (
                      <div className="flex items-center gap-2">
                        <div className="w-16 h-1.5 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-primary-500 rounded-full"
                            style={{ width: `${app.matchScore}%` }}
                          />
                        </div>
                        <span className="text-gray-600 text-xs">{app.matchScore}%</span>
                      </div>
                    ) : (
                      <span className="text-gray-300 text-xs">—</span>
                    )}
                  </td>
                  <td className="px-6 py-4 text-gray-400 text-xs">
                    {app.resumeVersion || '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors"
              >
                Previous
              </button>
              <span className="text-sm text-gray-500">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50 transition-colors"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}

      {showModal && <CreateApplicationModal onClose={() => setShowModal(false)} />}
    </div>
  )
}