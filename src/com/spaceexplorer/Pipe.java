package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A conduit running along a bulkhead. Drawn as a shaded cylinder with a
 * coupling ring every tile, which is enough to sell the sci-fi interior
 * without any sprite assets.
 */
public final class Pipe extends Prop {

    private final boolean horizontal;

    public Pipe(int tileX, int tileY, int tileW, int tileH, boolean horizontal) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.horizontal = horizontal;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 80));
        if (horizontal) {
            g.fillRect(x, y + height - 6, width, 6);
        } else {
            g.fillRect(x + width - 7, y, 7, height);
        }

        // body, shaded across its thickness
        g.setColor(Palette.METAL);
        g.fillRect(x, y, width, height);
        g.setColor(Palette.METAL_LIGHT);
        if (horizontal) {
            g.fillRect(x, y + height / 3, width, Math.max(2, height / 3));
        } else {
            g.fillRect(x + width / 3, y, Math.max(2, width / 3), height);
        }
        g.setColor(Palette.METAL_DARK);
        if (horizontal) {
            g.fillRect(x, y, width, 2);
            g.fillRect(x, y + height - 2, width, 2);
        } else {
            g.fillRect(x, y, 2, height);
            g.fillRect(x + width - 2, y, 2, height);
        }

        // coupling rings
        g.setColor(Palette.METAL_DARK);
        g.setStroke(new BasicStroke(2f));
        for (int ring = World.TILE_SIZE; ring < (horizontal ? width : height); ring += World.TILE_SIZE) {
            if (horizontal) {
                g.drawLine(x + ring, y, x + ring, y + height);
            } else {
                g.drawLine(x, y + ring, x + width, y + ring);
            }
        }
    }
}
