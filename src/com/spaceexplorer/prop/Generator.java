package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * The reactor / generator block that the control console is wired to. It glows
 * warm red while the ship is dark and cool blue once the power is back, which
 * gives the second room a clear "this is the power room" identity.
 */
public final class Generator extends Prop {

    private final Light coreLight;

    public Generator(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.coreLight = new Light(getCenterX(), getCenterY(),
                90.0,
                200.0,
                new Color(0xFF7A3C),
                new Color(0xBFE6FF),
                0.9);
    }

    @Override
    public Light getLight() {
        return coreLight;
    }

    @Override
    public void draw(Graphics2D g) {
        Color glow = coreLight.getColor();

        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(x - 4, y + height - 8, width + 8, 12));

        // main housing
        g.setPaint(new GradientPaint(x, y, Palette.METAL, x, y + height, Palette.METAL_DARK));
        g.fillRect(x + 2, y + 2, width - 4, height - 4);
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 2, y + 2, width - 5, height - 5);

        // top plate with ribs
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 5, y + 5, width - 10, 5);
        g.setColor(Palette.METAL_DARK);
        for (int rib = x + 8; rib < x + width - 8; rib += 7) {
            g.fillRect(rib, y + 7, 3, 26);
        }

        // glowing coils
        g.setColor(glow);
        for (int row = 0; row < 2; row++) {
            int cy = y + 16 + row * 12;
            g.fill(new Ellipse2D.Double(x + 14, cy, 12, 9));
            g.fill(new Ellipse2D.Double(x + width - 26, cy, 12, 9));
        }
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(new Ellipse2D.Double(x + 17, y + 17, 4, 3));
        g.fill(new Ellipse2D.Double(x + width - 23, y + 29, 4, 3));

        // hazard band along the base
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 4; stripe < x + width - 6; stripe += 10) {
            g.fillRect(stripe, y + height - 12, 6, 8);
        }
    }
}
