import React, {useContext, useState} from "react";
import {Tab, Tabs} from "react-bootstrap";
import {Overview} from "./overview";
import {isEmpty} from "lodash";
import {Server} from "./server";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";
import {ConnectionContext} from "../contexts/connection_context";
import {ServerContext, ServersContext} from "../contexts/servers_context";
import {toast} from "react-toastify";

export function App() {
    const [activeTab, setActiveTab] = useState('overview');
    const connectionContext = useContext(ConnectionContext);
    const serversContext = useContext(ServersContext);

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
    const connectionEstablished = !isEmpty(version) && !frontendConfigQuery.isError;
    if (connectionEstablished != connectionContext.connectionEstablished) {
        if (connectionEstablished) {
            connectionContext.setConnectionEstablished(true);
            toast.success("Соединение установлено");
            console.log("Version: " + version);
        } else {
            // @ts-ignore
            const failedToFetch = frontendConfigQuery?.error?.message === 'Failed to fetch';
            if (failedToFetch) {
                connectionContext.setConnectionEstablished(false);
            }
        }
    }

    const storageServers: ServerContext[] = serversContext.servers ?? [];
    const configServers: number[] = frontendConfigQuery?.data?.servers ?? [];
    const servers = !isEmpty(configServers)
        ? storageServers.filter(s => configServers.includes(s.id))
            .concat(
                configServers.filter(s => !storageServers.some(cs => cs.id == s))
                    .map(s => new ServerContext(s, true))
            )
        : storageServers;
    if (!isEmpty(servers)
        && !isEmpty(configServers)
        && (!servers.every(s => storageServers.some(cs => cs.id == s.id))
            || servers.length != storageServers.length)) {
        serversContext.setServers(servers);
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