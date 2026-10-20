package com.spaceexplorer;

import java.awt.Graphics2D;

/**
 * Base class for everything that dresses the inside of the ship.
 *
 * <p>Props are placed in whole tiles, which keeps the level definition simple
 * and matches the tile based collision of {@link World}. {@link #getLight()}
 * lets a prop contribute a light source, and {@link #blocksMovement()} decides
 * whether it is an obstacle.</p>
 */
public abstract class Prop {

    /** Which render pass this prop belongs to (floor decals vs. standing objects). */
    public enum Layer {
        /** Drawn under the player: floor grates, scorch marks. */
        FLOOR,
        /** Drawn above the player: crates, consoles, lamps. */
        OBJECT
    }

    protected final int x;
    protected final int y;
    protected final int width;
    protected final int height;
    protected final Layer layer;

    protected Prop(int tileX, int tileY, int tileW, int tileH, Layer layer) {
        this.x = tileX * World.TILE_SIZE;
        this.y = tileY * World.TILE_SIZE;
        this.width = tileW * World.TILE_SIZE;
        this.height = tileH * World.TILE_SIZE;
        this.layer = layer;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getCenterX() {
        return x + width / 2.0;
    }

    public double getCenterY() {
        return y + height / 2.0;
    }

    public Layer getLayer() {
        return layer;
    }

    /** Light this prop contributes to the lighting system, or null. */
    public Light getLight() {
        return null;
    }

    public boolean blocksMovement() {
        return layer == Layer.OBJECT;
    }

    /** Axis aligned overlap test against a world space box. */
    public boolean intersects(double px, double py, double boxWidth, double boxHeight) {
        return px < x + width
                && px + boxWidth > x
                && py < y + height
                && py + boxHeight > y;
    }

    /** Hook for future animation, called once per frame from {@link World#update(double)}. */
    public void update(double dt) {
        // no per-frame behaviour by default
    }

    /** Draws the prop in world coordinates, no camera translation applied. */
    public abstract void draw(Graphics2D g);
}
