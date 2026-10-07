package uk.gov.moj.cpp.progression.command.helper;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.CourtOrder.courtOrder;
import static uk.gov.justice.core.courts.LinkType.LINKED;
import static uk.gov.justice.core.courts.LinkType.STANDALONE;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.HearingQueryService;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChildApplicationCreationValidatorTest {

    private static final String AAAG = "AAAG";
    private static final String MH = "MH";

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Mock
    private HearingQueryService hearingQueryService;

    @Mock
    private ChildApplicationPermissionChecker childApplicationPermissionChecker;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @InjectMocks
    private ChildApplicationCreationValidator validator;

    private final UUID hearingId = randomUUID();

    @BeforeEach
    public void setUp() {
        lenient().when(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(any())).thenReturn(true);
    }

    // ---- gate ----

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"HOME", "CAAG", "UNKNOWN"})
    public void shouldApplyNoRuleWhenSourceIsNotAaagOrMh(final String applicationSource) {
        assertDoesNotThrow(() -> validator.validate(command(applicationSource, randomUUID(), null)));

        verifyNoInteractions(prosecutionCaseQueryService, hearingQueryService, childApplicationPermissionChecker);
    }

    // ---- AAAG ----

    @Test
    public void shouldRejectAaagWithoutParentApplicationId() {
        assertBadRequest(() -> validator.validate(command(AAAG, null, null)), "parent application id is required");

        verifyNoInteractions(prosecutionCaseQueryService);
    }

    @Test
    public void shouldRejectAaagWhenParentNotFound() {
        final UUID parentId = randomUUID();
        when(prosecutionCaseQueryService.getCourtProceedingsForApplication(eq(parentId), any())).thenReturn(empty());

        assertBadRequest(() -> validator.validate(command(AAAG, parentId, null)), "Parent application not found");
    }

    public static Stream<Arguments> aaagAllowedParents() {
        return Stream.of(
                arguments("standalone (AC6)", standalone()),
                arguments("linked, all offences concluded (AC1/2/3/6A)", linkedWithOffences(true, true)),
                arguments("linked, court order (AC4)", linkedWithCourtOrder()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aaagAllowedParents")
    public void shouldAllowAaagChildOfParent(final String description, final CourtApplication parent) {
        givenAaagParent(parent);

        assertDoesNotThrow(() -> validator.validate(command(AAAG, parent.getId(), null)));

        verify(prosecutionCaseQueryService).getCourtProceedingsForApplication(eq(parent.getId()), any());
        verify(childApplicationPermissionChecker).hasLinkCreateChildApplicationPermission(any());
        verifyNoInteractions(hearingQueryService);
    }

    public static Stream<Arguments> aaagRejectedParents() {
        return Stream.of(
                arguments("parent is a child (AC7)", child(randomUUID()), "child application cannot have a child application"),
                arguments("linked, an offence not concluded (AC5)", linkedWithOffences(true, false), "active offences"),
                arguments("linked, no offences (AC5)", linkedWithNoOffences(), "active offences"),
                arguments("linked, neither cases nor court order", linkedWithoutCasesOrCourtOrder(), "neither court application cases nor a court order"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aaagRejectedParents")
    public void shouldRejectAaagChildOfParent(final String description, final CourtApplication parent, final String message) {
        givenAaagParent(parent);

        assertBadRequest(() -> validator.validate(command(AAAG, parent.getId(), null)), message);

        verify(prosecutionCaseQueryService).getCourtProceedingsForApplication(eq(parent.getId()), any());
        verifyNoInteractions(childApplicationPermissionChecker, hearingQueryService);
    }

    @Test
    public void shouldForbidAaagChildWithoutPermission() {
        final CourtApplication parent = linkedWithCourtOrder();
        givenAaagParent(parent);
        when(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(any())).thenReturn(false);

        assertThrows(ForbiddenRequestException.class, () -> validator.validate(command(AAAG, parent.getId(), null)));
    }

    // ---- MH ----

    @Test
    public void shouldRejectMhWithoutCourtHearingId() {
        assertBadRequest(() -> validator.validate(command(MH, null, null)), "court hearing id is required");

        verifyNoInteractions(hearingQueryService);
    }

    @Test
    public void shouldApplyNoRuleForMhHearingWithoutApplications() {
        givenHearingApplications();

        assertDoesNotThrow(() -> validator.validate(command(MH, null, hearingId)));
        assertDoesNotThrow(() -> validator.validate(command(MH, randomUUID(), hearingId)));

        verify(hearingQueryService, times(2)).getCourtApplications(eq(hearingId), any());
        verifyNoInteractions(childApplicationPermissionChecker, prosecutionCaseQueryService);
    }

    public static Stream<Arguments> mhSingleRejectedApplications() {
        return Stream.of(
                arguments("standalone (AC6)", standalone(), "standalone application"),
                arguments("child (AC7/7A)", child(randomUUID()), "child application cannot have a child application"),
                arguments("linked, neither cases nor court order", linkedWithoutCasesOrCourtOrder(), "neither court application cases nor a court order"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mhSingleRejectedApplications")
    public void shouldRejectMhSingleApplication(final String description, final CourtApplication application, final String message) {
        givenHearingApplications(application);

        assertBadRequest(() -> validator.validate(command(MH, null, hearingId)), message);
        assertBadRequest(() -> validator.validate(command(MH, application.getId(), hearingId)), message);
    }

    @Test
    public void shouldAllowOnlyParentForMhSingleActiveApplication() {
        final CourtApplication active = linkedWithOffences(false);
        givenHearingApplications(active);

        assertDoesNotThrow(() -> validator.validate(command(MH, null, hearingId)));
        assertBadRequest(() -> validator.validate(command(MH, active.getId(), hearingId)), "only a parent application is allowed");

        verifyNoInteractions(childApplicationPermissionChecker);
    }

    public static Stream<Arguments> inactiveApplications() {
        return Stream.of(
                arguments("all offences concluded (AC1-3/9A)", linkedWithOffences(true)),
                arguments("court order (AC4)", linkedWithCourtOrder()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("inactiveApplications")
    public void shouldAllowOnlyChildForMhSingleInactiveApplication(final String description, final CourtApplication inactive) {
        givenHearingApplications(inactive);

        assertDoesNotThrow(() -> validator.validate(command(MH, inactive.getId(), hearingId)));
        assertBadRequest(() -> validator.validate(command(MH, null, hearingId)), "must be created as a child of application");
        assertBadRequest(() -> validator.validate(command(MH, randomUUID(), hearingId)), "must be created as a child of application");

        verify(childApplicationPermissionChecker).hasLinkCreateChildApplicationPermission(any());
        verifyNoInteractions(prosecutionCaseQueryService);
    }

    @Test
    public void shouldForbidMhChildWithoutPermission() {
        final CourtApplication inactive = linkedWithCourtOrder();
        givenHearingApplications(inactive);
        when(childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(any())).thenReturn(false);

        assertThrows(ForbiddenRequestException.class, () -> validator.validate(command(MH, inactive.getId(), hearingId)));
    }

    @Test
    public void shouldRejectMhHearingWithOnlyChildApplications() {
        final UUID parentId = randomUUID();
        givenHearingApplications(child(parentId), child(parentId));

        assertBadRequest(() -> validator.validate(command(MH, parentId, hearingId)), "child application cannot have a child application");
    }

    @Test
    public void shouldAllowOnlyChildOfTheSingleParentForMhHearingWithOneParentAndChildren() {
        final CourtApplication parent = linkedWithOffences(false);
        final CourtApplication childOfParent = child(parent.getId());
        final CourtApplication childOfOtherApplication = child(randomUUID());
        givenHearingApplications(parent, childOfParent, childOfOtherApplication);

        assertDoesNotThrow(() -> validator.validate(command(MH, parent.getId(), hearingId)));
        assertBadRequest(() -> validator.validate(command(MH, childOfParent.getId(), hearingId)), "must be created as a child of application");
        assertBadRequest(() -> validator.validate(command(MH, null, hearingId)), "must be created as a child of application");
    }

    public static Stream<Arguments> mhMultipleParentsRejected() {
        return Stream.of(
                arguments("standalone among parents", List.of(standalone(), linkedWithOffences(false)), "standalone application"),
                arguments("active + inactive via cases (AC9B)", List.of(linkedWithOffences(false), linkedWithOffences(true)), "both active and inactive"),
                arguments("active + inactive via court order (AC9B)", List.of(linkedWithOffences(false), linkedWithCourtOrder()), "both active and inactive"),
                arguments("all inactive", List.of(linkedWithOffences(true), linkedWithCourtOrder()), "parent application is ambiguous"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mhMultipleParentsRejected")
    public void shouldRejectMhHearingWithSeveralParents(final String description, final List<CourtApplication> parents, final String message) {
        givenHearingApplications(parents.toArray(CourtApplication[]::new));

        assertBadRequest(() -> validator.validate(command(MH, null, hearingId)), message);
        assertBadRequest(() -> validator.validate(command(MH, parents.get(0).getId(), hearingId)), message);
    }

    @Test
    public void shouldAllowOnlyParentForMhHearingWithSeveralActiveParents() {
        final CourtApplication active1 = linkedWithOffences(false);
        final CourtApplication active2 = linkedWithNoOffences();
        givenHearingApplications(active1, active2, child(active1.getId()));

        assertDoesNotThrow(() -> validator.validate(command(MH, null, hearingId)));
        assertBadRequest(() -> validator.validate(command(MH, active1.getId(), hearingId)), "only a parent application is allowed");

        verify(childApplicationPermissionChecker, never()).hasLinkCreateChildApplicationPermission(any());
    }

    // ---- helpers ----

    private void givenAaagParent(final CourtApplication parent) {
        when(prosecutionCaseQueryService.getCourtProceedingsForApplication(eq(parent.getId()), any())).thenReturn(of(parent));
    }

    private void givenHearingApplications(final CourtApplication... applications) {
        when(hearingQueryService.getCourtApplications(eq(hearingId), any())).thenReturn(List.of(applications));
    }

    private static void assertBadRequest(final Executable executable, final String message) {
        final BadRequestException exception = assertThrows(BadRequestException.class, executable);
        assertThat(exception.getMessage(), containsString(message));
    }

    private static JsonEnvelope command(final String applicationSource, final UUID newParentId, final UUID hearingId) {
        final JsonObjectBuilder newApplication = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("type", createObjectBuilder().add("linkType", LINKED.toString()));
        if (nonNull(newParentId)) {
            newApplication.add("parentApplicationId", newParentId.toString());
        }
        final JsonObjectBuilder payload = createObjectBuilder().add("courtApplication", newApplication);
        if (nonNull(applicationSource)) {
            payload.add("applicationSource", applicationSource);
        }
        if (nonNull(hearingId)) {
            payload.add("courtHearing", createObjectBuilder().add("id", hearingId.toString()));
        }
        return envelopeFrom(metadataWithRandomUUID("progression.initiate-court-proceedings-for-application"), payload.build());
    }

    private static CourtApplication.Builder linked() {
        return courtApplication().withId(randomUUID()).withType(courtApplicationType().withLinkType(LINKED).build());
    }

    private static CourtApplication standalone() {
        return courtApplication().withId(randomUUID()).withType(courtApplicationType().withLinkType(STANDALONE).build()).build();
    }

    private static CourtApplication child(final UUID parentId) {
        return linked().withParentApplicationId(parentId).build();
    }

    private static CourtApplication linkedWithOffences(final Boolean... proceedingsConcluded) {
        final List<Offence> offences = Arrays.stream(proceedingsConcluded)
                .map(concluded -> offence().withId(randomUUID()).withProceedingsConcluded(concluded).build())
                .toList();
        return linked().withCourtApplicationCases(List.of(courtApplicationCase().withProsecutionCaseId(randomUUID()).withOffences(offences).build())).build();
    }

    private static CourtApplication linkedWithNoOffences() {
        return linked().withCourtApplicationCases(List.of(courtApplicationCase().withProsecutionCaseId(randomUUID()).build())).build();
    }

    private static CourtApplication linkedWithCourtOrder() {
        return linked().withCourtOrder(courtOrder().withId(randomUUID()).build()).build();
    }

    private static CourtApplication linkedWithoutCasesOrCourtOrder() {
        return linked().build();
    }
}
