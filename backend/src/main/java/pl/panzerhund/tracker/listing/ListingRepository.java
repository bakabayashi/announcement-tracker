package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    /** Pewny match dla deduplikacji: ten sam source + external_id. */
    Optional<Listing> findBySourceAndExternalId(Source source, String externalId);

    List<Listing> findByStatus(ListingStatus status);
}
