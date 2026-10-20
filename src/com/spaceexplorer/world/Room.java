package com.spaceexplorer.world;

/**
 * One compartment of the ship, described by its walkable floor rectangle in
 * tile coordinates. Walls are generated automatically around the floor by
 * {@link World#buildWalls()}, so rooms simply have to be adjacent to connect.
 */
public class Room {

    private final String name;
    private final int tileX;
    private final int tileY;
    private final int tileWidth;
    private final int tileHeight;

    public Room(String name, int tileX, int tileY, int tileWidth, int tileHeight) {
        this.name = name;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public String getName() {
        return name;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getRightTile() {
        return tileX + tileWidth - 1;
    }

    public int getBottomTile() {
        return tileY + tileHeight - 1;
    }

    public boolean containsTile(int tx, int ty) {
        return tx >= tileX && tx <= getRightTile() && ty >= tileY && ty <= getBottomTile();
    }

    @Override
    public String toString() {
        return name;
    }
}
