package pl.panzerhund.tracker.cleanup;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.PriceHistoryRepository;
import pl.panzerhund.tracker.listing.entity.ListingStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * TTL maintenance run nightly. Saved listings are always preserved:
 * <ul>
 *   <li>ACTIVE listing unseen for 30 days -> INACTIVE;</li>
 *   <li>INACTIVE listing older than 90 days -> deleted (unless saved);</li>
 *   <li>price history older than 365 days -> deleted (kept for saved listings).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    static final Duration STALE_AFTER = Duration.ofDays(30);
    static final Duration INACTIVE_RETENTION = Duration.ofDays(90);
    static final Duration PRICE_HISTORY_RETENTION = Duration.ofDays(365);

    private final ListingRepository listingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final Clock clock;

    @Transactional
    public void runCleanup() {
        Instant now = clock.instant();

        int deactivated = listingRepository.updateStatusForStale(
                ListingStatus.ACTIVE, ListingStatus.INACTIVE, now.minus(STALE_AFTER));
        int deletedListings = listingRepository.deleteUnsavedByStatusOlderThan(
                ListingStatus.INACTIVE, now.minus(INACTIVE_RETENTION));
        int deletedPriceHistory = priceHistoryRepository.deleteOlderThanUnsaved(
                now.minus(PRICE_HISTORY_RETENTION));

        log.info("Cleanup done: {} deactivated, {} listings deleted, {} price-history rows deleted",
                deactivated, deletedListings, deletedPriceHistory);
    }
}
