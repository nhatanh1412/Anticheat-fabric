package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class VelocityCheck implements Check {
    @Override public String id() { return "velocity"; }
    @Override public String name() { return "Velocity"; }
    @Override public String description() { return "Checks repeated movement inconsistent with a server-observed impulse."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.velocityGrace() || frame.creative() || frame.spectator() || frame.vehicle()
                || frame.swimming() || frame.water() || frame.lava() || !frame.collisionFree()) return 0.0;
        return frame.velocityMismatch() ? 0.8 : 0.0;
    }
}