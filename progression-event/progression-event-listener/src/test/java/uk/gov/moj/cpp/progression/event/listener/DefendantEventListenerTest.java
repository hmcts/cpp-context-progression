package uk.gov.moj.cpp.progression.event.listener;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantEventToDefendantConverter;
import uk.gov.moj.cpp.progression.event.service.CaseService;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantEventListenerTest {

    @Mock
    private DefendantEventToDefendantConverter defendantEventToDefendantConverter;

    @Mock
    private DefendantRepository defendantRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantAdditionalInformationAdded defendantEvent;

    @Mock
    private Defendant defendant;

    @Mock
    private CaseProgressionDetail caseProgressionDetail;

    @InjectMocks
    private DefendantEventListener listener;

    @Mock
    private CaseService service;

    @Test
    public void shouldAddAdditionalInformationForDefendant() throws Exception {
        // given
        given(envelope.payloadAsJsonObject()).willReturn(payload);
        // and
        given(jsonObjectConverter.convert(payload, DefendantAdditionalInformationAdded.class))
                .willReturn(defendantEvent);
        given(defendantEventToDefendantConverter.populateAdditionalInformation(defendant, defendantEvent))
                .willReturn(defendant);
        given(defendantRepository.findBy(defendantEvent.getDefendantId())).willReturn(defendant);
        listener.addAdditionalInformationForDefendant(envelope);
        verify(service).addAdditionalInformationForDefendant(defendantEvent);
    }

}
