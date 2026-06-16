package pl.panzerhund.tracker.listing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.listing.dto.PriceStatsResponse;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceStatsService {

    private static final int WINDOW_DAYS = 30;
    private static final int SCALE = 2;

    private final ListingService listingService;
    private final ListingRepository listingRepository;

    @Transactional(readOnly = true)
    public PriceStatsResponse forListing(UUID listingId) {
        Listing listing = listingService.getById(listingId);
        if (listing.getRegion() == null) {
            return new PriceStatsResponse(null, null, 0);
        }
        Instant since = Instant.now().minus(WINDOW_DAYS, ChronoUnit.DAYS);
        List<BigDecimal> prices = new ArrayList<>(listingRepository
                .findPricesByCategoryAndRegionSince(listing.getCategory(), listing.getRegion(), since));
        if (prices.isEmpty()) {
            return new PriceStatsResponse(null, null, 0);
        }
        prices.sort(Comparator.naturalOrder());
        return new PriceStatsResponse(average(prices), median(prices), prices.size());
    }

    private static BigDecimal average(List<BigDecimal> prices) {
        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(prices.size()), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal median(List<BigDecimal> sorted) {
        int n = sorted.size();
        int mid = n / 2;
        if (n % 2 == 1) {
            return sorted.get(mid).setScale(SCALE, RoundingMode.HALF_UP);
        }
        return sorted.get(mid - 1).add(sorted.get(mid))
                .divide(BigDecimal.valueOf(2), SCALE, RoundingMode.HALF_UP);
    }
}
