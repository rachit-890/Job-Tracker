import client from './client'

export const fetchResumes = async () => {
    const { data } = await client.get('/resumes')
    return data
}

export const fetchResume = async (id) => {
    const { data } = await client.get(`/resumes/${id}`)
    return data
}

export const createResume = async (payload) => {
    const { data } = await client.post('/resumes', payload)
    return data
}

export const deleteResume = async (id) => {
    await client.delete(`/resumes/${id}`)
}
