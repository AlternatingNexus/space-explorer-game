package com.spaceexplorer;

import java.awt.Color;
import java.awt.Graphics2D;

/** A floor vent / grate. Purely decorative, drawn under the player. */
public final class FloorGrate extends Prop {

    public FloorGrate(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.FLOOR);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Palette.FLOOR_LINE);
        g.fillRect(x + 2, y + 2, width - 4, height - 4);

        g.setColor(Palette.METAL_DARK);
        for (int slat = y + 5; slat < y + height - 4; slat += 5) {
            g.fillRect(x + 5, slat, width - 10, 3);
        }
        g.setColor(new Color(255, 255, 255, 25));
        g.fillRect(x + 3, y + 3, width - 6, 1);

        // corner bolts
        g.setColor(Palette.METAL);
        g.fillRect(x + 2, y + 2, 3, 3);
        g.fillRect(x + width - 5, y + 2, 3, 3);
        g.fillRect(x + 2, y + height - 5, 3, 3);
        g.fillRect(x + width - 5, y + height - 5, 3, 3);
    }
}
