import React, {ReactElement, useEffect, useRef, useState} from "react";
import {Form, Container, Row} from "react-bootstrap";
import {useQuery} from "react-query";
import {getServerInfo, getServerLog} from "../clients/info_client";
import {isEmpty, isNil} from "lodash";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {solid} from "@fortawesome/fontawesome-svg-core/import.macro";
import {faX} from "@fortawesome/free-solid-svg-icons/faX";
import {faCheck} from "@fortawesome/free-solid-svg-icons/faCheck";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    const query = useQuery(
        ['getServerInfo', {serverId}],
        () => getServerInfo(serverId),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true
        });

    let commandText = "-";
    if (!isEmpty(query?.data?.executingCommand?.name)) {
        commandText = `${query?.data?.executingCommand?.name ?? "-"} идёт ${query?.data?.executingCommandTimer ?? ""} сек.`;
    } else if (!isEmpty(query?.data?.scheduledCommand?.name)) {
        commandText = `${query?.data?.scheduledCommand?.name ?? "-"} через ${query?.data?.scheduledCommandTimer ?? ""} сек.`;
    } else if (!query?.data?.wsAdminShell) {
        commandText = `X (WsAdmin запускается)`;
    }

    return (
        <div className="component-overview-server">
            <Form.Text className="component-overview-server-id">{serverId}</Form.Text>
            <div className="component-overview-server-content">
                <div className="component-overview-server-row">
                    <Form.Text className="component-overview-server-status">Сборка: {query?.data?.build ?? "OFF"}</Form.Text></div>
                <div className="component-overview-server-row">
                    <Form.Text className="component-overview-server-status">GP: {query?.data?.gpBuild ?? "OFF"}</Form.Text></div>
                <div className="component-overview-server-row">
                    <Form.Text className="component-overview-server-status">Integ: {query?.data?.integBuild ?? "OFF"}</Form.Text>
                </div>
                <div className="component-overview-server-row">
                    <Form.Text className="component-overview-server-status">Операция: {commandText}</Form.Text>
                </div>
            </div>
        </div>
    )
}