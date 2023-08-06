import React, {useEffect, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, getServerLog, LogEntry} from "../clients/info_client";
import dateFormat from "dateformat";
import {commandCancel, commandDelay, commandRun, Type} from "../clients/command_client";
import {useQuery, useQueryClient} from "react-query";
import {isEmpty, isNil, trim} from "lodash";
import {OverviewServer} from "./overview_server";

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
    const [disableAll, setDisableAll] = useState(false);
    const [commandId, setCommandId] = useState("");
    //const [commandScheduled, setCommandScheduled] = useState(false);
    //const [commandExecuting, setCommandExecuting] = useState(false);

    const queryClient = useQueryClient();

    const commands = [{
        id: "announce",
        name: "Оповещение",
        blocks: "NONE"
    }, {
        id: "ra",
        name: "Рестарт",
        blocks: "WS_BLOCK"
    }, {
        id: "uric",
        name: "Обновление (integ + cfg)",
        blocks: "WS_BLOCK"
    }, {
        id: "ura",
        name: "Обновление (gp + integ + cfg)",
        blocks: "WS_BLOCK"
    }, {
        id: "clear_cache",
        name: "Клир кэш",
        blocks: "SERVER_BLOCK"
    }];
    const getCommand = (id: string) => commands.find(c => c.id == id);

    const logQuery = useQuery(
        ['getServerLog', {serverId: serverId, logLast: logLast.current}],
        async () => await getServerLog(serverId, logLast.current),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: isActive
        });

    const infoQuery = useQuery(
        ['getServerInfo', {serverId: serverId}],
        async () => await getServerInfo(serverId),
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
    if (logQuery.isError) {
        // @ts-ignore
        addInfo(logQuery.error.message);
    }
    if (infoQuery.isError) {
        // @ts-ignore
        addInfo(infoQuery.error.message);
    }
    if (infoQuery.isLoading || logQuery.isLoading) {
        // setDisableAll(true);
    }

    if (!logQuery.isLoading && !logQuery.isError) {
        logLast.current = logQuery.data.logLast;
        addInfo(
            logQuery.data.logs.map(e =>
                `${dateFormat(e.date, "hh:MM:ss")} [${e.severity}] [${e.user}] ${e.text}`
            ).join("\n")
        );
    }

    if (!infoQuery.isRefetching
        && !logQuery.isRefetching
        && !infoQuery.isLoading
        && !logQuery.isError
        && disableAll == true) {
        setDisableAll(false);
    }

    const commandScheduled = infoQuery?.data?.scheduledCommand;
    const commandExecuting = infoQuery?.data?.executingCommand;

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
        }).finally(() => {
            queryClient.resetQueries(['getServerLog', {serverId: serverId}]);
            queryClient.resetQueries(['getServerInfo', {serverId: serverId}]);
        })
    };

    const cancelCommand = () => {
        setDisableAll(true);
        commandCancel({
            serverId: serverId,
            comment: comment.current
        }).then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
            alert(e);
        }).finally(() => {
            queryClient.resetQueries(['getServerLog', {serverId: serverId}]);
            queryClient.resetQueries(['getServerInfo', {serverId: serverId}]);
        })
    };

    const delayCommand = () => {
        setDisableAll(true);
        commandDelay({
            serverId: serverId,
            comment: comment.current,
            delaySeconds: parseInt(delay.current)
        }).then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
            alert(e);
        }).finally(() => {
            queryClient.resetQueries(['getServerLog', {serverId: serverId}]);
            queryClient.resetQueries(['getServerInfo', {serverId: serverId}]);
        })
    };

    const delayActive = (!isEmpty(delay.current)
        && parseInt(delay.current) > 0);

    const cantSchedule = !isNil(commandScheduled)
        && delayActive
        && getCommand(commandId)?.blocks != "NONE";

    const cantExecute = !isNil(commandExecuting) && !delayActive;

    const cantExecuteBecauseSchedule = !isNil(commandScheduled)
        && !delayActive
        && getCommand(commandId)?.blocks != "NONE";

    let errorMessages = new Array<string>;
    if (cantSchedule) {
        errorMessages.push("Уже есть запланированная операция");
    }
    if (cantExecute) {
        errorMessages.push("Выполняемая операция мешает выбранной");
    }
    if (cantExecuteBecauseSchedule) {
        errorMessages.push("Запланированная операция мешает выбранной");
    }
    if (disableAll) {
        errorMessages.push("Жду ответа");
    }

    return (
        <div className="component-server">
            <div className="component-server-container">
                <OverviewServer serverId={serverId}></OverviewServer>
                <Form.Control className="component-server-container-textarea"
                              value={info.current}
                              readOnly as="textarea" rows={10}/>

                <Form.Text muted>Команда</Form.Text>
                <Form.Select
                    aria-label="Выбор команды"
                    onChange={e => {
                        setCommandId(e.target.value);
                        setDisableAll(isNil(getCommand(e.target.value)));
                    }}>
                    {(commandId == "") ? (<option key="none" value="">Выберите команду</option>) : ""}
                    {commands.map(c => (<option key={`k${c.id}`} value={c.id}>{c.name}</option>))}
                </Form.Select>

                <Form.Text muted>Комментарий</Form.Text>
                <Form.Control onChange={e => comment.current = e.target.value}
                              disabled={disableAll}
                              type="text"
                              placeholder=""/>

                <div className="component-server-container-footer">
                    <Form.Text className="component-server-container-footer-error">
                        {errorMessages.join("\n")}</Form.Text>
                    <div className="component-server-container-footer-delay">
                        <Form.Text muted>Задержка (сек)</Form.Text>
                        <Form.Control onChange={e => delay.current = e.target.value}
                                      className="component-server-container-footer-delay-textarea"
                                      disabled={disableAll ||
                                          isNil(getCommand(commandId)) ||
                                          getCommand(commandId).blocks == "NONE"}
                                      type="number"
                                      min="0"
                                      max="600"
                                      placeholder="0"/>
                    </div>
                    <div className="component-server-container-footer-buttons">
                        <Button disabled={disableAll
                            || cantSchedule
                            || cantExecute
                            || cantExecuteBecauseSchedule
                            || isEmpty(commandId)}
                                onClick={runCommand}
                                variant="primary">Запустить</Button>
                        <Button disabled={disableAll
                            || !commandScheduled}
                                onClick={delayCommand}
                                variant="primary">Отложить</Button>
                        <Button disabled={disableAll
                            || !commandScheduled}
                                onClick={cancelCommand}
                                variant="primary">Отменить</Button>
                    </div>
                </div>
            </div>
        </div>
    );
}