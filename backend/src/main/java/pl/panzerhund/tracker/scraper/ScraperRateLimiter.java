package pl.panzerhund.tracker.scraper;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Throttles requests to a single external service with a randomized delay.
 * One instance per source (held by the source bean), so services are rate-limited independently.
 */
public class ScraperRateLimiter {

    private final long minMillis;
    private final long maxMillis;

    public ScraperRateLimiter(int minSeconds, int maxSeconds) {
        this.minMillis = minSeconds * 1000L;
        this.maxMillis = maxSeconds * 1000L;
    }

    /** Sleep for a random duration within the configured bounds. No-op when both bounds are zero. */
    public void pause() {
        if (maxMillis <= 0) {
            return;
        }
        long millis = ThreadLocalRandom.current().nextLong(minMillis, maxMillis + 1);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
