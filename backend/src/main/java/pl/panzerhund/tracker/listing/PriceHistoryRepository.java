package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.panzerhund.tracker.listing.entity.PriceHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    /** Listing price history, oldest first (for the chart). */
    List<PriceHistory> findByListing_IdOrderByRecordedAtAsc(UUID listingId);

    /** Cleanup: delete price history older than the threshold, keeping all history of saved listings. */
    @Modifying
    @Query("""
            delete from PriceHistory p
            where p.recordedAt < :threshold
              and not exists (select 1 from SavedListing s where s.listing = p.listing)
            """)
    int deleteOlderThanUnsaved(@Param("threshold") Instant threshold);
}
