package pl.panzerhund.tracker.scraper.source;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Assumed JSON search-response contract shared by the Otodom/Otomoto/Allegro Lokalnie sources.
 * These services are not OLX's documented Partner API; this shape is a placeholder modelled on a
 * typical paginated listings endpoint. TODO: reconcile with each service's real response and split
 * into per-source DTOs where they diverge.
 */
public record GenericSearchResponse(List<Item> items, Integer page, Integer totalPages) {

    public record Item(
            String id,
            String title,
            String description,
            Price price,
            String url,
            Location location,
            Map<String, Object> attributes) {}

    public record Price(BigDecimal amount, String currency) {}

    public record Location(String city, String region, Double latitude, Double longitude) {}
}
