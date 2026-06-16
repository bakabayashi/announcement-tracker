package pl.panzerhund.tracker.deduplication;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroup;
import pl.panzerhund.tracker.deduplication.entity.DuplicateStatus;

import java.util.List;
import java.util.UUID;

public interface DuplicateGroupRepository extends JpaRepository<DuplicateGroup, UUID> {

    List<DuplicateGroup> findByStatus(DuplicateStatus status);
}
