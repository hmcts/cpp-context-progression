package uk.gov.moj.cpp.prosecution.event.listener;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentCreated;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.event.listener.CourtDocumentEventListener;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ProsecutionCaseEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private CourtsDocumentCreated courtsDocumentCreated;

    @Mock
    private CourtDocument courtDocument;

    @Mock
    private DocumentCategory documentCategory;

    @Captor
    private ArgumentCaptor<CourtDocumentEntity> argumentCaptor;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private CourtDocumentEventListener eventListener;

    @Test
    public void shouldHandleCourtDocumentCreatedEvent() throws Exception {

        final CaseDocument caseDocument =  CaseDocument.caseDocument()
                .withProsecutionCaseId(UUID.randomUUID())
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentCreated.class))
                .thenReturn(courtsDocumentCreated);
        when(envelope.metadata()).thenReturn(metadata);
        when(courtDocument.getCourtDocumentId()).thenReturn(UUID.randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(documentCategory.getCaseDocument()).thenReturn(caseDocument);
        when(documentCategory.getDefendantDocument()).thenReturn(null);
        when(courtsDocumentCreated.getCourtDocument()).thenReturn(courtDocument);
        when(objectToJsonObjectConverter.convert(courtDocument)).thenReturn(jsonObject);
        eventListener.processCourtDocumentCreated(envelope);
        verify(repository).save(argumentCaptor.capture());
        final CourtDocumentEntity entity = argumentCaptor.getValue();
        MatcherAssert.assertThat(entity.getIndices().iterator().next().getProsecutionCaseId(),
                Matchers.is(caseDocument.getProsecutionCaseId()));
        //TODO expand this out
    }
}
