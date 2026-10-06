package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class FlyCheck implements Check {
    @Override public String id() { return "fly"; }
    @Override public String name() { return "Fly"; }
    @Override public String description() { return "Flags prolonged near-zero vertical motion without a legitimate flight state."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.creative() || frame.spectator() || frame.fallFlying() || frame.vehicle()
                || frame.levitating() || frame.slowFalling() || frame.water() || frame.lava() || frame.climbing()
                || frame.onGround() || !frame.collisionFree()) return 0.0;
        if (frame.airTicks() >= 8 && Math.abs(frame.dy()) < 0.025 && Math.abs(frame.velocityY()) < 0.06) return 0.8;
        return 0.0;
    }
}