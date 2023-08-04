export enum Type {
    WSADMIN = "WSADMIN",
    SSH = "SSH"
}

export interface RunCommandRequest {
    serverId: number,
    commandId: string,
    comment: string,
    delaySeconds: number
}

export const run = (
    request: RunCommandRequest
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