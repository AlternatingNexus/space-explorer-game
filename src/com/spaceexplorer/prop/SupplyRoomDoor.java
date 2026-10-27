package com.spaceexplorer.prop;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.core.StoryState;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.interaction.Interactable;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The locked threshold to the Supply Room.
 *
 * <p>It is a real obstacle while locked and becomes passable once the player
 * has received the key from a survivor and used it here. All state lives in
 * {@link StoryState}, so room transitions and retries stay consistent.</p>
 */
public final class SupplyRoomDoor extends Prop implements Interactable {

    private static final double INTERACT_RANGE = 96.0;
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    private final StoryState storyState;

    public SupplyRoomDoor(int tileX, int tileY, int tileW, int tileH, StoryState storyState) {
        super(tileX, tileY, tileW, tileH, Layer.OBJECT);
        this.storyState = storyState;
    }

    @Override
    public boolean blocksMovement() {
        return !storyState.isSupplyRoomUnlocked();
    }

    @Override
    public boolean isInRange(Player player) {
        double dx = player.getCenterX() - getCenterX();
        double dy = player.getCenterY() - getCenterY();
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !storyState.isSupplyRoomUnlocked();
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return storyState.isSupplyRoomUnlocked() ? null : "SUPPLY ROOM LOCKED";
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        if (storyState.isSupplyRoomUnlocked()) {
            return null;
        }
        return storyState.hasSupplyRoomKey()
                ? "Press E to unlock supply room"
                : "Press E to check door";
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (storyState.isSupplyRoomUnlocked()) {
            player.say("Supply Room already unlocked.", 3.0);
            return true;
        }
        if (!storyState.hasSupplyRoomKey()) {
            player.say("Supply Room locked. Find a key.", 4.0);
            return true;
        }
        storyState.unlockSupplyRoom();
        player.say("Supply Room unlocked.", 4.0);
        return true;
    }

    @Override
    public void draw(Graphics2D g) {
        Color frame = storyState.isSupplyRoomUnlocked()
                ? Palette.ACCENT_BLUE
                : Palette.LED_EMERGENCY;

        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 2, y + 2, width - 4, height - 4);
        g.setColor(Palette.METAL);
        g.fillRect(x + 5, y + 5, width - 10, height - 10);

        g.setColor(frame);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x + 2, y + 2, width - 4, height - 4, 6, 6));

        // lock plate
        int plateX = x + width / 2 - 9;
        int plateY = y + height / 2 - 10;
        g.setColor(Palette.WALL_EDGE);
        g.fill(new RoundRectangle2D.Double(plateX, plateY, 18, 20, 4, 4));
        g.setColor(frame);
        g.draw(new RoundRectangle2D.Double(plateX, plateY, 18, 20, 4, 4));

        g.setFont(LABEL_FONT);
        g.setColor(Palette.TEXT_PRIMARY);
        String label = storyState.isSupplyRoomUnlocked() ? "OPEN" : "LOCKED";
        int textX = (int) (getCenterX() - g.getFontMetrics().stringWidth(label) / 2.0);
        g.drawString(label, textX, plateY + 13);
    }
}