import React, {useRef, useState} from "react";
import {Button, Form, OverlayTrigger, Tooltip} from "react-bootstrap";
import {getServerConfigFile, saveServerConfigFile} from "../clients/file_configs_client";
import {isEmpty} from "lodash";
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
    const comment = useRef("");
    const skipAnalysis = useRef(false);
    const [disableAll, setDisableAll] = useState(false);
    const textArea: React.MutableRefObject<any> = useRef();

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            staleTime: Infinity,
            enabled: !disableAll
        });

    const configs = frontendConfigQuery?.data?.configs ?? [];
    const requestConfig = () => {
        setDisableAll(true);
        return getServerConfigFile(serverId, configId)
            .then(config => {
                config.text?.replace?.("\r\r\n", "\n");
                setConfigText(config.text);
                textArea.current.value = config.text;
            })
            .finally(() => {
                setDisableAll(false);
            });
    }

    const saveConfig = () => {
        setDisableAll(true);
        return saveServerConfigFile({
            serverId,
            configId,
            configText,
            comment: comment.current,
            skipAnalysis: skipAnalysis.current
        }).finally(() => {
            requestConfig().finally(() => setDisableAll(false));
        });
    }

    return (
        <div className="comp-server-configs">
            <Form.Control className="comp-bigtextarea"
                          onChange={e => setConfigText(e.target.value)}
                          ref={textArea}
                          as="textarea"
                          rows={30}/>
            <div className="comp-controls">

                <Form.Text muted>Конфиг</Form.Text>
                <Form.Select onChange={e => setConfigId(e.target.value)}>
                    {(configId == "") ? (<option key="none" value="">Выберите конфиг</option>) : ""}
                    {configs.map(c => (<option key={`k${c.id}`} value={c.id}>{c.id}</option>))}
                </Form.Select>

                <Form.Text muted>Комментарий</Form.Text>
                <Form.Control onChange={e => comment.current = e.target.value}
                              disabled={disableAll}
                              type="text"
                              placeholder=""/>

                <OverlayTrigger placement="top"
                                overlay={(props) =>
                                    <Tooltip {...props}>
                                        При включенном анализе изменений система автоматически проанализирует
                                        изменения в конфиге и выведет сгенерированный комментарий. В этом режиме
                                        разрешено изменять/добавлять/удалять только одну строчку за одно сохранение.
                                    </Tooltip>
                                }>
                    <Form.Check label="Пропустить анализ изменений"
                                onChange={e => skipAnalysis.current = e.target.value == 'true'}/>
                </OverlayTrigger>

                <div className="comp-button-container">
                    <Button onClick={requestConfig}
                            disabled={isEmpty(configId)
                                || disableAll}
                            variant="primary">Запросить</Button>
                    <Button onClick={saveConfig}
                            disabled={isEmpty(configId)
                                || isEmpty(configText)
                                || disableAll}
                            variant="primary">Сохранить</Button>
                </div>
            </div>
        </div>
    );
}