package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.COMMAND_UPDATE_DEFENDANT_FOR_HEARING;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.PUBLIC_CASE_DEFENDANT_CHANGED;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseDefendantUpdatedProcessorTest {

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(objectToJsonObjectConverter.convert(Mockito.any(Defendant.class))).thenReturn(payload);
        final DefendantUpdate pc = DefendantUpdate.defendantUpdate().withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withOffences(Collections.emptyList())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withId(randomUUID())
                                .withName("HMP Belmarsh")
                                .withCustody("Prison")
                                .build())
                        .build())
                .build();
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(pc);

        when(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DEFENDANT_CHANGED)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(envelope);

        //Then
        verify(sender).send(finalEnvelope);

    }

    @Test
    public void shouldCallUpdateDefendantHearingWhenHearingIdExists(){
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(objectToJsonObjectConverter.convert(Mockito.any(Defendant.class))).thenReturn(payload);
        final UUID hearingId = randomUUID();
        final DefendantUpdate pc = DefendantUpdate.defendantUpdate().withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withOffences(Collections.emptyList())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withId(randomUUID())
                                .withName("HMP Belmarsh")
                                .withCustody("Prison")
                                .build())
                        .build())
                .build();
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(pc);
        when(prosecutionCaseDefendantUpdated.getHearingIds()).thenReturn(Arrays.asList(hearingId));

        when(enveloper.withMetadataFrom(envelope, PUBLIC_CASE_DEFENDANT_CHANGED)).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_DEFENDANT_FOR_HEARING)).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        //When
        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(envelope);

        //Then
        verify(sender, times(2)).send(finalEnvelope);
    }

}
