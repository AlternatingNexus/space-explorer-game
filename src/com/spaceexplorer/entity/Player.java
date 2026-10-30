package com.spaceexplorer.entity;

import com.spaceexplorer.core.InputHandler;
import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.World;
import com.spaceexplorer.world.WorldLike;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The player character. Movement is expressed in pixels per second and driven
 * by delta time, so the walk speed is identical whatever the frame rate is.
 * Collision is resolved one axis at a time against {@link World}, which gives
 * the usual "slide along the wall" feel without any physics engine.
 */
public class Player {

    public static final int WIDTH = 20;
    public static final int HEIGHT = 20;

    private static final double WALK_SPEED = 155.0;
    private static final double RUN_SPEED = 240.0;

    private double x;
    private double y;
    private double facingAngle;
    private double walkPhase;
    private boolean moving;

    private String message = "";
    private double messageTimer;

    public Player(int tileX, int tileY) {
        this.x = tileX * World.TILE_SIZE + (World.TILE_SIZE - WIDTH) / 2.0;
        this.y = tileY * World.TILE_SIZE + (World.TILE_SIZE - HEIGHT) / 2.0;
        this.facingAngle = 0.0;
    }

    /* ---------- movement ---------- */

    public void update(double dt, InputHandler input, WorldLike world) {
        double dx = 0.0;
        double dy = 0.0;
        if (input.isLeft()) {
            dx -= 1.0;
        }
        if (input.isRight()) {
            dx += 1.0;
        }
        if (input.isUp()) {
            dy -= 1.0;
        }
        if (input.isDownKey()) {
            dy += 1.0;
        }

        if (dx == 0.0 && dy == 0.0) {
            moving = false;
        } else {
            double length = Math.sqrt(dx * dx + dy * dy);
            double speed = input.isRunning() ? RUN_SPEED : WALK_SPEED;
            double stepX = dx / length * speed * dt;
            double stepY = dy / length * speed * dt;

            move(stepX, 0.0, world);
            move(0.0, stepY, world);

            facingAngle = Math.atan2(dy, dx);
            moving = true;
            walkPhase += dt * 11.0;
        }

        if (messageTimer > 0.0) {
            messageTimer -= dt;
            if (messageTimer <= 0.0) {
                message = "";
            }
        }
    }

    private void move(double deltaX, double deltaY, WorldLike world) {
        if (deltaX == 0.0 && deltaY == 0.0) {
            return;
        }
        double nextX = x + deltaX;
        double nextY = y + deltaY;
        if (!world.isBlocked(nextX, y, WIDTH, HEIGHT)) {
            x = nextX;
        }
        if (!world.isBlocked(x, nextY, WIDTH, HEIGHT)) {
            y = nextY;
        }
    }

    /* ---------- state ---------- */

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getCenterX() {
        return x + WIDTH / 2.0;
    }

    public double getCenterY() {
        return y + HEIGHT / 2.0;
    }

    public boolean isMoving() {
        return moving;
    }

    public String getMessage() {
        return message;
    }

    /** Shows a short lived toast in the HUD. */
    public void say(String text, double seconds) {
        this.message = text;
        this.messageTimer = seconds;
    }

    /* ---------- rendering ---------- */

    /**
     * A small top-down astronaut: helmet with a glowing visor, a suit jacket
     * with shoulder pads, arms, legs and boots. Everything stays inside the
     * existing {@link #WIDTH} x {@link #HEIGHT} bounds so movement and collision
     * are untouched, and the little walking bob is the same one as before.
     */
    public void draw(Graphics2D g) {
        int px = (int) Math.round(x);
        int py = (int) Math.round(y);
        int step = moving ? (int) Math.round(Math.sin(walkPhase) * 2.0) : 0;

        // contact shadow
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(px - 2, py + HEIGHT - 7, WIDTH + 4, 10));

        // backpack behind the shoulders
        g.setColor(Palette.METAL_DARK);
        g.fillRect(px - 2, py + 7, 3, 9);
        g.setColor(Palette.METAL);
        g.fillRect(px - 2, py + 13, 3, 2);

        // torso: the suit jacket
        g.setColor(Palette.PLAYER_BODY);
        g.fill(new RoundRectangle2D.Double(px + 2, py + 5, WIDTH - 4, 11, 6, 6));
        g.setColor(Palette.PLAYER_DARK);
        g.draw(new RoundRectangle2D.Double(px + 2, py + 5, WIDTH - 4, 11, 6, 6));

        // shoulder pads
        g.setColor(Palette.PLAYER_LIGHT);
        g.fillRect(px + 4, py + 6, 4, 2);
        g.fillRect(px + 12, py + 6, 4, 2);

        // arms swing along the sides, slightly out of phase with the legs
        g.setColor(Palette.PLAYER_BODY);
        g.fill(new RoundRectangle2D.Double(px + 0.5, py + 7 - step, 3, 8, 2, 2));
        g.fill(new RoundRectangle2D.Double(px + WIDTH - 3.5, py + 7 + step, 3, 8, 2, 2));
        g.setColor(Palette.PLAYER_LIGHT);
        g.fillRect(px + 1, py + 14 - step, 2, 2);
        g.fillRect(px + 17, py + 14 + step, 2, 2);

        // belt and chest lamp
        g.setColor(Palette.PLAYER_DARK);
        g.fillRect(px + 3, py + 12, WIDTH - 6, 2);
        g.setColor(Palette.ACCENT_AMBER);
        g.fillRect(px + 8, py + 8, 4, 3);
        g.setColor(new Color(Palette.ACCENT_AMBER.getRed(), Palette.ACCENT_AMBER.getGreen(),
                Palette.ACCENT_AMBER.getBlue(), 90));
        g.fillRect(px + 8, py + 8, 4, 3);

        // legs, one forward and one back while walking
        g.setColor(Palette.PLAYER_DARK);
        g.fill(new RoundRectangle2D.Double(px + 4, py + 14 - step, 4, 5, 2, 2));
        g.fill(new RoundRectangle2D.Double(px + 12, py + 14 + step, 4, 5, 2, 2));

        // boots
        g.setColor(new Color(0x182E38));
        g.fillRect(px + 4, py + 18 - step, 4, 2);
        g.fillRect(px + 12, py + 18 + step, 4, 2);

        // helmet (the head), with the visor facing the walking direction
        double cx = px + WIDTH / 2.0;
        double cy = py + 4.5;
        g.setColor(Palette.PLAYER_LIGHT);
        g.fill(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));
        g.setColor(Palette.PLAYER_DARK);
        g.draw(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));

        double visorX = cx + Math.cos(facingAngle) * 3.4;
        double visorY = cy + Math.sin(facingAngle) * 3.4;
        g.setColor(new Color(0x0B2230));
        g.fill(new Ellipse2D.Double(visorX - 3.5, visorY - 3, 7, 6));
        g.setColor(new Color(0x9B, 0xE8, 0xFF, 190));
        g.fill(new Ellipse2D.Double(visorX - 1.5, visorY - 2.5, 3, 2));
    }
}
