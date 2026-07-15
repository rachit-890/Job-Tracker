import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchResumes, createResume, deleteResume } from '../api/resumes'
import { format } from 'date-fns'

export default function ResumesPage() {
    const queryClient = useQueryClient()
    const [showForm, setShowForm] = useState(false)
    const [label, setLabel] = useState('')
    const [content, setContent] = useState('')
    const [selectedResumeId, setSelectedResumeId] = useState(null)

    const { data: resumes = [], isLoading } = useQuery({
        queryKey: ['resumes'],
        queryFn: fetchResumes
    })

    const createMutation = useMutation({
        mutationFn: createResume,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['resumes'] })
            setShowForm(false)
            setLabel('')
            setContent('')
        }
    })

    const deleteMutation = useMutation({
        mutationFn: deleteResume,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['resumes'] })
            setSelectedResumeId(null)
        }
    })

    const handleSubmit = (e) => {
        e.preventDefault()
        if (label && content) {
            createMutation.mutate({ label, content })
        }
    }

    const selectedResume = resumes.find(r => r.id === selectedResumeId)

    if (isLoading) return <div className="text-center py-16 text-gray-400">Loading...</div>

    return (
        <div className="max-w-6xl mx-auto flex gap-6">
            {/* Sidebar List */}
            <div className="w-1/3 flex flex-col gap-4">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Resumes</h1>
                    <button
                        onClick={() => setShowForm(true)}
                        className="bg-primary-600 hover:bg-primary-700 text-white px-3 py-1.5 rounded-lg text-sm font-medium transition-colors"
                    >
                        + New
                    </button>
                </div>

                <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 overflow-hidden">
                    {resumes.length === 0 ? (
                        <p className="text-gray-500 text-sm p-6 text-center">No resumes uploaded yet.</p>
                    ) : (
                        <div className="divide-y divide-gray-100">
                            {resumes.map(resume => (
                                <button
                                    key={resume.id}
                                    onClick={() => {
                                        setSelectedResumeId(resume.id)
                                        setShowForm(false)
                                    }}
                                    className={`w-full text-left px-4 py-3 transition-colors ${selectedResumeId === resume.id ? 'bg-primary-50' : 'hover:bg-gray-50 dark:bg-gray-900'}`}
                                >
                                    <div className="flex items-center justify-between mb-1">
                                        <span className="font-medium text-gray-900 dark:text-gray-100 text-sm">{resume.label}</span>
                                        <span className="text-gray-400 text-xs">
                                            {format(new Date(resume.createdAt), 'MMM d, yyyy')}
                                        </span>
                                    </div>
                                    <p className="text-gray-500 text-xs truncate">{resume.content}</p>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Main Content Area */}
            <div className="w-2/3">
                {showForm ? (
                    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                        <div className="flex justify-between items-center mb-6">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Upload New Version</h2>
                            <button onClick={() => setShowForm(false)} className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-300">Cancel</button>
                        </div>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Version Label</label>
                                <input
                                    value={label}
                                    onChange={e => setLabel(e.target.value)}
                                    placeholder="e.g. Frontend v2, Backend v1"
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Resume Content</label>
                                <textarea
                                    value={content}
                                    onChange={e => setContent(e.target.value)}
                                    placeholder="Paste your full resume text here..."
                                    rows={15}
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none font-mono"
                                    required
                                />
                            </div>
                            <div className="flex justify-end">
                                <button
                                    type="submit"
                                    disabled={createMutation.isPending}
                                    className="bg-primary-600 hover:bg-primary-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50"
                                >
                                    {createMutation.isPending ? 'Saving...' : 'Save Resume'}
                                </button>
                            </div>
                            {createMutation.isError && (
                                <p className="text-red-500 text-sm text-right mt-2">{createMutation.error?.response?.data?.detail || 'Failed to save'}</p>
                            )}
                        </form>
                    </div>
                ) : selectedResume ? (
                    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                        <div className="flex justify-between items-start mb-6 pb-4 border-b">
                            <div>
                                <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{selectedResume.label}</h2>
                                <p className="text-sm text-gray-500 mt-1">
                                    Uploaded on {format(new Date(selectedResume.createdAt), 'MMMM d, yyyy h:mm a')}
                                </p>
                            </div>
                            <button
                                onClick={() => {
                                    if(window.confirm('Delete this resume version?')) {
                                        deleteMutation.mutate(selectedResume.id)
                                    }
                                }}
                                disabled={deleteMutation.isPending}
                                className="text-red-600 hover:text-red-700 text-sm font-medium border border-red-200 hover:bg-red-50 px-3 py-1.5 rounded-lg transition-colors"
                            >
                                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
                            </button>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4 font-mono text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed h-[600px] overflow-y-auto">
                            {selectedResume.content}
                        </div>
                    </div>
                ) : (
                    <div className="h-full flex items-center justify-center border-2 border-dashed border-gray-200 dark:border-gray-700 rounded-xl bg-gray-50 dark:bg-gray-900">
                        <p className="text-gray-400">Select a resume to view details or create a new one.</p>
                    </div>
                )}
            </div>
        </div>
    )
}
