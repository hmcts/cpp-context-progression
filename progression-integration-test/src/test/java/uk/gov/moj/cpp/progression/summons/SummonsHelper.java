package uk.gov.moj.cpp.progression.summons;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCaseWithMatchers;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.getSummonsTemplate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.core.courts.summons.SummonsDocumentContent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;

public class SummonsHelper {

    private static final String PUBLIC_HEARING_CONFIRMED_PAYLOAD = "public.listing.summons-hearing-confirmed.json";
    private static final String PUBLIC_HEARING_CONFIRMED_EVENT = "public.listing.hearing-confirmed";

    private static final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    public static String getSubjectDateOfBirth(final boolean isYouth) {
        return isYouth ? "2010-01-01" : "1981-01-05";
    }

    public static UUID verifyMaterialRequestRecordedAndExtractMaterialId(final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer) {
        final Optional<JsonObject> jsonPayload = retrieveMessageBody(nowsMaterialRequestRecordedConsumer);
        assertThat(jsonPayload.isPresent(), is(true));
        return fromString(jsonPayload.get().getJsonObject("context").getString("materialId"));
    }

    public static String getLanguagePrefix(final boolean isWelsh) {
        return isWelsh ? "B" : "E";
    }

    public static void verifyTemplatePayloadValues(final String templateName, final String summonsType, String... contains) {
        verifyTemplatePayloadValues(false, templateName, summonsType, null, false, contains);
    }

    public static void verifyTemplatePayloadValues(final boolean checkAdditionalResults, final String templateName, final String summonsType, final String prosecutorCost, final boolean personalService, String... contains) {
        final Optional<JsonObject> optionalSummonPayload = getSummonsTemplate(templateName, contains);
        assertThat(optionalSummonPayload.isPresent(), is(true));

        // only high level validation done in integration test (rest covered in unit tests)
        final SummonsDocumentContent actualPayload = jsonObjectToObjectConverter.convert(optionalSummonPayload.get(), SummonsDocumentContent.class);
        assertThat(actualPayload, notNullValue());
        assertThat(actualPayload.getType(), is(summonsType));

        // only applicable to first hearing summons
        if (checkAdditionalResults) {
            assertThat(actualPayload.getPersonalService(), is(personalService));
            assertThat(actualPayload.getProsecutorCosts(), is(prosecutorCost));
        }
    }

    public static void verifyCaseDocumentAddedToCdes(final String defendantId, final String caseId, final int numberOfDocuments) {

        final Matcher[] matchers = {
                withJsonPath("$.documentIndices", hasSize(greaterThanOrEqualTo(numberOfDocuments))),
                withJsonPath("$.documentIndices[*].document.name", hasItems("Summons")),
                withJsonPath("$..defendantIds[?(@ =='" + defendantId + "')]", hasSize(greaterThanOrEqualTo(numberOfDocuments)))
        };
        getCourtDocumentsByCaseWithMatchers(USER_ID, caseId, matchers);
    }

    public static void sendPublicEventToConfirmHearingForInitiatedCase(final String hearingId, final String defendantId, final String offenceId, final String caseId, final boolean isWelsh) {
        final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

        final String payloadStr = getPayload(PUBLIC_HEARING_CONFIRMED_PAYLOAD)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("COURT_CENTRE_ID", isWelsh ? ReferenceDataStub.WELSH_COURT_ID : ReferenceDataStub.ENGLISH_COURT_ID)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("OFFENCE_ID", offenceId)
                .replaceAll("DEFENDANT_ID", defendantId);
        final JsonObject hearingConfirmedPayload = new StringToJsonObjectConverter().convert(payloadStr);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_CONFIRMED_EVENT, randomUUID()), hearingConfirmedPayload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_CONFIRMED_EVENT, publicEventEnvelope);
    }
}
