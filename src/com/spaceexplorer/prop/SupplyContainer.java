package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.core.StoryState;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.interaction.Interactable;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A one-time food or water pickup in the Supply Room.
 *
 * <p>The container itself only owns its presentation and interaction. The
 * collected flags live in the shared {@link StoryState}, so leaving the room and
 * coming back can never make a supply reappear.</p>
 */
public final class SupplyContainer extends Prop implements Interactable {

    public enum SupplyType {
        FOOD("FOOD SUPPLIES", "food supplies", "Food supplies collected.",
                new Color(0xFFC46B)),
        WATER("WATER SUPPLIES", "water supplies", "Water supplies collected.",
                new Color(0x7FD6FF));

        private final String label;
        private final String promptNoun;
        private final String collectedMessage;
        private final Color accent;

        SupplyType(String label, String promptNoun, String collectedMessage, Color accent) {
            this.label = label;
            this.promptNoun = promptNoun;
            this.collectedMessage = collectedMessage;
            this.accent = accent;
        }
    }

    private static final double INTERACT_RANGE = 100.0;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    private final SupplyType type;
    private final StoryState storyState;
    private final Light supplyLight;

    private double time;

    public SupplyContainer(SupplyType type, int tileX, int tileY, int tileW, int tileH,
                           StoryState storyState) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.type = type;
        this.storyState = storyState;
        this.supplyLight = new Light(getCenterX(), getCenterY(),
                78.0, 126.0,
                type.accent,
                type.accent,
                0.45);
    }

    public SupplyType getType() {
        return type;
    }

    public boolean isCollected() {
        return type == SupplyType.FOOD
                ? storyState.hasFoodSupplies()
                : storyState.hasWaterSupplies();
    }

    @Override
    public Light getLight() {
        return supplyLight;
    }

    /** Once empty, the player can walk through the open container. */
    @Override
    public boolean blocksMovement() {
        return !isCollected();
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        if (isCollected()) {
            return false;
        }
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !isCollected() && storyState.isFuelProblemLearned();
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        if (isCollected()) {
            return null;
        }
        return storyState.isFuelProblemLearned() ? type.label : "SUPPLIES LOCKED";
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        if (isCollected()) {
            return null;
        }
        if (!storyState.isFuelProblemLearned()) {
            return "Talk to the survivors first";
        }
        return "Press E to collect " + type.promptNoun;
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (!storyState.isFuelProblemLearned()) {
            player.say("Talk to the survivors before collecting supplies.", 4.0);
            return true;
        }
        boolean collected = type == SupplyType.FOOD
                ? storyState.collectFoodSupplies()
                : storyState.collectWaterSupplies();
        if (!collected) {
            return false;
        }
        supplyLight.setActive(false);
        player.say(type.collectedMessage, 3.5);
        return true;
    }

    /* ---------- per frame + drawing ---------- */

    @Override
    public void update(double dt) {
        time += dt;
        supplyLight.setActive(storyState.isSupplyRoomUnlocked() && !isCollected());
    }

    @Override
    public void draw(Graphics2D g) {
        if (!storyState.isSupplyRoomUnlocked()) {
            return;
        }
        if (type == SupplyType.FOOD) {
            drawFoodContainer(g);
        } else {
            drawWaterContainer(g);
        }
    }

    private void drawFoodContainer(Graphics2D g) {
        boolean collected = isCollected();

        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(x + 3, y + height - 6, width - 6, 9));

        if (!collected) {
            drawGlow(g, type.accent);
        }

        // crate body
        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(x + 2, y + 5, width - 4, height - 10, 5, 5));
        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(x + 4, y + 7, width - 8, height - 14, 3, 3));

        if (collected) {
            // open lid and a dark, empty interior
            g.setColor(new Color(0x141A24));
            g.fillRect(x + 7, y + 10, width - 14, height - 17);
            g.setColor(Palette.METAL_DARK);
            g.fill(new RoundRectangle2D.Double(x + 2, y + 2, width - 4, 5, 3, 3));
            g.setColor(Palette.METAL_LIGHT);
            g.fillRect(x + 5, y + 3, width - 10, 1);
        } else {
            // sealed lid with a food-service stripe
            g.setColor(Palette.METAL_LIGHT);
            g.fillRect(x + 6, y + 8, width - 12, 3);
            g.setColor(type.accent);
            g.fillRect(x + 6, y + 14, width - 12, 4);
            g.setColor(new Color(0x3B2A12));
            g.fillRect(x + 10, y + 15, 8, 2);
            g.fillRect(x + width - 20, y + 15, 8, 2);
        }

        // stencilled label and corner braces
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(x + 2, y + 5, width - 4, height - 10, 5, 5));
        g.setFont(LABEL_FONT);
        g.setColor(collected ? Palette.TEXT_DIM : type.accent);
        String label = collected ? "EMPTY" : "FOOD";
        int labelX = (int) (getCenterX() - g.getFontMetrics().stringWidth(label) / 2.0);
        g.drawString(label, labelX, y + height - 6);
    }

    private void drawWaterContainer(Graphics2D g) {
        boolean collected = isCollected();

        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(x + 4, y + height - 6, width - 8, 9));

        if (!collected) {
            drawGlow(g, type.accent);
        }

        // canister body
        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(x + 6, y + 7, width - 12, height - 13, 7, 7));
        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(x + 8, y + 9, width - 16, height - 17, 5, 5));

        // cap
        g.setColor(Palette.METAL_LIGHT);
        g.fill(new RoundRectangle2D.Double(x + 10, y + 3, width - 20, 6, 3, 3));
        g.setColor(Palette.WALL_EDGE);
        g.draw(new RoundRectangle2D.Double(x + 10, y + 3, width - 20, 6, 3, 3));

        // water-level window
        int windowY = y + 14;
        int windowHeight = height - 26;
        g.setColor(new Color(0x13202A));
        g.fill(new RoundRectangle2D.Double(x + 11, windowY, width - 22, windowHeight, 4, 4));
        if (!collected) {
            g.setColor(new Color(type.accent.getRed(), type.accent.getGreen(),
                    type.accent.getBlue(), 210));
            g.fill(new RoundRectangle2D.Double(x + 13, windowY + 3,
                    width - 26, windowHeight - 6, 3, 3));
            g.setColor(new Color(255, 255, 255, 120));
            g.fillRect(x + 15, windowY + 5, 2, Math.max(2, windowHeight - 12));
        }

        // handle rails
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 2, y + 15, 3, 20);
        g.fillRect(x + width - 5, y + 15, 3, 20);

        // stencilled label
        g.setFont(LABEL_FONT);
        g.setColor(collected ? Palette.TEXT_DIM : type.accent);
        String label = collected ? "EMPTY" : "WATER";
        int labelX = (int) (getCenterX() - g.getFontMetrics().stringWidth(label) / 2.0);
        g.drawString(label, labelX, y + height - 6);
    }

    private void drawGlow(Graphics2D g, Color accent) {
        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 2.5));
        float radius = (float) Math.max(width, height) * 0.6f;
        g.setPaint(new RadialGradientPaint(
                (float) getCenterX(), (float) getCenterY(), radius,
                new float[]{0f, 1f},
                new Color[]{withAlpha(accent, Math.round(38 + 32 * pulse)),
                        withAlpha(accent, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Ellipse2D.Double(getCenterX() - radius, getCenterY() - radius,
                radius * 2, radius * 2));
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
