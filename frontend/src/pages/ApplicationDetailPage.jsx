import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchApplication, updateStatus, deleteApplication } from '../api/applications'
import { fetchNotes, addNote, deleteNote } from '../api/notes'
import StatusBadge from '../components/StatusBadge'
import { format } from 'date-fns'

const STATUSES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE']
export default function ApplicationDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [selectedStatus, setSelectedStatus] = useState('')
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [newNote, setNewNote] = useState('')

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

  const { data: notes = [] } = useQuery({
    queryKey: ['notes', id],
    queryFn: () => fetchNotes(id),
    enabled: !!app
  })

  const addNoteMutation = useMutation({
    mutationFn: addNote,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes', id] })
      setNewNote('')
    }
  })

  const deleteNoteMutation = useMutation({
    mutationFn: deleteNote,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notes', id] })
  })

  const deleteMutation = useMutation({
    mutationFn: deleteApplication,
    onSuccess: () => navigate('/applications')
  })

  if (isLoading) return <div className="text-center py-16 text-gray-400">Loading...</div>
  if (isError) return <div className="text-center py-16 text-red-500">Application not found</div>

  const allowedTransitions = STATUSES.filter(s => s !== app.currentStatus)

  // Combine and sort history + notes
  const timeline = [
    ...(app.statusHistory?.map(h => ({ ...h, type: 'status', timestamp: h.changedAt })) || []),
    ...notes.map(n => ({ ...n, type: 'note', timestamp: n.createdAt }))
  ].sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <Link to="/applications" className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-300 mb-3 inline-block">
          ← Back to Applications
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{app.company}</h1>
            <p className="text-gray-600 dark:text-gray-400 mt-1">{app.role}</p>
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
          <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4">Details</h2>
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
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
              <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-3">
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

          {/* Gap Analysis */}
          {app.gapAnalysis && (
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
              <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-3">
                Resume Gap Analysis
                <span className="text-gray-400 font-normal text-xs ml-2">AI-powered comparison</span>
              </h2>

              {app.gapAnalysis.summary && (
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 leading-relaxed bg-gray-50 dark:bg-gray-900 p-3 rounded-lg">
                  {app.gapAnalysis.summary}
                </p>
              )}

              <div className="grid grid-cols-2 gap-4">
                {/* Resume Covers */}
                <div>
                  <h3 className="text-xs font-semibold text-green-700 uppercase tracking-wider mb-2 flex items-center gap-1">
                    <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                    Your Resume Covers
                  </h3>
                  <div className="flex flex-wrap gap-1.5">
                    {app.gapAnalysis.resumeCovers?.map(skill => (
                      <span
                        key={skill}
                        className="bg-green-50 text-green-700 border border-green-200 px-2 py-0.5 rounded-full text-xs"
                      >
                        {skill}
                      </span>
                    ))}
                    {(!app.gapAnalysis.resumeCovers || app.gapAnalysis.resumeCovers.length === 0) && (
                      <span className="text-gray-300 text-xs">None identified</span>
                    )}
                  </div>
                </div>

                {/* Resume Lacks */}
                <div>
                  <h3 className="text-xs font-semibold text-red-700 uppercase tracking-wider mb-2 flex items-center gap-1">
                    <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                    </svg>
                    Missing from Resume
                  </h3>
                  <div className="flex flex-wrap gap-1.5">
                    {app.gapAnalysis.resumeLacks?.map(skill => (
                      <span
                        key={skill}
                        className="bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded-full text-xs"
                      >
                        {skill}
                      </span>
                    ))}
                    {(!app.gapAnalysis.resumeLacks || app.gapAnalysis.resumeLacks.length === 0) && (
                      <span className="text-gray-300 text-xs">None identified</span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Job Description */}
          {app.jdText && (
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6">
              <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-3">Job Description</h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap leading-relaxed">
                {app.jdText}
              </p>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">

          {/* Update Status */}
          {allowedTransitions.length > 0 && (
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
              <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-3 text-sm">Update Status</h2>
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

          {/* Notes & Timeline */}
          <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4">
            <h2 className="font-semibold text-gray-900 dark:text-gray-100 mb-4 text-sm">Notes & Timeline</h2>

            <form
              onSubmit={e => {
                e.preventDefault()
                if (newNote.trim()) addNoteMutation.mutate({ applicationId: id, content: newNote })
              }}
              className="mb-6"
            >
              <textarea
                value={newNote}
                onChange={e => setNewNote(e.target.value)}
                placeholder="Add a note (e.g. Called HR)..."
                rows={2}
                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none mb-2"
              />
              <button
                type="submit"
                disabled={!newNote.trim() || addNoteMutation.isPending}
                className="bg-primary-600 hover:bg-primary-700 text-white px-3 py-1.5 rounded-lg text-xs font-medium transition-colors disabled:opacity-50"
              >
                {addNoteMutation.isPending ? 'Adding...' : 'Add Note'}
              </button>
            </form>

            <div className="space-y-4">
              {timeline.map((item, i) => (
                <div key={i} className="flex gap-3 text-sm">
                  <div className="mt-1 w-2 h-2 rounded-full shrink-0 flex items-center justify-center">
                    <div className={`w-2 h-2 rounded-full ${item.type === 'status' ? 'bg-primary-500' : 'bg-gray-400'}`} />
                  </div>
                  <div className="flex-1">
                    {item.type === 'status' ? (
                      <p className="font-medium text-gray-900 dark:text-gray-100 text-xs">
                        Status changed to <span className="text-primary-700">{item.newStatus}</span>
                        {item.oldStatus && <span className="text-gray-500 font-normal"> (from {item.oldStatus})</span>}
                      </p>
                    ) : (
                      <div className="group flex justify-between items-start gap-2">
                        <p className="text-gray-700 dark:text-gray-300 text-xs whitespace-pre-wrap">{item.content}</p>
                        <button
                          onClick={() => deleteNoteMutation.mutate({ applicationId: id, noteId: item.id })}
                          className="opacity-0 group-hover:opacity-100 text-red-500 hover:text-red-700 text-xs shrink-0 transition-opacity"
                        >
                          Delete
                        </button>
                      </div>
                    )}
                    <p className="text-gray-400 text-xs mt-0.5">
                      {format(new Date(item.timestamp), 'MMM d, h:mm a')}
                    </p>
                  </div>
                </div>
              ))}
              {timeline.length === 0 && (
                <p className="text-gray-400 text-xs text-center">No timeline events yet</p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Delete confirm */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl p-6 max-w-sm w-full mx-4">
            <h3 className="font-semibold text-gray-900 dark:text-gray-100 mb-2">Delete Application?</h3>
            <p className="text-sm text-gray-500 mb-4">
              This will permanently delete the application for {app.role} at {app.company}.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="flex-1 px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 dark:bg-gray-900 transition-colors"
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