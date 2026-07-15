import client from './client'

export const fetchNotes = async (applicationId) => {
    const { data } = await client.get(`/applications/${applicationId}/notes`)
    return data
}

export const addNote = async ({ applicationId, content }) => {
    const { data } = await client.post(`/applications/${applicationId}/notes`, { content })
    return data
}

export const deleteNote = async ({ applicationId, noteId }) => {
    await client.delete(`/applications/${applicationId}/notes/${noteId}`)
}
