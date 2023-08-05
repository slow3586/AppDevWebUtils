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
    fetch(`api/command/run`, {
        method: 'POST',
        body: JSON.stringify(request),
        headers: {
            'Content-Type': 'application/json; charset=utf-8; charset=UTF-8'
        }
    }).then((response) => {
        if (!response.ok) {
            throw new Error(response.statusText)
        }
        return response.text() as Promise<string>;
    })

export interface CommandDelayRequest {
    serverId: number,
    comment: string,
    delaySeconds: number
}

export const commandDelay = (
    request: CommandDelayRequest
): Promise<string> =>
    fetch(`api/command/delay`, {
        method: 'POST',
        body: JSON.stringify(request),
        headers: {
            'Content-Type': 'application/json; charset=utf-8; charset=UTF-8'
        }
    }).then((response) => {
        if (!response.ok) {
            throw new Error(response.statusText)
        }
        return response.text() as Promise<string>;
    })

export interface CommandCancelRequest {
    serverId: number,
    comment: string
}

export const commandCancel = (
    request: CommandCancelRequest
): Promise<string> =>
    fetch(`api/command/cancel`, {
        method: 'POST',
        body: JSON.stringify(request),
        headers: {
            'Content-Type': 'application/json; charset=utf-8; charset=UTF-8'
        }
    }).then((response) => {
        if (!response.ok) {
            throw new Error(response.statusText)
        }
        return response.text() as Promise<string>;
    })