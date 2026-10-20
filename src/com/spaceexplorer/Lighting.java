package com.spaceexplorer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The lighting layer.
 *
 * <p>How it works, and why it is easy to grow later:</p>
 * <ol>
 *   <li>The world is rendered normally, fully lit.</li>
 *   <li>A separate ARGB buffer is filled with a dark navy colour at a
 *       semi-transparent alpha. The alpha is the "darkness level" and never
 *       reaches 1.0, so the player can always see the ship.</li>
 *   <li>Every {@link Light} in the level punches a soft hole in that buffer
 *       with {@link AlphaComposite#DstOut} and a radial gradient.</li>
 *   <li>The buffer is then blended over the frame, plus a subtle amber tint
 *       while the ship is still on emergency power and a soft vignette.</li>
 * </ol>
 *
 * <p>Restoring the power simply ramps the darkness alpha to zero and flips
 * every light to its powered profile, so a real lighting system (flicker,
 * shadows, powered equipment) only has to be added to {@link Light}.</p>
 */
public class Lighting {

    /** Upper bound of the darkness. Deliberately below 1.0: never a black screen. */
    public static final float MAX_DARKNESS = 0.66f;

    /** Seconds the lights take to come up once the console is used. */
    private static final float RAMP_UP_SECONDS = 0.9f;

    private static final Color DARKNESS_COLOR = new Color(0x060B18);
    private static final int EMERGENCY_TINT_ALPHA = 26;
    private static final int VIGNETTE_ALPHA_DARK = 96;
    private static final int VIGNETTE_ALPHA_LIT = 26;

    private final List<Light> lights = new ArrayList<>();
    private BufferedImage overlay;

    private boolean powerRestored;
    private float lightLevel;
    private float emergencyTint = 1.0f;

    public void addLight(Light light) {
        lights.add(light);
    }

    public List<Light> getLights() {
        return lights;
    }

    public boolean isPowerRestored() {
        return powerRestored;
    }

    public void setPowerRestored(boolean powerRestored) {
        this.powerRestored = powerRestored;
    }

    /** 0 while on emergency power, 1 once the main lights are up. */
    public float getLightLevel() {
        return lightLevel;
    }

    public void update(double dt, double secondsElapsed) {
        float target = powerRestored ? 1.0f : 0.0f;
        float step = (float) dt / RAMP_UP_SECONDS;
        if (lightLevel < target) {
            lightLevel = Math.min(target, lightLevel + step);
        } else if (lightLevel > target) {
            lightLevel = Math.max(target, lightLevel - step);
        }
        emergencyTint = 1.0f - lightLevel;

        for (Light light : lights) {
            light.setPowered(powerRestored);
            light.update(secondsElapsed);
        }
    }

    /** Composites the darkness for one frame on top of an already drawn world. */
    public void render(Graphics2D target, int viewWidth, int viewHeight, Camera camera) {
        int darknessAlpha = Math.round(MAX_DARKNESS * (1.0f - lightLevel) * 255.0f);
        if (darknessAlpha <= 1) {
            return;
        }
        if (overlay == null
                || overlay.getWidth() != viewWidth
                || overlay.getHeight() != viewHeight) {
            overlay = new BufferedImage(viewWidth, viewHeight, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics2D g = overlay.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(DARKNESS_COLOR.getRed(), DARKNESS_COLOR.getGreen(),
                DARKNESS_COLOR.getBlue(), darknessAlpha));
        g.fillRect(0, 0, viewWidth, viewHeight);

        // Punch a hole for every light source.
        g.setComposite(AlphaComposite.DstOut);
        for (Light light : lights) {
            punchLight(g, light, viewWidth, viewHeight, camera);
        }

        // Emergency ambience and a gentle vignette to frame the view.
        g.setComposite(AlphaComposite.SrcOver);
        if (emergencyTint > 0.01f) {
            g.setColor(new Color(255, 132, 64, Math.round(EMERGENCY_TINT_ALPHA * emergencyTint)));
            g.fillRect(0, 0, viewWidth, viewHeight);
        }
        g.setPaint(vignette(viewWidth, viewHeight, lightLevel));
        g.fillRect(0, 0, viewWidth, viewHeight);
        g.dispose();

        target.drawImage(overlay, 0, 0, null);
    }

    private void punchLight(Graphics2D g, Light light, int viewWidth, int viewHeight, Camera camera) {
        double radius = light.getRadius();
        double strength = light.getStrength();
        if (radius <= 1.0 || strength <= 0.01) {
            return;
        }
        double screenX = camera.toScreenX(light.getX());
        double screenY = camera.toScreenY(light.getY());
        if (screenX + radius < 0 || screenX - radius > viewWidth
                || screenY + radius < 0 || screenY - radius > viewHeight) {
            return;
        }

        Color color = light.getColor();
        g.setPaint(new RadialGradientPaint(
                (float) screenX, (float) screenY, (float) radius,
                new float[]{0.0f, 0.45f, 0.75f, 1.0f},
                new Color[]{
                        alpha(color, strength),
                        alpha(color, strength * 0.72f),
                        alpha(color, strength * 0.28f),
                        alpha(color, 0.0f)
                },
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(new Rectangle2D.Double(screenX - radius, screenY - radius, radius * 2.0, radius * 2.0));
    }

    /** Corner darkening, strongest while the ship is unlit and almost gone once lit. */
    private static RadialGradientPaint vignette(int viewWidth, int viewHeight, float lightLevel) {
        float radius = (float) Math.max(viewWidth, viewHeight) * 0.72f;
        int edgeAlpha = Math.round(VIGNETTE_ALPHA_DARK
                + (VIGNETTE_ALPHA_LIT - VIGNETTE_ALPHA_DARK) * lightLevel);
        int midAlpha = Math.round(edgeAlpha * 0.32f);
        return new RadialGradientPaint(
                viewWidth / 2.0f, viewHeight / 2.0f, radius,
                new float[]{0.0f, 0.62f, 1.0f},
                new Color[]{
                        new Color(0, 0, 0, 0),
                        new Color(0, 0, 0, midAlpha),
                        new Color(0, 0, 0, edgeAlpha)
                },
                MultipleGradientPaint.CycleMethod.NO_CYCLE);
    }

    private static Color alpha(Color color, double factor) {
        int a = (int) Math.round(255.0 * Math.max(0.0, Math.min(1.0, factor)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }
}
