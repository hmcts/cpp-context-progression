package uk.gov.moj.cpp.progression.query;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtRegisterRequestRepository;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtRegisterDocumentRequestQueryViewTest {

    @Mock
    private CourtRegisterRequestRepository courtRegisterRequestRepository;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private CourtRegisterDocumentRequestQueryView courtRegisterDocumentRequestQueryView;

    @Test
    public void shouldGetCourtRegisterRequests() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.court-register-document-request").build(),
                JsonObjects.createObjectBuilder().add("requestStatus", RegisterStatus.RECORDED.name()).build());
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        final UUID courtCentreId = UUID.randomUUID();
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);
        final JsonObject transformedJsonEntity = JsonObjects.createObjectBuilder().add("courtCentreId", courtCentreId.toString()).build();
        when(objectToJsonObjectConverter.convert(courtRegisterRequestEntity)).thenReturn(transformedJsonEntity);
        when(courtRegisterRequestRepository.findByStatusRecorded()).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        final JsonEnvelope courtRegisterRequests = courtRegisterDocumentRequestQueryView.getCourtRegisterRequests(envelope);
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests").size(), is(1));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("courtCentreId"), is(courtCentreId.toString()));
    }

    @Test
    public void shouldGetCourtRegisterByMaterial() {
        final UUID materialId = UUID.randomUUID();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.court-register-document-by-material").build(),
                JsonObjects.createObjectBuilder().add("materialId", materialId.toString()).build());
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        final UUID courtCentreId = UUID.randomUUID();
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);
        final JsonObject transformedJsonEntity = JsonObjects.createObjectBuilder().add("courtCentreId", courtCentreId.toString()).build();
        when(objectToJsonObjectConverter.convert(courtRegisterRequestEntity)).thenReturn(transformedJsonEntity);
        when(courtRegisterRequestRepository.findBySystemDocGeneratorId(materialId)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        final JsonEnvelope courtRegisterRequests = courtRegisterDocumentRequestQueryView.getCourtRegisterByMaterial(envelope);
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests").size(), is(1));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("courtCentreId"), is(courtCentreId.toString()));
    }

    @Test
    public void shouldGetCourtRegistersByDate() {
        final LocalDate requestDate = LocalDate.now();
        final String courtHouse = "liver pool";
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("registerDate", requestDate.toString())
                .add("courtHouse", courtHouse)
                .build();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.court-register-document-by-request-date").build(),
                payload);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        final UUID courtCentreId = UUID.randomUUID();
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        courtRegisterRequestEntity.setCourtHouse(courtHouse);
        courtRegisterRequestEntity.setRegisterDate(requestDate);
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);

        final JsonObject transformedJsonEntity = JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("registerDate", requestDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(courtRegisterRequestEntity)).thenReturn(transformedJsonEntity);
        when(courtRegisterRequestRepository.findByRequestDateAndCourtHouse(LocalDate.now(), courtHouse)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        final JsonEnvelope courtRegisterRequests = courtRegisterDocumentRequestQueryView.getCourtRegistersByRequestDate(envelope);
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests").size(), is(1));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(requestDate.toString()));
    }

    @Test
    public void shouldGetCourtRegistersByDateCourtHouseEmpty() {
        final LocalDate requestDate = LocalDate.now();
        final String courtHouse = "liver pool";
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("registerDate", requestDate.toString())
                .build();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.court-register-document-by-request-date").build(),
                payload);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        final UUID courtCentreId = UUID.randomUUID();
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        courtRegisterRequestEntity.setCourtHouse(courtHouse);
        courtRegisterRequestEntity.setRegisterDate(requestDate);
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);

        final JsonObject transformedJsonEntity = JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("registerDate", requestDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(courtRegisterRequestEntity)).thenReturn(transformedJsonEntity);
        when(courtRegisterRequestRepository.findByRequestDate(requestDate)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        final JsonEnvelope courtRegisterRequests = courtRegisterDocumentRequestQueryView.getCourtRegistersByRequestDate(envelope);
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests").size(), is(1));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(courtRegisterRequests.payloadAsJsonObject().getJsonArray("courtRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(requestDate.toString()));
    }
}
