import {getWrapper, postWrapper} from "../utils/client";

export type GetServerLogFileRequest = {
    serverId: number,
    logId: string,
    linesCount: number
}

export type GetServerLogFileResponse = {
    text: string
}

export const getServerLogFile = (
    request: GetServerLogFileRequest
): Promise<GetServerLogFileResponse> =>
    postWrapper(`api/file/log`, request)