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
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.event.converter.CaseSentToCrownCourtToCaseProgressionDetailConverter;
import uk.gov.moj.cpp.progression.event.listener.CaseSentToCrownCourtEventListener;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;

@RunWith(MockitoJUnitRunner.class)
public class CaseSentToCrownCourtEventListenerTest {

	@Mock
	private JsonObjectToObjectConverter jsonObjectToObjectConverter;
	
	@Mock
	private CaseSentToCrownCourtToCaseProgressionDetailConverter caseSentToCrownCourtConverter;

	@Mock
	private CaseProgressionDetailRepository repository;

	@Mock
	private JsonEnvelope envelope;

	@Mock
	private CaseSentToCrownCourt caseSentToCrownCourt;

	@Mock
	private CaseProgressionDetail caseProgressionDetail;

	@Mock
	private JsonObject payload;
	
	@Mock
	private Metadata metadata;

	@InjectMocks
	private CaseSentToCrownCourtEventListener eventListener;

	@Test
	public void shouldHandleHearingListedEvent() throws Exception {

		when(envelope.payloadAsJsonObject()).thenReturn(payload);
		when(jsonObjectToObjectConverter.convert(payload, CaseSentToCrownCourt.class)).thenReturn(caseSentToCrownCourt);
		when(caseSentToCrownCourtConverter.convert(caseSentToCrownCourt)).thenReturn(caseProgressionDetail);
		when(envelope.metadata()).thenReturn(metadata);
        when(envelope.metadata().version()).thenReturn(Optional.of(0l));
		eventListener.sentToCrownCourt(envelope);

		verify(repository).save(caseProgressionDetail);

	}
}