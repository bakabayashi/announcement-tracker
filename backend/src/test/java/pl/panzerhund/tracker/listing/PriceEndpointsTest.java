package pl.panzerhund.tracker.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.PriceHistory;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PriceEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private PriceHistoryRepository prices;

    @BeforeEach
    void clean() {
        prices.deleteAll();
        listings.deleteAll();
    }

    private Listing saveListing(String externalId, Category category, String region, String price) {
        Listing l = new Listing();
        l.setSource(Source.OLX);
        l.setExternalId(externalId);
        l.setCategory(category);
        l.setTitle("Listing " + externalId);
        l.setUrl("https://olx.pl/" + externalId);
        l.setStatus(ListingStatus.ACTIVE);
        l.setRegion(region);
        l.setPrice(new BigDecimal(price));
        return listings.save(l);
    }

    private void savePrice(Listing listing, String price) {
        PriceHistory ph = new PriceHistory();
        ph.setListing(listing);
        ph.setPrice(new BigDecimal(price));
        ph.setCurrency("PLN");
        prices.save(ph);
    }

    @Test
    void priceHistoryReturnsEntries() throws Exception {
        Listing listing = saveListing("a", Category.PLOT, "mazowieckie", "100000");
        savePrice(listing, "100000");
        savePrice(listing, "95000");

        mvc.perform(get("/api/v1/listings/{id}/price-history", listing.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void priceHistoryReturns404ForUnknownListing() throws Exception {
        mvc.perform(get("/api/v1/listings/{id}/price-history", UUID.randomUUID()).with(oauth2Login()))
                .andExpect(status().isNotFound());
    }

    @Test
    void priceStatsAggregatesSimilarListings() throws Exception {
        Listing target = saveListing("a", Category.PLOT, "mazowieckie", "100000");
        saveListing("b", Category.PLOT, "mazowieckie", "200000");

        mvc.perform(get("/api/v1/listings/{id}/price-stats", target.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(2))
                .andExpect(jsonPath("$.average").isNumber())
                .andExpect(jsonPath("$.median").isNumber());
    }
}
