import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import { format } from 'date-fns'

export default function SettingsPage() {
    const { logout } = useAuth()
    const [apiKey, setApiKey] = useState('demo-api-key-8x9q2m')
    
    // Simple copy helper
    const copyToClipboard = (text) => {
        navigator.clipboard.writeText(text)
        alert('Copied to clipboard!')
    }

    return (
        <div>
            <div className="mb-6 flex justify-between items-end">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Settings & Integrations</h1>
                    <p className="text-gray-500 dark:text-gray-400 mt-1">Configure your ingestion methods and account preferences.</p>
                </div>
                <button
                    onClick={logout}
                    className="text-red-500 hover:text-red-700 font-medium px-4 py-2 border border-red-200 hover:border-red-400 rounded-lg transition-colors"
                >
                    Sign Out
                </button>
            </div>

            <div className="space-y-6 max-w-4xl">
                
                {/* Email Ingestion */}
                <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2 flex items-center gap-2">
                        <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                        </svg>
                        Email Ingestion
                    </h2>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                        Forward application confirmation emails to this webhook to automatically create new applications.
                    </p>
                    
                    <div className="bg-gray-50 dark:bg-gray-900/50 p-4 rounded-lg border border-gray-100 dark:border-gray-800 flex justify-between items-center">
                        <div>
                            <p className="text-xs text-gray-500 mb-1">Webhook URL</p>
                            <code className="text-sm text-primary-600 dark:text-primary-400">http://localhost:8080/api/v1/ingest/email</code>
                        </div>
                        <button 
                            onClick={() => copyToClipboard('http://localhost:8080/api/v1/ingest/email')}
                            className="text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 px-3 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600"
                        >
                            Copy
                        </button>
                    </div>
                </div>

                {/* Chrome Extension */}
                <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-2 flex items-center gap-2">
                        <svg className="w-5 h-5 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" />
                        </svg>
                        Chrome Extension
                    </h2>
                    <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
                        Use our browser extension to scrape job descriptions directly from LinkedIn, Indeed, and Greenhouse.
                    </p>
                    
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-gray-50 dark:bg-gray-900/50 p-4 rounded-lg border border-gray-100 dark:border-gray-800 flex flex-col justify-between">
                            <div>
                                <p className="text-xs text-gray-500 mb-1">API Endpoint</p>
                                <code className="text-sm text-primary-600 dark:text-primary-400">POST /api/v1/ingest/extension</code>
                            </div>
                            <button 
                                onClick={() => copyToClipboard('http://localhost:8080/api/v1/ingest/extension')}
                                className="text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 px-3 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 mt-3 self-start"
                            >
                                Copy Endpoint
                            </button>
                        </div>
                        <div className="bg-gray-50 dark:bg-gray-900/50 p-4 rounded-lg border border-gray-100 dark:border-gray-800 flex flex-col justify-between">
                            <div>
                                <p className="text-xs text-gray-500 mb-1">Your API Key (Required for Extension)</p>
                                <code className="text-sm font-medium text-gray-900 dark:text-gray-100">{apiKey}</code>
                            </div>
                            <button 
                                onClick={() => {
                                    setApiKey('new-api-key-' + Math.random().toString(36).substring(7))
                                }}
                                className="text-xs bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 px-3 py-1.5 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 mt-3 self-start text-red-600 dark:text-red-400"
                            >
                                Regenerate Key
                            </button>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    )
}
