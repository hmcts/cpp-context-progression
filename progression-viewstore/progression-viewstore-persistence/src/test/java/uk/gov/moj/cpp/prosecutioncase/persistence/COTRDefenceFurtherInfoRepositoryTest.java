package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRDefenceFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRDefenceFurtherInfoRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class COTRDefenceFurtherInfoRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private COTRDefenceFurtherInfoRepository cotrDefenceFurtherInfoRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        cotrDefenceFurtherInfoRepository = new COTRDefenceFurtherInfoRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cotrDefenceFurtherInfoRepository);
    }

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

    private COTRDefenceFurtherInfoEntity furtherInfoFor(final UUID cotrDefendantId, final String furtherInformation) {
        return new COTRDefenceFurtherInfoEntity(randomUUID(), cotrDefendantId, furtherInformation,
                Boolean.TRUE, randomUUID(), "James Turner", ZonedDateTime.now());
    }

    @Test
    public void shouldFindEveryFurtherInfoRecordedAgainstACotrDefendant() {
        final UUID cotrDefendantId = randomUUID();
        cotrDefenceFurtherInfoRepository.saveAndFlush(furtherInfoFor(cotrDefendantId, "first note"));
        cotrDefenceFurtherInfoRepository.saveAndFlush(furtherInfoFor(cotrDefendantId, "second note"));
        cotrDefenceFurtherInfoRepository.saveAndFlush(furtherInfoFor(randomUUID(), "another defendant's note"));

        final List<COTRDefenceFurtherInfoEntity> found =
                cotrDefenceFurtherInfoRepository.findByCotrDefendantId(cotrDefendantId);

        assertThat(found.size(), equalTo(2));
        assertThat(found.stream().map(COTRDefenceFurtherInfoEntity::getFurtherInformation).sorted().toList(),
                equalTo(List.of("first note", "second note")));
    }

    @Test
    public void shouldReturnNothingWhenTheCotrDefendantHasNoFurtherInfo() {
        assertThat(cotrDefenceFurtherInfoRepository.findByCotrDefendantId(randomUUID()).isEmpty(), equalTo(true));
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