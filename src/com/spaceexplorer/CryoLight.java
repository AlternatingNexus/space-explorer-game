package com.spaceexplorer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;

/**
 * A bulkhead lamp for the cryosleep bay.
 *
 * <p>Unlike the ship-wide {@link WallLamp}s, these are fed by the cryo power
 * grid: the tube only lights up once the {@link CryoPowerPanel} has been used,
 * and its {@link Light} is inactive otherwise, so the bay stays dark until the
 * player restores local power.</p>
 */
public final class CryoLight extends Prop {

    private final CryoPowerPanel panel;
    private final Light lampLight;

    public CryoLight(int tileX, int tileY, CryoPowerPanel panel) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.panel = panel;
        this.lampLight = new Light(getCenterX(), getCenterY(),
                110.0, 170.0,
                Palette.LED_EMERGENCY,
                new Color(0xD8ECFF),
                0.85);
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
    public void update(double dt) {
        lampLight.setActive(panel.isPowered());
    }

    @Override
    public void draw(Graphics2D g) {
        boolean on = panel.isPowered();

        // bloom (a faint warm glow even when the bay has no local power)
        Color glow = on ? new Color(150, 230, 255) : new Color(255, 80, 60);

        if (on) {
            float radius = 34f;
            g.setPaint(new RadialGradientPaint(
                    (float) getCenterX(), (float) getCenterY() + 2f, radius,
                    new float[]{0f, 1f},
                    new Color[]{new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 120),
                            new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0)}));
            g.fill(new Ellipse2D.Double(getCenterX() - radius, getCenterY() + 2f - radius,
                    radius * 2, radius * 2));
        }

        // housing
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 6, y + 8, width - 12, height - 12);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 8, y + 10, width - 16, 2);

        // tube: cyan when the cryo grid is live, a dim red filament otherwise
        g.setColor(on ? new Color(0x9FE4FF) : new Color(0x5C1E18));
        g.fillRect(x + 8, y + 13, width - 16, height - 20);
        if (on) {
            g.setColor(new Color(255, 255, 255, 160));
            g.fillRect(x + 9, y + 14, width - 18, 2);
        }
    }
}