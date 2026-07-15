import client from './client'

export const fetchAnalyticsSummary = async () => {
    const { data } = await client.get('/analytics/summary')
    return data
}

export const fetchResumePerformance = async () => {
    const { data } = await client.get('/analytics/resume-performance')
    return data
}

export const fetchCompanyAnalytics = async () => {
    const { data } = await client.get('/analytics/company')
    return data
}

export const fetchTrend = async (range = '30d') => {
    const { data } = await client.get('/analytics/trend', { params: { range } })
    return data
}

export const fetchStatusFlow = async () => {
    const { data } = await client.get('/analytics/status-flow')
    return data
}

export const fetchDayOfWeek = async () => {
    const { data } = await client.get('/analytics/day-of-week')
    return data
}