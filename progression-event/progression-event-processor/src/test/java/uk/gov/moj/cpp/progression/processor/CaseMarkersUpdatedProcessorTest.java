package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.core.courts.CaseMarkersSharedWithHearings;
import uk.gov.justice.core.courts.CaseMarkersUpdatedInHearing;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
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
    private ArgumentCaptor<DefaultEnvelope> senderCaptor;

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private CaseMarkersUpdatedProcessor processor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Mock
    private Logger logger;

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

    @Test
    public void shouldProcessCaseMarkerUpdateInHearing(){
        final CaseMarkersUpdatedInHearing caseMarkersUpdatedInHearing = CaseMarkersUpdatedInHearing.caseMarkersUpdatedInHearing()
                .withHearingId(randomUUID())
                .build();
        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.case-markers-updated-in-hearing"),
                objectToJsonObjectConverter.convert(caseMarkersUpdatedInHearing));
        processor.processCaseMarkerUpdateInHearing(requestMessage);
        verify(progressionService, times(1)).populateHearingToProbationCaseworker(Mockito.eq(requestMessage), any(UUID.class));
    }

}
