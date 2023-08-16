import React from "react";
import {Tab, Tabs} from "react-bootstrap";
import {ServerControl} from "./server_control";
import {ServerLogs} from "./server_logs";
import {ServerConfigs} from "./server_configs";

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
    return (
        <div className="comp-server">
            <Tabs
                defaultActiveKey="overview"
                transition={false}
            >
                <Tab eventKey="control"
                     title="Управление">
                    <ServerControl serverId={serverId} isActive={true}></ServerControl>
                </Tab>
                <Tab eventKey="logs"
                     title="Логи">
                    <ServerLogs serverId={serverId}></ServerLogs>
                </Tab>
                <Tab eventKey="configs"
                     title="Конфиги">
                    <ServerConfigs serverId={serverId}></ServerConfigs>
                </Tab>
            </Tabs>
        </div>
    );
}