package uk.gov.moj.cpp.nows.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.PrisonCourtRegisterGenerated;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSavePrisonCourtRegister() {

        final UUID courtCenterId = UUID.randomUUID();
        final PrisonCourtRegisterDocumentRequest prisonCourtRegisterDocumentRequest = PrisonCourtRegisterDocumentRequest.prisonCourtRegisterDocumentRequest()
                .withCourtCentreId(courtCenterId)
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().withCourtHouse("Court House").withLjaName("LJA Name").build())
                .build();

        final PrisonCourtRegisterRecorded prisonCourtRegisterRecorded = new PrisonCourtRegisterRecorded(courtCenterId, "Applicant",prisonCourtRegisterDocumentRequest );

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
        assertThat(prisonCourtRegisterEntityArgumentCaptor.getValue().getPayload(), is(requestMessage.payloadAsJsonObject().getJsonObject("prisonCourtRegister").toString()));

    }

    @Test
    public void testPrisonCourtRegisterGenerated() {
        final UUID courtCenterId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();

        final PrisonCourtRegisterGenerated prisonCourtRegisterGenerated = PrisonCourtRegisterGenerated.prisonCourtRegisterGenerated()
                .withCourtCentreId(courtCenterId)
                .withDefendant(PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build())
                .withFileId(fileId)
                .withHearingDate(ZonedDateTime.now(UTC))
                .withHearingVenue(PrisonCourtRegisterHearingVenue.prisonCourtRegisterHearingVenue().build())
                .withHearingId(hearingId)
                .withRecipients(Collections.emptyList())
                .build();

        final PrisonCourtRegisterEntity prisonCourtRegisterEntity = new PrisonCourtRegisterEntity();
        prisonCourtRegisterEntity.setCourtCentreId(courtCenterId);
        when(prisonCourtRegisterRepository.findByCourtCentreId(courtCenterId)).thenReturn(Collections.singletonList(prisonCourtRegisterEntity));

        prisonCourtRegisterEventListener.generatePrisonCourtRegister(envelopeFrom(metadataWithRandomUUID("progression.event.prison-court-register-generated"),
                objectToJsonObjectConverter.convert(prisonCourtRegisterGenerated)));

        assertThat(prisonCourtRegisterEntity.getFileId(), is(fileId));
    }
}
