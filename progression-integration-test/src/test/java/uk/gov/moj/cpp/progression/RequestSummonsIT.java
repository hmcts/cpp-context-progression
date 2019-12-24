package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.ListingStub.getHearingIdFromListCourtHearingRequest;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.QueryUtil.waitForQueryMatch;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
@Ignore
public class RequestSummonsIT extends AbstractIT {

    private static final String PUBLIC_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String CASE_ID = randomUUIDString();
    private static final String COURT_DOCUMENT_ID = randomUUIDString();
    private static final String MATERIAL_ID_ACTIVE = randomUUIDString();
    private static final String MATERIAL_ID_DELETED = randomUUIDString();
    private static final String DEFENDANT_ID = randomUUIDString();
    private static final String COURT_CENTRE_ID = randomUUIDString();
    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createProducer();
    private static final MessageConsumer PRIVATE_MESSAGE_CONSUMER = privateEvents.createConsumer("progression.event.nows-material-request-recorded");
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "progression-service/query/api/rest/progression/courtdocumentsearch?caseId=%s";

    private static final String APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON = "application/vnd.progression.query.courtdocuments+json";

    @AfterClass
    public static void tearDown() throws JMSException {
        PUBLIC_MESSAGE_PRODUCER.close();
        PRIVATE_MESSAGE_CONSUMER.close();
    }

    private static String randomUUIDString() {
        return randomUUID().toString();
    }

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();
    }

    @Test
    public void shouldRequestSummons() throws Exception {
        addProsecutionCaseToCrownCourt(CASE_ID, DEFENDANT_ID, MATERIAL_ID_ACTIVE, MATERIAL_ID_DELETED, COURT_DOCUMENT_ID, "2daefec3-2f76-8109-82d9-2e60544a6c02");
        ListingStub.verifyPostListCourtHearing(CASE_ID, DEFENDANT_ID);

        String hearingId = getHearingIdFromListCourtHearingRequest();

        final Metadata metadata = generateMetadata();
        JsonObject hearingConfirmedPayload = generateHearingConfirmedPayload(hearingId);
        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_HEARING_CONFIRMED, hearingConfirmedPayload, metadata);

        verifyPrintRequestAccepted();

        // check document query
        final BeanMatcher<Courtdocuments> pregeneratedResultMatcher = isBean(Courtdocuments.class)
                .with(Courtdocuments::getDocumentIndices, first(Is.is(isBean(CourtDocumentIndex.class)
                        .with(CourtDocumentIndex::getDocument, isBean(CourtDocument.class)
                                .withValue(CourtDocument::getDocumentTypeDescription, "Summons")
                        )
                )));

        final RequestParams preGeneratedRequestParams = requestParams(getReadUrl(String.format(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, CASE_ID)),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(AbstractIT.CPP_UID_HEADER.getName(), AbstractIT.CPP_UID_HEADER.getValue())
                .build();

        waitForQueryMatch(preGeneratedRequestParams, 30, pregeneratedResultMatcher, Courtdocuments.class);


    }

    private JsonObject generateHearingConfirmedPayload(final String hearingId) {
        String payloadStr = getPayload(PUBLIC_HEARING_CONFIRMED + ".json")
                .replaceAll("CASE_ID", CASE_ID)
                .replaceAll("COURT_CENTRE_ID", COURT_CENTRE_ID)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", DEFENDANT_ID);
        return new StringToJsonObjectConverter().convert(payloadStr);
    }

    private Metadata generateMetadata() {
        return metadataBuilder()
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .withName(PUBLIC_HEARING_CONFIRMED)
                .build();
    }

    private void verifyPrintRequestAccepted() {
        final JsonPath jsonResponse = retrieveMessage(PRIVATE_MESSAGE_CONSUMER);

        assertThat(jsonResponse.get("context.caseId"), is(CASE_ID));
    }
}
