package pl.panzerhund.tracker.scraper.source;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.AllegroLokalnieProperties;
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

class AllegroLokalnieScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private static final String BODY = """
            {
              "items": [
                {
                  "id": "al-3",
                  "title": "Dzialka rekreacyjna",
                  "price": {"amount": 120000, "currency": "PLN"},
                  "url": "https://allegrolokalnie.pl/oferta/al-3",
                  "location": {"city": "Gdansk", "region": "pomorskie", "latitude": 54.35, "longitude": 18.65},
                  "attributes": {"area": "600"}
                }
              ]
            }
            """;

    private AllegroLokalnieScraper scraper(boolean enabled) {
        AllegroLokalnieProperties props = new AllegroLokalnieProperties();
        props.setEnabled(enabled);
        props.setBaseUrl(wm.baseUrl());
        props.setSearchPath("/api/listings");
        props.setPageSize(40);
        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0);
        scraperProps.setDelayMaxSeconds(0);
        return new AllegroLokalnieScraper(props, scraperProps, RestClient.builder());
    }

    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        criteria = new SearchCriteria();
        criteria.setName("Dzialki");
        criteria.setCategory(Category.PLOT);
        criteria.setFilters(Map.of());
    }

    @Test
    void source() {
        assertThat(scraper(true).source()).isEqualTo(Source.ALLEGRO);
    }

    @Test
    void fetchPageParsesListing() {
        wm.stubFor(get(urlPathEqualTo("/api/listings")).willReturn(okJson(BODY)));

        List<ScrapedListing> result = scraper(true).fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("al-3");
        assertThat(s.category()).isEqualTo(Category.PLOT);
        assertThat(s.city()).isEqualTo("Gdansk");
        assertThat(s.price()).isEqualByComparingTo("120000");
    }

    @Test
    void disabledSourceSkipsRequest() {
        List<ScrapedListing> result = scraper(false).fetchPage(criteria, 0);

        assertThat(result).isEmpty();
        wm.verify(0, getRequestedFor(urlPathEqualTo("/api/listings")));
    }
}
