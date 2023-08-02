import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";

require('../less/index.less')

createRoot(document.getElementById("root")).render(<App></App>);