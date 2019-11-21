package uk.gov.moj.cpp.progression;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.test.TestTemplates;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.QueryUtil;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
@SuppressWarnings({"squid:S1607"})
public class GenerateNowsIT extends AbstractIT {

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();

    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";
    private static final String DOCUMENT_TEXT = STRING.next();

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "progression.query.courtdocuments";
    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCHDEFENDANT = "progression.query.courtdocumentsbydefendant";

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    private static final String PUBLIC_NOW_DOCUMENT_REQUEST = "public.hearing.now-document-requested";

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldAddUpdateNowsAndUpdateStatus() {

        createMockEndpoints();

        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);

        final NowDocumentRequest nowDocumentRequest = TestTemplates.generateNowDocumentRequestTemplate(UUID.randomUUID());

        final UUID userId = randomUUID();
        final UUID caseId = nowDocumentRequest.getCaseId();
        final UUID materialId = nowDocumentRequest.getMaterialId();
        final UUID defendantId = nowDocumentRequest.getDefendantId();

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(nowDocumentRequest);

        sendMessage(messageProducerClientPublic, PUBLIC_NOW_DOCUMENT_REQUEST, requestAsJson,
                metadataOf(randomUUID(), PUBLIC_NOW_DOCUMENT_REQUEST).withUserId(randomUUID().toString()).build());

        final BeanMatcher<Courtdocuments> preGeneratedResultMatcher = isBean(Courtdocuments.class)
                .with(Courtdocuments::getDocumentIndices, first(is(isBean(CourtDocumentIndex.class)
                        .withOptional(courtDocumentIndex -> Optional.of(courtDocumentIndex.getDocument()), isBean(CourtDocument.class)
                                .withValue(CourtDocument::getName, nowDocumentRequest.getNowContent().getOrderName())))));

        final RequestParams preGeneratedRequestParams = requestParams(getURL(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, caseId.toString()),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 45, preGeneratedResultMatcher, Courtdocuments.class);

        final RequestParams preGeneratedRequestParamsDefendant = requestParams(getURL(PROGRESSION_QUERY_COURTDOCUMENTSSEARCHDEFENDANT,
                defendantId.toString()),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParamsDefendant, 45, preGeneratedResultMatcher, Courtdocuments.class);


        sendMaterialFileUploadedPublicEvent(materialId, userId);

        DocumentGeneratorStub.verifyCreate(singletonList(nowDocumentRequest.getNowContent().getCases().get(0).getUrn()));
    }

    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId) {
        final String commandName = "material.material-added";
        final Metadata metadata = getMetadataFrom(userId.toString());
        final JsonObject payload = Json.createObjectBuilder().add("materialId", materialId.toString()).add(
                "fileDetails",
                Json.createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2016-04-26T13:01:787.345").build();
        QueueUtil.sendMessage(messageProducerClientPublic, commandName, payload, metadata);
    }

    private Metadata getMetadataFrom(final String userId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add(ID, randomUUID().toString())
                .add(USER_ID, userId)
                .add(NAME, "material.material-added")
                .build()).build();
    }

    @After
    public void tearDown() throws JMSException {
        closeSilently(messageProducerClientPublic);
    }

}

