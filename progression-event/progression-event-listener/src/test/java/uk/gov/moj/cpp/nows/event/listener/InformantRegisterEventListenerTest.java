package uk.gov.moj.cpp.nows.event.listener;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.InformantRegisterRecorded;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterDocumentRequest;
import uk.gov.justice.core.courts.informantRegisterDocument.InformantRegisterHearingVenue;
import uk.gov.justice.progression.courts.InformantRegisterGenerated;
import uk.gov.justice.progression.courts.InformantRegisterNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InformantRegisterRepository;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;


@RunWith(MockitoJUnitRunner.class)
public class InformantRegisterEventListenerTest {

    @Mock
    private InformantRegisterRepository informantRegisterRepository;

    @InjectMocks
    private InformantRegisterEventListener informantRegisterEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSaveInformantRegisterRequested() {
        final UUID prosecutionAuthId = UUID.randomUUID();
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withRegisterDate(ZonedDateTime.now())
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue().build())
                .build();

        final InformantRegisterRecorded informantRegisterRecorded = new InformantRegisterRecorded(
                informantRegisterDocumentRequest,
                informantRegisterDocumentRequest.getProsecutionAuthorityId());

        informantRegisterEventListener.saveInformantRegister(envelopeFrom(metadataWithRandomUUID("progression.event.informant-register-recorded"),
                objectToJsonObjectConverter.convert(informantRegisterRecorded)));

        final ArgumentCaptor<InformantRegisterEntity> informantRegisterRequestEntity = ArgumentCaptor.forClass(InformantRegisterEntity.class);
        verify(this.informantRegisterRepository).save(informantRegisterRequestEntity.capture());
        final InformantRegisterEntity savedInformantRegisterEntity = informantRegisterRequestEntity.getValue();
        final JsonObject jsonPayload = Json.createReader(new StringReader(savedInformantRegisterEntity.getPayload())).readObject();
        final InformantRegisterDocumentRequest informantRegisterRequestSaved = jsonObjectToObjectConverter.convert(jsonPayload, InformantRegisterDocumentRequest.class);

        assertThat(savedInformantRegisterEntity.getProsecutionAuthorityId(), is(prosecutionAuthId));
        assertThat(informantRegisterRequestSaved.getProsecutionAuthorityId(), is(prosecutionAuthId));
        assertThat(savedInformantRegisterEntity.getStatus(), is(RegisterStatus.RECORDED));
    }

    @Test
    public void shouldSaveInformantRegisterGenerated() {
        final UUID prosecutionAuthId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        final InformantRegisterDocumentRequest informantRegisterDocumentRequest = InformantRegisterDocumentRequest.informantRegisterDocumentRequest()
                .withProsecutionAuthorityId(prosecutionAuthId)
                .withRegisterDate(ZonedDateTime.now())
                .withHearingVenue(InformantRegisterHearingVenue.informantRegisterHearingVenue().build())
                .build();

        final InformantRegisterGenerated informantRegisterGenerated = new InformantRegisterGenerated(
                Collections.singletonList(informantRegisterDocumentRequest),
                fileId, false);

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(RegisterStatus.RECORDED);
        when(informantRegisterRepository.findByProsecutionAuthorityIdAndStatusRecorded(prosecutionAuthId)).thenReturn(Collections.singletonList(informantRegisterEntity));

        informantRegisterEventListener.generateInformantRegister(envelopeFrom(metadataWithRandomUUID("progression.event.informant-register-generated"),
                objectToJsonObjectConverter.convert(informantRegisterGenerated)));

        assertThat(informantRegisterEntity.getProcessedOn().toString(), is(notNullValue()));
        Assert.assertThat(informantRegisterEntity.getStatus(), is(RegisterStatus.GENERATED));
    }

    @Test
    public void shouldNotifyInformantRegister() {
        final UUID prosecutionAuthId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();

        final InformantRegisterNotified informantRegisterNotified = InformantRegisterNotified.informantRegisterNotified()
                .withFileId(fileId)
                .withProsecutionAuthorityId(prosecutionAuthId)
                .build();

        final InformantRegisterEntity informantRegisterEntity = new InformantRegisterEntity();
        informantRegisterEntity.setProsecutionAuthorityId(prosecutionAuthId);
        informantRegisterEntity.setStatus(RegisterStatus.GENERATED);
        when(informantRegisterRepository.findByProsecutionAuthorityIdAndStatusGenerated(prosecutionAuthId)).thenReturn(Lists.newArrayList(informantRegisterEntity));
        informantRegisterEventListener.notifyInformantRegister(envelopeFrom(metadataWithRandomUUID("progression.event.informant-register-notified"),
                objectToJsonObjectConverter.convert(informantRegisterNotified)));
        Assert.assertThat(informantRegisterEntity.getStatus(), is(RegisterStatus.NOTIFIED));
        Assert.assertThat(informantRegisterEntity.getProcessedOn(), is(notNullValue()));
    }
}
