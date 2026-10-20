package com.spaceexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ship itself: a tile grid plus the rooms and props that sit on it, and
 * the collision queries the {@link Player} uses.
 *
 * <p>Level building works in three steps: add rooms (their floor rectangles are
 * carved out), mark doorways, then call {@link #buildWalls()} which turns every
 * empty tile that touches walkable floor into a solid bulkhead. Because rooms
 * that are placed next to each other simply share an open edge, corridors and
 * doorways connect themselves without any special case.</p>
 */
public class World {

    public static final int TILE_SIZE = 32;

    /** Safety bound for the hull growth loop, plenty for any sane layout. */
    private static final int MAX_HULL_PASSES = 16;

    private final int widthInTiles;
    private final int heightInTiles;
    private final TileType[][] tiles;
    private final List<Room> rooms = new ArrayList<>();
    private final List<Prop> props = new ArrayList<>();
    private final ControlPanel controlPanel;
    private CryoRoom cryoRoom;

    public World(int widthInTiles, int heightInTiles, ControlPanel controlPanel) {
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.controlPanel = controlPanel;
        this.tiles = new TileType[heightInTiles][widthInTiles];
        for (int y = 0; y < heightInTiles; y++) {
            for (int x = 0; x < widthInTiles; x++) {
                tiles[y][x] = TileType.VOID;
            }
        }
    }

    /* ---------- level construction ---------- */

    public void addRoom(Room room) {
        rooms.add(room);
        for (int ty = room.getTileY(); ty <= room.getBottomTile(); ty++) {
            for (int tx = room.getTileX(); tx <= room.getRightTile(); tx++) {
                tiles[ty][tx] = TileType.FLOOR;
            }
        }
    }

    /** Turns a floor tile into a drawn (but still walkable) airlock threshold. */
    public void setDoor(int tileX, int tileY) {
        tiles[tileY][tileX] = TileType.DOOR;
    }

    public void addProp(Prop prop) {
        props.add(prop);
    }

    /**
     * Wraps every open floor tile with a hull. Call once, after the rooms and
     * doorways are in place.
     *
     * <p>The first pass puts a one tile bulkhead against the deck, then the hull
     * keeps growing outwards until it stops changing. That closes the corners
     * and the diagonal pockets so the ship reads as a solid shell. Empty space
     * is never walkable, so growing the hull can never block a path.</p>
     */
    public void buildWalls() {
        growHull(true);
        for (int pass = 0; pass < MAX_HULL_PASSES; pass++) {
            if (!growHull(false)) {
                return;
            }
        }
    }

    /** @return true when at least one tile was turned into hull. */
    private boolean growHull(boolean fromFloor) {
        TileType[][] result = new TileType[heightInTiles][];
        boolean changed = false;
        for (int ty = 0; ty < heightInTiles; ty++) {
            result[ty] = tiles[ty].clone();
            for (int tx = 0; tx < widthInTiles; tx++) {
                if (tiles[ty][tx] != TileType.VOID) {
                    continue;
                }
                if (touches(tx, ty, fromFloor ? TileType.FLOOR : TileType.WALL)
                        || touches(tx, ty, TileType.DOOR)) {
                    result[ty][tx] = TileType.WALL;
                    changed = true;
                }
            }
        }
        System.arraycopy(result, 0, tiles, 0, heightInTiles);
        return changed;
    }

    private boolean touches(int tileX, int tileY, TileType type) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (tileAt(tileX + dx, tileY + dy) == type) {
                    return true;
                }
            }
        }
        return false;
    }

    /* ---------- queries ---------- */

    public TileType tileAt(int tileX, int tileY) {
        if (tileX < 0 || tileY < 0 || tileX >= widthInTiles || tileY >= heightInTiles) {
            return TileType.WALL;
        }
        return tiles[tileY][tileX];
    }

    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }

    public int getPixelWidth() {
        return widthInTiles * TILE_SIZE;
    }

    public int getPixelHeight() {
        return heightInTiles * TILE_SIZE;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public List<Prop> getProps() {
        return Collections.unmodifiableList(props);
    }

    public List<Prop> getProps(Prop.Layer layer) {
        List<Prop> result = new ArrayList<>();
        for (Prop prop : props) {
            if (prop.getLayer() == layer) {
                result.add(prop);
            }
        }
        return result;
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    /** The cryosleep bay, or null when the level has none. */
    public CryoRoom getCryoRoom() {
        return cryoRoom;
    }

    public void setCryoRoom(CryoRoom cryoRoom) {
        this.cryoRoom = cryoRoom;
    }

    /**
     * Every prop the player can press E on, in level order. The lighting console
     * is handled separately in {@link Game} because restoring power also drives
     * the lighting system.
     */
    public List<Interactable> getInteractables() {
        List<Interactable> result = new ArrayList<>();
        for (Prop prop : props) {
            if (prop instanceof Interactable) {
                result.add((Interactable) prop);
            }
        }
        return result;
    }

    /** Every light contributed by a prop in this level. */
    public List<Light> getLights() {
        List<Light> lights = new ArrayList<>();
        for (Prop prop : props) {
            Light light = prop.getLight();
            if (light != null) {
                lights.add(light);
            }
        }
        return lights;
    }

    /**
     * True when the given world space box overlaps a solid tile or a solid prop.
     * The player uses this to stop dead instead of walking through the hull.
     */
    public boolean isBlocked(double px, double py, double boxWidth, double boxHeight) {
        int minTileX = (int) Math.floor(px / TILE_SIZE);
        int maxTileX = (int) Math.floor((px + boxWidth - 0.0001) / TILE_SIZE);
        int minTileY = (int) Math.floor(py / TILE_SIZE);
        int maxTileY = (int) Math.floor((py + boxHeight - 0.0001) / TILE_SIZE);

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                if (tileAt(tx, ty).isSolid()) {
                    return true;
                }
            }
        }
        for (Prop prop : props) {
            if (prop.blocksMovement() && prop.intersects(px, py, boxWidth, boxHeight)) {
                return true;
            }
        }
        return false;
    }

    public void update(double dt) {
        for (Prop prop : props) {
            prop.update(dt);
        }
    }
}
