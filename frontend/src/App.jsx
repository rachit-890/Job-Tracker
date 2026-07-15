import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import SharedApplicationPage from './pages/SharedApplicationPage'
import AdminPage from './pages/AdminPage'
import SettingsPage from './pages/SettingsPage'
import ApplicationsPage from './pages/ApplicationsPage'
import ApplicationDetailPage from './pages/ApplicationDetailPage'
import AnalyticsPage from './pages/AnalyticsPage'
import ResumesPage from './pages/ResumesPage'
import Layout from './components/Layout'
import PrivateRoute from './components/PrivateRoute'

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/shared/:token" element={<SharedApplicationPage />} />
                <Route element={<PrivateRoute />}>
                    <Route element={<Layout />}>
                        <Route path="/" element={<Navigate to="/applications" replace />} />
                        <Route path="/admin" element={<AdminPage />} />
                        <Route path="/settings" element={<SettingsPage />} />
                        <Route path="/applications" element={<ApplicationsPage />} />
                        <Route path="/applications/:id" element={<ApplicationDetailPage />} />
                        <Route path="/resumes" element={<ResumesPage />} />
                        <Route path="/analytics" element={<AnalyticsPage />} />
                    </Route>
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}