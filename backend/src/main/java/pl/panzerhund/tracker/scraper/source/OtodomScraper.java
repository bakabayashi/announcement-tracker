package pl.panzerhund.tracker.scraper.source;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.OtodomProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;

/** {@link ListingSource} for Otodom (real estate), over the assumed JSON search contract. */
@Component
public class OtodomScraper extends AbstractRestListingSource {

    public OtodomScraper(
            OtodomProperties properties, ScraperProperties scraperProperties, RestClient.Builder builder) {
        super(
                properties.getBaseUrl(),
                properties.getSearchPath(),
                properties.getPageSize(),
                properties.isEnabled(),
                scraperProperties,
                builder);
    }

    @Override
    public Source source() {
        return Source.OTODOM;
    }
}
