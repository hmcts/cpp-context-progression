package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

@RunWith(MockitoJUnitRunner.class)
public class AllStatementsIdentifiedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private AllStatementsIdentified allStatementsIdentified;

    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseService service;

    @InjectMocks
    private AllStatementsIdentifiedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.metadata().version()).thenReturn(Optional.of(0l));
        when(jsonObjectToObjectConverter.convert(payload, AllStatementsIdentified.class))
                .thenReturn(allStatementsIdentified);

        listener.processEvent(envelope);

        verify(service).indicateAllStatementsIdentified(allStatementsIdentified, 0l);
    }

}
