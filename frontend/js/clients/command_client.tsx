import {post} from "../utils/client";

export enum Type {
    WSADMIN = "WSADMIN",
    SSH = "SSH"
}

export interface CommandRunRequest {
    serverId: number,
    commandId: string,
    comment: string,
    delaySeconds: number
}

export const commandRun = (
    request: CommandRunRequest
): Promise<string> =>
    post(`api/command/run`, request, true)

export interface CommandDelayRequest {
    serverId: number,
    comment: string,
    delaySeconds: number
}

export const commandDelay = (
    request: CommandDelayRequest
): Promise<string> =>
    post(`api/command/delay`, request, true)

export interface CommandCancelRequest {
    serverId: number,
    comment: string
}

export const commandCancel = (
    request: CommandCancelRequest
): Promise<string> =>
    post(`api/command/cancel`, request, true)