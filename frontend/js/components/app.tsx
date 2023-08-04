import React, {ReactNode, useRef} from "react";
import {Col, Form, Tab, Tabs, Container, Row} from "react-bootstrap";
import {Overview} from "./overview";
import {Server} from "./server";

export function App() {
    return (
        <div className="component-app">
            <Tabs
                defaultActiveKey="overview"
                id="uncontrolled-tab-example"
                transition={false}
            >
                <Tab eventKey="overview" title="Общее">
                    <Overview></Overview>
                </Tab>
                <Tab eventKey="b60" title="60">
                    <Server serverId={60}></Server>
                </Tab>
                <Tab eventKey="b61" title="61">
                    <Server serverId={61}></Server>
                </Tab>
            </Tabs>
        </div>
    )
}