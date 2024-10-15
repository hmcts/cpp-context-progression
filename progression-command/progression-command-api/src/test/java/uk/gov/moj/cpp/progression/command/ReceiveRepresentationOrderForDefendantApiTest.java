package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.service.OrganisationService;

import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReceiveRepresentationOrderForDefendantApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private ReceiveRepresentationOrderForDefendantApi receiveRepresentationOrderForDefendantApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    OrganisationService organisationService;

    @Mock
    Requester requester;

    @Test
    public void shouldReceiveRepresentationOrderForDefendantAPI() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder().add("defendantId", randomUUID().toString()).build());

        final JsonObject jsonObjectPayload = Json.createObjectBuilder().add("organisationId", randomUUID().toString()).build();
        when(organisationService.getAssociatedOrganisation(any(), any(), any())).thenReturn(jsonObjectPayload);
        receiveRepresentationOrderForDefendantApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.receive-representationOrder-for-defendant"));
        final JsonObject payload = envelopeArgumentCaptor.getValue().payload();
        assertThat(payload.getString("associatedOrganisationId"), is(jsonObjectPayload.getString("organisationId")));
    }


    @Test
    public void shouldReceiveRepresentationOrderForDefendantAPIWithNoAssociation() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder().add("defendantId", randomUUID().toString()).build());

        final JsonObject jsonObjectPayload = Json.createObjectBuilder().build();
        when(organisationService.getAssociatedOrganisation(any(), any(), any())).thenReturn(jsonObjectPayload);
        receiveRepresentationOrderForDefendantApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.receive-representationOrder-for-defendant"));
        final JsonObject payload = envelopeArgumentCaptor.getValue().payload();
        assertThat(payload.containsKey("associatedOrganisationId"), is(false));
    }

}
