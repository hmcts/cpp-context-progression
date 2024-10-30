package uk.gov.moj.cpp.progression.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.DefendantBailDocument;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 
 * @deprecated
 *
 */
@Deprecated
@SuppressWarnings({"WeakerAccess", "squid:S1133"})
@ExtendWith(MockitoExtension.class)
public class DefendantUpdatedListenerTest {

    private final UUID caseId = UUID.randomUUID();
    private final UUID defendantId = UUID.randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private DefendantUpdated defendantUpdated;

    @Mock
    private CaseProgressionDetail caseDetail;

    @Mock
    private Defendant defendant;

    @Mock
    private uk.gov.moj.cpp.progression.persistence.entity.Person person;

    @Mock
    private JsonObject payload;

    @Mock
    private SearchProsecutionCase searchCase;

    @InjectMocks
    private DefendantUpdatedListener listener;

    @BeforeEach
    public void setup() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
    }

    @Test
    public void shouldUpdateDefendantBailStatus() {
        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendantUpdated.getBailStatus())
                .thenReturn("UNCONDITIONAL");

        listener.defendantUpdated(envelope);

        verify(defendant).setBailStatus(defendantUpdated.getBailStatus());
    }

    @Test
    public void shouldUpdateDefendantCustodyTimeLimitDate() {
        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendantUpdated.getCustodyTimeLimitDate())
                .thenReturn(LocalDate.now());

        listener.defendantUpdated(envelope);

        verify(defendant).setCustodyTimeLimitDate(defendantUpdated.getCustodyTimeLimitDate());
    }

    @Test
    public void shouldUpdateDefendantBailDocument() {
        final UUID documentId = UUID.randomUUID();
        final Set<DefendantBailDocument> listDefendantBailDocument = new HashSet<>();
        final DefendantBailDocument defendantBailDocument = new DefendantBailDocument();
        defendantBailDocument.setDocumentId(documentId);
        defendantBailDocument.setActive(Boolean.TRUE);
        listDefendantBailDocument.add(defendantBailDocument);

        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendantUpdated.getBailDocument())
                .thenReturn(new BailDocument( UUID.randomUUID(),  documentId));

        listener.defendantUpdated(envelope);

        verify(defendant).addDefendantBailDocument(Mockito.any());
    }

    @Test
    public void shouldUpdateDefendantInterpreter() {
        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendantUpdated.getInterpreter())
                .thenReturn(new Interpreter( true, "language"));

        listener.defendantUpdated(envelope);

        final ArgumentCaptor<InterpreterDetail> captor =
                ArgumentCaptor.forClass(InterpreterDetail.class);

        verify(defendant).setInterpreter(captor.capture());

        assertThat(captor.getValue().getLanguage(), is("language"));
        assertThat(captor.getValue().getNeeded(), is(Boolean.TRUE));
    }


    @Test
    public void shouldUpdateDefendantPerson() {

        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(defendantUpdated.getPerson()).thenReturn(new Person());
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendant.getPerson()).thenReturn(person);
        listener.defendantUpdated(envelope);

        verify(defendant.getPerson()).setTitle(defendantUpdated.getPerson().getTitle());
    }
    
    @Test
    public void shouldUpdateDefendantDefenceSolicitorFirm() {
        when(jsonObjectToObjectConverter.convert(payload, DefendantUpdated.class))
                .thenReturn(defendantUpdated);
        when(defendantUpdated.getCaseId()).thenReturn(caseId);
        when(defendantUpdated.getDefendantId()).thenReturn(defendantId);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
        when(defendantUpdated.getDefenceSolicitorFirm())
                .thenReturn("defenceSolicitorFirm");

        listener.defendantUpdated(envelope);

        verify(defendant).setDefenceSolicitorFirm(defendantUpdated.getDefenceSolicitorFirm());
    }


    
}