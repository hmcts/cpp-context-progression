package uk.gov.moj.cpp.progression.handler.courts.document;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.RefDataDefinitionException;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.referencedata.json.schemas.DocumentTypeAccess;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentTypeAccessProviderTest {

    @Mock
    private Requester requester;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private DocumentTypeAccessConverter documentTypeAccessConverter;

    @InjectMocks
    private DocumentTypeAccessProvider documentTypeAccessProvider;

    @Test
    public void shouldRequestTheDocumentTypeData() throws Exception {

        final UUID documentTypeId = randomUUID();

        final CourtDocument courtDocument = mock(CourtDocument.class);
        final JsonEnvelope defaultCourtDocumentEnvelope = mock(JsonEnvelope.class);

        final JsonObject documentTypeData = mock(JsonObject.class);
        final DocumentTypeAccess documentTypeAccess = mock(DocumentTypeAccess.class);

        when(courtDocument.getDocumentTypeId()).thenReturn(documentTypeId);
        when(referenceDataService.getDocumentTypeAccessData(
                documentTypeId,
                defaultCourtDocumentEnvelope,
                requester)).thenReturn(of(documentTypeData));
        when(documentTypeAccessConverter.toDocumentTypeAccess(documentTypeData)).thenReturn(documentTypeAccess);

        assertThat(documentTypeAccessProvider.getDocumentTypeAccess(courtDocument, defaultCourtDocumentEnvelope), is(documentTypeAccess));
    }

    @Test
    public void shouldFailIfTheDocumentTypeDataIsNotReturnedByTheRequester() throws Exception {

        final UUID documentTypeId = fromString("e620a88c-6ceb-44c7-873c-906b88b3af95");

        final CourtDocument courtDocument = mock(CourtDocument.class);
        final JsonEnvelope defaultCourtDocumentEnvelope = mock(JsonEnvelope.class);

        when(courtDocument.getDocumentTypeId()).thenReturn(documentTypeId);
        when(referenceDataService.getDocumentTypeAccessData(
                documentTypeId,
                defaultCourtDocumentEnvelope,
                requester)).thenReturn(empty());


        try {
            documentTypeAccessProvider.getDocumentTypeAccess(courtDocument, defaultCourtDocumentEnvelope);
            fail();
        } catch (final RefDataDefinitionException expected) {
            assertThat(expected.getMessage(), is("No DocumentTypeAccess with id 'e620a88c-6ceb-44c7-873c-906b88b3af95' found in referencedata context"));
        }
    }
}
