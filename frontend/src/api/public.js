import axios from 'axios'

// Create a separate axios instance without auth headers
const publicClient = axios.create({
    baseURL: 'http://localhost:8080/api/v1/public'
})

export const fetchSharedAnalytics = async (token) => {
    const { data } = await publicClient.get(`/${token}/analytics`)
    return data
}
