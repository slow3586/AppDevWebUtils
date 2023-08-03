import React, {useEffect, useRef, useState} from "react";
import {Col, Form, Container, Row} from "react-bootstrap";
import {getServerInfo, InfoEntry} from "../clients/client";
import dateFormat from "dateformat";

export function OverviewServer() {
    const fetchInfo = () => {
    }

    useEffect(() => {
        const interval =
            setInterval(() => fetchInfo(), 3000);
    }, []);

    return (
        <div className="component-overview-server">
            <Form.Text muted>Стенд</Form.Text>
            <Form.Check
                disabled
                type="switch"
                label="APPServer"
            />
            <Form.Check
                disabled
                type="switch"
                label="UZDO-GP"
            />
            <Form.Check
                disabled
                type="switch"
                label="UZDO-Integration"
            />
        </div>
    );
}