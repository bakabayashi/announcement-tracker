package pl.panzerhund.tracker.listing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.listing.entity.Listing;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository repository;

    @Transactional(readOnly = true)
    public Page<Listing> search(ListingFilter filter, Pageable pageable) {
        return repository.findAll(ListingSpecifications.matching(filter), pageable);
    }

    @Transactional(readOnly = true)
    public Listing getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Listing", id));
    }
}
