package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenceAssociationHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationHelper.class);

    private static final String DEFENCE_ASSOCIATION_MEDIA_TYPE = "application/vnd.progression.associate-defence-organisation+json";
    private static final String DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_REQUEST_NAME = "progression.associate-defence-organisation.json";

    private final MessageConsumer publicEventsConsumerForDefenceAssociationForDefendant =
            QueueUtil.publicEvents.createConsumer(
                    EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT);

    public void initiateDefenceAssociationForDefendant(final String defendantId, final String organisationId) throws IOException {

        String body = Resources.toString(Resources.getResource(DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_REQUEST_NAME),
                Charset.defaultCharset());
        body = body.replaceAll("%REQUESTOR_ORGANISATION_ID%", organisationId);

        final Response writeResponse = postCommand(getCommandUri("/defendants/" + defendantId + "/defenceorganisation"),
                DEFENCE_ASSOCIATION_MEDIA_TYPE,
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public void verifyPublicEventRaisedForDefenceAssociationForDefendant(final String defendantId, final String organisationId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForDefenceAssociationForDefendant);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendantId", Matchers.hasToString(
                Matchers.containsString(defendantId)))));
        assertThat(message.get(), isJson(withJsonPath("$.defenceOrganisationId", Matchers.hasToString(
                Matchers.containsString(organisationId)))));
    }
}
