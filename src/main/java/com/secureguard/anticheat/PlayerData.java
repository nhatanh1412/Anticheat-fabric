package com.secureguard.anticheat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.Set;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;
    private Level lastLevel;
    private boolean initialized;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double lastVelocityX;
    private double lastVelocityY;
    private double lastVelocityZ;
    private float lastYaw;
    private float lastPitch;
    private boolean lastOnGround;
    private int joinGraceTicks = 60;
    private int teleportGraceTicks;
    private int velocityGraceTicks;
    private int airTicks;
    private double maximumFallDistance;
    private double lastHealth = 20.0;
    private double impulseX;
    private double impulseZ;
    private int impulseTicks;
    private int failedImpulseTicks;
    private boolean hasSafePosition;
    private double safeX;
    private double safeY;
    private double safeZ;
    private float safeYaw;
    private float safePitch;
    private long lastSetbackTick = Long.MIN_VALUE;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public MovementFrame sample(ServerPlayer player, long tick, int packetRate, double tps) {
        Vec3 position = player.position();
        Vec3 velocity = player.getDeltaMovement();
        Level level = player.level();
        boolean first = !initialized;
        boolean worldChanged = initialized && level != lastLevel;
        double dx = first || worldChanged ? 0.0 : position.x - lastX;
        double dy = first || worldChanged ? 0.0 : position.y - lastY;
        double dz = first || worldChanged ? 0.0 : position.z - lastZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        boolean largeMove = initialized && !worldChanged && distanceSquared > 64.0;
        if (worldChanged || largeMove) teleportGraceTicks = Math.max(teleportGraceTicks, 40);

        double velocityChangeX = velocity.x - lastVelocityX;
        double velocityChangeZ = velocity.z - lastVelocityZ;
        boolean newImpulse = initialized && velocityChangeX * velocityChangeX + velocityChangeZ * velocityChangeZ > 0.09;
        if (newImpulse) {
            impulseX = velocityChangeX;
            impulseZ = velocityChangeZ;
            impulseTicks = 8 + Math.min(8, Math.max(0, player.connection.latency()) / 50);
            failedImpulseTicks = 0;
            velocityGraceTicks = Math.max(velocityGraceTicks, 3);
        }

        boolean onGround = player.onGround();
        boolean collisionFree = level.noCollision(player, player.getBoundingBox());
        boolean water = player.isInWater();
        boolean lava = player.isInLava();
        boolean climbing = player.onClimbable();
        if (worldChanged) {
            hasSafePosition = false;
            maximumFallDistance = 0.0;
            airTicks = 0;
        }
        boolean wasOnGround = initialized && !worldChanged && lastOnGround;
        boolean landed = initialized && !worldChanged && !lastOnGround && onGround;
        double fallDistanceBeforeLanding = maximumFallDistance;
        double health = player.getHealth();
        boolean landedWithoutDamage = landed && fallDistanceBeforeLanding >= 4.0
                && health >= lastHealth - 0.01 && !water && !lava && !climbing
                && !player.isPassenger() && !player.hasEffect(MobEffects.SLOW_FALLING);
        if (onGround) {
            airTicks = 0;
            maximumFallDistance = 0.0;
        } else {
            airTicks++;
            maximumFallDistance = Math.max(maximumFallDistance, player.fallDistance);
        }

        boolean velocityMismatch = false;
        if (!newImpulse && impulseTicks > 0) {
            double impulseLengthSquared = impulseX * impulseX + impulseZ * impulseZ;
            double projection = dx * impulseX + dz * impulseZ;
            if (!onGround && collisionFree && impulseLengthSquared > 0.04 && projection < impulseLengthSquared * 0.12) {
                failedImpulseTicks++;
            } else {
                failedImpulseTicks = 0;
            }
            impulseTicks--;
            velocityMismatch = failedImpulseTicks >= 3;
        }

        boolean teleportGrace = teleportGraceTicks > 0;
        boolean velocityGrace = velocityGraceTicks > 0;
        boolean inGrace = first || joinGraceTicks > 0 || teleportGrace || velocityGrace;
        double friction = level.getBlockState(BlockPos.containing(position.x, position.y - 0.05, position.z)).getBlock().getFriction();
        if (onGround && collisionFree) {
            safeX = position.x;
            safeY = position.y;
            safeZ = position.z;
            safeYaw = player.getYRot();
            safePitch = player.getXRot();
            hasSafePosition = true;
        } else if (!hasSafePosition && collisionFree) {
            safeX = position.x;
            safeY = position.y;
            safeZ = position.z;
            safeYaw = player.getYRot();
            safePitch = player.getXRot();
            hasSafePosition = true;
        }

        MovementFrame frame = new MovementFrame(
                uuid, player.getPlainTextName(), position.x, position.y, position.z,
                dx, dy, dz, velocity.x, velocity.y, velocity.z,
                player.getYRot(), player.getXRot(), Math.hypot(dx, dz), player.getSpeed(), friction,
                player.fallDistance, fallDistanceBeforeLanding, health, player.connection.latency(), packetRate, airTicks, tps,
                onGround, wasOnGround, player.isCreative(), player.isSpectator(), player.isFallFlying(), player.isPassenger(),
                player.isSwimming(), water, lava, climbing, player.hasEffect(MobEffects.LEVITATION),
                player.hasEffect(MobEffects.SLOW_FALLING), player.isSprinting(), collisionFree,
                inGrace, teleportGrace, velocityGrace, landedWithoutDamage, velocityMismatch);

        lastLevel = level;
        lastX = position.x;
        lastY = position.y;
        lastZ = position.z;
        lastVelocityX = velocity.x;
        lastVelocityY = velocity.y;
        lastVelocityZ = velocity.z;
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
        lastOnGround = onGround;
        lastHealth = health;
        initialized = true;
        if (joinGraceTicks > 0) joinGraceTicks--;
        if (teleportGraceTicks > 0) teleportGraceTicks--;
        if (velocityGraceTicks > 0) velocityGraceTicks--;
        return frame;
    }

    public boolean setback(ServerPlayer player, long tick, int cooldownTicks) {
        if (!hasSafePosition || (lastSetbackTick != Long.MIN_VALUE && tick - lastSetbackTick < cooldownTicks)) return false;
        boolean moved = player.teleportTo((ServerLevel) player.level(), safeX, safeY, safeZ, Set.of(), safeYaw, safePitch, false);
        if (moved) {
            lastSetbackTick = tick;
            teleportGraceTicks = Math.max(teleportGraceTicks, 20);
            velocityGraceTicks = Math.max(velocityGraceTicks, 5);
        }
        return moved;
    }

    public UUID uuid() {
        return uuid;
    }

    public double currentX() {
        return lastX;
    }

    public double currentY() {
        return lastY;
    }

    public double currentZ() {
        return lastZ;
    }

    public float currentYaw() {
        return lastYaw;
    }

    public float currentPitch() {
        return lastPitch;
    }
}