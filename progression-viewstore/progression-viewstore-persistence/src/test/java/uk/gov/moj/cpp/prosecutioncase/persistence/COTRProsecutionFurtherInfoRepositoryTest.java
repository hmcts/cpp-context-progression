package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRProsecutionFurtherInfoRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class COTRProsecutionFurtherInfoRepositoryTest {

    @Inject
    private COTRProsecutionFurtherInfoRepository cotrProsecutionFurtherInfoRepository;

    @Test
    public void shouldSaveAndReadCOTRProsecutionFurtherInfo() {

        final UUID id = randomUUID();
        final UUID cotrId = randomUUID();
        final String furtherInformation = "furtherInformation";
        final UUID infoAddedBy = randomUUID();
        final ZonedDateTime addedOn = ZonedDateTime.now();

        COTRProsecutionFurtherInfoEntity entity = new COTRProsecutionFurtherInfoEntity(id, cotrId, furtherInformation, infoAddedBy, addedOn, Boolean.TRUE, "Erica");
        cotrProsecutionFurtherInfoRepository.save(entity);

        COTRProsecutionFurtherInfoEntity entity1 = new COTRProsecutionFurtherInfoEntity();
        entity1.setId(id);
        entity1.setCotrId(cotrId);
        entity1.setFurtherInformation(furtherInformation);
        entity1.setInfoAddedBy(infoAddedBy);
        entity1.setAddedOn(addedOn);
        entity1.setIsCertificationReady(Boolean.TRUE);
        entity1.setInfoAddedByName("Erica");
        cotrProsecutionFurtherInfoRepository.save(entity1);

        final COTRProsecutionFurtherInfoEntity cotrProsecutionFurtherInfoEntity = cotrProsecutionFurtherInfoRepository.findBy(id);
        verifyCOTRProsecutionFurtherInfo(cotrProsecutionFurtherInfoEntity, entity);

    }

    private void verifyCOTRProsecutionFurtherInfo(final COTRProsecutionFurtherInfoEntity actual, final COTRProsecutionFurtherInfoEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getCotrId(), equalTo(expected.getCotrId()));
        assertThat(actual.getFurtherInformation(), equalTo(expected.getFurtherInformation()));
        assertThat(actual.getInfoAddedBy(), equalTo(expected.getInfoAddedBy()));
        assertThat(actual.getAddedOn(), equalTo(expected.getAddedOn()));
    }
}