import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";
import {QueryClient, QueryClientProvider,} from 'react-query'
import {ToastContainer} from "react-toastify";
import {ConnectionContextProvider} from "./contexts/connection_context";
import {ServersContextProvider} from "./contexts/servers_context";

require('../less/index.less')

// Просим разрешение на показ оповещений в операционной системе пользователя
Notification.requestPermission();

// Создание корневого элемента веб-приложения
createRoot(document.getElementById("root")).render(
    <div>
        <ConnectionContextProvider>
            <ServersContextProvider>
                <QueryClientProvider client={new QueryClient()}>
                    <App/>
                </QueryClientProvider>
                <ToastContainer/>
            </ServersContextProvider>
        </ConnectionContextProvider>
    </div>
);