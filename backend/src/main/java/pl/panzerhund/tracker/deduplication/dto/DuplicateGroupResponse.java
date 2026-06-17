package pl.panzerhund.tracker.deduplication.dto;

import pl.panzerhund.tracker.deduplication.entity.DuplicateStatus;

import java.util.List;
import java.util.UUID;

/** A duplicate group: the canonical (primary) listing and the candidate duplicates pointing at it. */
public record DuplicateGroupResponse(
        UUID groupId,
        DuplicateStatus status,
        DuplicateListingResponse primary,
        List<DuplicateListingResponse> members
) {
}
