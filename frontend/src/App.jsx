import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import ApplicationsPage from './pages/ApplicationsPage'
import ApplicationDetailPage from './pages/ApplicationDetailPage'
import AnalyticsPage from './pages/AnalyticsPage'
import Layout from './components/Layout'
import PrivateRoute from './components/PrivateRoute'

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route element={<PrivateRoute />}>
                    <Route element={<Layout />}>
                        <Route path="/" element={<Navigate to="/applications" replace />} />
                        <Route path="/applications" element={<ApplicationsPage />} />
                        <Route path="/applications/:id" element={<ApplicationDetailPage />} />
                        <Route path="/analytics" element={<AnalyticsPage />} />
                    </Route>
                </Route>
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}