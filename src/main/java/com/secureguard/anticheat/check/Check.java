package com.secureguard.anticheat.check;

import com.secureguard.anticheat.MovementFrame;

public interface Check {
    String id();
    String name();
    String description();
    double evaluate(MovementFrame frame);
}