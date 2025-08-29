package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.UserGroupsUserDetails;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserDetailsLoadTest {

    public static final String FIRST_NAME = "andrew";
    public static final String LAST_NAME = "eldritch";

    @InjectMocks
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private JsonEnvelope context;

    @Mock
    protected Requester requester;

    final static class UserDetailsQueryResult {
        private UUID id;
        private String firstName;
        private String lastName;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    @BeforeEach
    public void setup() {
        when(context.metadata()).thenReturn(Envelope.metadataBuilder()
                .withId(randomUUID())
                .withName("usersgroups.get-user-details")
                .build());
        when(requester.requestAsAdmin(any(JsonEnvelope.class), any())).thenAnswer(invocationOnMock -> {
            final JsonEnvelope envelope = (JsonEnvelope) invocationOnMock.getArguments()[0];
            JsonObject responsePayload = createObjectBuilder()
                    .add("firstName", FIRST_NAME)
                    .add("lastName", LAST_NAME)
                    .build();

            return envelopeFrom(envelope.metadata(), responsePayload);
        });
    }

    @Test
    public void testLoad() {
        final UserDetailsQueryResult userDetails = new UserDetailsQueryResult();
        userDetails.setId(randomUUID());
        userDetails.setFirstName(FIRST_NAME);
        userDetails.setLastName(LAST_NAME);
        UserGroupsUserDetails result = userDetailsLoader.getUserById(requester, context, userDetails.getId());
        assertThat(userDetails.getId(), is(result.getUserId()));
        assertThat(userDetails.getFirstName(), is(result.getFirstName()));
        assertThat(userDetails.getLastName(), is(result.getLastName()));

    }
}
