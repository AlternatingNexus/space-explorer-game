package com.spaceexplorer.core;

/** Coarse progression of the prototype's scripted objectives. */
public enum GameState {

    /** Player has just woken up, movement is locked for a moment. */
    INTRO,
    /** Free roam, looking for the power control panel. */
    EXPLORING,
    /** Power has been restored, the ventilation repair is now the objective. */
    POWER_RESTORED,
    /** The ventilation unit has been repaired, the gas leak is next. */
    VENTILATION_REPAIRED,
    /** The gas leak has been sealed, all objectives complete. */
    GAS_LEAK_CONTAINED,
    /** The leaked gas ignited: the run is over until the player retries. */
    SHIP_DESTROYED
}