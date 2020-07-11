package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InformantRegisterRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class InformantRegisterRepositoryTest {
    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime REGISTER_TIME_1 = ZonedDateTime.now();
    private static final ZonedDateTime REGISTER_TIME_2 = ZonedDateTime.now().plusHours(1);

    @Inject
    private InformantRegisterRepository informantRegisterRepository;


    @Before
    public void setUp() {
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_1));
        informantRegisterRepository.save(createInformantRegister(REGISTER_TIME_2));
    }

    @After
    public void tearDown() {
        List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findAll();
        informantRegisterEntities.forEach(ir -> informantRegisterRepository.remove(ir));
    }

    @Test
    public void shouldFindTheInformantRegisterRequestsByStatus() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByProsecutionAuthorityIdAndStatusRecorded(PROSECUTION_AUTHORITY_ID);
        assertThat(informantRegisterEntities.size(), is(1));
    }


    @Test
    public void shouldFindTheInformantRegisterRequestsByDateAndProsecutionAuthority() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(LocalDate.now(), "TFL");
        assertThat(informantRegisterEntities.size(), is(1));

    }

    @Test
    public void shouldFindTheInformantRegisterRequestsByDate() {
        final List<InformantRegisterEntity> informantRegisterEntities =
                informantRegisterRepository.findByRegisterDate(LocalDate.now());
        assertThat(informantRegisterEntities.size(), is(1));

    }

    private InformantRegisterEntity createInformantRegister(final ZonedDateTime registerTime) {
        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setId(randomUUID());
        informantRegisterEntity.setProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID);
        informantRegisterEntity.setProsecutionAuthorityCode("TFL");
        informantRegisterEntity.setStatus(RegisterStatus.RECORDED);
        informantRegisterEntity.setRegisterDate(LocalDate.now());
        informantRegisterEntity.setGeneratedDate(LocalDate.now());
        informantRegisterEntity.setRegisterTime(registerTime);
        informantRegisterEntity.setHearingId(HEARING_ID);

        return informantRegisterEntity;
    }
}
