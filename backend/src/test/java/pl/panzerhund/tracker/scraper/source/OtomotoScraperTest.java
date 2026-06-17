package pl.panzerhund.tracker.scraper.source;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

class OtomotoScraperTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Minimal Next.js page: __NEXT_DATA__ holds props.pageProps.advertSearch.edges[].node.
    private static final String HTML = """
            <!doctype html><html><body>
            <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"advertSearch":{"edges":[
            {"node":{"id":"6789","title":"Audi A4",
             "url":"https://www.otomoto.pl/osobowe/oferta/audi-a4-ID6789.html",
             "price":{"amount":{"value":65000,"currencyCode":"PLN"}},
             "location":{"city":{"name":"Warszawa"},"region":{"name":"mazowieckie"}},
             "parameters":[{"key":"year","value":"2018","displayValue":"2018"},
                           {"key":"mileage","value":"90000","displayValue":"90 000 km"}]}}
            ]}}}}</script>
            </body></html>
            """;

    private static final String RESULTS_PATH = "/osobowe";

    private OtomotoProperties props;
    private SearchCriteria criteria;

    @BeforeEach
    void setUp() {
        props = new OtomotoProperties();
        props.setEnabled(true);
        props.setBaseUrl(wm.baseUrl());

        criteria = new SearchCriteria();
        criteria.setName("Auta");
        criteria.setCategory(Category.CAR);
        criteria.setFilters(Map.of("priceMax", 100000));
    }

    private OtomotoScraper scraper(OtomotoProperties properties) {
        ScraperProperties scraperProps = new ScraperProperties();
        scraperProps.setDelayMinSeconds(0);
        scraperProps.setDelayMaxSeconds(0);
        return new OtomotoScraper(properties, scraperProps, RestClient.builder(), new ObjectMapper());
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder htmlPage() {
        return aResponse().withStatus(200).withHeader("Content-Type", "text/html; charset=utf-8").withBody(HTML);
    }

    @Test
    void source() {
        assertThat(scraper(props).source()).isEqualTo(Source.OTOMOTO);
    }

    @Test
    void fetchPageParsesNodeFromNextData() {
        wm.stubFor(get(urlPathEqualTo(RESULTS_PATH)).willReturn(htmlPage()));

        List<ScrapedListing> result = scraper(props).fetchPage(criteria, 0);

        assertThat(result).hasSize(1);
        ScrapedListing s = result.get(0);
        assertThat(s.externalId()).isEqualTo("6789");
        assertThat(s.category()).isEqualTo(Category.CAR);
        assertThat(s.title()).isEqualTo("Audi A4");
        assertThat(s.price()).isEqualByComparingTo("65000");
        assertThat(s.currency()).isEqualTo("PLN");
        assertThat(s.url()).isEqualTo("https://www.otomoto.pl/osobowe/oferta/audi-a4-ID6789.html");
        assertThat(s.city()).isEqualTo("Warszawa");
        assertThat(s.region()).isEqualTo("mazowieckie");
        assertThat(s.lat()).isNull();
        assertThat(s.attributes()).containsEntry("year", "2018").containsEntry("mileage", "90 000 km");
    }

    @Test
    void fetchPageSendsResultsUrlPagingAndBrowserUserAgent() {
        wm.stubFor(get(urlPathEqualTo(RESULTS_PATH)).willReturn(htmlPage()));

        scraper(props).fetchPage(criteria, 0);

        wm.verify(getRequestedFor(urlPathEqualTo(RESULTS_PATH))
                .withQueryParam("page", equalTo("1"))
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
