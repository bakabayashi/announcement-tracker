package pl.panzerhund.tracker.scraper.source;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.OlxProperties;
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

class OlxScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String ADVERTS_BODY = """
            {
              "data": [
                {
                  "id": 12345,
                  "url": "https://www.olx.pl/d/oferta/dzialka-12345",
                  "title": "Dzialka budowlana",
                  "description": "Piekna dzialka",
                  "category_id": 3,
                  "created_time": "2026-06-15T10:00:00+02:00",
                  "params": [
                    {"key": "price", "name": "Cena", "value": {"value": 199000, "currency": "PLN", "label": "199 000 zl"}},
                    {"key": "area", "name": "Powierzchnia", "value": {"value": "800", "label": "800 m2"}}
                  ],
                  "location": {"city": {"name": "Krakow"}, "region": {"name": "malopolskie"}, "district": {"name": "Krowodrza"}},
                  "map": {"lat": 50.06, "lon": 19.94}
                }
              ]
            }
            """;

    private OlxScraper scraper;
    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        OlxProperties props = new OlxProperties();
        props.setBaseUrl(wm.baseUrl());
        props.setAccessToken("test-token");
        props.setPageSize(50);

        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0); // no real sleep in tests
        scraperProps.setDelayMaxSeconds(0);

        scraper = new OlxScraper(props, scraperProps, RestClient.builder());

        criteria = new SearchCriteria();
        criteria.setName("Plots");
        criteria.setCategory(Category.PLOT);
        criteria.setFilters(Map.of("priceMax", 200000, "regionId", 5));
    }

    @Test
    void source() {
        assertThat(scraper.source()).isEqualTo(Source.OLX);
    }

    @Test
    void fetchPageParsesAdvert() {
        wm.stubFor(get(urlPathEqualTo("/adverts")).willReturn(okJson(ADVERTS_BODY)));

        List<ScrapedListing> result = scraper.fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("12345");
        assertThat(s.category()).isEqualTo(Category.PLOT);
        assertThat(s.title()).isEqualTo("Dzialka budowlana");
        assertThat(s.price()).isEqualByComparingTo("199000");
        assertThat(s.currency()).isEqualTo("PLN");
        assertThat(s.url()).isEqualTo("https://www.olx.pl/d/oferta/dzialka-12345");
        assertThat(s.city()).isEqualTo("Krakow");
        assertThat(s.region()).isEqualTo("malopolskie");
        assertThat(s.lat()).isEqualTo(50.06);
        assertThat(s.lng()).isEqualTo(19.94);
        assertThat(s.attributes()).containsEntry("area", "800 m2");
    }

    @Test
    void fetchPageSendsExpectedQueryAndAuth() {
        wm.stubFor(get(urlPathEqualTo("/adverts")).willReturn(okJson(ADVERTS_BODY)));

        scraper.fetchPage(criteria, 2);

        wm.verify(getRequestedFor(urlPathEqualTo("/adverts"))
                .withQueryParam("offset", equalTo("100")) // page 2 * pageSize 50
                .withQueryParam("limit", equalTo("50"))
                .withQueryParam("sort_by", equalTo("created_at:desc"))
                .withQueryParam("filter_float_price:to", equalTo("200000"))
                .withQueryParam("region_id", equalTo("5"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .withHeader("Version", equalTo("2.0")));
    }

    @Test
    void blankTokenSkipsRequest() {
        OlxProperties props = new OlxProperties();
        props.setBaseUrl(wm.baseUrl());
        props.setAccessToken("");
        OlxScraper noToken = new OlxScraper(props, new ScraperProperties(), RestClient.builder());

        List<ScrapedListing> result = noToken.fetchPage(criteria, 0);

        assertThat(result).isEmpty();
        wm.verify(0, getRequestedFor(urlPathEqualTo("/adverts")));
    }
}
