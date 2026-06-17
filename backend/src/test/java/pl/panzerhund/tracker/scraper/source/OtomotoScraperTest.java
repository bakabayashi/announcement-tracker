package pl.panzerhund.tracker.scraper.source;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.OtomotoProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OtomotoScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String BODY = """
            {
              "items": [
                {
                  "id": "om-7",
                  "title": "Audi A4",
                  "price": {"amount": 65000, "currency": "PLN"},
                  "url": "https://www.otomoto.pl/oferta/om-7",
                  "location": {"city": "Warszawa", "region": "mazowieckie", "latitude": 52.23, "longitude": 21.01},
                  "attributes": {"year": "2018", "mileage": "90000"}
                }
              ]
            }
            """;

    private OtomotoScraper scraper(boolean enabled) {
        OtomotoProperties props = new OtomotoProperties();
        props.setEnabled(enabled);
        props.setBaseUrl(wm.baseUrl());
        props.setSearchPath("/api/listings");
        props.setPageSize(40);
        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0);
        scraperProps.setDelayMaxSeconds(0);
        return new OtomotoScraper(props, scraperProps, RestClient.builder());
    }

    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        criteria = new SearchCriteria();
        criteria.setName("Auta");
        criteria.setCategory(Category.CAR);
        criteria.setFilters(Map.of());
    }

    @Test
    void source() {
        assertThat(scraper(true).source()).isEqualTo(Source.OTOMOTO);
    }

    @Test
    void fetchPageParsesCarListing() {
        wm.stubFor(get(urlPathEqualTo("/api/listings")).willReturn(okJson(BODY)));

        List<ScrapedListing> result = scraper(true).fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("om-7");
        assertThat(s.category()).isEqualTo(Category.CAR);
        assertThat(s.title()).isEqualTo("Audi A4");
        assertThat(s.price()).isEqualByComparingTo("65000");
        assertThat(s.attributes()).containsEntry("year", "2018");
    }

    @Test
    void disabledSourceSkipsRequest() {
        List<ScrapedListing> result = scraper(false).fetchPage(criteria, 0);

        assertThat(result).isEmpty();
        wm.verify(0, getRequestedFor(urlPathEqualTo("/api/listings")));
    }
}
