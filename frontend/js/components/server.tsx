import React, {useEffect, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, getServerLog, LogEntry} from "../clients/info_client";
import dateFormat from "dateformat";
import {run, Type} from "../clients/command_client";
import {useQuery} from "react-query";
import {isEmpty, trim} from "lodash";

export type ServerProps = {
    isActive: boolean,
    serverId: number
}

export function Server({isActive, serverId}: ServerProps) {
    const logLast = useRef(0);
    const info = useRef("");
    const commandId = useRef("hostname");
    const comment = useRef("");
    const delay = useRef("0");
    let [disableAll, setDisableAll] = useState(false);

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
        run({
            serverId: serverId,
            commandId: commandId.current,
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

    };

    const delayCommand = () => {

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
                             disabled={disableAll}
                             onChange={e => commandId.current = e.target.value}>
                    <option value="hostname">Выберите команду</option>
                    <option value="announce">Оповещение</option>
                    <option value="ra">Рестарт</option>
                    <option value="ura">Обновление</option>
                    <option value="clear_cache">Клир кэш</option>
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
                                      disabled={disableAll}
                                      type="text"
                                      placeholder="0"/>
                    </div>
                    <div className="component-server-container-footer-buttons">
                        <Button disabled={disableAll}
                                onClick={runCommand}
                                variant="primary">Запустить</Button>
                        <Button disabled={disableAll}
                                onClick={delayCommand}
                                variant="primary">Оттянуть</Button>
                        <Button disabled={disableAll}
                                onClick={cancelCommand}
                                variant="primary">Отменить</Button>
                    </div>
                </div>
            </div>
        </div>
    );
}