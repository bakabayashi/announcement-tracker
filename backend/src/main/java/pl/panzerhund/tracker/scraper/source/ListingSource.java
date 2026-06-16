package pl.panzerhund.tracker.scraper.source;

import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.util.List;

/**
 * A single external service to scrape (OLX, Otodom, ...). One bean per service.
 * Scraping is criteria-driven: the source builds its query from the criteria's JSONB filters.
 * Pagination is page-by-page so the {@code ScraperService} can stop incrementally
 * once it reaches listings it already knows.
 */
public interface ListingSource {

    /** Which service this source scrapes. */
    Source source();

    /**
     * Fetch one page of results for the given criteria, sorted newest first.
     * {@code page} is zero-based. Returns an empty list when there are no more results.
     */
    List<ScrapedListing> fetchPage(SearchCriteria criteria, int page);
}
