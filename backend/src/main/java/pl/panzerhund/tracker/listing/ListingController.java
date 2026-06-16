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
import pl.panzerhund.tracker.listing.dto.PriceHistoryResponse;
import pl.panzerhund.tracker.listing.dto.PriceStatsResponse;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.listing.mapper.ListingMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService service;
    private final PriceHistoryService priceHistoryService;
    private final PriceStatsService priceStatsService;

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
        return PageResponse.of(service.search(filter, pageable).map(ListingMapper::toResponse));
    }

    @GetMapping("/{id}")
    public ListingResponse get(@PathVariable UUID id) {
        return ListingMapper.toResponse(service.getById(id));
    }

    @GetMapping("/{id}/price-history")
    public List<PriceHistoryResponse> priceHistory(@PathVariable UUID id) {
        return priceHistoryService.forListing(id).stream().map(ListingMapper::toResponse).toList();
    }

    @GetMapping("/{id}/price-stats")
    public PriceStatsResponse priceStats(@PathVariable UUID id) {
        return priceStatsService.forListing(id);
    }
}
