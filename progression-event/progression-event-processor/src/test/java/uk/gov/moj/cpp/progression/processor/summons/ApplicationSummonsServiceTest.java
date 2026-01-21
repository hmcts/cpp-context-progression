package uk.gov.moj.cpp.progression.processor.summons;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.isEmptyString;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.CourtOrder.courtOrder;
import static uk.gov.justice.core.courts.LegalEntityDefendant.legalEntityDefendant;
import static uk.gov.justice.core.courts.LjaDetails.ljaDetails;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.SummonsDataPrepared.summonsDataPrepared;
import static uk.gov.justice.core.courts.SummonsType.APPLICATION;
import static uk.gov.justice.core.courts.SummonsType.BREACH;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.generateCourtCentreJson;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.getAddress;
import static uk.gov.moj.cpp.progression.processor.helper.DataPreparedEventProcessorTestHelper.getAssociatedPerson;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsDataPrepared;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.summons.SummonsAddress;
import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.core.courts.summons.SummonsHearingCourtDetails;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationSummonsServiceTest {

    private static final UUID APPLICATION_ID = randomUUID();
    private static final UUID COURT_CENTRE_ID = randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.fromString("d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a");
    private static final UUID SUBJECT_ID = randomUUID();
    private static final UUID PROSECUTION_AUTHORITY_ID = randomUUID();
    private static final String LJA_CODE = "ljaCode";

    private static final ZonedDateTime HEARING_DATE_TIME = ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z");
    private static final String APPLICATION_TYPE = randomAlphabetic(10);
    private static final String LEGISLATION = randomAlphabetic(15);
    private static final String APPLICATION_PARTICULARS = randomAlphabetic(15);
    private static final String WELSH_VALUE_SUFFIX = "welsh";
    private static final String APPLICATION_REFERENCE = randomAlphabetic(15);
    private static final LocalDate BREACH_ORDER_DATE = LocalDate.now();
    private static final String COURT_NAME = randomAlphabetic(25);
    private static final String LJA_NAME = "ljaName";
    private static final String WELSH_LJA_NAME = "welshLjaName";
    private static final String DEFENDANT_FIRST_NAME = randomAlphabetic(8);
    private static final String DEFENDANT_LAST_NAME = randomAlphabetic(8);
    private static final String INDIVIDUAL_FIRST_NAME = randomAlphabetic(8);
    private static final String INDIVIDUAL_LAST_NAME = randomAlphabetic(8);
    private static final String ORGANISATION_NAME = randomAlphabetic(8);
    private static final String PROSECUTION_AUTHORITY_NAME = randomAlphabetic(8);
    private static final String LEGAL_ENTITY_NAME = randomAlphabetic(8);
    private static final LocalDate SUBJECT_DATE_OF_BIRTH = LocalDate.now().minusYears(20);

    private ApplicationSummonsService applicationSummonsService = new ApplicationSummonsService();

    public static Stream<Arguments> applicationSummonsSpecifications() {
        return Stream.of(
                // summons required, welsh values, subject type
                Arguments.of(APPLICATION, true, PartyType.INDIVIDUAL),
                Arguments.of(APPLICATION, false, PartyType.ORGANISATION),
                Arguments.of(BREACH, true, PartyType.MASTER_DEFENDANT_PERSON),
                Arguments.of(BREACH, false, PartyType.MASTER_DEFENDANT_LEGAL_ENTITY),
                Arguments.of(BREACH, false, PartyType.PROSECUTION_AUTHORITY)
        );
    }

    public static Stream<Arguments> applicationSummonsSpecificationsWithVariousCosts() {
        return Stream.of(
                // summons required, welsh values, subject type
                Arguments.of(APPLICATION, "£0", true),
                Arguments.of(APPLICATION, "£0", false),
                Arguments.of(BREACH, "", true),
                Arguments.of(BREACH, null , false),
                Arguments.of(BREACH, "£200", true)
        );
    }

    @MethodSource("applicationSummonsSpecifications")
    @ParameterizedTest
    void generateSummonsDocumentContent(final SummonsType summonsRequired, final boolean welshValuesPresent, final PartyType partyType) {

        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplication(summonsRequired);
        final CourtApplication courtApplication = getCourtApplication(welshValuesPresent, partyType);
        final JsonObject courtCentreJson = generateCourtCentreJson(true);
        final Optional<LjaDetails> optionalLjaDetails = getLjaDetails();

        final CourtApplicationPartyListingNeeds subjectNeeds = summonsDataPrepared.getSummonsData().getCourtApplicationPartyListingNeeds().get(0);
        final SummonsDocumentContent applicationSummonsPayload = applicationSummonsService.generateSummonsDocumentContent(summonsDataPrepared, courtApplication, subjectNeeds, courtCentreJson, optionalLjaDetails);

        assertThat(applicationSummonsPayload, notNullValue());
        assertThat(applicationSummonsPayload.getSubTemplateName(), is(summonsRequired.toString()));
        assertThat(applicationSummonsPayload.getType(), is(summonsRequired.toString()));
        assertThat(applicationSummonsPayload.getCaseReference(), is(APPLICATION_REFERENCE));
        assertThat(applicationSummonsPayload.getIssueDate(), is(LocalDate.now()));

        assertThat(applicationSummonsPayload.getLjaCode(), is(LJA_CODE));
        assertThat(applicationSummonsPayload.getLjaName(), is(LJA_NAME));
        assertThat(applicationSummonsPayload.getLjaNameWelsh(), is(WELSH_LJA_NAME));

        assertThat(applicationSummonsPayload.getProsecutorCosts(), is("£300.00"));
        assertThat(applicationSummonsPayload.getPersonalService(), is(true));

        if (PartyType.MASTER_DEFENDANT_PERSON == partyType) {
            assertThat(applicationSummonsPayload.getDefendant().getName(), is(DEFENDANT_FIRST_NAME + " " + DEFENDANT_LAST_NAME));
            assertThat(applicationSummonsPayload.getDefendant().getDateOfBirth(), is(SUBJECT_DATE_OF_BIRTH.toString()));
        } else if (PartyType.MASTER_DEFENDANT_LEGAL_ENTITY == partyType) {
            assertThat(applicationSummonsPayload.getDefendant().getName(), is(LEGAL_ENTITY_NAME));
            assertThat(applicationSummonsPayload.getDefendant().getDateOfBirth(), nullValue());
        } else if (PartyType.PROSECUTION_AUTHORITY == partyType) {
            assertThat(applicationSummonsPayload.getDefendant().getName(), is(PROSECUTION_AUTHORITY_NAME));
            assertThat(applicationSummonsPayload.getDefendant().getDateOfBirth(), nullValue());
        } else if (PartyType.ORGANISATION == partyType) {
            assertThat(applicationSummonsPayload.getDefendant().getName(), is(ORGANISATION_NAME));
            assertThat(applicationSummonsPayload.getDefendant().getDateOfBirth(), nullValue());
        } else {
            assertThat(applicationSummonsPayload.getDefendant().getName(), is(INDIVIDUAL_FIRST_NAME + " " + INDIVIDUAL_LAST_NAME));
            assertThat(applicationSummonsPayload.getDefendant().getDateOfBirth(), isEmptyString());
        }

        final SummonsAddress defendantAddress = applicationSummonsPayload.getDefendant().getAddress();
        assertThat(defendantAddress.getLine1(), is("subject line 1"));
        assertThat(defendantAddress.getLine2(), is("subject line 2"));
        assertThat(defendantAddress.getLine3(), is(""));
        assertThat(defendantAddress.getLine4(), is(""));
        assertThat(defendantAddress.getLine5(), is(""));
        assertThat(defendantAddress.getPostCode(), is("CD1 5TG"));

        assertThat(applicationSummonsPayload.getAddressee().getName(), is(applicationSummonsPayload.getDefendant().getName()));
        final SummonsAddress addresseeAddress = applicationSummonsPayload.getAddressee().getAddress();
        assertThat(addresseeAddress.getLine1(), is(defendantAddress.getLine1()));
        assertThat(addresseeAddress.getLine2(), is(defendantAddress.getLine2()));
        assertThat(addresseeAddress.getLine3(), is(defendantAddress.getLine3()));
        assertThat(addresseeAddress.getLine4(), is(defendantAddress.getLine4()));
        assertThat(addresseeAddress.getLine5(), is(defendantAddress.getLine5()));
        assertThat(addresseeAddress.getPostCode(), is(defendantAddress.getPostCode()));

        final SummonsHearingCourtDetails hearingCourtDetails = applicationSummonsPayload.getHearingCourtDetails();
        assertThat(hearingCourtDetails.getCourtName(), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getCourtNameWelsh(), is("Liverpool Mag Court Welsh"));
        assertThat(hearingCourtDetails.getCourtRoomName(), is("room name english"));
        assertThat(hearingCourtDetails.getCourtRoomNameWelsh(), is("room name welsh"));
        assertThat(hearingCourtDetails.getHearingDate(), is("2018-04-01"));
        assertThat(hearingCourtDetails.getHearingTime(), equalToIgnoringCase("2:00 PM"));
        final SummonsAddress hearingCourtAddress = hearingCourtDetails.getCourtAddress();
        assertThat(hearingCourtAddress.getLine1(), is("176a Lavender Hill"));
        assertThat(hearingCourtAddress.getLine2(), is("London"));
        assertThat(hearingCourtAddress.getLine3(), is("address line 3"));
        assertThat(hearingCourtAddress.getLine4(), is("address line 4"));
        assertThat(hearingCourtAddress.getLine5(), is("address line 5"));
        assertThat(hearingCourtAddress.getLine1Welsh(), is("176a Lavender Hill Welsh"));
        assertThat(hearingCourtAddress.getLine2Welsh(), is("London Welsh"));
        assertThat(hearingCourtAddress.getLine3Welsh(), is("address line 3 Welsh"));
        assertThat(hearingCourtAddress.getLine4Welsh(), is("address line 4 Welsh"));
        assertThat(hearingCourtAddress.getLine5Welsh(), is("address line 5 Welsh"));
        assertThat(hearingCourtAddress.getPostCode(), is("SW11 1JU"));

        assertThat(applicationSummonsPayload.getProsecutor(), nullValue());

        final String welshSuffix = welshValuesPresent ? WELSH_VALUE_SUFFIX : "";
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationType(), is(APPLICATION_TYPE));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationTypeWelsh(), is(APPLICATION_TYPE + welshSuffix));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationLegislation(), is(LEGISLATION));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationLegislationWelsh(), is(LEGISLATION + welshSuffix));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationParticulars(), is(APPLICATION_PARTICULARS));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationParticularsWelsh(), is(APPLICATION_PARTICULARS));
        assertThat(applicationSummonsPayload.getApplicationContent().getApplicationReference(), is(APPLICATION_REFERENCE));

        assertThat(applicationSummonsPayload.getBreachContent().getBreachedOrderDate(), is(BREACH_ORDER_DATE.toString()));
        assertThat(applicationSummonsPayload.getBreachContent().getOrderingCourt(), is(COURT_NAME));
    }

    @MethodSource("applicationSummonsSpecificationsWithVariousCosts")
    @ParameterizedTest
    void generateSummonsDocumentContentWithVariousCosts(final SummonsType summonsType, final String costString, final boolean isWelsh) {
        final SummonsDataPrepared summonsDataPrepared = getSummonsDataPreparedForApplicationWithCostAndLanguageNeeds(summonsType, costString, isWelsh);
        final CourtApplication courtApplication = getCourtApplication(false, PartyType.INDIVIDUAL);
        final JsonObject courtCentreJson = generateCourtCentreJson(true);
        final Optional<LjaDetails> optionalLjaDetails = getLjaDetails();

        final CourtApplicationPartyListingNeeds subjectNeeds = summonsDataPrepared.getSummonsData().getCourtApplicationPartyListingNeeds().get(0);
        final SummonsDocumentContent applicationSummonsPayload = applicationSummonsService.generateSummonsDocumentContent(summonsDataPrepared, courtApplication, subjectNeeds, courtCentreJson, optionalLjaDetails);

        assertThat(applicationSummonsPayload, notNullValue());
        if (isEmpty(costString)) {
            assertThat(applicationSummonsPayload.getProsecutorCosts(), is(""));
        } else if (costString.contains("£0")) {
            assertThat(applicationSummonsPayload.getProsecutorCosts(), is(isWelsh ? "Heb ei bennu" : "Unspecified"));
        } else {
            assertThat(applicationSummonsPayload.getProsecutorCosts(), is(costString));
        }
    }

    private SummonsDataPrepared getSummonsDataPreparedForApplication(final SummonsType summonsRequired) {

        final SummonsData summonsData = summonsData()
                .withConfirmedApplicationIds(newArrayList(APPLICATION_ID))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withCourtApplicationPartyListingNeeds(newArrayList(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withSummonsRequired(summonsRequired)
                        .withCourtApplicationId(APPLICATION_ID)
                        .withCourtApplicationPartyId(SUBJECT_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorEmailAddress("test@test.com")
                                .withProsecutorCost("£300.00")
                                .withPersonalService(true)
                                .withSummonsSuppressed(true)
                                .build())
                        .withHearingLanguageNeeds(HearingLanguage.ENGLISH)
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }


    private SummonsDataPrepared getSummonsDataPreparedForApplicationWithCostAndLanguageNeeds(final SummonsType summonsRequired, final String costString, final Boolean isWelsh) {

        final SummonsData summonsData = summonsData()
                .withConfirmedApplicationIds(newArrayList(APPLICATION_ID))
                .withCourtCentre(courtCentre()
                        .withId(COURT_CENTRE_ID)
                        .withRoomId(COURT_ROOM_ID)
                        .build())
                .withHearingDateTime(HEARING_DATE_TIME)
                .withCourtApplicationPartyListingNeeds(newArrayList(CourtApplicationPartyListingNeeds.courtApplicationPartyListingNeeds()
                        .withSummonsRequired(summonsRequired)
                        .withCourtApplicationId(APPLICATION_ID)
                        .withCourtApplicationPartyId(SUBJECT_ID)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorEmailAddress("test@test.com")
                                .withProsecutorCost(costString)
                                .withPersonalService(true)
                                .withSummonsSuppressed(true)
                                .build())
                        .withHearingLanguageNeeds(isWelsh ? HearingLanguage.WELSH : HearingLanguage.ENGLISH)
                        .build()))
                .build();

        return summonsDataPrepared().withSummonsData(summonsData).build();
    }


    private CourtApplication getCourtApplication(final boolean welshValuesPresent, final PartyType partyType) {
        final String welshSuffix = welshValuesPresent ? WELSH_VALUE_SUFFIX : "";
        final ProsecutingAuthority prosecutingAuthority = prosecutingAuthority().withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID).build();
        final CourtApplicationParty applicant = courtApplicationParty().withProsecutingAuthority(prosecutingAuthority).build();

        final CourtApplicationParty.Builder subjectBuilder = courtApplicationParty()
                .withOrganisationPersons(newArrayList(getAssociatedPerson()))
                .withId(SUBJECT_ID);
        buildApplicationSubject(subjectBuilder, partyType);

        final CourtApplicationParty subject = subjectBuilder.build();
        final CourtApplicationType.Builder applicationTypeBuilder = courtApplicationType();

        applicationTypeBuilder
                .withType(APPLICATION_TYPE)
                .withTypeWelsh(APPLICATION_TYPE + welshSuffix)
                .withLegislation(LEGISLATION)
                .withLegislationWelsh(LEGISLATION + welshSuffix);

        final CourtOrder courtOrder = courtOrder()
                .withOrderDate(BREACH_ORDER_DATE)
                .withOrderingCourt(courtCentre().withName(COURT_NAME).build())
                .build();

        return courtApplication()
                .withId(APPLICATION_ID)
                .withApplicant(applicant)
                .withSubject(subject)
                .withType(applicationTypeBuilder.build())
                .withApplicationParticulars(APPLICATION_PARTICULARS)
                .withApplicationReference(APPLICATION_REFERENCE)
                .withCourtOrder(courtOrder)
                .build();

    }

    private void buildApplicationSubject(final CourtApplicationParty.Builder builder, final PartyType partyType) {
        switch (partyType) {
            case MASTER_DEFENDANT_PERSON:
                builder.withMasterDefendant(masterDefendant()
                        .withPersonDefendant(personDefendant()
                                .withPersonDetails(person()
                                        .withFirstName(DEFENDANT_FIRST_NAME)
                                        .withLastName(DEFENDANT_LAST_NAME)
                                        .withDateOfBirth(SUBJECT_DATE_OF_BIRTH)
                                        .withAddress(getAddress("subject line 1", "subject line 2", "", "", "", "CD1 5TG"))
                                        .build())
                                .build())
                        .build());
                break;
            case MASTER_DEFENDANT_LEGAL_ENTITY:
                builder.withMasterDefendant(masterDefendant()
                        .withLegalEntityDefendant(legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withName(LEGAL_ENTITY_NAME)
                                        .withAddress(getAddress("subject line 1", "subject line 2", "", "", "", "CD1 5TG"))
                                        .build())
                                .build()).build());
                break;
            case INDIVIDUAL:
                builder.withPersonDetails(person()
                        .withFirstName(INDIVIDUAL_FIRST_NAME)
                        .withLastName(INDIVIDUAL_LAST_NAME)
                        .withAddress(getAddress("subject line 1", "subject line 2", "", "", "", "CD1 5TG"))
                        .build());
                break;
            case ORGANISATION:
                builder.withOrganisation(Organisation.organisation()
                        .withName(ORGANISATION_NAME)
                        .withAddress(getAddress("subject line 1", "subject line 2", "", "", "", "CD1 5TG"))
                        .build());
                break;
            case PROSECUTION_AUTHORITY:
                builder.withProsecutingAuthority(prosecutingAuthority()
                        .withName(PROSECUTION_AUTHORITY_NAME)
                        .withAddress(getAddress("subject line 1", "subject line 2", "", "", "", "CD1 5TG"))
                        .build());

        }
    }

    private Optional<LjaDetails> getLjaDetails() {
        return of(ljaDetails().withLjaCode(LJA_CODE).withLjaName(LJA_NAME).withWelshLjaName(WELSH_LJA_NAME).build());
    }
}