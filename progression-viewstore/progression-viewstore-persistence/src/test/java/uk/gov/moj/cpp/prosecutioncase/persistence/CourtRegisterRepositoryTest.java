package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtRegisterRequestRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CourtRegisterRepositoryTest {

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("progression-test-persistence-unit");
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final ZonedDateTime REGISTER_TIME_1 = ZonedDateTime.now();
    private static final ZonedDateTime REGISTER_TIME_2 = ZonedDateTime.now().plusHours(1);

    private CourtRegisterRequestRepository courtRegisterRequestRepository;

    @BeforeEach
    void createRepositoriesWithATestEntityManager() {
        courtRegisterRequestRepository = new CourtRegisterRequestRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(courtRegisterRequestRepository);
    }

    @BeforeEach
    public void setUp() {
        courtRegisterRequestRepository.save(createCourtRegister(REGISTER_TIME_1));
        courtRegisterRequestRepository.save(createCourtRegister(REGISTER_TIME_2));
    }
    @AfterEach
    public void tearDown() {
        List<CourtRegisterRequestEntity> courtRegisterRequestEntities = courtRegisterRequestRepository.findAll();
        courtRegisterRequestEntities.forEach(cr -> courtRegisterRequestRepository.remove(cr));
    }

    @Test
    public void shouldFindTheCourtRegisterRequestsByDateAndProsecutionAuthority() {
        final List<CourtRegisterRequestEntity> courtRegisterRequestEntities =
                courtRegisterRequestRepository.findByRequestDateAndCourtHouse(LocalDate.now(), "Lavender Hill");
        assertThat(courtRegisterRequestEntities.size(), is(1));

    }

    @Test
    public void shouldFindTheCourtRegisterRequestsByDate() {
        final List<CourtRegisterRequestEntity> courtRegisterRequestEntities =
                courtRegisterRequestRepository.findByRequestDate(LocalDate.now());
        assertThat(courtRegisterRequestEntities.size(), is(1));

    }

    private CourtRegisterRequestEntity createCourtRegister(final ZonedDateTime registerTime) {
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        courtRegisterRequestEntity.setCourtRegisterRequestId(randomUUID());
        courtRegisterRequestEntity.setCourtCentreId(COURT_CENTRE_ID);
        courtRegisterRequestEntity.setCourtHouse("Lavender Hill");
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);
        courtRegisterRequestEntity.setRegisterDate(LocalDate.now());
        courtRegisterRequestEntity.setGeneratedDate(LocalDate.now());
        courtRegisterRequestEntity.setRegisterTime(registerTime);
        courtRegisterRequestEntity.setHearingId(HEARING_ID);

        return courtRegisterRequestEntity;
    }
}
