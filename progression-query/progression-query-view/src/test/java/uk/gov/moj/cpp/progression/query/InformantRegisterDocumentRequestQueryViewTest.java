package uk.gov.moj.cpp.progression.query;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InformantRegisterRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InformantRegisterDocumentRequestQueryViewTest {

    @Mock
    private InformantRegisterRepository informantRegisterRepository;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private InformantRegisterDocumentRequestQueryView informantRegisterDocumentRequestQueryView;

    @Test
    public void shouldGetInformantRegisterRequestsByStatus() {
        final UUID prosecutionAuthorityId = UUID.randomUUID();
        final LocalDate registerDate = LocalDate.now();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.informant-register-document-request").build(),
                Json.createObjectBuilder().add("requestStatus", RegisterStatus.RECORDED.name()).build());

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setStatus(RegisterStatus.RECORDED);

        final JsonObject transformedJsonEntity = Json.createObjectBuilder().add("prosecutionAuthorityId", prosecutionAuthorityId.toString()).build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByStatusRecorded()).thenReturn(Collections.singletonList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegisterRequests(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
    }


    @Test
    public void shouldGetInformantRegisterByMaterial() {
        final UUID fileId = UUID.randomUUID();
        final UUID prosecutionAuthorityId = UUID.randomUUID();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.informant-register-document-by-material").build(),
                Json.createObjectBuilder().add("fileId", fileId.toString()).build());

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setFileId(fileId);
        informantRegisterEntity.setRegisterDate(LocalDate.now());

        final JsonObject transformedJsonEntity = Json.createObjectBuilder().add("prosecutionAuthorityId", prosecutionAuthorityId.toString()).build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByFileId(fileId)).thenReturn(Lists.newArrayList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByMaterial(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
    }

    @Test
    public void shouldGetInformantRegisterByDate() {
        final LocalDate registerDate = LocalDate.now();
        final String prosecutionAuthorityCode = "TFL";

        final JsonObject payload = Json.createObjectBuilder().add("registerDate", registerDate.toString())
                .add("prosecutionAuthorityCode", prosecutionAuthorityCode)
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                        .withName("progression.query.informant-register-document-by-request-date").build(),payload);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final UUID prosecutionAuthorityId = UUID.randomUUID();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setProsecutionAuthorityCode(prosecutionAuthorityCode);

        final JsonObject transformedJsonEntity = Json.createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("registerDate", registerDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(registerDate, prosecutionAuthorityCode)).thenReturn(Lists.newArrayList(informantRegisterEntity));

        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByRequestDate(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(registerDate.toString()));
    }

    @Test
    public void shouldGetInformantRegisterByDateWhenProsecutionAuthorityIsEmpty() {
        final LocalDate registerDate = LocalDate.now();
        final String prosecutionAuthorityCode = "TFL";

        final JsonObject payload = Json.createObjectBuilder().add("registerDate", registerDate.toString())
                .build();

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("progression.query.informant-register-document-by-request-date").build(),payload);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        final UUID prosecutionAuthorityId = UUID.randomUUID();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthorityId);
        informantRegisterEntity.setRegisterDate(registerDate);
        informantRegisterEntity.setProsecutionAuthorityCode(prosecutionAuthorityCode);

        final JsonObject transformedJsonEntity = Json.createObjectBuilder()
                .add("prosecutionAuthorityId", prosecutionAuthorityId.toString())
                .add("registerDate", registerDate.toString())
                .build();
        when(objectToJsonObjectConverter.convert(informantRegisterEntity)).thenReturn(transformedJsonEntity);
        when(informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(registerDate, prosecutionAuthorityCode)).thenReturn(Lists.newArrayList(informantRegisterEntity));
        when(informantRegisterRepository.findByRegisterDate(registerDate)).thenReturn(Lists.newArrayList(informantRegisterEntity));


        final JsonEnvelope informantRegisterRequests = informantRegisterDocumentRequestQueryView.getInformantRegistersByRequestDate(envelope);
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests").size(), is(1));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("prosecutionAuthorityId"), is(prosecutionAuthorityId.toString()));
        assertThat(informantRegisterRequests.payloadAsJsonObject().getJsonArray("informantRegisterDocumentRequests")
                .getJsonObject(0).getString("registerDate"), is(registerDate.toString()));
    }
}
