import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";
import {QueryClient, QueryClientProvider,} from 'react-query'
import {ToastContainer} from "react-toastify";
import {ConnectionContextProvider} from "./contexts/connection_context";
import {ServersContextProvider} from "./contexts/servers_context";

require('../less/index.less')

const queryClient = new QueryClient()
Notification.requestPermission();

createRoot(document.getElementById("root")).render(
    <div>
        <ConnectionContextProvider>
            <ServersContextProvider>
                <QueryClientProvider client={queryClient}>
                    <App/>
                </QueryClientProvider>
                <ToastContainer/>
            </ServersContextProvider>
        </ConnectionContextProvider>
    </div>
);