package com.secureguard;

import com.secureguard.alert.AlertPreferences;
import com.secureguard.audit.AuditLogger;
import com.secureguard.command.CommandManager;
import com.secureguard.config.ConfigManager;
import com.secureguard.anticheat.AntiCheatManager;
import com.secureguard.permission.PermissionManager;
import com.secureguard.storage.UuidStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public final class SecureGuard implements ModInitializer {
    public static final String MOD_ID = "secureguard";
    public static final System.Logger LOGGER = System.getLogger(MOD_ID);
    public static ConfigManager CONFIG;
    public static UuidStore ADMINS;
    public static UuidStore WHITELIST;
    public static AuditLogger AUDIT;
    public static AlertPreferences ALERT_PREFERENCES;
    public static PermissionManager PERMISSIONS;
    public static DebugRegistry DEBUG_TARGETS;
    public static AntiCheatManager ANTICHEAT;
    public static volatile boolean consoleAlerts = true;
    public static volatile boolean antiCheatEnabled = true;

    @Override
    public void onInitialize() {
        Path directory = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        CONFIG = new ConfigManager(directory);
        ADMINS = new UuidStore(directory, "admins.json", "admins");
        WHITELIST = new UuidStore(directory, "whitelist.json", "whitelist");
        AUDIT = new AuditLogger(directory);
        ALERT_PREFERENCES = new AlertPreferences(directory);
        DEBUG_TARGETS = new DebugRegistry();
        PERMISSIONS = new PermissionManager(ADMINS, WHITELIST);
        loadSafely(CONFIG::load, "config");
        loadSafely(ADMINS::load, "admins");
        loadSafely(WHITELIST::load, "whitelist");
        try {
            WHITELIST.add(UUID.fromString("c7e49082-9d2e-46db-ba67-2c074688dc59"));
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not add the configured whitelist UUID.", exception);
        }
        loadSafely(ALERT_PREFERENCES::load, "alert preferences");
        consoleAlerts = CONFIG.get().consoleAlerts;
        CommandRegistrationCallback.EVENT.register(new CommandManager(PERMISSIONS)::register);
        ANTICHEAT = new AntiCheatManager();
        ANTICHEAT.register();
    }

    private static void loadSafely(IoAction action, String label) {
        try {
            action.run();
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not load SecureGuard " + label + "; using safe defaults.", exception);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }
}