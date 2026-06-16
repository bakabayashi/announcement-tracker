package pl.panzerhund.tracker.listing.mapper;

import pl.panzerhund.tracker.listing.dto.SavedListingResponse;
import pl.panzerhund.tracker.listing.entity.SavedListing;

public final class SavedListingMapper {

    private SavedListingMapper() {
    }

    public static SavedListingResponse toResponse(SavedListing savedListing) {
        return new SavedListingResponse(
                savedListing.getId(),
                ListingMapper.toResponse(savedListing.getListing()),
                savedListing.getNotes(),
                savedListing.getSavedAt());
    }
}
