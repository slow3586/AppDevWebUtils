import React, {useContext, useState} from "react";
import {Tab, Tabs} from "react-bootstrap";
import {Overview} from "./overview";
import {useCookies} from "react-cookie";
import {isEmpty} from "lodash";
import {Server} from "./server";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {ConnectionContext, ConnectionContextType} from "../contexts/connection_context";

export class ServerContext {
    id: number;
    enabled: boolean;

    constructor(id: number, enabled: boolean) {
        this.id = id;
        this.enabled = enabled;
    }
}

export function App() {
    const [activeTab, setActiveTab] = useState('overview');
    const [cookies, setCookies] = useCookies(['servers']);
    const connectionContext: ConnectionContextType = useContext(ConnectionContext);

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            refetchInterval: 3000,
            refetchIntervalInBackground: true,
            enabled: !connectionContext.connectionEstablished,
            retry: false
        });

    const version = frontendConfigQuery?.data?.version;
    const connectionEstablished = !isEmpty(version)
        && !frontendConfigQuery.isError;
    if (connectionEstablished != connectionContext.connectionEstablished) {
        connectionContext.setConnectionEstablished(connectionEstablished);
        if (connectionEstablished && !isEmpty(version)) {
            console.log("Version: " + version);
        }
    }

    const cookiesServers: ServerContext[] = cookies.servers ?? [];
    const configServers: number[] = frontendConfigQuery?.data?.servers ?? [];
    const servers = !isEmpty(configServers)
        ? cookiesServers.filter(s => configServers.includes(s.id))
            .concat(
                configServers.filter(s => !cookiesServers.some(cs => cs.id == s))
                    .map(s => new ServerContext(s, true))
            )
        : [];
    if (!isEmpty(servers) && !servers.every(s => cookiesServers.some(cs => cs.id == s.id))) {
        setCookies('servers', servers);
    }

    return (
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
                {(servers)
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
    )
}