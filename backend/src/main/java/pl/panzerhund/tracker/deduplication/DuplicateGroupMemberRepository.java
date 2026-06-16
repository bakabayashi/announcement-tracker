package pl.panzerhund.tracker.deduplication;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroupMember;

import java.util.List;
import java.util.UUID;

public interface DuplicateGroupMemberRepository extends JpaRepository<DuplicateGroupMember, UUID> {

    List<DuplicateGroupMember> findByGroup_Id(UUID groupId);
}
