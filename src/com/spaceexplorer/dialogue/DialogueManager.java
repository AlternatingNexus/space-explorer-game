package com.spaceexplorer.dialogue;

import com.spaceexplorer.core.InputHandler;

/**
 * Reusable, UI-independent dialogue controller.
 *
 * <p>It owns the current conversation, the current line and the typewriter
 * reveal position. The HUD reads those values and draws the box; the game loop
 * calls {@link #update(double, InputHandler)} only while dialogue is open. One
 * press can either finish the current line or move to the next one, never both,
 * because the input layer reports edge-triggered presses only once per frame.</p>
 */
public final class DialogueManager {

    /** Fast enough to feel responsive, slow enough to read as a typewriter. */
    private static final double CHARACTERS_PER_SECOND = 46.0;

    private DialogueConversation conversation;
    private int lineIndex;
    private int revealedCharacters;
    private double revealAccumulator;
    private boolean open;
    private Runnable completionAction;

    public void start(DialogueConversation conversation, Runnable onComplete) {
        if (conversation == null || conversation.size() == 0) {
            return;
        }
        this.conversation = conversation;
        this.lineIndex = 0;
        this.revealedCharacters = 0;
        this.revealAccumulator = 0.0;
        this.open = true;
        this.completionAction = onComplete;
    }

    /**
     * Advances the typewriter and processes at most one advance press.
     *
     * <p>If the current line is still appearing when a press arrives, the press
     * only reveals the rest of that line. The next press moves on.</p>
     */
    public void update(double dt, InputHandler input) {
        if (!open) {
            return;
        }
        DialogueLine line = getCurrentLine();
        if (line == null) {
            return;
        }

        int textLength = line.getText().length();
        boolean completeBeforeUpdate = revealedCharacters >= textLength;

        if (!completeBeforeUpdate) {
            revealAccumulator += Math.max(0.0, dt) * CHARACTERS_PER_SECOND;
            int wholeCharacters = (int) revealAccumulator;
            if (wholeCharacters > 0) {
                revealedCharacters = Math.min(textLength, revealedCharacters + wholeCharacters);
                revealAccumulator -= wholeCharacters;
                if (revealedCharacters >= textLength) {
                    revealAccumulator = 0.0;
                }
            }
        }

        if (input != null && input.isDialogueAdvancePressed()) {
            if (!completeBeforeUpdate) {
                // The first press always finishes the line that is appearing.
                revealedCharacters = textLength;
                revealAccumulator = 0.0;
            } else if (lineIndex + 1 < conversation.size()) {
                lineIndex++;
                revealedCharacters = 0;
                revealAccumulator = 0.0;
            } else {
                close();
            }
        }
    }

    private void close() {
        open = false;
        conversation = null;
        lineIndex = 0;
        revealedCharacters = 0;
        revealAccumulator = 0.0;

        Runnable action = completionAction;
        completionAction = null;
        if (action != null) {
            action.run();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public DialogueLine getCurrentLine() {
        if (!open || conversation == null || lineIndex < 0 || lineIndex >= conversation.size()) {
            return null;
        }
        return conversation.get(lineIndex);
    }

    public String getSpeakerName() {
        DialogueLine line = getCurrentLine();
        return line == null ? "" : line.getSpeaker();
    }

    public String getText() {
        DialogueLine line = getCurrentLine();
        return line == null ? "" : line.getText();
    }

    public String getVisibleText() {
        String text = getText();
        int visible = Math.min(text.length(), Math.max(0, revealedCharacters));
        return text.substring(0, visible);
    }

    public int getRevealedCharacters() {
        return revealedCharacters;
    }

    public boolean isLineComplete() {
        DialogueLine line = getCurrentLine();
        return line == null || revealedCharacters >= line.getText().length();
    }

    public boolean hasNextLine() {
        return open && conversation != null && lineIndex + 1 < conversation.size();
    }

    public int getLineNumber() {
        return lineIndex;
    }
}
