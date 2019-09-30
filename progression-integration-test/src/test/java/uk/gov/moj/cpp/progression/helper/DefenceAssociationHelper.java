package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponseWithUserId;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommandWithUserId;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class DefenceAssociationHelper {

    private static final String DEFENCE_ASSOCIATION_MEDIA_TYPE = "application/vnd.progression.associate-defence-organisation+json";
    private static final String DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_REQUEST_NAME = "progression.associate-defence-organisation.json";
    private static final String ASSOCIATED_STATUS = "Active Barrister/Solicitor of record";
    private static final String ADDRESS_LINE_1 = "Legal House";
    private static final String ADDRESS_LINE_4 = "London";
    private static final String POST_CODE = "SE14 2AB";


    private static final MessageConsumer publicEventsConsumerForDefenceAssociationForDefendant =
            QueueUtil.publicEvents.createConsumer(
                    EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT);

    public static void associateOrganisation(final String defendantId,
                                             final String userId) throws IOException {

        final String body = Resources.toString(Resources.getResource(DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_REQUEST_NAME),
                Charset.forName("UTF-8"));

        final Response writeResponse = postCommandWithUserId(getCommandUri("/defendants/" + defendantId + "/defenceorganisation"),
                DEFENCE_ASSOCIATION_MEDIA_TYPE, body, userId);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static void verifyDefenceOrganisationAssociatedEventGenerated(final String defendantId, final String organisationId) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForDefenceAssociationForDefendant);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendantId", Matchers.hasToString(
                Matchers.containsString(defendantId)))));
        assertThat(message.get(), isJson(withJsonPath("$.organisationId", Matchers.hasToString(
                Matchers.containsString(organisationId)))));
    }

    public static void verifyDefenceOrganisationAssociatedDataPersisted(final String defendantId,
                                                                        final String organisationId,
                                                                        final String userId) {
        final String associatedOrganisationResponse =
                pollForResponseWithUserId(join("", "/defendants/", defendantId, "/associatedOrganisation"),
                        "application/vnd.progression.query.associated-organisation+json", userId);

        final JsonObject associatedOrganisationJsonObject = getJsonObject(associatedOrganisationResponse);
        final JsonObject association = associatedOrganisationJsonObject.getJsonObject("association");
        assertThat(association, notNullValue());
        assertTrue(association.getString("organisationId").equals(organisationId));
        assertTrue(association.getString("status").equals(ASSOCIATED_STATUS));

        final JsonObject organisationAddress = association.getJsonObject("address");
        assertEquals(ADDRESS_LINE_1, organisationAddress.getString("address1"));
        assertEquals(ADDRESS_LINE_4, organisationAddress.getString("address4"));
        assertEquals(POST_CODE, organisationAddress.getString("addressPostcode"));

    }
}
