package pl.panzerhund.tracker.listing.dto;

import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        Source source,
        String externalId,
        Category category,
        String title,
        String description,
        BigDecimal price,
        String currency,
        String url,
        String city,
        String region,
        Double lat,
        Double lng,
        Map<String, Object> attributes,
        ListingStatus status,
        Instant firstSeenAt,
        Instant lastSeenAt
) {
}
