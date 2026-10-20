package com.spaceexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The cryosleep bay and everything that happens in it.
 *
 * <p>Spawning the bay runs the whole flow from the game's point of view: three
 * cryopods along the back wall, a {@link CryoPowerPanel} by the door that the
 * player must switch on, and a slot beside each pod where a woken crew member
 * will stand. The first pod the player opens is always the empty one; the other
 * two release the {@link Survivor}s, and the survivors are added straight to the
 * world so they persist after the player leaves.</p>
 */
public final class CryoRoom {

    public static final String MESSAGE_DISCOVERED =
            "The air here is cold. Cryopods line the back wall.";
    public static final String MESSAGE_FINAL = "Two survivors have been awakened.";

    private static final int POD_COUNT = 3;

    private final World world;
    private final Room room;
    private final CryoPowerPanel panel;
    private final List<CryoPod> pods = new ArrayList<>();
    private boolean discovered;
    private boolean finalMessageArmed;
    private double finalMessageTimer;

    public CryoRoom(World world, Room room) {
        this.world = world;
        this.room = room;

        int rx = room.getTileX();
        int ry = room.getTileY();
        int bottom = room.getBottomTile();

        int panelX = rx + 1;
        int panelY = bottom - 2;
        this.panel = new CryoPowerPanel(panelX, panelY, 1, 2);
        world.addProp(panel);

        int[] podXs = {rx + 2, rx + 6, rx + 10};
        for (int podX : podXs) {
            CryoPod pod = new CryoPod(podX, ry + 1,
                    podX + 1, ry + 3,
                    panel, this);
            pods.add(pod);
            world.addProp(pod);
        }
        world.setCryoRoom(this);
    }

    /* ---------- accessors ---------- */

    public Room getRoom() {
        return room;
    }

    public boolean isDiscovered() {
        return discovered;
    }

    public boolean isPowered() {
        return panel.isPowered();
    }

    public CryoPowerPanel getPowerPanel() {
        return panel;
    }

    public int getPodCount() {
        return POD_COUNT;
    }

    public int getOpenedCount() {
        int opened = 0;
        for (CryoPod pod : pods) {
            if (pod.isOpened()) {
                opened++;
            }
        }
        return opened;
    }

    public List<CryoPod> getPods() {
        return Collections.unmodifiableList(pods);
    }

    /* ---------- per frame ---------- */

    /** Run after {@link World#update(double)} so pod state from this frame is seen. */
    public void update(double dt, Player player) {
        if (!discovered && room.containsTile(
                (int) (player.getX() / World.TILE_SIZE),
                (int) (player.getY() / World.TILE_SIZE))) {
            discovered = true;
            player.say(MESSAGE_DISCOVERED, 4.5);
        }

        for (CryoPod pod : pods) {
            if (pod.consumeNewlyOpened()) {
                int sequence = getOpenedCount();
                if (sequence == 1) {
                    pod.completeOpening(true);
                    player.say(CryoPod.MESSAGE_EMPTY, 4.0);
                } else {
                    releaseSurvivor(sequence, pod, player);
                }
            }
        }

        if (getOpenedCount() == POD_COUNT) {
            if (!finalMessageArmed) {
                finalMessageArmed = true;
                finalMessageTimer = 1.6;
            } else if (finalMessageTimer > 0.0) {
                finalMessageTimer -= dt;
                if (finalMessageTimer <= 0.0) {
                    player.say(MESSAGE_FINAL, 4.5);
                }
            }
        }
    }

    private void releaseSurvivor(int sequence, CryoPod pod, Player player) {
        int variant = sequence - 1;
        world.addProp(new Survivor(pod.getSurvivorTileX(), pod.getSurvivorTileY(), variant));
        pod.completeOpening(false);
        player.say(CryoPod.MESSAGE_SURVIVOR, 4.0);
    }
}