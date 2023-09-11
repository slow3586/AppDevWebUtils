import React, {useState} from "react";

const GlobalContext = React.createContext(null);

export type GlobalSettings = {
    blockAllActions: boolean
}

export const GlobalContextProvider = ({children}: any) => {
    const [currentSettings, setCurrentSettings] = useState({
        blockAllActions: false
    });

    const saveSettings = (values: GlobalSettings) => {
        setCurrentSettings(values)
    };

    return (
        <GlobalContext.Provider
            value={{settings: currentSettings, saveSettings}}
        >
            {children}
        </GlobalContext.Provider>
    );
};