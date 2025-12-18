package uk.gov.justice.api.resource.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    public void shouldReturnOrganisationDetails() {

        //given
        final UUID userId = randomUUID();
        final UUID organisationId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadataBuilder(userId);
        final JsonEnvelope query = JsonEnvelopeBuilder.envelope().with(metadataBuilder).withPayloadOf(organisationId.toString(), "organisationId").build();
        final JsonObjectBuilder associationBuilder = JsonObjects.createObjectBuilder().add("organisationId", organisationId.toString());

        final JsonEnvelope response = JsonEnvelope.envelopeFrom(metadataBuilder, associationBuilder);

        when(requester.requestAsAdmin(any())).thenReturn(response);

        //when
        final JsonObject result = usersAndGroupsService.getOrganisationDetails(query);

        //then
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(result.getString("organisationId"), is(organisationId.toString()));
    }

    private MetadataBuilder getMetadataBuilder(final UUID userId) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-organisation-details")
                .withCausation(randomUUID())
                .withClientCorrelationId(randomUUID().toString())
                .withStreamId(randomUUID())
                .withUserId(userId.toString());
    }

}
