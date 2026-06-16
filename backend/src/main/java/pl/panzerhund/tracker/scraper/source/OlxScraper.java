package pl.panzerhund.tracker.scraper.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.ScraperRateLimiter;
import pl.panzerhund.tracker.scraper.config.OlxProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link ListingSource} for the OLX Partner REST API ({@code GET /adverts}). */
@Component
public class OlxScraper implements ListingSource {

    private static final Logger log = LoggerFactory.getLogger(OlxScraper.class);

    private static final String PRICE_PARAM_KEY = "price";

    /**
     * Maps our generic filter keys (in SearchCriteria.filters JSONB) to OLX query parameters.
     * TODO: OLX expects numeric category_id/region_id/city_id; a translation layer from our
     *       generic filter values to OLX taxonomy IDs is still required for real queries.
     */
    private static final Map<String, String> FILTER_PARAM_MAP = Map.of(
            "categoryId", "category_id",
            "regionId", "region_id",
            "cityId", "city_id",
            "priceMin", "filter_float_price:from",
            "priceMax", "filter_float_price:to");

    private final OlxProperties properties;
    private final ScraperRateLimiter rateLimiter;
    private final RestClient restClient;

    public OlxScraper(OlxProperties properties, ScraperProperties scraperProperties, RestClient.Builder builder) {
        this.properties = properties;
        this.rateLimiter = new ScraperRateLimiter(
                scraperProperties.getDelayMinSeconds(), scraperProperties.getDelayMaxSeconds());
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Version", properties.getApiVersion())
                .build();
    }

    @Override
    public Source source() {
        return Source.OLX;
    }

    @Override
    public List<ScrapedListing> fetchPage(SearchCriteria criteria, int page) {
        if (properties.getAccessToken().isBlank()) {
            log.warn("OLX access token not configured; skipping OLX scrape");
            return List.of();
        }

        rateLimiter.pause();
        int limit = properties.getPageSize();
        int offset = page * limit;

        OlxResponse response = restClient.get()
                .uri(uriBuilder -> buildUri(uriBuilder, criteria, offset, limit))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .retrieve()
                .body(OlxResponse.class);

        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream().map(advert -> toScraped(advert, criteria)).toList();
    }

    private java.net.URI buildUri(UriBuilder uriBuilder, SearchCriteria criteria, int offset, int limit) {
        uriBuilder.path("/adverts")
                .queryParam("offset", offset)
                .queryParam("limit", limit)
                .queryParam("sort_by", "created_at:desc");

        Map<String, Object> filters = criteria.getFilters();
        if (filters != null) {
            FILTER_PARAM_MAP.forEach((filterKey, paramName) -> {
                Object value = filters.get(filterKey);
                if (value != null) {
                    uriBuilder.queryParam(paramName, value);
                }
            });
        }
        return uriBuilder.build();
    }

    private ScrapedListing toScraped(OlxResponse.Advert advert, SearchCriteria criteria) {
        BigDecimal price = null;
        String currency = null;
        Map<String, Object> attributes = new HashMap<>();

        List<OlxResponse.Param> params = advert.params() != null ? advert.params() : List.of();
        for (OlxResponse.Param param : params) {
            if (param.value() == null) {
                continue;
            }
            if (PRICE_PARAM_KEY.equals(param.key())) {
                price = parsePrice(param.value().value());
                currency = param.value().currency();
            } else {
                attributes.put(param.key(), param.value().label());
            }
        }

        OlxResponse.Location location = advert.location();
        OlxResponse.MapPoint map = advert.map();

        return new ScrapedListing(
                String.valueOf(advert.id()),
                criteria.getCategory(),
                advert.title(),
                advert.description(),
                price,
                currency,
                advert.url(),
                named(location != null ? location.city() : null),
                named(location != null ? location.region() : null),
                map != null ? map.lat() : null,
                map != null ? map.lng() : null,
                attributes);
    }

    private static String named(OlxResponse.Named named) {
        return named != null ? named.name() : null;
    }

    private static BigDecimal parsePrice(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
