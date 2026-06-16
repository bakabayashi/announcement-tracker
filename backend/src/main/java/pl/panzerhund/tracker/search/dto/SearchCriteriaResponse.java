package pl.panzerhund.tracker.search.dto;

import pl.panzerhund.tracker.category.entity.Category;

import java.util.Map;
import java.util.UUID;

public record SearchCriteriaResponse(
        UUID id,
        String name,
        Category category,
        Map<String, Object> filters
) {
}
