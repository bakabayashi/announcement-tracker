package pl.panzerhund.tracker.deduplication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.common.exception.ResourceNotFoundException;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroup;
import pl.panzerhund.tracker.deduplication.entity.DuplicateGroupMember;
import pl.panzerhund.tracker.deduplication.entity.DuplicateStatus;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.SavedListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.SavedListing;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.notification.RepostNotificationProducer;
import pl.panzerhund.tracker.user.entity.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeduplicationServiceTest {

    private ListingRepository listingRepository;
    private SavedListingRepository savedListingRepository;
    private DuplicateGroupRepository groupRepository;
    private DuplicateGroupMemberRepository memberRepository;
    private RepostNotificationProducer repostNotificationProducer;
    private DeduplicationService service;

    @BeforeEach
    void setUp() {
        listingRepository = mock(ListingRepository.class);
        savedListingRepository = mock(SavedListingRepository.class);
        groupRepository = mock(DuplicateGroupRepository.class);
        memberRepository = mock(DuplicateGroupMemberRepository.class);
        repostNotificationProducer = mock(RepostNotificationProducer.class);
        service = new DeduplicationService(listingRepository, savedListingRepository,
                groupRepository, memberRepository, repostNotificationProducer);
    }

    private Listing listing(Source source, ListingStatus status) {
        Listing l = new Listing();
        l.setId(UUID.randomUUID());
        l.setSource(source);
        l.setCategory(Category.PLOT);
        l.setStatus(status);
        l.setLat(52.0);
        l.setLng(21.0);
        l.setPrice(new BigDecimal("100000"));
        l.setCurrency("PLN");
        l.setAttributes(Map.<String, Object>of("area", "1000"));
        return l;
    }

    private void scanReturns(Listing subject, Listing... candidates) {
        when(listingRepository.findByStatusAndLastSeenAtGreaterThanEqual(eq(ListingStatus.ACTIVE), any()))
                .thenReturn(List.of(subject));
        when(listingRepository.findGeoCandidates(any(), any(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(candidates));
    }

    @Test
    void suggestsDuplicateForMatchingActiveCandidate() {
        Listing subject = listing(Source.OTODOM, ListingStatus.ACTIVE);
        Listing candidate = listing(Source.OLX, ListingStatus.ACTIVE);
        scanReturns(subject, candidate);
        when(memberRepository.pairLinked(candidate.getId(), subject.getId())).thenReturn(false);
        when(groupRepository.findByPrimaryListing_IdAndStatus(candidate.getId(), DuplicateStatus.SUGGESTED))
                .thenReturn(Optional.empty());
        when(groupRepository.save(any(DuplicateGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        service.scanRecentlyActive(Instant.now());

        // Primary is the existing candidate; the new subject becomes the member.
        verify(groupRepository).save(any(DuplicateGroup.class));
        verify(memberRepository).save(any(DuplicateGroupMember.class));
        verify(repostNotificationProducer, never()).notifyReposted(any(), any());
    }

    @Test
    void doesNotResuggestAnAlreadyLinkedPair() {
        Listing subject = listing(Source.OTODOM, ListingStatus.ACTIVE);
        Listing candidate = listing(Source.OLX, ListingStatus.ACTIVE);
        scanReturns(subject, candidate);
        when(memberRepository.pairLinked(candidate.getId(), subject.getId())).thenReturn(true);

        service.scanRecentlyActive(Instant.now());

        verify(groupRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void extendsExistingSuggestionInsteadOfCreatingANewGroup() {
        Listing subject = listing(Source.OTODOM, ListingStatus.ACTIVE);
        Listing candidate = listing(Source.OLX, ListingStatus.ACTIVE);
        scanReturns(subject, candidate);
        DuplicateGroup existing = new DuplicateGroup();
        existing.setPrimaryListing(candidate);
        existing.setStatus(DuplicateStatus.SUGGESTED);
        when(memberRepository.pairLinked(candidate.getId(), subject.getId())).thenReturn(false);
        when(groupRepository.findByPrimaryListing_IdAndStatus(candidate.getId(), DuplicateStatus.SUGGESTED))
                .thenReturn(Optional.of(existing));

        service.scanRecentlyActive(Instant.now());

        verify(groupRepository, never()).save(any());
        verify(memberRepository).save(any(DuplicateGroupMember.class));
    }

    @Test
    void repostOfSavedListingNotifiesSaversAndCreatesNoGroup() {
        Listing subject = listing(Source.OLX, ListingStatus.ACTIVE);     // new relisting
        Listing original = listing(Source.OLX, ListingStatus.INACTIVE);  // same source, gone inactive
        scanReturns(subject, original);
        User saver = new User();
        saver.setId(UUID.randomUUID());
        SavedListing saved = new SavedListing();
        saved.setUser(saver);
        saved.setListing(original);
        when(savedListingRepository.findByListing_Id(original.getId())).thenReturn(List.of(saved));

        service.scanRecentlyActive(Instant.now());

        verify(repostNotificationProducer).notifyReposted(saver.getId(), subject.getId());
        verify(groupRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
    }

    @Test
    void confirmMergesMembersAndRepointsSavedListings() {
        UUID groupId = UUID.randomUUID();
        Listing primary = listing(Source.OLX, ListingStatus.ACTIVE);
        Listing merged = listing(Source.OTODOM, ListingStatus.ACTIVE);
        DuplicateGroup group = new DuplicateGroup();
        group.setPrimaryListing(primary);
        group.setStatus(DuplicateStatus.SUGGESTED);
        DuplicateGroupMember member = new DuplicateGroupMember();
        member.setListing(merged);
        User saver = new User();
        saver.setId(UUID.randomUUID());
        SavedListing saved = new SavedListing();
        saved.setUser(saver);
        saved.setListing(merged);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_Id(groupId)).thenReturn(List.of(member));
        when(savedListingRepository.findByListing_Id(merged.getId())).thenReturn(List.of(saved));
        when(savedListingRepository.findByUser_IdAndListing_Id(saver.getId(), primary.getId()))
                .thenReturn(Optional.empty());

        service.confirm(groupId);

        assertThat(group.getStatus()).isEqualTo(DuplicateStatus.CONFIRMED);
        assertThat(merged.getStatus()).isEqualTo(ListingStatus.MERGED);
        assertThat(primary.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(saved.getListing()).isEqualTo(primary); // re-pointed
        verify(savedListingRepository, never()).delete(any());
    }

    @Test
    void confirmDropsSavedListingWhenUserAlreadySavedThePrimary() {
        UUID groupId = UUID.randomUUID();
        Listing primary = listing(Source.OLX, ListingStatus.ACTIVE);
        Listing merged = listing(Source.OTODOM, ListingStatus.ACTIVE);
        DuplicateGroup group = new DuplicateGroup();
        group.setPrimaryListing(primary);
        group.setStatus(DuplicateStatus.SUGGESTED);
        DuplicateGroupMember member = new DuplicateGroupMember();
        member.setListing(merged);
        User saver = new User();
        saver.setId(UUID.randomUUID());
        SavedListing dup = new SavedListing();
        dup.setUser(saver);
        dup.setListing(merged);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_Id(groupId)).thenReturn(List.of(member));
        when(savedListingRepository.findByListing_Id(merged.getId())).thenReturn(List.of(dup));
        when(savedListingRepository.findByUser_IdAndListing_Id(saver.getId(), primary.getId()))
                .thenReturn(Optional.of(new SavedListing()));

        service.confirm(groupId);

        verify(savedListingRepository).delete(dup);
    }

    @Test
    void rejectMarksGroupRejected() {
        UUID groupId = UUID.randomUUID();
        DuplicateGroup group = new DuplicateGroup();
        group.setStatus(DuplicateStatus.SUGGESTED);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        service.reject(groupId);

        assertThat(group.getStatus()).isEqualTo(DuplicateStatus.REJECTED);
    }

    @Test
    void confirmOnNonSuggestedGroupIsRejected() {
        UUID groupId = UUID.randomUUID();
        DuplicateGroup group = new DuplicateGroup();
        group.setStatus(DuplicateStatus.CONFIRMED);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> service.confirm(groupId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void confirmOnMissingGroupThrowsNotFound() {
        UUID groupId = UUID.randomUUID();
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(groupId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
