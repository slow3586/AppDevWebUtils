export enum Severity {
    CRIT = "CRIT",
    INFO = "INFO",
    TRACE = "TRACE"
}

export type LogEntry = {
    date: Date,
    text: string,
    severity: Severity,
    user: string
}

export type GetServerInfoRequest = {
    serverId: number
}

export type GetServerLogRequest = {
    serverId: number,
    logLast: number
}

export type GetServerInfoResponse = {
    wsAdminShell: boolean,
    currentOperation: string
}

export type GetServerLogResponse = {
    logs: LogEntry[],
    logLast: number
}

export const getServerInfo = (
    serverId: number
): Promise<GetServerInfoResponse> => fetch(`api/info/getServerInfo/${serverId}`, {
    method: 'GET'
}).then((response) => {
    if (!response.ok) {
        throw new Error(response.statusText)
    }
    return response.json() as Promise<GetServerInfoResponse>;
})

export const getServerLog = (
    serverId: number,
    last: number
): Promise<GetServerLogResponse> => fetch(`api/info/getServerLog/${serverId}?last=${last}`, {
    method: 'GET'
}).then((response) => {
    if (!response.ok) {
        throw new Error(response.statusText)
    }
    return response.json() as Promise<GetServerLogResponse>;
})