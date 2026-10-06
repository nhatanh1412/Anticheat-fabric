package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public final class NoFallCheck implements Check {
    @Override public String id() { return "nofall"; }
    @Override public String name() { return "NoFall"; }
    @Override public String description() { return "Checks high falls that land without observed damage, excluding common safe mechanics."; }

    @Override
    public double evaluate(MovementFrame frame) {
        if (frame.inGrace() || !frame.landedWithoutDamage() || frame.creative() || frame.spectator()
                || frame.vehicle() || frame.swimming() || frame.water() || frame.lava() || frame.climbing()
                || frame.slowFalling()) return 0.0;
        return frame.fallDistanceBeforeLanding() >= 8.0 ? 1.0 : 0.6;
    }
}