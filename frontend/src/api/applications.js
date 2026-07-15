import client from './client'

export const fetchApplications = async (params) => {
    const { data } = await client.get('/applications', { params })
    return data
}

export const fetchApplication = async (id) => {
    const { data } = await client.get(`/applications/${id}`)
    return data
}

export const createApplication = async (payload) => {
    const { data } = await client.post('/applications', payload)
    return data
}

export const updateApplication = async ({ id, ...payload }) => {
    const { data } = await client.patch(`/applications/${id}`, payload)
    return data
}

export const updateStatus = async ({ id, newStatus }) => {
    const { data } = await client.patch(`/applications/${id}/status`, { newStatus })
    return data
}

export const deleteApplication = async (id) => {
    await client.delete(`/applications/${id}`)
}

export const exportCsv = async () => {
    const response = await client.get('/export/csv', { responseType: 'blob' })
    return response.data
}

export const exportPdf = async () => {
    const response = await client.get('/export/pdf', { responseType: 'blob' })
    return response.data
}

export const createShareToken = async () => {
    const { data } = await client.post(`/applications/share-token`)
    return data
}

export const revokeShareToken = async (token) => {
    await client.delete(`/applications/share-token/${token}`)
}