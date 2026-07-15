import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { fetchGoalProgress, setGoal } from '../api/goals'

export default function GoalProgressWidget() {
    const queryClient = useQueryClient()
    const [isEditing, setIsEditing] = useState(false)
    const [targetCount, setTargetCount] = useState(10)
    const [period, setPeriod] = useState('WEEKLY')

    const { data: progress, isLoading } = useQuery({
        queryKey: ['goalProgress'],
        queryFn: fetchGoalProgress
    })

    const mutation = useMutation({
        mutationFn: ({ count, p }) => setGoal(count, p),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['goalProgress'] })
            setIsEditing(false)
        }
    })

    if (isLoading) return null

    if (isEditing || !progress) {
        return (
            <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 shadow-sm">
                <h3 className="font-semibold text-gray-900 dark:text-gray-100 mb-3 text-sm">
                    {progress ? 'Edit Goal' : 'Set Application Goal'}
                </h3>
                <div className="flex gap-2 items-center">
                    <input
                        type="number"
                        min="1"
                        value={targetCount}
                        onChange={e => setTargetCount(Number(e.target.value))}
                        className="w-20 border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
                    />
                    <select
                        value={period}
                        onChange={e => setPeriod(e.target.value)}
                        className="border border-gray-300 rounded-lg px-2 py-1.5 text-sm"
                    >
                        <option value="WEEKLY">per week</option>
                        <option value="MONTHLY">per month</option>
                    </select>
                    <button
                        onClick={() => mutation.mutate({ count: targetCount, p: period })}
                        disabled={mutation.isPending}
                        className="bg-primary-600 text-white px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-primary-700 transition-colors"
                    >
                        Save
                    </button>
                    {progress && (
                        <button
                            onClick={() => setIsEditing(false)}
                            className="text-gray-500 hover:text-gray-700 dark:text-gray-300 text-sm px-2"
                        >
                            Cancel
                        </button>
                    )}
                </div>
            </div>
        )
    }

    const percentage = Math.min(100, progress.progressPercentage)
    const isCompleted = percentage >= 100

    return (
        <div className="bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4 shadow-sm group">
            <div className="flex justify-between items-start mb-2">
                <div>
                    <h3 className="font-semibold text-gray-900 dark:text-gray-100 text-sm">
                        {progress.period === 'WEEKLY' ? 'Weekly' : 'Monthly'} Goal
                    </h3>
                    <p className="text-gray-500 text-xs">
                        {progress.currentCount} / {progress.targetCount} applications
                    </p>
                </div>
                <button
                    onClick={() => {
                        setTargetCount(progress.targetCount)
                        setPeriod(progress.period)
                        setIsEditing(true)
                    }}
                    className="opacity-0 group-hover:opacity-100 text-gray-400 hover:text-primary-600 text-xs transition-opacity"
                >
                    Edit
                </button>
            </div>
            
            <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden mt-3">
                <div 
                    className={`h-full rounded-full transition-all duration-500 ${isCompleted ? 'bg-green-500' : 'bg-primary-500'}`}
                    style={{ width: `${percentage}%` }}
                />
            </div>
            {isCompleted && (
                <p className="text-green-600 text-xs mt-2 font-medium">
                    🎉 Goal reached! Great job!
                </p>
            )}
        </div>
    )
}
