package pl.panzerhund.tracker.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.dto.PriceStatsResponse;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceStatsServiceTest {

    private ListingService listingService;
    private ListingRepository listingRepository;
    private PriceStatsService service;

    @BeforeEach
    void setUp() {
        listingService = mock(ListingService.class);
        listingRepository = mock(ListingRepository.class);
        service = new PriceStatsService(listingService, listingRepository);
    }

    private UUID stubListing(Category category, String region) {
        UUID id = UUID.randomUUID();
        Listing listing = new Listing();
        listing.setCategory(category);
        listing.setRegion(region);
        when(listingService.getById(id)).thenReturn(listing);
        return id;
    }

    private void stubPrices(BigDecimal... prices) {
        when(listingRepository.findPricesByCategoryAndRegionSince(any(), any(), any()))
                .thenReturn(List.of(prices));
    }

    @Test
    void oddSampleAverageAndMedian() {
        UUID id = stubListing(Category.PLOT, "mazowieckie");
        stubPrices(new BigDecimal("300"), new BigDecimal("100"), new BigDecimal("200"));

        PriceStatsResponse stats = service.forListing(id);

        assertThat(stats.average()).isEqualByComparingTo("200.00");
        assertThat(stats.median()).isEqualByComparingTo("200.00");
        assertThat(stats.sampleSize()).isEqualTo(3);
    }

    @Test
    void evenSampleMedianIsAverageOfTwoMiddle() {
        UUID id = stubListing(Category.CAR, "slaskie");
        stubPrices(new BigDecimal("100"), new BigDecimal("200"),
                new BigDecimal("300"), new BigDecimal("400"));

        PriceStatsResponse stats = service.forListing(id);

        assertThat(stats.average()).isEqualByComparingTo("250.00");
        assertThat(stats.median()).isEqualByComparingTo("250.00");
        assertThat(stats.sampleSize()).isEqualTo(4);
    }

    @Test
    void emptySampleYieldsNullStats() {
        UUID id = stubListing(Category.PLOT, "mazowieckie");
        when(listingRepository.findPricesByCategoryAndRegionSince(eq(Category.PLOT), eq("mazowieckie"), any(Instant.class)))
                .thenReturn(List.of());

        PriceStatsResponse stats = service.forListing(id);

        assertThat(stats.average()).isNull();
        assertThat(stats.median()).isNull();
        assertThat(stats.sampleSize()).isZero();
    }

    @Test
    void nullRegionSkipsQuery() {
        UUID id = stubListing(Category.PLOT, null);

        PriceStatsResponse stats = service.forListing(id);

        assertThat(stats.sampleSize()).isZero();
        verify(listingRepository, never()).findPricesByCategoryAndRegionSince(any(), any(), any());
    }
}
