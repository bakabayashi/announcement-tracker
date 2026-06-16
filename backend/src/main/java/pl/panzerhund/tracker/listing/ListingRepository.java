package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    /** Exact match for deduplication: same source + external_id. */
    Optional<Listing> findBySourceAndExternalId(Source source, String externalId);

    List<Listing> findByStatus(ListingStatus status);

    /** Prices of similar listings (same category + region, seen within the window), for stats. */
    @Query("""
            select l.price from Listing l
            where l.category = :category and l.region = :region
              and l.lastSeenAt >= :since and l.price is not null
            """)
    List<BigDecimal> findPricesByCategoryAndRegionSince(@Param("category") Category category,
                                                        @Param("region") String region,
                                                        @Param("since") Instant since);
}
