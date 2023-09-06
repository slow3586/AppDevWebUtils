package ru.blogic.muzedodevwebutils.api.file.logs;

import lombok.Value;

public record LogFile(
    String id,
    String path
) {}
