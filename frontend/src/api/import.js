import client from './client'

export const uploadCsv = async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    
    const { data } = await client.post('/applications/import', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    return data
}
