package pl.panzerhund.tracker.scraper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.PriceHistoryRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.PriceHistory;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.mapper.ScrapedListingMapper;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Persists a single {@link ScrapedListing}: inserts a new {@link Listing} or updates the re-seen one in place,
 * appending a {@link PriceHistory} entry whenever the price changes. The stop policy (what counts as
 * "already known") stays with the orchestrator — this component only reports the previous last-seen time.
 */
@Component
@RequiredArgsConstructor
public class ListingUpserter {

    /** Default currency for Polish marketplaces when a source omits it. */
    private static final String DEFAULT_CURRENCY = "PLN";

    private final ListingRepository listings;
    private final PriceHistoryRepository priceHistory;

    public UpsertResult upsert(Source source, ScrapedListing item) {
        return listings.findBySourceAndExternalId(source, item.externalId())
                .map(existing -> update(existing, item))
                .orElseGet(() -> insert(source, item));
    }

    private UpsertResult insert(Source source, ScrapedListing item) {
        Listing listing = ScrapedListingMapper.toNewListing(source, item);
        listings.save(listing);
        if (item.price() != null) {
            recordPrice(listing, item.price(), item.currency());
        }
        return UpsertResult.created();
    }

    private UpsertResult update(Listing listing, ScrapedListing item) {
        Instant previousLastSeen = listing.getLastSeenAt();
        BigDecimal previousPrice = listing.getPrice();

        ScrapedListingMapper.apply(item, listing);
        listing.setStatus(ListingStatus.ACTIVE);  // re-seen, so active again
        listing.setLastSeenAt(Instant.now());      // @CreationTimestamp only stamps on insert

        boolean priceDropped = recordPriceIfChanged(listing, previousPrice, item);
        return UpsertResult.updated(priceDropped, previousLastSeen);
    }

    /**
     * Append a price-history entry when the price changed (or is the first one known).
     * Returns {@code true} only when the new price is strictly lower than the previous one.
     */
    private boolean recordPriceIfChanged(Listing listing, BigDecimal previousPrice, ScrapedListing item) {
        BigDecimal newPrice = item.price();
        if (newPrice == null) {
            return false;
        }
        if (previousPrice == null) {
            recordPrice(listing, newPrice, item.currency());
            return false;  // first known price, not a drop
        }
        int comparison = newPrice.compareTo(previousPrice);
        if (comparison != 0) {
            recordPrice(listing, newPrice, item.currency());
        }
        return comparison < 0;
    }

    private void recordPrice(Listing listing, BigDecimal price, String currency) {
        PriceHistory entry = new PriceHistory();
        entry.setListing(listing);
        entry.setPrice(price);
        entry.setCurrency(currency != null ? currency : DEFAULT_CURRENCY);
        priceHistory.save(entry);
    }
}
