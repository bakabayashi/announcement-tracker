package pl.panzerhund.tracker.scraper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.PriceHistoryRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.PriceHistory;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListingUpserterTest {

    private ListingRepository listings;
    private PriceHistoryRepository priceHistory;
    private ListingUpserter upserter;

    @BeforeEach
    void setUp() {
        listings = mock(ListingRepository.class);
        priceHistory = mock(PriceHistoryRepository.class);
        upserter = new ListingUpserter(listings, priceHistory);
    }

    private ScrapedListing scraped(String externalId, BigDecimal price) {
        return new ScrapedListing(externalId, Category.PLOT, "Title " + externalId, "desc",
                price, "PLN", "https://olx.pl/" + externalId,
                "Krakow", "malopolskie", null, null, Map.of());
    }

    private Listing stored(String externalId, BigDecimal price, Instant lastSeenAt) {
        Listing l = new Listing();
        l.setId(UUID.randomUUID());
        l.setSource(Source.OLX);
        l.setExternalId(externalId);
        l.setCategory(Category.PLOT);
        l.setTitle("old");
        l.setUrl("https://olx.pl/" + externalId);
        l.setStatus(ListingStatus.ACTIVE);
        l.setPrice(price);
        l.setCurrency("PLN");
        l.setLastSeenAt(lastSeenAt);
        return l;
    }

    @Test
    void insertsNewListingAndRecordsInitialPrice() {
        UUID assignedId = UUID.randomUUID();
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.empty());
        when(listings.save(any(Listing.class))).thenAnswer(inv -> {
            Listing saved = inv.getArgument(0);
            saved.setId(assignedId);  // JPA assigns the id on insert
            return saved;
        });

        UpsertResult result = upserter.upsert(Source.OLX, scraped("a", new BigDecimal("100000")));

        assertThat(result.outcome()).isEqualTo(UpsertResult.Outcome.CREATED);
        assertThat(result.listingId()).isEqualTo(assignedId);
        assertThat(result.priceDropped()).isFalse();
        assertThat(result.previousLastSeenAt()).isNull();
        verify(priceHistory).save(any(PriceHistory.class));
    }

    @Test
    void updatesExistingAndDetectsPriceDrop() {
        Instant twoDaysAgo = Instant.now().minus(Duration.ofDays(2));
        Listing existing = stored("a", new BigDecimal("100000"), twoDaysAgo);
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(existing));

        UpsertResult result = upserter.upsert(Source.OLX, scraped("a", new BigDecimal("80000")));

        assertThat(result.outcome()).isEqualTo(UpsertResult.Outcome.UPDATED);
        assertThat(result.listingId()).isEqualTo(existing.getId());
        assertThat(result.priceDropped()).isTrue();
        assertThat(result.previousLastSeenAt()).isEqualTo(twoDaysAgo);
        assertThat(existing.getPrice()).isEqualByComparingTo("80000");
        verify(priceHistory).save(any(PriceHistory.class));
    }

    @Test
    void priceIncreaseRecordsHistoryButIsNotADrop() {
        Listing existing = stored("a", new BigDecimal("100000"), Instant.now().minus(Duration.ofDays(2)));
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(existing));

        UpsertResult result = upserter.upsert(Source.OLX, scraped("a", new BigDecimal("120000")));

        assertThat(result.priceDropped()).isFalse();
        assertThat(existing.getPrice()).isEqualByComparingTo("120000");
        verify(priceHistory).save(any(PriceHistory.class));
    }

    @Test
    void unchangedPriceDoesNotRecordHistory() {
        Listing existing = stored("a", new BigDecimal("100000"), Instant.now().minus(Duration.ofDays(2)));
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(existing));

        UpsertResult result = upserter.upsert(Source.OLX, scraped("a", new BigDecimal("100000")));

        assertThat(result.outcome()).isEqualTo(UpsertResult.Outcome.UPDATED);
        assertThat(result.priceDropped()).isFalse();
        verify(priceHistory, never()).save(any(PriceHistory.class));
    }
}
