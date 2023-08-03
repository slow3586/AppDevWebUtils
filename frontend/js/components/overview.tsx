import React, {MutableRefObject, useEffect, useRef, useState} from "react";
import {Col, Form, Container, Row, FormControl} from "react-bootstrap";
import {OverviewServer} from "./overview_server";
import {getOverview, getServerInfo, InfoEntry} from "../clients/client";
import dateFormat, {masks} from "dateformat";

export function Overview() {
    let [last, setLast] = useState(0);
    let [info, setInfo] = useState("");

    const fetchInfo = () => {
        getOverview(last).then((response: Map<number, InfoEntry[]>) => {
            setInfo(
                Object.entries(response)
                    .flatMap(e => {
                        const k = e[0];
                        const v = e[1];
                        return v.map((e: InfoEntry) => {
                            const date = new Date(e.date);
                            return ({
                                date: date,
                                text: `${dateFormat(date, "hh:MM:ss")} [${k}] [${e.severity}] [${e.user}] ${e.text}`
                            });
                        });
                    })
                    .sort((a, b) =>
                        a.date.getTime() - b.date.getTime())
                    .map(e => e.text)
                    .join("\n")
            );
        });
    }

    useEffect(() => {
        const interval =
            setInterval(() => fetchInfo(), 3000);
    }, []);

    return (
        <div className="component-overview">
            <Container>
                <Row>
                    <Col className="component-overview-col">
                        <Form.Group controlId="exampleForm.ControlTextarea1">
                            <Form.Control className="component-overview-textarea"
                                          value={info}
                                          readOnly as="textarea" rows={15}/>
                        </Form.Group>
                    </Col>
                    <Col className="component-overview-col">
                        <OverviewServer></OverviewServer>
                        <OverviewServer></OverviewServer>
                    </Col>
                </Row>
            </Container>
        </div>
    );
}