package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyServiceTest {

    @Mock
    private Sender sender;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @InjectMocks
    private NotificationNotifyService notificationNotifyService;

    @Test
    public void shouldSendLetterNotification() {
        final String letterUrl = "http://the.letter.url";
        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-requested")
                        .withClientCorrelationId(clientId),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("caseId", caseId.toString())
                        .build());

        when(materialUrlGenerator.pdfFileStreamUrlFor(materialId)).thenReturn(letterUrl);

        notificationNotifyService.sendLetterNotification(event, notificationId, materialId, true);

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());

        final Envelope<?> printCommandEnvelope = captor.getValue();

        final Metadata metadata = printCommandEnvelope.metadata();

        assertThat(metadata.name(), is("notificationnotify.send-letter-notification"));

        final Optional<String> clientCorrelationId = metadata.clientCorrelationId();

        if (clientCorrelationId.isPresent()) {
            assertThat(clientCorrelationId.get(), is(clientId));
        } else {
            fail();
        }

        final JsonObject payload = (JsonObject) printCommandEnvelope.payload();

        with(payload.toString())
                .assertThat("notificationId", is(notificationId.toString()))
                .assertThat("letterUrl", is(letterUrl))
                .assertThat("postage", is("first"));
    }
}