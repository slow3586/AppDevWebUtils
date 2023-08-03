import {createRoot} from "react-dom/client";
import React from "react";
import {App} from "./components/app";
import 'bootstrap/dist/css/bootstrap.min.css';

require('../less/index.less')

createRoot(document.getElementById("root")).render(<App></App>);