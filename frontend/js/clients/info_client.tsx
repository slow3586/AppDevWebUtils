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

export type Command = {
    id: string,
    name: string,
    text: string,
    shell: string,
    blocks: string,
    command: string
}

export type GetServerInfoResponse = {
    wsAdminShell: boolean,
    executingCommand: Command,
    scheduledCommand: Command
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
        response.text().then(body => alert(body));
        throw response;
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
        response.text().then(body => alert(body));
        throw response;
    }
    return response.json() as Promise<GetServerLogResponse>;
})