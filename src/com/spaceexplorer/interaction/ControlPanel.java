package com.spaceexplorer.interaction;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

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
 * The prototype's objective: a wall mounted lighting control console.
 *
 * <p>It carries its own light so it stays the most eye catching object in the
 * dark ship (dim red while unpowered), it can only be used once, and it asks
 * the player to press E when they are close enough.</p>
 */
public class ControlPanel extends Prop {

    public static final String INTERACT_PROMPT = "Press E to restore power";
    private static final double INTERACT_RANGE = 130.0;

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);
    private static final Font STATUS_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

    private final Light consoleLight;
    private final double interactX;
    private final double interactY;
    private boolean activated;

    public ControlPanel(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.interactX = x + width / 2.0;
        this.interactY = y + height + 10.0;
        this.consoleLight = new Light(interactX, interactY,
                115.0, 210.0,
                Palette.LED_EMERGENCY,
                Palette.ACCENT_BLUE,
                0.9);
    }

    @Override
    public Light getLight() {
        return consoleLight;
    }

    public boolean isActivated() {
        return activated;
    }

    /** Restores the lights. Ignored when the console has already been used. */
    public void activate() {
        activated = true;
    }

    /** True when the player is close enough and the console is still idle. */
    public boolean canInteract(Player player) {
        if (activated) {
            return false;
        }
        double dx = player.getCenterX() - interactX;
        double dy = player.getCenterY() - interactY;
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    public String getPrompt() {
        return INTERACT_PROMPT;
    }

    @Override
    public void draw(Graphics2D g) {
        Color screenColor = activated ? Palette.SCREEN_ON : Palette.SCREEN_OFF;
        Color textColor = activated ? Palette.SCREEN_TEXT_ON : Palette.SCREEN_TEXT_OFF;
        Color ledColor = activated ? Palette.LED_ON : Palette.LED_EMERGENCY;

        // soft bloom, so the console reads as "the thing that makes light"
        float bloomRadius = (float) (height * 1.5);
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), y + height * 0.7f, bloomRadius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(ledColor, 95), withAlpha(ledColor, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(getCenterX() - bloomRadius, y + height * 0.7f - bloomRadius,
                bloomRadius * 2, bloomRadius * 2));

        // housing
        g.setPaint(new GradientPaint(x, y, Palette.METAL, x, y + height, Palette.METAL_DARK));
        g.fill(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 8, 8));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x + 1, y + 1, width - 2, height - 2, 8, 8));

        drawScreen(g, screenColor, textColor, ledColor);

        // engraved label under the screen
        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("LIGHTING", x + 7, y + 48);
        g.drawString("CONTROL", x + 7, y + 58);

        // large physical button with an indicator LED
        int buttonCx = x + width - 12;
        int buttonCy = y + 50;
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new Ellipse2D.Double(buttonCx - 8, buttonCy - 8, 16, 16));
        g.setColor(activated ? Palette.LED_ON : Palette.LED_OFF);
        g.fill(new Ellipse2D.Double(buttonCx - 5, buttonCy - 5, 10, 10));
        g.setColor(ledColor);
        g.fill(new Ellipse2D.Double(buttonCx - 4, y + 39, 8, 5));

        // hazard band along the base
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 6; stripe < x + width - 22; stripe += 9) {
            g.fillRect(stripe, y + height - 8, 5, 4);
        }
    }

    private void drawScreen(Graphics2D g, Color screenColor, Color textColor, Color ledColor) {
        int screenX = x + 6;
        int screenY = y + 6;
        int screenW = width - 12;
        int screenH = 32;

        g.setColor(Palette.WALL_EDGE);
        g.fillRect(screenX - 2, screenY - 2, screenW + 4, screenH + 4);
        g.setColor(screenColor);
        g.fillRect(screenX, screenY, screenW, screenH);

        g.setColor(new Color(0, 0, 0, 40));
        for (int sy = screenY + 3; sy < screenY + screenH; sy += 4) {
            g.fillRect(screenX, sy, screenW, 1);
        }

        // bulb icon: dark while the lights are out, lit once power is back
        int bulbCx = screenX + 16;
        int bulbCy = screenY + 12;
        g.setColor(ledColor);
        g.fill(new Ellipse2D.Double(bulbCx - 9, bulbCy - 10, 18, 18));
        g.setColor(screenColor);
        g.fillRect(bulbCx - 5, bulbCy + 6, 10, 6);
        if (!activated) {
            // fault mark on the dead bulb
            g.setColor(Palette.SCREEN_TEXT_OFF);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(bulbCx - 4, bulbCy - 8, bulbCx + 4, bulbCy);
            g.fillRect(bulbCx - 1, bulbCy + 8, 2, 4);
        }

        g.setFont(STATUS_FONT);
        g.setColor(textColor);
        g.drawString(activated ? "ON" : "OFF", screenX + 30, screenY + 22);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
