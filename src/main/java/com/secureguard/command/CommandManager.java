package com.secureguard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.secureguard.SecureGuard;
import com.secureguard.permission.PermissionManager;
import com.secureguard.storage.UuidStore;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.UUID;

public final class CommandManager {
    private final PermissionManager permissions;

    public CommandManager(PermissionManager permissions) {
        this.permissions = permissions;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        registerRoot(dispatcher, "sg");
        registerRoot(dispatcher, "secureguard");
    }

    private void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        var root = Commands.literal(name).requires(permissions::isAuthorized)
                .then(Commands.literal("help").executes(context -> help(context.getSource())))
                .then(Commands.literal("info").executes(context -> reply(context.getSource(), "SecureGuard server-side protection is active.")))
                .then(Commands.literal("reload").requires(permissions::canSeeAdminCommands)
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("whitelist").requires(permissions::canSeeWhitelistCommands)
                        .then(Commands.literal("add").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> updateUuidStore(context.getSource(), SecureGuard.WHITELIST, "WHITELIST_ADD", StringArgumentType.getString(context, "player"), true))))
                        .then(Commands.literal("remove").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> updateUuidStore(context.getSource(), SecureGuard.WHITELIST, "WHITELIST_REMOVE", StringArgumentType.getString(context, "player"), false))))
                        .then(Commands.literal("list").executes(context -> listUuids(context.getSource(), "Whitelist", SecureGuard.WHITELIST))))
                .then(Commands.literal("admin").requires(permissions::canSeeAdminCommands)
                        .then(Commands.literal("add").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> updateUuidStore(context.getSource(), SecureGuard.ADMINS, "ADMIN_ADD", StringArgumentType.getString(context, "player"), true))))
                        .then(Commands.literal("remove").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> updateUuidStore(context.getSource(), SecureGuard.ADMINS, "ADMIN_REMOVE", StringArgumentType.getString(context, "player"), false))))
                        .then(Commands.literal("list").executes(context -> listUuids(context.getSource(), "Admins", SecureGuard.ADMINS))))
                .then(Commands.literal("anticheat").requires(permissions::canSeeAdminCommands)
                        .then(Commands.literal("on").executes(context -> setAntiCheat(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setAntiCheat(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> reply(context.getSource(), "AntiCheat is " + (SecureGuard.antiCheatEnabled ? "enabled" : "disabled") + "."))))
                .then(Commands.literal("alert").requires(permissions::canSeeAdminCommands)
                        .then(Commands.literal("on").executes(context -> setAlerts(context.getSource(), true)))
                        .then(Commands.literal("off").executes(context -> setAlerts(context.getSource(), false)))
                        .then(Commands.literal("status").executes(context -> alertStatus(context.getSource()))))
                .then(Commands.literal("ban").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "ban " + word(context, "player"), "BAN", word(context, "player")))
                                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> nativeCommand(context.getSource(), "ban " + word(context, "player") + " " + greedy(context, "reason"), "BAN", word(context, "player"))))))
                .then(Commands.literal("ban-ip").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "ban-ip " + word(context, "player"), "BAN_IP", word(context, "player")))
                                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(context -> nativeCommand(context.getSource(), "ban-ip " + word(context, "player") + " " + greedy(context, "reason"), "BAN_IP", word(context, "player"))))))
                .then(Commands.literal("unban").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "pardon " + word(context, "player"), "UNBAN", word(context, "player")))))
                .then(Commands.literal("unban-ip").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("ip", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "pardon-ip " + word(context, "ip"), "UNBAN_IP", word(context, "ip")))))
                .then(Commands.literal("tp").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> teleportSelf(context.getSource(), word(context, "player")))
                                .then(Commands.argument("target", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "teleport " + word(context, "player") + " " + word(context, "target"), "TP", word(context, "player") + " -> " + word(context, "target"))))))
                .then(Commands.literal("op").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "op " + word(context, "player"), "OP", word(context, "player")))))
                .then(Commands.literal("deop").requires(permissions::canSeeAdminCommands)
                        .then(Commands.argument("player", StringArgumentType.word()).executes(context -> nativeCommand(context.getSource(), "deop " + word(context, "player"), "DEOP", word(context, "player")))))
                .then(Commands.literal("restart").requires(permissions::canSeeAdminCommands).executes(context -> restart(context.getSource())))
                .then(Commands.literal("stop").requires(permissions::canSeeAdminCommands)
                        .executes(context -> nativeCommand(context.getSource(), "stop", "STOP", "server")))
                .then(Commands.literal("auth").requires(permissions::canSeeAdminCommands)
                        .then(Commands.literal("bypass").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> authBypass(context.getSource(), word(context, "player"))))))
                .then(Commands.literal("debug").requires(permissions::canSeeAdminCommands)
                        .then(Commands.literal("off").then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> debug(context.getSource(), word(context, "player"), false))))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(context -> debug(context.getSource(), word(context, "player"), true))))
                .then(Commands.argument("hidden", StringArgumentType.greedyString())
                        .requires(permissions::isConsoleOrWhitelist)
                        .executes(context -> executeHidden(context.getSource(), greedy(context, "hidden"))));
        dispatcher.register(root);
    }

    private int help(CommandSourceStack source) {
        if (permissions.isAdmin(source) || permissions.isConsole(source)) {
            String whitelistCommand = permissions.canSeeWhitelistCommands(source) ? "whitelist, " : "";
            return reply(source, "SecureGuard: help, info, reload, " + whitelistCommand + "admin, anticheat, alert, ban, ban-ip, unban, unban-ip, tp, op, deop, restart, stop, auth, debug");
        }
        if (permissions.isWhitelist(source) && permissions.canSeeWhitelistCommands(source)) return reply(source, "SecureGuard: help, info, whitelist");
        return reply(source, "SecureGuard: help, info");
    }

    private int reload(CommandSourceStack source) {
        try {
            SecureGuard.CONFIG.load();
            return reply(source, "SecureGuard reloaded.");
        } catch (IOException exception) {
            return fail(source, "SecureGuard could not reload its configuration: " + exception.getMessage());
        }
    }

    private int updateUuidStore(CommandSourceStack source, UuidStore store, String action, String input, boolean add) {
        UUID uuid = resolveUuid(source, input);
        if (uuid == null) {
            audit(source, action, input, "FAILED: target UUID unresolved");
            return fail(source, "Player not found. Use an online player name or UUID.");
        }
        String result;
        try {
            boolean changed = add ? store.add(uuid) : store.remove(uuid);
            result = changed ? "SUCCESS" : "UNCHANGED";
            audit(source, action, uuid.toString(), result);
            return reply(source, changed ? (add ? "Added " : "Removed ") + uuid + "." : "No change; UUID was already in that state.");
        } catch (IOException exception) {
            result = "FAILED: " + exception.getMessage();
            audit(source, action, uuid.toString(), result);
            return fail(source, "Could not save SecureGuard data.");
        }
    }

    private int listUuids(CommandSourceStack source, String label, UuidStore store) {
        return reply(source, label + " UUIDs: " + (store.snapshot().isEmpty() ? "(empty)" : store.snapshot().toString()));
    }

    private int setAntiCheat(CommandSourceStack source, boolean enabled) {
        SecureGuard.antiCheatEnabled = enabled;
        return reply(source, "AntiCheat " + (enabled ? "enabled" : "disabled") + ".");
    }

    private int setAlerts(CommandSourceStack source, boolean enabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            SecureGuard.consoleAlerts = enabled;
            return reply(source, "Console alerts " + (enabled ? "enabled" : "disabled") + ".");
        }
        try {
            SecureGuard.ALERT_PREFERENCES.setEnabled(player.getUUID(), enabled);
            return reply(source, "Personal alerts " + (enabled ? "enabled" : "disabled") + ".");
        } catch (IOException exception) {
            return fail(source, "Could not save alert preferences.");
        }
    }

    private int alertStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return reply(source, player == null ? "Console alerts are " + (SecureGuard.consoleAlerts ? "enabled" : "disabled") + "."
                : "Personal alerts are " + (SecureGuard.ALERT_PREFERENCES.isEnabled(player.getUUID()) ? "enabled" : "disabled") + ".");
    }

    private int nativeCommand(CommandSourceStack source, String command, String action, String target) {
        try {
            audit(source, action, target, "DISPATCHED");
            source.getServer().getCommands().performPrefixedCommand(source.withPermission(PermissionSet.ALL_PERMISSIONS), command);
            return 1;
        } catch (RuntimeException exception) {
            audit(source, action, target, "FAILED: " + exception.getMessage());
            return fail(source, "SecureGuard could not execute that server action.");
        }
    }

    private int teleportSelf(CommandSourceStack source, String target) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return fail(source, "Console must use /sg tp <player> <target>.");
        return nativeCommand(source, "teleport " + player.getPlainTextName() + " " + target, "TP", player.getPlainTextName() + " -> " + target);
    }

    private int restart(CommandSourceStack source) {
        audit(source, "RESTART", "server", "FAILED: restart is unsupported by the server environment");
        return fail(source, "Restart is not supported by this server environment.");
    }

    private int authBypass(CommandSourceStack source, String target) {
        audit(source, "AUTH_BYPASS", target, "DENIED: authentication system disabled");
        return fail(source, "SecureGuard authentication system is not enabled.");
    }

    private int debug(CommandSourceStack source, String input, boolean enable) {
        UUID uuid = resolveUuid(source, input);
        if (uuid == null) return fail(source, "Player not found. Use an online player name or UUID.");
        boolean changed = enable ? SecureGuard.DEBUG_TARGETS.enable(uuid) : SecureGuard.DEBUG_TARGETS.disable(uuid);
        return reply(source, changed ? "Debug " + (enable ? "enabled" : "disabled") + " for " + uuid + "." : "Debug state was already unchanged.");
    }

    private int executeHidden(CommandSourceStack source, String command) {
        String[] parts = command.strip().split("\\s+", 3);
        if (parts.length == 0 || parts[0].isBlank()) return help(source);
        String first = parts[0].toLowerCase(java.util.Locale.ROOT);
        if (first.equals("whitelist") || first.equals("admin")) {
            if (parts.length < 2) return fail(source, "Usage: /sg " + first + " <add|remove|list> [player]");
            String operation = parts[1].toLowerCase(java.util.Locale.ROOT);
            UuidStore store = first.equals("whitelist") ? SecureGuard.WHITELIST : SecureGuard.ADMINS;
            String actionPrefix = first.equals("whitelist") ? "WHITELIST_" : "ADMIN_";
            if (operation.equals("list")) return listUuids(source, first.equals("whitelist") ? "Whitelist" : "Admins", store);
            if ((operation.equals("add") || operation.equals("remove")) && parts.length == 3) {
                return updateUuidStore(source, store, actionPrefix + operation.toUpperCase(java.util.Locale.ROOT), parts[2], operation.equals("add"));
            }
            return fail(source, "Usage: /sg " + first + " <add|remove|list> [player]");
        }
        if (first.equals("help")) return help(source);
        if (first.equals("info")) return reply(source, "SecureGuard server-side protection is active.");
        if (first.equals("reload")) return reload(source);
        if (first.equals("anticheat")) {
            if (parts.length == 2 && parts[1].equalsIgnoreCase("on")) return setAntiCheat(source, true);
            if (parts.length == 2 && parts[1].equalsIgnoreCase("off")) return setAntiCheat(source, false);
            return reply(source, "AntiCheat is " + (SecureGuard.antiCheatEnabled ? "enabled" : "disabled") + ".");
        }
        if (first.equals("alert")) {
            if (parts.length == 2 && parts[1].equalsIgnoreCase("on")) return setAlerts(source, true);
            if (parts.length == 2 && parts[1].equalsIgnoreCase("off")) return setAlerts(source, false);
            return alertStatus(source);
        }
        if (first.equals("stop")) return nativeCommand(source, "stop", "STOP", "server");
        if (first.equals("restart")) return restart(source);
        if (first.equals("auth") && parts.length == 3 && parts[1].equalsIgnoreCase("bypass")) return authBypass(source, parts[2]);
        if (first.equals("debug") && parts.length >= 2) {
            if (parts.length == 3 && parts[1].equalsIgnoreCase("off")) return debug(source, parts[2], false);
            return debug(source, parts[1], true);
        }
        return fallbackNative(source, command);
    }

    private int fallbackNative(CommandSourceStack source, String input) {
        String[] parts = input.strip().split("\\s+", 2);
        if (parts.length == 0) return fail(source, "Unknown SecureGuard command.");
        String command = switch (parts[0].toLowerCase(java.util.Locale.ROOT)) {
            case "ban", "ban-ip", "unban", "unban-ip", "tp", "op", "deop" -> parts[0].toLowerCase(java.util.Locale.ROOT);
            default -> null;
        };
        if (command == null || parts.length < 2) return fail(source, "Unknown or incomplete SecureGuard command.");
        String vanilla = switch (command) {
            case "unban" -> "pardon " + parts[1];
            case "unban-ip" -> "pardon-ip " + parts[1];
            case "tp" -> "teleport " + parts[1];
            default -> command + " " + parts[1];
        };
        String action = switch (command) {
            case "ban" -> "BAN";
            case "ban-ip" -> "BAN_IP";
            case "unban" -> "UNBAN";
            case "unban-ip" -> "UNBAN_IP";
            case "tp" -> "TP";
            case "op" -> "OP";
            default -> "DEOP";
        };
        return nativeCommand(source, vanilla, action, parts[1]);
    }

    private UUID resolveUuid(CommandSourceStack source, String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(input);
            return player == null ? null : player.getUUID();
        }
    }

    private void audit(CommandSourceStack source, String action, String target, String result) {
        ServerPlayer player = source.getPlayer();
        try {
            SecureGuard.AUDIT.record(player == null ? "CONSOLE" : player.getPlainTextName(), player == null ? null : player.getUUID(), action, target, result);
        } catch (IOException exception) {
            SecureGuard.LOGGER.log(System.Logger.Level.ERROR, "Could not write SecureGuard audit entry for " + action, exception);
        }
    }

    private static String word(CommandContext<CommandSourceStack> context, String key) {
        return StringArgumentType.getString(context, key);
    }

    private static String greedy(CommandContext<CommandSourceStack> context, String key) {
        return StringArgumentType.getString(context, key);
    }

    private static int reply(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }

}