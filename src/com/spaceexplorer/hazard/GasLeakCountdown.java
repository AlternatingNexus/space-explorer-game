package com.spaceexplorer.hazard;

/**
 * The ninety second failure window that opens once the gas leak becomes the
 * active objective and only closes for good when the pipe is sealed.
 *
 * <p>The remaining time is derived from wall clock nanoseconds instead of the
 * frame delta, so a stalled or unfocused window cannot quietly pause the
 * countdown: the very next tick catches the clock back up. It is deliberately
 * a plain state holder - {@link Game} owns the policy of when to arm it.</p>
 */
public final class GasLeakCountdown {

    public static final double DURATION_SECONDS = 90.0;

    private enum Phase {
        /** Not started: the gas leak objective has not begun yet. */
        DORMANT,
        /** Ticking down towards the failure. */
        RUNNING,
        /** The pipe was sealed in time; the countdown is permanently off. */
        SEALED,
        /** Time ran out; the failure has been triggered. */
        EXPIRED
    }

    private Phase phase = Phase.DORMANT;
    private long startNanos;
    private double remainingSeconds = DURATION_SECONDS;

    /** Arms the countdown the first time; later calls are ignored. */
    public void start(long nowNanos) {
        if (phase != Phase.DORMANT) {
            return;
        }
        phase = Phase.RUNNING;
        startNanos = nowNanos;
        remainingSeconds = DURATION_SECONDS;
    }

    /** Advances the wall clock; only meaningful while running. */
    public void update(long nowNanos) {
        if (phase != Phase.RUNNING) {
            return;
        }
        remainingSeconds = DURATION_SECONDS - (nowNanos - startNanos) / 1_000_000_000.0;
        if (remainingSeconds <= 0.0) {
            remainingSeconds = 0.0;
            phase = Phase.EXPIRED;
        }
    }

    /** Stops the countdown permanently after a successful repair. */
    public void seal() {
        if (phase == Phase.DORMANT || phase == Phase.RUNNING) {
            phase = Phase.SEALED;
        }
    }

    /** True once the timer has been armed at all. */
    public boolean hasStarted() {
        return phase != Phase.DORMANT;
    }

    public boolean isRunning() {
        return phase == Phase.RUNNING;
    }

    public boolean isSealed() {
        return phase == Phase.SEALED;
    }

    public boolean isExpired() {
        return phase == Phase.EXPIRED;
    }

    /** Whether the timer card should be on screen right now. */
    public boolean isVisible() {
        return phase == Phase.RUNNING || phase == Phase.EXPIRED;
    }

    public double getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * The remaining time as {@code MM:SS}, rounded up so the readout only ever
     * shows {@code 00:00} at the very end of the window.
     */
    public String getRemainingText() {
        int totalSeconds = (int) Math.ceil(Math.max(0.0, remainingSeconds));
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
