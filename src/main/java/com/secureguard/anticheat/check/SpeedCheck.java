package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class SpeedCheck implements Check {
    @Override public String id() { return "speed"; }
    @Override public String name() { return "Speed"; }
    @Override public String description() { return "Adaptive horizontal movement envelope with effect, friction, ping, and lag tolerance."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.creative() || frame.spectator() || frame.vehicle() || frame.fallFlying()
                || frame.swimming() || frame.water() || frame.lava() || frame.climbing()) return 0.0;
        double frictionScale = Math.sqrt(Math.max(0.3, frame.friction()) / 0.6);
        double expected = Math.max(0.19, frame.movementSpeed() * (frame.sprinting() ? 1.35 : 1.1)) * frictionScale;
        expected += 0.055 + Math.min(0.07, frame.ping() * 0.00045) + Math.max(0.0, 20.0 - frame.tps()) * 0.012;
        if (frame.horizontalDistance() <= expected) return 0.0;
        return Math.min(1.0, (frame.horizontalDistance() / expected - 1.0) * 1.5);
    }
}