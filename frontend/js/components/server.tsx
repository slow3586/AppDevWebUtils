import React, {useEffect, useRef, useState} from "react";
import {Button, Form} from "react-bootstrap";
import {getServerInfo, getServerLog, LogEntry} from "../clients/info_client";
import dateFormat from "dateformat";
import {commandCancel, commandDelay, commandRun, Type} from "../clients/command_client";
import {useQuery, useQueryClient} from "react-query";
import {isEmpty, isNil, trim} from "lodash";
import {OverviewServer} from "./overview_server";
import {toast} from "react-toastify";

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
    const allowEnableAll = useRef(true);
    const [disableAll, setDisableAll] = useState(false);
    const [commandId, setCommandId] = useState("");

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
        ['getServerLog', serverId, logLast.current],
        () => getServerLog(serverId, logLast.current),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: isActive && allowEnableAll.current,
            staleTime: Infinity,
        });

    const infoQuery = useQuery(
        ['getServerInfo', serverId],
        () => getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: isActive && allowEnableAll.current,
            staleTime: Infinity
        });

    const addInfo = (add: string) => {
        if (!isEmpty(trim(add))) {
            info.current += add + "\n";
        }
    }
    if (!logQuery.isLoading && !logQuery.isError) {
        logLast.current = logQuery.data.logLast;
        addInfo(
            logQuery.data.logs.map(e =>
                `${dateFormat(e.date, "hh:MM:ss")} [${e.user}] ${e.text}` // [${e.data.severity}]
            ).join("\n")
        );
    }

    if (!infoQuery.isLoading
        && !logQuery.isLoading
        && !logQuery.isError
        && !infoQuery.isError
        && !logQuery.isStale
        && !infoQuery.isStale
        && !logQuery.isFetching
        && !infoQuery.isFetching
        && !logQuery.isRefetching
        && !infoQuery.isRefetching
        && allowEnableAll.current
        && disableAll == true) {
        setDisableAll(false);
    }

    const requestWrapper = (promise: Promise<string>) => {
        setDisableAll(true);
        queryClient.invalidateQueries(['getServerLog', serverId], {active: true, fetching: true, stale: true});
        queryClient.invalidateQueries(['getServerInfo', serverId], {active: true, fetching: true, stale: true});
        allowEnableAll.current = false;
        promise.then((r) => {
            console.log(r);
        }).catch((e) => {
            console.error(e);
        }).finally(() => {
            allowEnableAll.current = true;
        })
    }

    const runCommand = () => requestWrapper(
        commandRun({
            serverId: serverId,
            commandId: commandId,
            comment: comment.current,
            delaySeconds: parseInt(delay.current)
        })
    );

    const cancelCommand = () => requestWrapper(
        commandCancel({
            serverId: serverId,
            comment: comment.current
        })
    );

    const delayCommand = () => requestWrapper(
        commandDelay({
            serverId: serverId,
            comment: comment.current,
            delaySeconds: parseInt(delay.current)
        })
    );

    const commandScheduled = infoQuery?.data?.scheduledCommand;
    const commandExecuting = infoQuery?.data?.executingCommand;

    const delayActive = (!isEmpty(delay.current)
        && parseInt(delay.current) > 0);

    const cantSchedule = !isNil(commandScheduled)
        && delayActive
        && getCommand(commandId)?.blocks != "NONE";

    const cantExecute = !isNil(commandExecuting) && !delayActive;

    const cantExecuteBecauseSchedule = !isNil(commandScheduled)
        && !delayActive
        && getCommand(commandId)?.blocks != "NONE";

    const wsadminUnavailable = getCommand(commandId)?.blocks != "NONE"
        && !infoQuery?.data?.wsAdminShell;

    let errorMessages = new Array<string>;
    if (wsadminUnavailable) {
        errorMessages.push("WsAdmin недоступен");
    }
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
        <div className="comp-server">
            <div className="comp-container">
                <OverviewServer serverId={serverId}></OverviewServer>
                <Form.Control className="comp-textarea"
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

                <div className="comp-footer">
                    <div className="comp-delay">
                        <Form.Text muted>Задержка (сек)</Form.Text>
                        <Form.Control onChange={e => delay.current = e.target.value}
                                      className="comp-textarea"
                                      disabled={disableAll ||
                                          isNil(getCommand(commandId)) ||
                                          getCommand(commandId).blocks == "NONE"}
                                      type="number"
                                      min="0"
                                      max="600"
                                      placeholder="0"/>
                    </div>
                    <div className="comp-buttons">
                        <Button disabled={disableAll
                            || cantSchedule
                            || cantExecute
                            || cantExecuteBecauseSchedule
                            || wsadminUnavailable
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
                <Form.Text className="comp-error">
                    {errorMessages.join("; ")}</Form.Text>
            </div>
        </div>
    );
}