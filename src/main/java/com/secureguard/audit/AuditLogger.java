package com.secureguard.audit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;

public final class AuditLogger {
    private final Path file;

    public AuditLogger(Path directory) {
        this.file = directory.resolve("audit.log");
    }

    public synchronized void record(String executor, UUID executorUuid, String action, String target, String result) throws IOException {
        Files.createDirectories(file.getParent());
        String entry = "{\"timestamp\":\"" + escape(Instant.now().toString())
                + "\",\"executor\":\"" + escape(executor)
                + "\",\"executorUuid\":" + (executorUuid == null ? "null" : "\"" + executorUuid + "\"")
                + ",\"action\":\"" + escape(action)
                + "\",\"target\":\"" + escape(target)
                + "\",\"result\":\"" + escape(result) + "\"}\n";
        Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}