package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class TimerCheck implements Check {
    @Override public String id() { return "timer"; }
    @Override public String name() { return "Timer"; }
    @Override public String description() { return "Checks bounded one-second movement-packet rates with ping and low-TPS tolerance."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.creative() || frame.spectator() || frame.vehicle() || frame.tps() < 18.0) return 0.0;
        int limit = 34 + Math.min(12, frame.ping() / 35);
        return frame.movementPacketsPerSecond() > limit
                ? Math.min(1.0, (double) (frame.movementPacketsPerSecond() - limit) / 12.0)
                : 0.0;
    }
}