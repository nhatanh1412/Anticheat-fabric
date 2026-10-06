package com.secureguard.alert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AlertPreferences {
    private final Path file;
    private final Map<UUID, Boolean> enabled = new HashMap<>();

    public AlertPreferences(Path directory) {
        this.file = directory.resolve("alerts.json");
    }

    public synchronized void load() throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.notExists(file)) {
            save();
            return;
        }
        Map<UUID, Boolean> loaded = new HashMap<>();
        Matcher matcher = Pattern.compile("\\\"([0-9a-fA-F-]{36})\\\"\\s*:\\s*(true|false)").matcher(Files.readString(file, StandardCharsets.UTF_8));
        while (matcher.find()) {
            try {
                loaded.put(UUID.fromString(matcher.group(1)), Boolean.parseBoolean(matcher.group(2)));
            } catch (IllegalArgumentException ignored) {
                // Invalid UUID keys are not trusted.
            }
        }
        enabled.clear();
        enabled.putAll(loaded);
    }

    public synchronized boolean isEnabled(UUID uuid) {
        return enabled.getOrDefault(uuid, true);
    }

    public synchronized void setEnabled(UUID uuid, boolean value) throws IOException {
        enabled.put(uuid, value);
        save();
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<UUID, Boolean> entry : enabled.entrySet()) {
            if (!first) json.append(",\n");
            json.append("  \"").append(entry.getKey()).append("\": ").append(entry.getValue());
            first = false;
        }
        json.append("\n}\n");
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}