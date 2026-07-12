import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchApplication, updateStatus, deleteApplication } from '../api/applications'
import StatusBadge from '../components/StatusBadge'
import { format } from 'date-fns'

const VALID_TRANSITIONS = {
  APPLIED:   ['SCREENING', 'REJECTED', 'STALE'],
  SCREENING: ['INTERVIEW', 'REJECTED'],
  INTERVIEW: ['OFFER', 'REJECTED'],
  STALE:     ['SCREENING', 'REJECTED'],
  OFFER:     [],
  REJECTED:  []
}

export default function ApplicationDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [selectedStatus, setSelectedStatus] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const { data: app, isLoading, isError } = useQuery({
    queryKey: ['application', id],
    queryFn: () => fetchApplication(id)
  })

  const statusMutation = useMutation({
    mutationFn: updateStatus,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application', id] })
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      setSelectedStatus('')
    }
  })

  const deleteMutation = useMutation({
    mutationFn: deleteApplication,
    onSuccess: () => navigate('/applications')
  })

  if (isLoading) return <div className="text-center py-16 text-gray-400">Loading...</div>
  if (isError) return <div className="text-center py-16 text-red-500">Application not found</div>

  const allowedTransitions = VALID_TRANSITIONS[app.currentStatus] ?? []

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <Link to="/applications" className="text-sm text-gray-500 hover:text-gray-700 mb-3 inline-block">
          ← Back to Applications
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{app.company}</h1>
            <p className="text-gray-600 mt-1">{app.role}</p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={app.currentStatus} />
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="text-sm text-red-500 hover:text-red-700 border border-red-200 hover:border-red-400 px-3 py-1.5 rounded-lg transition-colors"
            >
              Delete
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-3 gap-6">
        {/* Main info */}
        <div className="col-span-2 space-y-6">

          {/* Details card */}
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <h2 className="font-semibold text-gray-900 mb-4">Details</h2>
            <dl className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <dt className="text-gray-500">Applied Date</dt>
                <dd className="font-medium mt-0.5">
                  {format(new Date(app.appliedDate), 'MMMM d, yyyy')}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Resume Version</dt>
                <dd className="font-medium mt-0.5">{app.resumeVersion || '—'}</dd>
              </div>
              <div>
                <dt className="text-gray-500">Match Score</dt>
                <dd className="font-medium mt-0.5">
                  {app.matchScore != null ? (
                    <span className={`${app.matchScore >= 70 ? 'text-green-600' : app.matchScore >= 50 ? 'text-yellow-600' : 'text-red-500'}`}>
                      {app.matchScore}%
                    </span>
                  ) : (
                    <span className="text-gray-300">Processing...</span>
                  )}
                </dd>
              </div>
              <div>
                <dt className="text-gray-500">Source</dt>
                <dd className="font-medium mt-0.5 truncate">
                  {app.sourceUrl ? (
                    <a href={app.sourceUrl} target="_blank" rel="noreferrer" className="text-primary-600 hover:underline">
                      View Job Posting ↗
                    </a>
                  ) : '—'}
                </dd>
              </div>
            </dl>
          </div>

          {/* AI Tags */}
          {app.tags?.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-900 mb-3">
                AI-Extracted Skills
                <span className="text-gray-400 font-normal text-xs ml-2">from job description</span>
              </h2>
              <div className="flex flex-wrap gap-2">
                {app.tags.map(tag => (
                  <span
                    key={tag}
                    className="bg-primary-50 text-primary-700 border border-primary-200 px-2.5 py-1 rounded-full text-xs font-medium"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Job Description */}
          {app.jdText && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h2 className="font-semibold text-gray-900 mb-3">Job Description</h2>
              <p className="text-sm text-gray-600 whitespace-pre-wrap leading-relaxed">
                {app.jdText}
              </p>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">

          {/* Update Status */}
          {allowedTransitions.length > 0 && (
            <div className="bg-white rounded-xl border border-gray-200 p-4">
              <h2 className="font-semibold text-gray-900 mb-3 text-sm">Update Status</h2>
              <select
                value={selectedStatus}
                onChange={e => setSelectedStatus(e.target.value)}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 mb-3"
              >
                <option value="">Select new status...</option>
                {allowedTransitions.map(s => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
              <button
                onClick={() => statusMutation.mutate({ id, newStatus: selectedStatus })}
                disabled={!selectedStatus || statusMutation.isPending}
                className="w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-40 text-white py-2 rounded-lg text-sm font-medium transition-colors"
              >
                {statusMutation.isPending ? 'Updating...' : 'Update'}
              </button>
              {statusMutation.isError && (
                <p className="text-red-500 text-xs mt-2">
                  {statusMutation.error?.response?.data?.detail || 'Update failed'}
                </p>
              )}
            </div>
          )}

          {/* Status History */}
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <h2 className="font-semibold text-gray-900 mb-3 text-sm">Status History</h2>
            <div className="space-y-3">
              {app.statusHistory?.map((entry, i) => (
                <div key={i} className="flex items-start gap-2 text-xs">
                  <div className="mt-1 w-1.5 h-1.5 rounded-full bg-primary-400 shrink-0" />
                  <div>
                    <p className="font-medium text-gray-700">
                      {entry.oldStatus ? `${entry.oldStatus} → ` : ''}{entry.newStatus}
                    </p>
                    <p className="text-gray-400">
                      {format(new Date(entry.changedAt), 'MMM d, yyyy HH:mm')}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Delete confirm */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full mx-4">
            <h3 className="font-semibold text-gray-900 mb-2">Delete Application?</h3>
            <p className="text-sm text-gray-500 mb-4">
              This will permanently delete the application for {app.role} at {app.company}.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => deleteMutation.mutate(id)}
                disabled={deleteMutation.isPending}
                className="flex-1 px-4 py-2 text-sm bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors disabled:opacity-50"
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}