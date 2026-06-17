package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Listings of a status seen at or after the given instant (deduplication scans freshly-active ones). */
    List<Listing> findByStatusAndLastSeenAtGreaterThanEqual(ListingStatus status, Instant since);

    /**
     * Duplicate candidates within {@code radiusMeters} of a point (PostGIS {@code ST_DWithin} on the
     * GIST-indexed geography column): same category, georeferenced, not merged, excluding the subject.
     */
    @Query(value = """
            select * from listings l
            where l.category = :category and l.status <> 'MERGED' and l.id <> :excludeId
              and l.geo is not null
              and ST_DWithin(l.geo, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
            """, nativeQuery = true)
    List<Listing> findGeoCandidates(@Param("category") String category,
                                    @Param("excludeId") UUID excludeId,
                                    @Param("lng") double lng,
                                    @Param("lat") double lat,
                                    @Param("radiusMeters") double radiusMeters);

    /** Prices of similar listings (same category + region, seen within the window), for stats. */
    @Query("""
            select l.price from Listing l
            where l.category = :category and l.region = :region
              and l.lastSeenAt >= :since and l.price is not null
            """)
    List<BigDecimal> findPricesByCategoryAndRegionSince(@Param("category") Category category,
                                                        @Param("region") String region,
                                                        @Param("since") Instant since);

    /** Cleanup: flip stale listings of one status to another (e.g. ACTIVE -> INACTIVE). Returns rows updated. */
    @Modifying
    @Query("update Listing l set l.status = :to where l.status = :from and l.lastSeenAt < :threshold")
    int updateStatusForStale(@Param("from") ListingStatus from,
                             @Param("to") ListingStatus to,
                             @Param("threshold") Instant threshold);

    /**
     * Cleanup: delete listings of a status older than the threshold, skipping any a user saved.
     * DB-level ON DELETE CASCADE removes their price history, notifications and duplicate-group rows.
     */
    @Modifying
    @Query("""
            delete from Listing l
            where l.status = :status and l.lastSeenAt < :threshold
              and not exists (select 1 from SavedListing s where s.listing = l)
            """)
    int deleteUnsavedByStatusOlderThan(@Param("status") ListingStatus status,
                                       @Param("threshold") Instant threshold);
}
