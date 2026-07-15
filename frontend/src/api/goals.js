import client from './client'

export const fetchGoalProgress = async () => {
    try {
        const { data } = await client.get('/goals/progress')
        return data
    } catch (e) {
        if (e.response?.status === 204) return null
        throw e
    }
}

export const setGoal = async (targetCount, period) => {
    await client.post('/goals', null, { params: { targetCount, period } })
}
