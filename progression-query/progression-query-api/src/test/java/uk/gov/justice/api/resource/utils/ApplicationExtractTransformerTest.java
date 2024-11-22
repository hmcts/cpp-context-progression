package uk.gov.justice.api.resource.utils;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.api.resource.DefaultQueryApiApplicationsApplicationIdExtractResource.STANDALONE_APPLICATION;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicantCounsel;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.RespondentCounsel;
import uk.gov.justice.core.courts.YouthCourt;
import uk.gov.justice.progression.courts.exract.Applicant;
import uk.gov.justice.progression.courts.exract.ApplicantRepresentation;
import uk.gov.justice.progression.courts.exract.ApplicationCourtExtractRequested;
import uk.gov.justice.progression.courts.exract.CourtApplications;
import uk.gov.justice.progression.courts.exract.CourtDecisions;
import uk.gov.justice.progression.courts.exract.Hearings;
import uk.gov.justice.progression.courts.exract.Respondent;
import uk.gov.justice.progression.courts.exract.RespondentRepresentation;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationExtractTransformerTest {
    private static final UUID HEARING_ID_1 = randomUUID();
    private static final UUID HEARING_ID_2 = randomUUID();
    private static final String FULL_NAME = "Jack Denial";
    private static final UUID DEFANDANT_ID = randomUUID();

    private static final String APPLICATION_TYPE = "Appeal";
    private static final String ADDRESS_4 = "NORWICH";
    private static final String ADDRESS_5 = "BERKSHIRE";
    private static final String APPLICATION_REFERENCE = "APP-1";
    private static final String ORG_NAME = "ABC Corp";
    private static final String JUDICIAL_DISPLAY_NAME = "Chair: Denial Winger1: Denial Winger2: Denial";
    private static final String ROLE_DISPLAY_NAME = "District judge";
    private static final String REPORTING_RESTRICTION_REASON = "Suspect is minor";
    private static final UUID APPLICATION_ID = randomUUID();
    private static final LocalDate ORDERED_DATE = LocalDate.now();

    private static final String HEARING_DATE_1 = "2018-06-01T10:00:00.000Z";
    private static final String HEARING_DATE_2 = "2018-07-01T10:00:00.000Z";

    private static final String COURT_NAME = "liverpool crown pool";
    private static final String HEARING_TYPE_APPLICATION = "Application";
    private static final String ADDRESS_1 = "22";
    private static final String ADDRESS_2 = "Acacia Avenue";
    private static final String ADDRESS_3 = "Acacia Town";
    private static final String POST_CODE = "CR7 0AA";
    private static final String FIRST_NAME = "Jack";
    private static final String LAST_NAME = "Denial";
    private static final String LABEL = "Fine";
    private static final String PROMPT_VALUE = "10";
    private static final String COURT_EXTRACT = "Y";

    @Spy
    ApplicationExtractTransformer applicationExtractTransformer;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    TransformationHelper transformationHelper;

    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private RequestedNameMapper requestedNameMapper;

    @BeforeEach
    public void init() {
        setField(this.applicationExtractTransformer, "transformationHelper", transformationHelper);
        setField(this.applicationExtractTransformer, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
    }

    @Test
    public void shouldTransformApplicationWithHearings() {
        //given
        final CourtApplication courtApplication = createCourtApplication();
        List<Hearing> hearingsForApplication = createHearingsForApplication();

        when(referenceDataService.getCourtCentreAddress(any(), any())).thenReturn(createAddress());
        when(requestedNameMapper.getRequestedJudgeName(any(JsonObject.class))).thenReturn("Denial");
        when(referenceDataService.getJudiciary(any(UUID.class))).thenReturn(Optional.ofNullable(createJudiciaryJsonObject()));

        //when
        final ApplicationCourtExtractRequested applicationCourtExtractRequested = applicationExtractTransformer
                .getApplicationCourtExtractRequested(courtApplication, hearingsForApplication, STANDALONE_APPLICATION, randomUUID());
        // then
        assertValues(applicationCourtExtractRequested, true);
    }

    @Test
    public void shouldTransformApplicationWithNoHearings() {
        //given
        final CourtApplication courtApplication = createCourtApplication();

        //when
        final ApplicationCourtExtractRequested applicationCourtExtractRequested = applicationExtractTransformer
                .getApplicationCourtExtractRequested(courtApplication, emptyList(), STANDALONE_APPLICATION, randomUUID());
        // then
        assertValues(applicationCourtExtractRequested, false);
    }

    @Test
    public void shouldTransformApplicationWithHearingWhenHearingIsHeardInYouthCourt() {
        //given
        final CourtApplication courtApplication = createCourtApplictionWithDefendantApplicant();
        List<Hearing> hearingsForApplication = createHearingsForApplicationWithHearingHeardInYouthCourt();

        when(referenceDataService.getCourtCentreAddress(any(), any())).thenReturn(createAddress());
        when(requestedNameMapper.getRequestedJudgeName(any(JsonObject.class))).thenReturn("Denial");
        when(referenceDataService.getJudiciary(any(UUID.class))).thenReturn(Optional.ofNullable(createJudiciaryJsonObject()));

        //when
        final ApplicationCourtExtractRequested applicationCourtExtractRequested = applicationExtractTransformer
                .getApplicationCourtExtractRequested(courtApplication, hearingsForApplication, STANDALONE_APPLICATION, randomUUID());
        assertThat(applicationCourtExtractRequested.getPublishingCourt().getName(), is("Youth Court Name"));
        assertThat(applicationCourtExtractRequested.getPublishingCourt().getWelshName(), is("Welsh Youth Court Name"));
        assertThat(applicationCourtExtractRequested.getHearings().get(1).getCourtCentre().getName(), is("Youth Court Name"));
        assertThat(applicationCourtExtractRequested.getHearings().get(1).getCourtCentre().getWelshName(), is("Welsh Youth Court Name"));
    }

    @Test
    public void shouldGetHearingsForApplications(){

        //when
        List<Hearing> hearings = applicationExtractTransformer
                .getHearingsForApplication(createHearingsJsonArray(), asList(HEARING_ID_1.toString(), HEARING_ID_2.toString()));
        // then
        assertThat(hearings.size(), is(2));
        // when
        hearings = applicationExtractTransformer
                .getHearingsForApplication(createHearingsJsonArray(), asList(randomUUID().toString()));
        // then
        assertThat(hearings.size(), is(0));

        hearings = applicationExtractTransformer.getHearingsForApplication(createHearingsJsonArray(), emptyList());
        assertThat(hearings.size(), is(0));

    }

    private JsonArray createHearingsJsonArray() {
        return createArrayBuilder().add(createObjectBuilder().add("id", HEARING_ID_1.toString()))
                .add(createObjectBuilder().add("id", HEARING_ID_2.toString()))
                .build();
    }

    private void assertValues(final ApplicationCourtExtractRequested applicationCourtExtractRequested, final boolean withHearings) {
        assertApplicant(applicationCourtExtractRequested.getApplicant());
        assertThat(applicationCourtExtractRequested.getExtractType(), is(STANDALONE_APPLICATION));
        assertThat(applicationCourtExtractRequested.getReference(), is(APPLICATION_REFERENCE));
        assertThat(applicationCourtExtractRequested.getIsAppealPending(), is(true));
        assertRespondents(applicationCourtExtractRequested.getRespondent());
        assertCourtApplications(applicationCourtExtractRequested.getCourtApplications(), withHearings);
        if (withHearings) {
            assertPublishingCourt(applicationCourtExtractRequested.getPublishingCourt());
            assertCourtDecisions(applicationCourtExtractRequested.getCourtDecisions());
            assertHearings(applicationCourtExtractRequested.getHearings());
        }
        else{
            assertThat(applicationCourtExtractRequested.getHearings().size(), is(0));
        }
    }

    private void assertHearings(List<Hearings> hearings) {
        hearings.stream().forEach(hearing -> {
            assertThat(hearing.getType(), is(HEARING_TYPE_APPLICATION));
            assertThat(hearing.getJurisdictionType(), is(JurisdictionType.CROWN));
            assertThat(hearing.getReportingRestrictionReason(), is(REPORTING_RESTRICTION_REASON));
            assertThat(hearing.getCourtCentre().getName(), is(COURT_NAME));
            assertThat(hearing.getHearingDays().get(0).getDay(), is(ZonedDateTimes.fromString(HEARING_DATE_1).toLocalDate()));
            assertThat(hearing.getHearingDays().get(1).getDay(), is(ZonedDateTimes.fromString(HEARING_DATE_2).toLocalDate()));
        });
    }

    private void assertCourtApplications(List<CourtApplications> courtApplications, boolean isHearingExist) {
        courtApplications.forEach(ca -> {
            assertThat(ca.getApplicationType(), is(APPLICATION_TYPE));
            assertApplicantRepresentation(ca.getRepresentation().getApplicantRepresentation(), isHearingExist);
            assertRespondentRepresentation(ca.getRepresentation().getRespondentRepresentation(), isHearingExist);
            assertResults(ca.getResults());
        });
    }

    private void assertApplicantRepresentation(ApplicantRepresentation applicantRepresentation, boolean isHearingExist) {
        assertAddress(applicantRepresentation.getAddress());
        if (isHearingExist) {
            applicantRepresentation.getApplicantCounsels().stream().forEach(ac -> {
                assertThat(ac.getFirstName(), is(FIRST_NAME));
                assertThat(ac.getLastName(), is(LAST_NAME));
            });
        } else {
            assertThat(applicantRepresentation.getApplicantCounsels(), empty());
        }

        assertThat(applicantRepresentation.getName(), is(ORG_NAME));
    }

    private void assertRespondentRepresentation(List<RespondentRepresentation> respondentRepresentation, boolean isHearingExist) {
        respondentRepresentation.forEach(reportingRestriction -> {
            assertAddress(reportingRestriction.getAddress());
            if (isHearingExist) {
                reportingRestriction.getRespondentCounsels().stream().forEach(rc -> {
                    assertThat(rc.getFirstName(), is(FIRST_NAME));
                    assertThat(rc.getLastName(), is(LAST_NAME));
                });
            } else {
                assertThat(reportingRestriction.getRespondentCounsels(), empty());
            }
            assertThat(reportingRestriction.getName(), is(ORG_NAME));
        });

    }

    private void assertResults(List<JudicialResult> results) {
        results.stream().forEach(r -> {
            assertThat(r.getDelegatedPowers().getFirstName(), is(FIRST_NAME));
            assertThat(r.getDelegatedPowers().getLastName(), is(LAST_NAME));
            assertThat(r.getIsAvailableForCourtExtract(), is(Boolean.TRUE));
            assertThat(r.getLabel(), is(LABEL));
            assertThat(r.getJudicialResultPrompts().get(0).getCourtExtract(), is("Y"));
            assertThat(r.getJudicialResultPrompts().get(0).getLabel(), is(LABEL));
            assertThat(r.getJudicialResultPrompts().get(0).getValue(), is(PROMPT_VALUE));
            assertThat(r.getOrderedDate(), is(ORDERED_DATE));
        });
    }

    private void assertCourtDecisions(List<CourtDecisions> courtDecisions) {
        courtDecisions.stream().forEach(cd -> {
            assertThat(cd.getDates().get(0).getDay(), is(ZonedDateTimes.fromString(HEARING_DATE_1).toLocalDate()));
            assertThat(cd.getDates().get(1).getDay(), is(ZonedDateTimes.fromString(HEARING_DATE_2).toLocalDate()));
            assertThat(cd.getJudicialDisplayName(), is(JUDICIAL_DISPLAY_NAME));
            assertThat(cd.getRoleDisplayName(), is(ROLE_DISPLAY_NAME));
        });
    }

    private void assertPublishingCourt(CourtCentre publishingCourt) {
        assertAddress(publishingCourt.getAddress());
        assertThat(publishingCourt.getName(), is(COURT_NAME));
    }

    private void assertAddress(final Address address) {
        assertThat(address.getAddress1(), is(ADDRESS_1));
        assertThat(address.getPostcode(), is(POST_CODE));
        assertThat(address.getAddress2(), is(ADDRESS_2 + SPACE + ADDRESS_3));
        assertThat(address.getAddress3(), is(ADDRESS_4 + SPACE + ADDRESS_5));
    }

    private void assertRespondents(List<Respondent> respondent) {
        respondent.stream().forEach(r -> {
            assertThat(r.getName(), is(ORG_NAME));
            assertExtractAddress(r.getAddress());
        });
    }

    private void assertApplicant(Applicant applicant) {
        assertThat(applicant.getName(), is(FULL_NAME));
        assertExtractAddress(applicant.getAddress());
    }

    private void assertExtractAddress(final uk.gov.justice.progression.courts.exract.Address address) {
        assertThat(address.getAddress1(), is(ADDRESS_1));
        assertThat(address.getPostCode(), is(POST_CODE));
        assertThat(address.getAddress2(), is(ADDRESS_2 + SPACE + ADDRESS_3));
        assertThat(address.getAddress3(), is(ADDRESS_4 + SPACE + ADDRESS_5));
    }

    private List<Hearing> createHearingsForApplication() {
        return asList(
                Hearing.hearing()
                        .withId(HEARING_ID_1)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE_APPLICATION).build())
                        .withApplicantCounsels(createApplicationCounsels())
                        .withCourtApplications(asList(createCourtApplication()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)
                        .build(),
                Hearing.hearing()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE_APPLICATION).build())
                        .withApplicantCounsels(createApplicationCounsels())
                        .withRespondentCounsels(createRespondentCounsel())
                        .withCourtApplications(asList(createCourtApplication()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)
                        .build()
        );
    }

    private List<Hearing> createHearingsForApplicationWithHearingHeardInYouthCourt() {
        return asList(
                Hearing.hearing()
                        .withId(HEARING_ID_1)
                        .withHearingDays(createHearingDays())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE_APPLICATION).build())
                        .withApplicantCounsels(createApplicationCounsels())
                        .withCourtApplications(asList(createCourtApplication()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)

                        .build(),
                Hearing.hearing()
                        .withId(HEARING_ID_2)
                        .withHearingDays(createHearingDays2())
                        .withCourtCentre(createCourtCenter())
                        .withJudiciary(createJudiciary())
                        .withType(HearingType.hearingType().withDescription(HEARING_TYPE_APPLICATION).build())
                        .withApplicantCounsels(createApplicationCounsels())
                        .withRespondentCounsels(createRespondentCounsel())
                        .withCourtApplications(asList(createCourtApplication()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withReportingRestrictionReason(REPORTING_RESTRICTION_REASON)
                        .withYouthCourtDefendantIds(asList(DEFANDANT_ID))
                        .withYouthCourt(YouthCourt.youthCourt()
                                .withCourtCode(1002)
                                .withYouthCourtId(randomUUID())
                                .withName("Youth Court Name")
                                .withWelshName("Welsh Youth Court Name")
                                .build())
                        .build()
        );
    }

    private List<ApplicantCounsel> createApplicationCounsels() {
        return asList(ApplicantCounsel.applicantCounsel()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .build());
    }

    private List<RespondentCounsel> createRespondentCounsel() {
        return asList(RespondentCounsel
                .respondentCounsel()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .build());
    }


    private List<JudicialRole> createJudiciary() {
        return asList(
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(true)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType(ROLE_DISPLAY_NAME)
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Magistrate")
                                        .build()
                        )
                        .build(),
                JudicialRole.judicialRole()
                        .withIsDeputy(true)
                        .withFirstName(FIRST_NAME)
                        .withLastName(LAST_NAME)
                        .withIsBenchChairman(false)
                        .withJudicialId(randomUUID())
                        .withJudicialRoleType(
                                JudicialRoleType.judicialRoleType()
                                        .withJudicialRoleTypeId(randomUUID())
                                        .withJudiciaryType("Circuit Judge")
                                        .build()
                        )
                        .build()
        );
    }

    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName(COURT_NAME)
                .withAddress(createAddress())
                .build();
    }

    private List<JudicialResult> createResults() {
        return asList(
                JudicialResult.judicialResult()
                        .withIsAvailableForCourtExtract(true)
                        .withLabel(LABEL)
                        .withJudicialResultPrompts(createPrompts())
                        .withDelegatedPowers(createDelegatedPower())
                        .withOrderedDate(ORDERED_DATE)
                        .build()
        );
    }

    private DelegatedPowers createDelegatedPower() {
        return DelegatedPowers.delegatedPowers()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .build();
    }

    private List<JudicialResultPrompt> createPrompts() {
        return asList(
                JudicialResultPrompt.judicialResultPrompt()
                        .withLabel(LABEL)
                        .withCourtExtract(COURT_EXTRACT)
                        .withValue(PROMPT_VALUE)
                        .build()
        );
    }

    private Address createAddress() {
        return Address.address()
                .withAddress1(ADDRESS_1)
                .withAddress2(ADDRESS_2)
                .withAddress3(ADDRESS_3)
                .withAddress4(ADDRESS_4)
                .withAddress5(ADDRESS_5)
                .withPostcode(POST_CODE)
                .build();
    }

    private List<HearingDay> createHearingDays() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build()
        );
    }

    private List<HearingDay> createHearingDays2() {
        return asList(
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_1))
                        .build(),
                HearingDay.hearingDay()
                        .withSittingDay(ZonedDateTimes.fromString(HEARING_DATE_2))
                        .build()
        );
    }

    private ContactNumber createContact() {
        return ContactNumber.contactNumber()
                .withFax("fax")
                .withHome("home")
                .withMobile("mobile")
                .build();
    }

    private Person createPerson() {
        return Person.person()
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withAddress(createAddress())
                .withTitle("DR")
                .build();
    }

    private CourtApplication createCourtApplication() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationParty())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(now())
                .withJudicialResults(createResults())
                .withRespondents(createRespondents())
                .withApplicationReference(APPLICATION_REFERENCE)
                .build();
    }

    private List<CourtApplicationParty> createRespondents() {
        return Arrays.asList(CourtApplicationParty.courtApplicationParty()
                        .withId(UUID.randomUUID())
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .build())
                        .withRepresentationOrganisation(createOrganisation())
                        .withOrganisation(createOrganisation())
                        .build()
                        );
    }

    private CourtApplicationParty createApplicationParty() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(UUID.randomUUID())
                .withRepresentationOrganisation(createOrganisation())
                .withPersonDetails(createPerson())
                .build();
    }

    private CourtApplicationParty createApplicationPartyWithDefandant() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(UUID.randomUUID())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(DEFANDANT_ID)
                        .build())
                .withPersonDetails(createPerson())
                .build();
    }


    private CourtApplicationType createCourtApplicationType() {
        return CourtApplicationType.courtApplicationType()
                .withType(APPLICATION_TYPE)
                //.withApplicantSynonym(SYNONYM)
                .withAppealFlag(true)
                //.withRespondentSynonym(SYNONYM + "R")
                .build();
    }

    private Organisation createOrganisation() {
        return Organisation.organisation()
                .withAddress(createAddress())
                .withName(ORG_NAME)
                .withContact(createContact())
                .build();
    }

    private JsonObject createJudiciaryJsonObject() {
        final JsonObjectBuilder judiciaryBuilder = Json.createObjectBuilder();
        judiciaryBuilder.add("value", "desc");
        return judiciaryBuilder.build();
    }

    private CourtApplication createCourtApplictionWithDefendantApplicant() {
        return CourtApplication.courtApplication()
                .withId(APPLICATION_ID)
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicant(createApplicationPartyWithDefandant())
                .withType(createCourtApplicationType())
                .withApplicationReceivedDate(now())
                .withJudicialResults(createResults())
                .withRespondents(createRespondents())
                .withApplicationReference(APPLICATION_REFERENCE)
                .build();
    }
}
