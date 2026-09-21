package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantLAAAssociationRepository;

import java.util.List;
import java.util.UUID;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DefendantLAAAssociationRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private static final UUID DEFENDANT_ID = randomUUID();

    private static final UUID DEFENDANT_ID_2 = randomUUID();

    private static final String LAA_CONTRACT_NUMBER = "LAA1234";

        private DefendantLAAAssociationRepository defendantLAAAssociationRepository;
    @BeforeEach
    public void setUp() {
        defendantLAAAssociationRepository = new DefendantLAAAssociationRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantLAAAssociationRepository);
        final DefendantLAAAssociationEntity defendantLAAAssociationEntity = new DefendantLAAAssociationEntity();
        defendantLAAAssociationEntity.setDefendantLAAKey(new DefendantLAAKey(DEFENDANT_ID, LAA_CONTRACT_NUMBER));
        defendantLAAAssociationEntity.setAssociatedByLAA(false);
        defendantLAAAssociationRepository.save(defendantLAAAssociationEntity);
        final DefendantLAAAssociationEntity defendantLAAAssociationEntity2 = new DefendantLAAAssociationEntity();
        defendantLAAAssociationEntity2.setDefendantLAAKey(new DefendantLAAKey(DEFENDANT_ID_2, LAA_CONTRACT_NUMBER));
        defendantLAAAssociationEntity2.setAssociatedByLAA(false);
        defendantLAAAssociationRepository.save(defendantLAAAssociationEntity2);

    }

    @Test
    public void shouldGetDefenceLAAAssociationByPrimaryKey() {
        final DefendantLAAKey defendantLAAKey = new DefendantLAAKey(DEFENDANT_ID, LAA_CONTRACT_NUMBER);
        final DefendantLAAAssociationEntity actualEntity = defendantLAAAssociationRepository.findBy(defendantLAAKey);
        assertThat(actualEntity.getDefendantLAAKey().getDefendantId(), is(DEFENDANT_ID));
    }

    @Test
    public void shouldGetDefenceLAAAssociationByLAAContractNumber() {
        final List<DefendantLAAAssociationEntity> entityList = defendantLAAAssociationRepository.findByLAAContractNUmber(LAA_CONTRACT_NUMBER);
        assertThat(entityList.get(0).getDefendantLAAKey().getDefendantId(), is(DEFENDANT_ID));
        assertThat(entityList.get(1).getDefendantLAAKey().getDefendantId(), is(DEFENDANT_ID_2));
    }
}
