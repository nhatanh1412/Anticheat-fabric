package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class StepCheck implements Check {
    @Override public String id() { return "step"; }
    @Override public String name() { return "Step"; }
    @Override public String description() { return "Flags unusually large grounded-to-air vertical displacement without jump or vehicle states."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || frame.creative() || frame.spectator() || frame.vehicle() || frame.fallFlying()
                || frame.levitating() || frame.slowFalling() || frame.water() || frame.lava() || frame.climbing()) return 0.0;
        if (frame.wasOnGround() && frame.dy() > 0.78 && frame.velocityY() < 0.45) return 0.8;
        return 0.0;
    }
}