import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function Layout() {
    const { logout } = useAuth()

    return (
        <div className="min-h-screen flex flex-col">
            <nav className="bg-primary-900 text-white shadow-lg">
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
                        </div>
                    </div>
                    <button
                        onClick={logout}
                        className="text-sm text-gray-300 hover:text-white transition-colors"
                    >
                        Sign out
                    </button>
                </div>
            </nav>

            <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-8">
                <Outlet />
            </main>
        </div>
    )
}