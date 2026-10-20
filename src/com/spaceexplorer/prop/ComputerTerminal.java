package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.Color;
import java.awt.Graphics2D;

/** A wall mounted terminal. Cosmetic for now, but it sells the "working ship". */
public final class ComputerTerminal extends Prop {

    public ComputerTerminal(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 2, y + 3, width - 4, height - 6);
        g.setColor(Palette.METAL);
        g.fillRect(x + 4, y + 5, width - 8, height - 12);

        // screen with fake text rows
        g.setColor(Palette.SCREEN_OFF);
        g.fillRect(x + 6, y + 7, width - 12, height - 18);
        g.setColor(Palette.SCREEN_TEXT_OFF);
        g.fillRect(x + 9, y + 10, width - 22, 2);
        g.fillRect(x + 9, y + 14, width - 16, 2);
        g.fillRect(x + 9, y + 18, width - 26, 2);
        g.fillRect(x + 9, y + 22, width - 19, 2);

        g.setColor(Palette.LED_OFF);
        g.fillRect(x + 8, y + height - 10, 4, 3);
    }
}
