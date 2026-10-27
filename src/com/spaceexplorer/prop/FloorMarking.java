package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A painted deck marking. It sits in the floor layer, so the player walks over
 * it, and is used for hangar fuel/cargo labels and direction markings.
 */
public final class FloorMarking extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final String text;
    private final Color accent;

    public FloorMarking(int tileX, int tileY, int tileW, int tileH,
                        String text, Color accent) {
        super(tileX, tileY, tileW, tileH, Layer.FLOOR);
        this.text = text == null ? "" : text;
        this.accent = accent == null ? Palette.HAZARD : accent;
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
        g.fill(new RoundRectangle2D.Double(x + 3, y + 4, width - 6, height - 8, 6, 6));

        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(x + 3, y + 4, width - 6, height - 8, 6, 6));

        // Broken hazard stripes at the ends keep the marking readable without
        // covering the deck like a solid warning panel.
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        for (int stripe = x + 7; stripe < x + 25; stripe += 9) {
            g.fillRect(stripe, y + height - 9, 5, 3);
        }
        for (int stripe = x + width - 30; stripe < x + width - 7; stripe += 9) {
            g.fillRect(stripe, y + height - 9, 5, 3);
        }

        g.setFont(LABEL_FONT);
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 190));
        int textX = (int) (getCenterX() - g.getFontMetrics().stringWidth(text) / 2.0);
        int textY = (int) (getCenterY() + g.getFontMetrics().getAscent() / 2.0 - 2);
        g.drawString(text, textX, textY);
    }
}
