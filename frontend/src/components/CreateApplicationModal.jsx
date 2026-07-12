import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createApplication } from '../api/applications'

const schema = z.object({
    company:       z.string().min(1, 'Company is required').max(255),
    role:          z.string().min(1, 'Role is required').max(255),
    jdText:        z.string().optional(),
    resumeVersion: z.string().max(100).optional(),
    resumeText:    z.string().max(10000).optional(),
    sourceUrl:     z.string().url('Must be a valid URL').optional().or(z.literal('')),
    appliedDate:   z.string().min(1, 'Applied date is required')
})

export default function CreateApplicationModal({ onClose }) {
    const queryClient = useQueryClient()

    const { register, handleSubmit, formState: { errors } } = useForm({
        resolver: zodResolver(schema),
        defaultValues: { appliedDate: new Date().toISOString().split('T')[0] }
    })

    const mutation = useMutation({
        mutationFn: createApplication,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['applications'] })
            onClose()
        }
    })

    const onSubmit = (data) => {
        mutation.mutate({
            ...data,
            sourceUrl: data.sourceUrl || undefined
        })
    }

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                <div className="p-6 border-b">
                    <h2 className="text-xl font-semibold">Add Application</h2>
                </div>

                <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Company *
                            </label>
                            <input
                                {...register('company')}
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                                placeholder="Google"
                            />
                            {errors.company && (
                                <p className="text-red-500 text-xs mt-1">{errors.company.message}</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Role *
                            </label>
                            <input
                                {...register('role')}
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                                placeholder="Backend Engineer"
                            />
                            {errors.role && (
                                <p className="text-red-500 text-xs mt-1">{errors.role.message}</p>
                            )}
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Applied Date *
                            </label>
                            <input
                                type="date"
                                {...register('appliedDate')}
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                            />
                            {errors.appliedDate && (
                                <p className="text-red-500 text-xs mt-1">{errors.appliedDate.message}</p>
                            )}
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">
                                Resume Version
                            </label>
                            <input
                                {...register('resumeVersion')}
                                className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                                placeholder="v3"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Source URL
                        </label>
                        <input
                            {...register('sourceUrl')}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                            placeholder="https://linkedin.com/jobs/..."
                        />
                        {errors.sourceUrl && (
                            <p className="text-red-500 text-xs mt-1">{errors.sourceUrl.message}</p>
                        )}
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Job Description
                            <span className="text-gray-400 font-normal ml-1">(used for AI tag extraction)</span>
                        </label>
                        <textarea
                            {...register('jdText')}
                            rows={4}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                            placeholder="Paste the full job description here..."
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Resume Text
                            <span className="text-gray-400 font-normal ml-1">(used for AI match score)</span>
                        </label>
                        <textarea
                            {...register('resumeText')}
                            rows={4}
                            className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                            placeholder="Paste your resume text here..."
                        />
                        {errors.resumeText && (
                            <p className="text-red-500 text-xs mt-1">{errors.resumeText.message}</p>
                        )}
                    </div>

                    {mutation.isError && (
                        <p className="text-red-500 text-sm">
                            {mutation.error?.response?.data?.detail || 'Failed to create application'}
                        </p>
                    )}

                    <div className="flex justify-end gap-3 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={mutation.isPending}
                            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors"
                        >
                            {mutation.isPending ? 'Adding...' : 'Add Application'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}