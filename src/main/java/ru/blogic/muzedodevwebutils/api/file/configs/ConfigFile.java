package ru.blogic.muzedodevwebutils.api.file.configs;

public record ConfigFile(
    String id,
    String path,
    boolean skipChangesCheck
) {}
