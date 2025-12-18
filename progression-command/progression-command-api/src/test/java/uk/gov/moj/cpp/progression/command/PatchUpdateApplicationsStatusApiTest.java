package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatchUpdateApplicationsStatusApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeCaptor;

    @InjectMocks
    private PatchUpdateApplicationsStatusApi patchUpdateApplicationsStatusApi;

    @Test
    void shouldHandlePatchUpdateApplications() {
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("applications", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder().add("id", application1Id.toString()).add("applicationStatus", FINALISED.toString()).build())
                        .add(JsonObjects.createObjectBuilder().add("id", application2Id.toString()).build())
                )
                .build();
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.patch-update-application-status").withUserId(randomUUID().toString()).build());
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder, payload);

        patchUpdateApplicationsStatusApi.handle(envelope);

        verify(sender, times(2)).send(envelopeCaptor.capture());
        List<Envelope<?>> currentEvents = envelopeCaptor.getAllValues();
        assertThat(currentEvents, hasSize(2));
        final Envelope<?> firstCommand = currentEvents.get(0);
        final JsonObject firstPayload = (JsonObject) firstCommand.payload();
        assertThat(firstCommand.metadata().name(), is("progression.command.patch-update-application-status"));
        assertThat(firstPayload.getString("id"), is(application1Id.toString()));
        assertThat(firstPayload.getString("applicationStatus"), is(FINALISED.toString()));

        final Envelope<?> secondCommand = currentEvents.get(1);
        final JsonObject secondPayload = (JsonObject) secondCommand.payload();
        assertThat(secondCommand.metadata().name(), is("progression.command.patch-update-application-status"));
        assertThat(secondPayload.getString("id"), is(application2Id.toString()));
        assertThat(secondPayload.containsKey("applicationStatus"), is(false));

    }

}
