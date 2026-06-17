package pl.panzerhund.tracker.cleanup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.PriceHistoryRepository;
import pl.panzerhund.tracker.listing.entity.ListingStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-17T05:00:00Z");

    private ListingRepository listingRepository;
    private PriceHistoryRepository priceHistoryRepository;
    private CleanupService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        priceHistoryRepository = mock(PriceHistoryRepository.class);
        service = new CleanupService(listingRepository, priceHistoryRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void deactivatesActiveListingsUnseenForThirtyDays() {
        service.runCleanup();
        verify(listingRepository).updateStatusForStale(
                ListingStatus.ACTIVE, ListingStatus.INACTIVE, NOW.minus(Duration.ofDays(30)));
    }

    @Test
    void deletesInactiveListingsOlderThanNinetyDays() {
        service.runCleanup();
        verify(listingRepository).deleteUnsavedByStatusOlderThan(
                ListingStatus.INACTIVE, NOW.minus(Duration.ofDays(90)));
    }

    @Test
    void deletesPriceHistoryOlderThanOneYear() {
        service.runCleanup();
        verify(priceHistoryRepository).deleteOlderThanUnsaved(NOW.minus(Duration.ofDays(365)));
    }
}
