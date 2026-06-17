package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Otodom (real estate) source configuration, bound from {@code app.scraper.otodom}.
 * Otodom serves Next.js HTML with an embedded {@code __NEXT_DATA__} JSON blob (no public REST API)
 * and rejects non-browser clients, so a realistic User-Agent is required.
 */
@ConfigurationProperties(prefix = "app.scraper.otodom")
@Getter
@Setter
public class OtodomProperties {

    /** Disabled by default; Otodom also bot-protects by IP, so enabling may still need a proxy. */
    private boolean enabled = false;

    private String baseUrl = "https://www.otodom.pl";

    /** Results per page; Otodom caps the listing view at 72. */
    private int pageSize = 72;

    /** Default transaction segment of the results URL (sprzedaz/wynajem). */
    private String transaction = "sprzedaz";

    /** Default property-type segment of the results URL (e.g. dzialka, mieszkanie, dom). */
    private String propertyType = "dzialka";

    /** Default location segment of the results URL (e.g. cala-polska, mazowieckie/warszawa). */
    private String location = "cala-polska";

    /** Sent as User-Agent; Otodom returns 403 to non-browser clients. */
    private String userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
}
