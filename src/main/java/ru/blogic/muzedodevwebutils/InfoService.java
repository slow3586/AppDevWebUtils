package ru.blogic.muzedodevwebutils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InfoService {
    private final MuzedoServerService muzedoServerService;
    private static final ThreadLocal<SimpleDateFormat> dateTimeFormat_ddMM_HHmmss = ThreadLocal.withInitial(
        () -> new SimpleDateFormat("dd.MM HH:mm:ss"));

    @Autowired
    public InfoService(
        MuzedoServerService muzedoServerService
    ) {
        this.muzedoServerService = muzedoServerService;
    }

    @PostConstruct
    public void postConstruct() {
        try {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, List<InfoEntry>> getOverview(
        final int last
    ) {
        final var muzedoServer = muzedoServerService.getMuzedoServer(61);
        final var log = muzedoServer.log;
        final var skip = Math.min(log.size(), Math.max(log.size() - last < 100 ? last : log.size() - 100, 0));

        final var map = new HashMap<Integer, List<InfoEntry>>();

        map.put(61, log
            .stream()
            .skip(skip)
            .filter(e -> e.severity() == InfoEntry.Severity.INFO
                || e.severity() == InfoEntry.Severity.CRIT)
            .toList());

        return map;
    }

    public List<InfoEntry> getServerInfo(
        final int serverId,
        final int last
    ) {
        final var muzedoServer = muzedoServerService.getMuzedoServer(serverId);
        final var log = muzedoServer.log;
        final var skip = Math.min(log.size(), Math.max(log.size() - last < 100 ? last : log.size() - 100, 0));
        return log
            .stream()
            .skip(skip)
            .toList();
    }

    public void writeInfo(
        final int serverId,
        final InfoEntry.Severity severity,
        final String text
    ) {
        final var auth = SecurityContextHolder.getContext().getAuthentication();
        final var user = auth == null
            ? "Система"
            : ((User) auth.getPrincipal()).getUsername();

        muzedoServerService
            .getMuzedoServer(serverId)
            .log
            .add(new InfoEntry(
                new Date(),
                text,
                severity,
                user
            ));
    }
}
