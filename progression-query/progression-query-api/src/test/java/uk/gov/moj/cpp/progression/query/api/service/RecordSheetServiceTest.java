package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.QueryClientTestBase.readJson;
import static uk.gov.moj.cpp.progression.query.api.service.RecordSheetService.RECORD_SHEET;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordSheetServiceTest {

    private static final String PROSECUTION_CASE_QUERY_VIEW_JSON = "json/prosecutionCaseQueryMultipleDefendantsResponse.json";
    private static final String PROSECUTION_CASE_QUERY = "progression.query.prosecutioncase";


    @Mock
    private CourtExtractTransformer courtExtractTransformer;

    @InjectMocks
    private RecordSheetService recordSheetService;

    @Test
    void shouldGetTrialRecordSheetPayloadForApplication() throws IOException {
        final String caseId = "63d5739e-4aa3-4d53-ae3b-4f16b2ce6c95";
        final String defendantId = "96ec1814-cfcd-4ef4-ba18-315a6c48659e";
        final String offenceId = "ca438f25-e9a4-4a6b-a26d-467674755171";
        final String defendantName = "name1";
        final UUID userId = UUID.randomUUID();
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope document = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("offenceIds", offenceId)
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(QueryClientTestBase.metadataFor("progression.query.record-sheet-for-application", randomUUID()), payload);

        final JsonObject transformerResult = createObjectBuilder().add("defendant",createObjectBuilder().add("name", defendantName).build()).build();
        when(courtExtractTransformer.getTransformedPayload(document,defendantId,RECORD_SHEET, Collections.emptyList(), userId)).thenReturn(transformerResult);

        final JsonEnvelope result = recordSheetService.getTrialRecordSheetPayloadForApplication(envelope, document,userId);

        verify(courtExtractTransformer).getTransformedPayload(document,defendantId,RECORD_SHEET, Collections.emptyList(), userId);

        final JsonArray payloadAsJsonArray = result.payloadAsJsonObject().getJsonArray("payloads");
        assertThat(payloadAsJsonArray.size(), is(1));
        final JsonObject jsonObject = payloadAsJsonArray.get(0).asJsonObject();
        assertThat(jsonObject.getJsonObject("payload"), is(transformerResult));
        assertThat(jsonObject.getString("defendantName"), is(defendantName));

    }


    @Test
    void shouldGetTrialRecordSheetPayloadForApplicationWhenMultipleOffenceAndDefendant() throws IOException {
        final String caseId = "63d5739e-4aa3-4d53-ae3b-4f16b2ce6c95";
        final String defendantId1 = "96ec1814-cfcd-4ef4-ba18-315a6c48659e";
        final String defendantId2 = "96ec1814-cfcd-4ef4-ba18-315a6c48659f";
        final String offenceId1 = "ca438f25-e9a4-4a6b-a26d-467674755171";
        final String offenceId2 = "ca438f25-e9a4-4a6b-a26d-467674755171";
        final String defendantName1= "name1";
        final String defendantName2 = "name2";
        final UUID userId = UUID.randomUUID();
        final JsonObject prosecutionCasePayload = readJson(PROSECUTION_CASE_QUERY_VIEW_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID());
        final JsonEnvelope document = JsonEnvelope.envelopeFrom(metadata, prosecutionCasePayload);
        final JsonObject payload = createObjectBuilder()
                .add("caseId", caseId)
                .add("offenceIds", offenceId1+","+offenceId2)
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(QueryClientTestBase.metadataFor("progression.query.record-sheet-for-application", randomUUID()), payload);

        final JsonObject transformerResult1 = createObjectBuilder().add("defendant",createObjectBuilder().add("name", defendantName1).build()).build();
        final JsonObject transformerResult2 = createObjectBuilder().add("defendant",createObjectBuilder().add("name", defendantName2).build()).build();

        when(courtExtractTransformer.getTransformedPayload(document,defendantId1,RECORD_SHEET, Collections.emptyList(), userId)).thenReturn(transformerResult1);
        when(courtExtractTransformer.getTransformedPayload(document,defendantId2,RECORD_SHEET, Collections.emptyList(), userId)).thenReturn(transformerResult2);
        final JsonEnvelope result = recordSheetService.getTrialRecordSheetPayloadForApplication(envelope, document,userId);

        verify(courtExtractTransformer).getTransformedPayload(document,defendantId1,RECORD_SHEET, Collections.emptyList(), userId);
        verify(courtExtractTransformer).getTransformedPayload(document,defendantId2,RECORD_SHEET, Collections.emptyList(), userId);

        final JsonArray payloadAsJsonArray = result.payloadAsJsonObject().getJsonArray("payloads");
        assertThat(payloadAsJsonArray.size(), is(2));
        final JsonObject jsonObject1 = payloadAsJsonArray.get(0).asJsonObject();
        assertThat(jsonObject1.getJsonObject("payload"), is(transformerResult1));
        assertThat(jsonObject1.getString("defendantName"), is(defendantName1));
        final JsonObject jsonObject2 = payloadAsJsonArray.get(1).asJsonObject();
        assertThat(jsonObject2.getJsonObject("payload"), is(transformerResult2));
        assertThat(jsonObject2.getString("defendantName"), is(defendantName2));

    }



}