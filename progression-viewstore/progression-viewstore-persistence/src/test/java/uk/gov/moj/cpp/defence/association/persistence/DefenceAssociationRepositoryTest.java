package uk.gov.moj.cpp.defence.association.persistence;

import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static uk.gov.justice.core.courts.FundingType.REPRESENTATION_ORDER;

import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.NoResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefenceAssociationRepositoryTest {

    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID ORGANISATION_ID = randomUUID();
    private static final String LAA_CONTRACT_NUMBER = "123-ABC-456-XYZ";
    private static final String UTC = "UTC";


    @Inject
    private DefenceAssociationRepository repository;

    @Before
    public void setUp() {
        repository.findAll().stream().forEach(defenceAssociation -> repository.removeAndFlush(defenceAssociation));
    }

    @Test(expected = NoResultException.class)
    public void shouldReturnExceptionIfNoAssociationExist() {
        repository.findByDefendantId(DEFENDANT_ID);
    }

    @Test
    public void shouldPerformAssociation() throws Exception {

        //Given
        final DefenceAssociationDefendant defenceAssociationDefendant = getDefenceAssociation();

        //When
        repository.save(defenceAssociationDefendant);

        //Then
        final DefenceAssociationDefendant actual = repository.findByDefendantId(DEFENDANT_ID);
        assertNotNull("Should not be null", actual);

        assertEquals(DEFENDANT_ID, actual.getDefendantId());
        assertEquals(1, actual.getDefenceAssociations().size());

        final DefenceAssociation association = actual.getDefenceAssociations().iterator().next();
        assertEquals(ORGANISATION_ID, association.getOrgId());
        assertEquals(USER_ID, association.getUserId());
        assertEquals(REPRESENTATION_ORDER.toString(), association.getRepresentationType());
        assertEquals(LAA_CONTRACT_NUMBER, association.getLaaContractNumber());
    }

    @Test
    public void shoulPerformDisAssociation() throws Exception {

        //Given
        final DefenceAssociationDefendant defenceAssociationDefendant = getDefenceAssociation();
        final ZonedDateTime testZonedDateTime = now(of(UTC));

        //...performing the Association
        repository.save(defenceAssociationDefendant);
        final DefenceAssociationDefendant associatedCase = repository.findByDefendantId(DEFENDANT_ID);
        assertNotNull("Should not be null", associatedCase);
        assertNull(associatedCase.getDefenceAssociations().iterator().next().getEndDate());

        //When
        //...a Disassociated Case has an active End Date....
        performDisassociation(testZonedDateTime, associatedCase);

        //...perform Disassociation
        repository.save(associatedCase);

        //Then
        final DefenceAssociationDefendant disassociatedCase = repository.findByDefendantId(DEFENDANT_ID);
        assertEquals(DEFENDANT_ID, disassociatedCase.getDefendantId());
        assertEquals(ORGANISATION_ID, disassociatedCase.getDefenceAssociations().iterator().next().getOrgId());
        assertEquals(USER_ID, disassociatedCase.getDefenceAssociations().iterator().next().getUserId());
        assertEquals(testZonedDateTime, disassociatedCase.getDefenceAssociations().iterator().next().getEndDate());

    }

    private void performDisassociation(final ZonedDateTime testZonedDateTime, final DefenceAssociationDefendant associatedCase) {
        final DefenceAssociation defenceAssociation = associatedCase.getDefenceAssociations().iterator().next();
        defenceAssociation.setEndDate(testZonedDateTime);
        associatedCase.getDefenceAssociations().clear();
        associatedCase.getDefenceAssociations().add(defenceAssociation);
    }

    private DefenceAssociationDefendant getDefenceAssociation() {

        final DefenceAssociationDefendant defenceAssociationDefendant = new DefenceAssociationDefendant();
        defenceAssociationDefendant.setDefendantId(DEFENDANT_ID);

        final DefenceAssociation defenceAssociation = new DefenceAssociation();
        defenceAssociation.setId(randomUUID());
        defenceAssociation.setDefenceAssociationDefendant(defenceAssociationDefendant);
        defenceAssociation.setUserId(USER_ID);
        defenceAssociation.setOrgId(ORGANISATION_ID);
        defenceAssociation.setStartDate(now(of(UTC)));
        defenceAssociation.setRepresentationType(REPRESENTATION_ORDER.toString());
        defenceAssociation.setLaaContractNumber(LAA_CONTRACT_NUMBER);

        defenceAssociationDefendant.setDefenceAssociations(new HashSet<>());
        defenceAssociationDefendant.getDefenceAssociations().add(defenceAssociation);

        return defenceAssociationDefendant;
    }
}
