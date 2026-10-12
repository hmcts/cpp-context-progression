package uk.gov.moj.cpp.progression;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;

import javax.json.JsonObject;
import java.util.Random;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPrivateJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;

@SuppressWarnings("squid:S1607")
public class ApplicationStatusUpdateIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClient = newPrivateJmsMessageProducerClientProvider(CONTEXT_NAME).getMessageProducerClient();
    private static final String APPLICATION_STATUS_CHANGED = "progression.event.court-application-status-changed";
    private static String userId;
    private ConvictionDateHelper helper;
    private String caseId;
    private String offenceId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        userId = randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new ConvictionDateHelper(caseId, offenceId, null);

    }

    @Test
    public void shouldRetainFinalApplicationStatusWhenConvictionAdded() throws Exception {
        // given
        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-standalone-application.json");
        pollForApplication(courtApplicationId);

        publishApplicationStatusChangeEvent(courtApplicationId);

        // when
        new ConvictionDateHelper(null, null, courtApplicationId).addConvictionDate();

        final Matcher[] applicationStatusMatcher = {
                withJsonPath("$.courtApplication.applicationStatus", is("FINALISED"))
        };
        //then
        pollForApplication(courtApplicationId, applicationStatusMatcher);
    }

    @Test
    public void shouldRetainFinalApplicationStatusWhenConvictionRemoved() throws Exception {
        // given
        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-standalone-application.json");
        pollForApplication(courtApplicationId);

        publishApplicationStatusChangeEvent(courtApplicationId);

        // when
        new ConvictionDateHelper(null, null, courtApplicationId).removeConvictionDate();

        final Matcher[] applicationStatusMatcher = {
                withJsonPath("$.courtApplication.applicationStatus", is("FINALISED"))
        };
        //then
        pollForApplication(courtApplicationId, applicationStatusMatcher);
    }

    private void publishApplicationStatusChangeEvent(String courtApplicationId) {
        final Metadata metadata = createMetadata(APPLICATION_STATUS_CHANGED);
        JsonObject convictionDateChangedPayload = new StringToJsonObjectConverter().convert("{\"applicationStatus\":\"FINALISED\",\"id\":\"" + courtApplicationId + "\"}");
        final JsonEnvelope eventEnvelope = envelopeFrom(metadata, convictionDateChangedPayload);
        messageProducerClient.sendMessage(APPLICATION_STATUS_CHANGED, eventEnvelope);
    }

    private Metadata createMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withStreamId(randomUUID())
                .withPosition(1)
                .withPreviousEventNumber(123)
                .withEventNumber(new Random().nextLong())
                .withSource("event-listener-test")
                .withName(eventName)
                .withUserId(randomUUID().toString())
                .build();
    }
}
