package uk.gov.moj.cpp.prosecutioncase.persistence.repository;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SharedAllCourtDocumentsEntity;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link SharedAllCourtDocumentsRepository} class
 */


@RunWith(CdiTestRunner.class)
public class SharedAllCourtDocumentsRepositoryTest {
    @Inject
    private SharedAllCourtDocumentsRepository sharedAllCourtDocumentsRepository;

    @Test
    public void shouldFindByCaseIdAndDefendantIdAndHearingIdAndUserGroupIdsAndUserId() {
        final UUID id = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID userGroupId = randomUUID();
        final UUID userId = randomUUID();
        final UUID sharedByUser = randomUUID();
        final ZonedDateTime dateShared = ZonedDateTime.now();
        final SharedAllCourtDocumentsEntity sharedAllCourtDocumentsEntity = new SharedAllCourtDocumentsEntity(id, caseId, defendantId, hearingId, userGroupId, userId, sharedByUser, dateShared);

        assertTrue(sharedAllCourtDocumentsRepository.findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(caseId, hearingId, defendantId, Arrays.asList(userGroupId, randomUUID()), userId).isEmpty());
        sharedAllCourtDocumentsRepository.save(sharedAllCourtDocumentsEntity);
        final List<SharedAllCourtDocumentsEntity> result = sharedAllCourtDocumentsRepository.findByCaseIdAndHearingIdAndDefendantIdAndUserGroupsAndUserId(caseId, hearingId, defendantId , Arrays.asList(userGroupId, randomUUID()), randomUUID());

        assertThat(result.size(), is(1));
        final  SharedAllCourtDocumentsEntity entity = result.get(0);
        assertThat(entity.getId(), is(id));
        assertThat(entity.getCaseId(), is(caseId));
        assertThat(entity.getDefendantId(), is(defendantId));
        assertThat(entity.getApplicationHearingId(), is(hearingId));
        assertThat(entity.getUserId(), is(userId));
        assertThat(entity.getUserGroupId(), is(userGroupId));
        assertThat(entity.getDateShared(), is(dateShared));
        assertThat(entity.getSharedByUser(), is(sharedByUser));
    }
}
