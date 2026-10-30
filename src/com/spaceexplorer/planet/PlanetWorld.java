package com.spaceexplorer.planet;

import com.spaceexplorer.world.WorldLike;

import java.util.HashMap;
import java.util.Map;

/**
 * A small, streaming planetary surface. Terrain is generated deterministically
 * from chunk coordinates, while only chunks near the player are kept in memory.
 */
public final class PlanetWorld implements WorldLike {

    public static final int TILE_SIZE = 8;
    public static final int CHUNK_SIZE = 16;

    private static final int LOAD_RADIUS = 4;
    private static final int UNLOAD_RADIUS = 7;

    private final long seed;
    private final Map<Long, PlanetChunk> chunks = new HashMap<>();

    public PlanetWorld(long seed) {
        this.seed = seed;
    }

    public void update(double dt, com.spaceexplorer.entity.Player player) {
        ensureChunksAround(player.getCenterX(), player.getCenterY());
        unloadDistant(player.getCenterX(), player.getCenterY());
    }

    public int getTileColor(int tileX, int tileY) {
        int chunkX = Math.floorDiv(tileX, CHUNK_SIZE);
        int chunkY = Math.floorDiv(tileY, CHUNK_SIZE);
        PlanetChunk chunk = getChunk(chunkX, chunkY);
        int localX = Math.floorMod(tileX, CHUNK_SIZE);
        int localY = Math.floorMod(tileY, CHUNK_SIZE);
        return chunk.color(localX, localY);
    }

    @Override
    public boolean isBlocked(double px, double py, double boxWidth, double boxHeight) {
        double centerX = px + boxWidth / 2.0;
        double centerY = py + boxHeight / 2.0;
        int playerChunkX = Math.floorDiv((int) Math.floor(centerX / TILE_SIZE), CHUNK_SIZE);
        int playerChunkY = Math.floorDiv((int) Math.floor(centerY / TILE_SIZE), CHUNK_SIZE);

        for (Map.Entry<Long, PlanetChunk> entry : chunks.entrySet()) {
            long key = entry.getKey();
            int chunkX = (int) (key >> 32);
            int chunkY = (int) key;
            if (Math.abs(chunkX - playerChunkX) > 2 || Math.abs(chunkY - playerChunkY) > 2) {
                continue;
            }

            int chunkPixelX = chunkX * CHUNK_SIZE * TILE_SIZE;
            int chunkPixelY = chunkY * CHUNK_SIZE * TILE_SIZE;
            for (PlanetChunk.Rock rock : entry.getValue().rocks) {
                double rockX = chunkPixelX + rock.localX;
                double rockY = chunkPixelY + rock.localY;
                if (circleIntersectsRect(rockX, rockY, rock.collisionRadius,
                        px, py, boxWidth, boxHeight)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean circleIntersectsRect(double circleX, double circleY, double radius,
                                               double rectX, double rectY, double rectWidth, double rectHeight) {
        double closestX = Math.max(rectX, Math.min(circleX, rectX + rectWidth));
        double closestY = Math.max(rectY, Math.min(circleY, rectY + rectHeight));
        double dx = circleX - closestX;
        double dy = circleY - closestY;
        return dx * dx + dy * dy < radius * radius;
    }

    private void ensureChunksAround(double worldX, double worldY) {
        int centerChunkX = Math.floorDiv((int) Math.floor(worldX / TILE_SIZE), CHUNK_SIZE);
        int centerChunkY = Math.floorDiv((int) Math.floor(worldY / TILE_SIZE), CHUNK_SIZE);
        for (int chunkY = centerChunkY - LOAD_RADIUS; chunkY <= centerChunkY + LOAD_RADIUS; chunkY++) {
            for (int chunkX = centerChunkX - LOAD_RADIUS; chunkX <= centerChunkX + LOAD_RADIUS; chunkX++) {
                getChunk(chunkX, chunkY);
            }
        }
    }

    private void unloadDistant(double worldX, double worldY) {
        int centerChunkX = Math.floorDiv((int) Math.floor(worldX / TILE_SIZE), CHUNK_SIZE);
        int centerChunkY = Math.floorDiv((int) Math.floor(worldY / TILE_SIZE), CHUNK_SIZE);
        chunks.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            int chunkX = (int) (key >> 32);
            int chunkY = (int) key;
            int distance = Math.max(Math.abs(chunkX - centerChunkX), Math.abs(chunkY - centerChunkY));
            return distance > UNLOAD_RADIUS;
        });
    }

    PlanetChunk getChunk(int chunkX, int chunkY) {
        long key = ((long) chunkX << 32) | (chunkY & 0xffffffffL);
        PlanetChunk chunk = chunks.get(key);
        if (chunk == null) {
            chunk = new PlanetChunk(chunkX, chunkY, seed);
            chunks.put(key, chunk);
        }
        return chunk;
    }
}