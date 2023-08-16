import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";
import {QueryClient, QueryClientProvider,} from 'react-query'
import {ToastContainer} from "react-toastify";

require('../less/index.less')

console.log("v0.7");

const queryClient = new QueryClient()
Notification.requestPermission();

createRoot(document.getElementById("root")).render(
    <div>
        <QueryClientProvider client={queryClient}>
            <App/>
        </QueryClientProvider>
        <ToastContainer/>
    </div>
);