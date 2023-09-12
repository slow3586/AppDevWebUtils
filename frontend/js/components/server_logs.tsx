import React, {useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getEntireLogFile, getLogsArchive, getServerLogFile} from "../clients/file_logs_client";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {isEmpty, toInteger} from "lodash";
import download from "downloadjs";
import {toast} from "react-toastify";

export type ServerLogsProps = {
    serverId: number
}

export type Command = {
    id: string,
    name: string,
    effect: string
}

const DEFAULT_LINES_COUNT = "25";
const MAX_LINES_COUNT = 1000;

export function ServerLogs({serverId}: ServerLogsProps) {
    const [logText, setLogText] = useState("");
    const [logId, setLogId] = useState("");
    const linesCount = useRef(DEFAULT_LINES_COUNT);
    const [disableAll, setDisableAll] = useState(false);

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            staleTime: Infinity,
        });

    const logs = frontendConfigQuery?.data?.logs ?? [];
    const showToast = (
        text: string,
        time: number
    ) => {
        toast.dismiss("requestToast");
        return toast(text, {
            toastId: "requestToast",
            position: "top-right",
            autoClose: time * 1000,
            hideProgressBar: false,
            closeOnClick: true,
            pauseOnHover: true,
            draggable: true,
            progress: undefined,
            theme: "light",
            closeButton: false
        });
    }
    const requestWrapper = (
        toastText: string,
        toastTime: number,
        request: Promise<any>
    ) => {
        setDisableAll(true);
        if (!isEmpty(toastText)) {
            showToast(toastText, toastTime);
        }
        return request.then(() => {
            if (!isEmpty(toastText)) {
                showToast("Успешно!", 5);
            }
        }).finally(() => {
            setDisableAll(false);
        })
    }
    const requestLog = () => {
        return requestWrapper(
            "",
            0,
            getServerLogFile({
                serverId,
                logId,
                linesCount: toInteger(linesCount.current)
            }).then(logText => setLogText(logText)));
    }
    const requestEntireLogFile = () => {
        return requestWrapper(
            "Подготавливаю лог...",
            3,
            getEntireLogFile(serverId, logId).then(
                blobWrapper => blobWrapper.blob.then((blob) => {
                        download(blob, blobWrapper.filename, "application/zip");
                    }
                )
            ));
    }
    const requestLogsArchive = () => {
        return requestWrapper(
            "Подготавливаю архив...",
            15,
            getLogsArchive(serverId).then(
                blobWrapper => blobWrapper.blob.then((blob) => {
                        download(blob, blobWrapper.filename, "application/zip");
                    }
                )
            ));
    }

    return (
        <div className="comp-server-logs">
            <Form.Control className="comp-bigtextarea"
                          value={logText}
                          readOnly
                          as="textarea"
                          rows={30}/>
            <div className="comp-controls">
                <Form.Text muted>Лог</Form.Text>
                <Form.Select onChange={e => {
                    setLogId(e.target.value);
                }}>
                    {(logId == "") ? (<option key="none" value="">Выберите лог</option>) : ""}
                    {logs.map(c => (<option key={`k${c.id}`} value={c.id}>{c.id}</option>))}
                </Form.Select>
                <Form.Text muted>Кол-во строк</Form.Text>
                <Form.Control onChange={e => linesCount.current = e.target.value}
                              className="comp-textarea"
                              type="number"
                              min="1"
                              max={MAX_LINES_COUNT}
                              placeholder={DEFAULT_LINES_COUNT}/>
                <div className="comp-button-container">
                    <Button onClick={requestLog}
                            disabled={isEmpty(logId)
                                || disableAll}
                            variant="primary">Запросить</Button>
                    <Button onClick={requestEntireLogFile}
                            disabled={isEmpty(logId)
                                || disableAll}
                            variant="primary">Скачать</Button>
                    <Button onClick={requestLogsArchive}
                            disabled={disableAll}
                            variant="primary">Архив логов</Button>
                </div>
            </div>
        </div>
    );
}