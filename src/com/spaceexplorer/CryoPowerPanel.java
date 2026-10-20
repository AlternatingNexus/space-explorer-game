package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The wall console that powers the cryosleep bay.
 *
 * <p>This is a local, one way switch: it brings the cryopods and their lights
 * online, but it does not touch the ship-wide lighting restored by the
 * {@link ControlPanel}. Its {@link Light} (and the pods' lights) only become
 * active once the panel is used, which is how an unpowered room stays dark even
 * after main power is back.</p>
 */
public final class CryoPowerPanel extends Prop implements Interactable {

    public static final String PROMPT_POWER = "Press E to restore cryopod power";
    public static final String STATUS_OFFLINE = "CRYOPOD POWER OFFLINE";
    public static final String STATUS_ONLINE = "CRYOPOD POWER ONLINE";
    public static final String MESSAGE_POWERED = "Restoring cryopod power...";

    private static final double INTERACT_RANGE = 130.0;

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);
    private static final Font STATUS_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private final Light panelLight;
    private final double interactX;
    private final double interactY;
    private boolean powered;

    public CryoPowerPanel(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.interactX = x + width / 2.0;
        this.interactY = y + height + 10.0;
        this.panelLight = new Light(interactX, interactY,
                120.0, 190.0,
                Palette.LED_EMERGENCY,
                new Color(0x9FDBFF),
                0.8);
    }

    @Override
    public Light getLight() {
        return panelLight;
    }

    public boolean isPowered() {
        return powered;
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
        return !powered;
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return powered ? STATUS_ONLINE : STATUS_OFFLINE;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        return powered ? null : PROMPT_POWER;
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (powered) {
            return false;
        }
        powered = true;
        player.say(MESSAGE_POWERED, 3.5);
        return true;
    }

    /* ---------- per frame + drawing ---------- */

    @Override
    public void update(double dt) {
        panelLight.setActive(powered);
    }

    @Override
    public void draw(Graphics2D g) {
        Color glow = powered ? Palette.ACCENT_BLUE : Palette.LED_EMERGENCY;
        Color textColor = powered ? Palette.SCREEN_TEXT_ON : Palette.SCREEN_TEXT_OFF;

        // soft bloom so the room's control stands out
        float bloomRadius = (float) (height * 1.4);
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), y + height * 0.7f, bloomRadius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(glow, 95), withAlpha(glow, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(getCenterX() - bloomRadius, y + height * 0.7f - bloomRadius,
                bloomRadius * 2, bloomRadius * 2));

        // housing
        g.setPaint(new GradientPaint(x, y, Palette.METAL, x, y + height, Palette.METAL_DARK));
        g.fill(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 8, 8));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 8, 8));

        // screen
        int screenX = x + 6;
        int screenY = y + 6;
        int screenW = width - 12;
        int screenH = 30;
        g.setColor(Palette.WALL_EDGE);
        g.fillRect(screenX - 2, screenY - 2, screenW + 4, screenH + 4);
        g.setColor(powered ? Palette.SCREEN_ON : Palette.SCREEN_OFF);
        g.fillRect(screenX, screenY, screenW, screenH);
        g.setColor(new Color(0, 0, 0, 40));
        for (int sy = screenY + 3; sy < screenY + screenH; sy += 4) {
            g.fillRect(screenX, sy, screenW, 1);
        }

        // snowflake / cryo icon on the screen
        int iconCx = screenX + 16;
        int iconCy = screenY + 13;
        g.setColor(glow);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(iconCx, iconCy - 9, iconCx, iconCy + 9);
        g.drawLine(iconCx - 7, iconCy - 4, iconCx + 7, iconCy + 4);
        g.drawLine(iconCx - 7, iconCy + 4, iconCx + 7, iconCy - 4);

        g.setFont(STATUS_FONT);
        g.setColor(textColor);
        g.drawString(powered ? "ON" : "OFF", screenX + 32, screenY + 20);

        // engraved label
        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("CRYO", x + 6, y + 44);
        g.drawString("POWER", x + 6, y + 53);

        // big switch button with an indicator LED
        int buttonCx = x + width - 12;
        int buttonCy = y + 46;
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new Ellipse2D.Double(buttonCx - 8, buttonCy - 8, 16, 16));
        g.setColor(powered ? Palette.LED_ON : Palette.LED_OFF);
        g.fill(new Ellipse2D.Double(buttonCx - 5, buttonCy - 5, 10, 10));
        g.setColor(glow);
        g.fill(new Ellipse2D.Double(buttonCx - 4, y + 35, 8, 5));

        // hazard band along the base
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 6; stripe < x + width - 22; stripe += 9) {
            g.fillRect(stripe, y + height - 8, 5, 4);
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}