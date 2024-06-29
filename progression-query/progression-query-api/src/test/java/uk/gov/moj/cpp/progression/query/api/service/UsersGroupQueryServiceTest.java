package uk.gov.moj.cpp.progression.query.api.service;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsersGroupQueryServiceTest {

    private static final String DEFENCE_ORG = "Defence Org";

    @InjectMocks
    private UsersGroupQueryService usersGroupQueryService;

    @Mock
    private Requester requester;


    @Mock
    private JsonEnvelope jsonEnvelope;


    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;


    private static final UUID USER_ID = UUID.randomUUID();
    public static final String NON_CPS_PROSECUTORS = "Non CPS Prosecutors";


    @Test
    public void shouldGetUserGroups() {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("test");
        final Envelope envelope = Envelope.envelopeFrom(metadataBuilder.build(), jsonObject);
        when(requester.request(any(), any())).thenReturn(envelope);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        usersGroupQueryService.getUserGroups(metadataBuilder.build(), USER_ID);
        verify(requester).request(any(), any());
    }

    @Test
    public void shouldReturnTrueForValidateNonCPSUserOrg() {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("test");
        final Envelope envelope = Envelope.envelopeFrom(metadataBuilder.build(), getUserGroupsResponse());
        when(requester.request(any(), any())).thenReturn(envelope);
        final Optional<String> isNonCPSUserOrg = usersGroupQueryService.validateNonCPSUserOrg(metadataBuilder.build(), randomUUID(), NON_CPS_PROSECUTORS, "DVLA");
        Assert.assertTrue(isNonCPSUserOrg.isPresent());
        assertThat(isNonCPSUserOrg.get(), is("OrganisationMatch"));
    }

    @Test
    public void shouldReturnTrueForValidateNonCPSUserNonOrg() {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName("test");
        final Envelope envelope = Envelope.envelopeFrom(metadataBuilder.build(), getUserGroupsResponse());
        when(requester.request(any(), any())).thenReturn(envelope);
        final Optional<String> isNonCPSUserOrg = usersGroupQueryService.validateNonCPSUserOrg(metadataBuilder.build(), randomUUID(), NON_CPS_PROSECUTORS, "DVLA1");
        Assert.assertTrue(isNonCPSUserOrg.isPresent());
        assertThat(isNonCPSUserOrg.get(), is("OrganisationMisMatch"));
    }

    private JsonObject getUserGroupsResponse() {
        final JsonArrayBuilder groupsArray = createArrayBuilder();
        groupsArray.add(createObjectBuilder().add("groupId", String.valueOf(randomUUID())).add("groupName", "Non CPS Prosecutors"));
        groupsArray.add(createObjectBuilder().add("groupId", String.valueOf(randomUUID())).add("groupName", "DVLA Prosectutors").add("prosecutingAuthority" , "DVLA"));
        return createObjectBuilder().add("groups", groupsArray).build();
    }

}
