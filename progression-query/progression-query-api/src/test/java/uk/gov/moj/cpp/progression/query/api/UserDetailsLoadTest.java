package uk.gov.moj.cpp.progression.query.api;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.services.core.requester.Requester;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsLoadTest extends QueryClientTestBase {

    @InjectMocks
    private UserDetailsLoader userDetailsLoader;

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

    @Test
    public void testLoad() {
        final UserDetailsQueryResult userDetails = new UserDetailsQueryResult();
        userDetails.setId(UUID.randomUUID());
        userDetails.setFirstName("andrew");
        userDetails.setFirstName("eldritch");
        mockQuery(UserDetailsLoader.GET_USER_DETAILS_REQUEST_ID, userDetails, true);
        UserGroupsUserDetails result = userDetailsLoader.getUserById(requester, context, userDetails.getId());
        Assert.assertEquals(userDetails.getId(), result.getUserId());
        Assert.assertEquals(userDetails.getFirstName(), result.getFirstName());
        Assert.assertEquals(userDetails.getLastName(), result.getLastName());

    }
}
