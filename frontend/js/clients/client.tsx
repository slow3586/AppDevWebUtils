export const getLog = (
    host: string
): Promise<String> => fetch(`api/log`, {
    method: 'GET'
}).then((response) => {
    if (!response.ok) {
        throw new Error(response.statusText)
    }
    return response.text();
})