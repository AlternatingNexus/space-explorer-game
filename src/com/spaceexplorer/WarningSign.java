package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/** A bulkhead stencil marking a hazardous compartment. Small and static. */
public final class WarningSign extends Prop {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 8);

    public WarningSign(int tileX, int tileY) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void draw(Graphics2D g) {
        // backing plate
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 7, y + 7, 18, 18);
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.2f));
        g.drawRect(x + 7, y + 7, 18, 18);

        // hazard triangle
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(x + 16, y + 10);
        triangle.lineTo(x + 22, y + 21);
        triangle.lineTo(x + 10, y + 21);
        triangle.closePath();
        g.setColor(Palette.HAZARD);
        g.fill(triangle);
        g.setColor(new Color(0x3A2E10));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(triangle);

        // exclamation mark
        g.setColor(new Color(0x2A2110));
        g.fillRect(x + 15, y + 13, 2, 4);
        g.fillRect(x + 15, y + 18, 2, 2);

        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("HAZARD", x + 6, y + 30);
    }
}