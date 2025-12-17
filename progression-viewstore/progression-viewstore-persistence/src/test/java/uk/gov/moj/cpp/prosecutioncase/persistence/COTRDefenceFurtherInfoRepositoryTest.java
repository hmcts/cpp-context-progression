package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefenceFurtherInfoRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class COTRDefenceFurtherInfoRepositoryTest {

    @Inject
    private COTRDefenceFurtherInfoRepository cotrDefenceFurtherInfoRepository;

    @Test
    public void shouldSaveAndReadCOTRDefenceFurtherInfo() {

        final UUID id = randomUUID();
        final UUID cotrDefendantId = randomUUID();
        final String furtherInformation = "furtherInformation";
        final Boolean isCertificationReady = Boolean.TRUE;
        final UUID infoAddedBy = randomUUID();
        final String infoAddedByName = "James Turner";
        final ZonedDateTime addedOn = ZonedDateTime.now();

        final COTRDefenceFurtherInfoEntity entity = new COTRDefenceFurtherInfoEntity(id, cotrDefendantId, furtherInformation, isCertificationReady, infoAddedBy, infoAddedByName, addedOn);

        cotrDefenceFurtherInfoRepository.save(entity);

        final COTRDefenceFurtherInfoEntity cotrDefenceFurtherInfoEntity = cotrDefenceFurtherInfoRepository.findBy(id);
        verifyCOTRDefenceFurtherInfo(cotrDefenceFurtherInfoEntity, entity);

    }

    private void verifyCOTRDefenceFurtherInfo(final COTRDefenceFurtherInfoEntity actual, final COTRDefenceFurtherInfoEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getCotrDefendantId(), equalTo(expected.getCotrDefendantId()));
        assertThat(actual.getFurtherInformation(), equalTo(expected.getFurtherInformation()));
        assertThat(actual.getIsCertificationReady(), equalTo(expected.getIsCertificationReady()));
        assertThat(actual.getInfoAddedBy(), equalTo(expected.getInfoAddedBy()));
        assertThat(actual.getInfoAddedByName(), equalTo(expected.getInfoAddedByName()));
        assertThat(actual.getAddedOn(), equalTo(expected.getAddedOn()));
    }
}