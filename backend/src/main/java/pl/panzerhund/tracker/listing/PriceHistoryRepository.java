package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.listing.entity.PriceHistory;

import java.util.List;
import java.util.UUID;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    /** Listing price history, oldest first (for the chart). */
    List<PriceHistory> findByListing_IdOrderByRecordedAtAsc(UUID listingId);
}
