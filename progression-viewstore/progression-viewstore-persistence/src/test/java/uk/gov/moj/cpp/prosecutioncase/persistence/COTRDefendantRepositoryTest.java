package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefendantEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefendantRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class COTRDefendantRepositoryTest {

    @Inject
    private COTRDefendantRepository cotrDefendantRepository;

    @Test
    public void shouldSaveAndReadCOTRDefendant() {

        final UUID id = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();

        final Integer dNumber = 1;
        final String defendantForm = "defendantForm";
        final UUID servedBy = randomUUID();
        final ZonedDateTime servedOn = ZonedDateTime.now();

        COTRDefendantEntity entity1 = new COTRDefendantEntity(id, cotrId, defendantId1, dNumber, defendantForm, servedBy, servedOn,"name");

        cotrDefendantRepository.save(entity1);

        final COTRDefendantEntity cotrDetailsEntity1 = cotrDefendantRepository.findBy(id);
        verifyCOTRDefendant(cotrDetailsEntity1, entity1);

        COTRDefendantEntity entity2 = new COTRDefendantEntity();
        entity2.setId(id);
        entity2.setCotrId(cotrId);
        entity2.setDefendantId(defendantId2);
        entity2.setdNumber(dNumber);
        entity2.setDefendantForm(defendantForm);
        entity2.setServedBy(servedBy);
        entity2.setServedOn(servedOn);
        entity2.setServedByName("name");

        cotrDefendantRepository.save(entity2);

        final COTRDefendantEntity cotrDetailsEntity2 = cotrDefendantRepository.findBy(id);
        verifyCOTRDefendant(cotrDetailsEntity2, entity2);

        List<COTRDefendantEntity>  cotrDefendantEntities = cotrDefendantRepository.findByCotrIdAndDefendantId(cotrId, defendantId2);
        assertThat(cotrDefendantEntities, hasSize(equalTo(1)));
    }

    @Test
    public void shouldFindCOTRDefendantsForCotrId() {

        final UUID cotrId1 = randomUUID();
        final UUID cotrId2 = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID defendantId4 = randomUUID();

        final Integer dNumber = 1;
        final String defendantForm = "defendantForm";
        final UUID servedBy = randomUUID();
        final ZonedDateTime servedOn = ZonedDateTime.now();

        final COTRDefendantEntity entity1 = new COTRDefendantEntity(randomUUID(), cotrId1, defendantId1, dNumber, defendantForm, servedBy, servedOn,"name");
        final COTRDefendantEntity entity2 = new COTRDefendantEntity(randomUUID(), cotrId1, defendantId2, dNumber, defendantForm, servedBy, servedOn,"name");
        final COTRDefendantEntity entity3 = new COTRDefendantEntity(randomUUID(), cotrId2, defendantId3, dNumber, defendantForm, servedBy, servedOn,"name");
        final COTRDefendantEntity entity4 = new COTRDefendantEntity(randomUUID(), cotrId2, defendantId4, dNumber, defendantForm, servedBy, servedOn,"name");

        cotrDefendantRepository.save(entity1);
        cotrDefendantRepository.save(entity2);
        cotrDefendantRepository.save(entity3);
        cotrDefendantRepository.save(entity4);

        final List<COTRDefendantEntity>  cotrDefendantEntities = cotrDefendantRepository.findByCotrId(cotrId1);
        assertThat(cotrDefendantEntities.size(), is(2));
    }

    private void verifyCOTRDefendant(final COTRDefendantEntity actual, final COTRDefendantEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getCotrId(), equalTo(expected.getCotrId()));
        assertThat(actual.getDefendantId(), equalTo(expected.getDefendantId()));
        assertThat(actual.getdNumber(), equalTo(expected.getdNumber()));
        assertThat(actual.getDefendantForm(), equalTo(expected.getDefendantForm()));
        assertThat(actual.getServedBy(), equalTo(expected.getServedBy()));
        assertThat(actual.getServedOn(), equalTo(expected.getServedOn()));
    }
}