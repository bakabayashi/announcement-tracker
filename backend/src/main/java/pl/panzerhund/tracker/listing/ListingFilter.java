package pl.panzerhund.tracker.listing;

import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;

/** Optional filters for listing search. Null fields are ignored. */
public record ListingFilter(
        Category category,
        Source source,
        ListingStatus status,
        String region,
        String q,
        BigDecimal priceMin,
        BigDecimal priceMax
) {
}
