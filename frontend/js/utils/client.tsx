const common = (
    path: string,
    options: any,
    textResponse: boolean
): Promise<any> =>
    fetch(path, options)
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
            if (error == "Failed to fetch") {
                throw "Потеряно соединение!";
            }
            throw error;
        })

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