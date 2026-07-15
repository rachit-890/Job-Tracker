import { useState } from 'react'
import client from '../api/client'

export default function AdminPage() {
    const [dltTopic, setDltTopic] = useState('job-events-dlt')
    const [targetTopic, setTargetTopic] = useState('job-events')
    const [loading, setLoading] = useState(false)
    const [result, setResult] = useState(null)
    const [error, setError] = useState(null)

    const handleReplay = async () => {
        setLoading(true)
        setError(null)
        setResult(null)
        try {
            const { data } = await client.post(`/admin/dlt/replay?dltTopic=${dltTopic}&targetTopic=${targetTopic}`)
            setResult(data)
        } catch (err) {
            setError(err.response?.data?.detail || 'Failed to replay dead letters')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div>
            <div className="mb-6">
                <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Admin Dashboard</h1>
                <p className="text-gray-500 dark:text-gray-400 mt-1">Manage system operations and message recovery.</p>
            </div>

            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 max-w-2xl shadow-sm">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                    <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    DLT Message Replay
                </h2>
                <p className="text-sm text-gray-600 dark:text-gray-400 mb-6">
                    Manually replay failed Kafka messages from the Dead Letter Topic (DLT) back into the primary processing queue.
                </p>

                <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Source (DLT Topic)</label>
                            <input
                                type="text"
                                value={dltTopic}
                                onChange={e => setDltTopic(e.target.value)}
                                className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Destination Topic</label>
                            <input
                                type="text"
                                value={targetTopic}
                                onChange={e => setTargetTopic(e.target.value)}
                                className="w-full border border-gray-300 dark:border-gray-600 rounded-lg px-3 py-2 text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100"
                            />
                        </div>
                    </div>

                    <button
                        onClick={handleReplay}
                        disabled={loading || !dltTopic || !targetTopic}
                        className="bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-medium px-4 py-2 rounded-lg text-sm transition-colors"
                    >
                        {loading ? 'Replaying...' : 'Replay Failed Events'}
                    </button>

                    {error && (
                        <div className="p-3 bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400 text-sm rounded-lg border border-red-200 dark:border-red-800">
                            {error}
                        </div>
                    )}

                    {result && (
                        <div className="p-4 bg-green-50 dark:bg-green-900/30 text-green-800 dark:text-green-300 rounded-lg border border-green-200 dark:border-green-800">
                            <h3 className="font-medium mb-1 flex items-center gap-2">
                                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                                </svg>
                                Replay Successful
                            </h3>
                            <p className="text-sm opacity-90">
                                Successfully recovered and replayed <strong>{result.replayedCount}</strong> messages from <code>{result.dltTopic}</code>.
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
