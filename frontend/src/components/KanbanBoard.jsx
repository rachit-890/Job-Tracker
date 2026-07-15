import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd'
import { Link } from 'react-router-dom'
import { format } from 'date-fns'

const STATUSES = ['APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'STALE']

export default function KanbanBoard({ applications, onStatusChange }) {
    // Group applications by status
    const columns = STATUSES.reduce((acc, status) => {
        acc[status] = applications.filter(app => app.currentStatus === status)
        return acc
    }, {})

    const handleDragEnd = (result) => {
        const { source, destination, draggableId } = result
        
        // Dropped outside a list
        if (!destination) return
        
        // Dropped in same place
        if (source.droppableId === destination.droppableId && source.index === destination.index) return

        const sourceStatus = source.droppableId
        const destStatus = destination.droppableId

        // Dropped in same column (reordering not persisted currently)
        if (sourceStatus === destStatus) return

        // Validate transition
        if (sourceStatus === destStatus) return

        // Trigger update
        onStatusChange(draggableId, destStatus)
    }

    const getColumnBg = (status, isDraggingOver) => {
        if (isDraggingOver) return 'bg-gray-100 dark:bg-gray-800 border-primary-300 dark:border-primary-700'
        return 'bg-gray-50 dark:bg-gray-900/50 border-gray-200 dark:border-gray-800'
    }

    return (
        <DragDropContext onDragEnd={handleDragEnd}>
            <div className="flex gap-4 overflow-x-auto pb-4 h-[calc(100vh-250px)]">
                {STATUSES.map(status => (
                    <Droppable key={status} droppableId={status}>
                        {(provided, snapshot) => (
                            <div
                                ref={provided.innerRef}
                                {...provided.droppableProps}
                                className={`flex flex-col w-72 shrink-0 rounded-xl border transition-colors ${getColumnBg(status, snapshot.isDraggingOver)}`}
                            >
                                <div className="p-3 border-b border-gray-200 dark:border-gray-800 flex justify-between items-center bg-white/50 dark:bg-gray-900/50 rounded-t-xl">
                                    <h3 className="font-semibold text-gray-700 dark:text-gray-300 text-sm">
                                        {status}
                                    </h3>
                                    <span className="bg-gray-200 dark:bg-gray-700 text-gray-600 dark:text-gray-300 px-2 py-0.5 rounded-full text-xs font-medium">
                                        {columns[status].length}
                                    </span>
                                </div>
                                
                                <div className="p-3 flex-1 overflow-y-auto space-y-3">
                                    {columns[status].map((app, index) => (
                                        <Draggable key={app.id} draggableId={app.id} index={index}>
                                            {(provided, snapshot) => (
                                                <div
                                                    ref={provided.innerRef}
                                                    {...provided.draggableProps}
                                                    {...provided.dragHandleProps}
                                                    style={provided.draggableProps.style}
                                                    className={`bg-white dark:bg-gray-800 p-3 rounded-lg border shadow-sm group ${
                                                        snapshot.isDragging 
                                                            ? 'border-primary-400 dark:border-primary-500 shadow-md rotate-2' 
                                                            : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600'
                                                    }`}
                                                >
                                                    <Link to={`/applications/${app.id}`} className="block">
                                                        <h4 className="font-medium text-gray-900 dark:text-gray-100 text-sm truncate">{app.company}</h4>
                                                        <p className="text-gray-500 dark:text-gray-400 text-xs truncate mt-0.5">{app.role}</p>
                                                        
                                                        <div className="flex justify-between items-center mt-3 pt-2 border-t border-gray-100 dark:border-gray-700/50">
                                                            <span className="text-gray-400 dark:text-gray-500 text-[10px]">
                                                                {format(new Date(app.appliedDate), 'MMM d, yyyy')}
                                                            </span>
                                                            {app.matchScore != null && (
                                                                <span className="text-primary-600 dark:text-primary-400 font-medium text-xs">
                                                                    {Math.round(app.matchScore * 100)}% Match
                                                                </span>
                                                            )}
                                                        </div>
                                                    </Link>
                                                </div>
                                            )}
                                        </Draggable>
                                    ))}
                                    {provided.placeholder}
                                </div>
                            </div>
                        )}
                    </Droppable>
                ))}
            </div>
        </DragDropContext>
    )
}
