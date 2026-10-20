package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A wall mounted equipment shelf. The maintenance and storage decks each have a
 * few of these to sell the room's purpose: tools in the workshop, chemical
 * canisters in the stores. It is a solid obstacle.
 */
public final class Shelf extends Prop {

    private final Stock stock;

    public Shelf(int tileX, int tileY, int tileW, int tileH, Stock stock) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.stock = stock;
    }

    @Override
    public void draw(Graphics2D g) {
        int items = Math.max(2, width / 14);

        g.setColor(new Color(0, 0, 0, 70));
        g.fillRect(x + 3, y + height - 5, width - 6, 4);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x, y + height - 11, width, 6);
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x, y + height - 11, width, 2);

        for (int i = 0; i < items; i++) {
            int cx = x + 6 + i * Math.max(9, (width - 12) / (items - 1));
            if (stock == Stock.TOOLS) {
                drawTool(g, cx);
            } else {
                drawCanister(g, cx);
            }
        }
    }

    private void drawTool(Graphics2D g, int cx) {
        int deck = y + height - 11;
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Palette.METAL_LIGHT);
        g.drawLine(cx, deck, cx - 4, deck - 9);
        g.setColor(Palette.ACCENT_BLUE);
        g.drawLine(cx, deck, cx + 4, deck - 9);
    }

    private void drawCanister(Graphics2D g, int cx) {
        int deck = y + height - 11;
        g.setColor(new Color(0x2E4A3A));
        g.fill(new RoundRectangle2D.Double(cx - 3, deck - 9, 6, 9, 3, 3));
        g.setColor(Palette.HAZARD);
        g.fillRect(cx - 2, deck - 6, 4, 1);
        g.setColor(Palette.GAS);
        g.fillRoundRect(cx - 2, deck - 5, 4, 1, 1, 1);
    }
}