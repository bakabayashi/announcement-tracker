package pl.panzerhund.tracker.deduplication.mapper;

import pl.panzerhund.tracker.deduplication.dto.DuplicateGroupResponse;
import pl.panzerhund.tracker.deduplication.dto.DuplicateListingResponse;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroup;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroupMember;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.util.List;

public final class DuplicateGroupMapper {

    private DuplicateGroupMapper() {
    }

    public static DuplicateGroupResponse toResponse(DuplicateGroup group, List<DuplicateGroupMember> members) {
        return new DuplicateGroupResponse(
                group.getId(),
                group.getStatus(),
                toListingResponse(group.getPrimaryListing()),
                members.stream().map(m -> toListingResponse(m.getListing())).toList());
    }

    private static DuplicateListingResponse toListingResponse(Listing listing) {
        return new DuplicateListingResponse(
                listing.getId(),
                listing.getSource(),
                listing.getTitle(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getCity(),
                listing.getRegion(),
                listing.getUrl(),
                listing.getStatus());
    }
}
