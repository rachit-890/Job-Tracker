import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchSharedAnalytics } from '../api/public'
import {
    PieChart, Pie, Cell, Tooltip, ResponsiveContainer
} from 'recharts'

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
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5 shadow-sm">
            <p className="text-sm text-gray-500">{label}</p>
            <p className="text-3xl font-bold text-gray-900 dark:text-gray-100 mt-1">{value}</p>
            {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
        </div>
    )
}

export default function SharedApplicationPage() {
    const { token } = useParams()

    const { data: summary, isLoading, isError } = useQuery({
        queryKey: ['shared-analytics', token],
        queryFn: () => fetchSharedAnalytics(token)
    })

    if (isLoading) return <div className="min-h-screen bg-gray-50 flex items-center justify-center text-gray-400">Loading Dashboard...</div>
    if (isError) return <div className="min-h-screen bg-gray-50 flex items-center justify-center text-red-500">This link is invalid or has expired.</div>

    const statusBreakdown = summary ? [
        { name: 'APPLIED', value: summary.appliedCount },
        { name: 'SCREENING', value: summary.screeningCount },
        { name: 'INTERVIEW', value: summary.interviewCount },
        { name: 'OFFER', value: summary.offerCount },
        { name: 'REJECTED', value: summary.rejectedCount },
        { name: 'STALE', value: summary.staleCount }
    ].filter(item => item.value > 0) : []

    return (
        <div className="min-h-screen bg-gray-50 dark:bg-gray-900 p-8">
            <div className="max-w-5xl mx-auto">
                <div className="mb-6 flex justify-between items-end">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100">Public Dashboard</h1>
                        <p className="text-gray-500 dark:text-gray-400 mt-1 text-lg">Job Search Progress</p>
                    </div>
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                    <StatCard
                        label="Total Applications"
                        value={summary?.totalApplications ?? 0}
                    />
                    <StatCard
                        label="Response Rate"
                        value={`${summary?.responseRate ?? 0}%`}
                        sub="Moved past Applied"
                    />
                    <StatCard
                        label="Interviews"
                        value={summary?.interviewCount ?? 0}
                    />
                    <StatCard
                        label="Offers"
                        value={summary?.offerCount ?? 0}
                    />
                </div>

                <div className="grid grid-cols-1 gap-6">
                    <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-6 shadow-sm">
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">Status Breakdown</h2>
                        <div className="h-[300px]">
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Pie
                                        data={statusBreakdown}
                                        dataKey="value"
                                        nameKey="name"
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={100}
                                        paddingAngle={2}
                                    >
                                        {statusBreakdown.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={STATUS_COLORS[entry.name]} />
                                        ))}
                                    </Pie>
                                    <Tooltip 
                                        contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                    />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                </div>

                <div className="text-center mt-12 text-sm text-gray-400">
                    Shared via <a href="/" className="font-medium text-primary-500">JobTrackr</a>
                </div>
            </div>
        </div>
    )
}
