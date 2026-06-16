package pl.panzerhund.tracker.listing.dto;

import java.math.BigDecimal;

/** Market price stats for similar listings. average/median are null when sampleSize is 0. */
public record PriceStatsResponse(
        BigDecimal average,
        BigDecimal median,
        int sampleSize
) {
}
