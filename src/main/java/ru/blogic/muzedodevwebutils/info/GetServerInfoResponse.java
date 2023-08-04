package ru.blogic.muzedodevwebutils.info;

public record GetServerInfoResponse(
    boolean wsAdminShell,
    String currentOperation
) {}
