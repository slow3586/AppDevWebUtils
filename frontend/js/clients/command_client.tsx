/**
 * Клиент для операций. Связан с CommandService.
 */

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

export interface CommandDelayRequest {
    serverId: number,
    comment: string,
    delaySeconds: number
}

export interface CommandCancelRequest {
    serverId: number,
    comment: string
}

export const commandRun = (
    request: CommandRunRequest
): Promise<string> =>
    postWrapper('Запуск операции',
        `api/command/run`,
        request,
        ResponseType.TEXT)

export const commandDelay = (
    request: CommandDelayRequest
): Promise<string> =>
    postWrapper('Отсрочка операции',
        `api/command/delay`,
        request,
        ResponseType.TEXT)

export const commandCancel = (
    request: CommandCancelRequest
): Promise<string> =>
    postWrapper('Отмена операции',
        `api/command/cancel`,
        request,
        ResponseType.TEXT)