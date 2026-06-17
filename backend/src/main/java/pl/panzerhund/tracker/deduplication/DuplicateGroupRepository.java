package pl.panzerhund.tracker.deduplication;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroup;
import pl.panzerhund.tracker.deduplication.entity.DuplicateStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DuplicateGroupRepository extends JpaRepository<DuplicateGroup, UUID> {

    List<DuplicateGroup> findByStatus(DuplicateStatus status);

    /** An open suggestion anchored on a given primary listing, to extend instead of duplicating it. */
    Optional<DuplicateGroup> findByPrimaryListing_IdAndStatus(UUID primaryListingId, DuplicateStatus status);
}
