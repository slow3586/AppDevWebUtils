export enum Severity {
    CRIT = "CRIT",
    INFO = "INFO",
    TRACE = "TRACE"
}

export interface InfoEntry {
    date: Date,
    text: string,
    severity: Severity,
    user: string
}

export const getOverview = (
    last: number
): Promise<Map<number, InfoEntry[]>> => fetch(`api/info/overview/${last}`, {
    method: 'GET'
}).then((response) => {
    if (!response.ok) {
        throw new Error(response.statusText)
    }
    return response.json() as Promise<Map<number, InfoEntry[]>>;
})

export const getServerInfo = (
    serverId: number,
    last: number
): Promise<InfoEntry[]> => fetch(`api/info/server/${serverId}/${last}`, {
    method: 'GET'
}).then((response) => {
    if (!response.ok) {
        throw new Error(response.statusText)
    }
    return response.json() as Promise<InfoEntry[]>;
})