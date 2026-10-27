package com.spaceexplorer.ui;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.core.StoryState;
import com.spaceexplorer.cryo.CryoRoom;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.hazard.GasLeak;
import com.spaceexplorer.hazard.GasLeakCountdown;
import com.spaceexplorer.interaction.ControlPanel;
import com.spaceexplorer.interaction.Interactable;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.interaction.RepairPartType;
import com.spaceexplorer.interaction.Vent;
import com.spaceexplorer.world.Camera;
import com.spaceexplorer.world.World;

import com.spaceexplorer.dialogue.DialogueManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import java.util.ArrayList;
import java.util.List;

/**
 * All 2D user interface: ship power status, the current objective, the
 * interaction prompt and the short wake up intro.
 *
 * <p>The HUD is drawn after the darkness overlay so it stays readable while
 * the ship is unlit.</p>
 */
public class Hud {

    private static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font FONT_BIG = new Font(Font.SANS_SERIF, Font.BOLD, 30);
    private static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 44);
    private static final Font FONT_DIALOGUE = new Font(Font.SANS_SERIF, Font.PLAIN, 17);
    private static final Font FONT_DIALOGUE_SPEAKER = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private static final int RETRY_BUTTON_WIDTH = 220;
    private static final int RETRY_BUTTON_HEIGHT = 56;

    private final Player player;
    private final ControlPanel controlPanel;
    private final World world;
    private final Inventory inventory;
    private final Camera camera;
    private final Vent vent;
    private final GasLeak gasLeak;
    private final CryoRoom cryoRoom;
    private final GasLeakCountdown gasLeakCountdown;
    private final StoryState storyState;
    private final DialogueManager dialogueManager;

    private float introFade = 1.0f;
    private double time;
    private boolean gameOver;

    public Hud(Player player, ControlPanel controlPanel, World world, Inventory inventory, Camera camera,
               GasLeakCountdown gasLeakCountdown) {
        this(player, controlPanel, world, inventory, camera, gasLeakCountdown,
                new StoryState(), new DialogueManager());
    }

    public Hud(Player player, ControlPanel controlPanel, World world, Inventory inventory, Camera camera,
               GasLeakCountdown gasLeakCountdown, StoryState storyState, DialogueManager dialogueManager) {
        this.player = player;
        this.controlPanel = controlPanel;
        this.world = world;
        this.inventory = inventory;
        this.camera = camera;
        this.vent = findVent(world);
        this.gasLeak = findGasLeak(world);
        this.cryoRoom = world.getCryoRoom();
        this.gasLeakCountdown = gasLeakCountdown;
        this.storyState = storyState == null ? new StoryState() : storyState;
        this.dialogueManager = dialogueManager == null ? new DialogueManager() : dialogueManager;
    }

    /** The objective card needs the vent itself, so it is looked up once here. */
    private static Vent findVent(World world) {
        for (Interactable target : world.getInteractables()) {
            if (target instanceof Vent) {
                return (Vent) target;
            }
        }
        return null;
    }

    /** Same for the gas leak hazard. */
    private static GasLeak findGasLeak(World world) {
        for (Interactable target : world.getInteractables()) {
            if (target instanceof GasLeak) {
                return (GasLeak) target;
            }
        }
        return null;
    }

    public void setIntroFade(float introFade) {
        this.introFade = introFade;
    }

    /** While the ship is destroyed only the failure screen is drawn. */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void draw(Graphics2D g, int viewWidth, int viewHeight, double secondsElapsed) {
        this.time = secondsElapsed;

        if (gameOver) {
            return;
        }

        drawLockedSupplyRoomDarkness(g, viewWidth, viewHeight);

        drawPowerStatus(g);
        drawObjectiveCard(g);
        drawGasLeakTimer(g);
        drawSurvivalMeters(g, viewWidth);
        drawObjectiveMarker(g, viewWidth, viewHeight);
        if (dialogueManager == null || !dialogueManager.isOpen()) {
            drawInteractionPrompt(g, viewWidth, viewHeight);
            drawPlayerToast(g, viewWidth, viewHeight);
        }
        drawIntro(g, viewWidth, viewHeight);
        if (dialogueManager != null && dialogueManager.isOpen()) {
            drawDialogue(g, viewWidth, viewHeight);
        }
    }

    /* ---------- top left status card ---------- */

    private void drawPowerStatus(Graphics2D g) {
        boolean powered = controlPanel.isActivated();
        Color accent = powered ? Palette.LED_ON : Palette.LED_EMERGENCY;

        int cardWidth = 224;
        int cardHeight = 44;
        int x = 14;
        int y = 14;

        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));

        g.setColor(accent);
        g.fill(new Ellipse2D.Double(x + 12, y + 17, 10, 10));

        g.setFont(FONT_LABEL);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString("SHIP LIGHTING", x + 32, y + 20);
        g.setFont(FONT_SMALL);
        g.setColor(accent);
        g.drawString(powered ? "POWER RESTORED" : "EMERGENCY POWER ONLY", x + 32, y + 35);
    }

    /* ---------- objective text and off screen marker ---------- */

    /**
     * Card under the power status: what to do next. While the gas leak is on
     * the agenda it carries the two component checklist alongside the text.
     */
    private void drawObjectiveCard(Graphics2D g) {
        if (vent == null || gasLeak == null) {
            return;
        }
        boolean powered = controlPanel.isActivated();
        boolean ventRepaired = vent.isRepaired();
        boolean contained = gasLeak.isContained();
        boolean discovered = gasLeak.isDiscovered();

        int cardWidth = 400;
        int cardHeight = 78;
        int x = 14;
        int y = 66;

        Color accent = Palette.ACCENT_AMBER;
        String objective;
        String line1 = null;
        String line2 = null;
        boolean[] checklist = null;
        boolean supplyChecklist = false;

        if (cryoRoom != null && cryoRoom.isDiscovered()) {
            if (!cryoRoom.isPowered()) {
                accent = Palette.LED_EMERGENCY;
                objective = "Restore power to the cryopods";
                line1 = "Use the console just inside the door";
            } else if (cryoRoom.getOpenedCount() < cryoRoom.getPodCount()) {
                accent = Palette.ACCENT_BLUE;
                objective = "Open the cryopods";
                line1 = "Cryopods opened: " + cryoRoom.getOpenedCount()
                        + "/" + cryoRoom.getPodCount();
            } else {
                if (!storyState.isFuelProblemLearned()) {
                    accent = Palette.LED_ON;
                    objective = "Talk to the survivors.";
                    line1 = "Two survivors have been awakened";
                } else if (!storyState.areSuppliesComplete()) {
                    accent = Palette.ACCENT_BLUE;
                    objective = "Collect food and water before leaving the ship.";
                    supplyChecklist = true;
                    checklist = new boolean[]{storyState.hasFoodSupplies(),
                            storyState.hasWaterSupplies()};
                    line2 = "Supplies are stored off the power deck";
                } else {
                    accent = Palette.LED_ON;
                    objective = "Prepare for the trip to the planet.";
                    line1 = "Supplies secured. You're ready for the next stage.";
                }
            }
        } else if (contained) {
            accent = Palette.LED_ON;
            objective = "Gas leak contained.";
            line1 = "All repairs complete";
            line2 = "A sealed bay waits beyond the storage deck";
        } else if (!ventRepaired) {
            accent = Palette.ACCENT_AMBER;
            objective = powered ? "Repair the ventilation system" : "Restore main lighting";
            if (powered) {
                int carried = inventory.count();
                int needed = Vent.REQUIRED_PARTS.size();
                line1 = "Parts: " + carried + "/" + needed
                        + (carried == needed ? "  - ready to repair" : "");
            }
        } else if (discovered) {
            accent = Palette.LED_EMERGENCY;
            checklist = new boolean[]{inventory.has(RepairPartType.PIPE_CLAMP),
                    inventory.has(RepairPartType.SEALING_COMPOUND)};
            if (checklist[0] && checklist[1]) {
                objective = "Return to the gas leak and make an emergency repair.";
            } else {
                objective = "Contain the gas leak";
                line2 = "Parts are stored in the maintenance and storage decks";
            }
        } else {
            objective = "Explore the ship";
            line1 = "Find a way below the crew quarters";
        }

        g.setColor(new Color(0, 0, 0, 140));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));

        g.setFont(FONT_LABEL);
        g.setColor(Palette.TEXT_DIM);
        g.drawString("OBJECTIVE", x + 14, y + 16);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString(objective, x + 14, y + 33);

        if (checklist != null) {
            g.setFont(FONT_LABEL);
            int rowY = y + 52;
            if (supplyChecklist) {
                int firstWidth = drawSupplyChecklistItem(g, x + 14, rowY,
                        "FOOD SUPPLIES", checklist[0]);
                drawSupplyChecklistItem(g, x + 14 + firstWidth + 18, rowY,
                        "WATER SUPPLIES", checklist[1]);
            } else {
                int firstWidth = drawChecklistItem(g, x + 14, rowY,
                        RepairPartType.PIPE_CLAMP, checklist[0]);
                drawChecklistItem(g, x + 14 + firstWidth + 18, rowY,
                        RepairPartType.SEALING_COMPOUND, checklist[1]);
            }
        } else {
            g.setFont(FONT_SMALL);
            g.setColor(Palette.TEXT_DIM);
            if (line1 != null) {
                g.drawString(line1, x + 14, y + 51);
            }
        }

        if (line2 != null) {
            g.setFont(FONT_SMALL);
            g.setColor(accent);
            g.drawString(line2, x + 14, y + 69);
        }
    }

    /** One checklist row, e.g. "[X] Emergency Pipe Clamp". Returns its width. */
    private int drawChecklistItem(Graphics2D g, int x, int y,
                                  RepairPartType part, boolean owned) {
        g.setColor(owned ? Palette.LED_ON : Palette.TEXT_DIM);
        g.drawString(owned ? "[X] " : "[ ] ", x, y);
        g.setColor(owned ? Palette.LED_ON : Palette.TEXT_PRIMARY);
        g.drawString(part.getDisplayName(), x + 32, y);
        return g.getFontMetrics().stringWidth("[X] ") + g.getFontMetrics().stringWidth(part.getDisplayName()) + 14;
    }

    /** Checklist row for the food/water supplies. */
    private int drawSupplyChecklistItem(Graphics2D g, int x, int y,
                                       String label, boolean owned) {
        g.setColor(owned ? Palette.LED_ON : Palette.TEXT_DIM);
        String prefix = label + ": ";
        g.drawString(prefix, x, y);
        int prefixWidth = g.getFontMetrics().stringWidth(prefix);
        g.setColor(owned ? Palette.LED_ON : Palette.TEXT_PRIMARY);
        g.drawString(owned ? "[X]" : "[ ]", x + prefixWidth, y);
        return prefixWidth + g.getFontMetrics().stringWidth("[X]") + 18;
    }
    /**
     * The failure timer, shown under the objective card once the gas leak has
     * become the live objective. It stays hidden before it starts and after a
     * successful repair, and turns critical red in the last fifteen seconds.
     */
    private void drawGasLeakTimer(Graphics2D g) {
        if (gasLeakCountdown == null || !gasLeakCountdown.isVisible()) {
            return;
        }
        double remaining = gasLeakCountdown.getRemainingSeconds();
        boolean critical = remaining <= 15.0;

        Color accent = critical ? Palette.LED_EMERGENCY : Palette.ACCENT_AMBER;
        int borderAlpha = 120;
        if (critical) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(time * 8.0);
            borderAlpha = Math.round(120 + 110 * pulse);
        }

        int cardWidth = 420;
        int cardHeight = 40;
        int x = 14;
        int y = 152;

        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), borderAlpha));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));

        g.setColor(accent);
        g.fill(new Ellipse2D.Double(x + 14, y + 14, 12, 12));

        g.setFont(FONT_LABEL);
        g.setColor(critical ? Palette.LED_EMERGENCY : Palette.TEXT_PRIMARY);
        g.drawString("GAS LEAK \u2014 TIME REMAINING: " + gasLeakCountdown.getRemainingText(),
                x + 36, y + 25);
    }

    private void drawObjectiveMarker(Graphics2D g, int viewWidth, int viewHeight) {
        double targetX;
        double targetY;
        if (!controlPanel.isActivated()) {
            targetX = controlPanel.getCenterX();
            targetY = controlPanel.getCenterY();
        } else if (cryoRoom != null && cryoRoom.isDiscovered() && !cryoRoom.isPowered()) {
            targetX = cryoRoom.getPowerPanel().getCenterX();
            targetY = cryoRoom.getPowerPanel().getCenterY();
        } else {
            return;
        }
        double screenX = camera.toScreenX(targetX);
        double screenY = camera.toScreenY(targetY);
        boolean onScreen = screenX > 24 && screenX < viewWidth - 24
                && screenY > 24 && screenY < viewHeight - 24;
        if (onScreen) {
            return;
        }

        double dx = screenX - viewWidth / 2.0;
        double dy = screenY - viewHeight / 2.0;
        double halfWidth = viewWidth / 2.0 - 34;
        double halfHeight = viewHeight / 2.0 - 34;
        double scaleX = dx == 0 ? Double.MAX_VALUE : halfWidth / Math.abs(dx);
        double scaleY = dy == 0 ? Double.MAX_VALUE : halfHeight / Math.abs(dy);
        double scale = Math.min(scaleX, scaleY);

        double px = viewWidth / 2.0 + dx * scale;
        double py = viewHeight / 2.0 + dy * scale;
        double angle = Math.atan2(dy, dx);

        float pulse = (float) (0.65 + 0.35 * Math.sin(time * 4.0));
        g.setColor(new Color(Palette.ACCENT_AMBER.getRed(), Palette.ACCENT_AMBER.getGreen(),
                Palette.ACCENT_AMBER.getBlue(), Math.round(120 + 100 * pulse)));

        GeneralPath arrow = new GeneralPath(Path2D.WIND_NON_ZERO, 3);
        arrow.moveTo(12, 0);
        arrow.lineTo(-8, 8);
        arrow.lineTo(-4, 0);
        arrow.lineTo(-8, -8);
        arrow.closePath();

        Graphics2D marker = (Graphics2D) g.create();
        marker.translate(px, py);
        marker.rotate(angle);
        marker.fill(arrow);
        marker.dispose();
    }

    /* ---------- survival meters ---------- */

    private void drawSurvivalMeters(Graphics2D g, int viewWidth) {
        if (storyState == null || !storyState.areSuppliesComplete()) {
            return;
        }
        int cardWidth = 224;
        int cardHeight = 42;
        int gap = 8;
        int x = Math.max(14, viewWidth - cardWidth - 14);
        int y = 14;

        drawSurvivalMeter(g, x, y, cardWidth, cardHeight, "FOOD",
                storyState.getFoodPercent(), Palette.LED_ON);
        drawSurvivalMeter(g, x, y + cardHeight + gap, cardWidth, cardHeight, "WATER",
                storyState.getWaterPercent(), Palette.ACCENT_BLUE);
    }

    private void drawSurvivalMeter(Graphics2D g, int x, int y, int cardWidth, int cardHeight,
                                   String label, int percent, Color accent) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Double(x, y, cardWidth, cardHeight, 10, 10));

        g.setFont(FONT_LABEL);
        g.setColor(Palette.TEXT_PRIMARY);
        g.drawString(label, x + 12, y + 18);

        int barX = x + 66;
        int barY = y + 24;
        int barWidth = 118;
        int barHeight = 8;
        g.setColor(Palette.METAL_DARK);
        g.fill(new RoundRectangle2D.Double(barX, barY, barWidth, barHeight, 4, 4));
        g.setColor(accent);
        g.fill(new RoundRectangle2D.Double(barX, barY,
                Math.round(barWidth * percent / 100.0), barHeight, 4, 4));

        g.setFont(FONT_SMALL);
        g.setColor(accent);
        String value = percent + "%";
        g.drawString(value, x + cardWidth - 12 - g.getFontMetrics().stringWidth(value),
                y + 32);
    }

    /* ---------- dialogue box ---------- */

    private void drawDialogue(Graphics2D g, int viewWidth, int viewHeight) {
        if (dialogueManager == null || dialogueManager.getCurrentLine() == null) {
            return;
        }
        int marginX = Math.max(24, viewWidth / 40);
        int boxHeight = Math.max(108, Math.min(148, Math.round(viewHeight * 0.23f)));
        int bottomMargin = Math.max(16, viewHeight / 30);
        int x = marginX;
        int y = viewHeight - boxHeight - bottomMargin;
        int width = viewWidth - marginX * 2;
        int padding = 22;

        g.setColor(new Color(0, 0, 0, 130));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 4, width, boxHeight, 14, 14));
        g.setColor(new Color(7, 12, 22, 242));
        g.fill(new RoundRectangle2D.Double(x, y, width, boxHeight, 14, 14));
        g.setColor(Palette.ACCENT_BLUE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, width, boxHeight, 14, 14));
        g.setColor(new Color(255, 255, 255, 45));
        g.draw(new RoundRectangle2D.Double(x + 2, y + 2, width - 4, boxHeight - 4, 12, 12));

        g.setFont(FONT_DIALOGUE_SPEAKER);
        g.setColor(Palette.ACCENT_AMBER);
        String speaker = dialogueManager.getSpeakerName();
        g.drawString(speaker, x + padding, y + 28);

        g.setColor(new Color(255, 255, 255, 55));
        g.drawLine(x + padding, y + 38, x + width - padding, y + 38);

        g.setFont(FONT_DIALOGUE);
        g.setColor(Palette.TEXT_PRIMARY);
        String fullText = dialogueManager.getText();
        List<String> wrapped = wrapDialogueText(g, fullText, width - padding * 2);
        int revealed = dialogueManager.getRevealedCharacters();
        int consumed = 0;
        int lineHeight = 21;
        int textY = y + 62;
        for (String wrappedLine : wrapped) {
            int remaining = revealed - consumed;
            if (remaining <= 0) {
                break;
            }
            String visible = remaining >= wrappedLine.length()
                    ? wrappedLine
                    : wrappedLine.substring(0, Math.max(0, remaining));
            g.drawString(visible, x + padding, textY);
            textY += lineHeight;
            consumed += wrappedLine.length() + 1;
        }

        String hint = dialogueManager.isLineComplete()
                ? (dialogueManager.hasNextLine() ? "E / SPACE  NEXT" : "E / SPACE  CLOSE")
                : "...";
        g.setFont(FONT_SMALL);
        g.setColor(Palette.TEXT_DIM);
        int hintWidth = g.getFontMetrics().stringWidth(hint);
        g.drawString(hint, x + width - padding - hintWidth, y + boxHeight - 12);

        if (dialogueManager.isLineComplete()) {
            g.setColor(Palette.ACCENT_AMBER);
            g.fill(new RoundRectangle2D.Double(x + width - padding - hintWidth - 16,
                    y + boxHeight - 20, 8, 8, 3, 3));
        }
    }

    private static List<String> wrapDialogueText(Graphics2D g, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        FontMetrics metrics = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth || current.length() == 0) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    /* ---------- locked supply room darkness ---------- */

    /**
     * Keeps the Supply Room unreadable while it is locked, even after the main
     * ship lights have been restored. The overlay is drawn after the world and
     * lighting, but before the HUD elements, so it never covers prompts, meters
     * or dialogue.
     */
    private void drawLockedSupplyRoomDarkness(Graphics2D g, int viewWidth, int viewHeight) {
        if (storyState == null || storyState.isSupplyRoomUnlocked()) {
            return;
        }

        int roomX = 38 * World.TILE_SIZE;
        int roomY = 2 * World.TILE_SIZE;
        int roomWidth = 11 * World.TILE_SIZE;
        int roomHeight = 11 * World.TILE_SIZE;

        int screenX = (int) Math.round(camera.toScreenX(roomX));
        int screenY = (int) Math.round(camera.toScreenY(roomY));
        int screenWidth = (int) Math.round(roomWidth * 1.0);
        int screenHeight = (int) Math.round(roomHeight * 1.0);

        // Clamp to the viewport so the overlay stays well behaved at the edges.
        int left = Math.max(0, screenX);
        int top = Math.max(0, screenY);
        int right = Math.min(viewWidth, screenX + screenWidth);
        int bottom = Math.min(viewHeight, screenY + screenHeight);
        if (right <= left || bottom <= top) {
            return;
        }

        // Use a high but non-opaque alpha so the room's walls and floor still
        // read as a faint silhouette rather than disappearing entirely.
        g.setColor(new Color(6, 10, 22, 210));
        g.fillRect(left, top, right - left, bottom - top);

        // A slightly lighter inner edge hints that the room continues beyond
        // the doorway without revealing any of its contents.
        g.setColor(new Color(255, 255, 255, 18));
        g.drawRect(left, top, right - left - 1, bottom - top - 1);
    }

    /* ---------- interaction prompt ---------- */

    private void drawInteractionPrompt(Graphics2D g, int viewWidth, int viewHeight) {
        if (controlPanel.canInteract(player)) {
            drawInteractionBox(g, viewWidth, viewHeight, null, controlPanel.getPrompt(), true);
            return;
        }
        for (Interactable target : world.getInteractables()) {
            if (!target.isInRange(player)) {
                continue;
            }
            drawInteractionBox(g, viewWidth, viewHeight,
                    target.getStatusText(player, inventory),
                    target.getActionPrompt(player, inventory),
                    target.isActionReady(player, inventory));
            return;
        }
    }

    /**
     * One shared prompt box: an optional dim status line above the action line,
     * which only gets the E badge when pressing E would really do something.
     */
    private void drawInteractionBox(Graphics2D g, int viewWidth, int viewHeight,
                                    String status, String prompt, boolean actionable) {
        if (status == null && prompt == null) {
            return;
        }
        g.setFont(FONT_SMALL);
        int statusWidth = status == null ? 0 : g.getFontMetrics().stringWidth(status) + 16;
        g.setFont(FONT_LABEL);
        int promptWidth = prompt == null ? 0 : g.getFontMetrics().stringWidth(prompt) + (actionable ? 42 : 16);

        int boxWidth = Math.max(statusWidth, promptWidth) + 20;
        int extraLine = status == null ? 0 : 15;
        int boxHeight = 44 + extraLine;
        int x = (viewWidth - boxWidth) / 2;
        int y = viewHeight - 132 - extraLine;

        Color accent = actionable ? Palette.ACCENT_AMBER : Palette.TEXT_DIM;
        g.setPaint(new GradientPaint(x, y, new Color(0, 0, 0, 60), x, y + boxHeight, new Color(0, 0, 0, 190)));
        g.fill(new RoundRectangle2D.Double(x, y, boxWidth, boxHeight, 12, 12));
        g.setColor(accent);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, boxWidth, boxHeight, 12, 12));

        if (status != null) {
            g.setFont(FONT_SMALL);
            g.setColor(Palette.TEXT_DIM);
            g.drawString(status, x + 16, y + 18);
        }
        if (prompt == null) {
            return;
        }

        int textX = x + (actionable ? 42 : 16);
        int baseline = status == null ? y + 27 : y + 38;
        if (actionable) {
            int badgeY = status == null ? y + 12 : y + 27;
            g.setColor(accent);
            g.fill(new RoundRectangle2D.Double(x + 12, badgeY, 20, 20, 6, 6));
            g.setColor(new Color(0x10151F));
            g.setFont(FONT_LABEL);
            g.drawString("E", x + 19, badgeY + 15);
        }
        g.setColor(actionable ? Palette.TEXT_PRIMARY : Palette.TEXT_DIM);
        g.setFont(FONT_LABEL);
        g.drawString(prompt, textX, baseline);
    }

    private void drawPlayerToast(Graphics2D g, int viewWidth, int viewHeight) {
        String message = player.getMessage();
        if (message.isEmpty()) {
            return;
        }
        g.setFont(FONT_LABEL);
        int textWidth = g.getFontMetrics().stringWidth(message);
        int x = (viewWidth - textWidth) / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(new RoundRectangle2D.Double(x - 14, viewHeight - 78, textWidth + 28, 30, 10, 10));
        g.setColor(Palette.ACCENT_BLUE);
        g.drawString(message, x, viewHeight - 57);
    }

    /* ---------- failure screen ---------- */

    /** The clickable RETRY button rectangle, shared by drawing and hit testing. */
    public Rectangle2D retryButtonBounds(int viewWidth, int viewHeight) {
        int x = (viewWidth - RETRY_BUTTON_WIDTH) / 2;
        int y = viewHeight / 2 + 70;
        return new Rectangle2D.Double(x, y, RETRY_BUTTON_WIDTH, RETRY_BUTTON_HEIGHT);
    }

    /**
     * The ship destruction screen: a white flash, an expanding fireball with
     * shock rings, the headline and the RETRY button. Drawn by {@link Game} on
     * top of the frozen world, so it is the last thing on screen.
     */
    public void drawFailure(Graphics2D g, int viewWidth, int viewHeight, double seconds) {
        int wash = (int) Math.min(205, 45 + seconds * 95);
        g.setColor(new Color(22, 4, 4, wash));
        g.fillRect(0, 0, viewWidth, viewHeight);

        double cx = viewWidth / 2.0;
        double cy = viewHeight / 2.0 - 30;

        for (int i = 0; i < 3; i++) {
            double age = seconds - i * 0.18;
            if (age < 0.0) {
                continue;
            }
            double radius = 40 + age * 330;
            int alpha = (int) Math.max(0.0, 210 * (1.0 - age * 1.7));
            if (alpha == 0) {
                continue;
            }
            Color ring = (i & 1) == 0 ? Palette.LED_EMERGENCY : Palette.ACCENT_AMBER;
            g.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), alpha));
            g.setStroke(new BasicStroke(Math.max(1.5f, 7f - i * 2f)));
            g.draw(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0));
        }

        double core = Math.max(0.0, 95.0 - seconds * 34.0);
        if (core > 1.0) {
            Color[] fireball = {
                    new Color(255, 244, 206), new Color(255, 176, 64),
                    new Color(226, 82, 38), new Color(120, 20, 18)};
            for (int i = 0; i < fireball.length; i++) {
                double r = core * (1.0 - i * 0.17);
                int a = Math.max(0, 210 - i * 45);
                Color c = fireball[i];
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
                g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0));
            }
        }

        if (seconds < 0.25) {
            int a = (int) (255 * (1.0 - seconds / 0.25));
            g.setColor(new Color(255, 255, 255, Math.max(0, a)));
            g.fillRect(0, 0, viewWidth, viewHeight);
        }

        // Headline and button fade in once the blast is under way.
        float reveal = clamp01((seconds - 0.35) / 0.4);
        if (reveal <= 0.0f) {
            return;
        }
        int textAlpha = Math.round(255 * reveal);

        g.setFont(FONT_TITLE);
        g.setColor(new Color(Palette.LED_EMERGENCY.getRed(), Palette.LED_EMERGENCY.getGreen(),
                Palette.LED_EMERGENCY.getBlue(), textAlpha));
        String title = "CRITICAL FAILURE";
        g.drawString(title, (viewWidth - g.getFontMetrics().stringWidth(title)) / 2, viewHeight / 2 - 40);

        g.setFont(FONT_BIG);
        g.setColor(new Color(255, 233, 214, textAlpha));
        String subtitle = "SHIP DESTROYED";
        g.drawString(subtitle, (viewWidth - g.getFontMetrics().stringWidth(subtitle)) / 2, viewHeight / 2 - 2);

        Rectangle2D button = retryButtonBounds(viewWidth, viewHeight);
        g.setColor(new Color(0, 0, 0, Math.round(190 * reveal)));
        g.fill(new RoundRectangle2D.Double(button.getX(), button.getY(),
                button.getWidth(), button.getHeight(), 12, 12));
        g.setColor(new Color(Palette.LED_EMERGENCY.getRed(), Palette.LED_EMERGENCY.getGreen(),
                Palette.LED_EMERGENCY.getBlue(), textAlpha));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(button.getX(), button.getY(),
                button.getWidth(), button.getHeight(), 12, 12));

        g.setFont(FONT_BIG);
        g.setColor(new Color(Palette.TEXT_PRIMARY.getRed(), Palette.TEXT_PRIMARY.getGreen(),
                Palette.TEXT_PRIMARY.getBlue(), textAlpha));
        String label = "RETRY";
        g.drawString(label, (int) (button.getX() + (button.getWidth() - g.getFontMetrics().stringWidth(label)) / 2),
                (int) (button.getY() + button.getHeight() / 2 + 11));
    }

    /* ---------- intro ---------- */

    private void drawIntro(Graphics2D g, int viewWidth, int viewHeight) {
        if (introFade <= 0.01f) {
            return;
        }
        int alpha = Math.round(210 * introFade);
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(0, 0, viewWidth, viewHeight);

        float pulse = 0.6f + 0.4f * (float) Math.sin(time * 2.5);

        g.setFont(FONT_BIG);
        g.setColor(new Color(Palette.LED_EMERGENCY.getRed(), Palette.LED_EMERGENCY.getGreen(),
                Palette.LED_EMERGENCY.getBlue(), Math.round(255 * introFade)));
        String title = "SYSTEMS OFFLINE";
        int titleWidth = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (viewWidth - titleWidth) / 2, viewHeight / 2 - 30);

        g.setFont(FONT_SMALL);
        String subtitle = "EMERGENCY POWER ONLY  -  MAIN LIGHTING DISABLED";
        int subtitleWidth = g.getFontMetrics().stringWidth(subtitle);
        g.setColor(new Color(Palette.TEXT_DIM.getRed(), Palette.TEXT_DIM.getGreen(),
                Palette.TEXT_DIM.getBlue(), Math.round(255 * introFade)));
        g.drawString(subtitle, (viewWidth - subtitleWidth) / 2, viewHeight / 2 - 8);

        String hint = "Find the lighting control panel in the power room.";
        int hintWidth = g.getFontMetrics().stringWidth(hint);
        g.setColor(new Color(Palette.ACCENT_AMBER.getRed(), Palette.ACCENT_AMBER.getGreen(),
                Palette.ACCENT_AMBER.getBlue(), Math.round(255 * introFade * pulse)));
        g.drawString(hint, (viewWidth - hintWidth) / 2, viewHeight / 2 + 22);
    }

    private static float clamp01(double value) {
        if (value < 0.0) {
            return 0.0f;
        }
        return (float) Math.min(1.0, value);
    }
}
