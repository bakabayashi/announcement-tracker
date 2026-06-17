package pl.panzerhund.tracker.scraper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.notification.ScrapeNotificationProducer;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.scraper.source.ListingSource;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;
import pl.panzerhund.tracker.search.SearchCriteriaRepository;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates criteria-driven, incremental scraping: for each {@link SearchCriteria} it runs every
 * registered {@link ListingSource}, paginating newest-first and delegating persistence to {@link ListingUpserter}.
 * Pagination stops early once it reaches a listing already seen within the last 24h.
 * <p>
 * Each upsert is translated into notifications for the criteria's owner via
 * {@link ScrapeNotificationProducer}: a newly inserted listing yields NEW_MATCH, a price drop yields PRICE_DROP.
 */
@Service
@RequiredArgsConstructor
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    /** Listings seen more recently than this are treated as "known" and stop pagination. */
    private static final Duration KNOWN_WINDOW = Duration.ofHours(24);

    private final List<ListingSource> sources;
    private final SearchCriteriaRepository searchCriteriaRepository;
    private final ListingUpserter upserter;
    private final ScrapeNotificationProducer notificationProducer;
    private final ScraperProperties properties;

    /** Scrape every stored criteria across all sources. Returns the aggregate summary. */
    @Transactional
    public ScrapeSummary scrapeAll() {
        ScrapeSummary total = ScrapeSummary.empty();
        for (SearchCriteria criteria : searchCriteriaRepository.findAll()) {
            total = total.plus(scrape(criteria));
        }
        return total;
    }

    /** Scrape a single criteria across all sources. */
    @Transactional
    public ScrapeSummary scrape(SearchCriteria criteria) {
        ScrapeSummary summary = ScrapeSummary.empty();
        for (ListingSource source : sources) {
            summary = summary.plus(scrapeSource(source, criteria));
        }
        return summary;
    }

    private ScrapeSummary scrapeSource(ListingSource source, SearchCriteria criteria) {
        Instant knownThreshold = Instant.now().minus(KNOWN_WINDOW);
        UUID userId = criteria.getUser().getId();
        ScrapeSummary summary = ScrapeSummary.empty();

        for (int page = 0; page < properties.getMaxPagesPerCriteria(); page++) {
            List<ScrapedListing> items = source.fetchPage(criteria, page);
            if (items.isEmpty()) {
                break;
            }

            boolean reachedKnown = false;
            for (ScrapedListing item : items) {
                UpsertResult result = upserter.upsert(source.source(), item);
                summary = summary.plus(result);
                produceNotifications(userId, result);

                // Newest-first: once we hit a recently-seen listing, everything after is already known.
                if (result.wasSeenAfter(knownThreshold)) {
                    reachedKnown = true;
                    break;
                }
            }

            if (reachedKnown) {
                break;
            }
        }

        log.info("Scraped {} for criteria '{}': {} new, {} updated, {} price drops",
                source.source(), criteria.getName(),
                summary.created(), summary.updated(), summary.priceDrops());
        return summary;
    }

    /** Emit the notifications a single upsert warrants for the criteria's owner. */
    private void produceNotifications(UUID userId, UpsertResult result) {
        if (result.outcome() == UpsertResult.Outcome.CREATED) {
            notificationProducer.notifyNewMatch(userId, result.listingId());
        }
        if (result.priceDropped()) {
            notificationProducer.notifyPriceDrop(userId, result.listingId());
        }
    }

    /** Aggregate counts produced by a scrape run. */
    public record ScrapeSummary(int created, int updated, int priceDrops) {

        static ScrapeSummary empty() {
            return new ScrapeSummary(0, 0, 0);
        }

        ScrapeSummary plus(ScrapeSummary other) {
            return new ScrapeSummary(
                    created + other.created,
                    updated + other.updated,
                    priceDrops + other.priceDrops);
        }

        ScrapeSummary plus(UpsertResult result) {
            boolean inserted = result.outcome() == UpsertResult.Outcome.CREATED;
            return new ScrapeSummary(
                    created + (inserted ? 1 : 0),
                    updated + (inserted ? 0 : 1),
                    priceDrops + (result.priceDropped() ? 1 : 0));
        }
    }
}
