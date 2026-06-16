package pl.panzerhund.tracker.listing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.common.dto.PageResponse;
import pl.panzerhund.tracker.listing.dto.ListingResponse;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.listing.mapper.ListingMapper;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService service;
    private final ListingMapper mapper;

    @GetMapping
    public PageResponse<ListingResponse> list(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Source source,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            Pageable pageable) {
        ListingFilter filter = new ListingFilter(category, source, status, region, q, priceMin, priceMax);
        return PageResponse.of(service.search(filter, pageable).map(mapper::toResponse));
    }

    @GetMapping("/{id}")
    public ListingResponse get(@PathVariable UUID id) {
        return mapper.toResponse(service.getById(id));
    }
}
