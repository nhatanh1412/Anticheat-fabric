package com.secureguard.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigManager {
    private static final String DEFAULTS = """
            {
              "enabled": true,
              "debug": false,
              "commandVisibility": {
                "whitelistHiddenFromConsole": true,
                "whitelistHiddenFromWhitelistUsers": true,
                "whitelistVisibleToAdmins": true
              },
              "alerts": { "enabled": true, "console": true, "admins": true },
              "punishments": { "enabled": false },
              "checks": {
                "speed": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "fly": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "nofall": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "phase": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "velocity": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "timer": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 },
                "step": { "enabled": true, "alertThreshold": 5.0, "punishThreshold": 20.0, "decay": 0.25, "buffer": 0.0 }
              },
              "tps": { "lowTpsThreshold": 18.0, "punishmentMultiplier": 0.5 },
              "setback": { "enabled": true, "cooldownTicks": 40 }
            }
            """;
    private static final String PUNISHMENTS_DEFAULTS = """
            {
              "enabled": false,
              "commands": {
                "speed": "ban %player% Unfair Advantage",
                "fly": "ban %player% Unfair Advantage",
                "nofall": "ban %player% Unfair Advantage",
                "phase": "ban %player% Unfair Advantage",
                "velocity": "ban %player% Unfair Advantage",
                "timer": "ban %player% Unfair Advantage",
                "step": "ban %player% Unfair Advantage"
              }
            }
            """;

    private final Path file;
    private final Path punishmentsFile;
    private volatile Settings settings = Settings.defaults();

    public ConfigManager(Path directory) {
        this.file = directory.resolve("config.json");
        this.punishmentsFile = directory.resolve("punishments.json");
    }

    public synchronized Settings load() throws IOException {
        Files.createDirectories(file.getParent());
        if (Files.notExists(file)) {
            writeAtomically(file, DEFAULTS);
        }
        if (Files.notExists(punishmentsFile)) writeAtomically(punishmentsFile, PUNISHMENTS_DEFAULTS);
        String json = Files.readString(file, StandardCharsets.UTF_8);
        String punishmentJson = Files.readString(punishmentsFile, StandardCharsets.UTF_8);
        Settings loaded = parse(json, punishmentJson);
        this.settings = loaded;
        return loaded;
    }

    public Settings get() {
        return settings;
    }

    private static Settings parse(String json, String punishmentJson) {
        Settings defaults = Settings.defaults();
        Settings result = new Settings();
        result.enabled = bool(json, "enabled", defaults.enabled);
        result.debug = bool(json, "debug", defaults.debug);
        String visibility = object(json, "commandVisibility");
        result.whitelistHiddenFromConsole = bool(visibility, "whitelistHiddenFromConsole", defaults.whitelistHiddenFromConsole);
        result.whitelistHiddenFromWhitelistUsers = bool(visibility, "whitelistHiddenFromWhitelistUsers", defaults.whitelistHiddenFromWhitelistUsers);
        result.whitelistVisibleToAdmins = bool(visibility, "whitelistVisibleToAdmins", defaults.whitelistVisibleToAdmins);
        String alerts = object(json, "alerts");
        result.alertsEnabled = bool(alerts, "enabled", defaults.alertsEnabled);
        result.consoleAlerts = bool(alerts, "console", defaults.consoleAlerts);
        result.adminAlerts = bool(alerts, "admins", defaults.adminAlerts);
        String punishments = object(json, "punishments");
        result.punishmentsEnabled = bool(punishments, "enabled", defaults.punishmentsEnabled);
        String punishmentCommands = object(punishments, "commands");
        for (Map.Entry<String, String> entry : defaults.punishmentCommands.entrySet()) {
            result.punishmentCommands.put(entry.getKey(), string(punishmentCommands, entry.getKey(), entry.getValue()));
        }
        result.punishmentsEnabled = bool(punishmentJson, "enabled", result.punishmentsEnabled);
        String externalCommands = object(punishmentJson, "commands");
        for (Map.Entry<String, String> entry : result.punishmentCommands.entrySet()) {
            result.punishmentCommands.put(entry.getKey(), string(externalCommands, entry.getKey(), entry.getValue()));
        }
        String tps = object(json, "tps");
        result.lowTpsThreshold = number(tps, "lowTpsThreshold", defaults.lowTpsThreshold);
        result.lowTpsPunishmentMultiplier = number(tps, "punishmentMultiplier", defaults.lowTpsPunishmentMultiplier);
        result.setbackEnabled = bool(object(json, "setback"), "enabled", defaults.setbackEnabled);
        result.setbackCooldownTicks = (int) number(object(json, "setback"), "cooldownTicks", defaults.setbackCooldownTicks);
        for (String id : defaults.checks.keySet()) {
            String section = object(object(json, "checks"), id);
            CheckSettings fallback = defaults.checks.get(id);
            result.checks.put(id, new CheckSettings(
                    bool(section, "enabled", fallback.enabled),
                    number(section, "alertThreshold", fallback.alertThreshold),
                    number(section, "punishThreshold", fallback.punishThreshold),
                    number(section, "decay", fallback.decay),
                    number(section, "buffer", fallback.buffer)));
        }
        return result;
    }

    private static String object(String json, String key) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\{").matcher(json);
        if (!matcher.find()) return "";
        int start = matcher.end();
        int depth = 1;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return json.substring(start, i);
        }
        return "";
    }

    private static boolean bool(String json, String key, boolean fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(true|false)").matcher(json);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : fallback;
    }

    private static double number(String json, String key, double fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").matcher(json);
        if (!matcher.find()) return fallback;
        try {
            double value = Double.parseDouble(matcher.group(1));
            return Double.isFinite(value) ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String string(String json, String key, String fallback) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!matcher.find()) return fallback;
        return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private synchronized void writeAtomically(Path destination, String content) throws IOException {
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException unsupportedAtomicMove) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static final class Settings {
        public boolean enabled;
        public boolean debug;
        public boolean whitelistHiddenFromConsole;
        public boolean whitelistHiddenFromWhitelistUsers;
        public boolean whitelistVisibleToAdmins;
        public boolean alertsEnabled;
        public boolean consoleAlerts;
        public boolean adminAlerts;
        public boolean punishmentsEnabled;
        public double lowTpsThreshold;
        public double lowTpsPunishmentMultiplier;
        public boolean setbackEnabled;
        public int setbackCooldownTicks;
        public final Map<String, CheckSettings> checks = new LinkedHashMap<>();
        public final Map<String, String> punishmentCommands = new LinkedHashMap<>();

        private static Settings defaults() {
            Settings settings = new Settings();
            settings.enabled = true;
            settings.debug = false;
            settings.whitelistHiddenFromConsole = true;
            settings.whitelistHiddenFromWhitelistUsers = true;
            settings.whitelistVisibleToAdmins = true;
            settings.alertsEnabled = true;
            settings.consoleAlerts = true;
            settings.adminAlerts = true;
            settings.punishmentsEnabled = false;
            settings.lowTpsThreshold = 18.0;
            settings.lowTpsPunishmentMultiplier = 0.5;
            settings.setbackEnabled = true;
            settings.setbackCooldownTicks = 40;
            for (String id : new String[]{"speed", "fly", "nofall", "phase", "velocity", "timer", "step"}) {
                settings.checks.put(id, new CheckSettings(true, 5.0, 20.0, 0.25, 0.0));
                settings.punishmentCommands.put(id, "ban %player% Unfair Advantage");
            }
            return settings;
        }
    }

    public record CheckSettings(boolean enabled, double alertThreshold, double punishThreshold, double decay, double buffer) { }
}