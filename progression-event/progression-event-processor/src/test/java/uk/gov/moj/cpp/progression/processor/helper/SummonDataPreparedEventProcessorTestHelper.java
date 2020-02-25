package uk.gov.moj.cpp.progression.processor.helper;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.SummonsData;
import uk.gov.justice.core.courts.SummonsRequired;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class SummonDataPreparedEventProcessorTestHelper {
    public static SummonsData generateSummonsData(final SummonsRequired summonsRequired,
                                                  final UUID caseId,
                                                  final UUID defendantId,
                                                  final UUID courtCentreId,
                                                  final UUID referralId) {
        return SummonsData.summonsData()
                .withConfirmedProsecutionCaseIds(Arrays.asList(generateConfirmedProsecutionId(caseId, defendantId)))
                .withCourtCentre(generateCourtCentre(courtCentreId))
                .withHearingDateTime(ZonedDateTimes.fromString("2018-04-01T13:00:00.000Z"))
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withSummonsRequired(summonsRequired)
                        .withProsecutionCaseId(caseId)
                        .withReferralReason(ReferralReason.referralReason()
                                .withDefendantId(defendantId)
                                .withId(referralId)
                                .build())
                        .build()))
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
                .add("referralReasons", createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("id", referralId)
                                .add("reason", "Sections 135")
                                .add("welshReason", "Reason for Welsh")
                                .add("subReason", "reason text")
                                .add("welshSubReason", "welsh reason text")
                        ))
                .build();
    }

    public static JsonObject generateProsecutionCaseJson(String caseId, String defendantId) {
        return createObjectBuilder()
                .add("prosecutionCase",
                        createObjectBuilder()
                                .add("id", caseId)
                                .add("defendants", generateDefendantArray(caseId, defendantId))
                                .add("prosecutionCaseIdentifier", generateProsecutionCaseIdentifier())
                )
                .build();
    }

    public static JsonObject generateDocumentTypeAccess(UUID id) {
        return createObjectBuilder()
                                .add("id", id.toString())
                                .add("section", "Charges")
                .build();
    }

    public static JsonArray generateDefendantArray(String caseId, String defendantId) {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("id", defendantId)
                                .add("prosecutionCaseId", caseId)
                                .add("personDefendant", generatePersonDefendant())
                                .add("associatedPersons", generateAssociatedPersonsArray())
                                .add("offences", generateOffenceArray())
                )
                .build();
    }

    public static JsonObject generateCourtCentreJson() {
        return createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Name", "Liverpool Mag Court")
                .add("lja", "1810")
                .add("address1", "176a Lavender Hill")
                .add("address2", "London")
                .add("address3", "address line 3")
                .add("address4", "address line 4")
                .add("address5", "address line 5")
                .add("postcode", "SW11 1JU")
                .build();
    }

    public static JsonObject generateCourtCentreJsonInWelsh() {
        return createObjectBuilder()
                .add("id", "f8254db1-1683-483e-afb3-b87fde5a0a26")
                .add("oucodeL1Name", "Magistrates' Courts")
                .add("oucodeL3Name", "Liverpool Mag Court")
                .add("lja", "1810")
                .add("oucodeL3WelshName", "welshName_Test")
                .add("address1", "176a Lavender Hill")
                .add("address2", "London")
                .add("address3", "address line 3")
                .add("address4", "address line 4")
                .add("address5", "address line 5")
                .add("postcode", "SW11 1JU")
                .add("isWelsh", true)
                .add("welshAddress1", "420 Catherine Court")
                .add("welshAddress2", "Trawler Road")
                .add("welshAddress3", "Clydach")
                .add("welshAddress4", "SA1 1XW")
                .build();
    }

    public static JsonObject generateLjaDetails() {
        return createObjectBuilder()
                .add("localJusticeArea",
                        createObjectBuilder()
                                .add("name", "South West London Magistrates' Court")
                                .add("nationalCourtCode", "2577")
                )
                .build();
    }

    public static JsonObject generateLjaDetailsWithWelsh(boolean welshRequired) {
        if (!welshRequired) {
            return createObjectBuilder()
                    .add("localJusticeAreas",
                            createArrayBuilder().add(createObjectBuilder()
                                    .add("name", "South West London Magistrates' Court")
                                    .add("nationalCourtCode", "2577"))
                    )
                    .build();
        }
        return createObjectBuilder()
                .add("localJusticeAreas",
                        createArrayBuilder().add(createObjectBuilder()
                                .add("name", "South West London Magistrates' Court")
                                .add("nationalCourtCode", "2577")
                                .add("welshName", "cymruCourtCentreName"))
                )
                .build();
    }

    public static JsonObject generatePersonDefendant() {
        return createObjectBuilder()
                .add("personDetails",
                        Json.createObjectBuilder()
                                .add("firstName", "Harry")
                                .add("middleName", "Jack")
                                .add("lastName", "Kane")
                                .add("dateOfBirth", "2010-01-01")
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

    public static void assertOnCourtAddress(final JsonObject courtAddress, boolean welshRequiredFlag) {
        assertThat(courtAddress.getString("line1"), is("176a Lavender Hill"));
        assertThat(courtAddress.getString("line2"), is("London"));
        assertThat(courtAddress.getString("line3"), is("address line 3"));
        assertThat(courtAddress.getString("line4"), is("address line 4"));
        assertThat(courtAddress.getString("line5"), is("address line 5"));
        if (welshRequiredFlag) {
            assertThat(courtAddress.getString("line1Welsh"), is("420 Catherine Court"));
            assertThat(courtAddress.getString("line2Welsh"), is("Trawler Road"));
            assertThat(courtAddress.getString("line3Welsh"), is("Clydach"));
            assertThat(courtAddress.getString("line4Welsh"), is("SA1 1XW"));
        }
        assertThat(courtAddress.getString("postCode"), is("SW11 1JU"));
    }

    public static JsonObject assertOnDefendant(final JsonObject summonsDataJson) {
        final JsonObject defendantJson = summonsDataJson.getJsonObject("defendant");
        assertThat(defendantJson, notNullValue());
        assertThat(defendantJson.getString("name"), is("Harry Jack Kane"));
        assertThat(defendantJson.getString("dateOfBirth"), is("2010-01-01"));
        return defendantJson;
    }

    public static JsonObject generateProsecutionCaseIdentifier() {
        return createObjectBuilder()
                .add("prosecutionAuthorityReference", "TFL12345")
                .build();
    }

    public static void assertOnSummonsData(final JsonObject summonsDataJson,
                                           final SummonsRequired sjpReferral,
                                           boolean welshRequiredFlag) {
        assertThat(summonsDataJson, notNullValue());
        assertThat(summonsDataJson.getString("subTemplateName"), is(sjpReferral.toString()));
        assertThat(summonsDataJson.getString("type"), is(sjpReferral.toString()));
        assertThat(summonsDataJson.getString("ljaCode"), is("2577"));
        assertThat(summonsDataJson.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(summonsDataJson.getString("caseReference"), is("TFL12345"));
        assertThat(summonsDataJson.getString("courtCentreName"), is("Liverpool Mag Court"));
        if (welshRequiredFlag) {
            assertThat(summonsDataJson.getString("ljaNameWelsh"), is("cymruCourtCentreName"));
            assertThat(summonsDataJson.getString("courtCentreNameWelsh"), is("welshName_Test"));
        }
    }

    public static void assertOnOffences(final JsonArray offencesJson, boolean welshRequiredFlag) {
        final JsonObject offenceJson = offencesJson.getJsonObject(0);
        assertThat(offenceJson.getString("offenceTitle"), is("off title"));
        assertThat(offenceJson.getString("offenceLegislation"), is("off legis"));
        assertThat(offenceJson.getString("wording"), is("Offence Wording"));
        if (welshRequiredFlag) {
            assertThat(offenceJson.getString("offenceTitleWelsh"), is("off title welsh"));
            assertThat(offenceJson.getString("offenceLegislationWelsh"), is("off legis welsh"));
            assertThat(offenceJson.getString("wordingWelsh"), is("Offence Wording Welsh"));
        }
    }

    public static void assertOnHearingCourtDetails(final JsonObject hearingCourtDetails) {
        assertThat(hearingCourtDetails.getString("courtName"), is("Liverpool Mag Court"));
        assertThat(hearingCourtDetails.getString("hearingDate"), is("2018-04-01"));
        assertThat(hearingCourtDetails.getString("hearingTime"), is("2:00 PM"));
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

    public static void assertOnReferralReason(final JsonObject referralReason, boolean welshRequired) {
        assertThat(referralReason, notNullValue());
        assertThat(referralReason.getString("referralReason"), is("Sections 135"));
        assertThat(referralReason.getString("referralText"), is("reason text"));
        if (welshRequired) {
            assertThat(referralReason.getString("referralTextWelsh"), is("welsh reason text"));
            assertThat(referralReason.getString("referralReasonWelsh"), is("Reason for Welsh"));
        }
    }

    public static JsonArray generateOffenceArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("offenceTitle", "off title")
                                .add("offenceTitleWelsh", "off title welsh")
                                .add("offenceLegislation", "off legis")
                                .add("offenceLegislationWelsh", "off legis welsh")
                                .add("wording", "Offence Wording")
                                .add("wordingWelsh", "Offence Wording Welsh")
                )
                .build();
    }

    public static JsonArray generateAssociatedPersonsArray() {
        return createArrayBuilder()
                .add(
                        createObjectBuilder()
                                .add("person",
                                        Json.createObjectBuilder()
                                                .add("firstName", "William")
                                                .add("middleName", "Senior")
                                                .add("lastName", "Kane")
                                                .add("dateOfBirth", "2010-01-01")
                                                .add("address", createObjectBuilder()
                                                        .add("address1", "22 Acacia Avenue")
                                                        .add("address2", "Acacia Town")
                                                        .add("address3", "Acacia City")
                                                        .add("address4", "Acacia District")
                                                        .add("address5", "Acacia County")
                                                        .add("postcode", "AC1 4AC")
                                                )
                                )
                                .add("role", "role text")
                )
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

    public static JsonEnvelope createEnvelopeForGenerateSummons() {

        final UUID uuid = randomUUID();
        final UUID userId = randomUUID();

        final JsonObject payload = Json.createObjectBuilder()
                .add("organisationId", randomUUID().toString())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.event.summons-data-prepared")
                .withId(uuid)
                .withUserId(userId.toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
        return commandEnvelope;
    }

}