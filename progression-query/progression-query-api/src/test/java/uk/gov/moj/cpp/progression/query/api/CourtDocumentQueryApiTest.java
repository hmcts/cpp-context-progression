package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentApi.MATERIAL_CONTENT_FOR_PROSECUTION;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.APPLICATION_ID;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.CASE_ID;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.COURT_DOCUMENTS_SEARCH_PROSECUTION;
import static uk.gov.moj.cpp.progression.query.api.CourtDocumentQueryApi.DEFENDANT_ID;

import uk.gov.QueryClientTestBase;
import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.CourtDocumentQueryView;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.SharedCourtDocumentsQueryView;
import uk.gov.moj.cpp.progression.query.api.service.UsersGroupQueryService;
import uk.gov.moj.cpp.progression.query.view.UserDetailsLoader;
import uk.gov.moj.cpp.progression.query.view.UserGroupsDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentQueryApiTest {

    public static final String JSON_COURT_DOCUMENT_WITH_RBAC_JSON = "json/courtDocumentWithRBAC.json";
    public static final String PROGRESSION_QUERY_COURTDOCUMENT = "progression.query.courtdocument";
    private static final String COURT_DOCUMENTS_SEARCH_NAME = "progression.query.courtdocuments";
    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope queryForMaterial;

    @Mock
    private JsonEnvelope response;

    @Mock
    private Requester requester;

    @Mock
    private CourtDocumentQueryView courtDocumentQueryView;

    @Mock
    private HearingDetailsLoader hearingDetailsLoader;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private SharedCourtDocumentsQueryView sharedCourtDocumentsQueryView;

    @Mock
    private Enveloper enveloper;

    @Mock
    private DefenceQueryService defenceQueryService;

    @Mock
    private Function<Object, JsonEnvelope> objectJsonEnvelopeFunction;

    @Mock
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Mock
    UsersGroupQueryService usersGroupQueryService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private CourtDocumentApi courtDocumentApi;

    @InjectMocks
    private CourtDocumentQueryApi courtDocumentQueryApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCaseDocumentMetadataQuery() {

        final JsonObject jsonObjectMaterial = createObjectBuilder().add("materialId", randomUUID().toString()).build();
        when(enveloper.withMetadataFrom(query, "material.query.material-metadata")).thenReturn(objectJsonEnvelopeFunction);
        when(objectJsonEnvelopeFunction.apply(jsonObjectMaterial)).thenReturn(queryForMaterial);
        when(requester.requestAsAdmin(queryForMaterial)).thenReturn(response);
        when(query.payloadAsJsonObject()).thenReturn(jsonObjectMaterial);
        assertThat(courtDocumentApi.getCaseDocumentMetadata(query), equalTo(response));
    }

    @Test
    public void shouldHandleCaseDocumentDetailsQuery() {
        assertThat(courtDocumentApi.getCaseDocumentDetails(query), equalTo(query));
    }

    @Test
    public void shouldHandleCourtDocumentNotificationStatusQuery() {
        when(courtDocumentQueryView.getCaseNotifications(query)).thenReturn(response);
        assertThat(courtDocumentQueryApi.getCaseNotificationStatus(query), equalTo(response));
    }

    @Test
    public void shouldHandleCourtDocumentQuery() {

        final JsonObject jsonObjectPayload = QueryClientTestBase.readJson(JSON_COURT_DOCUMENT_WITH_RBAC_JSON, JsonObject.class);
        final Metadata metadata = QueryClientTestBase.metadataFor(PROGRESSION_QUERY_COURTDOCUMENT);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(courtDocumentQueryView.getCourtDocument(any())).thenReturn(envelope);
        courtDocumentQueryApi.getCourtDocument(envelope);
        assertThat(courtDocumentQueryApi.getCourtDocument(envelope), equalTo(envelope));
    }

    @Test
    public void shouldSearchCourtDocumentsWhenHearingTypeIsTrialAndDefendantIsNullForMagsUser() {
        ArgumentCaptor<JsonEnvelope> argumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();

        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_NAME);
        final UUID userId = UUID.fromString(metadata.userId().get());
        final JsonEnvelope expectedResult = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("documentIndices", createArrayBuilder().build())
                .build());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("hearingId", hearingId.toString())
                .build());
        final UUID trialType = randomUUID();
        final Map<UUID, ReferenceDataService.ReferenceHearingDetails> hearingTypes = new HashMap<>();
        final ReferenceDataService.ReferenceHearingDetails referenceHearingDetails = new ReferenceDataService.ReferenceHearingDetails(trialType, "", "", true);
        hearingTypes.put(trialType, referenceHearingDetails);
        when(referenceDataService.getHearingTypes(any())).thenReturn(hearingTypes);
        when(sharedCourtDocumentsQueryView.getSharedCourtDocuments(any())).thenReturn(expectedResult);
        final HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setType("Trial");
        hearingDetails.setHearingTypeId(trialType);
        hearingDetails.addUserId(userId);
        when(hearingDetailsLoader.getHearingDetails(any(), eq(hearingId))).thenReturn(hearingDetails);
        final UUID groupId = randomUUID();
        final UserGroupsDetails userGroupsDetails = new UserGroupsDetails(groupId, "Magistrates");
        when(userDetailsLoader.getGroupsUserBelongsTo(any(), eq(userId))).thenReturn(Arrays.asList(userGroupsDetails));

        final JsonEnvelope result = courtDocumentQueryApi.searchCourtDocuments(envelope);

        assertThat(result, equalTo(expectedResult));
        verify(sharedCourtDocumentsQueryView).getSharedCourtDocuments(argumentCaptor.capture());

        final JsonEnvelope argumentCaptorValue = argumentCaptor.getValue();
        assertThat(argumentCaptorValue.payloadAsJsonObject().containsKey("defendantId"), is(false));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("userGroupId"), is(groupId.toString()));
    }

    @Test
    public void shouldSearchCourtDocumentsWhenHearingTypeIsTrialAndDefendantIsNotNullForMagsUser() {
        ArgumentCaptor<JsonEnvelope> argumentCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_NAME);
        final UUID userId = UUID.fromString(metadata.userId().get());
        final JsonEnvelope expectedResult = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("documentIndices", createArrayBuilder().build())
                .build());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("hearingId", hearingId.toString())
                .add("defendantId", defendantId.toString())
                .build());
        final UUID trialType = randomUUID();
        final Map<UUID, ReferenceDataService.ReferenceHearingDetails> hearingTypes = new HashMap<>();
        final ReferenceDataService.ReferenceHearingDetails referenceHearingDetails = new ReferenceDataService.ReferenceHearingDetails(trialType, "", "", true);
        hearingTypes.put(trialType, referenceHearingDetails);
        when(referenceDataService.getHearingTypes(any())).thenReturn(hearingTypes);
        when(sharedCourtDocumentsQueryView.getSharedCourtDocuments(any())).thenReturn(expectedResult);
        final HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setType("Trial");
        hearingDetails.setHearingTypeId(trialType);
        hearingDetails.addUserId(userId);
        when(hearingDetailsLoader.getHearingDetails(any(), eq(hearingId))).thenReturn(hearingDetails);
        final UUID groupId = randomUUID();
        final UserGroupsDetails userGroupsDetails = new UserGroupsDetails(groupId, "Magistrates");
        when(userDetailsLoader.getGroupsUserBelongsTo(any(), eq(userId))).thenReturn(Arrays.asList(userGroupsDetails));

        final JsonEnvelope result = courtDocumentQueryApi.searchCourtDocuments(envelope);

        assertThat(result, equalTo(expectedResult));
        verify(sharedCourtDocumentsQueryView).getSharedCourtDocuments(argumentCaptor.capture());

        final JsonEnvelope argumentCaptorValue = argumentCaptor.getValue();
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("defendantId"), is(defendantId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("caseId"), is(caseId.toString()));
        assertThat(argumentCaptorValue.payloadAsJsonObject().getString("userGroupId"), is(groupId.toString()));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenNoCaseIdAndNoApplicationIdQueryParamInSearchCourtDocumentsForProsecution() {

        final JsonObject jsonObjectPayload = createObjectBuilder().build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        assertThrows(BadRequestException.class, () -> courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope));
    }

    @Test
    public void shouldReturnApplicationDocumentsWhenNoCaseIdAndApplicationIdQueryParamInSearchCourtDocumentsForProsecution() {

        String applicationId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(APPLICATION_ID, applicationId)
                .add("isProsecuting",true)
                .add("caseId",randomUUID().toString())
                .build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_NAME);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        when(courtDocumentQueryView.searchCourtDocumentsForProsecution(any())).thenReturn(envelope);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope);
        verify(courtDocumentQueryView).searchCourtDocumentsForProsecution(jsonEnvelopeArgumentCaptor.capture());

        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo(COURT_DOCUMENTS_SEARCH_NAME));
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonObject(CASE_ID), nullValue());
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonString(APPLICATION_ID).getString(), is(applicationId));
    }

    @Test
    public void shouldThrowForbiddenRequestExceptionWhenUserNotInProsecutorRoleForTheCaseId() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(defenceQueryService.isUserProsecutingCase(envelope, caseId)).thenReturn(false);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenRequestException.class, () -> courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope));
    }

    @Test
    public void shouldCallQueryViewWhenUserInProsecutorRoleForTheCaseId() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_NAME);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(courtDocumentQueryView.searchCourtDocumentsForProsecution(any())).thenReturn(envelope);
        when(defenceQueryService.isUserProsecutingCase(envelope, caseId)).thenReturn(true);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope);

        verify(courtDocumentQueryView).searchCourtDocumentsForProsecution(jsonEnvelopeArgumentCaptor.capture());

        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo(COURT_DOCUMENTS_SEARCH_NAME));
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonString(CASE_ID).getString(), is(caseId));
    }

    @Test
    public void shouldCallQueryViewWhenNonCPSUserInProsecutorRoleForTheCaseId() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_NAME);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(courtDocumentQueryView.searchCourtDocumentsForProsecution(any())).thenReturn(envelope);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMatch"));

        courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope);

        verify(courtDocumentQueryView).searchCourtDocumentsForProsecution(jsonEnvelopeArgumentCaptor.capture());

        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo(COURT_DOCUMENTS_SEARCH_NAME));
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonString(CASE_ID).getString(), is(caseId));
    }

    @Test
    public void shouldCallQueryViewWhenUserInProsecutorRoleForTheCaseIdAndApplicationId() {

        final String caseId = randomUUID().toString();
        final String applicationId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(APPLICATION_ID, applicationId)
                .build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(courtDocumentQueryView.searchCourtDocumentsForProsecution(any())).thenReturn(envelope);
        when(defenceQueryService.isUserProsecutingCase(envelope, caseId)).thenReturn(true);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        courtDocumentQueryApi.searchCourtDocumentsForProsecution(envelope);

        verify(courtDocumentQueryView).searchCourtDocumentsForProsecution(jsonEnvelopeArgumentCaptor.capture());

        assertThat(jsonEnvelopeArgumentCaptor.getValue().metadata().name(), equalTo(COURT_DOCUMENTS_SEARCH_NAME));
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonObject(CASE_ID), nullValue());
        assertThat(((JsonObject)jsonEnvelopeArgumentCaptor.getValue().payload()).getJsonString(APPLICATION_ID).getString(), is(applicationId));
    }

    @Test
    public void shouldThrowBadRequestExceptionWhenNoCaseIdQueryParamInGetMaterialByIdForProsecution() {

        final JsonObject jsonObjectPayload = createObjectBuilder().build();
        final Metadata metadata = QueryClientTestBase.metadataFor(MATERIAL_CONTENT_FOR_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        assertThrows(BadRequestException.class, () -> courtDocumentApi.getCaseDocumentDetailsForProsecution(envelope));
    }

    @Test
    public void shouldThrowForbiddenRequestExceptionWhenGetMaterialByIdAndUserNotInProsecutorRoleForTheCaseId() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(MATERIAL_CONTENT_FOR_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(defenceQueryService.isUserProsecutingCase(envelope, caseId)).thenReturn(false);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        assertThrows(ForbiddenRequestException.class, () -> courtDocumentApi.getCaseDocumentDetailsForProsecution(envelope));
    }

    @Test
    public void shouldCallQueryViewWhenGetMaterialByIdAndUserInProsecutorRoleForTheCaseId() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(defenceQueryService.isUserProsecutingCase(envelope, caseId)).thenReturn(true);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.empty());

        JsonEnvelope response = courtDocumentApi.getCaseDocumentDetailsForProsecution(envelope);

        assertThat(response, equalTo(envelope));
    }

    @Test
    public void shouldCallQueryViewWhenGetMaterialByIdAndUserInProsecutorRoleForTheCaseIdForNonCps() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMatch"));

        JsonEnvelope response = courtDocumentApi.getCaseDocumentDetailsForProsecution(envelope);

        assertThat(response, equalTo(envelope));
    }

    @Test
    public void shouldCallQueryViewWhenGetMaterialByIdAndUserInProsecutorRoleForTheCaseIdForNonCpsWithProsecutorObjectExists() {

        String caseId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder().add(CASE_ID, caseId).build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponseWithProsecutor());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMatch"));
        when(prosecutionCaseQuery.getProsecutionCase(any())).thenReturn(createQueryResponse());
        when(usersGroupQueryService.validateNonCPSUserOrg(any(),any(),any(),any())).thenReturn(Optional.of("OrganisationMatch"));

        JsonEnvelope response = courtDocumentApi.getCaseDocumentDetailsForProsecution(envelope);

        assertThat(response, equalTo(envelope));
    }

    @Test
    public void shouldFetchCaseDocumentDetailsForDefence() {
        String applicationId = randomUUID().toString();
        final JsonObject jsonObjectPayload = createObjectBuilder()
                .add(APPLICATION_ID, applicationId)
                .add(DEFENDANT_ID, randomUUID().toString()).build();
        final Metadata metadata = QueryClientTestBase.metadataFor("progression.query.material-content-for-defence");
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);

        JsonEnvelope response = courtDocumentApi.getCaseDocumentDetailsForDefence(envelope);

        assertThat(response, equalTo(envelope));
    }

    private JsonEnvelope createQueryResponse(){
        final JsonObject jsonObjectPayload = createObjectBuilder()
                .add("prosecutionCase", createObjectBuilder()
                        .add("prosecutionCaseIdentifier",createObjectBuilder()
                                .add("prosecutionAuthorityCode","DVLA").build())
                        .build())
                .build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        return JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
    }

    private JsonEnvelope createQueryResponseWithProsecutor() {
        final JsonObject jsonObjectPayload = createObjectBuilder()
                .add("prosecutionCase", createObjectBuilder().add("prosecutionCaseIdentifier",
                                createObjectBuilder().add("prosecutionAuthorityCode", "DVLA1").build())
                        .build())
                .add("prosecutor", createObjectBuilder().add("prosecutorCode", "DVLA"))

                .build();
        final Metadata metadata = QueryClientTestBase.metadataFor(COURT_DOCUMENTS_SEARCH_PROSECUTION);
        return JsonEnvelope.envelopeFrom(metadata, jsonObjectPayload);
    }

}
