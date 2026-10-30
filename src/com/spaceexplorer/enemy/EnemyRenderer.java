package com.spaceexplorer.enemy;

import com.spaceexplorer.world.Camera;

import java.awt.Graphics2D;

/**
 * Draws active enemies in world coordinates. Kept separate from the HUD radar.
 */
public final class EnemyRenderer {

    private final EnemyManager manager;

    public EnemyRenderer(EnemyManager manager) {
        this.manager = manager;
    }

    public void render(Graphics2D screen, Camera camera) {
        if (manager == null) {
            return;
        }
        Graphics2D worldGraphics = (Graphics2D) screen.create();
        worldGraphics.translate(-camera.getX(), -camera.getY());
        for (Enemy enemy : manager.getEnemies()) {
            if (enemy.isActive()) {
                enemy.draw(worldGraphics);
            }
        }
        worldGraphics.dispose();
    }
}