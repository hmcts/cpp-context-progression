package uk.gov.moj.cpp.prosecutioncase.persistence;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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

/**
 * Worth running against a real database rather than mocking: three of these queries use HQL's
 * row-value constructor, "(registerTime, hearingId) in (select max(...), hearingId ...)", to pick
 * the latest register per hearing. Nothing but executing them proves they still parse and still
 * select the right row.
 *
 * It uses ProductionLikeEntityManagerProvider rather than HibernateTestEntityManagerProvider, which
 * every other repository test here uses. That provider passes hibernate.jpa.compliance.query=true to
 * createEntityManagerFactory, and an override beats persistence.xml, so under it these three queries
 * throw StrictJpaComplianceViolation before they reach the database - they cannot be tested through
 * it at all. The application runs with compliance off.
 *
 * It takes a database of its own because findByStatusRecorded filters on nothing but status, so rows
 * left by another test class would change its result, and each test rolls back so the same holds
 * between the tests here.
 *
 * The latest-per-hearing tests always write more than one register for the same hearing: a single
 * row would pass even if the grouping were wrong.
 */
public class CourtRegisterRequestRepositoryTest {

    private ProductionLikeEntityManagerProvider entityManagerProvider;
    private CourtRegisterRequestRepository courtRegisterRequestRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        entityManagerProvider = new ProductionLikeEntityManagerProvider("courtregistertest");
        entityManagerProvider.beginTransaction();

        courtRegisterRequestRepository = new CourtRegisterRequestRepository();
        entityManagerProvider.injectEntityManagerInto(courtRegisterRequestRepository);
    }

    @AfterEach
    void rollBackAndCloseTheEntityManager() {
        entityManagerProvider.rollbackTransaction();
        entityManagerProvider.close();
    }

    private CourtRegisterRequestEntity register(final UUID courtCentreId, final UUID hearingId,
                                                final RegisterStatus status, final ZonedDateTime registerTime) {
        final CourtRegisterRequestEntity register = new CourtRegisterRequestEntity();
        register.setCourtRegisterRequestId(randomUUID());
        register.setCourtCentreId(courtCentreId);
        register.setHearingId(hearingId);
        register.setStatus(status);
        register.setRegisterTime(registerTime);
        register.setRegisterDate(registerTime.toLocalDate());
        register.setGeneratedDate(registerTime.toLocalDate());
        register.setSystemDocGeneratorId(randomUUID());
        register.setCourtHouse("Liverpool Crown Court");
        register.setPayload("{}");
        return register;
    }

    private List<UUID> idsOf(final List<CourtRegisterRequestEntity> registers) {
        return registers.stream().map(CourtRegisterRequestEntity::getCourtRegisterRequestId).toList();
    }

    @Test
    public void shouldFindEveryRegisterWithTheGivenStatus() {
        final ZonedDateTime now = ZonedDateTime.now();
        final CourtRegisterRequestEntity notified = register(randomUUID(), randomUUID(), RegisterStatus.NOTIFIED, now);
        courtRegisterRequestRepository.saveAndFlush(notified);
        courtRegisterRequestRepository.saveAndFlush(register(randomUUID(), randomUUID(), RegisterStatus.GENERATED, now));

        final List<CourtRegisterRequestEntity> found = courtRegisterRequestRepository.findByStatus(RegisterStatus.NOTIFIED);

        assertThat(idsOf(found), contains(notified.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldFindTheRegisterRaisedForASystemDocGeneratorRequest() {
        final CourtRegisterRequestEntity register =
                register(randomUUID(), randomUUID(), RegisterStatus.GENERATED, ZonedDateTime.now());
        final UUID systemDocGeneratorId = randomUUID();
        register.setSystemDocGeneratorId(systemDocGeneratorId);
        courtRegisterRequestRepository.saveAndFlush(register);
        courtRegisterRequestRepository.saveAndFlush(
                register(randomUUID(), randomUUID(), RegisterStatus.GENERATED, ZonedDateTime.now()));

        final List<CourtRegisterRequestEntity> found =
                courtRegisterRequestRepository.findBySystemDocGeneratorId(systemDocGeneratorId);

        assertThat(idsOf(found), contains(register.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldReturnNothingWhenNoRegisterCameFromThatSystemDocGeneratorRequest() {
        assertThat(courtRegisterRequestRepository.findBySystemDocGeneratorId(randomUUID()), is(empty()));
    }

    @Test
    public void shouldTakeOnlyTheLatestRecordedRegisterForEachHearingAtACourtCentre() {
        final UUID courtCentreId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime earlier = ZonedDateTime.now().minusHours(2);
        final ZonedDateTime later = ZonedDateTime.now();

        courtRegisterRequestRepository.saveAndFlush(register(courtCentreId, hearingId, RegisterStatus.RECORDED, earlier));
        final CourtRegisterRequestEntity latest = register(courtCentreId, hearingId, RegisterStatus.RECORDED, later);
        courtRegisterRequestRepository.saveAndFlush(latest);

        final List<CourtRegisterRequestEntity> found =
                courtRegisterRequestRepository.findByCourtCenterIdAndStatusRecorded(courtCentreId);

        assertThat(idsOf(found), contains(latest.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldIgnoreARecordedRegisterThatHasAlreadyBeenProcessed() {
        final UUID courtCentreId = randomUUID();
        final CourtRegisterRequestEntity processed =
                register(courtCentreId, randomUUID(), RegisterStatus.RECORDED, ZonedDateTime.now());
        processed.setProcessedOn(ZonedDateTime.now());
        courtRegisterRequestRepository.saveAndFlush(processed);

        assertThat(courtRegisterRequestRepository.findByCourtCenterIdAndStatusRecorded(courtCentreId), is(empty()));
    }

    @Test
    public void shouldFindTheGeneratedRegistersForACourtCentre() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final CourtRegisterRequestEntity generated = register(courtCentreId, randomUUID(), RegisterStatus.GENERATED, now);
        courtRegisterRequestRepository.saveAndFlush(generated);
        courtRegisterRequestRepository.saveAndFlush(register(courtCentreId, randomUUID(), RegisterStatus.RECORDED, now));

        final List<CourtRegisterRequestEntity> found =
                courtRegisterRequestRepository.findByCourtCenterIdAndStatusGenerated(courtCentreId);

        assertThat(idsOf(found), contains(generated.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldNarrowTheGeneratedRegistersForACourtCentreToOneRegisterDate() {
        final UUID courtCentreId = randomUUID();
        final ZonedDateTime today = ZonedDateTime.now();
        final ZonedDateTime yesterday = today.minusDays(1);
        final CourtRegisterRequestEntity todays = register(courtCentreId, randomUUID(), RegisterStatus.GENERATED, today);
        courtRegisterRequestRepository.saveAndFlush(todays);
        courtRegisterRequestRepository.saveAndFlush(register(courtCentreId, randomUUID(), RegisterStatus.GENERATED, yesterday));

        final List<CourtRegisterRequestEntity> found = courtRegisterRequestRepository
                .findByCourtCenterIdForRegisterDateAndStatusGenerated(courtCentreId, today.toLocalDate());

        assertThat(idsOf(found), contains(todays.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldFindTheRecordedRegistersForAHearing() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime now = ZonedDateTime.now();
        final CourtRegisterRequestEntity first = register(randomUUID(), hearingId, RegisterStatus.RECORDED, now.minusHours(1));
        final CourtRegisterRequestEntity second = register(randomUUID(), hearingId, RegisterStatus.RECORDED, now);
        courtRegisterRequestRepository.saveAndFlush(first);
        courtRegisterRequestRepository.saveAndFlush(second);
        courtRegisterRequestRepository.saveAndFlush(register(randomUUID(), hearingId, RegisterStatus.GENERATED, now));

        final List<CourtRegisterRequestEntity> found =
                courtRegisterRequestRepository.findByHearingIdAndStatusRecorded(hearingId);

        assertThat(found, hasSize(2));
        assertThat(idsOf(found),
                containsInAnyOrder(first.getCourtRegisterRequestId(), second.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldTakeOnlyTheLatestUnprocessedRecordedRegisterForEachHearing() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime earlier = ZonedDateTime.now().minusHours(3);
        final ZonedDateTime later = ZonedDateTime.now().minusMinutes(5);

        courtRegisterRequestRepository.saveAndFlush(register(randomUUID(), hearingId, RegisterStatus.RECORDED, earlier));
        final CourtRegisterRequestEntity latest = register(randomUUID(), hearingId, RegisterStatus.RECORDED, later);
        courtRegisterRequestRepository.saveAndFlush(latest);

        final List<CourtRegisterRequestEntity> found = courtRegisterRequestRepository.findByStatusRecorded();

        assertThat(idsOf(found), contains(latest.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldFindTheLatestRegisterPerHearingForARequestDate() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime earlier = ZonedDateTime.now().minusHours(4);
        final ZonedDateTime later = ZonedDateTime.now().minusHours(1);

        courtRegisterRequestRepository.saveAndFlush(register(randomUUID(), hearingId, RegisterStatus.GENERATED, earlier));
        final CourtRegisterRequestEntity latest = register(randomUUID(), hearingId, RegisterStatus.GENERATED, later);
        courtRegisterRequestRepository.saveAndFlush(latest);

        final List<CourtRegisterRequestEntity> found =
                courtRegisterRequestRepository.findByRequestDate(later.toLocalDate());

        assertThat(idsOf(found), contains(latest.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldFindTheLatestRegisterPerHearingForARequestDateAtOneCourtHouse() {
        final UUID hearingId = randomUUID();
        final ZonedDateTime earlier = ZonedDateTime.now().minusHours(4);
        final ZonedDateTime later = ZonedDateTime.now().minusHours(1);

        courtRegisterRequestRepository.saveAndFlush(register(randomUUID(), hearingId, RegisterStatus.GENERATED, earlier));
        final CourtRegisterRequestEntity latest = register(randomUUID(), hearingId, RegisterStatus.GENERATED, later);
        courtRegisterRequestRepository.saveAndFlush(latest);

        final CourtRegisterRequestEntity elsewhere =
                register(randomUUID(), randomUUID(), RegisterStatus.GENERATED, later);
        elsewhere.setCourtHouse("Manchester Crown Court");
        courtRegisterRequestRepository.saveAndFlush(elsewhere);

        final List<CourtRegisterRequestEntity> found = courtRegisterRequestRepository
                .findByRequestDateAndCourtHouse(later.toLocalDate(), "Liverpool Crown Court");

        assertThat(idsOf(found), contains(latest.getCourtRegisterRequestId()));
    }

    @Test
    public void shouldReturnNothingWhenNoRegisterWasRaisedOnThatDate() {
        assertThat(courtRegisterRequestRepository.findByRequestDate(LocalDate.of(1999, 1, 1)), is(empty()));
    }
}
