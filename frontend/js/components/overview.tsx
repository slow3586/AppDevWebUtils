import React, {useRef} from "react";
import {getLog} from "../clients/client";
import {ServerProps} from "./server";
import {Col, Form, Container, Row} from "react-bootstrap";

export interface OverviewProps {
}

export interface OverviewState {
}

export class Overview extends React.Component<OverviewProps, OverviewState> {
    constructor(props: ServerProps) {
        super(props);
    }

    componentDidMount() {
    }

    render() {
        return (
            <Container>
                <Row>
                    <Col>
                        <Form.Check
                            type="switch"
                            id="custom-switch60"
                            label="60"
                        />
                        <Form.Check
                            type="switch"
                            id="custom-switch61"
                            label="61"
                        />
                    </Col>
                    <Col>
                        <Form.Group controlId="exampleForm.ControlTextarea1">
                            <Form.Control readOnly as="textarea" rows={50}/>
                        </Form.Group>
                    </Col>
                </Row>
            </Container>
        )
    }
}