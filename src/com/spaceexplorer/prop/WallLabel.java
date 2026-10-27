package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** A stencilled wall plate naming a compartment. Purely decorative. */
public final class WallLabel extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    private final String text;
    private final Color accent;

    public WallLabel(int tileX, int tileY, int tileW, int tileH,
                     String text, Color accent) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.text = text == null ? "" : text;
        this.accent = accent == null ? Palette.ACCENT_AMBER : accent;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {
        int plateX = x + 4;
        int plateY = y + 8;
        int plateWidth = Math.max(24, width - 8);
        int plateHeight = Math.max(14, height - 16);

        g.setColor(new Color(0, 0, 0, 100));
        g.fill(new RoundRectangle2D.Double(plateX, plateY, plateWidth, plateHeight, 4, 4));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 150));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(plateX, plateY, plateWidth, plateHeight, 4, 4));

        g.setColor(accent);
        g.fillRect(plateX + 4, plateY + 4, 3, plateHeight - 8);

        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        int textX = (int) (plateX + 10 + (plateWidth - 14
                - g.getFontMetrics().stringWidth(text)) / 2.0);
        int textY = (int) (plateY + plateHeight / 2.0 + g.getFontMetrics().getAscent() / 2.0 - 2);
        g.drawString(text, textX, textY);
    }
}
