package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.progression.persistence.repository.DefendantRepository;

@RunWith(MockitoJUnitRunner.class)
public class DefendantEventListenerTest {

    @Mock
    private DefendantEventToDefendantConverter defendantEventToDefendantConverter;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private CaseProgressionDetailRepository caseProgressionDetailRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantEvent defendantEvent;

    @Mock
    private Defendant defendant;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @InjectMocks
    private DefendantEventListener listener;

    @Test
    public void shouldAddDefendant() throws Exception {
        // given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        // and
        given(jsonObjectConverter.convert(payload, DefendantEvent.class)).willReturn(defendantEvent);
        given(defendantEventToDefendantConverter.convert(defendantEvent)).willReturn(defendant);
        given(caseProgressionDetailRepository.findBy(defendantEvent.getCaseProgressionId()))
                .willReturn(caseProgressionDetail);
        listener.addDefendant(envelope);

        verify(defendantRepository).save(defendant);
    }

}
