/**
 * Клиент для файлов логов. Связан с LogFileService.
 */

import {BlobWrapper, getWrapper, postWrapper, ResponseType} from "../utils/client";

export type GetServerLogFileRequest = {
    serverId: number,
    logId: string,
    linesCount: number
}

export const getServerLogFile = (
    request: GetServerLogFileRequest
): Promise<string> =>
    postWrapper('Запрос лога',
        `api/file/log`,
        request,
        ResponseType.TEXT)

export const getEntireLogFile = (
    serverId: number,
    logId: string
): Promise<BlobWrapper> =>
    getWrapper('Скачивание лога',
        `api/file/log/getEntireLogFile/${serverId}/${logId}`,
        ResponseType.BLOB)

export const getLogsArchive = (
    serverId: number
): Promise<BlobWrapper> =>
    getWrapper('Скачивание архива логов',
        `api/file/log/getLogsArchive/${serverId}`,
        ResponseType.BLOB)