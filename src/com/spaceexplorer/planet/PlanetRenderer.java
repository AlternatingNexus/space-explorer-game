package com.spaceexplorer.planet;

import com.spaceexplorer.entity.Player;
import com.spaceexplorer.world.Camera;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Renders the currently visible part of Eryx9. Terrain is drawn directly from
 * the deterministic chunk data, so no chunk is regenerated during rendering.
 */
public final class PlanetRenderer {

    private final PlanetWorld world;
    private final Camera camera;
    private BufferedImage backBuffer;

    public PlanetRenderer(PlanetWorld world, Camera camera) {
        this.world = world;
        this.camera = camera;
    }

    public void render(Graphics2D screen, Player player, Camera activeCamera) {
        int viewWidth = activeCamera.getViewWidth();
        int viewHeight = activeCamera.getViewHeight();

        if (backBuffer == null
                || backBuffer.getWidth() != viewWidth
                || backBuffer.getHeight() != viewHeight) {
            backBuffer = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D g = backBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(0x6B241F));
        g.fillRect(0, 0, viewWidth, viewHeight);

        int tileSize = PlanetWorld.TILE_SIZE;
        int firstTileX = (int) Math.floor(activeCamera.getX() / tileSize);
        int lastTileX = (int) Math.ceil((activeCamera.getX() + viewWidth) / tileSize);
        int firstTileY = (int) Math.floor(activeCamera.getY() / tileSize);
        int lastTileY = (int) Math.ceil((activeCamera.getY() + viewHeight) / tileSize);

        Graphics2D worldGraphics = (Graphics2D) g.create();
        worldGraphics.translate(-activeCamera.getX(), -activeCamera.getY());
        for (int tileY = firstTileY; tileY <= lastTileY; tileY++) {
            for (int tileX = firstTileX; tileX <= lastTileX; tileX++) {
                worldGraphics.setColor(new Color(world.getTileColor(tileX, tileY)));
                worldGraphics.fillRect(tileX * tileSize, tileY * tileSize, tileSize, tileSize);
            }
        }

        int firstChunkX = Math.floorDiv(firstTileX, PlanetWorld.CHUNK_SIZE);
        int lastChunkX = Math.floorDiv(lastTileX, PlanetWorld.CHUNK_SIZE);
        int firstChunkY = Math.floorDiv(firstTileY, PlanetWorld.CHUNK_SIZE);
        int lastChunkY = Math.floorDiv(lastTileY, PlanetWorld.CHUNK_SIZE);
        for (int chunkY = firstChunkY; chunkY <= lastChunkY; chunkY++) {
            for (int chunkX = firstChunkX; chunkX <= lastChunkX; chunkX++) {
                PlanetChunk chunk = world.getChunk(chunkX, chunkY);
                chunk.drawRocks(worldGraphics,
                        chunkX * PlanetWorld.CHUNK_SIZE * PlanetWorld.TILE_SIZE,
                        chunkY * PlanetWorld.CHUNK_SIZE * PlanetWorld.TILE_SIZE);
            }
        }

        player.draw(worldGraphics);
        worldGraphics.dispose();
        g.dispose();

        screen.drawImage(backBuffer, 0, 0, null);
    }
}