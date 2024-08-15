package dev.firstdark.rpc.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * @author HypherionSA
 * Backoff implementation. Think of it as a ratelimiter for RPC
 */
public class Backoff {

    private final long minAmount;
    private final long maxAmount;
    private final Random random;
    private long current;

    /**
     * Create a new Backoff limiter
     *
     * @param min The minimum amount of time to wait
     * @param max The maximum amount of time to wait
     */
    public Backoff(long min, long max) {
        this.minAmount = min;
        this.maxAmount = max;
        this.random = new SecureRandom();
        this.current = min;
    }

    /**
     * Reset the current backoff
     */
    public void reset() {
        this.current = this.minAmount;
    }

    /**
     * Check when the next action should be executed
     *
     * @return The time value of when to try the action again
     */
    public long nextDelay() {
        long delay = (long) ((double) this.current * 2.0D * this.random.nextDouble());
        this.current = Math.min(this.current + delay, this.maxAmount);
        return this.current;
    }

}
