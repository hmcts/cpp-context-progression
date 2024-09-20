package uk.gov.moj.cpp.progression.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantAddedToDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.SearchProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.SearchProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.UUID;

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
@Deprecated
@SuppressWarnings({"WeakerAccess", "squid:S1133"})
@ExtendWith(MockitoExtension.class)
public class DefendantAddedListenerTest {

    private static final UUID VICTIM_ID = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID caseUrn = randomUUID();
    private final UUID defendantId = randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseProgressionDetailRepository caseRepository;


    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;


    @Mock
    private DefendantAddedToDefendant defendantAddedConverter;

    @Mock
    private DefendantAdded defendantAddedEvent;

    @Mock
    private CaseProgressionDetail caseDetail;

    @Mock
    private SearchProsecutionCaseRepository searchRepository;

    @Mock
    private SearchProsecutionCase jpaMapper;

    @Mock
    private Defendant defendantDetail;

    @InjectMocks
    private DefendantAddedListener listener;


    @Test
    public void shouldAddDefendantAndMakeItSearchable() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAdded.class)).thenReturn(defendantAddedEvent);
        when(defendantAddedEvent.getCaseId()).thenReturn(caseId);
        when(defendantAddedEvent.getCaseUrn()).thenReturn(caseUrn.toString());
        when(defendantAddedEvent.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(defendantAddedConverter.convert(defendantAddedEvent)).thenReturn(defendantDetail);

        SearchProsecutionCaseEntity searchEntity = new SearchProsecutionCaseEntity();
        searchEntity.setSearchTarget("URN-101 | John Smith | 22-06-1977");

        listener.addDefendant(envelope);

        verify(caseDetail).addDefendant(defendantDetail);
    }
}
