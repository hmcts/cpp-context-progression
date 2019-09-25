package uk.gov.moj.cpp.defence.association.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationHistory;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefenceAssociationRepositoryTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ORGANISATION_ID = UUID.randomUUID();

    @Inject
    private DefenceAssociationRepository repository;

    @Before
    public void setUp() {
        repository.findAll().stream().forEach(defenceAssociation -> repository.removeAndFlush(defenceAssociation));
    }

    @Test
    public void shouldPerformAssociation () throws Exception {

        repository.save(getDefenceAssociation());

        DefenceAssociation actual = repository.findByDefendantId(DEFENDANT_ID);
        assertNotNull("Should not be null", actual);

        assertEquals(DEFENDANT_ID, actual.getDefendantId());
        assertEquals(1, actual.getDefenceAssociationHistories().size());
        assertEquals(ORGANISATION_ID, actual.getDefenceAssociationHistories().iterator().next().getGrantorOrgId());
        assertEquals(USER_ID, actual.getDefenceAssociationHistories().iterator().next().getGrantorUserId());

    }

    @Test
    public void shoulPerformDisAssociation () throws Exception {

        ZonedDateTime testZonedDateTime = ZonedDateTime.now();

        //Performing the Association
        repository.save(getDefenceAssociation());

        DefenceAssociation associatedCase = repository.findByDefendantId(DEFENDANT_ID);
        assertNotNull("Should not be null", associatedCase);
        assertNull(associatedCase.getDefenceAssociationHistories().iterator().next().getEndDate());

        //A Disassociated Case has an active End Date....
        performDisassociation(testZonedDateTime, associatedCase);

        //Perform Disassociation
        repository.save(associatedCase);

        DefenceAssociation disAsssociatedCase = repository.findByDefendantId(DEFENDANT_ID);

        //Check Values of the Disassociated Case...
        assertEquals(DEFENDANT_ID, disAsssociatedCase.getDefendantId());
        assertEquals(ORGANISATION_ID, disAsssociatedCase.getDefenceAssociationHistories().iterator().next().getGrantorOrgId());
        assertEquals(USER_ID, disAsssociatedCase.getDefenceAssociationHistories().iterator().next().getGrantorUserId());
        assertEquals(testZonedDateTime, disAsssociatedCase.getDefenceAssociationHistories().iterator().next().getEndDate());

    }

    @Test(expected = NoResultException.class)
    public void shouldReturnExceptionIfNoAssociationExist() {
        repository.findByDefendantId(DEFENDANT_ID);
    }

    private void performDisassociation(final ZonedDateTime testZonedDateTime, final DefenceAssociation associatedCase) {
        DefenceAssociationHistory defenceAssociationHistory =  associatedCase.getDefenceAssociationHistories().iterator().next();
        defenceAssociationHistory.setEndDate(testZonedDateTime);
        associatedCase.getDefenceAssociationHistories().clear();
        associatedCase.getDefenceAssociationHistories().add(defenceAssociationHistory);
    }

    private DefenceAssociation getDefenceAssociation() {

        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setDefendantId(DEFENDANT_ID);

        final DefenceAssociationHistory defenceAssociationHistory = new DefenceAssociationHistory();
        defenceAssociationHistory.setId(UUID.randomUUID());
        defenceAssociationHistory.setDefenceAssociation(defenceAssociation);
        defenceAssociationHistory.setGrantorUserId(USER_ID);
        defenceAssociationHistory.setGrantorOrgId(ORGANISATION_ID);
        defenceAssociationHistory.setStartDate(ZonedDateTime.now());

        defenceAssociation.setDefenceAssociationHistories(new HashSet<>());
        defenceAssociation.getDefenceAssociationHistories().add(defenceAssociationHistory);

        return defenceAssociation;
    }
}
