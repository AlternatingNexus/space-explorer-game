package com.spaceexplorer.enemy;

import com.spaceexplorer.entity.Player;
import com.spaceexplorer.planet.PlanetWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tracks active enemies for the current planetary run. Only one enemy is used
 * for this first implementation.
 */
public final class EnemyManager {

    private final long seed;
    private final List<Enemy> enemies = new ArrayList<>();

    public EnemyManager(long seed) {
        this.seed = seed;
    }

    public void spawnInitial(Player player) {
        if (!enemies.isEmpty()) {
            return;
        }
        Random random = new Random(seed);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 750.0 + random.nextDouble() * 350.0;
        double x = player.getCenterX() + Math.cos(angle) * distance;
        double y = player.getCenterY() + Math.sin(angle) * distance;
        enemies.add(new Enemy(x, y));
    }

    public void update(double dt, Player player, PlanetWorld world) {
        for (Enemy enemy : enemies) {
            if (enemy.isActive()) {
                enemy.update(dt, player);
            }
        }
    }

    public Enemy getNearestEnemy(Player player) {
        Enemy nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Enemy enemy : enemies) {
            if (!enemy.isActive()) {
                continue;
            }
            double dx = enemy.getCenterX() - player.getCenterX();
            double dy = enemy.getCenterY() - player.getCenterY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = enemy;
            }
        }
        return nearest;
    }

    public List<Enemy> getEnemies() {
        return Collections.unmodifiableList(enemies);
    }

    public boolean hasActiveEnemy() {
        for (Enemy enemy : enemies) {
            if (enemy.isActive()) {
                return true;
            }
        }
        return false;
    }
}