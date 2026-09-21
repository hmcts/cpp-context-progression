package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.COTRProsecutionFurtherInfoEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.COTRProsecutionFurtherInfoRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class COTRProsecutionFurtherInfoRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");

    private COTRProsecutionFurtherInfoRepository cotrProsecutionFurtherInfoRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        cotrProsecutionFurtherInfoRepository = new COTRProsecutionFurtherInfoRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cotrProsecutionFurtherInfoRepository);
    }

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

    private COTRProsecutionFurtherInfoEntity furtherInfoFor(final UUID cotrId, final String furtherInformation) {
        return new COTRProsecutionFurtherInfoEntity(randomUUID(), cotrId, furtherInformation,
                randomUUID(), ZonedDateTime.now(), Boolean.TRUE, "Erica");
    }

    @Test
    public void shouldFindEveryFurtherInfoRecordedAgainstACotr() {
        final UUID cotrId = randomUUID();
        cotrProsecutionFurtherInfoRepository.saveAndFlush(furtherInfoFor(cotrId, "first note"));
        cotrProsecutionFurtherInfoRepository.saveAndFlush(furtherInfoFor(cotrId, "second note"));
        cotrProsecutionFurtherInfoRepository.saveAndFlush(furtherInfoFor(randomUUID(), "another cotr's note"));

        final List<COTRProsecutionFurtherInfoEntity> found =
                cotrProsecutionFurtherInfoRepository.findByCotrId(cotrId);

        assertThat(found.size(), equalTo(2));
        assertThat(found.stream().map(COTRProsecutionFurtherInfoEntity::getFurtherInformation).sorted().toList(),
                equalTo(List.of("first note", "second note")));
    }

    @Test
    public void shouldReturnNothingWhenTheCotrHasNoFurtherInfo() {
        assertThat(cotrProsecutionFurtherInfoRepository.findByCotrId(randomUUID()).isEmpty(), equalTo(true));
    }

    private void verifyCOTRProsecutionFurtherInfo(final COTRProsecutionFurtherInfoEntity actual, final COTRProsecutionFurtherInfoEntity expected) {
        assertThat(actual.getId(), equalTo(expected.getId()));
        assertThat(actual.getCotrId(), equalTo(expected.getCotrId()));
        assertThat(actual.getFurtherInformation(), equalTo(expected.getFurtherInformation()));
        assertThat(actual.getInfoAddedBy(), equalTo(expected.getInfoAddedBy()));
        assertThat(actual.getAddedOn(), equalTo(expected.getAddedOn()));
    }
}