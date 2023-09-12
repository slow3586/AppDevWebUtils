import {getWrapper, postWrapper, ResponseType} from "../utils/client";

export type GetServerConfigFileResponse = {
    text: string
}

export type SaveServerConfigFileRequest = {
    serverId: number,
    configId: string,
    configText: string,
    comment: string,
    skipAnalysis: boolean
}

export const getServerConfigFile = (
    serverId: number,
    configId: string
): Promise<string> =>
    getWrapper('Запрос конфига',
        `api/file/config/${serverId}/${configId}`,
        ResponseType.TEXT)

export const saveServerConfigFile = (
    saveServerConfigFileRequest: SaveServerConfigFileRequest
): Promise<string> =>
    postWrapper('Сохранение конфига',
        `api/file/config/save`,
        saveServerConfigFileRequest,
        ResponseType.TEXT)