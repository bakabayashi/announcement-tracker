package pl.panzerhund.tracker.deduplication.dto;

import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;
import java.util.UUID;

/** Slim listing projection shown inside a duplicate group, enough to compare the pair in the UI. */
public record DuplicateListingResponse(
        UUID listingId,
        Source source,
        String title,
        BigDecimal price,
        String currency,
        String city,
        String region,
        String url,
        ListingStatus status
) {
}
