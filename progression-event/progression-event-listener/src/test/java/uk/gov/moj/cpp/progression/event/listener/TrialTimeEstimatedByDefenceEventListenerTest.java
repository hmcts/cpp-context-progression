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
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;

@RunWith(MockitoJUnitRunner.class)
public class TrialTimeEstimatedByDefenceEventListenerTest {

	@Mock
	private DefenceTrialEstimateAdded defenceTrialEstimateAdded;
	
	@Mock
	private JsonEnvelope envelope;
    
    @Mock
    private JsonObject payload;
    
    @Mock
    private Metadata metadata;
    
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private CaseService service;

    @InjectMocks
    private TrialTimeEstimatedByDefenceEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

    	when(envelope.payloadAsJsonObject()).thenReturn(payload);
    	when(envelope.metadata()).thenReturn(metadata);
    	when(metadata.version()).thenReturn(Optional.of(0l));
    	when(jsonObjectConverter.convert(payload, DefenceTrialEstimateAdded.class)).thenReturn(defenceTrialEstimateAdded);

       listener.processEvent(envelope);

       verify(service).addTrialEstimateDefence(defenceTrialEstimateAdded, 0l);
    }

}
