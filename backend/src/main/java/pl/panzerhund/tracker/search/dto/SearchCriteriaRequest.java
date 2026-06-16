package pl.panzerhund.tracker.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.panzerhund.tracker.category.entity.Category;

import java.util.Map;

public record SearchCriteriaRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull Category category,
        Map<String, Object> filters
) {
}
