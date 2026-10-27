package com.spaceexplorer.dialogue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An ordered collection of dialogue lines spoken in one conversation. */
public final class DialogueConversation {

    private final List<DialogueLine> lines;

    public DialogueConversation(List<DialogueLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A conversation needs at least one line.");
        }
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
    }

    public static DialogueConversation of(DialogueLine... lines) {
        List<DialogueLine> copy = new ArrayList<>();
        if (lines != null) {
            Collections.addAll(copy, lines);
        }
        return new DialogueConversation(copy);
    }

    public int size() {
        return lines.size();
    }

    public DialogueLine get(int index) {
        return lines.get(index);
    }

    public List<DialogueLine> getLines() {
        return lines;
    }
}
