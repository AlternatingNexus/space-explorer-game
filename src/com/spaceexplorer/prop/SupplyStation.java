package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import java.util.function.BooleanSupplier;

/**
 * The small inventory console in the Supply Room. It is purely decorative: the
 * individual containers are the interactive objects.
 */
public final class SupplyStation extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);
    private static final Font SCREEN_FONT = new Font(Font.MONOSPACED, Font.BOLD, 8);

    private final Light screenLight;
    private final BooleanSupplier activeCondition;

    public SupplyStation(int tileX, int tileY, int tileW, int tileH) {
        this(tileX, tileY, tileW, tileH, null);
    }

    public SupplyStation(int tileX, int tileY, int tileW, int tileH, BooleanSupplier activeCondition) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.activeCondition = activeCondition;
        this.screenLight = new Light(getCenterX(), getCenterY(),
                55.0, 115.0,
                Palette.ACCENT_AMBER,
                Palette.ACCENT_BLUE,
                0.35);
    }

    @Override
    public void update(double dt) {
        if (activeCondition != null) {
            screenLight.setActive(activeCondition.getAsBoolean());
        }
    }

    @Override
    public Light getLight() {
        return screenLight;
    }

    @Override
    public void draw(Graphics2D g) {
        if (activeCondition != null && !activeCondition.getAsBoolean()) {
            return;
        }
        g.setColor(new Color(0, 0, 0, 75));
        g.fill(new Ellipse2D.Double(x + 3, y + height - 6, width - 5, 8));

        // counter body
        g.setPaint(new GradientPaint(x, y, Palette.METAL, x, y + height, Palette.METAL_DARK));
        g.fill(new RoundRectangle2D.Double(x + 1, y + 4, width - 2, height - 7, 5, 5));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x + 1, y + 4, width - 2, height - 7, 5, 5));

        // angled screen
        int screenX = x + 7;
        int screenY = y + 7;
        int screenW = Math.max(18, width - 26);
        int screenH = Math.max(10, height - 17);
        g.setColor(Palette.WALL_EDGE);
        g.fillRect(screenX - 2, screenY - 2, screenW + 4, screenH + 4);
        g.setColor(Palette.SCREEN_ON);
        g.fillRect(screenX, screenY, screenW, screenH);
        g.setColor(Palette.SCREEN_TEXT_ON);
        g.setFont(SCREEN_FONT);
        g.drawString("SUPPLY", screenX + 3, screenY + 9);
        g.drawString("MANIFEST", screenX + 3, screenY + 17);

        // little food / water status lights
        g.setColor(Palette.ACCENT_AMBER);
        g.fill(new Ellipse2D.Double(x + width - 14, y + 10, 5, 5));
        g.setColor(Palette.ACCENT_BLUE);
        g.fill(new Ellipse2D.Double(x + width - 14, y + 18, 5, 5));

        // stencilled label and hazard foot
        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("SUPPLY", x + 6, y + height - 4);
        g.setColor(Palette.HAZARD);
        for (int stripe = x + width / 2; stripe < x + width - 4; stripe += 8) {
            g.fillRect(stripe, y + height - 5, 4, 2);
        }
    }
}
