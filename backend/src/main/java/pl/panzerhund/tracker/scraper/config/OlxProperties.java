package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** OLX Partner API configuration, bound from {@code app.scraper.olx}. */
@ConfigurationProperties(prefix = "app.scraper.olx")
@Getter
@Setter
public class OlxProperties {

    /** Partner API base URL. */
    private String baseUrl = "https://www.olx.pl/api/partner";

    /** OAuth2 bearer token. Blank in dev disables OLX scraping. TODO: client-credentials token refresh. */
    private String accessToken = "";

    /** Value of the required {@code Version} header. */
    private String apiVersion = "2.0";

    /** Results per page (maps to the {@code limit} query parameter). */
    private int pageSize = 50;
}
