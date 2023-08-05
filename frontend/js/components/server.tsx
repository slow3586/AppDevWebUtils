import React, {useEffect, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, getServerLog, LogEntry} from "../clients/info_client";
import dateFormat from "dateformat";
import {commandCancel, commandDelay, commandRun, Type} from "../clients/command_client";
import {useQuery} from "react-query";
import {isEmpty, isNil, trim} from "lodash";

export type ServerProps = {
    isActive: boolean,
    serverId: number
}

export type Command = {
    id: string,
    name: string,
    effect: string
}

export function Server({isActive, serverId}: ServerProps) {
    const logLast = useRef(0);
    const info = useRef("");
    const comment = useRef("");
    const delay = useRef("0");
    const [disableAll, setDisableAll] = useState(true);
    const [commandId, setCommandId] = useState("");

    const commands = [{
        id: "announce",
        name: "Оповещение",
        effect: "NONE"
    }, {
        id: "ra",
        name: "Рестарт",
        effect: "WS_BLOCK"
    }, {
        id: "ura",
        name: "Обновление",
        effect: "WS_BLOCK"
    }, {
        id: "clear_cache",
        name: "Клир кэш",
        effect: "SERVER_BLOCK"
    }];
    const getCommand = (id: string) => commands.find(c => c.id == id);

    const query = useQuery(
        ['getServerLog', serverId, logLast.current],
        async () => await getServerLog(serverId, logLast.current),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: isActive
        });

    const addInfo = (add: string) => {
        if (!isEmpty(trim(add))) {
            info.current += add + "\n";
        }
    }
    if (query.isError) {
        // @ts-ignore
        addInfo(error.message);
    }
    if (!query.isLoading && !query.isError) {
        logLast.current = query.data.logLast;
        addInfo(
            query.data.logs.map(e =>
                `${dateFormat(e.date, "hh:MM:ss")} [${e.severity}] [${e.user}] ${e.text}`
            ).join("\n")
        );
    }

    const runCommand = () => {
        setDisableAll(true);
        commandRun({
            serverId: serverId,
            commandId: commandId,
            comment: comment.current,
            delaySeconds: parseInt(delay.current)
        }).then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
            alert(e);
        }).finally(() => setDisableAll(false))
    };

    const cancelCommand = () => {
        commandCancel({
            serverId: serverId,
            comment: comment.current
        }).then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
            alert(e);
        }).finally(() => setDisableAll(false))
    };

    const delayCommand = () => {
        commandDelay({
            serverId: serverId,
            comment: comment.current,
            delaySeconds: parseInt(delay.current)
        }).then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
            alert(e);
        }).finally(() => setDisableAll(false))
    };

    return (
        <div className="component-server">
            <div className="component-server-container">
                <Form.Text>{serverId}</Form.Text>
                <Form.Control className="component-server-container-textarea"
                              value={info.current}
                              readOnly as="textarea" rows={10}/>

                <Form.Text muted>Команда</Form.Text>
                <Form.Select aria-label="Выбор команды"
                             onChange={e => {
                                 setCommandId(e.target.value);
                                 setDisableAll(isNil(getCommand(e.target.value)));
                             }}>
                    {(commandId == "") ? (<option value="">Выберите команду</option>) : ""}
                    {commands.map(c => (<option value={c.id}>{c.name}</option>))}
                </Form.Select>

                <Form.Text muted>Комментарий</Form.Text>
                <Form.Control onChange={e => comment.current = e.target.value}
                              disabled={disableAll}
                              type="text"
                              placeholder=""/>

                <div className="component-server-container-footer">
                    <div className="component-server-container-footer-delay">
                        <Form.Text muted>Задержка (сек)</Form.Text>
                        <Form.Control onChange={e => delay.current = e.target.value}
                                      className="component-server-container-footer-delay-textarea"
                                      disabled={disableAll ||
                                          isNil(getCommand(commandId)) ||
                                          getCommand(commandId).effect == "NONE"}
                                      type="text"
                                      placeholder="0"/>
                    </div>
                    <div className="component-server-container-footer-buttons">
                        <Button disabled={disableAll}
                                onClick={runCommand}
                                variant="primary">Запустить</Button>
                        <Button onClick={delayCommand}
                                variant="primary">Отложить</Button>
                        <Button onClick={cancelCommand}
                                variant="primary">Отменить</Button>
                    </div>
                </div>
            </div>
        </div>
    );
}