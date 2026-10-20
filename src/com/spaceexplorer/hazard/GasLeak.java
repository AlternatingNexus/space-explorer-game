package com.spaceexplorer.hazard;

import com.spaceexplorer.core.Palette;
import com.spaceexplorer.entity.Player;
import com.spaceexplorer.interaction.Inventory;
import com.spaceexplorer.interaction.RepairPartType;
import com.spaceexplorer.world.Light;
import com.spaceexplorer.world.Prop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The third room's hazard: a ruptured coolant line venting gas into the
 * compartment.
 *
 * <p>The player cannot patch it with what the room offers; they must bring two
 * carried components fetched from the maintenance and storage decks. A
 * successful press consumes both parts, starts a short sealing animation (gas
 * stops, then particles and the warning light fade), and switches the room from
 * hazardous to safe. The leak is a {@link Hazard}, so it also notices the
 * player entering the room and hands out the objective.</p>
 */
public final class GasLeak extends Prop implements Hazard {

    public static final List<RepairPartType> REQUIRED_PARTS = Collections.unmodifiableList(
            Arrays.asList(RepairPartType.PIPE_CLAMP, RepairPartType.SEALING_COMPOUND));

    public static final String STATUS_DETECTED = "GAS LEAK DETECTED";
    public static final String PROMPT_REPAIR = "Press E to make emergency repair";
    public static final String PROMPT_BLOCKED_PREFIX = "Cannot repair leak. Missing: ";
    public static final String PROMPT_SEALING = "Sealing the pipe...";
    public static final String MESSAGE_DISCOVERED = "Gas leak detected. Find something to seal the pipe.";
    public static final String MESSAGE_START = "Emergency repair successful.";
    public static final String MESSAGE_CONTAINED = "Gas leak contained.";

    private static final double INTERACT_RANGE = 120.0;
    private static final double DISCOVER_RANGE = 300.0;
    private static final double SEAL_SECONDS = 1.3;
    private static final int PARTICLE_COUNT = 36;

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 9);

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random(90210);
    private final Light hazardLight;
    private final double interactX;
    private final double interactY;

    private boolean discovered;
    private boolean sealing;
    private boolean contained;
    private double sealTimer;
    private double time;

    public GasLeak(int tileX, int tileY) {
        super(tileX, tileY, 2, 2, Layer.OBJECT);
        this.interactX = x + width / 2.0;
        this.interactY = y + height + 12.0;
        this.hazardLight = new Light(x + width / 2.0, y + height * 0.55,
                150.0, 190.0,
                Palette.LED_EMERGENCY,
                new Color(0xFFB37A),
                0.7);
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle());
        }
    }

    public boolean isDiscovered() {
        return discovered;
    }

    @Override
    public boolean isContained() {
        return contained;
    }

    @Override
    public Light getLight() {
        return hazardLight;
    }

    /* ---------- Hazard: per frame ---------- */

    @Override
    public void updateHazard(double dt, Player player) {
        time += dt;

        if (!discovered && distanceToPlayer(player) <= DISCOVER_RANGE) {
            discovered = true;
            player.say(MESSAGE_DISCOVERED, 5.0);
        }

        if (sealing) {
            sealTimer += dt;
            if (sealTimer >= SEAL_SECONDS) {
                sealing = false;
                contained = true;
                hazardLight.setActive(false);
                player.say(MESSAGE_CONTAINED, 4.5);
            }
        }

        updateParticles(dt);
    }

    /* ---------- Interactable ---------- */

    @Override
    public boolean isInRange(Player player) {
        return distanceToPlayer(player) <= INTERACT_RANGE;
    }

    @Override
    public boolean isActionReady(Player player, Inventory inventory) {
        return !sealing && !contained && inventory.hasAll(REQUIRED_PARTS);
    }

    @Override
    public String getStatusText(Player player, Inventory inventory) {
        return discovered && !contained ? STATUS_DETECTED : null;
    }

    @Override
    public String getActionPrompt(Player player, Inventory inventory) {
        if (contained || !discovered) {
            return null;
        }
        if (sealing) {
            return PROMPT_SEALING;
        }
        if (inventory.hasAll(REQUIRED_PARTS)) {
            return PROMPT_REPAIR;
        }
        return PROMPT_BLOCKED_PREFIX + missingParts(inventory);
    }

    /**
     * Faithfully mirrors the prompt text so the HUD and the repair share one
     * source of truth: whichever E press the player is being offered, the
     * result is the same.
     */
    @Override
    public boolean interact(Player player, Inventory inventory) {
        if (sealing || contained) {
            return false;
        }
        if (!inventory.hasAll(REQUIRED_PARTS)) {
            player.say(getActionPrompt(player, inventory), 3.0);
            return false;
        }
        inventory.removeAll(REQUIRED_PARTS);
        sealing = true;
        sealTimer = 0.0;
        player.say(MESSAGE_START, 3.5);
        return true;
    }

    /* ---------- particles ---------- */

    private void updateParticles(double dt) {
        double leakX = x + width * 0.5;
        double leakY = y + height * 0.42;
        for (Particle particle : particles) {
            if (particle.life <= 0.0) {
                if (!sealing && !contained) {
                    particle.respawn(random, leakX, leakY, time);
                }
                continue;
            }
            particle.life -= dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vy -= 7.0 * dt;
            particle.vx += Math.sin(time * 1.8 + particle.seed) * 5.0 * dt;
            if (Math.abs(particle.vx) > 6.0) {
                particle.vx *= 0.9;
            }
        }
    }

    private void drawGas(Graphics2D g) {
        for (Particle particle : particles) {
            if (particle.life <= 0.0) {
                continue;
            }
            double age = 1.0 - particle.life / particle.maxLife;
            double radius = particle.size * (0.6 + age * 1.6);
            int alpha = (int) Math.round(160.0
                    * Math.min(1.0, age * 3.0)
                    * Math.min(1.0, (1.0 - age) * 2.0));

            g.setColor(withAlpha(Palette.GAS, alpha));
            g.fill(new Ellipse2D.Double(particle.x - radius, particle.y - radius,
                    radius * 2.0, radius * 2.0));
            g.setColor(withAlpha(new Color(0xE6FFF0), Math.round(alpha * 0.35f)));
            g.fill(new Ellipse2D.Double(particle.x - radius * 0.45, particle.y - radius * 0.45,
                    radius * 0.9, radius * 0.9));
        }
    }

    /* ---------- drawing ---------- */

    @Override
    public void draw(Graphics2D g) {
        double pipeCx = x + width * 0.5;
        double pipeY = y + height * 0.40;
        double crackHalf = sealProgress() >= 1.0 ? 0.0 : 7.0;

        // hazard haze tint baked straight into the prop so the gas reads in the dark
        if (!contained && (int) (time * 6.0) % 2 == 0) {
            g.setColor(withAlpha(Palette.GAS, 26));
            g.fillRect(x - 16, y - 30, width + 32, height + 46);
        }

        drawPipeBody(g, pipeCx, pipeY, crackHalf);
        drawValve(g, pipeCx, pipeY);
        drawGauge(g, pipeCx, pipeY);
        drawGas(g);
        drawWarningLed(g, pipeCx, pipeY);
        drawHazardBase(g);
        if (sealProgress() > 0.0) {
            drawPatch(g, pipeCx, pipeY, crackHalf);
        }
    }

    private void drawPipeBody(Graphics2D g, double pipeCx, double pipeY, double crackHalf) {
        int thickness = 15;
        int yTop = (int) (pipeY - thickness / 2.0);

        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 2, yTop + thickness - 5, width - 4, 6, 3, 3);

        g.setColor(Palette.METAL_DARK);
        g.fillRect(x, yTop, width, thickness);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRect(x, yTop + 3, width, 6);
        g.setColor(Palette.METAL_DARK);
        g.fillRect(x, yTop, width, 2);

        // hazard band around the damaged stretch
        g.setColor(Palette.HAZARD);
        for (int stripe = x; stripe < x + width; stripe += 10) {
            if (stripe > x + width * 0.28 && stripe < x + width * 0.66) {
                continue;
            }
            g.fillRect(stripe + (stripe % 20 == 0 ? 5 : 0), yTop + 2, 5, thickness - 4);
        }

        // jagged crack carved into the pipe, or a clean ridge once sealed
        Graphics2D clip = (Graphics2D) g.create();
        clip.clip(new Rectangle2D.Double(
                pipeCx - crackHalf, yTop, crackHalf * 2.0, thickness));
        g.setColor(Palette.WALL_EDGE);
        g.fillRect((int) (pipeCx - crackHalf), yTop, (int) (crackHalf * 2.0), thickness);
        g.setColor(new Color(0x0B1220));
        g.setStroke(new BasicStroke(2f));
        g.drawLine((int) pipeCx - 3, yTop, (int) pipeCx + 2, yTop + 5);
        g.drawLine((int) pipeCx + 2, yTop + 5, (int) pipeCx - 2, yTop + 10);
        g.drawLine((int) pipeCx - 2, yTop + 10, (int) pipeCx + 4, yTop + thickness);
        clip.dispose();
    }

    private void drawValve(Graphics2D g, double pipeCx, double pipeY) {
        int wheelCx = (int) (pipeCx - width * 0.28);
        int wheelCy = (int) (pipeY - 10);

        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Palette.METAL);
        g.drawLine(wheelCx, wheelCy, wheelCx, (int) (pipeY - 3));
        g.setColor(Palette.METAL_LIGHT);
        for (int spoke = 0; spoke < 4; spoke++) {
            double angle = time * 0.4 + spoke * Math.PI / 2;
            g.drawLine(wheelCx, wheelCy,
                    wheelCx + (int) (Math.cos(angle) * 7),
                    wheelCy + (int) (Math.sin(angle) * 7));
        }
        g.setColor(Palette.METAL_DARK);
        g.fill(new Ellipse2D.Double(wheelCx - 3, wheelCy - 3, 6, 6));
    }

    private void drawGauge(Graphics2D g, double pipeCx, double pipeY) {
        int gaugeCx = (int) (pipeCx + width * 0.30);
        int gaugeCy = (int) (pipeY - 15);
        int gaugeR = 9;

        double needleAngle = contained ? Math.PI / 4.0 : -Math.PI / 4.0 + Math.sin(time * 2.0) * 0.18;
        g.setColor(Palette.WALL_EDGE);
        g.fill(new Ellipse2D.Double(gaugeCx - gaugeR - 2, gaugeCy - gaugeR - 2, gaugeR * 2 + 4, gaugeR * 2 + 4));
        g.setColor(contained ? new Color(0x1C3B28) : new Color(0x3A1B1B));
        g.fill(new Ellipse2D.Double(gaugeCx - gaugeR, gaugeCy - gaugeR, gaugeR * 2, gaugeR * 2));

        // green / red zones
        g.setStroke(new BasicStroke(2f));
        g.setColor(withAlpha(Palette.LED_ON, 150));
        g.drawArc(gaugeCx - gaugeR + 2, gaugeCy - gaugeR + 3, gaugeR * 2 - 4, gaugeR * 2 - 6,
                -45, 45);
        g.setColor(withAlpha(Palette.LED_EMERGENCY, 180));
        g.drawArc(gaugeCx - gaugeR + 2, gaugeCy - gaugeR + 3, gaugeR * 2 - 4, gaugeR * 2 - 6,
                100, 80);

        g.setColor(Palette.METAL_LIGHT);
        g.setStroke(new BasicStroke(1.6f));
        g.drawLine(gaugeCx, gaugeCy,
                gaugeCx + (int) (Math.cos(needleAngle) * (gaugeR - 2)),
                gaugeCy + (int) (Math.sin(needleAngle) * (gaugeR - 2)));
        g.fill(new Ellipse2D.Double(gaugeCx - 1.5, gaugeCy - 1.5, 3, 3));
    }

    private void drawWarningLed(Graphics2D g, double pipeCx, double pipeY) {
        double blink = 0.5 + 0.5 * Math.sin(time * 6.0);
        Color led = contained ? Palette.LED_ON : Palette.LED_EMERGENCY;
        int ledCx = (int) (pipeCx - width * 0.34);
        int ledCy = (int) (pipeY + 10);

        g.setColor(withAlpha(led, (int) Math.round(70 + 140 * blink)));
        g.fill(new Ellipse2D.Double(ledCx - 7, ledCy - 7, 14, 14));
        g.setColor(withAlpha(led, (int) Math.round(120 + 135 * blink)));
        g.fill(new Ellipse2D.Double(ledCx - 4, ledCy - 4, 8, 8));
    }

    private void drawHazardBase(Graphics2D g) {
        g.setColor(Palette.HAZARD);
        for (int stripe = x + 5; stripe < x + width - 18; stripe += 9) {
            g.fillRect(stripe, y + height - 7, 5, 3);
        }
        if (!contained) {
            g.setColor(withAlpha(Palette.LED_EMERGENCY, 120 + (int) (80 * Math.sin(time * 5.0))));
            g.fillRoundRect(x + width - 19, y + height - 20, 10, 9, 3, 3);
            g.setColor(Palette.SCREEN_TEXT_OFF);
            g.setFont(LABEL_FONT);
            g.drawString("!", x + width - 16, y + height - 13);
        }
    }

    /** Clamp band plus compound patch, growing as the sealing animation runs. */
    private void drawPatch(Graphics2D g, double pipeCx, double pipeY, double crackHalf) {
        double progress = sealProgress();
        int yTop = (int) (pipeY - 7.5);

        // compound blob creeping over the crack
        g.setColor(withAlpha(Palette.GAS, Math.round(220)));
        g.fill(new RoundRectangle2D.Double(pipeCx - crackHalf - 3, yTop - 3,
                crackHalf * 2.0 + 6, 21, 6, 6));
        g.setColor(withAlpha(new Color(0x2E4A3A), 200));
        g.fill(new RoundRectangle2D.Double(pipeCx - crackHalf - 1, yTop - 1,
                crackHalf * 2.0 + 2, 17, 5, 5));

        // clamp band being tightened over it
        int bandWidth = (int) Math.round(4 + 14 * progress);
        int bandX = (int) (pipeCx - bandWidth / 2.0);
        g.setColor(Palette.METAL_LIGHT);
        g.fillRoundRect(bandX, yTop - 4, bandWidth, 26, 5, 5);
        g.setColor(Palette.WALL_EDGE);
        g.setStroke(new BasicStroke(1.6f));
        g.drawRoundRect(bandX, yTop - 4, bandWidth, 26, 5, 5);
        g.fillRect(bandX + bandWidth / 2 - 2, yTop - 7, 4, 5);
    }

    /** 0 while breaking, ramps to 1 during the sealing animation, then 1. */
    private double sealProgress() {
        if (contained) {
            return 1.0;
        }
        if (!sealing) {
            return 0.0;
        }
        return Math.min(1.0, sealTimer / SEAL_SECONDS);
    }

    private String missingParts(Inventory inventory) {
        StringBuilder missing = new StringBuilder();
        for (RepairPartType part : REQUIRED_PARTS) {
            if (!inventory.has(part)) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(part.getDisplayName());
            }
        }
        return missing.toString();
    }

    private double distanceToPlayer(Player player) {
        double dx = player.getCenterX() - interactX;
        double dy = player.getCenterY() - interactY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** One wisp of gas, drifting up and expanding while it fades. */
    private static final class Particle {

        double x;
        double y;
        double vx;
        double vy;
        double life;
        double maxLife;
        double size;
        double seed;

        void respawn(Random random, double leakX, double leakY, double now) {
            double angle = (random.nextDouble() - 0.5) * 1.9 - Math.PI / 2.0;
            double speed = 7.0 + random.nextDouble() * 14.0;
            this.x = leakX + (random.nextDouble() - 0.5) * 8.0;
            this.y = leakY + (random.nextDouble() - 0.5) * 2.0;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed * 0.6;
            this.maxLife = 1.8 + random.nextDouble() * 2.7;
            this.life = maxLife;
            this.size = 2.5 + random.nextDouble() * 4.5;
            this.seed = now + random.nextDouble() * 6.28;
        }
    }
}