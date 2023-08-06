import React, {ReactNode, useRef, useState} from "react";
import {Col, Form, Tab, Tabs, Container, Row} from "react-bootstrap";
import {Overview} from "./overview";
import {Server} from "./server";
import {useQueryClient} from "react-query";

export function App() {
    let [activeTab, setActiveTab] = useState("overview");
    const servers = [58, 59, 60, 61];

    return (
        <div className="component-app">
            <Tabs
                defaultActiveKey="overview"
                transition={false}
            >
                <Tab key="overview"
                     onSelect={() => setActiveTab(`overview`)}
                     eventKey="overview"
                     title="Общее">
                    <Overview></Overview>
                </Tab>
                {servers.map(s => (
                    <Tab key={`key${s}`}
                         onSelect={() => setActiveTab(`tab${s}`)}
                         eventKey={`tab${s}`}
                         title={s}>
                        <Server isActive={true} serverId={s}></Server>
                    </Tab>
                ))}
            </Tabs>
        </div>
    )
}