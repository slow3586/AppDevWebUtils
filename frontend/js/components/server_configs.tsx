import React, {useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerConfigFile} from "../clients/file_configs_client";
import {isNil} from "lodash";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";

export type ServerConfigsProps = {
    serverId: number
}

export type Command = {
    id: string,
    name: string,
    effect: string
}

export function ServerConfigs({serverId}: ServerConfigsProps) {
    const [configText, setConfigText] = useState("");
    const [configId, setConfigId] = useState("");

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            staleTime: Infinity,
        });

    const configs = frontendConfigQuery?.data?.configs ?? [];
    const requestConfig = () => {
        getServerConfigFile(serverId, configId)
            .then(config => setConfigText(config.text));
    }

    return (
        <div className="comp-server-configs">
            <Form.Control className="comp-bigtextarea"
                          value={configText}
                          readOnly as="textarea" rows={20}/>
            <div className="comp-controls">
                <Form.Text muted>Конфиг</Form.Text>
                <Form.Select onChange={e => {
                                 setConfigId(e.target.value);
                             }}>
                    {(configId == "") ? (<option key="none" value="">Выберите конфиг</option>) : ""}
                    {configs.map(c => (<option key={`k${c.id}`} value={c.id}>{c.id} ({c.name})</option>))}
                </Form.Select>
                <div className="comp-button-container">
                    <Button onClick={requestConfig}
                            variant="primary">Запросить</Button>
                    <Button onClick={requestConfig}
                            variant="primary">Сохранить</Button>
                </div>
            </div>
        </div>
    );
}