import React, {useRef} from "react";
import {Button, Form} from "react-bootstrap";

export type ServerConfigsProps = {
    serverId: number
}

export type Command = {
    id: string,
    name: string,
    effect: string
}

export function ServerConfigs({serverId}: ServerConfigsProps) {
    const info = useRef("");

    return (
        <div className="comp-server-configs">
            <Form.Group>
                <Form.Control className="comp-textarea"
                              value={info.current}
                              readOnly as="textarea" rows={20}/>
            </Form.Group>
            <Form.Text muted>Конфиг</Form.Text>
            <Form.Select aria-label="Выбор конфига">
                <option key="integ" value="integ">UZDO-integration.log</option>
            </Form.Select>
            <Form.Text muted>Кол-во строк</Form.Text>
            <Form.Control className="comp-textarea"
                          type="number"
                          min="0"
                          max="600"
                          placeholder="0"/>
            <Button variant="primary">Запросить</Button>
        </div>
    );
}