package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The small surface shuttle parked in the hangar.
 *
 * <p>It is environmental scenery only: it has no interaction prompt and cannot
 * be entered or launched. The open rear ramp is drawn so the hangar reads as a
 * future departure point without promising a mechanic that does not exist yet.</p>
 */
public final class Shuttle extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 10);
    private static final Font SMALL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);

    private double time;

    public Shuttle(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void draw(Graphics2D g) {
        double cx = getCenterX();
        int hullLeft = x + 38;
        int hullRight = x + width - 38;
        int hullTop = y + 15;
        int hullBottom = y + height - 26;

        // ground shadow
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(x + 10, y + height - 24, width - 20, 30));

        // swept wings
        Polygon leftWing = new Polygon(
                new int[]{hullLeft + 8, x + 8, x + 22, hullLeft + 8},
                new int[]{hullTop + 18, hullTop + 46, hullTop + 70, hullBottom - 6}, 4);
        Polygon rightWing = new Polygon(
                new int[]{hullRight - 8, x + width - 8, x + width - 22, hullRight - 8},
                new int[]{hullTop + 18, hullTop + 46, hullTop + 70, hullBottom - 6}, 4);
        g.setColor(Palette.METAL_DARK);
        g.fill(leftWing);
        g.fill(rightWing);
        g.setColor(Palette.METAL);
        g.fill(new Polygon(
                new int[]{hullLeft + 10, x + 14, x + 25, hullLeft + 10},
                new int[]{hullTop + 22, hullTop + 47, hullTop + 65, hullBottom - 10}, 4));
        g.fill(new Polygon(
                new int[]{hullRight - 10, x + width - 14, x + width - 25, hullRight - 10},
                new int[]{hullTop + 22, hullTop + 47, hullTop + 65, hullBottom - 10}, 4));

        // engine pods at the rear corners
        drawEngine(g, x + 24, y + height - 34);
        drawEngine(g, x + width - 42, y + height - 34);

        // main fuselage
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new RoundRectangle2D.Double(hullLeft, hullTop, hullRight - hullLeft,
                hullBottom - hullTop, 30, 30));
        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(hullLeft + 4, hullTop + 4,
                hullRight - hullLeft - 8, hullBottom - hullTop - 8, 26, 26));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(hullLeft, hullTop, hullRight - hullLeft,
                hullBottom - hullTop, 30, 30));

        // cockpit glass at the nose
        g.setColor(new Color(0x0A2534));
        g.fill(new RoundRectangle2D.Double(cx - 24, hullTop + 9, 48, 23, 18, 18));
        g.setColor(new Color(0x7FD6FF));
        g.fill(new RoundRectangle2D.Double(cx - 19, hullTop + 13, 38, 12, 10, 10));
        g.setColor(new Color(255, 255, 255, 125));
        g.fillRect((int) (cx - 15), hullTop + 15, 12, 2);

        // hull panel lines and identification stripe
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(hullLeft + 6, hullTop + 43, hullRight - 6, hullTop + 43);
        g.setColor(Palette.HAZARD);
        g.fillRect(hullLeft + 8, hullTop + 48, hullRight - hullLeft - 16, 4);
        g.setColor(Palette.TEXT_PRIMARY);
        g.setFont(LABEL_FONT);
        g.drawString("SHUTTLE", (int) (cx - g.getFontMetrics().stringWidth("SHUTTLE") / 2.0),
                hullTop + 64);
        g.setFont(SMALL_FONT);
        g.drawString("S-01", (int) (cx - g.getFontMetrics().stringWidth("S-01") / 2.0),
                hullTop + 75);

        // open rear ramp / visible entrance
        g.setColor(new Color(0x0B111A));
        g.fill(new RoundRectangle2D.Double(cx - 25, hullBottom - 4, 50, 25, 5, 5));
        g.setColor(Palette.METAL_DARK);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(cx - 25, hullBottom - 4, 50, 25, 5, 5));
        g.setColor(Palette.HAZARD);
        g.fillRect((int) cx - 21, hullBottom + 17, 42, 3);
        g.setColor(Palette.ACCENT_AMBER);
        g.fillRect((int) cx - 17, hullBottom + 4, 3, 13);
        g.fillRect((int) cx + 14, hullBottom + 4, 3, 13);
        g.setFont(SMALL_FONT);
        g.setColor(Palette.ACCENT_AMBER);
        g.drawString("ENTRY", (int) (cx - g.getFontMetrics().stringWidth("ENTRY") / 2.0),
                hullBottom + 14);

        // running lights
        float blink = (float) (0.45 + 0.55 * Math.sin(time * 3.2));
        g.setColor(new Color(Palette.LED_ON.getRed(), Palette.LED_ON.getGreen(),
                Palette.LED_ON.getBlue(), Math.round(120 + 110 * blink)));
        g.fill(new Ellipse2D.Double(x + 15, hullTop + 45, 5, 5));
        g.setColor(new Color(Palette.LED_EMERGENCY.getRed(), Palette.LED_EMERGENCY.getGreen(),
                Palette.LED_EMERGENCY.getBlue(), Math.round(160 - 100 * blink)));
        g.fill(new Ellipse2D.Double(x + width - 20, hullTop + 45, 5, 5));
    }

    private void drawEngine(Graphics2D g, int ex, int ey) {
        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(ex, ey, 18, 27, 7, 7));
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new RoundRectangle2D.Double(ex + 3, ey + 3, 12, 7, 4, 4));
        g.setColor(Palette.ACCENT_BLUE);
        g.fill(new Ellipse2D.Double(ex + 5, ey + 17, 8, 6));
    }
}
