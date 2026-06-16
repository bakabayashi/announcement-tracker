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
import pl.panzerhund.tracker.scraper.ScraperService.ScrapeSummary;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.scraper.source.ListingSource;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;
import pl.panzerhund.tracker.search.SearchCriteriaRepository;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScraperServiceTest {

    private ListingSource source;
    private ListingRepository listings;
    private PriceHistoryRepository priceHistory;
    private SearchCriteriaRepository criteriaRepository;
    private ScraperProperties properties;
    private ScraperService service;

    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        source = mock(ListingSource.class);
        listings = mock(ListingRepository.class);
        priceHistory = mock(PriceHistoryRepository.class);
        criteriaRepository = mock(SearchCriteriaRepository.class);
        properties = new ScraperProperties();

        when(source.source()).thenReturn(Source.OLX);

        service = new ScraperService(List.of(source), criteriaRepository, listings, priceHistory, properties);

        criteria = new SearchCriteria();
        criteria.setName("Plots");
        criteria.setCategory(Category.PLOT);
    }

    private ScrapedListing scraped(String externalId, BigDecimal price) {
        return new ScrapedListing(externalId, Category.PLOT, "Title " + externalId, "desc",
                price, "PLN", "https://olx.pl/" + externalId,
                "Krakow", "malopolskie", null, null, Map.of());
    }

    private Listing stored(String externalId, BigDecimal price, Instant lastSeenAt) {
        Listing l = new Listing();
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
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a", new BigDecimal("100000"))));
        when(source.fetchPage(criteria, 1)).thenReturn(List.of());

        ScrapeSummary summary = service.scrape(criteria);

        assertThat(summary.created()).isEqualTo(1);
        assertThat(summary.updated()).isZero();
        verify(listings).save(any(Listing.class));
        verify(priceHistory).save(any(PriceHistory.class));
    }

    @Test
    void updatesExistingAndDetectsPriceDrop() {
        Listing existing = stored("a", new BigDecimal("100000"), Instant.now().minus(Duration.ofDays(2)));
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(existing));
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a", new BigDecimal("80000"))));
        when(source.fetchPage(criteria, 1)).thenReturn(List.of());

        ScrapeSummary summary = service.scrape(criteria);

        assertThat(summary.created()).isZero();
        assertThat(summary.updated()).isEqualTo(1);
        assertThat(summary.priceDrops()).isEqualTo(1);
        assertThat(existing.getPrice()).isEqualByComparingTo("80000");
        verify(priceHistory).save(any(PriceHistory.class));
    }

    @Test
    void unchangedPriceDoesNotRecordHistory() {
        Listing existing = stored("a", new BigDecimal("100000"), Instant.now().minus(Duration.ofDays(2)));
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(existing));
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a", new BigDecimal("100000"))));
        when(source.fetchPage(criteria, 1)).thenReturn(List.of());

        ScrapeSummary summary = service.scrape(criteria);

        assertThat(summary.updated()).isEqualTo(1);
        assertThat(summary.priceDrops()).isZero();
        verify(priceHistory, never()).save(any(PriceHistory.class));
    }

    @Test
    void stopsPaginationOnKnownFreshListing() {
        Listing fresh = stored("a", new BigDecimal("100000"), Instant.now());
        when(listings.findBySourceAndExternalId(Source.OLX, "a")).thenReturn(Optional.of(fresh));
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a", new BigDecimal("100000"))));

        service.scrape(criteria);

        // Reached a listing seen within 24h on page 0 -> no further pages requested.
        verify(source).fetchPage(criteria, 0);
        verify(source, never()).fetchPage(eq(criteria), eq(1));
    }

    @Test
    void respectsMaxPagesPerCriteria() {
        properties.setMaxPagesPerCriteria(2);
        when(source.fetchPage(eq(criteria), anyInt()))
                .thenAnswer(inv -> List.of(scraped("p" + inv.getArgument(1), new BigDecimal("100000"))));

        service.scrape(criteria);

        verify(source, times(2)).fetchPage(eq(criteria), anyInt());
    }
}
