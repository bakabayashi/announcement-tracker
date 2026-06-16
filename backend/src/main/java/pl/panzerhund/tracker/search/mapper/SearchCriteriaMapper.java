package pl.panzerhund.tracker.search.mapper;

import pl.panzerhund.tracker.search.dto.SearchCriteriaResponse;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

public final class SearchCriteriaMapper {

    private SearchCriteriaMapper() {
    }

    public static SearchCriteriaResponse toResponse(SearchCriteria criteria) {
        return new SearchCriteriaResponse(
                criteria.getId(),
                criteria.getName(),
                criteria.getCategory(),
                criteria.getFilters());
    }
}
