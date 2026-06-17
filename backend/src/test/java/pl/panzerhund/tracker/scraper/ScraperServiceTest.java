package pl.panzerhund.tracker.scraper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.category.entity.Category;
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
    private ListingUpserter upserter;
    private SearchCriteriaRepository criteriaRepository;
    private ScraperProperties properties;
    private ScraperService service;

    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        source = mock(ListingSource.class);
        upserter = mock(ListingUpserter.class);
        criteriaRepository = mock(SearchCriteriaRepository.class);
        properties = new ScraperProperties();

        when(source.source()).thenReturn(Source.OLX);

        service = new ScraperService(List.of(source), criteriaRepository, upserter, properties);

        criteria = new SearchCriteria();
        criteria.setName("Plots");
        criteria.setCategory(Category.PLOT);
    }

    private ScrapedListing scraped(String externalId) {
        return new ScrapedListing(externalId, Category.PLOT, "Title " + externalId, "desc",
                new BigDecimal("100000"), "PLN", "https://olx.pl/" + externalId,
                "Krakow", "malopolskie", null, null, Map.of());
    }

    @Test
    void countsCreatedAndStopsWhenPageEmpty() {
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a")));
        when(source.fetchPage(criteria, 1)).thenReturn(List.of());
        when(upserter.upsert(eq(Source.OLX), any())).thenReturn(UpsertResult.created());

        ScrapeSummary summary = service.scrape(criteria);

        assertThat(summary.created()).isEqualTo(1);
        assertThat(summary.updated()).isZero();
        verify(upserter).upsert(eq(Source.OLX), any());
    }

    @Test
    void countsUpdatedAndPriceDrop() {
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a")));
        when(source.fetchPage(criteria, 1)).thenReturn(List.of());
        Instant twoDaysAgo = Instant.now().minus(Duration.ofDays(2));
        when(upserter.upsert(eq(Source.OLX), any())).thenReturn(UpsertResult.updated(true, twoDaysAgo));

        ScrapeSummary summary = service.scrape(criteria);

        assertThat(summary.created()).isZero();
        assertThat(summary.updated()).isEqualTo(1);
        assertThat(summary.priceDrops()).isEqualTo(1);
    }

    @Test
    void stopsPaginationOnKnownFreshListing() {
        when(source.fetchPage(criteria, 0)).thenReturn(List.of(scraped("a")));
        when(upserter.upsert(eq(Source.OLX), any()))
                .thenReturn(UpsertResult.updated(false, Instant.now())); // seen within 24h

        service.scrape(criteria);

        // Reached a listing seen within 24h on page 0 -> no further pages requested.
        verify(source).fetchPage(criteria, 0);
        verify(source, never()).fetchPage(eq(criteria), eq(1));
    }

    @Test
    void respectsMaxPagesPerCriteria() {
        properties.setMaxPagesPerCriteria(2);
        when(source.fetchPage(eq(criteria), anyInt()))
                .thenAnswer(inv -> List.of(scraped("p" + inv.getArgument(1))));
        when(upserter.upsert(eq(Source.OLX), any())).thenReturn(UpsertResult.created());

        service.scrape(criteria);

        verify(source, times(2)).fetchPage(eq(criteria), anyInt());
    }
}
