package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** A supply crate: a simple obstacle that gives the rooms some depth. */
public final class Crate extends Prop {

    public Crate(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(x + 2, y + height - 6, width - 2, 8));

        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 1, y + 1, width - 2, height - 2);
        g.setColor(Palette.METAL);
        g.fillRect(x + 3, y + 3, width - 6, height - 6);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 3, y + 3, width - 6, 3);

        // corner braces
        g.setColor(Palette.METAL_DARK);
        g.drawLine(x + 4, y + 4, x + width - 5, y + height - 5);
        g.drawLine(x + width - 5, y + 4, x + 4, y + height - 5);

        // cargo marking
        g.setColor(Palette.HAZARD);
        g.fillRect(x + width / 2 - 4, y + height / 2 - 3, 8, 6);
    }
}
