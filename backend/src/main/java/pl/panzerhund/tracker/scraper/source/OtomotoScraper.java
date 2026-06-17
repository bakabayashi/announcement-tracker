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
import pl.panzerhund.tracker.scraper.config.OtomotoProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ListingSource} for Otomoto (cars). Otomoto is a Next.js site without a public REST API: each
 * results page is HTML embedding a {@code __NEXT_DATA__} blob whose
 * {@code props.pageProps.advertSearch.edges[].node} holds the listings (a GraphQL shape). We fetch the
 * results page with a browser User-Agent, extract that JSON and map the nodes.
 * <p>
 * The mechanism and results URL ({@code /osobowe} + {@code search[...]} filters + 1-based {@code page})
 * are reconciled with Otomoto's real site; the exact GraphQL node field names are best-effort (no
 * readable schema source), so parsing is null-safe and missing values map to null. Disabled by default;
 * Otomoto also bot-protects by IP.
 */
@Component
public class OtomotoScraper implements ListingSource {

    private static final Logger log = LoggerFactory.getLogger(OtomotoScraper.class);

    private final OtomotoProperties properties;
    private final ScraperRateLimiter rateLimiter;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OtomotoScraper(
            OtomotoProperties properties,
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
        return Source.OTOMOTO;
    }

    @Override
    public List<ScrapedListing> fetchPage(SearchCriteria criteria, int page) {
        if (!properties.isEnabled()) {
            log.warn("Otomoto source disabled; skipping scrape");
            return List.of();
        }

        rateLimiter.pause();
        String html = restClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, criteria, page))
                .retrieve()
                .body(String.class);

        String nextData = NextData.extract(html);
        if (nextData == null) {
            log.warn("Otomoto: __NEXT_DATA__ not found in response (page {})", page);
            return List.of();
        }

        try {
            JsonNode edges = objectMapper.readTree(nextData)
                    .path("props").path("pageProps").path("advertSearch").path("edges");
            if (!edges.isArray()) {
                return List.of();
            }
            List<ScrapedListing> result = new ArrayList<>();
            for (JsonNode edge : edges) {
                ScrapedListing listing = toScraped(edge.path("node"), criteria);
                if (listing != null) {
                    result.add(listing);
                }
            }
            return result;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("Otomoto: failed to parse __NEXT_DATA__ JSON", e);
            return List.of();
        }
    }

    private URI buildUri(UriBuilder uriBuilder, SearchCriteria criteria, int page) {
        Map<String, Object> filters = criteria.getFilters() != null ? criteria.getFilters() : Map.of();
        String basePath = NextData.str(filters.get("basePath"), properties.getBasePath());
        StringBuilder path = new StringBuilder(basePath);
        appendSegment(path, filters.get("brand"));
        appendSegment(path, filters.get("model"));

        uriBuilder.path(path.toString()).queryParam("page", page + 1);
        addQueryParam(uriBuilder, "search[filter_float_price:from]", filters.get("priceMin"));
        addQueryParam(uriBuilder, "search[filter_float_price:to]", filters.get("priceMax"));
        return uriBuilder.build();
    }

    private static void appendSegment(StringBuilder path, Object value) {
        if (value != null) {
            path.append('/').append(value);
        }
    }

    private ScrapedListing toScraped(JsonNode node, SearchCriteria criteria) {
        JsonNode id = node.path("id");
        if (id.isMissingNode() || id.isNull()) {
            return null;
        }
        JsonNode price = node.path("price").path("amount");
        JsonNode location = node.path("location");
        return new ScrapedListing(
                id.asText(),
                criteria.getCategory(),
                NextData.text(node, "title"),
                null,
                NextData.dec(price, "value"),
                NextData.text(price, "currencyCode"),
                NextData.text(node, "url"),
                NextData.text(location, "city", "name"),
                NextData.text(location, "region", "name"),
                null, // Otomoto search nodes do not expose coordinates
                null,
                parameters(node.path("parameters")));
    }

    /** Otomoto exposes specs as a {@code parameters} array of {key, value, displayValue}. */
    private static Map<String, Object> parameters(JsonNode parameters) {
        Map<String, Object> attributes = new HashMap<>();
        if (parameters.isArray()) {
            for (JsonNode parameter : parameters) {
                String key = NextData.text(parameter, "key");
                String value = NextData.text(parameter, "displayValue");
                if (value == null) {
                    value = NextData.text(parameter, "value");
                }
                if (key != null && value != null) {
                    attributes.put(key, value);
                }
            }
        }
        return attributes;
    }

    private static void addQueryParam(UriBuilder uriBuilder, String name, Object value) {
        if (value != null) {
            uriBuilder.queryParam(name, value);
        }
    }
}
