package com.spaceexplorer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * One of the two crew members the player wakes from cryosleep.
 *
 * <p>Each survivor is a small top-down human drawn in the same style as the
 * {@link Player}: head with hair, a coverall jacket with a collar, arms, legs
 * and boots. The two variants share that geometry but use different uniforms,
 * hair and skin tones so they are easy to tell apart.</p>
 *
 * <p>Survivors are kept out of the world until their cryopod is opened, then
 * added next to the pod. Once released they stay there, so leaving and
 * returning always shows the same awakened crew members.</p>
 */
public final class Survivor extends Prop {

    private static final int FIGURE_WIDTH = 20;
    private static final int FIGURE_HEIGHT = 20;

    private final int variant;
    private final Color uniform;
    private final Color uniformTrim;
    private final Color pants;
    private final Color skin;
    private final Color hair;
    private final Color boots;

    private double time;

    public Survivor(int tileX, int tileY, int variant) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.variant = variant;
        if (variant == 1) {
            // engineer: teal coveralls with amber trim, dark hair
            this.uniform = new Color(0x2C7A8C);
            this.uniformTrim = new Color(0xFFC46B);
            this.pants = new Color(0x1E5461);
            this.skin = new Color(0xE8B98A);
            this.hair = new Color(0x3A2A22);
            this.boots = new Color(0x2E2118);
        } else {
            // medic: rust coveralls with white trim, lighter hair
            this.uniform = new Color(0xB0552E);
            this.uniformTrim = new Color(0xE8EEF9);
            this.pants = new Color(0x7A3A1E);
            this.skin = new Color(0xF2C79B);
            this.hair = new Color(0x8A5A2B);
            this.boots = new Color(0x4A2410);
        }
    }

    /** Which design this survivor uses, 1 or 2, so the two are distinguishable. */
    public int getVariant() {
        return variant;
    }

    @Override
    public boolean blocksMovement() {
        return false;
    }

    @Override
    public void update(double dt) {
        time += dt;
    }

    @Override
    public void draw(Graphics2D g) {
        // a released survivor breathes gently in place
        int bob = (int) Math.round(Math.sin(time * 2.2) * 1.2);
        drawFigure(g, x + 6, y + 6 + bob);
    }

    /** Draws the 20x20 crew figure with its top left corner at (fx, fy). */
    private void drawFigure(Graphics2D g, int fx, int fy) {
        int cx = fx + FIGURE_WIDTH / 2;

        // contact shadow
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(new Ellipse2D.Double(fx - 2, fy + FIGURE_HEIGHT - 7, FIGURE_WIDTH + 4, 10));

        // arms at the sides
        g.setColor(uniform);
        g.fill(new RoundRectangle2D.Double(fx + 0.5, fy + 7, 3, 8, 2, 2));
        g.fill(new RoundRectangle2D.Double(fx + FIGURE_WIDTH - 3.5, fy + 7, 3, 8, 2, 2));
        g.setColor(skin);
        g.fillRect(fx + 1, fy + 14, 2, 2);
        g.fillRect(fx + 17, fy + 14, 2, 2);

        // coverall jacket
        g.setColor(uniform);
        g.fill(new RoundRectangle2D.Double(fx + 2, fy + 5, FIGURE_WIDTH - 4, 11, 5, 5));
        g.setColor(pants);
        g.draw(new RoundRectangle2D.Double(fx + 2, fy + 5, FIGURE_WIDTH - 4, 11, 5, 5));

        // collar, trim stripe and belt
        g.setColor(uniformTrim);
        g.fillRect(fx + 4, fy + 6, 4, 2);
        g.fillRect(fx + 12, fy + 6, 4, 2);
        g.fillRect(fx + 6, fy + 10, 8, 2);
        g.setColor(pants);
        g.fillRect(fx + 3, fy + 12, FIGURE_WIDTH - 6, 2);

        // legs and boots
        g.setColor(pants);
        g.fill(new RoundRectangle2D.Double(fx + 4, fy + 14, 4, 5, 2, 2));
        g.fill(new RoundRectangle2D.Double(fx + 12, fy + 14, 4, 5, 2, 2));
        g.setColor(boots);
        g.fillRect(fx + 4, fy + 18, 4, 2);
        g.fillRect(fx + 12, fy + 18, 4, 2);

        // head with hair visible above the collar
        g.setColor(skin);
        g.fill(new Ellipse2D.Double(cx - 5, fy, 10, 9));
        g.setColor(hair);
        g.fill(new Arc2D.Double(cx - 5, fy - 1, 10, 8, 180, 180, Arc2D.CHORD));
    }
}