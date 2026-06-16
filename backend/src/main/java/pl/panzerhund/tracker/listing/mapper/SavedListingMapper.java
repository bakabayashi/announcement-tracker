package pl.panzerhund.tracker.listing.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.panzerhund.tracker.listing.dto.SavedListingResponse;
import pl.panzerhund.tracker.listing.entity.SavedListing;

@Component
@RequiredArgsConstructor
public class SavedListingMapper {

    private final ListingMapper listingMapper;

    public SavedListingResponse toResponse(SavedListing savedListing) {
        return new SavedListingResponse(
                savedListing.getId(),
                listingMapper.toResponse(savedListing.getListing()),
                savedListing.getNotes(),
                savedListing.getSavedAt());
    }
}
