package com.spaceexplorer.ui;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.world.Camera;
import com.spaceexplorer.world.Lighting;
import com.spaceexplorer.world.Prop;
import com.spaceexplorer.world.World;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Draws the ship. Kept strictly separate from the game logic: it only reads
 * state and never changes it.
 *
 * <p>Draw order is world (floor, walls, floor decals) -> player -> standing
 * objects -> darkness overlay -> HUD, which keeps the HUD readable even while
 * the ship is dark.</p>
 */
public class WorldRenderer {

    private final World world;
    private final Camera camera;
    private BufferedImage backBuffer;

    public WorldRenderer(World world, Camera camera) {
        this.world = world;
        this.camera = camera;
    }

    public void render(Graphics2D screen, Player player, Lighting lighting, Hud hud, double secondsElapsed) {
        int viewWidth = camera.getViewWidth();
        int viewHeight = camera.getViewHeight();

        Graphics2D g = buffer(viewWidth, viewHeight);
        g.setColor(Palette.VOID);
        g.fillRect(0, 0, viewWidth, viewHeight);

        Graphics2D world2d = (Graphics2D) g.create();
        world2d.translate(-camera.getX(), -camera.getY());
        drawTiles(world2d);
        List<Prop> floorProps = world.getProps(Prop.Layer.FLOOR);
        for (Prop prop : floorProps) {
            prop.draw(world2d);
        }
        player.draw(world2d);
        List<Prop> objectProps = world.getProps(Prop.Layer.OBJECT);
        for (Prop prop : objectProps) {
            prop.draw(world2d);
        }
        world2d.dispose();
        g.dispose();

        screen.drawImage(backBuffer, 0, 0, null);
        lighting.render(screen, viewWidth, viewHeight, camera);
        hud.draw(screen, viewWidth, viewHeight, secondsElapsed);
    }

    private Graphics2D buffer(int viewWidth, int viewHeight) {
        if (backBuffer == null
                || backBuffer.getWidth() != viewWidth
                || backBuffer.getHeight() != viewHeight) {
            backBuffer = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g = backBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    private void drawTiles(Graphics2D g) {
        int size = World.TILE_SIZE;
        int firstTileX = Math.max(0, (int) Math.floor(camera.getX() / size));
        int lastTileX = Math.min(world.getWidthInTiles() - 1,
                (int) Math.ceil((camera.getX() + camera.getViewWidth()) / size));
        int firstTileY = Math.max(0, (int) Math.floor(camera.getY() / size));
        int lastTileY = Math.min(world.getHeightInTiles() - 1,
                (int) Math.ceil((camera.getY() + camera.getViewHeight()) / size));

        for (int ty = firstTileY; ty <= lastTileY; ty++) {
            for (int tx = firstTileX; tx <= lastTileX; tx++) {
                switch (world.tileAt(tx, ty)) {
                    case FLOOR:
                        drawFloor(g, tx, ty);
                        break;
                    case DOOR:
                        drawDoor(g, tx, ty);
                        break;
                    case WALL:
                        drawWall(g, tx, ty);
                        break;
                    default:
                        drawSpace(g, tx, ty);
                        break;
                }
            }
        }
    }

    /** Star field behind the ship, so the area outside the hull is not dead black. */
    private void drawSpace(Graphics2D g, int tx, int ty) {
        int size = World.TILE_SIZE;
        int px = tx * size;
        int py = ty * size;
        int hash = Math.abs(tx * 73856093 ^ ty * 19349663);
        if (hash % 11 == 0) {
            g.setColor(new Color(255, 255, 255, 40 + hash % 60));
            g.fillRect(px + (hash >> 3) % size, py + (hash >> 9) % size, 2, 2);
        }
        if (hash % 43 == 0) {
            g.setColor(new Color(190, 215, 255, 120));
            g.fillRect(px + (hash >> 5) % size, py + (hash >> 11) % size, 1, 1);
        }
    }

    private void drawFloor(Graphics2D g, int tx, int ty) {
        int size = World.TILE_SIZE;
        int px = tx * size;
        int py = ty * size;

        g.setColor(((tx + ty) & 1) == 0 ? Palette.FLOOR_A : Palette.FLOOR_B);
        g.fillRect(px, py, size, size);

        g.setColor(Palette.FLOOR_LINE);
        g.draw(new Line2D.Double(px + size - 0.5, py, px + size - 0.5, py + size));
        g.draw(new Line2D.Double(px, py + size - 0.5, px + size, py + size - 0.5));

        // A little deterministic panel detail so the deck is not a flat colour.
        if (((tx * 7 + ty * 13) % 5) == 0) {
            g.setColor(Palette.METAL_DARK);
            g.fillRect(px + 4, py + 4, 3, 3);
            g.fillRect(px + size - 7, py + size - 7, 3, 3);
        }
    }

    private void drawWall(Graphics2D g, int tx, int ty) {
        int size = World.TILE_SIZE;
        int px = tx * size;
        int py = ty * size;

        g.setColor(Palette.WALL_BODY);
        g.fillRect(px, py, size, size);

        // Rim light on the face that looks out of the room.
        if (!world.tileAt(tx, ty - 1).isSolid()) {
            g.setColor(Palette.WALL_TOP);
            g.fillRect(px, py, size, 5);
        }
        if (world.tileAt(tx, ty + 1).isWalkable()) {
            g.setColor(Palette.WALL_EDGE);
            g.fillRect(px, py + size - 3, size, 3);
        }
        if (world.tileAt(tx - 1, ty).isWalkable()) {
            g.setColor(Palette.WALL_EDGE);
            g.fillRect(px, py, 3, size);
        }
        if (world.tileAt(tx + 1, ty).isWalkable()) {
            g.setColor(Palette.WALL_EDGE);
            g.fillRect(px + size - 3, py, 3, size);
        }

        g.setColor(Palette.WALL_EDGE);
        g.draw(new Line2D.Double(px + 0.5, py + size - 0.5, px + size - 0.5, py + size - 0.5));
    }

    private void drawDoor(Graphics2D g, int tx, int ty) {
        int size = World.TILE_SIZE;
        int px = tx * size;
        int py = ty * size;

        g.setColor(Palette.DOOR_BODY);
        g.fillRect(px, py, size, size);

        // hazard stripes mark the threshold
        g.setColor(new Color(Palette.HAZARD.getRed(), Palette.HAZARD.getGreen(),
                Palette.HAZARD.getBlue(), 140));
        for (int stripe = px - size; stripe < px + size; stripe += 10) {
            g.fillRect(stripe, py + size - 10, 5, 6);
        }

        // frame posts on the sides of the opening
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(px, py, 4, size);
        g.fillRect(px + size - 4, py, 4, size);
        g.setColor(Palette.WALL_EDGE);
        g.fillRect(px + 4, py, 2, size);
        g.fillRect(px + size - 6, py, 2, size);
    }
}
