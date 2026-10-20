package com.spaceexplorer.interaction;

import com.spaceexplorer.entity.Player;

/**
 * Something in the world the player can press E on.
 *
 * <p>The HUD and {@link Game} only need these five answers to show a prompt and
 * react to a key press, so every new interactive object (repair parts, the
 * ventilation unit) is written against this one rule instead of adding its own
 * copy of the range and prompt logic.</p>
 *
 * <p>The lighting console keeps its existing handling in {@link Game} because
 * restoring power also drives the lighting system and the game state, which no
 * single prop should know about.</p>
 */
public interface Interactable {

    /** True when the player is standing close enough to notice this object. */
    boolean isInRange(Player player);

    /** True when pressing E right now would actually do something. */
    boolean isActionReady(Player player, Inventory inventory);

    /**
     * Extra information shown above the prompt, for example
     * {@code "Ventilation system damaged"}. Null when there is nothing to say.
     */
    String getStatusText(Player player, Inventory inventory);

    /**
     * What the player is being offered, for example
     * {@code "Press E to repair ventilation"} or {@code "Missing repair parts"}.
     * Null when the object has nothing to say right now.
     */
    String getActionPrompt(Player player, Inventory inventory);

    /** Handles the interaction. Returns true when something actually happened. */
    boolean interact(Player player, Inventory inventory);
}