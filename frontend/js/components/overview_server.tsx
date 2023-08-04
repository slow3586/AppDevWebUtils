import React, {useEffect, useRef, useState} from "react";
import {Col, Form, Container, Row} from "react-bootstrap";
import {useQuery} from "react-query";
import {getServerInfo, getServerLog} from "../clients/info_client";
import {isEmpty, trim} from "lodash";
import dateFormat from "dateformat";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    const query = useQuery(
        ['getServerInfo', serverId],
        async () => await getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true
        });

    const insides = () => {
        if (query.isError) {
            return <Form.Text muted>Ошибка</Form.Text>
        }
        if (query.isLoading) {
            return <Form.Text muted>Загрузка</Form.Text>
        }
        return <div>
            <Form.Check
                disabled
                checked={query.data.wsAdminShell}
                type="switch"
                label="WsAdmin"
            />
            <Form.Text muted>Операция: {isEmpty(query.data.currentOperation) ? "-" : query.data.currentOperation}</Form.Text>
        </div>
    };

    return (
        <div className="component-overview-server">
            <Form.Text muted>{serverId}</Form.Text>
            {insides()}
        </div>
    )
}