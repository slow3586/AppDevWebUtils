import React, {useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerConfigFile} from "../clients/file_configs_client";
import {getServerLogFile} from "../clients/file_logs_client";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {toInteger} from "lodash";

export type ServerLogsProps = {
    serverId: number
}

export type Command = {
    id: string,
    name: string,
    effect: string
}

export function ServerLogs({serverId}: ServerLogsProps) {
    const [logText, setLogText] = useState("");
    const [logId, setLogId] = useState("");
    const linesCount = useRef("25");

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            staleTime: Infinity,
        });

    const logs = frontendConfigQuery?.data?.logs ?? [];
    const requestLog = () => {
        getServerLogFile({
            serverId,
            logId,
            linesCount: toInteger(linesCount.current)
        }).then(log => setLogText(log.text));
    }

    return (
        <div className="comp-server-logs">
            <Form.Group>
                <Form.Control className="comp-bigtextarea"
                              value={logText}
                              readOnly as="textarea" rows={20}/>
            </Form.Group>
            <div className="comp-controls">
                <Form.Text muted>Лог</Form.Text>
                <Form.Select onChange={e => {
                    setLogId(e.target.value);
                }}>
                    {(logId == "") ? (<option key="none" value="">Выберите лог</option>) : ""}
                    {logs.map(c => (<option key={`k${c.id}`} value={c.id}>{c.id} ({c.name})</option>))}
                </Form.Select>
                <Form.Text muted>Кол-во строк</Form.Text>
                <Form.Control onChange={e => linesCount.current = e.target.value}
                              className="comp-textarea"
                              type="number"
                              min="0"
                              max="600"
                              placeholder="25"/>
                <div className="comp-button-container">
                    <Button onClick={requestLog}
                            variant="primary">Запросить</Button>
                </div>
            </div>
        </div>
    );
}