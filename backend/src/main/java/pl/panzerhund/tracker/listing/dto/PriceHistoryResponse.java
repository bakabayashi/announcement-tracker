package pl.panzerhund.tracker.listing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryResponse(
        BigDecimal price,
        String currency,
        Instant recordedAt
) {
}
