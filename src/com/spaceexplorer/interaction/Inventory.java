package com.spaceexplorer.interaction;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Everything the player is carrying, in its simplest possible form.
 *
 * <p>The prototype has no inventory menu, so this only answers two questions the
 * quest needs: "do I still have the part I picked up?" and "have I found them
 * all yet?". Using a {@link Set} means a part can never be collected twice.</p>
 */
public class Inventory {

    private final Set<RepairPartType> parts = EnumSet.noneOf(RepairPartType.class);

    /** Adds a part. Adding the same part twice has no effect. */
    public void add(RepairPartType type) {
        parts.add(type);
    }

    public boolean has(RepairPartType type) {
        return parts.contains(type);
    }

    /** True when every required component is currently being carried. */
    public boolean hasAll(Collection<RepairPartType> types) {
        return parts.containsAll(types);
    }

    /** Consumes the parts, used when the vent is repaired. */
    public void removeAll(Collection<RepairPartType> types) {
        parts.removeAll(types);
    }

    /** How many repair parts the player is carrying. */
    public int count() {
        return parts.size();
    }

    public Set<RepairPartType> getParts() {
        return Collections.unmodifiableSet(parts);
    }
}