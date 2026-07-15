import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { fetchApplications, updateStatus, exportCsv, exportPdf } from '../api/applications'
import KanbanBoard from '../components/KanbanBoard'
import StatusBadge from '../components/StatusBadge'
import CreateApplicationModal from '../components/CreateApplicationModal'
import CsvImportModal from '../components/CsvImportModal'
import GoalProgressWidget from '../components/GoalProgressWidget'
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts'
import { format } from 'date-fns'

const STATUSES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE']

export default function ApplicationsPage() {
  const [showModal, setShowModal] = useState(false)
  const [showImportModal, setShowImportModal] = useState(false)
  const [filters, setFilters] = useState({
    status: '',
    company: '',
    appliedDateFrom: '',
    appliedDateTo: ''
  })
  const [page, setPage] = useState(0)
  const [viewMode, setViewMode] = useState('table')
  const queryClient = useQueryClient()

  const statusMutation = useMutation({
    mutationFn: updateStatus,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    }
  })

  // Keyboard shortcuts
  useKeyboardShortcuts({
    onNew: () => setShowModal(true),
    onImport: () => setShowImportModal(true),
    onSearch: () => {
      const searchInput = document.getElementById('search-input')
      if (searchInput) {
        searchInput.focus()
      }
    }
  })

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

  const handleExport = async () => {
    try {
      const blob = await exportCsv()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `job-applications-${format(new Date(), 'yyyy-MM-dd')}.csv`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to export CSV', error)
      alert('Failed to export CSV')
    }
  }

  const handleExportPdf = async () => {
    try {
      const blob = await exportPdf()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `job-applications-${format(new Date(), 'yyyy-MM-dd')}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (error) {
      console.error('Failed to export PDF', error)
      alert('Failed to export PDF')
    }
  }

  return (

    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Applications</h1>
          {!isLoading && (
              <p className="text-sm text-gray-500 mt-1">
                {totalElements} application{totalElements !== 1 ? 's' : ''} total
              </p>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowImportModal(true)}
            className="border border-gray-300 hover:bg-gray-50 dark:bg-gray-900 text-gray-700 dark:text-gray-300 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            Import CSV
          </button>
          <button
            onClick={handleExport}
            className="border border-gray-300 hover:bg-gray-50 dark:bg-gray-900 text-gray-700 dark:text-gray-300 px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Export CSV
          </button>
          <button
            onClick={handleExportPdf}
            className="border border-gray-300 hover:bg-gray-50 dark:bg-gray-900 text-gray-700 dark:text-gray-300 px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Export PDF
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            + New Application
          </button>
        </div>
      </div>

      <div className="mb-6">
        <GoalProgressWidget />
      </div>

      {/* View Toggle & Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 mb-6">
        <div className="flex justify-between items-center mb-4">
          <div className="flex bg-gray-100 dark:bg-gray-900 rounded-lg p-1">
            <button
              onClick={() => setViewMode('table')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                viewMode === 'table' ? 'bg-white dark:bg-gray-800 shadow text-primary-700 dark:text-primary-400' : 'text-gray-500 hover:text-gray-900 dark:hover:text-white'
              }`}
            >
              Table View
            </button>
            <button
              onClick={() => setViewMode('kanban')}
              className={`px-4 py-1.5 rounded-md text-sm font-medium transition-colors ${
                viewMode === 'kanban' ? 'bg-white dark:bg-gray-800 shadow text-primary-700 dark:text-primary-400' : 'text-gray-500 hover:text-gray-900 dark:hover:text-white'
              }`}
            >
              Kanban Board
            </button>
          </div>
          {Object.values(filters).some(Boolean) && (
            <button
              onClick={clearFilters}
              className="text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 transition-colors"
            >
              Clear filters
            </button>
          )}
        </div>

        {viewMode === 'table' && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <select
              value={filters.status}
              onChange={e => handleFilterChange('status', e.target.value)}
              className="border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="">All Statuses</option>
              {STATUSES.map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>

            <div className="flex gap-2 items-center">
              <input
                id="search-input"
                type="text"
                placeholder="Search company or role (Press /)"
                value={filters.company}
                onChange={e => handleFilterChange('company', e.target.value)}
                className="w-full border border-gray-200 dark:border-gray-700 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 bg-transparent"
              />
            </div>

            <input
              type="date"
              value={filters.appliedDateFrom}
              onChange={e => handleFilterChange('appliedDateFrom', e.target.value)}
              className="border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <input
              type="date"
              value={filters.appliedDateTo}
              onChange={e => handleFilterChange('appliedDateTo', e.target.value)}
              className="border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-800 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        )}
      </div>

      {/* Applications List or Kanban */}
      {viewMode === 'kanban' ? (
        <KanbanBoard 
          applications={applications} 
          onStatusChange={(id, newStatus) => statusMutation.mutate({ id, newStatus })} 
        />
      ) : isLoading ? (
        <div className="text-center py-16 text-gray-400">Loading...</div>
      ) : isError ? (
        <div className="text-center py-16 text-red-500">Failed to load applications</div>
      ) : applications.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-gray-400 text-lg">No applications found</p>
          <p className="text-gray-300 text-sm mt-1">Add your first application to get started</p>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-900">
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Company</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Role</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Status</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Applied</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Match Score</th>
                <th className="text-left px-6 py-3 font-medium text-gray-600 dark:text-gray-400">Resume</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {applications.map(app => (
                <tr key={app.id} className="hover:bg-gray-50 dark:bg-gray-900 transition-colors">
                  <td className="px-6 py-4">
                    <Link
                      to={`/applications/${app.id}`}
                      className="font-medium text-gray-900 dark:text-gray-100 hover:text-primary-600 transition-colors"
                    >
                      {app.company}
                    </Link>
                  </td>
                  <td className="px-6 py-4 text-gray-600 dark:text-gray-400">{app.role}</td>
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
                        <span className="text-gray-600 dark:text-gray-400 text-xs">{app.matchScore}%</span>
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
            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 dark:border-gray-700">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50 dark:bg-gray-900 transition-colors"
              >
                Previous
              </button>
              <span className="text-sm text-gray-500">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg disabled:opacity-40 hover:bg-gray-50 dark:bg-gray-900 transition-colors"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}

      {showModal && (
        <CreateApplicationModal onClose={() => setShowModal(false)} />
      )}
      {showImportModal && (
        <CsvImportModal onClose={() => setShowImportModal(false)} />
      )}
    </div>
  )
}