package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

import javax.json.JsonObject;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NoMoreInformationRequiredEventListenerTest {

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private NoMoreInformationRequiredEvent noMoreInformationRequiredEvent;

    @Mock
    private Defendant defendant;


    @InjectMocks
    private NoMoreInformationRequiredEventListener listener;


    @Test
    public void shouldPassNoMoreInfoRequired() throws Exception {
        // given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        // and
        given(jsonObjectConverter.convert(payload, NoMoreInformationRequiredEvent.class))
                .willReturn(noMoreInformationRequiredEvent);

        given(defendantRepository.findByDefendantId(noMoreInformationRequiredEvent.getDefendantId())).willReturn(defendant);
        listener.noMoreInformationRequiredEventListener(envelope);
        verify(defendantRepository).save(defendant);
    }


}
