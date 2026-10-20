package com.spaceexplorer;

/**
 * An {@link Interactable} environmental hazard: something the player is meant
 * to notice and then neutralise with carried parts.
 *
 * <p>Because hazards need to react on their own (notice the player, run their
 * repair animation), {@link Game} drives {@link #updateHazard(double, Player)}
 * every frame instead of waiting for an E press.</p>
 */
public interface Hazard extends Interactable {

    /** Per frame hook: noticing the player, animating, completing a repair. */
    void updateHazard(double dt, Player player);

    /** True once the hazard has been neutralised for good. */
    boolean isContained();
}