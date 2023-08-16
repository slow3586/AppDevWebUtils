import {getWrapper} from "../utils/client";

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
    gpBuild: MuzedoBuildInfo,
    integBuild: MuzedoBuildInfo
}

export type MuzedoBuildInfo = {
    author: string,
    date: string,
    branch: string,
    hash: string
}

export const getServerInfo = (
    serverId: number
): Promise<GetServerInfoResponse> =>
    getWrapper(`api/info/${serverId}`)