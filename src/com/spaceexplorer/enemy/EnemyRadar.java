package com.spaceexplorer.enemy;

import com.spaceexplorer.entity.Player;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * Tracks the nearest active enemy and draws a compact directional radar.
 */
public final class EnemyRadar {

    private static final double FAR_DISTANCE = 1500.0;
    private static final double NEAR_DISTANCE = 150.0;

    private final Font font = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private boolean active;
    private double direction;
    private double distance;
    private double size = 10.0;
    private Color color = new Color(70, 200, 110);
    private double pulseTime;

    public void update(double dt, Player player, EnemyManager manager) {
        pulseTime += dt;
        Enemy nearest = manager == null ? null : manager.getNearestEnemy(player);
        if (nearest == null || !nearest.isActive()) {
            active = false;
            return;
        }

        active = true;
        double dx = nearest.getCenterX() - player.getCenterX();
        double dy = nearest.getCenterY() - player.getCenterY();
        distance = Math.sqrt(dx * dx + dy * dy);
        direction = Math.atan2(dy, dx);

        double proximity = (FAR_DISTANCE - distance) / (FAR_DISTANCE - NEAR_DISTANCE);
        proximity = Math.max(0.0, Math.min(1.0, proximity));

        size = 10.0 + 16.0 * proximity;

        Color far = new Color(70, 200, 110);
        Color midFar = new Color(225, 205, 70);
        Color midNear = new Color(235, 130, 45);
        Color near = new Color(225, 55, 55);

        if (proximity < 0.33) {
            color = lerp(far, midFar, proximity / 0.33);
        } else if (proximity < 0.66) {
            color = lerp(midFar, midNear, (proximity - 0.33) / 0.33);
        } else {
            color = lerp(midNear, near, (proximity - 0.66) / 0.34);
        }
    }

    public void draw(Graphics2D g, int viewWidth, int viewHeight) {
        if (!active) {
            return;
        }

        int centerX = viewWidth / 2;
        int centerY = 46;
        int boxWidth = 92;
        int boxHeight = 82;

        g.setColor(new Color(0, 0, 0, 155));
        g.fillRoundRect(centerX - boxWidth / 2, centerY - boxHeight / 2 + 8,
                boxWidth, boxHeight, 12, 12);
        g.setColor(new Color(255, 255, 255, 45));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(centerX - boxWidth / 2, centerY - boxHeight / 2 + 8,
                boxWidth, boxHeight, 12, 12);

        double pulse = (distance < 180.0) ? (1.0 + 0.12 * Math.sin(pulseTime * 6.0)) : 1.0;
        double arrowSize = size * pulse;

        Graphics2D arrowGraphics = (Graphics2D) g.create();
        arrowGraphics.translate(centerX, centerY);
        arrowGraphics.rotate(direction);

        Path2D arrow = new Path2D.Double();
        arrow.moveTo(arrowSize, 0);
        arrow.lineTo(-arrowSize * 0.55, -arrowSize * 0.62);
        arrow.lineTo(-arrowSize * 0.22, 0);
        arrow.lineTo(-arrowSize * 0.55, arrowSize * 0.62);
        arrow.closePath();

        arrowGraphics.setColor(color);
        arrowGraphics.fill(arrow);
        arrowGraphics.setColor(new Color(255, 255, 255, 90));
        arrowGraphics.fill(new java.awt.geom.Ellipse2D.Double(-arrowSize * 0.12, -arrowSize * 0.12,
                arrowSize * 0.24, arrowSize * 0.24));
        arrowGraphics.dispose();

        g.setFont(font);
        g.setColor(color);
        String text = Math.round(distance) + "m";
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, centerX - textWidth / 2, centerY + 34);
    }

    private static Color lerp(Color from, Color to, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        int red = (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t);
        int green = (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int blue = (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        return new Color(red, green, blue);
    }
}