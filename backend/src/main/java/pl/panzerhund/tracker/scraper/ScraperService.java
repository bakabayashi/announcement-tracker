package pl.panzerhund.tracker.scraper;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.listing.PriceHistoryRepository;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.PriceHistory;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;
import pl.panzerhund.tracker.scraper.source.ListingSource;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;
import pl.panzerhund.tracker.search.SearchCriteriaRepository;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates criteria-driven, incremental scraping: for each {@link SearchCriteria} it runs every
 * registered {@link ListingSource}, paginating newest-first and upserting results into {@code Listing}.
 * Pagination stops early once it reaches a listing already seen within the last 24h.
 * <p>
 * Notification producers (PRICE_DROP / NEW_MATCH) are intentionally not wired here yet.
 */
@Service
@RequiredArgsConstructor
public class ScraperService {

    private static final Logger log = LoggerFactory.getLogger(ScraperService.class);

    /** Listings seen more recently than this are treated as "known" and stop pagination. */
    private static final Duration KNOWN_WINDOW = Duration.ofHours(24);

    /** Default currency for Polish marketplaces when a source omits it. */
    private static final String DEFAULT_CURRENCY = "PLN";

    private final List<ListingSource> sources;
    private final SearchCriteriaRepository searchCriteriaRepository;
    private final ListingRepository listingRepository;
    private final PriceHistoryRepository priceHistoryRepository;
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
        int created = 0;
        int updated = 0;
        int priceDrops = 0;

        for (int page = 0; page < properties.getMaxPagesPerCriteria(); page++) {
            List<ScrapedListing> items = source.fetchPage(criteria, page);
            if (items.isEmpty()) {
                break;
            }

            boolean reachedKnown = false;
            for (ScrapedListing item : items) {
                Optional<Listing> existing =
                        listingRepository.findBySourceAndExternalId(source.source(), item.externalId());

                // Newest-first: once we hit a recently-seen listing, everything after is already known.
                boolean known = existing.isPresent()
                        && existing.get().getLastSeenAt().isAfter(knownThreshold);

                if (existing.isPresent()) {
                    if (updateExisting(existing.get(), item)) {
                        priceDrops++;
                    }
                    updated++;
                } else {
                    insertNew(source.source(), item);
                    created++;
                }

                if (known) {
                    reachedKnown = true;
                    break;
                }
            }

            if (reachedKnown) {
                break;
            }
        }

        log.info("Scraped {} for criteria '{}': {} new, {} updated, {} price drops",
                source.source(), criteria.getName(), created, updated, priceDrops);
        return new ScrapeSummary(created, updated, priceDrops);
    }

    private void insertNew(Source source, ScrapedListing item) {
        Listing listing = new Listing();
        listing.setSource(source);
        listing.setExternalId(item.externalId());
        listing.setStatus(ListingStatus.ACTIVE);
        applyScraped(listing, item);
        listingRepository.save(listing);

        if (item.price() != null) {
            recordPrice(listing, item.price(), item.currency());
        }
    }

    /** Update a re-seen listing in place. Returns {@code true} when the price dropped. */
    private boolean updateExisting(Listing listing, ScrapedListing item) {
        BigDecimal previousPrice = listing.getPrice();
        applyScraped(listing, item);
        listing.setStatus(ListingStatus.ACTIVE); // re-seen, so active again
        listing.setLastSeenAt(Instant.now());     // @CreationTimestamp only stamps on insert

        boolean priceChanged = item.price() != null && previousPrice != null
                && item.price().compareTo(previousPrice) != 0;
        if (item.price() != null && (previousPrice == null || priceChanged)) {
            recordPrice(listing, item.price(), item.currency());
        }
        return priceChanged && item.price().compareTo(previousPrice) < 0;
    }

    /** Copy the mutable scraped fields onto the entity (everything except identity and timestamps). */
    private void applyScraped(Listing listing, ScrapedListing item) {
        listing.setCategory(item.category());
        listing.setTitle(item.title());
        listing.setDescription(item.description());
        listing.setPrice(item.price());
        listing.setCurrency(item.currency());
        listing.setUrl(item.url());
        listing.setCity(item.city());
        listing.setRegion(item.region());
        listing.setLat(item.lat());
        listing.setLng(item.lng());
        listing.setAttributes(item.attributes() != null ? item.attributes() : new HashMap<>());
    }

    private void recordPrice(Listing listing, BigDecimal price, String currency) {
        PriceHistory entry = new PriceHistory();
        entry.setListing(listing);
        entry.setPrice(price);
        entry.setCurrency(currency != null ? currency : DEFAULT_CURRENCY);
        priceHistoryRepository.save(entry);
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
    }
}
