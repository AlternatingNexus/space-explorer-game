package com.spaceexplorer.world;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.core.StoryState;
import com.spaceexplorer.cryo.CryoLight;
import com.spaceexplorer.cryo.CryoPowerPanel;
import com.spaceexplorer.cryo.CryoRoom;
import com.spaceexplorer.hazard.GasLeak;
import com.spaceexplorer.interaction.ControlPanel;
import com.spaceexplorer.interaction.RepairPart;
import com.spaceexplorer.interaction.RepairPartType;
import com.spaceexplorer.interaction.Vent;
import com.spaceexplorer.prop.Cabinet;
import com.spaceexplorer.prop.ComputerTerminal;
import com.spaceexplorer.prop.Crate;
import com.spaceexplorer.prop.Debris;
import com.spaceexplorer.prop.FloorGrate;
import com.spaceexplorer.prop.FloorMarking;
import com.spaceexplorer.prop.Generator;
import com.spaceexplorer.prop.Pipe;
import com.spaceexplorer.prop.ScorchMark;
import com.spaceexplorer.prop.Shelf;
import com.spaceexplorer.prop.Shuttle;
import com.spaceexplorer.prop.Stock;
import com.spaceexplorer.prop.SupplyContainer;
import com.spaceexplorer.prop.SupplyStation;
import com.spaceexplorer.prop.WallLabel;
import com.spaceexplorer.prop.WallLamp;
import com.spaceexplorer.prop.WarningBeacon;
import com.spaceexplorer.prop.WarningSign;

/**
 * Builds the prototype ship: a crew quarters compartment the player wakes up
 * in, a corridor, a power compartment holding the lighting console, and a
 * lower deck with the gas leak bay between a maintenance deck and a storage
 * deck.
 *
 * <p>All coordinates are in tiles. Rooms are declared as their floor
 * rectangles, so anything declared next to each other is automatically
 * connected; the doorway tiles are only there to be drawn as airlocks.</p>
 *
 * <pre>
 *   tiles 0 ................................ 34
 *          0  ###################################
 *          1  #  CREW QUARTERS        #  POWER CONTROL  #
 *          ...
 *          7  #       ============ AIRLOCK ======       #   corridor + doors
 *          8  #       ===========================       #
 *          9  ###################################
 *          ...
 *         15  #  ..  #  AIRLOCK down to the lower deck
 *         16  #  ..  #
 *         17  #  GAS LEAK BAY          #  MAINTENANCE BAY   #
 *          ...
 *         28  #                        #                  #
 *         29  #       AIRLOCK v                        #   storage duct
 *         30  #  STORAGE  ..........          #
 *         37  ###################################
 * </pre>
 */
public final class PrototypeLevel {

    public static final int WORLD_WIDTH_IN_TILES = 58;
    public static final int WORLD_HEIGHT_IN_TILES = 41;

    public static final int PLAYER_START_TILE_X = 7;
    public static final int PLAYER_START_TILE_Y = 7;

    private PrototypeLevel() {
        // static factory only
    }

    /** Builds a level with a fresh story state, useful for isolated tests. */
    public static World build() {
        return build(new StoryState());
    }

    public static World build(StoryState storyState) {
        ControlPanel controlPanel = new ControlPanel(29, 0, 2, 2);
        World world = new World(WORLD_WIDTH_IN_TILES, WORLD_HEIGHT_IN_TILES, controlPanel);

        /* Rooms: floor rectangles, walls are generated around them later. */
        world.addRoom(new Room("Crew Quarters", 2, 1, 12, 14));
        world.addRoom(new Room("Main Corridor", 14, 7, 9, 2));
        world.addRoom(new Room("Power Control", 23, 1, 14, 14));

        // lower deck, reached through a service duct under the crew quarters
        world.addRoom(new Room("Service Duct", 7, 15, 2, 2));
        world.addRoom(new Room("Gas Leak Bay", 6, 17, 14, 12));
        world.addRoom(new Room("Maintenance", 21, 17, 11, 10));
        world.addRoom(new Room("Maintenance Duct", 20, 21, 1, 2));
        world.addRoom(new Room("Storage Duct", 12, 29, 2, 1));
        world.addRoom(new Room("Storage", 6, 30, 14, 8));

        // the cryosleep bay, reached through an airlock off the storage deck
        world.addRoom(new Room("Cryo Airlock", 20, 36, 2, 2));
        Room cryoSleep = new Room("Cryosleep Bay", 22, 30, 13, 8);
        world.addRoom(cryoSleep);

        // side compartments: supplies off the power deck, hangar off the cryo deck
        world.addRoom(new Room("Supply Airlock", 37, 7, 1, 2));
        Room supplyRoom = new Room("Supply Room", 38, 2, 11, 11);
        world.addRoom(supplyRoom);
        world.addRoom(new Room("Hangar Airlock", 35, 34, 3, 2));
        Room shuttleBay = new Room("Shuttle Bay", 38, 27, 15, 13);
        world.addRoom(shuttleBay);

        /* Airlock thresholds between the compartments. */
        world.setDoor(14, 7);
        world.setDoor(14, 8);
        world.setDoor(22, 7);
        world.setDoor(22, 8);
        world.setDoor(7, 15);
        world.setDoor(8, 15);
        world.setDoor(7, 17);
        world.setDoor(8, 17);
        world.setDoor(20, 21);
        world.setDoor(20, 22);
        world.setDoor(12, 29);
        world.setDoor(13, 29);
        world.setDoor(19, 36);
        world.setDoor(20, 36);
        world.setDoor(21, 36);
        world.setDoor(22, 36);

        // supply-room airlock: a short side passage east of the power deck
        world.setDoor(36, 7);
        world.setDoor(36, 8);
        world.setDoor(37, 7);
        world.setDoor(37, 8);
        world.setDoor(38, 7);
        world.setDoor(38, 8);

        // hangar airlock: a short side passage east of the cryo deck
        world.setDoor(34, 34);
        world.setDoor(34, 35);
        world.setDoor(35, 34);
        world.setDoor(35, 35);
        world.setDoor(36, 34);
        world.setDoor(36, 35);
        world.setDoor(37, 34);
        world.setDoor(37, 35);
        world.setDoor(38, 34);
        world.setDoor(38, 35);

        world.addProp(controlPanel);
        decorateCrewQuarters(world);
        decorateCorridor(world);
        decoratePowerRoom(world);
        decorateGasBay(world);
        decorateMaintenance(world);
        decorateStorage(world);

        CryoRoom cryoRoom = new CryoRoom(world, cryoSleep);
        decorateCryoRoom(world, cryoRoom.getPowerPanel());
        decorateSupplyRoom(world, storyState);
        decorateShuttleBay(world);

        world.buildWalls();
        return world;
    }

    private static void decorateCrewQuarters(World world) {
        world.addProp(new ComputerTerminal(2, 0, 1, 1));
        world.addProp(new Pipe(6, 0, 5, 1, true));
        world.addProp(new WallLamp(11, 0, 130, 240));

        world.addProp(new Crate(3, 4, 1, 1));
        world.addProp(new Crate(4, 4, 1, 1));
        world.addProp(new Crate(11, 12, 2, 1));

        world.addProp(new FloorGrate(6, 12, 2, 1));
        world.addProp(new ScorchMark(8, 6, 2, 2));

        // second objective: the ventilation unit the player wakes up next to
        world.addProp(new Vent(2, 10, 2, 2));
        // one component waits in the crew quarters, across the room from the vent
        world.addProp(new RepairPart(RepairPartType.REPAIR_PANEL, 11, 3));
    }

    private static void decorateCorridor(World world) {
        world.addProp(new Pipe(15, 6, 6, 1, true));
        world.addProp(new WallLamp(18, 6, 110, 200));
        world.addProp(new FloorGrate(16, 7, 2, 1));
        world.addProp(new ScorchMark(20, 7, 2, 1));

        world.addProp(new RepairPart(RepairPartType.FAN_BLADE, 18, 8));
    }

    private static void decoratePowerRoom(World world) {
        world.addProp(new WallLamp(25, 0, 130, 250));
        world.addProp(new WallLamp(33, 0, 130, 250));
        world.addProp(new ComputerTerminal(35, 0, 1, 1));
        world.addProp(new Pipe(23, 15, 6, 1, true));

        world.addProp(new Generator(31, 9, 3, 3));
        world.addProp(new Crate(34, 5, 1, 1));
        world.addProp(new Crate(35, 6, 1, 1));

        world.addProp(new FloorGrate(25, 12, 3, 1));
        world.addProp(new ScorchMark(27, 6, 2, 2));

        world.addProp(new RepairPart(RepairPartType.POWER_CELL, 25, 4));
    }

    /** The third room: a ruptured coolant line, warning fixtures and wreckage. */
    private static void decorateGasBay(World world) {
        // north bulkhead: warning lights, signs, damaged pipe runs
        world.addProp(new Pipe(6, 16, 2, 1, true));
        world.addProp(new WallLamp(8, 16, 130, 240));
        world.addProp(new WarningBeacon(10, 16, 120, 180));
        world.addProp(new WarningSign(12, 16));
        world.addProp(new Pipe(13, 16, 2, 1, true));
        world.addProp(new WarningBeacon(16, 16, 120, 180));
        world.addProp(new WallLamp(17, 16, 130, 240));
        world.addProp(new Pipe(18, 16, 2, 1, true));

        // the hazard itself and the emergency gear dropped nearby
        world.addProp(new GasLeak(12, 20));
        world.addProp(new Crate(10, 18, 1, 1));
        world.addProp(new Crate(11, 18, 1, 1));
        world.addProp(new Pipe(19, 18, 1, 3, false));

        // damaged machinery, scattered wreckage and scorch marks
        world.addProp(new Generator(16, 25, 3, 3));
        world.addProp(new Crate(7, 26, 1, 1));
        world.addProp(new Crate(8, 26, 1, 1));
        world.addProp(new Debris(15, 18, 2, 1));
        world.addProp(new Debris(6, 22, 1, 2));
        world.addProp(new Debris(13, 24, 2, 1));
        world.addProp(new ScorchMark(10, 22, 2, 2));
        world.addProp(new FloorGrate(9, 27, 3, 1));
    }

    /** A workshop deck: benches, tool cabinets and the pipe clamp part. */
    private static void decorateMaintenance(World world) {
        world.addProp(new Pipe(21, 16, 2, 1, true));
        world.addProp(new WallLamp(24, 16, 130, 250));
        world.addProp(new Pipe(26, 16, 3, 1, true));
        world.addProp(new WallLamp(30, 16, 130, 250));

        world.addProp(new Pipe(31, 18, 1, 3, false));
        world.addProp(new Shelf(22, 18, 2, 1, Stock.TOOLS));
        world.addProp(new Shelf(27, 18, 3, 1, Stock.TOOLS));
        world.addProp(new Cabinet(25, 19, 1, 1, Stock.TOOLS));
        world.addProp(new Cabinet(25, 21, 1, 1, Stock.TOOLS));

        // the emergency pipe clamp waits between the two tool cabinets
        world.addProp(new RepairPart(RepairPartType.PIPE_CLAMP, 25, 20));

        world.addProp(new Crate(23, 24, 1, 1));
        world.addProp(new Crate(24, 24, 1, 1));
        world.addProp(new Generator(28, 24, 3, 3));
        world.addProp(new FloorGrate(22, 25, 3, 1));
        world.addProp(new Debris(21, 23, 1, 1));
    }

    /** Chemical stores: full shelves, sealed lockers and the compound canister. */
    private static void decorateStorage(World world) {
        world.addProp(new Pipe(6, 29, 2, 1, true));
        world.addProp(new WallLamp(8, 29, 130, 240));
        world.addProp(new Pipe(9, 29, 3, 1, true));
        world.addProp(new WallLamp(16, 29, 130, 240));
        world.addProp(new Pipe(17, 29, 2, 1, true));

        world.addProp(new Shelf(9, 30, 3, 1, Stock.SUPPLIES));
        world.addProp(new Shelf(15, 30, 3, 1, Stock.SUPPLIES));
        world.addProp(new Cabinet(6, 30, 1, 2, Stock.SUPPLIES));
        world.addProp(new Cabinet(18, 30, 1, 2, Stock.SUPPLIES));

        world.addProp(new Crate(7, 33, 1, 1));
        world.addProp(new Crate(8, 33, 1, 1));
        world.addProp(new Crate(9, 33, 1, 1));
        world.addProp(new Crate(16, 33, 2, 1));
        world.addProp(new Crate(18, 33, 1, 1));

        // the sealing compound waits in the open at the far end of the bay
        world.addProp(new RepairPart(RepairPartType.SEALING_COMPOUND, 11, 35));

        world.addProp(new Debris(6, 36, 2, 1));
        world.addProp(new Debris(18, 36, 1, 1));
        world.addProp(new FloorGrate(10, 37, 4, 1));
    }

    /** A cold deck: three cryopods, a local power console and medical stores. */
    private static void decorateCryoRoom(World world, CryoPowerPanel panel) {
        // ceiling lamps are fed by the cryo grid, so they stay dark until it is on
        world.addProp(new CryoLight(24, 30, panel));
        world.addProp(new CryoLight(28, 30, panel));
        world.addProp(new CryoLight(32, 30, panel));

        // cryo-grid conduit: a drop beside the power console and along the far wall
        world.addProp(new Pipe(22, 31, 1, 3, false));
        world.addProp(new Pipe(34, 31, 1, 3, false));

        // bay monitoring terminals in the gaps between the pods
        world.addProp(new ComputerTerminal(23, 31, 1, 1));
        world.addProp(new ComputerTerminal(26, 31, 1, 1));
        world.addProp(new ComputerTerminal(30, 31, 1, 1));

        world.addProp(new Cabinet(22, 30, 1, 1, Stock.SUPPLIES));
        world.addProp(new Crate(23, 32, 1, 1));
        world.addProp(new Cabinet(34, 32, 1, 1, Stock.SUPPLIES));
        world.addProp(new FloorGrate(26, 35, 3, 1));
    }

    /** The food-and-water stores added for the next stage of the journey. */
    private static void decorateSupplyRoom(World world, StoryState storyState) {
        // lamps and compartment marking on the north bulkhead
        world.addProp(new WallLamp(40, 1, 130, 250));
        world.addProp(new WallLamp(47, 1, 130, 250));
        world.addProp(new WallLabel(42, 1, 4, 1, "SUPPLY ROOM", Palette.ACCENT_BLUE));
        world.addProp(new Pipe(38, 13, 5, 1, true));
        world.addProp(new Pipe(45, 13, 4, 1, true));

        // storage furniture: shelves above, lockers at the edges, and a small
        // manifest station between the racks
        world.addProp(new Shelf(40, 2, 3, 1, Stock.SUPPLIES));
        world.addProp(new Shelf(45, 2, 2, 1, Stock.SUPPLIES));
        world.addProp(new SupplyStation(43, 3, 2, 1));
        world.addProp(new Cabinet(38, 2, 1, 2, Stock.SUPPLIES));
        world.addProp(new Cabinet(48, 10, 1, 2, Stock.SUPPLIES));

        // the two independently collectable objectives
        world.addProp(new SupplyContainer(SupplyContainer.SupplyType.FOOD,
                40, 5, 2, 1, storyState));
        world.addProp(new SupplyContainer(SupplyContainer.SupplyType.WATER,
                46, 8, 1, 2, storyState));

        world.addProp(new Crate(39, 10, 1, 1));
        world.addProp(new Crate(40, 10, 1, 1));
        world.addProp(new Crate(44, 10, 1, 1));
        world.addProp(new FloorMarking(42, 7, 4, 2, "SUPPLIES", Palette.ACCENT_AMBER));
        world.addProp(new FloorGrate(42, 12, 3, 1));
    }

    /** The hangar and shuttle, deliberately scenery-only for this update. */
    private static void decorateShuttleBay(World world) {
        world.addProp(new WallLamp(40, 26, 140, 260));
        world.addProp(new WallLamp(49, 26, 140, 260));
        world.addProp(new WallLabel(43, 26, 5, 1, "SHUTTLE BAY", Palette.ACCENT_BLUE));
        world.addProp(new Pipe(38, 26, 2, 1, true));
        world.addProp(new Pipe(50, 26, 2, 1, true));

        // maintenance equipment around the bay
        world.addProp(new Shelf(39, 27, 2, 1, Stock.TOOLS));
        world.addProp(new ComputerTerminal(39, 29, 1, 1));
        world.addProp(new Cabinet(50, 28, 1, 2, Stock.TOOLS));
        world.addProp(new Generator(50, 37, 2, 2));
        world.addProp(new Crate(39, 38, 1, 1));
        world.addProp(new Crate(40, 38, 1, 1));
        world.addProp(new WarningSign(49, 27));

        // the shuttle itself: no interaction, only an open ramp and markings
        world.addProp(new Shuttle(42, 29, 7, 4));
        world.addProp(new FloorMarking(43, 27, 4, 1, "DOCK 02", Palette.ACCENT_BLUE));
        world.addProp(new FloorMarking(40, 34, 3, 1, "FUEL", Palette.HAZARD));
        world.addProp(new FloorMarking(45, 35, 3, 1, "CARGO", Palette.ACCENT_AMBER));
        world.addProp(new FloorGrate(42, 38, 3, 1));
        world.addProp(new ScorchMark(49, 34, 2, 1));
    }
}