package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationExtractPdf;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;

import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ApplicationExtractIT extends AbstractIT {
    private String hearingId;
    private String courtApplicationId;

    private static final String DOCUMENT_TEXT = STRING.next();

    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer("public.progression.court-application-created");

    @Before
    public void setUp() {
        hearingId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        consumerForCourtApplicationCreated.close();
    }

    @Test
    public void shouldGetApplicationExtract_whenExtractTypeIsStandAloneApplication() throws Exception {
        // given
        addStandaloneCourtApplication(courtApplicationId, randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        verifyInMessagingQueueForStandaloneCourtApplicationCreated();
        // when
        final String documentContentResponse = getApplicationExtractPdf(courtApplicationId, hearingId);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String arnResponse = message.get().getString("arn");
        assertThat(10, is(arnResponse.length()));
    }
}
