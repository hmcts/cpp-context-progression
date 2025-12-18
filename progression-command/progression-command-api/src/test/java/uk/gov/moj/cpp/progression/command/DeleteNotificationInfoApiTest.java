package uk.gov.moj.cpp.progression.command;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.DeleteNotificationInfoService;

import java.time.ZonedDateTime;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteNotificationInfoApiTest {

    @Mock
    private DeleteNotificationInfoService deleteNotificationInfoService;

    @InjectMocks
    private DeleteNotificationInfoApi deleteNotificationInfoApi;

    @Test
    public void shouldHandleDeleteNotificationInfo() {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("progression.delete-notification-info"),
                JsonObjects.createObjectBuilder()
                        .build()
        );

        deleteNotificationInfoApi.handle(jsonEnvelope);

        verify(deleteNotificationInfoService, times(1)).deleteNotifications(any(String.class), any(ZonedDateTime.class));
    }

}
