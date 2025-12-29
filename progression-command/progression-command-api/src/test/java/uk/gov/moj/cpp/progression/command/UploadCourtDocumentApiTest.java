package uk.gov.moj.cpp.progression.command;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;

import java.util.UUID;
import java.util.function.Function;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UploadCourtDocumentApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private UploadCourtDocumentApi uploadCourtDocumentApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Test
    public void shouldUpdateOffences() {

        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.upload-court-document", UUID.randomUUID().toString()));
        uploadCourtDocumentApi.handle(command);

        verify(sender, times(1)).send(any());
    }

    @Test
    public void shouldUploadMaterial() {

        when(command.payloadAsJsonObject()).thenReturn(JsonObjects.createObjectBuilder().build());
        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(true);
        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.upload-court-document", UUID.randomUUID().toString()));
        uploadCourtDocumentApi.handleUploadForDefence(command);

        verify(sender, times(1)).send(any());
    }

    @Test
    public void shouldNotUploadMaterial() {

        when(userDetailsLoader.isPermitted(any(), any())).thenReturn(false);
        assertThrows(ForbiddenRequestException.class, () -> uploadCourtDocumentApi.handleUploadForDefence(command));
    }
}
