package com.spaceexplorer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * One cryogenic sleeper pod in the cryosleep bay.
 *
 * <p>Each pod is a solid 2x2 unit that can only be opened once the room's
 * {@link CryoPowerPanel} is active. Pressing E starts a short opening animation;
 * when it finishes the pod reports itself to {@link CryoRoom}. The room decides
 * the outcome: the <em>first</em> pod to open is always the empty one, and the
 * following two wake the two {@link Survivor}s. Every pod keeps its own opened /
 * empty state, so pods never reopen and survivors are never released twice.</p>
 */
public final class CryoPod extends Prop implements Interactable {

    public static final String PROMPT_OPEN = "Press E to open cryopod";
    public static final String STATUS_OFFLINE = "CRYOPOD OFFLINE";
    public static final String MESSAGE_OPENING = "Cryosleep chamber opening...";
    public static final String MESSAGE_EMPTY = "Cryopod empty.";
    public static final String MESSAGE_SURVIVOR = "Survivor released from cryosleep.";

    private static final double INTERACT_RANGE = 120.0;
    private static final double OPEN_SECONDS = 1.1;

    private static final Font SCREEN_FONT = new Font(Font.MONOSPACED, Font.BOLD, 8);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    private final CryoPowerPanel panel;
    private final CryoRoom cryoRoom;
    private final double interactX;
    private final double interactY;
    private final int survivorTileX;
    private final int survivorTileY;
    private final Light podLight;

    private boolean opening;
    private boolean opened;
    private boolean empty;
    private boolean contentFinalized;
    private boolean newlyOpened;
    private double openTimer;
    private double time;

    public CryoPod(int tileX, int tileY, int survivorTileX, int survivorTileY,
                   CryoPowerPanel panel, CryoRoom cryoRoom) {
        super(tileX, tileY, 2, 2, Layer.OBJECT);
        this.panel = panel;
        this.cryoRoom = cryoRoom;
        this.survivorTileX = survivorTileX;
        this.survivorTileY = survivorTileY;
        this.interactX = x + width / 2.0;
        this.interactY = y + height + 6.0;
        this.podLight = new Light(x + width / 2.0, y + height * 0.5,
                90.0, 160.0,
                Palette.LED_EMERGENCY,
                new Color(0x9FDBFF),
                0.55);
    }

    @Override
    public Light getLight() {
        return podLight;
    }

    public boolean isOpening() {
        return opening;
    }

    public boolean isOpened() {
        return opened;
    }

    /** True once the room has decided this pod's outcome and it was the empty one. */
    public boolean isEmpty() {
        return contentFinalized && empty;
    }

    /** True once the room has committed this pod to an outcome. */
    public boolean isContentFinalized() {
        return contentFinalized;
    }

    /** Where the released survivor stands, just below the foot of this pod. */
    public int getSurvivorTileX() {
        return survivorTileX;
    }

    public int getSurvivorTileY() {
        return survivorTileY;
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        double dx = player.getCenterX() - interactX;
        double dy = player.getCenterY() - interactY;
        return Math.sqrt(dx * dx + dy * dy) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !opened && !opening && panel.isPowered();
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        if (opened || opening) {
            return null;
        }
        return panel.isPowered() ? null : STATUS_OFFLINE;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        return isActionReady(player, inventory) ? PROMPT_OPEN : null;
    }

    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (!isActionReady(player, inventory)) {
            return false;
        }
        opening = true;
        openTimer = 0.0;
        player.say(MESSAGE_OPENING, 3.5);
        return true;
    }

    /* ---------- per frame ---------- */

    @Override
    public void update(double dt) {
        time += dt;
        podLight.setActive(panel.isPowered());

        if (opening) {
            openTimer += dt;
            if (openTimer >= OPEN_SECONDS) {
                opening = false;
                opened = true;
                newlyOpened = true;
            }
        }
    }

    /** Called by {@link CryoRoom} after it has decided this pod's outcome. */
    public void completeOpening(boolean empty) {
        this.empty = empty;
        this.contentFinalized = true;
        this.newlyOpened = false;
    }

    /** One shot: did the opening animation finish this frame worth of logic? */
    public boolean consumeNewlyOpened() {
        boolean flag = newlyOpened;
        newlyOpened = false;
        return flag;
    }

    /* ---------- drawing ---------- */

    @Override
    public void draw(Graphics2D g) {
        double progress = Math.min(1.0, openTimer / OPEN_SECONDS);
        double cx = x + width / 2.0;
        double cy = y + height * 0.42;

        // contact shadow and the conduit that ties the pod to the deck
        g.setColor(new Color(0, 0, 0, 80));
        g.fill(new Ellipse2D.Double(x + 2, y + height - 7, width - 2, 12));
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x + 10, y + height - 6, 5, 8);
        g.fillRect(x + width - 15, y + height - 6, 5, 8);
        g.setColor(Palette.METAL);
        g.fillRect(x + 11, y + height - 6, 3, 8);
        g.fillRect(x + width - 14, y + height - 6, 3, 8);

        // base plate
        g.setColor(Palette.METAL);
        g.fill(new RoundRectangle2D.Double(x + 3, y + height - 14, width - 6, 10, 6, 6));
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x + 5, y + height - 12, width - 10, 2);

        // capsule body
        g.setPaint(new GradientPaint(x, y + 4, Palette.METAL, x, y + height - 12, Palette.METAL_DARK));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 4, width - 8, height - 18, 14, 14));
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x + 4, y + 4, width - 8, height - 18, 14, 14));

        // viewing window
        int gx = x + 13;
        int gy = y + 15;
        int gw = width - 26;
        int gh = height - 40;
        g.setColor(Palette.WALL_EDGE);
        g.fill(new RoundRectangle2D.Double(gx - 2, gy - 2, gw + 4, gh + 4, 8, 8));
        g.setColor(windowColor());
        g.fill(new RoundRectangle2D.Double(gx, gy, gw, gh, 6, 6));

        if (opened && contentFinalized) {
            drawOpenedInterior(g, gx, gy, gw, gh);
        } else if (opening) {
            drawOpeningInterior(g, gx, gy, gw, gh, progress);
        } else if (panel.isPowered()) {
            drawFrostedInterior(g, gx, gy, gw, gh);
        }

        // status screen and LED on the foot of the capsule
        drawStatus(g, cx);
    }

    private Color windowColor() {
        if (opened) {
            return new Color(0x20364A);
        }
        if (panel.isPowered()) {
            return new Color(0x29435A);
        }
        return new Color(0x141A24);
    }

    /** Powered but sealed: frosted glass with a soft interior glow. */
    private void drawFrostedInterior(Graphics2D g, int gx, int gy, int gw, int gh) {
        g.setColor(new Color(120, 220, 255, 26));
        g.fill(new RoundRectangle2D.Double(gx + 4, gy + 4, gw - 8, gh - 8, 4, 4));
        g.setColor(new Color(255, 255, 255, 34));
        g.fill(new RoundRectangle2D.Double(gx + 6, gy + gh / 2 - 8, gw - 12, 3, 2, 2));
        g.fill(new RoundRectangle2D.Double(gx + 8, gy + gh / 2 - 2, gw - 16, 2, 2, 2));
    }

    /** The lid swings open, slowly revealing the pad underneath. */
    private void drawOpeningInterior(Graphics2D g, int gx, int gy, int gw, int gh, double progress) {
        g.setColor(new Color(0x1C3A54));
        g.fill(new RoundRectangle2D.Double(gx + 4, gy + 4, gw - 8, gh - 8, 4, 4));
        g.setColor(new Color(120, 220, 255, (int) (40 + 70 * progress)));
        g.fill(new RoundRectangle2D.Double(gx + 9, gy + 10, gw - 18, gh - 22, 3, 3));

        // the capsule lid rotating up from its hinge
        Graphics2D lid = (Graphics2D) g.create();
        lid.translate(gx + gw, gy + 4);
        lid.rotate(progress * 1.9);
        lid.setColor(Palette.METAL);
        lid.fill(new RoundRectangle2D.Double(-gw - 4, -8, gw + 8, 14, 6, 6));
        lid.setColor(Palette.METAL_LIGHT);
        lid.fill(new RoundRectangle2D.Double(-gw, -6, gw - 8, 3, 2, 2));
        lid.dispose();
    }

    /** Fully open: a lit pad labelled EMPTY or RELEASED. */
    private void drawOpenedInterior(Graphics2D g, int gx, int gy, int gw, int gh) {
        Color pad = empty ? new Color(0x18222E) : new Color(0x1C3A54);
        g.setColor(pad);
        g.fill(new RoundRectangle2D.Double(gx + 4, gy + 4, gw - 8, gh - 8, 4, 4));

        g.setColor(empty ? Palette.TEXT_DIM : Palette.ACCENT_BLUE);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(new RoundRectangle2D.Double(gx + 8, gy + 8, gw - 16, gh - 18, 3, 3));

        g.setFont(LABEL_FONT);
        String text = empty ? "EMPTY" : "RELEASED";
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.setColor(empty ? Palette.TEXT_DIM : Palette.ACCENT_BLUE);
        g.drawString(text, gx + (gw - textWidth) / 2, gy + gh / 2 + 4);
    }

    private void drawStatus(Graphics2D g, double cx) {
        int screenX = x + 12;
        int screenY = y + 9;
        int screenW = width - 24;

        Color screenFill;
        Color textColor;
        String text;
        Color led;

        if (!panel.isPowered()) {
            screenFill = Palette.SCREEN_OFF;
            textColor = Palette.SCREEN_TEXT_OFF;
            text = "OFF";
            led = Palette.LED_OFF;
        } else if (opened) {
            screenFill = Palette.SCREEN_ON;
            textColor = Palette.SCREEN_TEXT_ON;
            text = "OPEN";
            led = Palette.LED_ON;
        } else if (opening) {
            screenFill = Palette.SCREEN_ON;
            textColor = Palette.SCREEN_TEXT_ON;
            text = "---";
            led = new Color(0x7A6A2E);
        } else {
            screenFill = new Color(0x0E3A3C);
            textColor = Palette.SCREEN_TEXT_ON;
            text = "STBY";
            led = (float) (0.5 + 0.5 * Math.sin(time * 3.4)) > 0.35
                    ? Palette.ACCENT_BLUE : new Color(0x215A6E);
        }

        g.setColor(Palette.WALL_EDGE);
        g.fillRect(screenX - 2, screenY - 2, screenW + 4, 10);
        g.setColor(screenFill);
        g.fillRect(screenX, screenY, screenW, 6);
        g.setFont(SCREEN_FONT);
        g.setColor(textColor);
        g.drawString(text, screenX + 2, screenY + 6);

        // indicator LED at the head of the capsule
        g.setColor(led);
        g.fill(new Ellipse2D.Double(cx - 6, y + 14, 7, 7));
        g.setColor(new Color(led.getRed(), led.getGreen(), led.getBlue(), 60));
        g.fill(new Ellipse2D.Double(cx - 10, y + 10, 15, 15));
    }
}