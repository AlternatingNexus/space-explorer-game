package com.spaceexplorer;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard state for the prototype. Held keys drive movement, while
 * {@link #wasPressed(int)} reports a key that went down during this frame only,
 * which is how the one shot "press E" interaction is triggered.
 */
public class InputHandler {

    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> justPressed = new HashSet<>();

    public void onKeyPressed(KeyEvent event) {
        if (down.add(event.getKeyCode())) {
            justPressed.add(event.getKeyCode());
        }
        event.consume();
    }

    public void onKeyReleased(KeyEvent event) {
        down.remove(event.getKeyCode());
        event.consume();
    }

    public boolean isDown(int keyCode) {
        return down.contains(keyCode);
    }

    public boolean wasPressed(int keyCode) {
        return justPressed.contains(keyCode);
    }

    public void endFrame() {
        justPressed.clear();
    }

    public boolean isLeft() {
        return isDown(KeyEvent.VK_A) || isDown(KeyEvent.VK_LEFT);
    }

    public boolean isRight() {
        return isDown(KeyEvent.VK_D) || isDown(KeyEvent.VK_RIGHT);
    }

    public boolean isUp() {
        return isDown(KeyEvent.VK_W) || isDown(KeyEvent.VK_UP);
    }

    public boolean isDownKey() {
        return isDown(KeyEvent.VK_S) || isDown(KeyEvent.VK_DOWN);
    }

    public boolean isRunning() {
        return isDown(KeyEvent.VK_SHIFT);
    }

    public boolean isInteractPressed() {
        return wasPressed(KeyEvent.VK_E) || wasPressed(KeyEvent.VK_ENTER);
    }
}
