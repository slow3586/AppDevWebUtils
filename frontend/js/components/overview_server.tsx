import React, {ReactElement, useContext, useEffect, useRef, useState} from "react";
import {Form, Container, Row} from "react-bootstrap";
import {useQuery, useQueryClient} from "react-query";
import {getServerInfo, getServerLog} from "../clients/info_client";
import {isEmpty, isNil} from "lodash";
import {ServerContext, ServersContext} from "./app";
import {useCookies} from "react-cookie";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    //const servers = useContext(ServersContext);
    const [cookies, setCookies] = useCookies(['servers']);
    const servers: ServerContext[] = cookies.servers;
    const server = servers.find(s => s.id == serverId);
    const queryClient = useQueryClient();

    const query = useQuery(
        ['getServerInfo', serverId],
        () => getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: servers.find(s => s.id == serverId).enabled
        });

    let commandText = "-";
    if (!isEmpty(query?.data?.executingCommand?.name)) {
        commandText = `${query?.data?.executingCommand?.name ?? "-"} идёт ${query?.data?.executingCommandTimer ?? ""} сек.`;
    } else if (!isEmpty(query?.data?.scheduledCommand?.name)) {
        commandText = `${query?.data?.scheduledCommand?.name ?? "-"} через ${query?.data?.scheduledCommandTimer ?? ""} сек.`;
    } else if (!query?.data?.wsAdminShell) {
        commandText = `- (WsAdmin запускается)`;
    }

    const changeServerEnabled = (enabled: boolean) => {
        queryClient.removeQueries(['getServerInfo', serverId]);
        queryClient.removeQueries(['getServerLog', serverId]);
        setCookies('servers', servers.map(s => {
            if (s.id == serverId)
                s.enabled = enabled
            return s;
        }));
    }

    return (
        <div className="comp-overview-server">
            <Form.Check className="comp-enabled"
                        checked={server.enabled}
                        onChange={({target: {checked}}) => changeServerEnabled(checked)}
                        type="switch"/>
            <div className="comp-info">
                <div>
                    <Form.Text className="comp-id">{serverId}</Form.Text>
                </div>
                {server.enabled && (
                    <div className="comp-content">
                        <div className="comp-row">
                            <Form.Text className="comp-status">Сборка: <span className="comp-status-bold">{query?.data?.build ?? "OFF"}</span></Form.Text>
                        </div>
                        <div className="comp-row">
                            <Form.Text className="comp-status">GP: {query?.data?.gpBuild ?? "OFF"}</Form.Text></div>
                        <div className="comp-row">
                            <Form.Text className="comp-status">Integ: {query?.data?.integBuild ?? "OFF"}</Form.Text>
                        </div>
                        <div className="comp-row">
                            <Form.Text className="comp-status">Операция: {commandText}</Form.Text>
                        </div>
                    </div>
                )}
            </div>
        </div>
    )
}