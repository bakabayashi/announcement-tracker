package pl.panzerhund.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies repository mapping against a real PostgreSQL (Testcontainers): JSONB round-trip
 * (attributes), @ManyToOne associations and selected finders. Schema applied by Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaMappingTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository users;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private SavedListingRepository savedListings;

    @Test
    void persistsAndReadsJsonAttributesAndAssociations() {
        // Unique identifiers: integration tests share one Testcontainers database, so fixed
        // keys would clash with users/listings other tests commit.
        String unique = UUID.randomUUID().toString();
        String externalId = "ext-" + unique;

        Listing listing = new Listing();
        listing.setSource(Source.OLX);
        listing.setExternalId(externalId);
        listing.setCategory(Category.PLOT);
        listing.setTitle("Plot 1000 sqm");
        listing.setUrl("https://olx.pl/1");
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setAttributes(Map.of("area", 1000, "fenced", true));
        // first_seen_at / last_seen_at NOT set manually - @CreationTimestamp
        Listing persistedListing = listings.saveAndFlush(listing);

        User user = new User();
        user.setGoogleSub("sub-" + unique);
        user.setEmail(unique + "@panzerhund.pl");
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
        assertThat(listings.findBySourceAndExternalId(Source.OLX, externalId)).isPresent();
        assertThat(savedListings.findByUser_IdAndListing_Id(persistedUser.getId(), persistedListing.getId()))
                .isPresent();
    }
}
