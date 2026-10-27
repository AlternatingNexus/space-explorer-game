package com.spaceexplorer.dialogue;

/**
 * One line of dialogue: who is speaking and what they say.
 *
 * <p>Lines are intentionally immutable and tiny. A conversation is just an
 * ordered list of these, so new NPC conversations do not need their own
 * rendering or input code.</p>
 */
public final class DialogueLine {

    private final String speaker;
    private final String text;

    public DialogueLine(String speaker, String text) {
        this.speaker = speaker == null ? "" : speaker;
        this.text = text == null ? "" : text;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }
}
