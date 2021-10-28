package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import uk.gov.justice.core.courts.CaseMarkersSharedWithHearings;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

@RunWith(MockitoJUnitRunner.class)
public class CaseMarkersUpdatedProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderCaptor;

    @InjectMocks
    private CaseMarkersUpdatedProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);


    @Test
    public void processCaseMarkersUpdated() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(enveloper.withMetadataFrom(envelope, "public.progression.case-markers-updated")).thenReturn(enveloperFunction);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        processor.processCaseMarkerUpdated(envelope);

        verify(sender).send(finalEnvelope);
    }

    @Test
    public void shouldCallCommandForEachHearing(){

        final CaseMarkersSharedWithHearings caseMarkersSharedWithHearings = CaseMarkersSharedWithHearings.caseMarkersSharedWithHearings()
                .withCaseMarkers(singletonList(Marker.marker().build()))
                .withHearingIds(asList(randomUUID(), randomUUID()))
                .withProsecutionCaseId(randomUUID())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-markers-shared-with-hearings"),
                objectToJsonObjectConverter.convert(caseMarkersSharedWithHearings));

        processor.processCaseMarkerSharedWithHearings(requestMessage);

        verify(sender, times(2)).send(senderCaptor.capture());
    }

}
