package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.event.converter.DefendantAddedToDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;


@SuppressWarnings("WeakerAccess")
@RunWith(MockitoJUnitRunner.class)
public class DefendantAddedListenerTest {

    private static final UUID VICTIM_ID = randomUUID();
    private UUID caseId = randomUUID();

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
    private Defendant defendantDetail;

    @InjectMocks
    private DefendantAddedListener listener;

    @Test
    public void shouldAddDefendant() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAdded.class)).thenReturn(defendantAddedEvent);
        when(defendantAddedEvent.getCaseId()).thenReturn(caseId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(defendantAddedConverter.convert(defendantAddedEvent)).thenReturn(defendantDetail);

        listener.addDefendant(envelope);

        verify(caseDetail).addDefendant(defendantDetail);
    }
}
