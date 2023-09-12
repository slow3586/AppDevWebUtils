import React, {useRef, useState} from "react";
import {Button, Form, OverlayTrigger, Tooltip} from "react-bootstrap";
import {getServerConfigFile, saveServerConfigFile} from "../clients/file_configs_client";
import {isEmpty} from "lodash";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {toast} from "react-toastify";

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

    const requestConfig = () => {
        return requestWrapper(
            "",
            0,
            getServerConfigFile(serverId, configId)
                .then(configText => {
                    configText.replace?.("\r\r\n", "\n");
                    setConfigText(configText);
                    textArea.current.value = configText;
                }));
    }

    const saveConfig = () => {
        return requestWrapper(
            "Сохраняю...",
            5,
            saveServerConfigFile({
                serverId,
                configId,
                configText,
                comment: comment.current,
                skipAnalysis: skipAnalysis.current
            }));
    }

    return (
        <div className="comp-server-configs">
            <Form.Control className="comp-bigtextarea"
                          onChange={e => setConfigText(e.target.value)}
                          ref={textArea}
                          as="textarea"
                          rows={25}/>
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

                <OverlayTrigger placement="right"
                                delay={{show: 250, hide: 0}}
                                overlay={(props) =>
                                    <Tooltip {...props}>
                                        При включенном анализе изменений система автоматически проанализирует
                                        изменения в конфиге и выведет сгенерированный комментарий. В этом режиме
                                        разрешено изменять/добавлять/удалять только одну строчку за одно сохранение.
                                    </Tooltip>
                                }>
                    <div>
                        <Form.Check label="Пропустить анализ изменений"
                                    onChange={e => skipAnalysis.current = e.target.checked}/>
                    </div>
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