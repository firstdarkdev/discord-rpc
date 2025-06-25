package dev.firstdark.rpc.utils;

/**
 * @author HypherionSA
 * Backoff implementation. Think of it as a ratelimiter for RPC
 */
public class Backoff {

    private final long minAmount;
    private final long maxAmount;

    /**
     * Create a new Backoff limiter
     *
     * @param min The minimum amount of time to wait
     * @param max The maximum amount of time to wait
     */
    public Backoff(long min, long max) {
        this.minAmount = min;
        this.maxAmount = max;
    }

    /**
     * Check when the next action should be executed
     *
     * @return The time value of when to try the action again
     */
    public long getDelay(int attempt, int maxAttempts) {
        double progress = Math.min(1.0, (double) attempt / maxAttempts);
        return minAmount + (long)((maxAmount - minAmount) * progress);
    }

    /**
     * Helper Class to format delays into more human-readable time
     *
     * @param millis The amount of time in milliseconds that the delay will take
     * @return The formatted display
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60_000) {
            long seconds = millis / 1000;
            long remainderMs = millis % 1000;
            if (remainderMs == 0) {
                return seconds + " s";
            } else {
                return seconds + "." + (remainderMs / 100) + " s";
            }
        } else {
            long minutes = millis / 60_000;
            long seconds = (millis % 60_000) / 1000;
            if (seconds == 0) {
                return minutes + " m";
            } else {
                return String.format("%d m %d s", minutes, seconds);
            }
        }
    }


}
