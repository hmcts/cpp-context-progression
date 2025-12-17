package uk.gov.moj.cpp.nows.event.listener;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtRegisterRecorded;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.courtRegisterDocument.CourtRegisterHearingVenue;
import uk.gov.justice.progression.courts.CourtRegisterGenerated;
import uk.gov.justice.progression.courts.CourtRegisterNotified;
import uk.gov.justice.progression.courts.CourtRegisterNotifiedV2;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtRegisterRequestRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtRegisterDocumentRequestListenerTest {

    @Mock
    private CourtRegisterRequestRepository courtRegisterRequestRepository;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    private CourtRegisterDocumentRequestListener courtRegisterDocumentRequestListener;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<CourtRegisterRequestEntity> courtRegisterRequestEntityArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSaveCourtRegisterRequest() {
        final UUID courtCenterId = UUID.randomUUID();
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = CourtRegisterDocumentRequest.courtRegisterDocumentRequest()
                .withCourtCentreId(courtCenterId)
                .withHearingVenue(CourtRegisterHearingVenue.courtRegisterHearingVenue().withCourtHouse("Liverpool Street").build())
                .withRegisterDate(ZonedDateTime.now()).build();
        final CourtRegisterRecorded courtRegisterRecorded = new CourtRegisterRecorded(courtCenterId, courtRegisterDocumentRequest);

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtRegisterRecorded);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-register-recorded"),
                jsonObject);

        courtRegisterDocumentRequestListener.saveCourtRegisterRequest(requestMessage);
        Mockito.verify(courtRegisterRequestRepository).save(courtRegisterRequestEntityArgumentCaptor.capture());
        assertThat(courtRegisterRequestEntityArgumentCaptor.getValue().getCourtCentreId().toString(), is(courtCenterId.toString()));
        assertThat(courtRegisterRequestEntityArgumentCaptor.getValue().getStatus(), is(RegisterStatus.RECORDED));
    }

    @Test
    public void shouldGenerateCourtRegister() {
        final UUID courtCentreId = UUID.randomUUID();
        final CourtRegisterDocumentRequest courtRegisterDocumentRequest = new CourtRegisterDocumentRequest.Builder().withCourtCentreId(courtCentreId).build();
        final CourtRegisterGenerated courtRegisterGenerated = CourtRegisterGenerated.courtRegisterGenerated()
                .withCourtRegisterDocumentRequests(Lists.newArrayList(courtRegisterDocumentRequest))
                .build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtRegisterGenerated);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-register-generated"),
                jsonObject);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        courtRegisterRequestEntity.setStatus(RegisterStatus.RECORDED);
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        Mockito.when(courtRegisterRequestRepository.findByCourtCenterIdAndStatusRecorded(courtCentreId)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        courtRegisterDocumentRequestListener.generateCourtRegister(requestMessage);
        assertThat(courtRegisterRequestEntity.getStatus(), is(RegisterStatus.GENERATED));
        assertThat(courtRegisterRequestEntity.getProcessedOn().toString(), is(notNullValue()));
    }

    @Test
    public void shouldNotifyCourtRegister() {
        final UUID courtCentreId = UUID.randomUUID();

        final CourtRegisterNotified courtRegisterNotified = CourtRegisterNotified.courtRegisterNotified()
                .withCourtCentreId(courtCentreId)
                .build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtRegisterNotified);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-register-notified"),
                jsonObject);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        courtRegisterRequestEntity.setStatus(RegisterStatus.GENERATED);
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        Mockito.when(courtRegisterRequestRepository.findByCourtCenterIdAndStatusGenerated(courtCentreId)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));
        courtRegisterDocumentRequestListener.notifyCourtRegister(requestMessage);
        assertThat(courtRegisterRequestEntity.getStatus(), is(RegisterStatus.NOTIFIED));
        assertThat(courtRegisterRequestEntity.getProcessedOn(), is(notNullValue()));
    }

    @Test
    public void shouldNotifyCourtRegisterV2() {
        final UUID courtCentreId = UUID.randomUUID();
        final LocalDate registerDate = LocalDate.parse("2024-10-25");

        final CourtRegisterNotifiedV2 courtRegisterNotified = CourtRegisterNotifiedV2.courtRegisterNotifiedV2()
                .withCourtCentreId(courtCentreId)
                .withRegisterDate(registerDate)
                .build();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtRegisterNotified);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-register-notified-v2"),
                jsonObject);
        final CourtRegisterRequestEntity courtRegisterRequestEntity = new CourtRegisterRequestEntity();
        courtRegisterRequestEntity.setStatus(RegisterStatus.GENERATED);
        courtRegisterRequestEntity.setCourtCentreId(courtCentreId);
        Mockito.when(courtRegisterRequestRepository.findByCourtCenterIdForRegisterDateAndStatusGenerated(courtCentreId, registerDate)).thenReturn(Lists.newArrayList(courtRegisterRequestEntity));

        courtRegisterDocumentRequestListener.notifyCourtRegisterV2(requestMessage);

        assertThat(courtRegisterRequestEntity.getStatus(), is(RegisterStatus.NOTIFIED));
        assertThat(courtRegisterRequestEntity.getProcessedOn(), is(notNullValue()));
    }
}