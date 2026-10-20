package com.spaceexplorer.interaction;

/**
 * The components the player has to find and carry around the ship.
 *
 * <p>An enum keeps the "which parts are still missing" logic honest: the vent,
 * the gas leak, the inventory and the parts in the world all agree on the same
 * set of values, and a typo becomes a compile error instead of a silent bug.</p>
 *
 * <p>The first three belong to the ventilation repair, the last two are the
 * makeshift emergency repair for the gas leak.</p>
 */
public enum RepairPartType {

    FAN_BLADE("Fan Blade"),
    POWER_CELL("Power Cell"),
    REPAIR_PANEL("Repair Panel"),
    PIPE_CLAMP("Emergency Pipe Clamp"),
    SEALING_COMPOUND("Sealing Compound");

    private final String displayName;

    RepairPartType(String displayName) {
        this.displayName = displayName;
    }

    /** Name used in prompts and messages, e.g. {@code "Press E to collect Fan Blade"}. */
    public String getDisplayName() {
        return displayName;
    }
}