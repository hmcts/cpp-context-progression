package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class DefenceAssociationIT extends BaseIntegrationTest {

    @Before
    public void setUp() throws IOException {
        createMockEndpoints();

    }

    @Test
    public void shouldPerformAssociation() throws Exception {

        final String defendantId = UUID.randomUUID().toString();
        final String organisationId = UUID.randomUUID().toString();

        String body = Resources.toString(Resources.getResource("progression.associate-defence-organisation.json"), Charset.defaultCharset());
        body = body.replaceAll("%REQUESTOR_ORGANISATION_ID%", organisationId);

        final Response writeResponse = postCommand(getCommandUri("/defendants/"+defendantId+"/defenceorganisation"),
                "application/vnd.progression.associate-defence-organisation+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

    }
}
