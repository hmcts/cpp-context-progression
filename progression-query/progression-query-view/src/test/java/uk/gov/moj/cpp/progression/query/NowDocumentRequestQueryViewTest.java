package uk.gov.moj.cpp.progression.query;


import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.NowDocumentRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.NowDocumentRequestRepository;

import java.util.Arrays;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowDocumentRequestQueryViewTest {
    private static final String NOW_DOCUMENT_REQUESTS_BY_REQUEST_ID_QUERY = "progression.query.now-document-requests-by-request-id";
    private static final String NOW_DOCUMENT_REQUEST_BY_HEARING_QUERY = "progression.query.now-document-request-by-hearing";
    private static final String REQUEST_ID_PARAM = "requestId";
    private static final String HEARING_ID_PARAM = "hearingId";
    private static final String MATERIAL_ID_PARAM = "materialId";
    private static final String NOW_DOCUMENT_REQUESTS_PARAM = "nowDocumentRequests";
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();

    @InjectMocks
    private NowDocumentRequestQueryView nowDocumentRequestQueryView;

    @Mock
    private NowDocumentRequestRepository nowDocumentRequestRepository;

    @Test
    public void shouldFindNowDocumentRequestByRequestId() {
        final UUID requestId = UUID.randomUUID();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add(REQUEST_ID_PARAM, requestId.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(NOW_DOCUMENT_REQUESTS_BY_REQUEST_ID_QUERY).build(),
                jsonObject);
        final NowDocumentRequestEntity nowDocumentRequestEntity = nowDocumentRequestEntity(requestId);

        when(nowDocumentRequestRepository.findByRequestId(requestId)).thenReturn(Arrays.asList(nowDocumentRequestEntity));

        final JsonEnvelope jsonEnvelopeOut = nowDocumentRequestQueryView.getNowDocumentRequestsByRequestId(jsonEnvelope);
        final JsonArray nowDocumentTypes = jsonEnvelopeOut.payloadAsJsonObject().getJsonArray(NOW_DOCUMENT_REQUESTS_PARAM);

        assertThat(nowDocumentTypes.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(nowDocumentTypes.getJsonObject(0).getString(MATERIAL_ID_PARAM), is(MATERIAL_ID.toString()));
        assertThat(nowDocumentTypes.getJsonObject(0).getString(REQUEST_ID_PARAM), is(requestId.toString()));
    }

    @Test
    public void shouldReturnEmptyNowDocumentRequests() {
        final UUID requestId = UUID.randomUUID();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add(REQUEST_ID_PARAM, requestId.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(NOW_DOCUMENT_REQUESTS_BY_REQUEST_ID_QUERY).build(),
                jsonObject);
        when(nowDocumentRequestRepository.findByRequestId(requestId)).thenReturn(emptyList());

        final JsonEnvelope jsonEnvelopeOut = nowDocumentRequestQueryView.getNowDocumentRequestsByRequestId(jsonEnvelope);
        final JsonArray nowDocumentTypes = jsonEnvelopeOut.payloadAsJsonObject().getJsonArray(NOW_DOCUMENT_REQUESTS_PARAM);

        assertThat(nowDocumentTypes.getValuesAs(JsonObject.class).size(), is(0));
    }

    @Test
    public void shouldNowDocumentRequestByHearing() {
        final UUID requestId = UUID.randomUUID();
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add(HEARING_ID_PARAM, HEARING_ID.toString()).build();
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID())
                        .withName(NOW_DOCUMENT_REQUEST_BY_HEARING_QUERY).build(),
                jsonObject);
        final NowDocumentRequestEntity nowDocumentRequestEntity = nowDocumentRequestEntity(requestId);

        when(nowDocumentRequestRepository.findByHearingId(HEARING_ID)).thenReturn(Arrays.asList(nowDocumentRequestEntity));

        final JsonEnvelope jsonEnvelopeOut = nowDocumentRequestQueryView.getNowDocumentRequestByHearing(jsonEnvelope);
        final JsonArray nowDocumentTypes = jsonEnvelopeOut.payloadAsJsonObject().getJsonArray(NOW_DOCUMENT_REQUESTS_PARAM);

        assertThat(nowDocumentTypes.getValuesAs(JsonObject.class).size(), is(1));
        assertThat(nowDocumentTypes.getJsonObject(0).getString(MATERIAL_ID_PARAM), is(MATERIAL_ID.toString()));
        assertThat(nowDocumentTypes.getJsonObject(0).getString(HEARING_ID_PARAM), is(HEARING_ID.toString()));
    }

    private NowDocumentRequestEntity nowDocumentRequestEntity(final UUID requestId) {
        final NowDocumentRequestEntity nowDocumentRequestEntity = new NowDocumentRequestEntity();
        nowDocumentRequestEntity.setRequestId(requestId);
        nowDocumentRequestEntity.setMaterialId(MATERIAL_ID);
        nowDocumentRequestEntity.setHearingId(HEARING_ID);
        nowDocumentRequestEntity.setPayload("payload");
        return nowDocumentRequestEntity;
    }
}
