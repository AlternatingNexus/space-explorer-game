package com.spaceexplorer;

/**
 * Translates world coordinates to screen coordinates and keeps the view inside
 * the bounds of the ship, so the player never sees the void outside the hull
 * while the ship is bigger than the window.
 */
public class Camera {

    private int viewWidth;
    private int viewHeight;
    private double x;
    private double y;

    public Camera(int viewWidth, int viewHeight) {
        this.viewWidth = viewWidth;
        this.viewHeight = viewHeight;
    }

    public void setViewportSize(int width, int height) {
        this.viewWidth = width;
        this.viewHeight = height;
    }

    public int getViewWidth() {
        return viewWidth;
    }

    public int getViewHeight() {
        return viewHeight;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void centerOn(double worldX, double worldY, World world) {
        x = worldX - viewWidth / 2.0;
        y = worldY - viewHeight / 2.0;
        clamp(world);
    }

    private void clamp(World world) {
        double maxX = world.getPixelWidth() - viewWidth;
        double maxY = world.getPixelHeight() - viewHeight;
        // A world smaller than the viewport is centred instead of clamped.
        x = maxX <= 0 ? maxX / 2.0 : Math.min(Math.max(0.0, x), maxX);
        y = maxY <= 0 ? maxY / 2.0 : Math.min(Math.max(0.0, y), maxY);
    }

    public double toScreenX(double worldX) {
        return worldX - x;
    }

    public double toScreenY(double worldY) {
        return worldY - y;
    }
}
