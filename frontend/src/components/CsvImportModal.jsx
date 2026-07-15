import { useState, useRef } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadCsv } from '../api/import'

export default function CsvImportModal({ onClose }) {
    const queryClient = useQueryClient()
    const [file, setFile] = useState(null)
    const [result, setResult] = useState(null)
    const fileInputRef = useRef(null)

    const mutation = useMutation({
        mutationFn: uploadCsv,
        onSuccess: (data) => {
            setResult(data)
            if (data.successfulImports > 0) {
                queryClient.invalidateQueries({ queryKey: ['applications'] })
            }
        }
    })

    const handleDrop = (e) => {
        e.preventDefault()
        const droppedFile = e.dataTransfer.files[0]
        if (droppedFile?.type === 'text/csv' || droppedFile?.name.endsWith('.csv')) {
            setFile(droppedFile)
        }
    }

    const handleSubmit = (e) => {
        e.preventDefault()
        if (file) mutation.mutate(file)
    }

    return (
        <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-lg overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50 dark:bg-gray-900/50">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Import Applications</h2>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-500 transition-colors">
                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <div className="p-6">
                    {result ? (
                        <div>
                            <div className="mb-4">
                                <h3 className="text-md font-medium text-gray-900 dark:text-gray-100 mb-2">Import Results</h3>
                                <div className="grid grid-cols-3 gap-4 mb-4">
                                    <div className="bg-gray-50 dark:bg-gray-900 p-3 rounded-lg text-center">
                                        <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{result.totalRows}</p>
                                        <p className="text-xs text-gray-500 uppercase tracking-wider font-medium">Total Rows</p>
                                    </div>
                                    <div className="bg-green-50 p-3 rounded-lg text-center">
                                        <p className="text-2xl font-bold text-green-700">{result.successfulImports}</p>
                                        <p className="text-xs text-green-600 uppercase tracking-wider font-medium">Success</p>
                                    </div>
                                    <div className="bg-red-50 p-3 rounded-lg text-center">
                                        <p className="text-2xl font-bold text-red-700">{result.failedImports}</p>
                                        <p className="text-xs text-red-600 uppercase tracking-wider font-medium">Failed</p>
                                    </div>
                                </div>
                            </div>
                            
                            {result.errors?.length > 0 && (
                                <div className="mb-4 bg-red-50 text-red-700 p-4 rounded-lg text-sm h-32 overflow-y-auto font-mono">
                                    <p className="font-semibold mb-1 border-b border-red-200 pb-1">Errors:</p>
                                    <ul className="list-disc pl-4 space-y-1">
                                        {result.errors.map((err, i) => <li key={i}>{err}</li>)}
                                    </ul>
                                </div>
                            )}

                            <div className="flex justify-end mt-6 pt-4 border-t border-gray-100">
                                <button
                                    onClick={onClose}
                                    className="bg-gray-100 hover:bg-gray-200 text-gray-700 dark:text-gray-300 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                                >
                                    Close
                                </button>
                            </div>
                        </div>
                    ) : (
                        <form onSubmit={handleSubmit}>
                            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 leading-relaxed">
                                Upload a CSV file to bulk import job applications. The file must include <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">Company</code> and <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">Role</code> columns. Optional columns: <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">Status</code>, <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">AppliedDate</code> (YYYY-MM-DD), <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">SourceUrl</code>, <code className="bg-gray-100 px-1 py-0.5 rounded text-gray-800 dark:text-gray-200">ResumeVersion</code>.
                            </p>

                            <div 
                                className={`border-2 border-dashed rounded-xl p-8 text-center transition-colors ${file ? 'border-primary-400 bg-primary-50' : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50 dark:bg-gray-900'}`}
                                onDragOver={e => e.preventDefault()}
                                onDrop={handleDrop}
                                onClick={() => fileInputRef.current?.click()}
                            >
                                <input 
                                    type="file" 
                                    accept=".csv" 
                                    className="hidden" 
                                    ref={fileInputRef}
                                    onChange={e => setFile(e.target.files[0])}
                                />
                                {file ? (
                                    <div>
                                        <svg className="w-8 h-8 text-primary-500 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        <p className="text-sm font-medium text-primary-700">{file.name}</p>
                                        <p className="text-xs text-primary-500 mt-1">{(file.size / 1024).toFixed(1)} KB</p>
                                    </div>
                                ) : (
                                    <div>
                                        <svg className="w-8 h-8 text-gray-400 mx-auto mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                        </svg>
                                        <p className="text-sm font-medium text-gray-700 dark:text-gray-300">Click to upload or drag and drop</p>
                                        <p className="text-xs text-gray-500 mt-1">CSV files only</p>
                                    </div>
                                )}
                            </div>

                            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-100">
                                <button
                                    type="button"
                                    onClick={onClose}
                                    className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:bg-gray-900 rounded-lg transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={!file || mutation.isPending}
                                    className="bg-primary-600 hover:bg-primary-700 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 flex items-center gap-2"
                                >
                                    {mutation.isPending && (
                                        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                    )}
                                    {mutation.isPending ? 'Importing...' : 'Start Import'}
                                </button>
                            </div>
                            
                            {mutation.isError && (
                                <p className="text-red-500 text-sm mt-3 text-center">
                                    {mutation.error?.response?.data?.detail || 'Import failed'}
                                </p>
                            )}
                        </form>
                    )}
                </div>
            </div>
        </div>
    )
}
