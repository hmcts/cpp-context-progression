package uk.gov.moj.cpp.progression.helper;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;

public class TestHelper {

    private final static String COMMITTING_COURT_CODE = "CCCODE";
    private final static String COMMITTING_COURT_NAME = "Committing Court";

    private TestHelper() {

    }

    public static JsonEnvelope buildJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("name").build(),
                createObjectBuilder().build());
    }

    public static ReferredCourtDocument buildCourtDocument(UUID documentTypeId) {
        return ReferredCourtDocument
                .referredCourtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(randomUUID())
                                .build()).build())
//                .withDocumentTypeDescription(randomUUID().toString().substring(0, 10))
                .withDocumentTypeId(documentTypeId)
//                .withIsRemoved(false)
                .withMaterials
                        (Arrays.asList(Material.material().withId(randomUUID())
                                .withUserGroups(Arrays.asList("Listing Officers", "Legal")).build()))
                .withMimeType("application/pdf")
                .withName("SampleReferredCourtDocument")
                .withContainsFinancialMeans(true)
                .build();
    }

    public static Hearing buildHearingWithCourtApplications(final List<CourtApplication> courtApplications) {
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withCourtApplications(courtApplications)
                .build();
    }

    public static Hearing buildHearing(final List<ProsecutionCase> prosecutionCases) {

        final HearingDay hearingDay0 = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                .withCourtCentreId(UUID.randomUUID()).build();
        final HearingDay hearingDay1 = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1))
                .withCourtCentreId(UUID.randomUUID()).build();
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .withHearingDays(Arrays.asList(hearingDay0, hearingDay1))
                .build();
    }

    public static Hearing buildSingleDayHearing(final List<ProsecutionCase> prosecutionCases) {

        final HearingDay hearingDay0 = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                .withCourtCentreId(UUID.randomUUID()).build();
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .withHearingDays(Arrays.asList(hearingDay0))
                .build();
    }

    public static Hearing buildHearingWithNextDayAsHearingDays(final List<ProsecutionCase> prosecutionCases) {
        HearingDay hearingDay0 = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1))
                .withCourtCentreId(UUID.randomUUID()).build();
        return Hearing.hearing()
                .withId(UUID.randomUUID())
                .withProsecutionCases(prosecutionCases)
                .withHearingDays(Arrays.asList(hearingDay0))
                .build();
    }

    public static CourtApplication buildCourtApplicationWithJudicialResults(final UUID courtApplicationId, final List<JudicialResult> judicialResults) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withJudicialResults(judicialResults)
                .withCourtApplicationCases(Arrays.asList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withJudicialResults(judicialResults)
                                .build()))
                        .build()))
                .build();
    }

    public static CourtApplication buildCourtApplicationWithJudicialResultsUnderCourtOrders(final UUID courtApplicationId, final List<JudicialResult> judicialResults) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence()
                                        .withJudicialResults(judicialResults)
                                        .build())
                                .build()))
                        .build())
                .build();
    }

    public static CourtApplication buildCourtApplicationWithJudicialResultsUnderCourtApplicationCases(final UUID courtApplicationId, final List<JudicialResult> judicialResults) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(singletonList(Offence.offence()
                                .withJudicialResults(judicialResults)
                                .build()))
                        .build()))
                .build();
    }

    public static CourtApplication buildCourtApplication(final UUID courtApplicationId, final NextHearing nextHearing) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                        .withNextHearing(nextHearing)
                        .build()))
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                .withIsSJP(false)
                .build()))
                .build();
    }

    public static CourtApplication buildCourtApplicationWithCourtOrder(final UUID courtApplicationId, final NextHearing nextHearing) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withCourtOrder(CourtOrder.courtOrder()
                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                .withOffence(Offence.offence()
                                        .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                                .withNextHearing(nextHearing)
                                                .build()))
                                        .build())
                                .build()))
                        .build())
                .build();
    }

    public static CourtApplication buildCourtApplicationWithCourtApplicationCases(final UUID courtApplicationId, final NextHearing nextHearing) {
        return CourtApplication.courtApplication()
                .withId(courtApplicationId)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(randomUUID())
                                .build())
                        .build())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                        .withOffences(singletonList(Offence.offence()
                                .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                        .withNextHearing(nextHearing)
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

    public static ProsecutionCase buildProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final NextHearing nextHearing) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withCpsOrganisation("A01")
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                        .withNextHearing(nextHearing)
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

    public static ProsecutionCase buildProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final UUID reportingRestrictionId, final NextHearing nextHearing) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                        .withNextHearing(nextHearing)
                                        .build()))
                                .withReportingRestrictions(Collections.singletonList(ReportingRestriction.reportingRestriction()
                                        .withId(reportingRestrictionId)
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

    public static ProsecutionCase buildProsecutionCaseWithCommittingCourt(final UUID caseId, final UUID defendantId, final UUID offenceId, final NextHearing nextHearing) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withCommittingCourt(CommittingCourt.committingCourt()
                                        .withCourtCentreId(UUID.randomUUID())
                                        .withCourtHouseShortName(COMMITTING_COURT_CODE)
                                        .withCourtHouseName(COMMITTING_COURT_NAME)
                                        .withCourtHouseCode(COMMITTING_COURT_CODE)
                                        .withCourtHouseType(JurisdictionType.MAGISTRATES)
                                        .build())
                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                        .withNextHearing(nextHearing)
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }

    public static ProsecutionCase buildProsecutionCaseWithoutJudicialResult(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

    public static NextHearing buildNextHearing(UUID type, UUID bookingReference, String courtLocation, LocalDate weekCommencingDate, ZonedDateTime listedStartDateTime) {
        return NextHearing.nextHearing()
                .withType(HearingType.hearingType()
                        .withId(type)
                        .withDescription("HearingType")
                        .build())
                .withBookingReference(bookingReference)
                .withEstimatedMinutes(60)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCourtHearingLocation(courtLocation)
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .withWeekCommencingDate(weekCommencingDate)
                .withListedStartDateTime(listedStartDateTime)
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
    }

    public static NextHearing buildNextHearing(final UUID existingHearingId) {
        return NextHearing.nextHearing()
                .withExistingHearingId(existingHearingId)
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCode(COMMITTING_COURT_CODE)
                        .withName(COMMITTING_COURT_NAME)
                        .build())
                .build();
    }

    public static JudicialResult buildJudicialResult(final NextHearing nextHearing) {
        return JudicialResult.judicialResult()
                .withNextHearing(nextHearing)
                .build();
    }

    public static ReportingRestriction buildReportingRestriction(final UUID id, final UUID judicialResultId, final String label, final LocalDate orderedDate) {
        return ReportingRestriction.reportingRestriction()
                .withId(id)
                .withJudicialResultId(judicialResultId)
                .withLabel(label)
                .withOrderedDate(orderedDate)
                .build();
    }

    public static Offence buildOffence(final UUID offenceId, final List<JudicialResult> judicialResults) {
        return Offence.offence()
                .withId(offenceId)
                .withJudicialResults(judicialResults)
                .build();
    }

    public static Offence buildOffence(final UUID offenceId, final List<JudicialResult> judicialResults, final List<ReportingRestriction> reportingRestrictions) {
        return Offence.offence()
                .withId(offenceId)
                .withJudicialResults(judicialResults)
                .withReportingRestrictions(reportingRestrictions)
                .build();
    }

    public static Defendant buildDefendant(final UUID defendantId, final List<Offence> offences) {
        return buildDefendant(defendantId, offences, null);
    }

    public static Defendant buildDefendant(final UUID defendantId, final List<Offence> offences, final List<JudicialResult> judicialResults) {
        return Defendant.defendant()
                .withId(defendantId)
                .withOffences(offences)
                .withDefendantCaseJudicialResults(judicialResults)
                .build();
    }

    public static ProsecutionCase buildProsecutionCase(final UUID prosecutionCaseId, final List<Defendant> defendants) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withCpsOrganisation("A01")
                .withDefendants(defendants)
                .build();
    }

    public static CommittingCourt buildCommittingCourt() {
        return CommittingCourt.committingCourt()
                .withCourtHouseName(COMMITTING_COURT_NAME)
                .withCourtHouseCode(COMMITTING_COURT_CODE)
                .withCourtHouseShortName(COMMITTING_COURT_CODE)
                .withCourtHouseType(JurisdictionType.MAGISTRATES)
                .withCourtCentreId(UUID.randomUUID())
                .build();
    }

    public static JsonObject getPayload(final String path) {
        try {
            return new StringToJsonObjectConverter().convert(Resources.toString(Resources.getResource(path), Charset.defaultCharset()));
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return createObjectBuilder().build();
    }
}
