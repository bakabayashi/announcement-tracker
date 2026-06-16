package pl.panzerhund.tracker.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.search.entity.SearchCriteria;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchCriteriaService {

    private final SearchCriteriaRepository searchCriteriaRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SearchCriteria> listForUser(UUID userId) {
        return searchCriteriaRepository.findByUser_Id(userId);
    }

    @Transactional
    public SearchCriteria create(UUID userId, String name, Category category, Map<String, Object> filters) {
        User user = userRepository.getReferenceById(userId);

        SearchCriteria criteria = new SearchCriteria();
        criteria.setUser(user);
        criteria.setName(name);
        criteria.setCategory(category);
        criteria.setFilters(filters != null ? filters : new HashMap<>());
        return searchCriteriaRepository.save(criteria);
    }

    @Transactional
    public SearchCriteria update(UUID userId, UUID id, String name, Category category, Map<String, Object> filters) {
        SearchCriteria criteria = searchCriteriaRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("SearchCriteria", id));
        criteria.setName(name);
        criteria.setCategory(category);
        criteria.setFilters(filters != null ? filters : new HashMap<>());
        return criteria; // dirty checking flushes the change
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        SearchCriteria criteria = searchCriteriaRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("SearchCriteria", id));
        searchCriteriaRepository.delete(criteria);
    }
}
