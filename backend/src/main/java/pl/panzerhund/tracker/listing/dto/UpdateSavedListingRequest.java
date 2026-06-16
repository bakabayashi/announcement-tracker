package pl.panzerhund.tracker.listing.dto;

import jakarta.validation.constraints.Size;

public record UpdateSavedListingRequest(
        @Size(max = 2000) String notes
) {
}
