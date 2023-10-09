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
    appBuildText: string,
    moduleBuildInfoList: ModuleBuildInfo[]
}

export type ModuleBuildInfo = {
    name: string,
    buildText: string
}

export const getServerInfo = (
    serverId: number
): Promise<GetServerInfoResponse> =>
    getWrapper('Запрос статуса стенда',
        `api/info/${serverId}`)