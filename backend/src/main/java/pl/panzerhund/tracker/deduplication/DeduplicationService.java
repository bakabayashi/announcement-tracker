package pl.panzerhund.tracker.deduplication;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.deduplication.dto.DuplicateGroupResponse;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroup;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroupMember;
import pl.panzerhund.tracker.deduplication.entity.DuplicateStatus;
import pl.panzerhund.tracker.deduplication.mapper.DuplicateGroupMapper;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.SavedListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.SavedListing;
import pl.panzerhund.tracker.notification.RepostNotificationProducer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Global duplicate detection and merging. Detection runs as a separate step AFTER a scrape run
 * (not in the scrape transaction): it scans freshly-active listings, suggests probable duplicates as
 * {@link DuplicateGroup}s, and notifies savers when a saved listing looks reposted. Merge decisions are
 * global and taken by an admin via {@link #confirm}/{@link #reject}.
 */
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);

    /** Probable-duplicate search radius (±1 km), applied by the PostGIS query. */
    private static final double GEO_RADIUS_METERS = 1000.0;

    private final ListingRepository listingRepository;
    private final SavedListingRepository savedListingRepository;
    private final DuplicateGroupRepository groupRepository;
    private final DuplicateGroupMemberRepository memberRepository;
    private final RepostNotificationProducer repostNotificationProducer;

    /**
     * Scan listings that became active at or after {@code since} (i.e. touched by the latest scrape)
     * and, for each, look for probable duplicates among existing listings.
     */
    @Transactional
    public void scanRecentlyActive(Instant since) {
        List<Listing> subjects = listingRepository.findByStatusAndLastSeenAtGreaterThanEqual(ListingStatus.ACTIVE, since);
        log.info("Deduplication scan over {} recently-active listings", subjects.size());
        subjects.forEach(this::scan);
    }

    private void scan(Listing subject) {
        if (subject.getLat() == null || subject.getLng() == null) {
            return; // geo is a hard requirement for the match criteria
        }
        listingRepository.findGeoCandidates(
                        subject.getCategory().name(), subject.getId(),
                        subject.getLng(), subject.getLat(), GEO_RADIUS_METERS)
                .stream()
                .filter(candidate -> DuplicateMatcher.isProbableDuplicate(subject, candidate))
                .forEach(candidate -> reconcile(subject, candidate));
    }

    private void reconcile(Listing subject, Listing candidate) {
        if (isRepost(subject, candidate)) {
            notifyRepost(subject, candidate);
        } else {
            suggest(candidate, subject); // primary = existing candidate, member = new subject
        }
    }

    /** A relisting: same source, and the matched original has gone inactive. */
    private boolean isRepost(Listing subject, Listing candidate) {
        return subject.getSource() == candidate.getSource() && candidate.getStatus() == ListingStatus.INACTIVE;
    }

    private void notifyRepost(Listing reposted, Listing original) {
        List<SavedListing> savers = savedListingRepository.findByListing_Id(original.getId());
        savers.forEach(saved -> repostNotificationProducer.notifyReposted(saved.getUser().getId(), reposted.getId()));
        if (!savers.isEmpty()) {
            log.info("Repost of listing {} -> {}: notified {} saver(s)",
                    original.getId(), reposted.getId(), savers.size());
        }
    }

    private void suggest(Listing primary, Listing duplicate) {
        // Skip pairs already related in any group (suggested, confirmed or rejected).
        if (memberRepository.pairLinked(primary.getId(), duplicate.getId())) {
            return;
        }
        DuplicateGroup group = groupRepository
                .findByPrimaryListing_IdAndStatus(primary.getId(), DuplicateStatus.SUGGESTED)
                .orElseGet(() -> createGroup(primary));

        DuplicateGroupMember member = new DuplicateGroupMember();
        member.setGroup(group);
        member.setListing(duplicate);
        memberRepository.save(member);
        log.info("Suggested duplicate: listing {} -> primary {}", duplicate.getId(), primary.getId());
    }

    private DuplicateGroup createGroup(Listing primary) {
        DuplicateGroup group = new DuplicateGroup();
        group.setPrimaryListing(primary);
        group.setStatus(DuplicateStatus.SUGGESTED);
        return groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroupResponse> listSuggested() {
        return groupRepository.findByStatus(DuplicateStatus.SUGGESTED).stream()
                .map(group -> DuplicateGroupMapper.toResponse(group, memberRepository.findByGroup_Id(group.getId())))
                .toList();
    }

    /**
     * Confirm a suggestion (admin, global): members become MERGED and disappear from listings, the primary
     * stays. SavedListing rows pointing at a merged listing are re-pointed to the primary (or dropped if the
     * user already saved the primary, to respect the user+listing uniqueness).
     */
    @Transactional
    public void confirm(UUID groupId) {
        DuplicateGroup group = requireSuggested(groupId);
        Listing primary = group.getPrimaryListing();
        group.setStatus(DuplicateStatus.CONFIRMED);

        memberRepository.findByGroup_Id(groupId).stream()
                .map(DuplicateGroupMember::getListing)
                .forEach(merged -> merge(merged, primary));
        log.info("Confirmed duplicate group {} (primary {})", groupId, primary.getId());
    }

    private void merge(Listing merged, Listing primary) {
        merged.setStatus(ListingStatus.MERGED);
        savedListingRepository.findByListing_Id(merged.getId())
                .forEach(saved -> repoint(saved, primary));
    }

    private void repoint(SavedListing saved, Listing primary) {
        UUID userId = saved.getUser().getId();
        boolean alreadyHasPrimary = savedListingRepository
                .findByUser_IdAndListing_Id(userId, primary.getId()).isPresent();
        if (alreadyHasPrimary) {
            savedListingRepository.delete(saved); // user already saved the primary; drop the duplicate
        } else {
            saved.setListing(primary); // dirty checking flushes the change
        }
    }

    /** Reject a suggestion (admin, global): the pair is never suggested again. */
    @Transactional
    public void reject(UUID groupId) {
        DuplicateGroup group = requireSuggested(groupId);
        group.setStatus(DuplicateStatus.REJECTED);
        log.info("Rejected duplicate group {}", groupId);
    }

    private DuplicateGroup requireSuggested(UUID groupId) {
        DuplicateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> ResourceNotFoundException.of("Duplicate group", groupId));
        if (group.getStatus() != DuplicateStatus.SUGGESTED) {
            throw ResourceNotFoundException.of("Suggested duplicate group", groupId);
        }
        return group;
    }
}
