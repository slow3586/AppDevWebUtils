import React, {useEffect, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, InfoEntry} from "../clients/info_client";
import dateFormat from "dateformat";
import {run, Type} from "../clients/command_client";

export type ServerProps = {
    serverId: number
}

export function Server({serverId}: ServerProps) {
    let [last, setLast] = useState(0);
    let [info, setInfo] = useState("");
    let [commandId, setCommandId] = useState("hostname");
    let [comment, setComment] = useState("");
    let [delay, setDelay] = useState("0");
    let [disableAll, setDisableAll] = useState(false);

    const fetchInfo = () => {
        getServerInfo(serverId, last).then((response: InfoEntry[]) => {
            setInfo(
                response.map(e =>
                    `${dateFormat(e.date, "hh:MM:ss")} [${e.severity}] [${e.user}] ${e.text}`
                ).join("\n")
            );
        });
    }

    useEffect(() => {
        const interval =
            setInterval(() => fetchInfo(), 3000);
    }, []);

    const runCommand = () => {
        setDisableAll(true);
        run({
            serverId: serverId,
            commandId: commandId,
            comment: comment,
            delaySeconds: parseInt(delay)
        }).then((r) => {
            console.log(r);
            alert(r);
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
                              value={info}
                              readOnly as="textarea" rows={10}/>

                <Form.Text muted>Команда</Form.Text>
                <Form.Select aria-label="Выбор команды"
                             disabled={disableAll}
                             onChange={e => setCommandId(e.target.value)}>
                    <option value="hostname">Проверка</option>
                    <option value="ra">Рестарт</option>
                    <option value="ura">Обновление</option>
                    <option value="clear_cache">Клир кэш</option>
                </Form.Select>

                <Form.Text muted>Комментарий</Form.Text>
                <Form.Control onChange={e => setComment(e.target.value)}
                              disabled={disableAll}
                              type="text"
                              placeholder=""/>

                <div className="component-server-container-footer">
                    <div className="component-server-container-footer-delay">
                        <Form.Text muted>Задержка (сек)</Form.Text>
                        <Form.Control onChange={e => setDelay(e.target.value)}
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