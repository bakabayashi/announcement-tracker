package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.listing.entity.SavedListing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedListingRepository extends JpaRepository<SavedListing, UUID> {

    List<SavedListing> findByUser_Id(UUID userId);

    Optional<SavedListing> findByUser_IdAndListing_Id(UUID userId, UUID listingId);
}
