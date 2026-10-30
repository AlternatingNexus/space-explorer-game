package com.spaceexplorer.planet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A fixed-size terrain chunk for Eryx9. The red desert terrain and scattered
 * rock formations are generated deterministically from chunk coordinates and
 * the world seed.
 */
final class PlanetChunk {

    static final int CHUNK_SIZE = PlanetWorld.CHUNK_SIZE;

    private final int[] colors = new int[CHUNK_SIZE * CHUNK_SIZE];

    final Rock[] rocks;

    PlanetChunk(int chunkX, int chunkY, long seed) {
        for (int localY = 0; localY < CHUNK_SIZE; localY++) {
            for (int localX = 0; localX < CHUNK_SIZE; localX++) {
                int globalX = chunkX * CHUNK_SIZE + localX;
                int globalY = chunkY * CHUNK_SIZE + localY;
                colors[localY * CHUNK_SIZE + localX] = generateTile(globalX, globalY, seed);
            }
        }
        rocks = generateRocks(chunkX, chunkY, seed);
    }

    int color(int localX, int localY) {
        return colors[localY * CHUNK_SIZE + localX];
    }

    void drawRocks(Graphics2D g, int chunkPixelX, int chunkPixelY) {
        for (Rock rock : rocks) {
            rock.draw(g, chunkPixelX, chunkPixelY);
        }
    }

    private static Rock[] generateRocks(int chunkX, int chunkY, long seed) {
        int chunkPixels = CHUNK_SIZE * PlanetWorld.TILE_SIZE;
        int chunkOriginX = chunkX * chunkPixels;
        int chunkOriginY = chunkY * chunkPixels;

        // Expanded area gives deterministic spacing checks against rocks whose
        // centers lie in neighboring chunks.
        int margin = 192;
        int areaMinX = chunkOriginX - margin;
        int areaMaxX = chunkOriginX + chunkPixels + margin;
        int areaMinY = chunkOriginY - margin;
        int areaMaxY = chunkOriginY + chunkPixels + margin;

        List<RockCandidate> candidates = new ArrayList<>();

        // Large boulders: rare, coarse global cells.
        addGlobalCandidates(candidates, seed, 384, 0.12, 2, areaMinX, areaMaxX, areaMinY, areaMaxY, chunkOriginX, chunkOriginY);
        // Medium rocks: occasional obstacles.
        addGlobalCandidates(candidates, seed, 192, 0.30, 1, areaMinX, areaMaxX, areaMinY, areaMaxY, chunkOriginX, chunkOriginY);
        // Small rocks: sparse scattered stones.
        addGlobalCandidates(candidates, seed, 96, 0.34, 0, areaMinX, areaMaxX, areaMinY, areaMaxY, chunkOriginX, chunkOriginY);

        // Deterministic ordering keeps filtering stable across chunk load order.
        candidates.sort((a, b) -> {
            if (a.worldY != b.worldY) return Integer.compare(a.worldY, b.worldY);
            return Integer.compare(a.worldX, b.worldX);
        });

        List<RockCandidate> acceptedCandidates = new ArrayList<>();
        for (RockCandidate candidate : candidates) {
            if (candidate.localX < 0 || candidate.localY < 0
                    || candidate.localX >= chunkPixels || candidate.localY >= chunkPixels) {
                continue;
            }
            if (chunkX == 0 && chunkY == 0) {
                int spawnX = PlanetWorld.TILE_SIZE * 8;
                int spawnY = PlanetWorld.TILE_SIZE * 8;
                int spawnExclusion = candidate.rock.collisionRadius > 18 ? 240 : 130;
                if (distanceSquared(candidate.worldX, candidate.worldY, spawnX, spawnY) < spawnExclusion * spawnExclusion) {
                    continue;
                }
            }
            if (!isFarEnough(candidate, acceptedCandidates)) {
                continue;
            }
            acceptedCandidates.add(candidate);
        }

        Rock[] accepted = new Rock[acceptedCandidates.size()];
        for (int i = 0; i < acceptedCandidates.size(); i++) {
            accepted[i] = acceptedCandidates.get(i).rock;
        }
        return accepted;
    }

    private static void addGlobalCandidates(List<RockCandidate> candidates, long seed, int cellSize, double probability,
                                          int category, int areaMinX, int areaMaxX, int areaMinY, int areaMaxY,
                                          int chunkOriginX, int chunkOriginY) {
        int minCellX = Math.floorDiv(areaMinX, cellSize) - 1;
        int maxCellX = Math.floorDiv(areaMaxX, cellSize) + 1;
        int minCellY = Math.floorDiv(areaMinY, cellSize) - 1;
        int maxCellY = Math.floorDiv(areaMaxY, cellSize) + 1;

        for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                Random cellRandom = new Random(mix(seed, cellX, cellY, 7000 + category));
                if (cellRandom.nextDouble() > probability) {
                    continue;
                }

                int centerX = cellX * cellSize + cellSize / 4 + cellRandom.nextInt(cellSize / 2);
                int centerY = cellY * cellSize + cellSize / 4 + cellRandom.nextInt(cellSize / 2);

                int localX = centerX - chunkOriginX;
                int localY = centerY - chunkOriginY;
                candidates.add(new RockCandidate(centerX, centerY, localX, localY,
                        new Rock(localX, localY, category, mix(seed, centerX, centerY, 7001 + category))));
            }
        }
    }

    private static boolean isFarEnough(RockCandidate candidate, List<RockCandidate> accepted) {
        for (RockCandidate existing : accepted) {
            int minimumDistance = minimumDistance(candidate.rock, existing.rock);
            int dx = candidate.worldX - existing.worldX;
            int dy = candidate.worldY - existing.worldY;
            if (dx * dx + dy * dy < minimumDistance * minimumDistance) {
                return false;
            }
        }
        return true;
    }

    private static int minimumDistance(Rock a, Rock b) {
        boolean aLarge = a.collisionRadius >= 18;
        boolean bLarge = b.collisionRadius >= 18;
        boolean aMedium = !aLarge && a.collisionRadius >= 10;
        boolean bMedium = !bLarge && b.collisionRadius >= 10;

        if (aLarge || bLarge) {
            return 96;
        }
        if (aMedium || bMedium) {
            return 42;
        }
        return 24;
    }

    private static void addRock(List<Rock> rocks, int chunkPixels, int chunkX, int chunkY,
                               Random random, long seed, int category) {
        int attempts = 8;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int localX = random.nextInt(chunkPixels - 24) + 12;
            int localY = random.nextInt(chunkPixels - 24) + 12;

            if (chunkX == 0 && chunkY == 0) {
                int worldX = chunkX * chunkPixels + localX;
                int worldY = chunkY * chunkPixels + localY;
                int spawnX = PlanetWorld.TILE_SIZE * 8;
                int spawnY = PlanetWorld.TILE_SIZE * 8;
                if (distanceSquared(worldX, worldY, spawnX, spawnY) < 120 * 120) {
                    continue;
                }
            }

            boolean tooClose = false;
            int minimumDistance = category == 0 ? 20 : 44;
            for (Rock existing : rocks) {
                if (distanceSquared(localX, localY, existing.localX, existing.localY)
                        < minimumDistance * minimumDistance) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }

            rocks.add(new Rock(localX, localY, category, mix(seed, chunkX * 1000 + localX, chunkY * 1000 + localY, category + 10)));
            return;
        }
    }

    private static int generateTile(int x, int y, long seed) {
        int regionX = Math.floorDiv(x, 16);
        int regionY = Math.floorDiv(y, 16);
        Random regionRandom = new Random(mix(seed, regionX, regionY, 1));
        double region = regionRandom.nextDouble();

        int base;
        if (region < 0.72) {
            base = 0x8A342B; // consistent rust-red desert
        } else if (region < 0.88) {
            base = 0x7A2C24; // slightly darker reddish-brown
        } else if (region < 0.96) {
            base = 0x933A30; // slightly lighter rust
        } else {
            base = 0x6F2820; // occasional darker patch
        }

        Random microRandom = new Random(mix(seed, x, y, 2));
        int delta = microRandom.nextInt(5) - 2;
        int red = clamp(((base >> 16) & 0xFF) + delta);
        int green = clamp(((base >> 8) & 0xFF) + delta / 2);
        int blue = clamp((base & 0xFF) + delta / 3);
        return (red << 16) | (green << 8) | blue;
    }

    private static long mix(long seed, int x, int y, int salt) {
        long hash = seed;
        hash ^= (long) x * 0x9E3779B97F4A7C15L;
        hash ^= (long) y * 0xC2B2AE3D27D4EB4FL;
        hash ^= (long) salt * 0x1505L;
        return hash;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int distanceSquared(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    static final class Rock {
        final int localX;
        final int localY;
        final int collisionRadius;
        final RockPart[] parts;

        Rock(int localX, int localY, int category, long seed) {
            this.localX = localX;
            this.localY = localY;
            RockBuild build = buildParts(localX, localY, category, seed);
            this.parts = build.parts;
            this.collisionRadius = build.collisionRadius;
        }

        void draw(Graphics2D g, int chunkPixelX, int chunkPixelY) {
            Graphics2D rockGraphics = (Graphics2D) g.create();
            rockGraphics.translate(chunkPixelX, chunkPixelY);
            for (RockPart part : parts) {
                rockGraphics.setColor(part.color);
                if (part.strokeWidth > 0) {
                    rockGraphics.setStroke(new java.awt.BasicStroke(part.strokeWidth));
                    rockGraphics.draw(part.shape);
                } else {
                    rockGraphics.fill(part.shape);
                }
            }
            rockGraphics.dispose();
        }

        private static RockBuild buildParts(int centerX, int centerY, int category, long seed) {
            Random random = new Random(seed);
            List<RockPart> parts = new ArrayList<>();

            int baseWidth;
            int baseHeight;
            if (category == 0) {
                baseWidth = 20 + random.nextInt(16);
                baseHeight = (int) (baseWidth * (0.72 + random.nextDouble() * 0.22));
            } else if (category == 1) {
                baseWidth = 40 + random.nextInt(26);
                baseHeight = (int) (baseWidth * (0.62 + random.nextDouble() * 0.24));
            } else {
                baseWidth = 70 + random.nextInt(41);
                baseHeight = (int) (baseWidth * (0.58 + random.nextDouble() * 0.26));
            }

            int collisionRadius = Math.max(6, (int) Math.round(baseWidth * 0.32));

            Color baseColor;
            Color mainColor;
            Color secondaryColor;
            Color highlightColor;
            Color shadowColor;

            if (category == 0) {
                baseColor = new Color(0x2A2A2A);
                mainColor = new Color(0x4A4A4A);
                secondaryColor = new Color(0x5A5A5A);
                highlightColor = new Color(0x7A7A7A);
                shadowColor = new Color(0x1E1E1E);
            } else if (category == 1) {
                baseColor = new Color(0x2F2F2F);
                mainColor = new Color(0x505050);
                secondaryColor = new Color(0x656565);
                highlightColor = new Color(0x8A8A8A);
                shadowColor = new Color(0x222222);
            } else {
                baseColor = new Color(0x262626);
                mainColor = new Color(0x404040);
                secondaryColor = new Color(0x555555);
                highlightColor = new Color(0x7A7A7A);
                shadowColor = new Color(0x1A1A1A);
            }

            // Mineral accent for some medium and large rocks.
            boolean mineral = category > 0 && random.nextDouble() < 0.35;
            Color mineralColor = mineral ? new Color(0x6B3A2A) : secondaryColor;

            // Base silhouette.
            parts.add(new RockPart(irregularPolygon(random, centerX, centerY,
                    baseWidth / 2.0 + 2, baseHeight / 2.0 + 2, 6 + random.nextInt(4)),
                    baseColor));

            // Connected stone masses.
            int massCount = category == 0 ? 1 : (1 + random.nextInt(category == 1 ? 2 : 3));
            for (int i = 0; i < massCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = baseWidth * (0.10 + random.nextDouble() * 0.18);
                double massWidth = baseWidth * (0.28 + random.nextDouble() * 0.20);
                double massHeight = baseHeight * (0.24 + random.nextDouble() * 0.18);
                parts.add(new RockPart(
                        new Ellipse2D.Double(
                                centerX + Math.cos(angle) * distance - massWidth / 2.0,
                                centerY + Math.sin(angle) * distance - massHeight / 2.0,
                                massWidth, massHeight),
                        vary(mainColor, random)));
            }

            // Main facets.
            int facetCount = 3 + random.nextInt(category == 2 ? 4 : 3);
            for (int i = 0; i < facetCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = baseWidth * (0.05 + random.nextDouble() * 0.22);
                double facetWidth = baseWidth * (0.22 + random.nextDouble() * 0.24);
                double facetHeight = baseHeight * (0.18 + random.nextDouble() * 0.20);
                Color color = random.nextBoolean() ? mainColor : secondaryColor;
                if (mineral && random.nextDouble() < 0.25) {
                    color = mineralColor;
                }
                parts.add(new RockPart(
                        irregularPolygon(random,
                                centerX + Math.cos(angle) * distance,
                                centerY + Math.sin(angle) * distance,
                                facetWidth / 2.0, facetHeight / 2.0,
                                3 + random.nextInt(3)),
                        vary(color, random)));
            }

            // Angular protrusions.
            int protrusionCount = 2 + random.nextInt(category == 0 ? 2 : 4);
            for (int i = 0; i < protrusionCount; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double length = baseWidth * (0.18 + random.nextDouble() * 0.22);
                double halfWidth = 2 + random.nextDouble() * 3.5;
                double tipX = centerX + Math.cos(angle) * length;
                double tipY = centerY + Math.sin(angle) * length;
                double leftX = centerX + Math.cos(angle + 1.1) * halfWidth;
                double leftY = centerY + Math.sin(angle + 1.1) * halfWidth;
                double rightX = centerX + Math.cos(angle - 1.1) * halfWidth;
                double rightY = centerY + Math.sin(angle - 1.1) * halfWidth;

                Path2D path = new Path2D.Double();
                path.moveTo(leftX, leftY);
                path.lineTo(tipX, tipY);
                path.lineTo(rightX, rightY);
                path.closePath();
                parts.add(new RockPart(path, vary(mainColor, random)));
            }

            // Shadow regions.
            int shadowCount = 1 + random.nextInt(category == 0 ? 2 : 3);
            for (int i = 0; i < shadowCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * baseWidth * 0.55;
                double offsetY = (random.nextDouble() - 0.5) * baseHeight * 0.55;
                double width = baseWidth * (0.22 + random.nextDouble() * 0.22);
                double height = baseHeight * (0.18 + random.nextDouble() * 0.18);
                parts.add(new RockPart(
                        new Ellipse2D.Double(centerX + offsetX - width / 2.0,
                                centerY + offsetY - height / 2.0, width, height),
                        shadowColor));
            }

            // Highlights.
            int highlightCount = 2 + random.nextInt(category == 0 ? 2 : 4);
            for (int i = 0; i < highlightCount; i++) {
                double offsetX = (random.nextDouble() - 0.5) * (baseWidth - 4);
                double offsetY = (random.nextDouble() - 0.5) * (baseHeight - 4);
                double size = 2 + random.nextInt(category == 2 ? 5 : 3);
                parts.add(new RockPart(
                        new Ellipse2D.Double(centerX + offsetX, centerY + offsetY, size, size),
                        vary(highlightColor, random)));
            }

            return new RockBuild(parts.toArray(new RockPart[0]), collisionRadius);
        }

        private static java.awt.Shape irregularPolygon(Random random, double centerX, double centerY,
                                                      double radiusX, double radiusY, int points) {
            Path2D path = new Path2D.Double();
            for (int i = 0; i < points; i++) {
                double baseAngle = (Math.PI * 2.0 * i) / points;
                double angle = baseAngle + (random.nextDouble() - 0.5) * 0.85;
                double rx = radiusX * (0.55 + 0.45 * random.nextDouble());
                double ry = radiusY * (0.55 + 0.45 * random.nextDouble());
                double x = centerX + Math.cos(angle) * rx;
                double y = centerY + Math.sin(angle) * ry;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            path.closePath();
            return path;
        }

        private static Color vary(Color color, Random random) {
            int red = clamp(color.getRed() + random.nextInt(19) - 9);
            int green = clamp(color.getGreen() + random.nextInt(19) - 9);
            int blue = clamp(color.getBlue() + random.nextInt(19) - 9);
            return new Color(red, green, blue);
        }
    }

    private static final class RockCandidate {
        final int worldX;
        final int worldY;
        final int localX;
        final int localY;
        final Rock rock;

        RockCandidate(int worldX, int worldY, int localX, int localY, Rock rock) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.localX = localX;
            this.localY = localY;
            this.rock = rock;
        }
    }

    private static final class RockBuild {
        final RockPart[] parts;
        final int collisionRadius;

        RockBuild(RockPart[] parts, int collisionRadius) {
            this.parts = parts;
            this.collisionRadius = collisionRadius;
        }
    }

    private static final class RockPart {
        final java.awt.Shape shape;
        final Color color;
        final float strokeWidth;

        RockPart(java.awt.Shape shape, Color color) {
            this(shape, color, 0f);
        }

        RockPart(java.awt.Shape shape, Color color, float strokeWidth) {
            this.shape = shape;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }
}