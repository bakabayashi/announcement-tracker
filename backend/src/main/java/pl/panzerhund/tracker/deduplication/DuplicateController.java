package pl.panzerhund.tracker.deduplication;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.panzerhund.tracker.deduplication.dto.DuplicateGroupResponse;

import java.util.List;
import java.util.UUID;

/**
 * Duplicate suggestions are visible to any logged-in user (the listing pool is shared); merging
 * decisions are global and therefore restricted to admins.
 */
@RestController
@RequestMapping("/api/v1/duplicates")
@RequiredArgsConstructor
public class DuplicateController {

    private final DeduplicationService service;

    @GetMapping
    public List<DuplicateGroupResponse> list() {
        return service.listSuggested();
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void confirm(@PathVariable UUID id) {
        service.confirm(id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void reject(@PathVariable UUID id) {
        service.reject(id);
    }
}
