import {getWrapper} from "../utils/client";

export type GetServerConfigFileResponse = {
    text: string
}

export const getServerConfigFile = (
    serverId: number,
    configId: string
): Promise<GetServerConfigFileResponse> =>
    getWrapper(`api/file/config/${serverId}/${configId}`)