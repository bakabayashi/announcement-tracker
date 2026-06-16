package pl.panzerhund.tracker.search;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.panzerhund.tracker.search.entity.SearchCriteria;

import java.util.List;
import java.util.UUID;

public interface SearchCriteriaRepository extends JpaRepository<SearchCriteria, UUID> {

    List<SearchCriteria> findByUser_Id(UUID userId);
}
