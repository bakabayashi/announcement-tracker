package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Otomoto (cars) source configuration, bound from {@code app.scraper.otomoto}. */
@ConfigurationProperties(prefix = "app.scraper.otomoto")
@Getter
@Setter
public class OtomotoProperties {

    /** Disabled until the live API contract is reconciled; enable in config to start scraping. */
    private boolean enabled = false;

    private String baseUrl = "https://www.otomoto.pl";

    /** Search endpoint path returning the assumed JSON contract. */
    private String searchPath = "/api/listings";

    private int pageSize = 40;
}
