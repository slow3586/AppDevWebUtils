import React, {createContext, ReactNode, useContext, useRef, useState} from "react";
import {Col, Form, Tab, Tabs, Container, Row} from "react-bootstrap";
import {Overview} from "./overview";
import {Server} from "./server";
import {useQueryClient} from "react-query";
import {useCookies} from "react-cookie";
import {isEmpty} from "lodash";

export class ServerContext {
    id: number;
    enabled: boolean;

    constructor(id: number, enabled: boolean) {
        this.id = id;
        this.enabled = enabled;
    }
}

const _servers = [58, 59, 60, 61, 146, 147].map(s => new ServerContext(s, true));
export const ServersContext = createContext({
    servers: new Array<ServerContext>,
    setServers: (a: any) => {
    }
})

export function App() {
    const [activeTab, setActiveTab] = useState("overview");
    const [servers, setServers] = useState(_servers);
    const firstLoad = useRef(true);
    const [cookies, setCookies] = useCookies(['servers']);
    const cookieServers: ServerContext[] = cookies.servers;

    if (isEmpty(cookies.servers)) {
        setCookies('servers', _servers);
    }

    return (
        <ServersContext.Provider value={{
            servers,
            setServers
        }}>
            <div className="comp-app">
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
                    {(cookieServers ?? [])
                        .filter(s => s.enabled)
                        .map(s => (
                            <Tab key={`key${s.id}`}
                                 onSelect={() => setActiveTab(`tab${s.id}`)}
                                 eventKey={`tab${s.id}`}
                                 title={s.id}>
                                <Server isActive={true} serverId={s.id}></Server>
                            </Tab>
                        ))}
                </Tabs>
            </div>
        </ServersContext.Provider>
    )
}