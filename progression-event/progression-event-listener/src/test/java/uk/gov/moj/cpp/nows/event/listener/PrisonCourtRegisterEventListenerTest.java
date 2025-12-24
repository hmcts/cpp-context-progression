package uk.gov.moj.cpp.nows.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.PrisonCourtRegisterRecorded;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDocumentRequest;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterHearingVenue;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class PrisonCourtRegisterEventListenerTest {


    @Mock
    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    private PrisonCourtRegisterEventListener prisonCourtRegisterEventListener;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<PrisonCourtRegisterEntity> prisonCourtRegisterEntityArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSavePrisonCourtRegister() {

        final UUID courtCenterId = UUID.randomUUID();
        final UUID prisonCourtRegisterStreamId = UUID.randomUUID();
        final UUID id = UUID.randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCenterId)
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().withCourtHouse("Court House").withLjaName("LJA Name").build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = new PrisonCourtRegisterRecorded(courtCenterId,"Applicant", id, prisonCourtRegisterDocumentRequest, prisonCourtRegisterStreamId);

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        prisonCourtRegisterEntity.setPayload(prisonCourtRegisterDocumentRequest.toString());


        prisonCourtRegisterEventListener.savePrisonCourtRegister(requestMessage);
        Mockito.verify(prisonCourtRegisterRepository).save(prisonCourtRegisterEntityArgumentCaptor.capture());
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getCourtCentreId().toString(), is(courtCenterId.toString()));
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getId().toString(), is(id.toString()));
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getPayload(), is(requestMessage.payloadAsJsonObject().getJsonObject("prisonCourtRegister").toString()));

    }

    // this is for old events, catch-up and replay DLQs
    @Test
    public void shouldSavePrisonCourtRegisterWithoutId() {

        final UUID courtCenterId = UUID.randomUUID();
        final UUID prisonCourtRegisterStreamId = UUID.randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCenterId)
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().withCourtHouse("Court House").withLjaName("LJA Name").build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = new PrisonCourtRegisterRecorded(courtCenterId, "Applicant", null, prisonCourtRegisterDocumentRequest,prisonCourtRegisterStreamId );

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(prisonCourtRegisterRecorded);
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.prison-court-register-recorded"),
                jsonObject);

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        prisonCourtRegisterEntity.setPayload(prisonCourtRegisterDocumentRequest.toString());


        prisonCourtRegisterEventListener.savePrisonCourtRegister(requestMessage);
        Mockito.verify(prisonCourtRegisterRepository).save(prisonCourtRegisterEntityArgumentCaptor.capture());
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getCourtCentreId().toString(), is(courtCenterId.toString()));
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getId(), is(notNullValue()));
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getPayload(), is(requestMessage.payloadAsJsonObject().getJsonObject("prisonCourtRegister").toString()));

    }

    @Test
    public void testPrisonCourtRegisterGenerated() {
        final UUID courtCenterId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID id = UUID.randomUUID();

        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withCourtCentreId(courtCenterId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build())
                .withFileId(fileId)
                .withHearingDate(ZonedDateTime.now(UTC))
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                .withHearingId(hearingId)
                .withId(id)
                .withRecipients(Collections.emptyList())
                .build();

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        prisonCourtRegisterEntity.setId(id);
        when(prisonCourtRegisterRepository.findById(id)).thenReturn(prisonCourtRegisterEntity);

        prisonCourtRegisterEventListener.generatePrisonCourtRegister(envelopeFrom(metadataWithRandomUUID("progression.event.prison-court-register-generated"),
                objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated)));

        assertThat(prisonCourtRegisterEntity.getFileId(), is(fileId));
    }

    @Test
    public void testPrisonCourtRegisterGeneratedV2() {
        final UUID courtCenterId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID id = UUID.randomUUID();

        final PrisonCourtRegisterGeneratedV2 prisonCourtRegisterGenerated = PrisonCourtRegisterGeneratedV2.prisonCourtRegisterGeneratedV2()
                .withCourtCentreId(courtCenterId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build())
                .withFileId(fileId)
                .withHearingDate(ZonedDateTime.now(UTC))
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                .withHearingId(hearingId)
                .withId(id)
                .withRecipients(Collections.emptyList())
                .build();

        prisonCourtRegisterEventListener.sendPrisonCourtRegisterNotificationToAmp(envelopeFrom(metadataWithRandomUUID("progression.event.prison-court-register-generated-V2"),
                objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated)));

        // TODO assert post to AMP
        // assertThat(prisonCourtRegisterEntity.getFileId(), is(fileId));
    }

    // this is for old events, catch-up and replay DLQs
    @Test
    public void testPrisonCourtRegisterGeneratedWithoutId() {
        final UUID courtCenterId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withCourtCentreId(courtCenterId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().withMasterDefendantId(defendantId).build())
                .withFileId(fileId)
                .withHearingDate(ZonedDateTime.now(UTC))
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                .withHearingId(hearingId)
                .withRecipients(Collections.emptyList())
                .build();

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        when(prisonCourtRegisterRepository.findByCourtCentreIdAndHearingIdAndDefendantId(courtCenterId, hearingId.toString(), defendantId.toString())).thenReturn(prisonCourtRegisterEntity);

        prisonCourtRegisterEventListener.generatePrisonCourtRegister(envelopeFrom(metadataWithRandomUUID("progression.event.prison-court-register-generated"),
                objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated)));

        assertThat(prisonCourtRegisterEntity.getFileId(), is(fileId));
    }


    // this is for old events, catch-up and replay DLQs, some DLQs may have multiple records, I don't know why.
    @Test
    public void testPrisonCourtRegisterGeneratedWithoutIdForMultipleRecords() {
        final UUID courtCenterId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withCourtCentreId(courtCenterId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().withMasterDefendantId(defendantId).build())
                .withFileId(fileId)
                .withHearingDate(ZonedDateTime.now(UTC))
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                .withHearingId(hearingId)
                .withRecipients(Collections.emptyList())
                .build();

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        prisonCourtRegisterEntity.setPayload(courtCenterId + " " + hearingId + " " + defendantId);
        final PrisonCourtRegisterEntity prisonCourtRegisterEntity2 = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity2.setCourtCentreId(courtCenterId);

        when(prisonCourtRegisterRepository.findByCourtCentreIdAndHearingIdAndDefendantId(courtCenterId, hearingId.toString(), defendantId.toString())).thenThrow( new RuntimeException());

        prisonCourtRegisterEventListener.generatePrisonCourtRegister(envelopeFrom(metadataWithRandomUUID("progression.event.prison-court-register-generated"),
                objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated)));

        assertThat(prisonCourtRegisterEntity.getFileId(), is(nullValue()));
    }
}
