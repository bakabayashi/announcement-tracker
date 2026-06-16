package pl.panzerhund.tracker.listing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.SavedListing;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedListingService {

    private final SavedListingRepository savedListingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SavedListing> listForUser(UUID userId) {
        return savedListingRepository.findByUser_Id(userId);
    }

    @Transactional
    public SavedListing save(UUID userId, UUID listingId, String notes) {
        Optional<SavedListing> existing = savedListingRepository.findByUser_IdAndListing_Id(userId, listingId);
        if (existing.isPresent()) {
            return existing.get(); // idempotent: already saved
        }
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Listing", listingId));
        User user = userRepository.getReferenceById(userId);

        SavedListing saved = new SavedListing();
        saved.setUser(user);
        saved.setListing(listing);
        saved.setNotes(notes);
        return savedListingRepository.save(saved);
    }

    @Transactional
    public SavedListing updateNotes(UUID userId, UUID id, String notes) {
        SavedListing saved = savedListingRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("SavedListing", id));
        saved.setNotes(notes);
        return saved; // dirty checking flushes the change
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        SavedListing saved = savedListingRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("SavedListing", id));
        savedListingRepository.delete(saved);
    }
}
