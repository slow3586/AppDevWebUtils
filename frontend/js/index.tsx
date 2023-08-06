import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";
import {
    QueryClient,
    QueryClientProvider,
} from 'react-query'

require('../less/index.less')

const queryClient = new QueryClient()

createRoot(document.getElementById("root")).render(
    <QueryClientProvider client={queryClient}>
        <App/>
    </QueryClientProvider>
);