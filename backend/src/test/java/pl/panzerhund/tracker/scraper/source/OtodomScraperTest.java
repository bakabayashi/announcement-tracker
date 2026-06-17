package pl.panzerhund.tracker.scraper.source;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OtodomScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Minimal Next.js page: the __NEXT_DATA__ script holds props.pageProps.data.searchAds.items[].
    private static final String HTML = """
            <!doctype html><html><body>
            <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"data":{"searchAds":{"items":[
            {"id":98765,"title":"Dzialka budowlana","slug":"dzialka-budowlana-ID98765",
             "totalPrice":{"value":199000,"currency":"PLN"},
             "areaInSquareMeters":800,
             "location":{"address":{"city":{"name":"Krakow"},"province":{"name":"malopolskie"}},
                         "coordinates":{"latitude":50.06,"longitude":19.94}}}
            ]}}}}}</script>
            </body></html>
            """;

    private static final String RESULTS_PATH = "/pl/wyniki/sprzedaz/dzialka/cala-polska";

    private OtodomProperties props;
    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        props = new OtodomProperties();
        props.setEnabled(true);
        props.setBaseUrl(wm.baseUrl());

        criteria = new SearchCriteria();
        criteria.setName("Dzialki");
        criteria.setCategory(Category.PLOT);
        criteria.setFilters(Map.of("priceMax", 200000));
    }

    private OtodomScraper scraper(OtodomProperties properties) {
        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0);
        scraperProps.setDelayMaxSeconds(0);
        return new OtodomScraper(properties, scraperProps, RestClient.builder(), new ObjectMapper());
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder htmlPage() {
        return aResponse().withStatus(200).withHeader("Content-Type", "text/html; charset=utf-8").withBody(HTML);
    }

    @Test
    void source() {
        assertThat(scraper(props).source()).isEqualTo(Source.OTODOM);
    }

    @Test
    void fetchPageParsesItemFromNextData() {
        wm.stubFor(get(urlPathEqualTo(RESULTS_PATH)).willReturn(htmlPage()));

        List<ScrapedListing> result = scraper(props).fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("98765");
        assertThat(s.category()).isEqualTo(Category.PLOT);
        assertThat(s.title()).isEqualTo("Dzialka budowlana");
        assertThat(s.price()).isEqualByComparingTo("199000");
        assertThat(s.currency()).isEqualTo("PLN");
        assertThat(s.url()).isEqualTo(wm.baseUrl() + "/pl/oferta/dzialka-budowlana-ID98765");
        assertThat(s.city()).isEqualTo("Krakow");
        assertThat(s.region()).isEqualTo("malopolskie");
        assertThat(s.lat()).isEqualTo(50.06);
        assertThat(s.lng()).isEqualTo(19.94);
        assertThat(s.attributes()).containsEntry("area", "800");
    }

    @Test
    void fetchPageSendsResultsUrlPagingAndBrowserUserAgent() {
        wm.stubFor(get(urlPathEqualTo(RESULTS_PATH)).willReturn(htmlPage()));

        scraper(props).fetchPage(criteria, 0);

        wm.verify(getRequestedFor(urlPathEqualTo(RESULTS_PATH))
                .withQueryParam("page", equalTo("1")) // 0-based input -> 1-based Otodom page
                .withQueryParam("limit", equalTo("72"))
                .withQueryParam("by", equalTo("LATEST"))
                .withQueryParam("direction", equalTo("DESC"))
                .withQueryParam("priceMax", equalTo("200000"))
                .withHeader("User-Agent", equalTo(props.getUserAgent())));
    }

    @Test
    void missingNextDataReturnsEmpty() {
        wm.stubFor(get(urlPathEqualTo(RESULTS_PATH))
                .willReturn(aResponse().withStatus(200).withBody("<html><body>no data</body></html>")));

        assertThat(scraper(props).fetchPage(criteria, 0)).isEmpty();
    }

    @Test
    void disabledSourceSkipsRequest() {
        props.setEnabled(false);

        List<ScrapedListing> result = scraper(props).fetchPage(criteria, 0);

        assertThat(result).isEmpty();
        wm.verify(0, getRequestedFor(urlPathEqualTo(RESULTS_PATH)));
    }
}
