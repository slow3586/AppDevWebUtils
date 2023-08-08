import React, {useRef} from "react";
import {Col, Container, Form, Row} from "react-bootstrap";
import {OverviewServer} from "./overview_server";
import {getServerLog, Severity} from "../clients/info_client";
import dateFormat from "dateformat";
import {useQueries} from "react-query";
import {isEmpty, trim} from "lodash";
import {runNotification} from "../utils/notification";

export function Overview() {
    const last = useRef(new Map<number, number>());
    const info = useRef("");
    const firstRun = useRef(true);

    const servers = [58, 59, 60, 61];

    const addInfo = (add: string) => {
        if (!isEmpty(trim(add))) {
            info.current += add + "\n";
        }
    }

    const queries = useQueries(
        servers.map(serverId => ({
                queryKey: ['getServerLog', {serverId: serverId, logLast: last.current.get(serverId) ?? 0}],
                queryFn: async () => await getServerLog(serverId, last.current.get(serverId) ?? 0),
                refetchInterval: 3000,
                refetchIntervalInBackground: true
            })
        ))
        .map((q, index) => ({serverId: servers[index], query: q}));

    const errored = queries.filter(q => q.query.isError);
    const loading = queries.filter(q => q.query.isLoading);
    if (!isEmpty(errored)) {
        // @ts-ignore
        addInfo(errored.map(q => q.query.error.message)
            .join("\n") + "\n");
    }

    if (isEmpty(loading) && isEmpty(errored)) {
        const logs = queries
            .filter(q => last.current.get(q.serverId) != q.query.data.logLast)
            .flatMap(q => q.query.data.logs.flatMap(l => ({serverId: q.serverId, data: l})))
            .sort((a, b) => new Date(a.data.date)?.getTime?.() - new Date(b.data.date)?.getTime?.())
            .map(e => ({
                severity: e.data.severity, text:
                    `${dateFormat(e.data.date, "hh:MM:ss")} [${e.serverId}] [${e.data.user}] ${e.data.text}` // [${e.data.severity}]
            }));
        if (!isEmpty(logs)) {
            addInfo(logs
                .map(l => l.text)
                .join("\n"));
            const crits = logs.filter(l => l.severity == Severity.CRIT).map(l => l.text).join("\n");
            if (!firstRun.current && !isEmpty(trim(crits))) {
                runNotification("МЮЗ ЭДО DEV", crits);
            }
        }
        queries.forEach(q => {
            last.current.set(q.serverId, q.query.data.logLast);
        })
        firstRun.current = false;
    }

    return (
        <div className="comp-overview">
            <div className="comp-col">
                <Form.Group controlId="exampleForm.ControlTextarea1">
                    <Form.Control className="comp-textarea"
                                  value={info.current}
                                  readOnly as="textarea" rows={15}/>
                </Form.Group>
            </div>
            <div className="comp-col">
                {servers.map(s => (<OverviewServer key={`k${s}`} serverId={s}/>))}
            </div>
        </div>
    );
}