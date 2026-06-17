package pl.panzerhund.tracker.deduplication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroupMember;

import java.util.List;
import java.util.UUID;

public interface DuplicateGroupMemberRepository extends JpaRepository<DuplicateGroupMember, UUID> {

    List<DuplicateGroupMember> findByGroup_Id(UUID groupId);

    /**
     * Whether two listings are already related in any group (in either primary/member direction),
     * regardless of status — so we never re-suggest a confirmed or rejected pair.
     */
    @Query("""
            select count(m) > 0 from DuplicateGroupMember m
            where (m.group.primaryListing.id = :a and m.listing.id = :b)
               or (m.group.primaryListing.id = :b and m.listing.id = :a)
            """)
    boolean pairLinked(@Param("a") UUID a, @Param("b") UUID b);
}
