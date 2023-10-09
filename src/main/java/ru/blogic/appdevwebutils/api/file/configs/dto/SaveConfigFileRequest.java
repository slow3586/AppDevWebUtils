package ru.blogic.appdevwebutils.api.file.configs.dto;

public record SaveConfigFileRequest(
    int serverId,
    String configId,
    String configText,
    String comment,
    boolean skipAnalysis
) {}
