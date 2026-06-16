package pl.panzerhund.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.SavedListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.SavedListing;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies repository mapping at runtime on H2: JSONB round-trip (attributes),
 * @ManyToOne associations and selected finders. Schema generated from entities (create-drop).
 */
@DataJpaTest
class JpaMappingTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private SavedListingRepository savedListings;

    @Test
    void persistsAndReadsJsonAttributesAndAssociations() {
        Listing listing = new Listing();
        listing.setSource(Source.OLX);
        listing.setExternalId("ext-1");
        listing.setCategory(Category.PLOT);
        listing.setTitle("Plot 1000 sqm");
        listing.setUrl("https://olx.pl/1");
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setAttributes(Map.of("area", 1000, "fenced", true));
        // first_seen_at / last_seen_at NOT set manually - @CreationTimestamp
        Listing persistedListing = listings.saveAndFlush(listing);

        User user = new User();
        user.setGoogleSub("sub-1");
        user.setEmail("tester@panzerhund.pl");
        user.setName("Tester");
        // created_at NOT set manually - @CreationTimestamp
        User persistedUser = users.saveAndFlush(user);

        SavedListing saved = new SavedListing();
        saved.setUser(persistedUser);
        saved.setListing(persistedListing);
        // saved_at NOT set manually - @CreationTimestamp
        savedListings.saveAndFlush(saved);

        // JSON round-trip
        Listing reloaded = listings.findById(persistedListing.getId()).orElseThrow();
        assertThat(reloaded.getAttributes())
                .containsEntry("area", 1000)
                .containsEntry("fenced", true);

        // auto timestamps (@CreationTimestamp) populated despite no manual setting
        assertThat(reloaded.getFirstSeenAt()).isNotNull();
        assertThat(reloaded.getLastSeenAt()).isNotNull();
        assertThat(persistedUser.getCreatedAt()).isNotNull();

        // finders + associations
        assertThat(listings.findBySourceAndExternalId(Source.OLX, "ext-1")).isPresent();
        assertThat(savedListings.findByUser_IdAndListing_Id(persistedUser.getId(), persistedListing.getId()))
                .isPresent();
    }
}
