package com.spaceexplorer;

import java.awt.Color;

/**
 * A single light source. The darkness overlay removes alpha in proportion to
 * the strength of every light that overlaps it, which is the cheap 2D
 * "lighting system" the prototype is built around: adding more lights later
 * needs no changes to the renderer.
 */
public class Light {

    private double x;
    private double y;
    private final double emergencyRadius;
    private final double poweredRadius;
    private final Color emergencyColor;
    private final Color poweredColor;
    private final double strength;
    private final double flickerSeed;

    private boolean powered;
    private boolean active = true;
    private double time;

    public Light(double x, double y,
                 double emergencyRadius, double poweredRadius,
                 Color emergencyColor, Color poweredColor,
                 double strength) {
        this.x = x;
        this.y = y;
        this.emergencyRadius = emergencyRadius;
        this.poweredRadius = poweredRadius;
        this.emergencyColor = emergencyColor;
        this.poweredColor = poweredColor;
        this.strength = strength;
        this.flickerSeed = (x * 0.013 + y * 0.021) % 6.283;
        this.powered = false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /** Player carried lights move around, so their position is mutable. */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public boolean isPowered() {
        return powered;
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    /**
     * Switches the lamp off without forgetting its position or profile, used by
     * props that stop glowing once they have been picked up.
     */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Color getColor() {
        return powered ? poweredColor : emergencyColor;
    }

    public double getRadius() {
        return powered ? poweredRadius : emergencyRadius;
    }

    /** Powered lamps are steady, emergency lamps flicker a little. */
    public double getStrength() {
        if (!active) {
            return 0.0;
        }
        if (powered) {
            return strength;
        }
        return strength * (0.82 + 0.18 * Math.sin(time * 5.5 + flickerSeed));
    }

    public void update(double secondsElapsed) {
        this.time = secondsElapsed;
    }
}
