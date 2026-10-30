package com.spaceexplorer.world;

/**
 * Minimal world contract used by the player controller and camera-independent
 * movement code. Both the spaceship and the planetary surface implement it.
 */
public interface WorldLike {
    boolean isBlocked(double px, double py, double boxWidth, double boxHeight);
}