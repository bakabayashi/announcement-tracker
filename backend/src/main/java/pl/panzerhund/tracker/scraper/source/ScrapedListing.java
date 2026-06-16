package pl.panzerhund.tracker.scraper.source;

import pl.panzerhund.tracker.category.entity.Category;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Raw listing scraped from an external source, before it is upserted into a {@code Listing}.
 * The owning {@link ListingSource} supplies the {@code Source}; everything else comes from the service response.
 */
public record ScrapedListing(
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
        Map<String, Object> attributes
) {
}
