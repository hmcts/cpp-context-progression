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
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AllStatementsServedEventListenerTest {

    @Mock
    private CaseService service;
    
    @Mock
    private AllStatementsServed allStatementsServed;
    
    @Mock
    private JsonObject payload;
    
    @Mock
    private Metadata metadata;
    
    @Mock
    private JsonEnvelope envelope;
    
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private AllStatementsServedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {
    	
    	when(envelope.payloadAsJsonObject()).thenReturn(payload);
    	when(envelope.metadata()).thenReturn(metadata);
    	when(metadata.version()).thenReturn(Optional.of(0l));
    	when(jsonObjectConverter.convert(payload, AllStatementsServed.class)).thenReturn(allStatementsServed);

        listener.processEvent(envelope);
        
        verify(service).indicateAllStatementsServed(allStatementsServed, 0l);
    }

}
