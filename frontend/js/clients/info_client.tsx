import {get} from "../utils/client";

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
    executingCommandTimer: number,
    scheduledCommand: Command,
    scheduledCommandTimer: number,
    build: string,
    gpBuild: string,
    integBuild: string
}

export type GetServerLogResponse = {
    logs: LogEntry[],
    logLast: number
}

export const getServerInfo = (
    serverId: number
): Promise<GetServerInfoResponse> =>
    get(`api/info/getServerInfo/${serverId}`)

export const getServerLog = (
    serverId: number,
    last: number
): Promise<GetServerLogResponse> =>
    get(`api/info/getServerLog/${serverId}?last=${last}`)