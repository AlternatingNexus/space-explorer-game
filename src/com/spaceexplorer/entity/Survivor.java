package com.spaceexplorer.entity;

import com.spaceexplorer.dialogue.DialogueConversation;
import com.spaceexplorer.dialogue.DialogueLine;
import com.spaceexplorer.interaction.Interactable;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.world.Prop;

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
public final class Survivor extends Prop implements Interactable {

    private static final int FIGURE_WIDTH = 20;
    private static final int FIGURE_HEIGHT = 20;

    private final int variant;
    private final Color uniform;
    private final Color uniformTrim;
    private final Color pants;
    private final Color skin;
    private final Color hair;
    private final Color boots;

    private static final double INTERACT_RANGE = 96.0;

    private final String displayName;
    private final DialogueConversation conversation;

    private double time;

    public Survivor(int tileX, int tileY, int variant) {
        super(tileX, tileY, 1, 1, Layer.OBJECT);
        this.variant = variant;
        this.displayName = variant == 1 ? "CREW MEMBER 1" : "CREW MEMBER 2";
        this.conversation = createConversation(variant);
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

    /** Name shown by the HUD prompt and the dialogue box. */
    public String getDisplayName() {
        return displayName;
    }

    /** The conversation this survivor speaks when talked to. */
    public DialogueConversation getConversation() {
        return conversation;
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return true;
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return displayName;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        return "Press E to talk";
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        // Game owns the conversation so it can coordinate the dialogue manager.
        // The boolean only reports that the survivor is a valid interaction
        // target; the conversation itself is started by Game.
        return true;
    }

    private static DialogueConversation createConversation(int variant) {
        if (variant == 1) {
            return DialogueConversation.of(
                    new DialogueLine("CREW MEMBER 1",
                            "You're awake too, huh? I was starting to think we'd never get those pods open."),
                    new DialogueLine("CREW MEMBER 1",
                            "The ship's fuel reserves are almost depleted. We won't get far at this rate."),
                    new DialogueLine("CREW MEMBER 1",
                            "There's a planet just below us. We're holding position above its atmosphere, but we can't stay here forever."),
                    new DialogueLine("CREW MEMBER 1",
                            "There's a shuttle aboard. If we can reach the surface and find fuel, we might have a chance."),
                    new DialogueLine("CREW MEMBER 1",
                            "Before you leave, grab some food and water from the supply room. We need enough to survive the trip."));
        }
        return DialogueConversation.of(
                new DialogueLine("CREW MEMBER 2",
                        "I checked the ship's fuel readings. They're worse than I'd hoped."),
                new DialogueLine("CREW MEMBER 2",
                        "The planet beneath us is our best option. The main ship doesn't have enough fuel to get us anywhere else."),
                new DialogueLine("CREW MEMBER 2",
                        "There's a shuttle we can use, but don't leave without supplies."),
                new DialogueLine("CREW MEMBER 2",
                        "Find the food and water stores first. We have no idea how long you'll be down there."));
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