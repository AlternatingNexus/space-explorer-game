package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

import java.util.function.BooleanSupplier;

/**
 * A bulkhead lamp. It is the ship lighting itself: dim red on emergency power,
 * cool white once the console has been activated. It also contributes its own
 * {@link Light} to the lighting system.
 */
public final class WallLamp extends Prop {

    private final Light lampLight;
    private final BooleanSupplier activeCondition;

    public WallLamp(int tileX, int tileY, double emergencyRadius, double poweredRadius) {
        this(tileX, tileY, emergencyRadius, poweredRadius, null);
    }

    public WallLamp(int tileX, int tileY, double emergencyRadius, double poweredRadius,
                    BooleanSupplier activeCondition) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.lampLight = new Light(getCenterX(), getCenterY(),
                emergencyRadius, poweredRadius,
                Palette.LED_EMERGENCY,
                new Color(0xD8ECFF),
                0.85);
        this.activeCondition = activeCondition;
    }

    @Override
    public void update(double dt) {
        if (activeCondition != null) {
            lampLight.setActive(activeCondition.getAsBoolean());
        }
    }

    @Override
    public Light getLight() {
        return lampLight;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {
        Color glow = lampLight.isActive() ? lampLight.getColor() : Palette.METAL_DARK;

        // bloom
        float radius = 30f;
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), (float) getCenterY(), radius,
                new float[]{0f, 1f},
                new Color[]{new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 110),
                        new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
        g.fill(new Ellipse2D.Double(getCenterX() - radius, getCenterY() - radius,
                radius * 2, radius * 2));

        // housing
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 6, y + 8, width - 12, height - 12);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 8, y + 10, width - 16, 2);

        // tube
        g.setColor(glow);
        g.fillRect(x + 8, y + 13, width - 16, height - 20);
        g.setColor(new Color(255, 255, 255, 150));
        g.fillRect(x + 9, y + 14, width - 18, 2);
    }
}
