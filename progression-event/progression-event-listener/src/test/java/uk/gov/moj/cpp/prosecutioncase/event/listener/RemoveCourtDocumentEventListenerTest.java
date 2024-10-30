package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtsDocumentRemoved;
import uk.gov.justice.core.courts.CourtsDocumentRemovedBdf;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtDocumentEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtDocumentRepository;

import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class RemoveCourtDocumentEventListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtDocumentRepository repository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Material material;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObject courtDocumentJson;

    @Mock
    private CourtsDocumentRemoved courtsDocumentRemoved;

    @Mock
    private CourtsDocumentRemovedBdf courtsDocumentRemovedBdf;

    @Mock
    private CourtDocument courtDocument;

    @Mock
    private DocumentCategory documentCategory;

    @Mock
    private CourtDocumentEntity courtDocumentEntity;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<UUID> argumentCaptorRepoFind;

    @Captor
    private ArgumentCaptor<String> argumentCaptorCourtDoc;

    @Mock
    private JsonObject payload;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private RemoveCourtDocumentEventListener eventListener;

    @Test
    public void shouldHandleCourtDocumentRemovedEvent() throws Exception {
        final UUID materialId = randomUUID();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentRemoved.class))
                .thenReturn(courtsDocumentRemoved);
        when(stringToJsonObjectConverter.convert(Mockito.any())).thenReturn(courtDocumentJson);
        when(jsonObjectToObjectConverter.convert(courtDocumentJson, CourtDocument.class))
                .thenReturn(courtDocument);
        when(repository.findBy(Mockito.any())).thenReturn(courtDocumentEntity);
        when(courtDocument.getMaterials()).thenReturn(Collections.singletonList(material));
        when(courtDocument.getCourtDocumentId()).thenReturn(randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(jsonObject);
        eventListener.processCourtDocumentRemoved(envelope);
        verify(repository).findBy(argumentCaptorRepoFind.capture());
        verify(courtDocumentEntity).setPayload(argumentCaptorCourtDoc.capture());
    }

    @Test
    public void shouldHandleCourtDocumentBdfRemovedEvent() throws Exception {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CourtsDocumentRemovedBdf.class))
                .thenReturn(courtsDocumentRemovedBdf);
        when(stringToJsonObjectConverter.convert(Mockito.any())).thenReturn(courtDocumentJson);
        when(jsonObjectToObjectConverter.convert(courtDocumentJson, CourtDocument.class))
                .thenReturn(courtDocument);
        when(repository.findBy(Mockito.any())).thenReturn(courtDocumentEntity);
        when(courtDocument.getMaterials()).thenReturn(Collections.singletonList(material));
        when(courtDocument.getCourtDocumentId()).thenReturn(randomUUID());
        when(courtDocument.getDocumentCategory()).thenReturn(documentCategory);
        when(objectToJsonObjectConverter.convert(Mockito.any(CourtDocument.class))).thenReturn(jsonObject);
        eventListener.processCourtDocumentRemovedBdf(envelope);
        verify(repository).findBy(argumentCaptorRepoFind.capture());
        verify(courtDocumentEntity).setPayload(argumentCaptorCourtDoc.capture());
    }
}
