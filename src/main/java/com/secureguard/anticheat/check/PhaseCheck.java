package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class PhaseCheck implements Check {
    @Override public String id() { return "phase"; }
    @Override public String name() { return "Phase"; }
    @Override public String description() { return "Detects sustained movement while the player's collision box intersects solid geometry."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.creative() || frame.spectator() || frame.vehicle() || frame.water()
                || frame.lava() || frame.climbing() || frame.collisionFree()) return 0.0;
        return frame.horizontalDistance() > 0.28 ? 0.7 : 0.0;
    }
}