package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.world.Prop;

import java.awt.Color;
import java.awt.Graphics2D;

import java.util.Random;

/**
 * Scattered wreckage on the deck of the damaged compartment: torn panels,
 * twisted metal and loose rivets. Drawn in the floor pass so the player walks
 * over it, and it never blocks movement.
 */
public final class Debris extends Prop {

    private final long seed;

    public Debris(int tileX, int tileY, int tileW, int tileH) {
        super(tileX, tileY, tileW, tileH, Layer.FLOOR);
        this.seed = tileX * 73856093L ^ tileY * 19349663L;
    }

    @Override
    public void draw(Graphics2D g) {
        Random random = new Random(seed);

        g.setColor(new Color(0, 0, 0, 50));
        g.fillRect(x, y, width, height);

        for (int i = 0; i < 5; i++) {
            int px = x + 2 + random.nextInt(Math.max(2, width - 4));
            int py = y + 2 + random.nextInt(Math.max(2, height - 4));
            boolean horizontal = random.nextBoolean();

            g.setColor(random.nextBoolean() ? Palette.METAL_DARK : Palette.WALL_BODY);
            if (horizontal) {
                g.fillRect(px, py, 3 + random.nextInt(8), 2 + random.nextInt(2));
            } else {
                g.fillRect(px, py, 2 + random.nextInt(2), 3 + random.nextInt(6));
            }

            // small glowing cable strand
            if (random.nextInt(3) == 0) {
                g.setColor(Palette.SCREEN_TEXT_OFF);
                g.fillRect(px + 2, py + 1, 4, 1);
            }
        }
    }
}