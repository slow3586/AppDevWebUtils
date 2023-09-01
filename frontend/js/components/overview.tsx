import React, {useRef} from "react";
import {Form} from "react-bootstrap";
import {OverviewServer} from "./overview_server";
import dateFormat from "dateformat";
import {useQueries} from "react-query";
import {isEmpty, isNil, trim} from "lodash";
import {runNotification} from "../utils/notification";
import {ServerContext} from "./app";
import {useCookies} from "react-cookie";
import {getServerHistory, Severity} from "../clients/history_client";

export function Overview() {
    const last = useRef(new Map<number, number>());
    const info = useRef("");
    const firstRun = useRef(true);
    const [cookies] = useCookies(['servers']);
    const servers: ServerContext[] = cookies.servers ?? [];

    servers.filter(s => !last.current.has(s.id))
        .forEach(s => last.current.set(s.id, 0));

    const addInfo = (add: string) => {
        if (!isEmpty(trim(add))) {
            info.current += add + "\n";
        }
    }

    const queries = useQueries(
        servers.map(server => ({
                queryKey: ['getServerLog', server.id, last.current.get(server.id) ?? 0],
                queryFn: async () => await getServerHistory(server.id, last.current.get(server.id) ?? 0),
                refetchInterval: 3000,
                refetchIntervalInBackground: true,
                enabled: server.enabled
            })
        ))
        .map((q, index) => ({serverId: servers[index].id, query: q}));

    const errored = queries.filter(q => q.query.isError);
    const loading = queries.filter(q => q.query.isLoading);
    if (!isEmpty(errored)) {
        // @ts-ignore
        addInfo(errored.map(q => q.query.error.message)
            .join("\n") + "\n");
    }

    if (isEmpty(loading) && isEmpty(errored)) {
        const goodQueries = queries.filter(q => !isNil(q.query.data));
        goodQueries.filter(q => last.current.get(q.serverId) > q.query.data.logLast)
            .forEach(q => last.current.set(q.serverId, q.query.data.logLast))
        const logs = goodQueries
            .filter(q => last.current.get(q.serverId) < q.query.data.logLast)
            .flatMap(q => q.query.data.logs.flatMap(l => ({serverId: q.serverId, data: l})))
            .sort((a, b) => new Date(a.data.date)?.getTime?.() - new Date(b.data.date)?.getTime?.())
            .map(e => ({
                severity: e.data.severity, text:
                    `${dateFormat(e.data.date, "dd.mm HH:MM:ss")} [${e.serverId}] [${e.data.user}] ${e.data.text}` // [${e.data.severity}]
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
        goodQueries.forEach(q => {
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
                {servers
                    .filter(s => s.enabled)
                    .map(s => (<OverviewServer key={`k${s.id}`} serverId={s.id}/>))}
                {servers
                    .filter(s => !s.enabled)
                    .map(s => (<OverviewServer key={`k${s.id}`} serverId={s.id}/>))}
            </div>
        </div>
    );
}