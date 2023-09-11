import {postWrapper, ResponseType} from "../utils/client";

export enum Type {
    WSADMIN = "WSADMIN",
    SSH = "SSH"
}

export type CommandRunRequest = {
    serverId: number,
    commandId: string,
    comment: string,
    delaySeconds: number
}

export const commandRun = (
    request: CommandRunRequest
): Promise<string> =>
    postWrapper(`api/command/run`, request, ResponseType.TEXT)

export interface CommandDelayRequest {
    serverId: number,
    comment: string,
    delaySeconds: number
}

export const commandDelay = (
    request: CommandDelayRequest
): Promise<string> =>
    postWrapper(`api/command/delay`, request, ResponseType.TEXT)

export interface CommandCancelRequest {
    serverId: number,
    comment: string
}

export const commandCancel = (
    request: CommandCancelRequest
): Promise<string> =>
    postWrapper(`api/command/cancel`, request, ResponseType.TEXT)