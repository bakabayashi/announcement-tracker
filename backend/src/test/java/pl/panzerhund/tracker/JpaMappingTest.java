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

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Weryfikuje runtime mapowań repozytoriów na H2: round-trip JSONB (attributes),
 * asocjacje @ManyToOne oraz wybrane findery. Schema z encji (create-drop).
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
        listing.setTitle("Działka 1000 m2");
        listing.setUrl("https://olx.pl/1");
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setAttributes(Map.of("area", 1000, "fenced", true));
        listing.setFirstSeenAt(Instant.now());
        listing.setLastSeenAt(Instant.now());
        Listing persistedListing = listings.saveAndFlush(listing);

        User user = new User();
        user.setGoogleSub("sub-1");
        user.setEmail("tester@panzerhund.pl");
        user.setName("Tester");
        user.setCreatedAt(Instant.now());
        User persistedUser = users.saveAndFlush(user);

        SavedListing saved = new SavedListing();
        saved.setUser(persistedUser);
        saved.setListing(persistedListing);
        saved.setSavedAt(Instant.now());
        savedListings.saveAndFlush(saved);

        // JSON round-trip
        Listing reloaded = listings.findById(persistedListing.getId()).orElseThrow();
        assertThat(reloaded.getAttributes())
                .containsEntry("area", 1000)
                .containsEntry("fenced", true);

        // findery + asocjacje
        assertThat(listings.findBySourceAndExternalId(Source.OLX, "ext-1")).isPresent();
        assertThat(savedListings.findByUser_IdAndListing_Id(persistedUser.getId(), persistedListing.getId()))
                .isPresent();
    }
}
