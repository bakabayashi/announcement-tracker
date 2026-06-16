package pl.panzerhund.tracker.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SaveListingRequest(
        @NotNull UUID listingId,
        @Size(max = 2000) String notes
) {
}
