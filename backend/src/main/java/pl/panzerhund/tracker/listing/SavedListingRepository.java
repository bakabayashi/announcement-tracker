package pl.panzerhund.tracker.listing;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.listing.entity.SavedListing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedListingRepository extends JpaRepository<SavedListing, UUID> {

    @EntityGraph(attributePaths = "listing")
    List<SavedListing> findByUser_Id(UUID userId);

    @EntityGraph(attributePaths = "listing")
    Optional<SavedListing> findByUser_IdAndListing_Id(UUID userId, UUID listingId);

    @EntityGraph(attributePaths = "listing")
    Optional<SavedListing> findByIdAndUser_Id(UUID id, UUID userId);
}
