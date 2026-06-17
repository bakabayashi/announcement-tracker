package pl.panzerhund.tracker.scraper.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.ScraperRateLimiter;
import pl.panzerhund.tracker.scraper.config.OtodomProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ListingSource} for Otodom (real estate). Otodom is a Next.js site with no public REST API:
 * each results page is HTML embedding a {@code <script id="__NEXT_DATA__" type="application/json">}
 * blob whose {@code props.pageProps.data.searchAds.items[]} holds the listings. We fetch the results
 * page with a browser User-Agent (Otodom 403s other clients), extract that JSON and map the items.
 * <p>
 * Reconciled with Otodom's real structure (item fields: {@code id}, {@code title}, {@code slug} ->
 * {@code /pl/oferta/{slug}}, {@code totalPrice.value/currency}, {@code areaInSquareMeters},
 * {@code location.address}/{@code location.coordinates}). Parsing is null-safe: nested location field
 * names are best-effort and missing values simply map to null. Disabled by default; Otodom also
 * bot-protects by IP, so live use may require a proxy.
 */
@Component
public class OtodomScraper implements ListingSource {

    private static final Logger log = LoggerFactory.getLogger(OtodomScraper.class);

    private final OtodomProperties properties;
    private final ScraperRateLimiter rateLimiter;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OtodomScraper(
            OtodomProperties properties,
            ScraperProperties scraperProperties,
            RestClient.Builder builder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.rateLimiter = new ScraperRateLimiter(
                scraperProperties.getDelayMinSeconds(), scraperProperties.getDelayMaxSeconds());
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml")
                .build();
    }

    @Override
    public Source source() {
        return Source.OTODOM;
    }

    @Override
    public List<ScrapedListing> fetchPage(SearchCriteria criteria, int page) {
        if (!properties.isEnabled()) {
            log.warn("Otodom source disabled; skipping scrape");
            return List.of();
        }

        rateLimiter.pause();
        String html = restClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, criteria, page))
                .retrieve()
                .body(String.class);

        String nextData = extractNextData(html);
        if (nextData == null) {
            log.warn("Otodom: __NEXT_DATA__ not found in response (page {})", page);
            return List.of();
        }

        try {
            JsonNode items = objectMapper.readTree(nextData)
                    .path("props").path("pageProps").path("data").path("searchAds").path("items");
            if (!items.isArray()) {
                return List.of();
            }
            List<ScrapedListing> result = new ArrayList<>();
            for (JsonNode item : items) {
                ScrapedListing listing = toScraped(item, criteria);
                if (listing != null) {
                    result.add(listing);
                }
            }
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Otodom: failed to parse __NEXT_DATA__ JSON", e);
            return List.of();
        }
    }

    private URI buildUri(UriBuilder uriBuilder, SearchCriteria criteria, int page) {
        Map<String, Object> filters = criteria.getFilters() != null ? criteria.getFilters() : Map.of();
        String transaction = str(filters.get("transaction"), properties.getTransaction());
        String propertyType = str(filters.get("propertyType"), properties.getPropertyType());
        // Segments are slugs (e.g. cala-polska); kept raw so multi-segment locations stay path separators.
        String location = str(filters.get("location"), properties.getLocation());

        uriBuilder.path("/pl/wyniki/" + transaction + "/" + propertyType + "/" + location)
                .queryParam("page", page + 1) // Otodom pages are 1-based
                .queryParam("limit", properties.getPageSize())
                .queryParam("by", "LATEST")
                .queryParam("direction", "DESC")
                .queryParam("viewType", "listing");

        addQueryParam(uriBuilder, "priceMin", filters.get("priceMin"));
        addQueryParam(uriBuilder, "priceMax", filters.get("priceMax"));
        return uriBuilder.build();
    }

    private ScrapedListing toScraped(JsonNode item, SearchCriteria criteria) {
        JsonNode id = item.path("id");
        if (id.isMissingNode() || id.isNull()) {
            return null; // skip non-listing entries (e.g. section markers)
        }
        JsonNode location = item.path("location");
        String slug = text(item, "slug");
        return new ScrapedListing(
                id.asText(),
                criteria.getCategory(),
                text(item, "title"),
                null, // search items carry no full description
                dec(item, "totalPrice", "value"),
                text(item, "totalPrice", "currency"),
                slug != null ? properties.getBaseUrl() + "/pl/oferta/" + slug : properties.getBaseUrl(),
                firstNonNull(text(location, "address", "city", "name"), text(location, "address", "city", "code")),
                firstNonNull(text(location, "address", "province", "name"), text(location, "address", "province", "code")),
                dbl(location, "coordinates", "latitude"),
                dbl(location, "coordinates", "longitude"),
                buildAttributes(item));
    }

    private static Map<String, Object> buildAttributes(JsonNode item) {
        Map<String, Object> attributes = new HashMap<>();
        String area = text(item, "areaInSquareMeters");
        if (area != null) {
            attributes.put("area", area);
        }
        return attributes;
    }

    /** Extracts the JSON body of the {@code __NEXT_DATA__} script tag from the HTML. */
    static String extractNextData(String html) {
        if (html == null) {
            return null;
        }
        int marker = html.indexOf("__NEXT_DATA__");
        if (marker < 0) {
            return null;
        }
        int open = html.indexOf('>', marker);
        int close = open >= 0 ? html.indexOf("</script>", open) : -1;
        if (open < 0 || close < 0) {
            return null;
        }
        return html.substring(open + 1, close).trim();
    }

    private static void addQueryParam(UriBuilder uriBuilder, String name, Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }

    private static String str(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static String text(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isValueNode() && !current.isNull() ? current.asText() : null;
    }

    private static Double dbl(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isNumber() ? current.asDouble() : null;
    }

    private static BigDecimal dec(JsonNode node, String... path) {
        JsonNode current = walk(node, path);
        return current.isNumber() ? current.decimalValue() : null;
    }

    private static JsonNode walk(JsonNode node, String... path) {
        JsonNode current = node;
        for (String segment : path) {
            current = current.path(segment);
        }
        return current;
    }
}
