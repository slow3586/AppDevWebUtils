// @ts-ignore
import {toast} from 'react-toastify';
import {startsWith} from 'lodash';

export enum ResponseType {
    NOTHING,
    TEXT,
    JSON,
    BLOB
}

export type BlobWrapper = {
    filename: string;
    blob: Promise<Blob>;
}

const common = (
    requestName: string,
    path: string,
    options: any,
    responseType: ResponseType
): Promise<any> => {
    const showToast = (text: string) => {
        toast.warn('Не удалось выполнить "' + requestName + '": ' + text, {
            toastId: "clienterr",
            position: "top-right",
            autoClose: 5000,
            hideProgressBar: false,
            closeOnClick: true,
            pauseOnHover: true,
            draggable: true,
            progress: undefined,
            theme: "light",
            closeButton: false
        });
    }
    try {
        return fetch(path, options)
            .then((response) => {
                if (!response.ok) {
                    throw response;
                }
                switch (responseType) {
                    case ResponseType.BLOB:
                        return new Promise((res, rej) => res({
                            filename: response.headers.get("Content-Disposition"),
                            blob: response.blob()
                        })) as Promise<BlobWrapper>
                    case ResponseType.TEXT:
                        return response.text()
                    case ResponseType.JSON:
                        return response.json()
                    default:
                        return null
                }
            })
            .catch((err) => {
                err.text().then((errText: any) => {
                    console.error(errText);
                    let errMessage = errText ?? 'Неизвестная ошибка';
                    if (startsWith(errMessage, `Failed to fetch`)) {
                        errMessage = "Потеряна связь, переподключаюсь..."
                    } else if (startsWith(errMessage, `Unexpected token '<'`)) {
                        errMessage = "Необходимо перезагрузить страницу"
                    }
                    showToast(errMessage);
                    throw errMessage;
                });
            })
    } catch (err) {
        console.error(err);
        showToast(err);
        throw err;
    }
}

export const getWrapper = (
    requestName: string,
    path: string,
    responseType = ResponseType.JSON
): Promise<any> =>
    common(requestName,
        path,
        {
            method: 'GET',
            redirect: 'follow'
        },
        responseType);

export const postWrapper = (
    requestName: string,
    path: string,
    body: any,
    responseType = ResponseType.JSON
): Promise<any> =>
    common(requestName,
        path,
        {
            method: 'POST',
            body: JSON.stringify(body),
            headers: {
                'Content-Type': 'application/json; charset=utf-8; charset=UTF-8'
            },
            redirect: 'follow'
        },
        responseType)