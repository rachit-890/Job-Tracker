import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

export function useKeyboardShortcuts(actions) {
    const navigate = useNavigate()

    useEffect(() => {
        const handleKeyDown = (e) => {
            // Don't trigger if user is typing in an input
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') {
                return
            }

            switch (e.key.toLowerCase()) {
                case 'n':
                    if (actions.onNew) {
                        e.preventDefault()
                        actions.onNew()
                    }
                    break
                case 'a':
                    navigate('/applications')
                    break
                case 'r':
                    navigate('/resumes')
                    break
                case 'c':
                    navigate('/analytics')
                    break
                case 'i':
                    if (actions.onImport) {
                        e.preventDefault()
                        actions.onImport()
                    }
                    break
                case '/':
                    if (actions.onSearch) {
                        e.preventDefault()
                        actions.onSearch()
                    }
                    break
                default:
                    break
            }
        }

        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [actions, navigate])
}
