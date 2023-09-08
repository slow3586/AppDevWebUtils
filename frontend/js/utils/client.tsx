// @ts-ignore
import {toast} from 'react-toastify';
import {startsWith} from 'lodash';

export enum ResponseType {
    TEXT,
    JSON,
    BLOB
}

export type BlobWrapper = {
    filename: string;
    blob: Promise<Blob>;
}

const common = (
    path: string,
    options: any,
    responseType: ResponseType
): Promise<any> => {
    try {
        return fetch(path, options)
            .then((response) => {
                if (!response.ok) {
                    throw response;
                }
                return responseType == ResponseType.TEXT
                    ? response.text()
                    : responseType == ResponseType.JSON
                        ? response.json() as Promise<any>
                        : responseType == ResponseType.BLOB
                            ? new Promise((res, rej) => res({
                                filename: response.headers.get("Content-Disposition"),
                                blob: response.blob()
                            })) as Promise<BlobWrapper>
                            : null;
            })
            .catch((error) => {
                console.error(error);
                let text = error.message;
                if (startsWith(error.message, `Failed to fetch`)) {
                    text = "Потеряна связь, переподключаюсь..."
                }
                if (startsWith(error.message, `Unexpected token '<'`)) {
                    text = "Необходимо перезагрузить страницу"
                }
                toast.warn(text, {
                    toastId: "clienterr",
                    position: "top-right",
                    autoClose: 3000,
                    hideProgressBar: false,
                    closeOnClick: true,
                    pauseOnHover: true,
                    draggable: true,
                    progress: undefined,
                    theme: "light",
                    closeButton: false
                });
                throw error.message;
            })
    } catch (error) {
        console.error(error);
        throw "Необходимо перезагрузить страницу";
    }
}

export const getWrapper = (
    path: string,
    responseType = ResponseType.JSON
): Promise<any> =>
    common(path,
        {
            method: 'GET',
            redirect: 'follow'
        },
        responseType);

export const postWrapper = (
    path: string,
    body: any,
    responseType = ResponseType.JSON
): Promise<any> =>
    common(path,
        {
            method: 'POST',
            body: JSON.stringify(body),
            headers: {
                'Content-Type': 'application/json; charset=utf-8; charset=UTF-8'
            },
            redirect: 'follow'
        },
        responseType)