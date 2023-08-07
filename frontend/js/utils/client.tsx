// @ts-ignore
import {toast} from 'react-toastify';
import {startsWith} from "lodash";

const common = (
    path: string,
    options: any,
    textResponse: boolean
): Promise<any> => {
    try {
        return fetch(path, options)
            .then((response) => {
                if (!response.ok) {
                    response.text().then(body => alert(body));
                    throw response;
                }
                return textResponse
                    ? response.text()
                    : response.json() as Promise<any>;
            })
            .catch((error) => {
                console.error(error);
                let text = error.message;
                if (startsWith(error.message, `Failed to fetch`)) {
                    text = "Потеряно соединение, необходимо перезагрузить страницу"
                }
                if (startsWith(error.message, `Unexpected token '<'`)) {
                    text = "Было потеряно и восстановлено соединение, необходимо перезагрузить страницу"
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

export const get = (
    path: string,
    textResponse = false
): Promise<any> =>
    common(path,
        {
            method: 'GET',
            redirect: 'follow'
        },
        textResponse);

export const post = (
    path: string,
    body: any,
    textResponse = false
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
        textResponse)