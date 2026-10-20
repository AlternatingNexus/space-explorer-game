package com.spaceexplorer.interaction;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A loose repair component lying somewhere in the ship.
 *
 * <p>Each part is picked up once with E, then it disappears and the
 * {@link Inventory} remembers it until it is fitted into the broken
 * {@link Vent} or the {@link GasLeak}. Parts do not block movement, so the
 * player simply walks over them, and each one carries a very dim light of its
 * own so it can still be spotted while the ship is running on emergency
 * power.</p>
 */
public class RepairPart extends Prop implements Interactable {

    private static final double INTERACT_RANGE = 96.0;

    private final RepairPartType type;
    private final Light partLight;
    private boolean collected;
    private double time;

    public RepairPart(RepairPartType type, int tileX, int tileY) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.type = type;
        this.partLight = new Light(x + width / 2.0, y + height / 2.0,
                78.0, 118.0,
                Palette.LED_ON,
                Palette.ACCENT_BLUE,
                0.5);
    }

    public RepairPartType getType() {
        return type;
    }

    public boolean isCollected() {
        return collected;
    }

    /** Parts lie on the floor, so the player can walk right over them. */
    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public Light getLight() {
        return partLight;
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        if (collected) {
            return false;
        }
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !collected;
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return null;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        return "Press E to collect " + type.getDisplayName();
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (collected) {
            return false;
        }
        collected = true;
        partLight.setActive(false);
        inventory.add(type);
        player.say(type.getDisplayName() + " collected", 3.0);
        return true;
    }

    /* ---------- per frame + drawing ---------- */

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void draw(Graphics2D g) {
        if (collected) {
            return;
        }
        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 2.4));

        // faint glow so the part is findable in an unlit ship
        float glowRadius = 24f;
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), (float) getCenterY(), glowRadius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(accentColor(), 80), withAlpha(accentColor(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(getCenterX() - glowRadius, getCenterY() - glowRadius,
                glowRadius * 2, glowRadius * 2));

        switch (type) {
            case FAN_BLADE -> drawFanBlade(g, pulse);
            case POWER_CELL -> drawPowerCell(g, pulse);
            case REPAIR_PANEL -> drawRepairPanel(g, pulse);
            case PIPE_CLAMP -> drawPipeClamp(g, pulse);
            case SEALING_COMPOUND -> drawSealingCompound(g, pulse);
        }

        // quiet pulsing outline hints that this is the interactable thing
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(withAlpha(Palette.ACCENT_AMBER, Math.round(35 + 70 * pulse)));
        g.draw(new RoundRectangle2D.Double(x + 2.5, y + 2.5, width - 5, height - 5, 8, 8));
    }

    private void drawFanBlade(Graphics2D g, float pulse) {
        double cx = getCenterX();
        double cy = getCenterY();

        Graphics2D blades = (Graphics2D) g.create();
        blades.translate(cx, cy);
        blades.rotate(time * 0.7);
        blades.setColor(Palette.METAL_LIGHT);
        for (int blade = 0; blade < 4; blade++) {
            blades.rotate(Math.PI / 2);
            blades.fill(new RoundRectangle2D.Double(3, -3.5, 9, 7, 4, 4));
        }
        blades.dispose();

        g.setColor(Palette.METAL_DARK);
        g.fill(new Ellipse2D.Double(cx - 4, cy - 4, 8, 8));
        g.setColor(withAlpha(Palette.ACCENT_BLUE, Math.round(140 + 80 * pulse)));
        g.fill(new Ellipse2D.Double(cx - 2, cy - 2, 4, 4));
    }

    private void drawPowerCell(Graphics2D g, float pulse) {
        int cellX = x + 8;
        int cellY = y + 10;

        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(cellX + 4, y + 5, 8, 5);

        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(cellX, cellY, 16, 14, 4, 4));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(cellX, cellY, 16, 14, 4, 4));

        g.setColor(withAlpha(Palette.LED_ON, Math.round(170 + 60 * pulse)));
        g.fillRect(cellX + 3, cellY + 4, 10, 6);
        g.setColor(Palette.SCREEN_TEXT_ON);
        g.fillRect(cellX + 3, cellY + 4, 10, 2);
    }

    private void drawRepairPanel(Graphics2D g, float pulse) {
        int panelX = x + 6;
        int panelY = y + 8;

        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(panelX, panelY, 20, 16, 4, 4));
        g.setColor(Palette.METAL_LIGHT);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(panelX + 3, panelY + 12, panelX + 10, panelY + 4);
        g.drawLine(panelX + 10, panelY + 12, panelX + 17, panelY + 4);

        g.setColor(withAlpha(Palette.ACCENT_AMBER, Math.round(120 + 90 * pulse)));
        for (int corner = 0; corner < 4; corner++) {
            double dotX = corner % 2 == 0 ? panelX + 2.5 : panelX + 15.5;
            double dotY = corner < 2 ? panelY + 2.5 : panelY + 11.5;
            g.fill(new Ellipse2D.Double(dotX, dotY, 2.5, 2.5));
        }
    }

    private void drawPipeClamp(Graphics2D g, float pulse) {
        int pipeY = y + 14;

        // the pipe stub the clamp belongs to
        g.setColor(Palette.METAL_DARK);
        g.fillRoundRect(x + 4, pipeY + 4, 24, 8, 4, 4);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 5, pipeY + 5, 22, 2);

        // the clamp band wrapped around it
        g.setColor(Palette.METAL);
        g.fillRoundRect(x + 10, pipeY + 1, 12, 14, 5, 5);
        g.setColor(Palette.METAL_LIGHT);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x + 10, pipeY + 1, 12, 14, 5, 5);

        // tightening bolt with a glowing thread
        g.setColor(Palette.WALL_EDGE);
        g.fillRect(x + 13, y + 4, 6, 5);
        g.setColor(withAlpha(Palette.ACCENT_BLUE, Math.round(140 + 80 * pulse)));
        g.fillRect(x + 15, y + 5, 2, 3);
    }

    private void drawSealingCompound(Graphics2D g, float pulse) {
        int canX = x + 7;
        int canY = y + 9;

        // nozzle + cap
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(canX + 6, y + 4, 5, 4);
        g.fillRect(canX + 6, y + 8, 5, 3);

        // canister body
        g.setColor(new Color(0x2E4A3A));
        g.fill(new RoundRectangle2D.Double(canX, canY, 18, 16, 4, 4));
        g.setColor(Palette.METAL_DARK);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(canX, canY, 18, 16, 4, 4));

        // hazard label with a glowing chemical stripe
        g.setColor(withAlpha(Palette.GAS, Math.round(150 + 90 * pulse)));
        g.fillRect(canX + 2, canY + 4, 14, 4);
        g.setColor(Palette.HAZARD);
        g.fillRect(canX + 2, canY + 10, 14, 2);
    }

    private Color accentColor() {
        return switch (type) {
            case FAN_BLADE -> Palette.ACCENT_BLUE;
            case POWER_CELL -> Palette.LED_ON;
            case REPAIR_PANEL -> Palette.ACCENT_AMBER;
            case PIPE_CLAMP -> Palette.ACCENT_BLUE;
            case SEALING_COMPOUND -> Palette.GAS;
        };
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}