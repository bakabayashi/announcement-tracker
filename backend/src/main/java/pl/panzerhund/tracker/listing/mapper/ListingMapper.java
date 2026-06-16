package pl.panzerhund.tracker.listing.mapper;

import pl.panzerhund.tracker.listing.dto.ListingResponse;
import pl.panzerhund.tracker.listing.dto.PriceHistoryResponse;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.PriceHistory;

public final class ListingMapper {

    private ListingMapper() {
    }

    public static ListingResponse toResponse(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getSource(),
                listing.getExternalId(),
                listing.getCategory(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getUrl(),
                listing.getCity(),
                listing.getRegion(),
                listing.getLat(),
                listing.getLng(),
                listing.getAttributes(),
                listing.getStatus(),
                listing.getFirstSeenAt(),
                listing.getLastSeenAt());
    }

    public static PriceHistoryResponse toResponse(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
                priceHistory.getPrice(),
                priceHistory.getCurrency(),
                priceHistory.getRecordedAt());
    }
}
