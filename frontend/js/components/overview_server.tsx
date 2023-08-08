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
        ['getServerInfo', serverId],
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
        commandText = `- (WsAdmin запускается)`;
    }

    return (
        <div className="comp-overview-server">
            <Form.Text className="comp-id">{serverId}</Form.Text>
            <div className="comp-content">
                <div className="comp-row">
                    <Form.Text className="comp-status">Сборка: <span className="comp-status-bold">{query?.data?.build ?? "OFF"}</span>
                    </Form.Text></div>
                <div className="comp-row">
                    <Form.Text className="comp-status">GP: {query?.data?.gpBuild ?? "OFF"}</Form.Text></div>
                <div className="comp-row">
                    <Form.Text className="comp-status">Integ: {query?.data?.integBuild ?? "OFF"}</Form.Text>
                </div>
                <div className="comp-row">
                    <Form.Text className="comp-status">Операция: {commandText}</Form.Text>
                </div>
            </div>
        </div>
    )
}