package uk.gov.moj.cpp.progression;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.WireMockStubUtils.mockUpdateResultsMaterialStatus;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.core.courts.CreateNowsRequest;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Now;
import uk.gov.justice.core.courts.NowVariant;
import uk.gov.justice.core.courts.NowsRequested;
import uk.gov.justice.courts.progression.query.Courtdocument;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.domain.notification.Subscription;
import uk.gov.moj.cpp.progression.domain.notification.Subscriptions;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.test.TestTemplates;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.NotifyStub;
import uk.gov.moj.cpp.progression.util.QueryUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.Json;
import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.jayway.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


@SuppressWarnings({"unchecked", "squid:S1607"})
public class GenerateNowsIT extends AbstractIT {

    private static final MessageProducer messageProducerClientPublic = QueueUtil.publicEvents.createProducer();

    private static final String ORIGINATOR = "originator";
    private static final String ORIGINATOR_VALUE = "court";
    private static final String DOCUMENT_TEXT = STRING.next();

    public static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "progression.query.courtdocuments";
    public static final String COURT_DOCUMENT_SEARCH_NAME = "progression.query.courtdocument";

    public static final String APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON = "application/vnd.progression.query.courtdocuments+json";
    public static final String APPLICATION_VND_PROGRESSION_QUERY_COURTDOCUMENT_JSON = "application/vnd.progression.query.courtdocument+json";
    public static final String GENERATED = "generated";

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldAddUpdateNowsAndUpdateStatus() throws IOException {

        wireMockConfig().notifier(new ConsoleNotifier(true));
        RestHelper.createMockEndpoints();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        stubMaterialUploadFile();

        final CreateNowsRequest createNowsRequest = TestTemplates.generateNowsRequestTemplate(UUID.randomUUID());
        final Now now = createNowsRequest.getNows().get(0);
        final NowVariant requestedMaterial = now.getRequestedMaterials().get(0);

        final Subscriptions subscriptions = subscriptions(requestedMaterial.getKey().getUsergroups().get(0));
        final UUID nowsTypeId = now.getNowsTypeId();
        final UUID materialId = requestedMaterial.getMaterialId();
        final Hearing hearing = createNowsRequest.getHearing();
        final UUID userId = randomUUID();
        final UUID caseId = hearing.getProsecutionCases().get(0).getId();

        HearingStub.stubSubscriptions(objectToJsonObjectConverter.convert(subscriptions), nowsTypeId);
        NotifyStub.stubNotifications();
        mockUpdateResultsMaterialStatus(hearing.getId(), materialId);

        final NowsRequested nowsRequested = new NowsRequested(createNowsRequest);

        Utilities.makeCommand(requestSpec, "progression.generate-nows")
                .ofType("application/vnd.progression.generate-nows+json")
                .withPayload(nowsRequested)
                .executeSuccessfully();

        // ensure upload material and update status called
        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> uploadMaterialCalled(materialId));

//        Function<CourtDocumentIndex, Optional<CourtDocument>> accessor = courtDocumentIndex -> Optional.of(courtDocumentIndex.getDocument());

        final BeanMatcher<Courtdocuments> pregeneratedResultMatcher = isBean(Courtdocuments.class)
                .with(Courtdocuments::getDocumentIndices, first(is(isBean(CourtDocumentIndex.class)
                        .withOptional(courtDocumentIndex -> Optional.of(courtDocumentIndex.getDocument()), isBean(CourtDocument.class)
                                        .withValue(CourtDocument::getName, "Imprisonment Order")
                        )
                )));

        final RequestParams preGeneratedRequestParams = requestParams(getURL(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, caseId.toString()),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 30, pregeneratedResultMatcher, Courtdocuments.class);

        sendMaterialFileUploadedPublicEvent(materialId, userId);

        Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> updateMaterialStatusResultsCommandCalled(materialId));
        //Function<Material, Optional<String>> accessor = material -> Optional.of(material.getGenerationStatus());
        final BeanMatcher<Courtdocuments> generatedResultMatcher = isBean(Courtdocuments.class)
                .with(Courtdocuments::getDocumentIndices, first(is(isBean(CourtDocumentIndex.class)
                        .withOptional(courtDocumentIndex -> Optional.of(courtDocumentIndex.getDocument()), isBean(CourtDocument.class)
                                .withValue(CourtDocument::getName, "Imprisonment Order")
                                .with(CourtDocument::getMaterials, first(BeanMatcher.isBean(Material.class)
                                    .withOptionalValue(material -> Optional.of(material.getGenerationStatus()), GENERATED)
                                ))
                        )
                )));

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 30, generatedResultMatcher, Courtdocuments.class);
        //check with document api
        final BeanMatcher<Courtdocument> documentResultMatcher = isBean(Courtdocument.class)
                .withOptional(courtDocument -> Optional.of(courtDocument.getCourtDocument()), isBean(CourtDocument.class)
                        .withValue(CourtDocument::getCourtDocumentId, now.getId())
                        .with(CourtDocument::getMaterials, first(isBean(Material.class)
                                        .withValue(Material::getId, requestedMaterial.getMaterialId())
                                        .withOptionalValue(material -> Optional.of(material.getGenerationStatus()), "generated")
                                )
                        )
                );

        final RequestParams documentSearchParams = requestParams(getURL(COURT_DOCUMENT_SEARCH_NAME, now.getId()),
                APPLICATION_VND_PROGRESSION_QUERY_COURTDOCUMENT_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        QueryUtil.waitForQueryMatch(documentSearchParams, 30, documentResultMatcher, Courtdocument.class);

        NotifyStub.verifyNotification(subscriptions.getSubscriptions().get(0), asList(hearing.getProsecutionCases().get(0).getProsecutionCaseIdentifier().getCaseURN()));
        DocumentGeneratorStub.verifyCreate(asList(materialId.toString()));

    }

    private Subscriptions subscriptions(final String userGroup) {
        final Subscriptions subscriptions = new Subscriptions();
        final Subscription subscription = new Subscription();
        subscription.setChannel("email");
        subscription.setDestination("generatenows@test.com");
        subscription.setUserGroups(asList(userGroup));
        Map<String, String> channelProperties = new HashMap<>();
        channelProperties.put("templateId", UUID.randomUUID().toString());
        subscription.setChannelProperties(channelProperties);

        subscriptions.setSubscriptions(asList(subscription));
        return subscriptions;
    }

    private boolean uploadMaterialCalled(UUID materialId) {
        return findAll(postRequestedFor(urlPathMatching(WireMockStubUtils.MATERIAL_UPLOAD_COMMAND)))
                .stream()
                .anyMatch(log -> log.getBodyAsString().contains(materialId.toString()));
    }

    private boolean updateMaterialStatusResultsCommandCalled(UUID materialId) {
        return findAll(postRequestedFor(urlPathMatching(WireMockStubUtils.MATERIAL_STATUS_UPLOAD_COMMAND)))
                .stream()
                .anyMatch(log -> log.getBodyAsString().contains(materialId.toString()));
    }

    private void sendMaterialFileUploadedPublicEvent(final UUID materialId, final UUID userId) {
        final String commandName = "material.material-added";
        final Metadata metadata = getMetadataFrom(userId.toString(), commandName);
        final JsonObject payload = Json.createObjectBuilder().add("materialId", materialId.toString()).add(
                "fileDetails",
                Json.createObjectBuilder().add("alfrescoAssetId", "aGVsbG8=")
                        .add("mimeType", "text/plain").add("fileName", "file.txt"))
                .add("materialAddedDate", "2016-04-26T13:01:787.345").build();
        QueueUtil.sendMessage(messageProducerClientPublic, commandName, payload, metadata);
    }

    private Metadata getMetadataFrom(final String userId, final String commandName) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ORIGINATOR, ORIGINATOR_VALUE)
                .add(ID, randomUUID().toString())
                .add(USER_ID, userId)
                .add(NAME, commandName)
                .build()).build();
    }

    @After
    public void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

}
