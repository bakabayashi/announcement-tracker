package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Scraper configuration, bound from {@code app.scraper}. */
@ConfigurationProperties(prefix = "app.scraper")
@Getter
@Setter
public class ScraperProperties {

    /** Maximum pages fetched per criteria before stopping. */
    private int maxPagesPerCriteria = 5;

    /** Lower bound of the randomized delay between requests to the same service. */
    private int delayMinSeconds = 3;

    /** Upper bound of the randomized delay between requests to the same service. */
    private int delayMaxSeconds = 8;

    /** Start of the nightly scraping window, HH:mm. */
    private String windowStart = "22:00";

    /** Length of the scraping window in minutes. */
    private int windowDurationMinutes = 360;
}
