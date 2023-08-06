import React, {ReactElement, useEffect, useRef, useState} from "react";
import {Form, Container, Row} from "react-bootstrap";
import {useQuery} from "react-query";
import {getServerInfo, getServerLog} from "../clients/info_client";
import {isEmpty} from "lodash";

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

    let execText = "-";
    if (!isEmpty(query?.data?.executingCommand?.name)) {
        execText = `${query?.data?.executingCommand?.name ?? "-"} идёт ${query?.data?.executingCommandTimer ?? ""} сек.`;
    }
    let schedText = "-";
    if (!isEmpty(query?.data?.scheduledCommand?.name)) {
        schedText = `${query?.data?.scheduledCommand?.name ?? "-"} через ${query?.data?.scheduledCommandTimer ?? ""} сек.`;
    }

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
                <Form.Text key="exec">Выполняется: {execText}</Form.Text>
                <Form.Text key="plan">Запланировано: {schedText}</Form.Text>
            </div>
        </div>
    )
}