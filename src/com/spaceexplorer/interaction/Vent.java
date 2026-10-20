package com.spaceexplorer.interaction;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The ventilation unit bolted to the wall of the crew quarters: the second half
 * of the prototype.
 *
 * <p>While it is damaged the housing hangs open, the fan is missing blades and a
 * warning light blinks. It refuses to work until the player is carrying every
 * component from {@link #REQUIRED_PARTS}; a successful repair consumes those
 * parts, closes the panel and is a one way trip, which is what ends the
 * prototype.</p>
 */
public class Vent extends Prop implements Interactable {

    public static final List<RepairPartType> REQUIRED_PARTS = Collections.unmodifiableList(
            Arrays.asList(RepairPartType.FAN_BLADE, RepairPartType.POWER_CELL, RepairPartType.REPAIR_PANEL));

    public static final String STATUS_DAMAGED = "Ventilation system damaged";
    public static final String STATUS_MISSING_PARTS = "Missing repair parts";
    public static final String PROMPT_REPAIR = "Press E to repair ventilation";
    public static final String MESSAGE_REPAIRED = "Ventilation restored!";

    private static final double INTERACT_RANGE = 100.0;

    private final double interactX;
    private final double interactY;
    private boolean repaired;
    private double time;

    public Vent(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.interactX = x + width / 2.0;
        this.interactY = y + height + 10.0;
    }

    public boolean isRepaired() {
        return repaired;
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        double dx = player.getCenterX() - interactX;
        double dy = player.getCenterY() - interactY;
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !repaired && inventory.hasAll(REQUIRED_PARTS);
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return repaired ? null : STATUS_DAMAGED;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        if (repaired) {
            return null;
        }
        return isActionReady(player, inventory) ? PROMPT_REPAIR : STATUS_MISSING_PARTS;
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (!isActionReady(player, inventory)) {
            return false;
        }
        repaired = true;
        inventory.removeAll(REQUIRED_PARTS);
        player.say(MESSAGE_REPAIRED, 4.0);
        return true;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    /* ---------- drawing ---------- */

    @Override
    public void draw(Graphics2D g) {
        Color indicator = repaired ? Palette.LED_ON : Palette.LED_EMERGENCY;
        float blink = (float) (0.5 + 0.5 * Math.sin(time * (repaired ? 1.6 : 5.2)));

        // halo: this is the objective, so it should read as important
        float haloRadius = (float) (width * 0.95);
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), (float) getCenterY(), haloRadius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(indicator, Math.round(45 + 35 * blink)), withAlpha(indicator, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(getCenterX() - haloRadius, getCenterY() - haloRadius,
                haloRadius * 2, haloRadius * 2));

        // housing
        g.setPaint(new GradientPaint(x, y, Palette.METAL, x, y + height, Palette.METAL_DARK));
        g.fill(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 10, 10));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 10, 10));

        // duct opening
        int ductX = x + 8;
        int ductY = y + 10;
        int ductWidth = width - 16;
        int ductHeight = height - 24;
        g.setColor(new Color(0x161C28));
        g.fill(new RoundRectangle2D.Double(ductX, ductY, ductWidth, ductHeight, 6, 6));

        if (repaired) {
            drawWorkingFan(g, ductX, ductY, ductWidth, ductHeight);
            drawClosedPanel(g, ductX, ductY, ductWidth, ductHeight);
        } else {
            drawDamagedFan(g, ductX, ductY, ductWidth, ductHeight);
            drawHangingPanel(g, ductX, ductY, ductWidth, ductHeight);
        }

        drawIndicator(g, indicator, blink);
        if (!repaired) {
            drawHazardBand(g);
        }
    }

    /** Damaged unit: two bent blades, snapped stub and loose cabling. */
    private void drawDamagedFan(Graphics2D g, int ductX, int ductY, int ductWidth, int ductHeight) {
        double cx = ductX + ductWidth / 2.0;
        double cy = ductY + ductHeight / 2.0;

        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(cx - 13, cy - 5, 12, 4, 2, 2));
        g.fill(new RoundRectangle2D.Double(cx - 6, cy - 14, 4, 12, 2, 2));

        g.setColor(Palette.LED_OFF);
        g.fill(new RoundRectangle2D.Double(cx + 8, cy + 7, 9, 4, 2, 2));

        Graphics2D stub = (Graphics2D) g.create();
        stub.translate(cx, cy);
        stub.rotate(0.5);
        stub.setColor(Palette.METAL_LIGHT);
        stub.fill(new RoundRectangle2D.Double(4, -2, 7, 4, 2, 2));
        stub.dispose();

        // exposed wiring along the top of the duct
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(Palette.HAZARD);
        g.drawLine(ductX + 4, ductY + 4, ductX + 14, ductY + 4);
        g.setColor(Palette.SCREEN_TEXT_OFF);
        g.drawLine(ductX + 16, ductY + 4, ductX + 26, ductY + 7);
        g.setColor(Palette.ACCENT_BLUE);
        g.drawLine(ductX + 4, ductY + 7, ductX + 18, ductY + 10);
    }

    /** Repaired unit: full rotor turning behind a straight grille. */
    private void drawWorkingFan(Graphics2D g, int ductX, int ductY, int ductWidth, int ductHeight) {
        double cx = ductX + ductWidth / 2.0;
        double cy = ductY + ductHeight / 2.0;

        Graphics2D rotor = (Graphics2D) g.create();
        rotor.translate(cx, cy);
        rotor.rotate(time * 2.2);
        rotor.setColor(Palette.METAL_LIGHT);
        for (int blade = 0; blade < 4; blade++) {
            rotor.rotate(Math.PI / 2);
            rotor.fill(new RoundRectangle2D.Double(2, -4, 11, 8, 4, 4));
        }
        rotor.dispose();

        g.setColor(Palette.METAL_DARK);
        g.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new Ellipse2D.Double(cx - 2, cy - 2, 4, 4));

        // grille slats
        g.setColor(Palette.WALL_EDGE);
        for (int slat = ductY + 5; slat < ductY + ductHeight - 3; slat += 6) {
            g.fillRect(ductX + 2, slat, ductWidth - 4, 2);
        }
    }

    /** Broken cover swung open on its hinge. */
    private void drawHangingPanel(Graphics2D g, int ductX, int ductY, int ductWidth, int ductHeight) {
        Graphics2D panel = (Graphics2D) g.create();
        panel.translate(ductX + ductWidth, ductY + 4);
        panel.rotate(0.6);
        panel.setColor(Palette.METAL);
        panel.fill(new RoundRectangle2D.Double(0, 0, 9, ductHeight - 8, 3, 3));
        panel.setColor(Palette.WALL_EDGE);
        panel.setStroke(new BasicStroke(1.4f));
        panel.draw(new RoundRectangle2D.Double(0, 0, 9, ductHeight - 8, 3, 3));
        panel.setColor(Palette.HAZARD);
        panel.fillRect(2, 4, 5, 3);
        panel.dispose();

        g.setColor(Palette.METAL_LIGHT);
        g.fill(new Ellipse2D.Double(ductX + ductWidth - 3, ductY + ductHeight / 2.0 - 2, 4, 4));
    }

    /** Closed cover with corner bolts, plus air streaks showing it is running. */
    private void drawClosedPanel(Graphics2D g, int ductX, int ductY, int ductWidth, int ductHeight) {
        g.setColor(withAlpha(Palette.LED_ON, 60));
        g.setStroke(new BasicStroke(1.5f));
        for (int streak = 0; streak < 3; streak++) {
            int streakY = ductY + 8 + streak * 9;
            int offset = (int) ((time * 22 + streak * 12) % (ductWidth + 12)) - 6;
            g.drawLine(ductX + offset, streakY, ductX + offset + 9, streakY);
        }

        g.setColor(Palette.METAL_LIGHT);
        for (int bolt = 0; bolt < 4; bolt++) {
            double boltX = ductX + (bolt % 2 == 0 ? 2 : ductWidth - 5);
            double boltY = ductY + (bolt < 2 ? 2 : ductHeight - 6);
            g.fill(new Ellipse2D.Double(boltX, boltY, 3, 3));
        }
    }

    private void drawIndicator(Graphics2D g, Color indicator, float blink) {
        int ledX = x + width - 14;
        int ledY = y + 14;

        g.setColor(withAlpha(indicator, Math.round(60 + 120 * blink)));
        g.fill(new Ellipse2D.Double(ledX - 8, ledY - 8, 16, 16));
        g.setColor(withAlpha(indicator, Math.round(120 + 135 * blink)));
        g.fill(new Ellipse2D.Double(ledX - 4, ledY - 4, 8, 8));
    }

    private void drawHazardBand(Graphics2D g) {
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 5; stripe < x + width - 24; stripe += 9) {
            g.fillRect(stripe, y + height - 8, 5, 4);
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}