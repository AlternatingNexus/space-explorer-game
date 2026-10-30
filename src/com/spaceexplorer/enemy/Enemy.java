package com.spaceexplorer.enemy;

import com.spaceexplorer.entity.Player;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * A simple Eryx9 enemy placeholder. It only moves slowly toward the player and
 * has no combat, health, or damage behavior yet.
 */
public final class Enemy {

    private static final double MOVE_SPEED = 28.0;
    private static final double MIN_DISTANCE = 42.0;
    private static final int SIZE = 28;

    private double x;
    private double y;
    private boolean active = true;
    private double time;

    public Enemy(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getCenterX() {
        return x + SIZE / 2.0;
    }

    public double getCenterY() {
        return y + SIZE / 2.0;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void update(double dt, Player player) {
        if (!active) {
            return;
        }
        time += dt;

        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance <= MIN_DISTANCE || distance < 0.001) {
            return;
        }

        double step = MOVE_SPEED * dt;
        double allowed = distance - MIN_DISTANCE;
        if (step > allowed) {
            step = allowed;
        }
        x += dx / distance * step;
        y += dy / distance * step;
    }

    public void draw(Graphics2D g) {
        if (!active) {
            return;
        }

        double cx = getCenterX();
        double cy = getCenterY();

        // Shadow.
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new Ellipse2D.Double(cx - 15, cy + 10, 30, 10));

        // Body.
        g.setColor(new Color(0x1A0A12));
        g.fill(new Ellipse2D.Double(cx - 15, cy - 13, 30, 26));
        g.setColor(new Color(0x4A1020));
        g.fill(new Ellipse2D.Double(cx - 11, cy - 9, 22, 18));

        // Spikes.
        g.setColor(new Color(0x2A0814));
        Path2D spikes = new Path2D.Double();
        spikes.moveTo(cx - 12, cy - 4);
        spikes.lineTo(cx - 20, cy - 12);
        spikes.lineTo(cx - 8, cy - 8);
        spikes.moveTo(cx + 12, cy - 4);
        spikes.lineTo(cx + 20, cy - 12);
        spikes.lineTo(cx + 8, cy - 8);
        spikes.moveTo(cx, cy + 10);
        spikes.lineTo(cx - 6, cy + 18);
        spikes.lineTo(cx + 6, cy + 18);
        g.fill(spikes);

        // Eyes.
        g.setColor(new Color(0x8A20A0));
        g.fill(new Ellipse2D.Double(cx - 7, cy - 4, 5, 4));
        g.fill(new Ellipse2D.Double(cx + 2, cy - 4, 5, 4));
        g.setColor(new Color(0xDDAAFF));
        g.fill(new Ellipse2D.Double(cx - 5, cy - 3, 2, 2));
        g.fill(new Ellipse2D.Double(cx + 4, cy - 3, 2, 2));
    }
}