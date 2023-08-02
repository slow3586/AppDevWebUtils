import React, {ReactNode, useRef} from "react";
import {getLog} from "../clients/client";
import {Button, Form} from "react-bootstrap";

export interface ServerProps {
    host: string
}

export interface ServerState {
}

export class Server extends React.Component<ServerProps, ServerState> {
    private textArea: React.MutableRefObject<HTMLTextAreaElement> = useRef(null);

    constructor(props: ServerProps) {
        super(props);
    }

    componentDidMount() {
        getLog(this.props.host).then((response) => {
            this.textArea.current.value = response.trim();
        });
    }

    render() {
        return (
            <div className="server">
                <Form.Control readOnly as="textarea" rows={3} />
                <Form.Select aria-label="Default select example">
                    <option>Open this select menu</option>
                    <option value="1">Рестарт</option>
                    <option value="2">Обновление</option>
                    <option value="3">Клир кэш</option>
                </Form.Select>
                <Form.Control type="text" placeholder="qwe" />
                <Form.Control type="text" placeholder="Normal text" />
                <Button variant="primary">Запустить</Button>
                <Button variant="primary">Отменить</Button>
            </div>
        )
    }
}