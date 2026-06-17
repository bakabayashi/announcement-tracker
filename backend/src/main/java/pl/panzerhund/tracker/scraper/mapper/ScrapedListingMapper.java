package pl.panzerhund.tracker.scraper.mapper;

import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.scraper.source.ScrapedListing;

import java.util.HashMap;

/**
 * Maps a {@link ScrapedListing} onto the {@link Listing} entity.
 * Identity (source + external id) and timestamps are owned by the caller / JPA, not copied here.
 */
public final class ScrapedListingMapper {

    private ScrapedListingMapper() {
    }

    /** Build a fresh ACTIVE listing for a first-time scrape. */
    public static Listing toNewListing(Source source, ScrapedListing item) {
        Listing listing = new Listing();
        listing.setSource(source);
        listing.setExternalId(item.externalId());
        listing.setStatus(ListingStatus.ACTIVE);
        apply(item, listing);
        return listing;
    }

    /** Copy the mutable scraped fields onto an existing entity (everything except identity and timestamps). */
    public static void apply(ScrapedListing item, Listing listing) {
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
}
