package uk.gov.moj.cpp.progression.processor.helper;

import static com.google.common.collect.Lists.newArrayList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AssociatedPerson.associatedPerson;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static uk.gov.justice.core.courts.ListDefendantRequest.listDefendantRequest;
import static uk.gov.justice.core.courts.Person.person;
import static uk.gov.justice.core.courts.ReferralReason.referralReason;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsData.summonsData;
import static uk.gov.justice.core.courts.summons.SummonsAddress.summonsAddress;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class DataPreparedEventProcessorTestHelper {

    private static final String CJS_OFFENCE_CODE = "code123";
    private static final ZonedDateTime HEARING_DATE_TIME = ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z");
    private static final JsonObjectToObjectConverter JSON_OBJECT_TO_OBJECT_CONVERTER = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final String LJA_CODE = "2577";
    private static final String LJA_NAME = "South West London Magistrates' Court";
    private static final String LJA_NAME_WELSH = "cymruCourtCentreName";
    private static final String PROSECUTOR_NAME = "TFL";

    public static SummonsData generateSummonsData(final SummonsType summonsRequired,
                                                  final UUID caseId,
                                                  final UUID defendantId,
                                                  final UUID courtCentreId,
                                                  final UUID referralId,
                                                  final boolean summonsSuppressed) {
        return summonsData()
                .withConfirmedProsecutionCaseIds(Arrays.asList(generateConfirmedProsecutionId(caseId, defendantId)))
                .withCourtCentre(generateCourtCentre(courtCentreId))
                .withHearingDateTime(HEARING_DATE_TIME)
                .withListDefendantRequests(Arrays.asList(listDefendantRequest()
                        .withSummonsRequired(summonsRequired)
                        .withProsecutionCaseId(caseId)
                        .withSummonsApprovedOutcome(summonsApprovedOutcome()
                                .withProsecutorEmailAddress("test@test.com")
                                .withProsecutorCost("£300.00")
                                .withPersonalService(true)
                                .withSummonsSuppressed(summonsSuppressed)
                                .build())
                        .withReferralReason(referralReason()
                                .withDefendantId(defendantId)
                                .withId(referralId)
                                .build())
                        .build()))
                .build();
    }

    public static CourtCentre generateCourtCentre(final UUID courtCentreId) {
        return CourtCentre.courtCentre()
                .withId(courtCentreId)
                .withName("00ObpXuu51")
                .withRoomId(UUID.fromString("d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a"))
                .withRoomName("JK2Y7hu0Tc")
                .withWelshName("3IpJDfdfhS")
                .withWelshRoomName("hm60SAXokc")
                .build();
    }

    public static ConfirmedProsecutionCaseId generateConfirmedProsecutionId(UUID caseID, UUID defendantId) {
        return ConfirmedProsecutionCaseId.confirmedProsecutionCaseId()
                .withId(caseID)
                .withConfirmedDefendantIds(Collections.singletonList(defendantId))
                .build();
    }

    public static JsonObject generateReferralReasonsJson(String referralId) {

        return createObjectBuilder()
                .add("id", referralId)
                .add("reason", "Sections 135")
                .add("welshReason", "Reason for Welsh")
                .add("subReason", "reason text")
                .add("welshSubReason", "welsh reason text")
                .add("summonsWording", "summonsWording text")
                .add("summonsWordingWelsh", "summonsWordingWelsh  text").build();
    }

    public static ProsecutionCase generateProsecutionCase(String caseId, String defendantId, final String summonsCode, final boolean isYouth) {
        final JsonObject prosecutionCaseJson = generateProsecutionCaseJson(caseId, defendantId, summonsCode, isYouth, false);
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    public static ProsecutionCase generateCivilProsecutionCase(String caseId, String defendantId, final String summonsCode, final boolean isYouth) {
        final JsonObject prosecutionCaseJson = generateProsecutionCaseJson(caseId, defendantId, summonsCode, isYouth, true);
        return JSON_OBJECT_TO_OBJECT_CONVERTER.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
    }

    public static JsonObject generateProsecutionCaseJson(String caseId, String defendantId, final String summonsCode, final boolean isYouth, final boolean isCivil) {
        return createObjectBuilder()
                .add("prosecutionCase",
                        createObjectBuilder()
                                .add("id", caseId)
                                .add("defendants", generateDefendantArray(caseId, defendantId, isYouth))
                                .add("prosecutionCaseIdentifier", generateProsecutionCaseIdentifier())
                                .add("summonsCode", summonsCode)
                                .add("initiationCode", "S")
                                .add("statementOfFacts", "dummy statement of facts")
                                .add("statementOfFactsWelsh", "dummy statement of facts in welsh")
                                .add("isCivil", isCivil)

                )
                .build();
    }

    public static JsonObject generateProsecutionCaseIdentifier() {
        return createObjectBuilder()
                .add("prosecutionAuthorityReference", "TFL12345")
                .build();
    }

    public static JsonObject generateDocumentTypeAccess(UUID id) {
        return createObjectBuilder()
                .add("id", id.toString())
                .add("section", "Charges")
                .build();
    }

    public static JsonArray generateDefendantArray(final String caseId, final String defendantId, final boolean isYouth) {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("id", defendantId)
                                .add("prosecutionCaseId", caseId)
                                .add("personDefendant", generatePersonDefendant(isYouth))
                                .add("associatedPersons", isYouth ? generateAssociatedPersonsArray() : JsonValue.NULL)
                                .add("offences", generateOffenceArray())
                )
                .build();
    }

    public static JsonObject generatePersonDefendant(final boolean isYouth) {
        final ZonedDateTime defendantDateOfBirth = isYouth ? HEARING_DATE_TIME.minusYears(16) : HEARING_DATE_TIME.minusYears(20);

        return createObjectBuilder()
                .add("personDetails",
                        createObjectBuilder()
                                .add("firstName", "Harry")
                                .add("middleName", "Jack")
                                .add("lastName", "Kane")
                                .add("dateOfBirth", DateTimeFormatter.ofPattern("YYYY-MM-dd").format(defendantDateOfBirth))
                                .add("address", createObjectBuilder()
                                        .add("address1", "22 Acacia Avenue")
                                        .add("address2", "Acacia Town")
                                        .add("address3", "Acacia City")
                                        .add("address4", "Acacia District")
                                        .add("address5", "Acacia County")
                                        .add("postcode", "AC1 4AC")
                                )
                )
                .build();
    }

    public static JsonArray generateAssociatedPersonsArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("person",
                                        createObjectBuilder()
                                                .add("firstName", "William")
                                                .add("middleName", "Senior")
                                                .add("lastName", "Kane")
                                                .add("dateOfBirth", "2010-01-01")
                                                .add("address", createObjectBuilder()
                                                        .add("address1", "33 Acacia Avenue")
                                                        .add("address2", "Bacacia Town")
                                                        .add("address3", "Bacacia City")
                                                        .add("address4", "Bacacia District")
                                                        .add("address5", "Bacacia County")
                                                        .add("postcode", "BC1 4AC")
                                                )
                                )
                                .add("role", "role text")
                )
                .build();
    }

    public static JsonArray generateOffenceArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("offenceCode", CJS_OFFENCE_CODE)
                                .add("offenceTitle", "off title")
                                .add("offenceTitleWelsh", "off title welsh")
                                .add("offenceLegislation", "off legis")
                                .add("offenceLegislationWelsh", "off legis welsh")
                                .add("wording", "Offence Wording")
                                .add("wordingWelsh", "Offence Wording Welsh")
                )
                .build();
    }

    public static JsonObject generateCourtCentreJson(final boolean isWelsh) {
        return createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Name", "Liverpool Mag Court")
                .add("oucodeL3WelshName", "Liverpool Mag Court Welsh")
                .add("lja", "1810")
                .add("address1", "176a Lavender Hill")
                .add("address2", "London")
                .add("address3", "address line 3")
                .add("address4", "address line 4")
                .add("address5", "address line 5")
                .add("welshAddress1", "176a Lavender Hill Welsh")
                .add("welshAddress2", "London Welsh")
                .add("welshAddress3", "address line 3 Welsh")
                .add("welshAddress4", "address line 4 Welsh")
                .add("welshAddress5", "address line 5 Welsh")
                .add("isWelsh", isWelsh)
                .add("postcode", "SW11 1JU")
                .add("courtRooms", createArrayBuilder().add(createObjectBuilder()
                        .add("courtRoomId", "3333")
                        .add("courtroomName", "room name english")
                        .add("id", "d7020fe0-cd97-4ce0-84c2-fd00ff0bc48a")
                        .add("welshCourtroomName", "room name welsh"))
                )
                .build();
    }

    public static JsonObject generateLjaDetails() {
        return createObjectBuilder()
                .add("localJusticeArea",
                        createObjectBuilder()
                                .add("name", LJA_NAME)
                                .add("nationalCourtCode", LJA_CODE)
                )
                .build();
    }

    public static JsonObject generateLjaDetailsWithWelsh() {
        return createObjectBuilder()
                .add("localJusticeAreas",
                        createArrayBuilder().add(createObjectBuilder()
                                .add("name", LJA_NAME)
                                .add("nationalCourtCode", LJA_CODE)
                                .add("welshName", LJA_NAME_WELSH))
                )
                .build();
    }

    // mock service payloads
    public static Optional<List<JsonObject>> getRefDataOffences() {
        return Optional.of(newArrayList(createObjectBuilder()
                .add("cjsOffenceCode", CJS_OFFENCE_CODE)
                .add("welshoffencetitle", CJS_OFFENCE_CODE + "welshoffencetitle")
                .add("endorsableFlag", true)
                .add("dvlaCode", CJS_OFFENCE_CODE + "dvlaCode")
                .add("welshlegislation", CJS_OFFENCE_CODE + "welshlegislation")
                .build()));
    }

    public static Optional<JsonObject> getRefDataProsecutorJson() {
        return Optional.of(createObjectBuilder()
                .add("fullName", PROSECUTOR_NAME)
                .add("address", createObjectBuilder()
                        .add("address1", "77 Habershon street")
                        .add("address2", "Splott")
                        .add("address3", "Cardiff")
                        .add("postcode", "CF24 7GT"))
                .build());
    }

    public static Optional<LjaDetails> getLjaDetails() {
        return Optional.of(LjaDetails.ljaDetails().withLjaCode(LJA_CODE).withLjaName(LJA_NAME).withWelshLjaName(LJA_NAME_WELSH).build());
    }

    public static SummonsProsecutor getProsecutor() {
        return SummonsProsecutor.summonsProsecutor()
                .withName(PROSECUTOR_NAME)
                .withAddress(summonsAddress()
                        .withLine1("77 Habershon street")
                        .withLine2("Splott")
                        .withLine3("Cardiff")
                        .withPostCode("CF24 7GT")
                        .build())
                .build();
    }

    // assertion helper methods below
    public static void assertOnCourtAddress(final JsonObject courtAddress, boolean welshRequiredFlag) {
        assertThat(courtAddress.getString("line1"), is("176a Lavender Hill"));
        assertThat(courtAddress.getString("line2"), is("London"));
        assertThat(courtAddress.getString("line3"), is("address line 3"));
        assertThat(courtAddress.getString("line4"), is("address line 4"));
        assertThat(courtAddress.getString("line5"), is("address line 5"));
        assertThat(courtAddress.getString("postCode"), is("SW11 1JU"));

        if (welshRequiredFlag) {
            assertThat(courtAddress.getString("line1Welsh"), is("420 Catherine Court"));
            assertThat(courtAddress.getString("line2Welsh"), is("Trawler Road"));
            assertThat(courtAddress.getString("line3Welsh"), is("Clydach"));
            assertThat(courtAddress.getString("line4Welsh"), is("SA1 1XW"));
        }
    }

    public static void assertOnProsecutor(final JsonObject prosecutor) {
        assertThat(prosecutor.getString("name"), is(PROSECUTOR_NAME));
        assertThat(prosecutor.getJsonObject("address").getString("line1"), is("77 Habershon street"));
        assertThat(prosecutor.getJsonObject("address").getString("line2"), is("Splott"));
        assertThat(prosecutor.getJsonObject("address").getString("line3"), is("Cardiff"));
        assertThat(prosecutor.getJsonObject("address").getString("line4", EMPTY), is(""));
        assertThat(prosecutor.getJsonObject("address").getString("line5", EMPTY), is(""));
        assertThat(prosecutor.getJsonObject("address").getString("postCode"), is("CF24 7GT"));
    }

    public static JsonObject assertOnDefendant(final JsonObject summonsDataJson, final boolean isYouth) {
        final ZonedDateTime defendantDateOfBirth = isYouth ? HEARING_DATE_TIME.minusYears(16) : HEARING_DATE_TIME.minusYears(20);
        final JsonObject defendantJson = summonsDataJson.getJsonObject("defendant");
        assertThat(defendantJson, notNullValue());
        assertThat(defendantJson.getString("name"), is("Harry Jack Kane"));
        assertThat(defendantJson.getString("dateOfBirth"), is(DateTimeFormatter.ofPattern("YYYY-MM-dd").format(defendantDateOfBirth)));
        return defendantJson;
    }

    public static void assertOnSummonsData(final JsonObject summonsDataJson,
                                           final SummonsType summonsRequired,
                                           final String summonsType) {
        assertThat(summonsDataJson, notNullValue());
        assertThat(summonsDataJson.getString("subTemplateName"), is(summonsRequired.toString()));
        assertThat(summonsDataJson.getString("type"), is(summonsType));
        assertThat(summonsDataJson.getString("ljaCode"), is(LJA_CODE));
        assertThat(summonsDataJson.getString("ljaName"), is(LJA_NAME));
        assertThat(summonsDataJson.getString("ljaNameWelsh"), is(LJA_NAME_WELSH));
        assertThat(summonsDataJson.getString("caseReference"), is("TFL12345"));
        assertThat(summonsDataJson.getString("statementOfFacts"), is("dummy statement of facts"));
        assertThat(summonsDataJson.getString("statementOfFactsWelsh"), is("dummy statement of facts in welsh"));
        assertThat(summonsDataJson.getString("issueDate"), notNullValue());
        assertThat(summonsDataJson.getString("prosecutorCosts"), is("£300.00"));
        assertThat(summonsDataJson.getBoolean("personalService", false), is(true));
    }

    public static void assertOnOffences(final JsonArray offencesJson) {
        final JsonObject offenceJson = offencesJson.getJsonObject(0);
        assertThat(offenceJson.getString("offenceTitle"), is("off title"));
        assertThat(offenceJson.getString("offenceLegislation"), is("off legis"));
        assertThat(offenceJson.getString("wording"), is("Offence Wording"));

        assertThat(offenceJson.getString("offenceTitleWelsh"), is(CJS_OFFENCE_CODE + "welshoffencetitle"));
        assertThat(offenceJson.getString("offenceLegislationWelsh"), is(CJS_OFFENCE_CODE + "welshlegislation"));
        assertThat(offenceJson.getString("wordingWelsh"), is("Offence Wording Welsh"));

        assertThat(offenceJson.getString("dvlaCode"), is(CJS_OFFENCE_CODE + "dvlaCode"));
        assertThat(offenceJson.getBoolean("isEndorsable"), is(true));
        assertThat(offenceJson.getString("offenceCode"), is(CJS_OFFENCE_CODE));
    }

    public static void assertOnHearingCourtDetails(final JsonObject hearingCourtDetails) {
        assertThat(hearingCourtDetails.getString("courtName"), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getString("courtNameWelsh"), is("Liverpool Mag Court Welsh"));
        assertThat(hearingCourtDetails.getString("courtRoomName"), is("room name english"));
        assertThat(hearingCourtDetails.getString("courtRoomNameWelsh"), is("room name welsh"));
        assertThat(hearingCourtDetails.getString("hearingDate"), is("2018-04-01"));
        assertThat(hearingCourtDetails.getString("hearingTime"), equalToIgnoringCase("2:00 PM"));
    }

    public static void assertOnDefendantAddress(final JsonObject defendantAddressJson) {
        assertThat(defendantAddressJson, notNullValue());
        assertThat(defendantAddressJson.getString("line1"), is("22 Acacia Avenue"));
        assertThat(defendantAddressJson.getString("line2"), is("Acacia Town"));
        assertThat(defendantAddressJson.getString("line3"), is("Acacia City"));
        assertThat(defendantAddressJson.getString("line4"), is("Acacia District"));
        assertThat(defendantAddressJson.getString("line5"), is("Acacia County"));
        assertThat(defendantAddressJson.getString("postCode"), is("AC1 4AC"));
    }

    public static void assertOnAssociatedPersonAddress(final JsonObject associatedPersonAddressJson) {
        assertThat(associatedPersonAddressJson, notNullValue());
        assertThat(associatedPersonAddressJson.getString("line1"), is("33 Acacia Avenue"));
        assertThat(associatedPersonAddressJson.getString("line2"), is("Bacacia Town"));
        assertThat(associatedPersonAddressJson.getString("line3"), is("Bacacia City"));
        assertThat(associatedPersonAddressJson.getString("line4"), is("Bacacia District"));
        assertThat(associatedPersonAddressJson.getString("line5"), is("Bacacia County"));
        assertThat(associatedPersonAddressJson.getString("postCode"), is("BC1 4AC"));
    }

    public static void assertOnReferralReason(final JsonObject referralReason) {
        assertThat(referralReason, notNullValue());
        assertThat(referralReason.getString("referralReason"), is("Sections 135"));
        assertThat(referralReason.getString("referralText"), is("reason text"));
        assertThat(referralReason.getString("referralTextWelsh"), is("welsh reason text"));
        assertThat(referralReason.getString("referralReasonWelsh"), is("Reason for Welsh"));
    }

    public static AssociatedPerson getAssociatedPerson() {
        return associatedPerson().withPerson(
                person()
                        .withFirstName("parent first name")
                        .withMiddleName("parent middle name")
                        .withLastName("parent last name")
                        .withAddress(getAddress("parent line 1", "", "", "", "", ""))
                        .build()).build();
    }

    public static Address getAddress(final String line1, final String line2, final String line3, final String line4, final String line5, final String postcode) {
        return address()
                .withAddress1(line1)
                .withAddress2(line2)
                .withAddress3(line3)
                .withAddress4(line4)
                .withAddress5(line5)
                .withPostcode(postcode)
                .build();
    }

}