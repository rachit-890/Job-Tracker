import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../hooks/useTheme'

export default function Layout() {
    const { logout } = useAuth()
    const { theme, toggleTheme } = useTheme()

    return (
        <div className="min-h-screen flex flex-col bg-gray-50 dark:bg-gray-900 dark:bg-gray-900 transition-colors duration-200">
            <nav className="bg-primary-900 dark:bg-gray-800 text-white shadow-lg">
                <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
                    <div className="flex items-center gap-8">
            <span className="text-xl font-bold tracking-tight">
              🎯 JobTrackr
            </span>
                        <div className="flex gap-1">
                            <NavLink
                                to="/applications"
                                className={({ isActive }) =>
                                    `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary-700 text-white'
                                            : 'text-gray-300 hover:bg-primary-800 hover:text-white'
                                    }`
                                }
                            >
                                Applications
                            </NavLink>
                            <NavLink
                                to="/resumes"
                                className={({ isActive }) =>
                                    `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary-700 text-white'
                                            : 'text-gray-300 hover:bg-primary-800 hover:text-white'
                                    }`
                                }
                            >
                                Resumes
                            </NavLink>
                            <NavLink
                                to="/analytics"
                                className={({ isActive }) =>
                                    `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary-700 text-white'
                                            : 'text-gray-300 hover:bg-primary-800 hover:text-white'
                                    }`
                                }
                            >
                                Analytics
                            </NavLink>
                            <NavLink
                                to="/admin"
                                className={({ isActive }) =>
                                    `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary-700 text-white'
                                            : 'text-gray-300 hover:bg-primary-800 hover:text-white'
                                    }`
                                }
                            >
                                Admin
                            </NavLink>
                            <NavLink
                                to="/settings"
                                className={({ isActive }) =>
                                    `px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                                        isActive
                                            ? 'bg-primary-700 text-white'
                                            : 'text-gray-300 hover:bg-primary-800 hover:text-white'
                                    }`
                                }
                            >
                                Settings
                            </NavLink>
                        </div>
                    </div>
                    <div className="flex items-center gap-6">
                        <button
                            onClick={toggleTheme}
                            className="text-gray-300 hover:text-white transition-colors"
                            aria-label="Toggle dark mode"
                        >
                            {theme === 'dark' ? (
                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
                                </svg>
                            ) : (
                                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                                </svg>
                            )}
                        </button>
                        <button
                            onClick={logout}
                            className="text-sm text-gray-300 hover:text-white transition-colors"
                        >
                            Sign out
                        </button>
                    </div>
                </div>
            </nav>

            <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8">
                <Outlet />
            </main>
        </div>
    )
}