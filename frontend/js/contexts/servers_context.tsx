import React, {Dispatch, SetStateAction, useState} from "react";

export const ServersContext: React.Context<ServersContextType>
    = React.createContext(null);

export class ServerContext {
    id: number;
    enabled: boolean;

    constructor(id: number, enabled: boolean) {
        this.id = id;
        this.enabled = enabled;
    }
}

export type ServersContextType = {
    servers: Array<ServerContext>,
    setServers: Dispatch<SetStateAction<ServerContext[]>>
}

const initialServers = localStorage.getItem('servers');

/**
 * Контекст, хранящий конфигурации сервером приложений.
 */
export const ServersContextProvider = ({children}: any) => {
    const [servers, setServers] = useState(JSON.parse(initialServers));

    localStorage.setItem("servers", JSON.stringify(servers));

    return (
        <ServersContext.Provider value={{
            servers,
            setServers
        }}>
            {children}
        </ServersContext.Provider>
    );
};