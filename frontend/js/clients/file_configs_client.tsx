import {getWrapper, postWrapper} from "../utils/client";

export type GetServerConfigFileResponse = {
    text: string
}

export type SaveServerConfigFileRequest = {
    serverId: number,
    configId: String,
    configText: String
}

export const getServerConfigFile = (
    serverId: number,
    configId: string
): Promise<GetServerConfigFileResponse> =>
    getWrapper(`api/file/config/${serverId}/${configId}`)

export const saveServerConfigFile = (
    saveServerConfigFileRequest: SaveServerConfigFileRequest
): Promise<GetServerConfigFileResponse> =>
    postWrapper(`api/file/config/save`, saveServerConfigFileRequest)