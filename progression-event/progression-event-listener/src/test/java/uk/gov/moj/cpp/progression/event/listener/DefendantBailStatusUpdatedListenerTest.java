package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefendantBailStatusUpdatedListenerTest {

    private UUID caseId = UUID.randomUUID();
    private UUID defendantId = UUID.randomUUID();
    private UUID documentId = UUID.randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private BailStatusUpdatedForDefendant bailStatusUpdatedForDefendant;

    @Mock
    private CaseProgressionDetail caseDetail;

    @Mock
    private Defendant defendant;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private DefendantBailStatusUpdatedListener listener;

    @Before
    public void setup() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldUpdateBailStatusUnconditional() {
        when(jsonObjectToObjectConverter.convert(payload, BailStatusUpdatedForDefendant.class))
                        .thenReturn(bailStatusUpdatedForDefendant);
        when(bailStatusUpdatedForDefendant.getCaseId()).thenReturn(caseId);
        when(bailStatusUpdatedForDefendant.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(bailStatusUpdatedForDefendant.getBailStatus())
                        .thenReturn(DefendantBailStatusUpdatedListener.UNCONDITIONAL);
        when(defendant.getDefendantBailDocuments()).thenReturn(null);

        listener.bailStatusUpdated(envelope);

        verify(defendant).setBailStatus(bailStatusUpdatedForDefendant.getBailStatus());
    }

    @Test
    public void shouldUpdateBailStatusUnconditionalOnActiveDocument() {
        Set<DefendantBailDocument> listDefendantBailDocument = new HashSet<>();
        DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(documentId);
        defendantBailDocument.setActive(Boolean.TRUE);
        listDefendantBailDocument.add(defendantBailDocument);
        when(jsonObjectToObjectConverter.convert(payload, BailStatusUpdatedForDefendant.class))
                        .thenReturn(bailStatusUpdatedForDefendant);
        when(bailStatusUpdatedForDefendant.getCaseId()).thenReturn(caseId);
        when(bailStatusUpdatedForDefendant.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(bailStatusUpdatedForDefendant.getBailStatus())
                            .thenReturn(DefendantBailStatusUpdatedListener.UNCONDITIONAL);
        LocalDate ctlDate = LocalDate.now();
        when(bailStatusUpdatedForDefendant.getCustodyTimeLimitDate())
                            .thenReturn(ctlDate);
        when(defendant.getDefendantBailDocuments()).thenReturn(listDefendantBailDocument);

        listener.bailStatusUpdated(envelope);

        verify(defendant).setBailStatus(bailStatusUpdatedForDefendant.getBailStatus());
        verify(defendant).setCustodyTimeLimitDate(ctlDate);
    }

    @Test
    public void shouldUpdateBailStatusAsConditional() {
        when(jsonObjectToObjectConverter.convert(payload, BailStatusUpdatedForDefendant.class))
                        .thenReturn(bailStatusUpdatedForDefendant);
        when(bailStatusUpdatedForDefendant.getCaseId()).thenReturn(caseId);
        when(bailStatusUpdatedForDefendant.getDefendantId()).thenReturn(defendantId);
        when(bailStatusUpdatedForDefendant.getBailDocument()).thenReturn(new BailDocument(UUID.randomUUID(), documentId));
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(bailStatusUpdatedForDefendant.getBailStatus()).thenReturn("Conditional");
        when(defendant.getDefendantBailDocuments()).thenReturn(null);

        listener.bailStatusUpdated(envelope);

        verify(defendant).setBailStatus(bailStatusUpdatedForDefendant.getBailStatus());
    }

    @Test
    public void shouldUpdateBailStatusAsConditionalOnAvtiveDocument() {

        Set<DefendantBailDocument> listDefendantBailDocument = new HashSet<>();
        DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(documentId);
        defendantBailDocument.setActive(Boolean.TRUE);
        listDefendantBailDocument.add(defendantBailDocument);

        when(jsonObjectToObjectConverter.convert(payload, BailStatusUpdatedForDefendant.class))
                        .thenReturn(bailStatusUpdatedForDefendant);
        when(bailStatusUpdatedForDefendant.getCaseId()).thenReturn(caseId);
        when(bailStatusUpdatedForDefendant.getDefendantId()).thenReturn(defendantId);
        when(bailStatusUpdatedForDefendant.getBailDocument()).thenReturn(new BailDocument(UUID.randomUUID(), documentId));
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(bailStatusUpdatedForDefendant.getBailStatus()).thenReturn("Conditional");
 
        listener.bailStatusUpdated(envelope);

        verify(defendant).setBailStatus(bailStatusUpdatedForDefendant.getBailStatus());
        verify(defendant).addDefendantBailDocument(Mockito.any());
    }


}
