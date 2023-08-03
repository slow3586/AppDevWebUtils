import React, {useEffect, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, InfoEntry} from "../clients/client";
import dateFormat from "dateformat";

export type ServerProps = {
    host: number
}

export function Server({host}: ServerProps) {
    let [last, setLast] = useState(0);
    let [info, setInfo] = useState("");

    const fetchInfo = () => {
        getServerInfo(61, last).then((response: InfoEntry[]) => {
            setInfo(
                response.map(e =>
                    `${dateFormat(e.date, "hh:MM:ss")} [${e.severity}] ${e.user} ${e.text}`
                ).join("\n")
            );
        });
    }

    useEffect(() => {
        const interval =
            setInterval(() => fetchInfo(), 3000);
    }, []);

    return (
        <div className="component-server">
            <div className="component-server-container">
                <Form.Text>{host}</Form.Text>
                <Form.Control className="component-server-container-textarea"
                              value={info}
                              readOnly as="textarea" rows={10}/>

                <Form.Text muted>Команда</Form.Text>
                <Form.Select aria-label="Выбор команды">
                    <option value="1">Рестарт</option>
                    <option value="2">Обновление</option>
                    <option value="3">Клир кэш</option>
                </Form.Select>

                <Form.Text muted>Комментарий</Form.Text>
                <Form.Control type="text" placeholder=""/>

                <div className="component-server-container-footer">
                    <div className="component-server-container-footer-delay">
                        <Form.Text muted>Задержка (сек)</Form.Text>
                        <Form.Control className="component-server-container-footer-delay-textarea"
                                      type="text" placeholder=""/>
                    </div>
                    <div className="component-server-container-footer-buttons">
                        <Button variant="primary">Запустить</Button>
                        <Button variant="primary">Оттянуть</Button>
                        <Button variant="primary">Отменить</Button>
                    </div>
                </div>
            </div>
        </div>
    );
}