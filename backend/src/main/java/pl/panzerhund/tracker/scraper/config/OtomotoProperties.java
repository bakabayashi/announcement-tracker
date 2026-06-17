package pl.panzerhund.tracker.scraper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Otomoto (cars) source configuration, bound from {@code app.scraper.otomoto}.
 * Like Otodom it is a Next.js site exposing listings in an embedded {@code __NEXT_DATA__} blob
 * (GraphQL {@code advertSearch.edges[].node}) and rejects non-browser clients.
 */
@ConfigurationProperties(prefix = "app.scraper.otomoto")
@Getter
@Setter
public class OtomotoProperties {

    /** Disabled by default; Otomoto also bot-protects by IP, so enabling may still need a proxy. */
    private boolean enabled = false;

    private String baseUrl = "https://www.otomoto.pl";

    private int pageSize = 32;

    /** Category segment of the results URL (e.g. osobowe = passenger cars). */
    private String basePath = "/osobowe";

    /** Sent as User-Agent; Otomoto returns 403 to non-browser clients. */
    private String userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
}
