package pl.panzerhund.tracker.scraper.source;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.OtodomProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OtodomScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String BODY = """
            {
              "items": [
                {
                  "id": "ot-1",
                  "title": "Dom z ogrodem",
                  "description": "Ladny dom",
                  "price": {"amount": 850000, "currency": "PLN"},
                  "url": "https://www.otodom.pl/oferta/ot-1",
                  "location": {"city": "Krakow", "region": "malopolskie", "latitude": 50.06, "longitude": 19.94},
                  "attributes": {"area": "120", "rooms": "5"}
                }
              ],
              "page": 0,
              "totalPages": 1
            }
            """;

    private OtodomProperties props;
    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        props = new OtodomProperties();
        props.setEnabled(true);
        props.setBaseUrl(wm.baseUrl());
        props.setSearchPath("/api/listings");
        props.setPageSize(40);

        criteria = new SearchCriteria();
        criteria.setName("Domy");
        criteria.setCategory(Category.PLOT);
        criteria.setFilters(Map.of("region", "malopolskie", "priceMax", 1000000));
    }

    private OtodomScraper scraper(OtodomProperties properties) {
        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0);
        scraperProps.setDelayMaxSeconds(0);
        return new OtodomScraper(properties, scraperProps, RestClient.builder());
    }

    @Test
    void source() {
        assertThat(scraper(props).source()).isEqualTo(Source.OTODOM);
    }

    @Test
    void fetchPageParsesItem() {
        wm.stubFor(get(urlPathEqualTo("/api/listings")).willReturn(okJson(BODY)));

        List<ScrapedListing> result = scraper(props).fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("ot-1");
        assertThat(s.category()).isEqualTo(Category.PLOT);
        assertThat(s.title()).isEqualTo("Dom z ogrodem");
        assertThat(s.price()).isEqualByComparingTo("850000");
        assertThat(s.currency()).isEqualTo("PLN");
        assertThat(s.url()).isEqualTo("https://www.otodom.pl/oferta/ot-1");
        assertThat(s.city()).isEqualTo("Krakow");
        assertThat(s.region()).isEqualTo("malopolskie");
        assertThat(s.lat()).isEqualTo(50.06);
        assertThat(s.lng()).isEqualTo(19.94);
        assertThat(s.attributes()).containsEntry("area", "120").containsEntry("rooms", "5");
    }

    @Test
    void fetchPageSendsPagingSortAndForwardedFilters() {
        wm.stubFor(get(urlPathEqualTo("/api/listings")).willReturn(okJson(BODY)));

        scraper(props).fetchPage(criteria, 1);

        wm.verify(getRequestedFor(urlPathEqualTo("/api/listings"))
                .withQueryParam("page", equalTo("1"))
                .withQueryParam("size", equalTo("40"))
                .withQueryParam("sort", equalTo("newest"))
                .withQueryParam("region", equalTo("malopolskie"))
                .withQueryParam("priceMax", equalTo("1000000")));
    }

    @Test
    void disabledSourceSkipsRequest() {
        props.setEnabled(false);

        List<ScrapedListing> result = scraper(props).fetchPage(criteria, 0);

        assertThat(result).isEmpty();
        wm.verify(0, getRequestedFor(urlPathEqualTo("/api/listings")));
    }
}
