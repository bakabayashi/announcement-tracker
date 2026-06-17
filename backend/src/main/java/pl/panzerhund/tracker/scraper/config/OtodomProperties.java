package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Otodom (real estate) source configuration, bound from {@code app.scraper.otodom}. */
@ConfigurationProperties(prefix = "app.scraper.otodom")
@Getter
@Setter
public class OtodomProperties {

    /** Disabled until the live API contract is reconciled; enable in config to start scraping. */
    private boolean enabled = false;

    private String baseUrl = "https://www.otodom.pl";

    /** Search endpoint path returning the assumed JSON contract. */
    private String searchPath = "/api/listings";

    private int pageSize = 40;
}
