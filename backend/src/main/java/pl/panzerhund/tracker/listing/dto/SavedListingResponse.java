package pl.panzerhund.tracker.listing.dto;

import java.time.Instant;
import java.util.UUID;

public record SavedListingResponse(
        UUID id,
        ListingResponse listing,
        String notes,
        Instant savedAt
) {
}
