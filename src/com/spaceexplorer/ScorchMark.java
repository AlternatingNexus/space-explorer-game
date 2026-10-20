package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * Floor damage left over from whatever knocked the ship dark. Drawn under the
 * player and never blocks movement.
 */
public final class ScorchMark extends Prop {

    public ScorchMark(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.FLOOR);
    }

    @Override
    public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x + 4, y + 4, width - 12, height - 10));
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(new Ellipse2D.Double(x + width / 2, y + height / 2 - 5, 8, 10));

        g.setColor(new Color(20, 16, 14, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(x + 6, y + height / 2, x + width / 2 - 4, y + height / 2 - 2);
        g.drawLine(x + width / 2 + 4, y + height / 2 + 1, x + width - 6, y + height / 2 + 4);
    }
}
