import {BlobWrapper, getWrapper, postWrapper, ResponseType} from "../utils/client";

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

export const getEntireLogFile = (
    serverId: number,
    logId: string
): Promise<BlobWrapper> =>
    getWrapper(`api/file/log/getEntireLogFile/${serverId}/${logId}`, ResponseType.BLOB)

export const getLogsArchive = (
    serverId: number
): Promise<BlobWrapper> =>
    getWrapper(`api/file/log/getLogsArchive/${serverId}`, ResponseType.BLOB)