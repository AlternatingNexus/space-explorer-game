package com.spaceexplorer;

import com.spaceexplorer.core.GameState;
import com.spaceexplorer.core.InputHandler;
import com.spaceexplorer.core.StoryState;
import com.spaceexplorer.cryo.CryoRoom;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.entity.Survivor;
import com.spaceexplorer.prop.Shuttle;
import com.spaceexplorer.hazard.GasLeak;
import com.spaceexplorer.hazard.GasLeakCountdown;
import com.spaceexplorer.hazard.Hazard;
import com.spaceexplorer.interaction.ControlPanel;
import com.spaceexplorer.interaction.Interactable;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.interaction.Vent;
import com.spaceexplorer.dialogue.DialogueManager;
import com.spaceexplorer.planet.PlanetWorld;
import com.spaceexplorer.planet.PlanetRenderer;
import com.spaceexplorer.ui.GamePanel;
import com.spaceexplorer.ui.Hud;
import com.spaceexplorer.ui.WorldRenderer;
import com.spaceexplorer.world.Camera;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Lighting;
import com.spaceexplorer.world.PrototypeLevel;
import com.spaceexplorer.world.World;

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
    private StoryState storyState;
    private DialogueManager dialogueManager;
    private double suppliesSecuredNoticeTimer;
    private Lighting lighting;
    private Light suitLight;
    private WorldRenderer renderer;
    private Hud hud;
    private GasLeak gasLeak;

    private enum WorldMode { SHIP, PLANET }
    private enum TransitionPhase { FADE_OUT, BLACK, FADE_IN }
    private WorldMode worldMode = WorldMode.SHIP;
    private PlanetWorld planetWorld;
    private PlanetRenderer planetRenderer;
    private boolean transitioningToPlanet;
    private TransitionPhase transitionPhase;
    private double transitionTimer;
    private GasLeakCountdown gasLeakCountdown;

    private GameState state;
    private double introTimer;
    private double elapsedSeconds;
    private double failureSeconds;
    private long lastTickNanos;

    private final boolean startInPlanetMode;
    private final java.awt.Font loadingFont;

    public Game() {
        this(false);
    }

    public Game(boolean startInPlanetMode) {
        this.startInPlanetMode = startInPlanetMode;
        this.loadingFont = loadLoadingFont();
        initRun();
        this.panel = new GamePanel(this, input);
        this.timer = new Timer(TIMER_INTERVAL_MS, event -> tick());
    }

    private static java.awt.Font loadLoadingFont() {
        try (java.io.InputStream stream = Game.class.getResourceAsStream("/com/spaceexplorer/resources/Orbitron.ttf")) {
            if (stream != null) {
                java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream);
                return font.deriveFont(java.awt.Font.BOLD, 30f);
            }
        } catch (Exception ignored) {
            // Fall through to the bundled-independent fallback below.
        }
        return new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 30);
    }

    /**
     * Builds a brand new playthrough from scratch. Also used by RETRY, so it
     * must leave no trace of a previous run: fresh world, player, inventory,
     * lighting, HUD and countdown, with the progression back at the start.
     */
    private void initRun() {
        this.planetWorld = null;
        this.planetRenderer = null;
        this.storyState = new StoryState();
        this.world = PrototypeLevel.build(storyState);
        this.dialogueManager = new DialogueManager();
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
        this.hud = new Hud(player, world.getControlPanel(), world, inventory, camera, gasLeakCountdown,
                storyState, dialogueManager);

        this.state = GameState.INTRO;
        this.introTimer = INTRO_SECONDS;
        this.elapsedSeconds = 0.0;
        this.failureSeconds = 0.0;
        this.suppliesSecuredNoticeTimer = 0.0;
        this.worldMode = WorldMode.SHIP;
        if (startInPlanetMode) {
            this.worldMode = WorldMode.PLANET;
            if (this.planetWorld == null) {
                this.planetWorld = new PlanetWorld(0x5EED1234L);
                this.planetRenderer = new PlanetRenderer(this.planetWorld, camera);
            }
            this.player = new Player(0, 0);
            this.camera.centerOn(player.getCenterX(), player.getCenterY());
            this.transitioningToPlanet = false;
        }
        this.transitioningToPlanet = false;
        this.transitionPhase = TransitionPhase.FADE_OUT;
        this.transitionTimer = 0.0;
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
        boolean planetMode = Boolean.getBoolean("eryx9")
                || Boolean.getBoolean("spaceexplorer.eryx9");
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--eryx9")) {
                planetMode = true;
            }
        }
        boolean finalPlanetMode = planetMode;
        SwingUtilities.invokeLater(() -> new Game(finalPlanetMode).start());
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

        if (transitioningToPlanet) {
            transitionTimer += dt;
            if (transitionPhase == TransitionPhase.FADE_OUT && transitionTimer >= 0.7) {
                transitionPhase = TransitionPhase.BLACK;
                transitionTimer = 0.0;
                worldMode = WorldMode.PLANET;
                if (planetWorld == null) {
                    planetWorld = new PlanetWorld(0x5EED1234L);
                    planetRenderer = new PlanetRenderer(planetWorld, camera);
                }
                player = new Player(0, 0);
                camera.centerOn(player.getCenterX(), player.getCenterY());
            } else if (transitionPhase == TransitionPhase.BLACK && transitionTimer >= 2.0) {
                transitionPhase = TransitionPhase.FADE_IN;
                transitionTimer = 0.0;
            } else if (transitionPhase == TransitionPhase.FADE_IN && transitionTimer >= 0.7) {
                transitioningToPlanet = false;
            }
            input.endFrame();
            return;
        }

        if (worldMode == WorldMode.PLANET) {
            if (planetWorld != null) {
                planetWorld.update(dt, player);
            }
            player.update(dt, input, planetWorld);
            camera.centerOn(player.getCenterX(), player.getCenterY());
            input.endFrame();
            return;
        }

        if (dialogueManager.isOpen()) {
            dialogueManager.update(dt, input);
            input.endFrame();
            return;
        }

        if (storyState.consumeSuppliesCompletionNotice()) {
            suppliesSecuredNoticeTimer = 1.8;
        }
        if (suppliesSecuredNoticeTimer > 0.0) {
            suppliesSecuredNoticeTimer -= dt;
            if (suppliesSecuredNoticeTimer <= 0.0) {
                player.say("Supplies secured. You're ready for the next stage.", 5.0);
            }
        }

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
            if (!target.isInRange(player)) {
                continue;
            }
            if (target instanceof Shuttle) {
                startPlanetTransition();
                return;
            }
            if (target instanceof Survivor survivor) {
                if (!dialogueManager.isOpen()) {
                    dialogueManager.start(survivor.getConversation(), () -> {
                        storyState.learnFuelProblem();
                        if (storyState.acquireSupplyRoomKey()) {
                            player.say("Supply Room Key acquired.", 4.0);
                        }
                    });
                }
                return;
            }
            if (target.interact(player, inventory)) {
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

    private void startPlanetTransition() {
        if (transitioningToPlanet || worldMode != WorldMode.SHIP) {
            return;
        }
        transitioningToPlanet = true;
        transitionPhase = TransitionPhase.FADE_OUT;
        transitionTimer = 0.0;
        if (planetWorld == null) {
            planetWorld = new PlanetWorld(0x5EED1234L);
            planetRenderer = new PlanetRenderer(planetWorld, camera);
        }
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

    public void onMouseClick(int x, int y, int viewWidth, int viewHeight) {
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

    public void render(Graphics2D g, int viewWidth, int viewHeight) {
        boolean destroyed = state == GameState.SHIP_DESTROYED;
        hud.setGameOver(destroyed);

        camera.setViewportSize(viewWidth, viewHeight);

        if (worldMode == WorldMode.PLANET && planetRenderer != null) {
            planetRenderer.render(g, player, camera);
        } else {
            renderer.render(g, player, lighting, hud, elapsedSeconds);
            if (destroyed) {
                hud.drawFailure(g, viewWidth, viewHeight, failureSeconds);
            }
        }

        if (transitioningToPlanet) {
            float alpha;
            if (transitionPhase == TransitionPhase.FADE_OUT) {
                alpha = (float) Math.min(1.0, transitionTimer / 0.7);
            } else if (transitionPhase == TransitionPhase.BLACK) {
                alpha = 1.0f;
            } else {
                alpha = (float) Math.max(0.0, 1.0 - transitionTimer / 0.7);
            }
            g.setColor(new Color(0, 0, 0, Math.round(255 * alpha)));
            g.fillRect(0, 0, viewWidth, viewHeight);

            if (transitionPhase == TransitionPhase.BLACK || alpha > 0.92f) {
                g.setFont(loadingFont);
                g.setColor(new Color(220, 225, 235, Math.round(255 * Math.min(1.0f, alpha))));
                String text = "TRAVELLING TO ERYX-9";
                java.awt.FontMetrics metrics = g.getFontMetrics();
                int spacing = 4;
                int totalWidth = metrics.stringWidth(text) + spacing * (text.length() - 1);
                int textX = (viewWidth - totalWidth) / 2;
                int textY = viewHeight / 2 + 24;
                for (int i = 0; i < text.length(); i++) {
                    String character = String.valueOf(text.charAt(i));
                    g.drawString(character, textX, textY);
                    textX += metrics.charWidth(text.charAt(i)) + spacing;
                }
            }
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
