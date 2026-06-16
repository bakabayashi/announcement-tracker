package pl.panzerhund.tracker.search;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.panzerhund.tracker.search.dto.SearchCriteriaRequest;
import pl.panzerhund.tracker.search.dto.SearchCriteriaResponse;
import pl.panzerhund.tracker.search.mapper.SearchCriteriaMapper;
import pl.panzerhund.tracker.user.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search-criteria")
@RequiredArgsConstructor
public class SearchCriteriaController {

    private final SearchCriteriaService service;

    @GetMapping
    public List<SearchCriteriaResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.listForUser(principal.getUserId()).stream().map(SearchCriteriaMapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchCriteriaResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
                                         @Valid @RequestBody SearchCriteriaRequest request) {
        return SearchCriteriaMapper.toResponse(
                service.create(principal.getUserId(), request.name(), request.category(), request.filters()));
    }

    @PutMapping("/{id}")
    public SearchCriteriaResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody SearchCriteriaRequest request) {
        return SearchCriteriaMapper.toResponse(
                service.update(principal.getUserId(), id, request.name(), request.category(), request.filters()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        service.delete(principal.getUserId(), id);
    }
}
