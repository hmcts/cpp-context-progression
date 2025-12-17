package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.events.CaseCpsProsecutorUpdated;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutorCaseCpsProsecutorUpdatedEventProcessorTest {

    @InjectMocks
    private ProsecutorCaseCpsProsecutorUpdatedEventProcessor prosecutorCaseCpsProsecutorUpdatedEventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProgressionService progressionService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private GetHearingsAtAGlance hearingsAtAGlance;

    @Mock
    private ProsecutionCase prosecutionCase;

    @Mock
    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private String prosecutionCaseId;
    private String prosecutionAuthorityId;
    private UUID hearingId;
    private String prosecutionCaseURN;
    private JsonEnvelope requestMessage;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        prosecutionCaseId = randomUUID().toString();
        prosecutionAuthorityId = randomUUID().toString();
        hearingId = randomUUID();
        prosecutionCaseURN = randomUUID().toString();

        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = CaseCpsProsecutorUpdated.caseCpsProsecutorUpdated()
                .withProsecutionAuthorityCode("test prosecutionAuthorityCode")
                .withProsecutionCaseId(UUID.fromString(prosecutionCaseId))
                .withProsecutionAuthorityReference("test prosecutionAuthorityReference")
                .withProsecutionAuthorityName("test prosecutionAuthorityName")
                .withProsecutionAuthorityId(UUID.fromString(prosecutionAuthorityId))
                .withCaseURN("test caseURN")
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(caseCpsProsecutorUpdated);
        requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-cps-prosecutor-updated"),
                payload);

    }

    @Test
    public void shouldProcessCpsProsecutorUpdated() {

        //Given
        JsonObject hearingsJsonObject =
                createObjectBuilder().add("hearings", createArrayBuilder().add(
                        createObjectBuilder().add("id", hearingId.toString()).build()).build()).build();

        when(progressionService.getCaseHearings(eq(prosecutionCaseId))).thenReturn(of(hearingsJsonObject));

        //When
        prosecutorCaseCpsProsecutorUpdatedEventProcessor.processCpsProsecutorUpdated(requestMessage);

        //Then
        verify(sender, times(1)).send(envelopeCaptor.capture());
        verify(progressionService, times(1)).getCaseHearings(eq(prosecutionCaseId));
        assertThat(envelopeCaptor.getValue().payload().size(), is(7));
        assertThat(envelopeCaptor.getValue().payload().getJsonArray("hearingIds").size(), is(1));
    }

    @Test
    public void shouldProcessCpsProsecutorUpdatedWithoutHearings() {
        //Given
        JsonObject hearingsJsonObject =
                createObjectBuilder().add("hearings", createArrayBuilder().build()).build();

        when(progressionService.getCaseHearings(eq(prosecutionCaseId))).thenReturn(of(hearingsJsonObject));

        //When
        prosecutorCaseCpsProsecutorUpdatedEventProcessor.processCpsProsecutorUpdated(requestMessage);

        //Then
        verify(sender, times(1)).send(envelopeCaptor.capture());
        verify(progressionService, times(1)).getCaseHearings(eq(prosecutionCaseId));
        assertThat(envelopeCaptor.getValue().payload().size(), is(7));
        assertThat(envelopeCaptor.getValue().payload().getJsonArray("hearingIds").size(), is(0));
    }

    @Test
    public void shouldNotProcessCpsProsecutorUpdatedWithErrorFlag() {
        //Given
        requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-cps-prosecutor-updated"),
                createObjectBuilder().add("isCpsOrgVerifyError", true));

        //When
        prosecutorCaseCpsProsecutorUpdatedEventProcessor.processCpsProsecutorUpdated(requestMessage);

        //Then
        verify(sender, times(0)).send(envelopeCaptor.capture());
        verify(progressionService, times(0)).getProsecutionCaseDetailById(any(), eq(prosecutionCaseId));
    }
}
