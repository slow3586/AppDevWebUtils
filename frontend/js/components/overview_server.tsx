import React, {useEffect, useRef, useState} from "react";
import {Col, Form, Container, Row} from "react-bootstrap";

export type OverviewServerProps = {
    serverId: number
}

export function OverviewServer({serverId}: OverviewServerProps) {
    const fetchInfo = () => {
    }

    useEffect(() => {
        const interval =
            setInterval(() => fetchInfo(), 3000);
    }, []);

    return (
        <div className="component-overview-server">
            <Form.Text muted>{serverId}</Form.Text>
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