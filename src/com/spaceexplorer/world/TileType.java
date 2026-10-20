package com.spaceexplorer.world;

/** The kinds of tiles the ship is built from. */
public enum TileType {

    /** Empty space outside of the hull, never walked on. */
    VOID(false),
    /** Solid hull / bulkhead, blocks movement. */
    WALL(true),
    /** Normal deck plating. */
    FLOOR(false),
    /** Walkable airlock threshold drawn as a doorway. */
    DOOR(false);

    private final boolean solid;

    TileType(boolean solid) {
        this.solid = solid;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isWalkable() {
        return this == FLOOR || this == DOOR;
    }
}
