import React, {ReactNode, useRef} from "react";
import {Col, Form, Tab, Tabs, Container, Row} from "react-bootstrap";
import {Overview} from "./overview";
import {Server} from "./server";

export interface AppProps {
}

export interface AppState {
}

export class App extends React.Component<AppProps, AppState> {
    private globalLog: React.MutableRefObject<HTMLTextAreaElement> = useRef(null);

    constructor(props: AppProps) {
        super(props);
    }

    componentDidMount() {
    }

    render() {
        return (
            <div className="component-app">
                <Tabs
                    defaultActiveKey="overview"
                    id="uncontrolled-tab-example"
                >
                    <Tab eventKey="overview" title="Общее">
                        <Overview></Overview>
                    </Tab>
                    <Tab eventKey="b60" title="60">
                        <Server host="172.19.203.60"></Server>
                    </Tab>
                    <Tab eventKey="b61" title="61">
                        <Server host="172.19.203.61"></Server>
                    </Tab>
                </Tabs>
            </div>
        )
    }
}