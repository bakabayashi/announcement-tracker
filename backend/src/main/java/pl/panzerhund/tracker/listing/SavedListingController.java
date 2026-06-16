package pl.panzerhund.tracker.listing;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.panzerhund.tracker.listing.dto.SaveListingRequest;
import pl.panzerhund.tracker.listing.dto.SavedListingResponse;
import pl.panzerhund.tracker.listing.dto.UpdateSavedListingRequest;
import pl.panzerhund.tracker.listing.mapper.SavedListingMapper;
import pl.panzerhund.tracker.security.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/saved-listings")
@RequiredArgsConstructor
public class SavedListingController {

    private final SavedListingService service;
    private final SavedListingMapper mapper;

    @GetMapping
    public List<SavedListingResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.listForUser(principal.getUserId()).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedListingResponse save(@AuthenticationPrincipal AppUserPrincipal principal,
                                     @Valid @RequestBody SaveListingRequest request) {
        return mapper.toResponse(service.save(principal.getUserId(), request.listingId(), request.notes()));
    }

    @PatchMapping("/{id}")
    public SavedListingResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable UUID id,
                                       @Valid @RequestBody UpdateSavedListingRequest request) {
        return mapper.toResponse(service.updateNotes(principal.getUserId(), id, request.notes()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        service.delete(principal.getUserId(), id);
    }
}
