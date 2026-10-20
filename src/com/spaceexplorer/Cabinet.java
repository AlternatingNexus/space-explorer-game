package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** A bolted equipment locker or stores cabinet. Solid, and stencil labelled. */
public final class Cabinet extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);

    private final Stock stock;

    public Cabinet(int tileX, int tileY, int tileW, int tileH, Stock stock) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.stock = stock;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(x + 1, y + height - 7, width + 2, 8));

        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(x + 1, y, width - 2, height - 2, 5, 5));
        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(x + 3, y + 3, width - 6, height - 6, 4, 4));

        // central handle rails
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + width / 2 - 2, y + 5, 4, height - 13);

        // hazard band along the base
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 4; stripe < x + width - 6; stripe += 8) {
            g.fillRect(stripe, y + height - 7, 5, 3);
        }

        // stencil label: spares in the workshop, chemical in the stores
        g.setFont(LABEL_FONT);
        g.setColor(stock == Stock.TOOLS ? Palette.ACCENT_BLUE : Palette.GAS);
        String label = stock == Stock.TOOLS ? "PARTS" : "CHEM";
        int labelX = x + width / 2 - g.getFontMetrics().stringWidth(label) / 2;
        g.drawString(label, labelX, y + 13);
    }
}