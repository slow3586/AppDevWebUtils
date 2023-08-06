import React, {ReactElement, useEffect, useRef, useState} from "react";
import {Form, Container, Row} from "react-bootstrap";
import {useQuery} from "react-query";
import {getServerInfo, getServerLog} from "../clients/info_client";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    const query = useQuery(
        ['getServerInfo', {serverId}],
        async () => await getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true
        });

    return (
        <div className="component-overview-server">
            <Form.Text key="serverId" muted>{serverId}</Form.Text>
            <div className="component-overview-server-insides">
                <Form.Check key="wsadmin"
                            disabled
                            checked={query?.data?.wsAdminShell ?? false}
                            type="switch"
                            label="WsAdmin"
                />
                <Form.Text key="exec">Выполняется: {query?.data?.executingCommand?.name ?? "-"}</Form.Text>
                <Form.Text key="plan">Запланировано: {query?.data?.scheduledCommand?.name ?? "-"}</Form.Text>
            </div>
        </div>
    )
}