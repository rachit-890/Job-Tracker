import axios from 'axios'

// Create a separate axios instance without auth headers
const publicClient = axios.create({
    baseURL: (import.meta.env.VITE_API_URL || '') + '/api/v1/public'
})

export const fetchSharedAnalytics = async (token) => {
    const { data } = await publicClient.get(`/${token}/analytics`)
    return data
}
