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
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.event.converter.IndicateEvidenceServedToIndicateStatementConverter;
import uk.gov.moj.cpp.progression.persistence.entity.IndicateStatement;
import uk.gov.moj.progression.persistence.repository.IndicateStatementRepository;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class IndicateEvidenceServedEventListenerTest {

	@Mock
	private IndicateStatement indicateStatement;
	
	@Mock
	private IndicateEvidenceServed indicateEvidenceServed;
	
	@Mock
	private JsonEnvelope envelope;
    
    @Mock
    private JsonObject payload;
    
    @Mock
    private Metadata metadata;
    
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
	private IndicateEvidenceServedToIndicateStatementConverter entityConverter;

	@Mock
	private IndicateStatementRepository repository;

    @InjectMocks
    private IndicateEvidenceServedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {


    	when(envelope.payloadAsJsonObject()).thenReturn(payload);
    	when(envelope.metadata()).thenReturn(metadata);
    	when(metadata.version()).thenReturn(Optional.of(0l));
    	when(jsonObjectConverter.convert(payload, IndicateEvidenceServed.class)).thenReturn(indicateEvidenceServed);
    	when(entityConverter.convert(indicateEvidenceServed)).thenReturn(indicateStatement);

    	listener.processEvent(envelope);

        verify(repository).save(indicateStatement);
    }

}
