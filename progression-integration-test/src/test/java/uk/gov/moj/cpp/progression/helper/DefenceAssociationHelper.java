package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENCE_DISASSOCIATION_FOR_DEFENDANT;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getCommandUri;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.ReadContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

public class DefenceAssociationHelper implements AutoCloseable {

    private static final String DEFENCE_ASSOCIATION_MEDIA_TYPE = "application/vnd.progression.associate-defence-organisation+json";
    private static final String DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_NAME = "progression.associate-defence-organisation.json";

    private static final String DEFENCE_DISASSOCIATION_MEDIA_TYPE = "application/vnd.progression.disassociate-defence-organisation+json";
    private static final String DEFENCE_DISASSOCIATION_REQUEST_TEMPLATE_NAME = "progression.disassociate-defence-organisation.json";

    public static final String DEFENCE_ASSOCIATION_QUERY_ENDPOINT = "/defendants/{0}/associatedOrganisation";
    public static final String DEFENCE_ASSOCIATION_QUERY_MEDIA_TYPE = "application/vnd.progression.query.associated-organisation+json";

    private final MessageConsumer publicEventsConsumerForDefenceAssociationForDefendant =
            QueueUtil.publicEvents.createConsumer(EVENT_SELECTOR_DEFENCE_ASSOCIATION_FOR_DEFENDANT);

    private final MessageConsumer publicEventsConsumerForDefenceDisassociationForDefendant =
            QueueUtil.publicEvents.createConsumer(EVENT_SELECTOR_DEFENCE_DISASSOCIATION_FOR_DEFENDANT);


    public static void associateOrganisation(final String defendantId,
                                             final String userId) throws IOException {
        String body = readFile(DEFENCE_ASSOCIATION_REQUEST_TEMPLATE_NAME);
        invokeCommand(defendantId, userId, body, DEFENCE_ASSOCIATION_MEDIA_TYPE);
    }

    public static void disassociateOrganisation(final String defendantId,
                                                final String userId,
                                                final String organisationId) throws IOException {
        String body = readFile(DEFENCE_DISASSOCIATION_REQUEST_TEMPLATE_NAME);
        body = body.replaceAll("%ORGANISATION_ID%", organisationId);
        invokeCommand(defendantId, userId, body, DEFENCE_DISASSOCIATION_MEDIA_TYPE);
    }

    private static void invokeCommand(final String defendantId,
                                      final String userId,
                                      final String body,
                                      final String mediaType) {

        final RestClient restClient = new RestClient();
        final javax.ws.rs.core.Response writeResponse =
                restClient.postCommand(getCommandUri("/defendants/" + defendantId + "/defenceorganisation"),
                        mediaType,
                        body,
                        createHttpHeaders(userId)
                );
        assertThat(writeResponse.getStatus(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    public static MultivaluedMap<String, Object> createHttpHeaders(final String userId) {
        MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
        headers.add(HeaderConstants.USER_ID, userId);
        return headers;
    }

    public void verifyDefenceOrganisationAssociatedEventGenerated(final String defendantId, final String organisationId) throws Exception {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForDefenceAssociationForDefendant);
        assertExternalPublicEventRaised(defendantId, organisationId, message);

    }

    public void verifyDefenceOrganisationDisassociatedEventGenerated(final String defendantId, final String organisationId) throws Exception {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventsConsumerForDefenceDisassociationForDefendant);
        assertExternalPublicEventRaised(defendantId, organisationId, message);

    }

    private static void assertExternalPublicEventRaised(final String defendantId, final String organisationId, final Optional<JsonObject> message) {
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendantId", Matchers.hasToString(
                Matchers.containsString(defendantId)))));
        assertThat(message.get(), isJson(withJsonPath("$.organisationId", Matchers.hasToString(
                Matchers.containsString(organisationId)))));
    }

    public static void verifyDefenceOrganisationAssociatedDataPersisted(final String defendantId,
                                                                        final String organisationId,
                                                                        final String userId) {

        List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.association.organisationId", IsEqual.equalTo(organisationId)))
                .add(withJsonPath("$.association.status", IsEqual.equalTo("Active Barrister/Solicitor of record")))
                .add(withJsonPath("$.association.address.address1", IsEqual.equalTo("Legal House")))
                .add(withJsonPath("$.association.address.address4", IsEqual.equalTo("London")))
                .add(withJsonPath("$.association.address.addressPostcode", IsEqual.equalTo("SE14 2AB")))
                .build();

        RestHelper.pollForResponse(format(DEFENCE_ASSOCIATION_QUERY_ENDPOINT, defendantId),
                DEFENCE_ASSOCIATION_QUERY_MEDIA_TYPE,
                userId,
                matchers);
    }

    public static void verifyDefenceOrganisationDisassociatedDataPersisted(final String defendantId,
                                                                           final String organisationId,
                                                                           final String userId) {

        List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.association"))
                .add(withoutJsonPath("$.association.organisationId"))
                .build();

        RestHelper.pollForResponse(format(DEFENCE_ASSOCIATION_QUERY_ENDPOINT, defendantId),
                DEFENCE_ASSOCIATION_QUERY_MEDIA_TYPE,
                userId,
                matchers);
    }

    private static String readFile(final String filename) throws IOException {
        return IOUtils.toString(DefenceAssociationHelper.class.getClassLoader().getResourceAsStream(filename));
    }

    @Override
    public void close() {
        closeSilently(publicEventsConsumerForDefenceAssociationForDefendant);
    }

    private static void closeSilently(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                //close silently
            }
        }
    }
}
