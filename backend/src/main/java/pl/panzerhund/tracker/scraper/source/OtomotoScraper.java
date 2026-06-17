package pl.panzerhund.tracker.scraper.source;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.OtomotoProperties;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;

/** {@link ListingSource} for Otomoto (cars), over the assumed JSON search contract. */
@Component
public class OtomotoScraper extends AbstractRestListingSource {

    public OtomotoScraper(
            OtomotoProperties properties, ScraperProperties scraperProperties, RestClient.Builder builder) {
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
        return Source.OTOMOTO;
    }
}
