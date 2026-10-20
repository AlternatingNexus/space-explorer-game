package com.spaceexplorer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Entry point and game loop.
 *
 * <p>Logic and rendering are kept apart: {@link #update(double)} advances the
 * simulation using a delta time in seconds, {@link #render} only draws the
 * current state. A Swing timer drives the loop on the event dispatch thread,
 * which is simple and safe for a 2D prototype - no extra threads needed.</p>
 *
 * <p>Everything that belongs to a single playthrough is built by
 * {@link #initRun()}, which is deliberately reusable: RETRY tears the run down
 * and builds a fresh one without touching the window, the timer or the input
 * plumbing.</p>
 */
public final class Game {

    public static final int VIEW_WIDTH = 1024;
    /** Matches the height of the prototype ship exactly, so no void is shown. */
    public static final int VIEW_HEIGHT = 16 * World.TILE_SIZE;

    private static final int TIMER_INTERVAL_MS = 1000 / 60;
    /** Longest step we ever simulate, so a stalled window cannot teleport the player. */
    private static final double MAX_DELTA_SECONDS = 0.05;
    private static final double INTRO_SECONDS = 3.0;

    private final InputHandler input = new InputHandler();
    private final Camera camera = new Camera(VIEW_WIDTH, VIEW_HEIGHT);
    private final GamePanel panel;
    private final Timer timer;

    /* Per run: rebuilt by initRun() for every playthrough. */
    private World world;
    private Player player;
    private Inventory inventory;
    private Lighting lighting;
    private Light suitLight;
    private WorldRenderer renderer;
    private Hud hud;
    private GasLeak gasLeak;
    private GasLeakCountdown gasLeakCountdown;

    private GameState state;
    private double introTimer;
    private double elapsedSeconds;
    private double failureSeconds;
    private long lastTickNanos;

    public Game() {
        initRun();
        this.panel = new GamePanel(this, input);
        this.timer = new Timer(TIMER_INTERVAL_MS, event -> tick());
    }

    /**
     * Builds a brand new playthrough from scratch. Also used by RETRY, so it
     * must leave no trace of a previous run: fresh world, player, inventory,
     * lighting, HUD and countdown, with the progression back at the start.
     */
    private void initRun() {
        this.world = PrototypeLevel.build();
        this.player = new Player(PrototypeLevel.PLAYER_START_TILE_X, PrototypeLevel.PLAYER_START_TILE_Y);
        this.inventory = new Inventory();

        this.lighting = new Lighting();
        for (Light light : world.getLights()) {
            lighting.addLight(light);
        }
        // Pool of light carried by the player, so the ship stays readable while dark.
        this.suitLight = new Light(
                player.getCenterX(), player.getCenterY(),
                235.0, 185.0,
                new Color(0xC7DCFF), new Color(0xD8E8FF), 0.72);
        lighting.addLight(suitLight);

        this.gasLeak = findGasLeak(world);
        this.gasLeakCountdown = new GasLeakCountdown();
        this.renderer = new WorldRenderer(world, camera);
        this.hud = new Hud(player, world.getControlPanel(), world, inventory, camera, gasLeakCountdown);

        this.state = GameState.INTRO;
        this.introTimer = INTRO_SECONDS;
        this.elapsedSeconds = 0.0;
        this.failureSeconds = 0.0;
        this.lastTickNanos = System.nanoTime();
    }

    /** The level's single gas leak hazard, or null when a level has none. */
    private static GasLeak findGasLeak(World world) {
        for (Interactable target : world.getInteractables()) {
            if (target instanceof GasLeak leak) {
                return leak;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Game().start());
    }

    private void start() {
        JFrame frame = new JFrame("SpaceExplorer - Prototype 1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();
        timer.start();
    }

    /* ---------- game loop ---------- */

    private void tick() {
        double deltaSeconds = (System.nanoTime() - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = System.nanoTime();

        update(Math.min(MAX_DELTA_SECONDS, deltaSeconds));
        panel.repaint();
    }

    private void update(double dt) {
        if (state == GameState.SHIP_DESTROYED) {
            updateFailure(dt);
            return;
        }

        elapsedSeconds += dt;

        if (state == GameState.INTRO) {
            introTimer -= dt;
            if (introTimer <= 0.0) {
                state = GameState.EXPLORING;
                player.say("Ship lighting is offline. Find the control panel.", 4.5);
            }
        } else {
            player.update(dt, input, world);
            updateInteraction();
            updateHazards(dt);
        }

        world.update(dt);
        CryoRoom cryoRoom = world.getCryoRoom();
        if (cryoRoom != null) {
            cryoRoom.update(dt, player);
        }
        updateGasLeakTimer();
        lighting.update(dt, elapsedSeconds);
        suitLight.setPosition(player.getCenterX(), player.getCenterY());

        camera.centerOn(player.getCenterX(), player.getCenterY(), world);
        hud.setIntroFade(clamp01(introTimer / 0.9));
        input.endFrame();
    }

    /**
     * The failure screen: the world is frozen, only the explosion animation
     * advances and the RETRY button (click or E / Enter) is live.
     */
    private void updateFailure(double dt) {
        failureSeconds += dt;
        if (input.isInteractPressed() || input.wasPressed(KeyEvent.VK_R)) {
            retry();
            return;
        }
        input.endFrame();
    }

    /**
     * The scripted interactions: restoring power at the console, picking up
     * repair parts and fitting them into the ventilation unit or the gas leak.
     * One key press resolves to at most one of them.
     */
    private void updateInteraction() {
        if (!input.isInteractPressed()) {
            return;
        }

        ControlPanel controlPanel = world.getControlPanel();
        if (controlPanel.canInteract(player)) {
            controlPanel.activate();
            lighting.setPowerRestored(true);
            state = advanceState(GameState.POWER_RESTORED);
            player.say("Power restored. Main lighting is online.", 4.5);
            return;
        }

        for (Interactable target : world.getInteractables()) {
            if (target.isInRange(player) && target.interact(player, inventory)) {
                if (target instanceof Vent) {
                    state = advanceState(GameState.VENTILATION_REPAIRED);
                } else if (target instanceof GasLeak) {
                    // Repair accepted: stop the clock at the moment of success.
                    gasLeakCountdown.seal();
                }
                return;
            }
        }
    }

    /**
     * Lets every environmental hazard react on its own (notice the player, run
     * its sealing animation) and records the moment one of them is contained.
     */
    private void updateHazards(double dt) {
        for (Interactable target : world.getInteractables()) {
            if (target instanceof Hazard hazard) {
                hazard.updateHazard(dt, player);
                if (hazard.isContained()) {
                    if (hazard instanceof GasLeak) {
                        // Belt and braces: containment always stops the countdown.
                        gasLeakCountdown.seal();
                    }
                    state = advanceState(GameState.GAS_LEAK_CONTAINED);
                }
            }
        }
    }

    /* ---------- gas leak failure timer ---------- */

    /**
     * Arms the countdown the moment the gas leak becomes the live objective,
     * keeps it in step with the wall clock, and trips the failure when it runs
     * out. It never restarts once armed and never trips after a successful
     * repair.
     */
    private void updateGasLeakTimer() {
        if (!gasLeakCountdown.hasStarted()) {
            if (isGasLeakObjectiveActive()) {
                gasLeakCountdown.start(System.nanoTime());
            }
            return;
        }
        gasLeakCountdown.update(System.nanoTime());
        if (gasLeakCountdown.isExpired()) {
            triggerFailure();
        }
    }

    /** True once the ventilation is repaired and the leak has been spotted. */
    private boolean isGasLeakObjectiveActive() {
        return gasLeak != null
                && !gasLeak.isContained()
                && gasLeak.isDiscovered()
                && state.ordinal() >= GameState.VENTILATION_REPAIRED.ordinal();
    }

    private void triggerFailure() {
        state = GameState.SHIP_DESTROYED;
        failureSeconds = 0.0;
    }

    /* ---------- retry ---------- */

    /**
     * Starts a completely fresh playthrough. The window, loop and input objects
     * outlive the run, so no timer, panel or listener is ever duplicated.
     */
    private void retry() {
        initRun();
        input.endFrame();
    }

    void onMouseClick(int x, int y, int viewWidth, int viewHeight) {
        if (state == GameState.SHIP_DESTROYED
                && hud.retryButtonBounds(viewWidth, viewHeight).contains(x, y)) {
            retry();
        }
    }

    /**
     * Progression is recorded monotonically: a reached state is never walked
     * back, even when a scripted beat could otherwise reset it. Returns the
     * resulting state so the caller can use it directly.
     */
    private GameState advanceState(GameState next) {
        if (next.ordinal() > state.ordinal()) {
            state = next;
        }
        return state;
    }

    /* ---------- rendering ---------- */

    void render(Graphics2D g, int viewWidth, int viewHeight) {
        boolean destroyed = state == GameState.SHIP_DESTROYED;
        hud.setGameOver(destroyed);

        camera.setViewportSize(viewWidth, viewHeight);
        renderer.render(g, player, lighting, hud, elapsedSeconds);

        if (destroyed) {
            hud.drawFailure(g, viewWidth, viewHeight, failureSeconds);
        }
    }

    /* ---------- accessors ---------- */

    public World getWorld() {
        return world;
    }

    public Player getPlayer() {
        return player;
    }

    public GameState getState() {
        return state;
    }

    public Inventory getInventory() {
        return inventory;
    }

    private static float clamp01(double value) {
        if (value < 0.0) {
            return 0.0f;
        }
        return (float) Math.min(1.0, value);
    }
}
