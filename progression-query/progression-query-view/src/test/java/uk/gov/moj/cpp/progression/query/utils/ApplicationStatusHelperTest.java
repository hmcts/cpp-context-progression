package uk.gov.moj.cpp.progression.query.utils;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AACA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AACD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AASA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AASD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.ACSD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.APA;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.ASV;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.AW;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.DISM;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.G;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.RFSD;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.ROPENED;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.STDEC;
import static uk.gov.moj.cpp.progression.domain.constant.ResultConstants.WDRN;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.ApplicationCategory.APPEAL;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.ApplicationCategory.REOPEN;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.ApplicationCategory.STAT_DEC;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.buildApplicationStatuses;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.getApplicationTypeCategories;
import static uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.resolveStatus;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.ApplicationCategory;
import uk.gov.moj.cpp.progression.query.utils.ApplicationStatusHelper.ApplicationLabelStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApplicationStatusHelperTest {

    private static final UUID STAT_DEC_TYPE_ID = randomUUID();
    private static final UUID REOPEN_TYPE_ID = randomUUID();
    private static final UUID APPEAL_TYPE_ID = randomUUID();
    private static final UUID SPECIAL_MEASURES_TYPE_ID = randomUUID();
    private static final UUID ADJOURNMENT_RESULT_ID = randomUUID();

    private static final Map<UUID, ApplicationCategory> CATEGORIES = Map.of(
            STAT_DEC_TYPE_ID, STAT_DEC,
            REOPEN_TYPE_ID, REOPEN,
            APPEAL_TYPE_ID, APPEAL);

    private final UUID caseId = randomUUID();

    static Stream<Arguments> mappedResults() {
        return Stream.of(
                Arguments.of(STAT_DEC, G, ApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED),
                Arguments.of(STAT_DEC, STDEC, ApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED),
                Arguments.of(STAT_DEC, RFSD, ApplicationLabelStatus.STATUTORY_DECLARATION_REFUSED),
                Arguments.of(STAT_DEC, WDRN, ApplicationLabelStatus.STATUTORY_DECLARATION_WITHDRAWN),
                Arguments.of(STAT_DEC, DISM, ApplicationLabelStatus.STATUTORY_DECLARATION_DISMISSED),
                Arguments.of(REOPEN, G, ApplicationLabelStatus.REOPENING_GRANTED),
                Arguments.of(REOPEN, ROPENED, ApplicationLabelStatus.REOPENING_GRANTED),
                Arguments.of(REOPEN, RFSD, ApplicationLabelStatus.REOPENING_REFUSED),
                Arguments.of(REOPEN, WDRN, ApplicationLabelStatus.REOPENING_WITHDRAWN),
                Arguments.of(REOPEN, DISM, ApplicationLabelStatus.REOPENING_DISMISSED),
                Arguments.of(APPEAL, AACA, ApplicationLabelStatus.APPEAL_ALLOWED),
                Arguments.of(APPEAL, AASA, ApplicationLabelStatus.APPEAL_ALLOWED),
                Arguments.of(APPEAL, RFSD, ApplicationLabelStatus.APPEAL_REFUSED),
                Arguments.of(APPEAL, AW, ApplicationLabelStatus.APPEAL_WITHDRAWN),
                Arguments.of(APPEAL, WDRN, ApplicationLabelStatus.APPEAL_WITHDRAWN),
                Arguments.of(APPEAL, APA, ApplicationLabelStatus.APPEAL_WITHDRAWN),
                Arguments.of(APPEAL, AACD, ApplicationLabelStatus.APPEAL_DISMISSED),
                Arguments.of(APPEAL, AASD, ApplicationLabelStatus.APPEAL_DISMISSED),
                Arguments.of(APPEAL, ACSD, ApplicationLabelStatus.APPEAL_DISMISSED));
    }

    @ParameterizedTest
    @MethodSource("mappedResults")
    void shouldMapApplicationResultToStatus(final ApplicationCategory category, final UUID resultId, final ApplicationLabelStatus expectedStatus) {
        assertThat(resolveStatus(category, application(randomUUID(), randomUUID(), List.of(result(resultId)))), is(expectedStatus));
    }

    @Test
    void shouldBeLodgedWhenApplicationHasNoResults() {
        assertThat(resolveStatus(STAT_DEC, application(randomUUID(), STAT_DEC_TYPE_ID, null)), is(ApplicationLabelStatus.LODGED));
        assertThat(resolveStatus(STAT_DEC, application(randomUUID(), STAT_DEC_TYPE_ID, List.of())), is(ApplicationLabelStatus.LODGED));
    }

    @Test
    void shouldBePendingWhenApplicationResultIsNotMapped() {
        assertThat(resolveStatus(REOPEN, application(randomUUID(), REOPEN_TYPE_ID, List.of(result(ADJOURNMENT_RESULT_ID)))), is(ApplicationLabelStatus.PENDING));
    }

    @Test
    void shouldBePendingForUnmappedAppealResultSentenceVaried() {
        assertThat(resolveStatus(APPEAL, application(randomUUID(), APPEAL_TYPE_ID, List.of(result(ASV)))), is(ApplicationLabelStatus.PENDING));
    }

    @Test
    void shouldBePendingWhenResultIsMappedOnlyForAnotherCategory() {
        assertThat(resolveStatus(APPEAL, application(randomUUID(), APPEAL_TYPE_ID, List.of(result(STDEC)))), is(ApplicationLabelStatus.PENDING));
    }

    @Test
    void shouldUseFinalStatusWhenGrantedAndAdjourned() {
        final CourtApplication application = application(randomUUID(), STAT_DEC_TYPE_ID, List.of(result(ADJOURNMENT_RESULT_ID), result(G)));
        assertThat(resolveStatus(STAT_DEC, application), is(ApplicationLabelStatus.STATUTORY_DECLARATION_GRANTED));
    }

    @Test
    void shouldBeLodgedWhenOnlyResultIsDeleted() {
        final CourtApplication application = application(randomUUID(), STAT_DEC_TYPE_ID, List.of(deletedResult(G)));
        assertThat(resolveStatus(STAT_DEC, application), is(ApplicationLabelStatus.LODGED));
    }

    @Test
    void shouldIgnoreDeletedResultWhenAmendedAndReshared() {
        final CourtApplication application = application(randomUUID(), STAT_DEC_TYPE_ID, List.of(deletedResult(G), result(RFSD)));
        assertThat(resolveStatus(STAT_DEC, application), is(ApplicationLabelStatus.STATUTORY_DECLARATION_REFUSED));
    }

    @Test
    void shouldIgnoreOffenceLevelResults() {
        final UUID defendantId = randomUUID();
        final CourtApplication application = CourtApplication.courtApplication()
                .withValuesFrom(application(randomUUID(), STAT_DEC_TYPE_ID, null))
                .withSubject(subject(defendantId))
                .withCourtApplicationCases(List.of(applicationCase(caseId, Offence.offence()
                        .withId(randomUUID())
                        .withJudicialResults(List.of(result(G)))
                        .build())))
                .build();

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(application), CATEGORIES);

        assertThat(statuses.getJsonObject(0).getString("status"), is("LODGED"));
    }

    @Test
    void shouldBuildOneEntryPerInScopeApplication() {
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final CourtApplication statDec = applicationFor(STAT_DEC_TYPE_ID, "MC80528", defendantId, List.of(result(RFSD)), offenceId1, offenceId2);
        final CourtApplication reopen = applicationFor(REOPEN_TYPE_ID, "MC80524", defendantId, null, offenceId1);
        final CourtApplication appeal = applicationFor(APPEAL_TYPE_ID, "MC80802", defendantId, List.of(result(ADJOURNMENT_RESULT_ID)), offenceId2);
        final CourtApplication specialMeasures = applicationFor(SPECIAL_MEASURES_TYPE_ID, "SE20501", defendantId, null, offenceId1);

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(statDec, reopen, appeal, specialMeasures), CATEGORIES);

        assertThat(statuses.size(), is(3));
        assertEntry(statuses.getJsonObject(0), statDec, "MC80528", "STAT_DEC", "STATUTORY_DECLARATION_REFUSED");
        assertEntry(statuses.getJsonObject(1), reopen, "MC80524", "REOPEN", "LODGED");
        assertEntry(statuses.getJsonObject(2), appeal, "MC80802", "APPEAL", "PENDING");

        final JsonObject statDecOffences = statuses.getJsonObject(0).getJsonArray("defendantOffences").getJsonObject(0);
        assertThat(statDecOffences.getString("defendantId"), is(defendantId.toString()));
        assertThat(statDecOffences.getJsonArray("offenceIds").getString(0), is(offenceId1.toString()));
        assertThat(statDecOffences.getJsonArray("offenceIds").getString(1), is(offenceId2.toString()));
        assertThat(statuses.getJsonObject(1).getJsonArray("defendantOffences").getJsonObject(0).getJsonArray("offenceIds").size(), is(1));
    }

    @Test
    void shouldBuildAnEntryForEachStatDecOnTheSameCase() {
        final UUID defendantId = randomUUID();
        final CourtApplication firstStatDec = applicationFor(STAT_DEC_TYPE_ID, "MC80528", defendantId, List.of(result(RFSD)), randomUUID());
        final CourtApplication secondStatDec = applicationFor(STAT_DEC_TYPE_ID, "MC80528", defendantId, null, randomUUID());

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(firstStatDec, secondStatDec), CATEGORIES);

        assertThat(statuses.size(), is(2));
        assertThat(statuses.getJsonObject(0).getString("status"), is("STATUTORY_DECLARATION_REFUSED"));
        assertThat(statuses.getJsonObject(1).getString("status"), is("LODGED"));
    }

    @Test
    void shouldBuildOneEntryWhenTheSameApplicationIsLinkedMoreThanOnce() {
        final CourtApplication statDec = applicationFor(STAT_DEC_TYPE_ID, "MC80527", randomUUID(), null, randomUUID());

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(statDec, statDec), CATEGORIES);

        assertThat(statuses.size(), is(1));
    }

    @Test
    void shouldOnlyIncludeOffencesOfTheApplicationDefendantOnThisCase() {
        final UUID defendant1 = randomUUID();
        final UUID defendant1OffenceId = randomUUID();
        final UUID otherCaseOffenceId = randomUUID();
        final CourtApplication statDec = CourtApplication.courtApplication()
                .withValuesFrom(applicationFor(STAT_DEC_TYPE_ID, "MC80527", defendant1, null, defendant1OffenceId))
                .withCourtApplicationCases(List.of(
                        applicationCase(caseId, Offence.offence().withId(defendant1OffenceId).build()),
                        applicationCase(randomUUID(), Offence.offence().withId(otherCaseOffenceId).build())))
                .build();

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(statDec), CATEGORIES);

        final JsonArray defendantOffences = statuses.getJsonObject(0).getJsonArray("defendantOffences");
        assertThat(defendantOffences.size(), is(1));
        assertThat(defendantOffences.getJsonObject(0).getString("defendantId"), is(defendant1.toString()));
        assertThat(defendantOffences.getJsonObject(0).getJsonArray("offenceIds").size(), is(1));
        assertThat(defendantOffences.getJsonObject(0).getJsonArray("offenceIds").getString(0), is(defendant1OffenceId.toString()));
    }

    @Test
    void shouldBuildEntryForCompanyDefendant() {
        final UUID defendantId = randomUUID();
        final CourtApplication statDec = applicationFor(STAT_DEC_TYPE_ID, "MC80527", defendantId, List.of(result(G)), randomUUID());
        final CourtApplication companyStatDec = CourtApplication.courtApplication()
                .withValuesFrom(statDec)
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withMasterDefendantId(randomUUID())
                                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                        .withOrganisation(Organisation.organisation().withName("ABC LTD").build())
                                        .build())
                                .withDefendantCase(List.of(DefendantCase.defendantCase().withCaseId(caseId).withDefendantId(defendantId).build()))
                                .build())
                        .build())
                .build();

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(companyStatDec), CATEGORIES);

        assertThat(statuses.getJsonObject(0).getString("status"), is("STATUTORY_DECLARATION_GRANTED"));
        assertThat(statuses.getJsonObject(0).getJsonArray("defendantOffences").getJsonObject(0).getString("defendantId"), is(defendantId.toString()));
    }

    @Test
    void shouldLeaveOutDefendantOffencesWhenApplicationHasNoOffencesOnTheCase() {
        final CourtApplication statDec = CourtApplication.courtApplication()
                .withValuesFrom(applicationFor(STAT_DEC_TYPE_ID, "MC80527", randomUUID(), null, randomUUID()))
                .withCourtApplicationCases(null)
                .build();

        final JsonArray statuses = buildApplicationStatuses(caseId, List.of(statDec), CATEGORIES);

        assertThat(statuses.getJsonObject(0).containsKey("defendantOffences"), is(false));
    }

    @Test
    void shouldReturnNoEntriesWhenNoApplicationIsInScope() {
        final CourtApplication specialMeasures = applicationFor(SPECIAL_MEASURES_TYPE_ID, "SE20501", randomUUID(), null, randomUUID());

        assertThat(buildApplicationStatuses(caseId, List.of(specialMeasures), CATEGORIES).isEmpty(), is(true));
        assertThat(buildApplicationStatuses(caseId, List.of(specialMeasures), Map.of()).isEmpty(), is(true));
    }

    @Test
    void shouldMapApplicationTypeGroupsToCategories() {
        final JsonObject groups = createObjectBuilder()
                .add("groups", createArrayBuilder()
                        .add(group("statdec", STAT_DEC_TYPE_ID, randomUUID()))
                        .add(group("reopen", REOPEN_TYPE_ID))
                        .add(group("appeal", APPEAL_TYPE_ID))
                        .add(group("unknown", SPECIAL_MEASURES_TYPE_ID)))
                .build();

        final Map<UUID, ApplicationCategory> categories = getApplicationTypeCategories(groups);

        assertThat(categories.size(), is(4));
        assertThat(categories.get(STAT_DEC_TYPE_ID), is(STAT_DEC));
        assertThat(categories.get(REOPEN_TYPE_ID), is(REOPEN));
        assertThat(categories.get(APPEAL_TYPE_ID), is(APPEAL));
        assertThat(categories.containsKey(SPECIAL_MEASURES_TYPE_ID), is(false));
    }

    @Test
    void shouldReturnNoCategoriesWhenGroupsAreMissing() {
        assertThat(getApplicationTypeCategories(createObjectBuilder().build()).isEmpty(), is(true));
        assertThat(getApplicationTypeCategories(null).isEmpty(), is(true));
    }

    private static JsonObject group(final String groupName, final UUID... applicationTypeIds) {
        final var applicationTypes = createArrayBuilder();
        Stream.of(applicationTypeIds).forEach(id -> applicationTypes.add(createObjectBuilder()
                .add("applicationTypeId", id.toString())
                .add("enforcementNotificationRequired", true)));
        return createObjectBuilder().add("groupName", groupName).add("applicationTypes", applicationTypes).build();
    }

    private static void assertEntry(final JsonObject entry, final CourtApplication application, final String code, final String category, final String status) {
        assertThat(entry.getString("applicationId"), is(application.getId().toString()));
        assertThat(entry.getString("applicationTypeCode"), is(code));
        assertThat(entry.getString("applicationCategory"), is(category));
        assertThat(entry.getString("status"), is(status));
    }

    private CourtApplication applicationFor(final UUID typeId, final String code, final UUID defendantId,
                                            final List<JudicialResult> results, final UUID... offenceIds) {
        return CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(CourtApplicationType.courtApplicationType().withId(typeId).withCode(code).build())
                .withJudicialResults(results)
                .withSubject(subject(defendantId))
                .withCourtApplicationCases(List.of(applicationCase(caseId,
                        Stream.of(offenceIds).map(id -> Offence.offence().withId(id).build()).toArray(Offence[]::new))))
                .build();
    }

    private CourtApplicationParty subject(final UUID defendantId) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(randomUUID())
                        .withDefendantCase(List.of(DefendantCase.defendantCase().withCaseId(caseId).withDefendantId(defendantId).build()))
                        .build())
                .build();
    }

    private static CourtApplicationCase applicationCase(final UUID prosecutionCaseId, final Offence... offences) {
        return CourtApplicationCase.courtApplicationCase()
                .withProsecutionCaseId(prosecutionCaseId)
                .withOffences(List.of(offences))
                .build();
    }

    private static CourtApplication application(final UUID applicationId, final UUID typeId, final List<JudicialResult> results) {
        return CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withId(typeId).build())
                .withJudicialResults(results)
                .build();
    }

    private static JudicialResult result(final UUID resultTypeId) {
        return JudicialResult.judicialResult()
                .withJudicialResultId(randomUUID())
                .withJudicialResultTypeId(resultTypeId)
                .build();
    }

    private static JudicialResult deletedResult(final UUID resultTypeId) {
        return JudicialResult.judicialResult()
                .withValuesFrom(result(resultTypeId))
                .withIsDeleted(true)
                .build();
    }
}
