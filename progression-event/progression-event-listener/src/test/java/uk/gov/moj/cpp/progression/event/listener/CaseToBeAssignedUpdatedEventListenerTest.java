package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseToBeAssignedUpdatedEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseToBeAssignedUpdated caseToBeAssignedUpdated;

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
    private CaseToBeAssignedUpdatedEventListener listener;

    @Test
    public void testProcessEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata()).thenReturn(metadata);

        when(jsonObjectToObjectConverter.convert(payload, CaseToBeAssignedUpdated.class))
                        .thenReturn(caseToBeAssignedUpdated);

        listener.processEvent(envelope);

        verify(service).caseToBeAssigned(caseToBeAssignedUpdated);
    }

}
