import client from './client'

export const login = async ({ username, password }) => {
    const { data } = await client.post('/auth/login', { username, password })
    return data
}

export const register = async ({ username, password }) => {
    const { data } = await client.post('/auth/register', { username, password })
    return data
}