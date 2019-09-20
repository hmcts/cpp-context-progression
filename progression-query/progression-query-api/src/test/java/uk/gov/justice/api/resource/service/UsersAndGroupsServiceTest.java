package uk.gov.justice.api.resource.service;

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
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsersAndGroupsServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private Requester requester;
    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;
    @InjectMocks
    private UsersAndGroupsService usersAndGroupsService;

    @Test
    public void shouldReturnOrganisationDetailsGivenValidUserId() {

        //given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(userId.toString(), "userId").build();
        final JsonEnvelope response = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(organisationId.toString(), "organisationId").build();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(userId));
        when(requester.request(any())).thenReturn(response);

        //when
        final JsonObject result = usersAndGroupsService.getOrganisationDetailsForUser(query);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());
        assertThat(result.getString("organisationId"), is(organisationId.toString()));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForMissingUserId() {

        final MetadataBuilder metadataBuilder = getMetadataBuilder(null);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(null, "userId").build();
        final JsonEnvelope response = JsonEnvelope.envelopeFrom(metadataBuilder, JsonValue.NULL);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(null);
        when(requester.request(any())).thenReturn(response);

        usersAndGroupsService .getOrganisationDetailsForUser(query);

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
