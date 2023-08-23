import React, {createContext, useState} from "react";
import {Tab, Tabs} from "react-bootstrap";
import {Overview} from "./overview";
import {useCookies} from "react-cookie";
import {isEmpty} from "lodash";
import {Server} from "./server";
import {useQuery} from "react-query";
import {getFrontendConfig} from "../clients/frontend_client";

export class ServerContext {
    id: number;
    enabled: boolean;

    constructor(id: number, enabled: boolean) {
        this.id = id;
        this.enabled = enabled;
    }
}
export const ServersContext = createContext({})

export function App() {
    const [activeTab, setActiveTab] = useState("overview");
    //const firstLoad = useRef(true);
    const [cookies, setCookies] = useCookies(['servers']);

    const frontendConfigQuery = useQuery(
        ['getFrontendConfig'],
        () => getFrontendConfig(),
        {
            staleTime: Infinity,
        });

    let cookieServers: ServerContext[] = [];
    const configServers = frontendConfigQuery?.data?.servers
    if (!isEmpty(configServers)) {
        cookieServers = cookies.servers;
        if (isEmpty(cookies.servers)) {
            setCookies('servers', configServers);
            cookieServers = configServers.map(s => new ServerContext(s, true));
        }
    }

    return (
        <ServersContext.Provider value={{}}>
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