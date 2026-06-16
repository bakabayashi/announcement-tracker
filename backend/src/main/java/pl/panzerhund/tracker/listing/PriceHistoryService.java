package pl.panzerhund.tracker.listing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.listing.entity.PriceHistory;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final ListingService listingService;
    private final PriceHistoryRepository repository;

    @Transactional(readOnly = true)
    public List<PriceHistory> forListing(UUID listingId) {
        listingService.getById(listingId); // 404 if the listing does not exist
        return repository.findByListing_IdOrderByRecordedAtAsc(listingId);
    }
}
