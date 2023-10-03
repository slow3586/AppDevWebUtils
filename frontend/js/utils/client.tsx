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
            .catch((err) => handleErr(requestName, err))
    } catch (err) {
        handleErr(requestName, err);
    }
}

const showErrToast = (
    requestName: string,
    text: string
) => {
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

const handleErr = (
    requestName: string,
    err: any
) => {
    console.error(err);

    const errTextPromise = err?.text instanceof Function
        ? err.text()
        : new Promise((res, rej) => res(err.message));

    errTextPromise.then((errText: string) => {
        errText = errText ?? 'Неизвестная ошибка';
        if (startsWith(errText, `Failed to fetch`)) {
            errText = "Потеряна связь, переподключаюсь...";
        } else if (startsWith(errText, `Unexpected token '<'`)) {
            errText = "Необходимо перезагрузить страницу";
        }
        showErrToast(requestName, errText);
    });

    throw err;
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