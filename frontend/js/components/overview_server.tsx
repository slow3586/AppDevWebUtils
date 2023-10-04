import React, {useContext} from "react";
import {Form} from "react-bootstrap";
import {useQuery, useQueryClient} from "react-query";
import {getServerInfo} from "../clients/info_client";
import {isEmpty, isNil} from "lodash";
import {ConnectionContext} from "../contexts/connection_context";
import {ServerContext, ServersContext} from "../contexts/servers_context";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    const queryClient = useQueryClient();
    const connectionContext = useContext(ConnectionContext);
    const serversContext = useContext(ServersContext);
    const servers: ServerContext[] = serversContext.servers ?? [];
    const server = servers.find(s => s.id == serverId);

    const query = useQuery(
        ['getServerInfo', serverId],
        () => getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: connectionContext.connectionEstablished && server.enabled,
            retry: false
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
        serversContext.setServers(servers.map(s => {
            if (s.id == serverId)
                s.enabled = enabled
            return s;
        }));
    }

    const gpBuild = query?.data?.gpBuild;
    const gpShort = isNil(gpBuild)
        ? "OFF"
        : !isNil(gpBuild?.date)
            ? `${gpBuild.author} @ ${gpBuild.date.substring(0, 22)}`
            : "Неизвестная сборка";
    const gpFull = gpBuild?.date
        ? `(${gpBuild.branch}, ${gpBuild.hash})`
        : '';

    const integBuild = query?.data?.integBuild;
    const integShort = isNil(integBuild)
        ? "OFF"
        : !isNil(integBuild?.date)
            ? `${integBuild.author} @ ${integBuild.date.substring(0, 22)}`
            : "Неизвестная сборка";
    const integFull = integBuild?.date
        ? `(${integBuild.branch}, ${integBuild.hash})`
        : '';

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
                        <div title={gpFull} className="comp-row">
                            <Form.Text className="comp-status">GP: {gpShort}</Form.Text>
                        </div>
                        <div title={integFull} className="comp-row">
                            <Form.Text className="comp-status">Integ: {integShort}</Form.Text>
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