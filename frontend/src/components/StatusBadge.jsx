const STATUS_STYLES = {
    APPLIED:    'bg-blue-100 text-blue-800',
    SCREENING:  'bg-yellow-100 text-yellow-800',
    INTERVIEW:  'bg-purple-100 text-purple-800',
    OFFER:      'bg-green-100 text-green-800',
    REJECTED:   'bg-red-100 text-red-800',
    STALE:      'bg-gray-100 text-gray-600'
}

export default function StatusBadge({ status }) {
    return (
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${STATUS_STYLES[status] ?? 'bg-gray-100 text-gray-600'}`}>
      {status}
    </span>
    )
}