package pl.panzerhund.tracker.scraper.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import pl.panzerhund.tracker.scraper.ScraperRateLimiter;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Base for the HTTP+JSON listing sources (Otodom, Otomoto, Allegro Lokalnie) that share the assumed
 * {@link GenericSearchResponse} contract. Subclasses only declare which {@code Source} they are and
 * supply their configuration. Disabled by default until the live API is reconciled.
 */
public abstract class AbstractRestListingSource implements ListingSource {

    /** Generic filter keys (in SearchCriteria.filters) forwarded as query params as-is.
     *  TODO: real services need a taxonomy translation layer (region/city ids etc.). */
    private static final List<String> FORWARDED_FILTERS = List.of("region", "city", "priceMin", "priceMax");

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final RestClient restClient;
    private final ScraperRateLimiter rateLimiter;
    private final String searchPath;
    private final int pageSize;
    private final boolean enabled;

    protected AbstractRestListingSource(
            String baseUrl,
            String searchPath,
            int pageSize,
            boolean enabled,
            ScraperProperties scraperProperties,
            RestClient.Builder builder) {
        this.searchPath = searchPath;
        this.pageSize = pageSize;
        this.enabled = enabled;
        this.rateLimiter = new ScraperRateLimiter(
                scraperProperties.getDelayMinSeconds(), scraperProperties.getDelayMaxSeconds());
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public List<ScrapedListing> fetchPage(SearchCriteria criteria, int page) {
        if (!enabled) {
            log.warn("{} source disabled; skipping scrape", source());
            return List.of();
        }

        rateLimiter.pause();
        GenericSearchResponse response = restClient
                .get()
                .uri(uriBuilder -> buildUri(uriBuilder, criteria, page))
                .retrieve()
                .body(GenericSearchResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }
        return response.items().stream().map(item -> toScraped(item, criteria)).toList();
    }

    private URI buildUri(UriBuilder uriBuilder, SearchCriteria criteria, int page) {
        uriBuilder.path(searchPath)
                .queryParam("page", page)
                .queryParam("size", pageSize)
                .queryParam("sort", "newest");

        Map<String, Object> filters = criteria.getFilters();
        if (filters != null) {
            for (String key : FORWARDED_FILTERS) {
                Object value = filters.get(key);
                if (value != null) {
                    uriBuilder.queryParam(key, value);
                }
            }
        }
        return uriBuilder.build();
    }

    private ScrapedListing toScraped(GenericSearchResponse.Item item, SearchCriteria criteria) {
        GenericSearchResponse.Price price = item.price();
        GenericSearchResponse.Location location = item.location();
        return new ScrapedListing(
                item.id(),
                criteria.getCategory(),
                item.title(),
                item.description(),
                price != null ? price.amount() : null,
                price != null ? price.currency() : null,
                item.url(),
                location != null ? location.city() : null,
                location != null ? location.region() : null,
                location != null ? location.latitude() : null,
                location != null ? location.longitude() : null,
                item.attributes() != null ? item.attributes() : Map.of());
    }
}
