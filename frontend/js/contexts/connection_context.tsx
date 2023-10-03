import React, {Dispatch, SetStateAction, useContext, useState} from "react";

export const ConnectionContext: React.Context<ConnectionContextType>
    = React.createContext(null);

export type ConnectionContextType = {
    connectionEstablished: boolean,
    setConnectionEstablished: Dispatch<SetStateAction<boolean>>
}

export const ConnectionContextProvider = ({children}: any) => {
    const [connectionEstablished, setConnectionEstablished] = useState(false);

    return (
        <ConnectionContext.Provider value={{
            connectionEstablished,
            setConnectionEstablished
        }}>
            {children}
        </ConnectionContext.Provider>
    );
};