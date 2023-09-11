import React, {useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getEntireLogFile, getLogsArchive, getServerLogFile} from "../clients/file_logs_client";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {isEmpty, toInteger} from "lodash";
import download from "downloadjs";

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
    const requestLog = () => {
        setDisableAll(true);
        getServerLogFile({
            serverId,
            logId,
            linesCount: toInteger(linesCount.current)
        }).then(log => setLogText(log.text)
        ).finally(() => setDisableAll(false));
    }
    const requestEntireLogFile = () => {
        setDisableAll(true);
        getEntireLogFile(serverId, logId).then(
            blobWrapper => blobWrapper.blob.then((blob) => {
                    download(blob, blobWrapper.filename, "application/zip");
                }
            )
        ).finally(() => setDisableAll(false));
    }
    const requestLogsArchive = () => {
        setDisableAll(true);
        getLogsArchive(serverId).then(
            blobWrapper => blobWrapper.blob.then((blob) => {
                    download(blob, blobWrapper.filename, "application/zip");
                }
            )
        ).finally(() => setDisableAll(false));
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