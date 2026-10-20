package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A bulkhead emergency beacon. It rotates a dim amber beam behind a lamp cover,
 * and it contributes its own small warning light so the gas room stays readable
 * while the ship is running on emergency power.
 */
public final class WarningBeacon extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);

    private final Light beaconLight;
    private double time;

    public WarningBeacon(int tileX, int tileY, double emergencyRadius, double poweredRadius) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.beaconLight = new Light(getCenterX(), getCenterY(),
                emergencyRadius, poweredRadius,
                Palette.ACCENT_AMBER,
                new Color(0xE8F9FF),
                0.5);
    }

    @Override
    public Light getLight() {
        return beaconLight;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void draw(Graphics2D g) {
        double cx = getCenterX();
        double cy = getCenterY();

        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 3.2));
        g.setPaint(new RadialGradientPaint(
                (float) cx, (float) cy, 26f,
                new float[]{0f, 1f},
                new Color[]{withAlpha(Palette.ACCENT_AMBER, Math.round(40 + 80 * pulse)),
                        withAlpha(Palette.ACCENT_AMBER, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(cx - 26, cy - 26, 52, 52));

        // bracket + housing
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 12, y + 6, 8, 6);
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new RoundRectangle2D.Double(x + 6, y + 9, 20, 12, 4, 4));

        // lens, with the sweep of the rotating lamp
        g.setColor(Palette.WALL_EDGE);
        g.fillRect(x + 11, y + 13, 10, 8);
        double angle = time * 3.0;
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(withAlpha(Palette.ACCENT_AMBER, Math.round(120 + 120 * pulse)));
        g.drawLine((int) cx, y + 22,
                (int) (cx + Math.cos(angle) * 14),
                (int) (y + 22 + Math.sin(angle) * 10));
        g.setColor(withAlpha(Palette.ACCENT_AMBER, Math.round(150 + 100 * pulse)));
        g.fill(new Ellipse2D.Double(x + 13, y + 15, 4, 4));

        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("!WARN!", x + 7, y + 30);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}