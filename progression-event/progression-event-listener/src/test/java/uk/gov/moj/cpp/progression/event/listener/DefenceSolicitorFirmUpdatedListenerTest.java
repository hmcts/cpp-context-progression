package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceSolicitorFirmUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefenceSolicitorFirmUpdatedListenerTest {

    private UUID caseId = UUID.randomUUID();
    private UUID defendantId = UUID.randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;


    @Mock
    private DefenceSolicitorFirmUpdatedForDefendant defenceSolicitorFirmUpdated;

    @Mock
    private CaseProgressionDetail caseDetail;

    @Mock
    private Defendant defendant;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private DefenceSolicitorFirmUpdatedListener listener;

    @Before
    public void setup() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldUpdateDefenceSolicitorFirmForDefendant() {
        when(jsonObjectToObjectConverter.convert(payload,
                        DefenceSolicitorFirmUpdatedForDefendant.class))
                                        .thenReturn(defenceSolicitorFirmUpdated);
        when(defenceSolicitorFirmUpdated.getCaseId()).thenReturn(caseId);
        when(defenceSolicitorFirmUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);

        listener.defenceSolicitorFirmUpdated(envelope);

        verify(defendant).setDefenceSolicitorFirm(
                        defenceSolicitorFirmUpdated.getDefenceSolicitorFirm());
    }
}
