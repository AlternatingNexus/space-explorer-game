package com.spaceexplorer.core;

/**
 * Story and survival state for one playthrough.
 *
 * <p>This is deliberately separate from {@link GameState}: the original
 * progression enum still drives the ship-repair beats and the gas-leak
 * countdown, while this object records the optional, persistent information
 * introduced by the survivors. A fresh instance is created by
 * {@code Game.initRun()}, so RETRY starts the story cleanly.</p>
 */
public final class StoryState {

    /** Both survival meters begin full; depletion is a future feature. */
    public static final int FULL_SUPPLIES = 100;

    private boolean fuelProblemLearned;
    private boolean foodSuppliesCollected;
    private boolean waterSuppliesCollected;
    private boolean suppliesCompletionPending;

    private int foodPercent = FULL_SUPPLIES;
    private int waterPercent = FULL_SUPPLIES;

    public boolean isFuelProblemLearned() {
        return fuelProblemLearned;
    }

    /** Records that a survivor has explained the fuel situation. */
    public void learnFuelProblem() {
        fuelProblemLearned = true;
    }

    public boolean hasFoodSupplies() {
        return foodSuppliesCollected;
    }

    public boolean hasWaterSupplies() {
        return waterSuppliesCollected;
    }

    /**
     * Collects the food supplies once.
     *
     * @return true when this call changed the state, false when already collected
     */
    public boolean collectFoodSupplies() {
        if (foodSuppliesCollected) {
            return false;
        }
        foodSuppliesCollected = true;
        markCompletionIfReady();
        return true;
    }

    /**
     * Collects the water supplies once.
     *
     * @return true when this call changed the state, false when already collected
     */
    public boolean collectWaterSupplies() {
        if (waterSuppliesCollected) {
            return false;
        }
        waterSuppliesCollected = true;
        markCompletionIfReady();
        return true;
    }

    public boolean areSuppliesComplete() {
        return foodSuppliesCollected && waterSuppliesCollected;
    }

    /**
     * One-shot flag used by the HUD/game loop to show the completion message
     * after the second container is opened.
     */
    public boolean consumeSuppliesCompletionNotice() {
        boolean pending = suppliesCompletionPending;
        suppliesCompletionPending = false;
        return pending;
    }

    public int getFoodPercent() {
        return foodPercent;
    }

    public int getWaterPercent() {
        return waterPercent;
    }

    private void markCompletionIfReady() {
        if (foodSuppliesCollected && waterSuppliesCollected) {
            suppliesCompletionPending = true;
        }
    }
}
