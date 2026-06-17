package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Allegro Lokalnie source configuration, bound from {@code app.scraper.allegro}. */
@ConfigurationProperties(prefix = "app.scraper.allegro")
@Getter
@Setter
public class AllegroLokalnieProperties {

    /** Disabled until the live API contract is reconciled; enable in config to start scraping. */
    private boolean enabled = false;

    private String baseUrl = "https://allegrolokalnie.pl";

    /** Search endpoint path returning the assumed JSON contract. */
    private String searchPath = "/api/listings";

    private int pageSize = 40;
}
