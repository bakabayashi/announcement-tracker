package pl.panzerhund.tracker.scraper;

import java.time.Instant;
import java.util.UUID;

/**
 * Outcome of upserting a single scraped listing.
 *
 * @param listingId           id of the upserted listing row
 * @param outcome             whether a new listing was inserted or an existing one updated
 * @param priceDropped        whether the price strictly dropped on this update
 * @param previousLastSeenAt  last-seen time before this upsert, or {@code null} for a freshly created listing.
 *                            Lets the orchestrator decide whether the listing was already "known",
 *                            keeping the stop policy out of the persistence layer.
 */
public record UpsertResult(UUID listingId, Outcome outcome, boolean priceDropped, Instant previousLastSeenAt) {

    public enum Outcome { CREATED, UPDATED }

    static UpsertResult created(UUID listingId) {
        return new UpsertResult(listingId, Outcome.CREATED, false, null);
    }

    static UpsertResult updated(UUID listingId, boolean priceDropped, Instant previousLastSeenAt) {
        return new UpsertResult(listingId, Outcome.UPDATED, priceDropped, previousLastSeenAt);
    }

    /** Whether the matched listing had been seen after {@code threshold}, i.e. is already known. */
    boolean wasSeenAfter(Instant threshold) {
        return previousLastSeenAt != null && previousLastSeenAt.isAfter(threshold);
    }
}
