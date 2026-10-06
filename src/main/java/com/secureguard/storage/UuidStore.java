package com.secureguard.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UuidStore {
    private final Path file;
    private final String key;
    private final Set<UUID> values = new LinkedHashSet<>();

    public UuidStore(Path directory, String fileName, String key) {
        this.file = directory.resolve(fileName);
        this.key = key;
    }

    public synchronized void load() throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.notExists(file)) {
            save();
            return;
        }
        String json = Files.readString(file, StandardCharsets.UTF_8);
        Matcher array = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\[([^]]*)]").matcher(json);
        Set<UUID> loaded = new LinkedHashSet<>();
        if (array.find()) {
            Matcher strings = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(array.group(1));
            while (strings.find()) {
                try {
                    loaded.add(UUID.fromString(strings.group(1)));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed identifiers; authorization remains UUID-only.
                }
            }
        }
        values.clear();
        values.addAll(loaded);
    }

    public synchronized boolean contains(UUID uuid) {
        return values.contains(uuid);
    }

    public synchronized boolean add(UUID uuid) throws IOException {
        if (!values.add(uuid)) return false;
        save();
        return true;
    }

    public synchronized boolean remove(UUID uuid) throws IOException {
        if (!values.remove(uuid)) return false;
        save();
        return true;
    }

    public synchronized Set<UUID> snapshot() {
        return Set.copyOf(values);
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        StringBuilder json = new StringBuilder("{\n  \"").append(key).append("\": [");
        boolean first = true;
        for (UUID uuid : values) {
            if (!first) json.append(',');
            json.append("\n    \"").append(uuid).append('"');
            first = false;
        }
        if (!first) json.append('\n').append("  ");
        json.append("]\n}\n");
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, json, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}