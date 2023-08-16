import {getWrapper} from "../utils/client";

export enum Severity {
    CRIT = "CRIT",
    INFO = "INFO",
    TRACE = "TRACE"
}

export type HistoryEntry = {
    date: Date,
    text: string,
    severity: Severity,
    user: string
}

export type GetServerHistoryResponse = {
    logs: HistoryEntry[],
    logLast: number
}

export const getServerHistory = (
    serverId: number,
    last: number
): Promise<GetServerHistoryResponse> =>
    getWrapper(`api/history/${serverId}?last=${last}`)