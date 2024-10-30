package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * 
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ExtendWith(MockitoExtension.class)
public class CaseAddedToCrownCourtEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;


    @Mock
    private CaseProgressionDetailRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseAddedToCrownCourt caseAddedToCrownCourt;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private CaseAddedToCrownCourtEventListener eventListener;

    @Test
    public void shouldHandleHearingListedEvent() throws Exception {

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseAddedToCrownCourt.class))
                .thenReturn(caseAddedToCrownCourt);
        when(repository.findByCaseId(caseAddedToCrownCourt.getCaseId())).thenReturn(caseProgressionDetail);
         
        eventListener.addedToCrownCourt(envelope);


    }
}