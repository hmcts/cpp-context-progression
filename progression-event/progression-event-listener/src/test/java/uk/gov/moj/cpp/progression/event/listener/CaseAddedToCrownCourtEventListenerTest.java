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
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.event.converter.CaseAddedToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaseAddedToCrownCourtEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseAddedToCrownCourtToCaseProgressionDetailConverter caseAddedToCrownCourtConverter;

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
        when(caseAddedToCrownCourtConverter.convert(caseAddedToCrownCourt)).thenReturn(caseProgressionDetail);
        when(envelope.metadata()).thenReturn(metadata);
        when(envelope.metadata().version()).thenReturn(Optional.of(0l));
        eventListener.addedToCrownCourt(envelope);

        verify(repository).save(caseProgressionDetail);

    }
}