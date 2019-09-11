package uk.gov.moj.cpp.progression.command.handler.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class UsersGroupQueryServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private Requester requester;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Metadata metadata;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @InjectMocks
    private UsersGroupQueryService usersGroupQueryService;

    @Test
    public void shouldReturnOrganisationDetails() {

        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));

        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);

        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();

        final JsonEnvelope response = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(organisationId.toString(), "organisationId").build();

        when(requester.request(any())).thenReturn(response);

        final JsonObject result = usersGroupQueryService.getOrganisationDetailsForUser(query);

        verify(requester).request(envelopeArgumentCaptor.capture());

        assertThat(result.getString("organisationId"), is(organisationId.toString()));

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionForMissingOrganisation() {

        final UUID userId = randomUUID();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));

        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);

        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();

        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                metadataBuilder, JsonValue.NULL);

        when(requester.request(any())).thenReturn(response);

        usersGroupQueryService.getOrganisationDetailsForUser(query);
    }

    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-organisation-details-for-user")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }
}