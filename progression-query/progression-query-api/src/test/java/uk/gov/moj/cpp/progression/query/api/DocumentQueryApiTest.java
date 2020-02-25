package uk.gov.moj.cpp.progression.query.api;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DocumentQueryApiTest {
    @InjectMocks
    private final CourtDocumentQueryApi target = new CourtDocumentQueryApi();
    @Mock
    private JsonEnvelope query;
    @Mock
    private JsonEnvelope response;
    @Mock
    private UserDetailsLoader userDetailsLoader;
    @Mock
    private HearingDetailsLoader hearingDetailsLoader;
    @Mock
    private ReferenceDataService referenceDataService;
    @Mock
    private Requester requester;
    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    private final UUID HEARING_TYPE_ID_FOR_TRIAL = UUID.fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");
    private final UUID HEARING_TYPE_ID_FOR_NON_TRIAL = UUID.fromString("39031052-42ff-4277-9f16-dfa30e9246b3");
    private final UUID HEARING_TYPE_ID_FOR_TRIAL_OF_ISSUE = UUID.fromString("9cc41e45-b594-4ba6-906e-1a4626b08fed");

    private static JsonObject buildCourtDocumentJsonObject() {

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentTypeRBAC", buildDocumentTypeDataWithRBAC()))
                        .build();

        return courtDocument;
    }

    private static JsonObject buildDocumentTypeDataWithRBAC() {
        return Json.createObjectBuilder()
                .add("documentAccess", Json.createArrayBuilder().add("Listing Officer"))
                .add("canCreateUserGroups", Json.createArrayBuilder().add("Listing Officer"))
                .add("canReadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .add("canDownloadUserGroups", Json.createArrayBuilder().add("Listing Officer").add("Magistrates"))
                .build();
    }

    private static JsonObject buildHearingTypeListJsonObject() {

        final JsonObject hearingTypeList = Json.createObjectBuilder()
                .add("hearingTypes", Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", "06b0c2bf-3f98-46ed-ab7e-56efaf9ecced")
                                        .add("hearingCode", "TIS")
                                        .add("hearingDescription", "Trial")
                                        .build())
                                .add(createObjectBuilder()
                                        .add("id", "9cc41e45-b594-4ba6-906e-1a4626b08fed")
                                        .add("hearingCode", "TRL")
                                        .add("hearingDescription", "Trial of Issue")
                                        .build())
                                .add(createObjectBuilder()
                                        .add("id", "39031052-42ff-4277-9f16-dfa30e9246b3")
                                        .add("hearingCode", "FPTP")
                                        .add("hearingDescription", "Further Plea & Trial Preparation")
                                        .build())
                            .build()
                        ).build();

        return hearingTypeList;
    }

    @Test
    public void shouldHandleCourtDocument() {
        final JsonObject courtDocumentPayload = buildCourtDocumentJsonObject();

        final JsonEnvelope response = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(CourtDocumentQueryApi.COURT_DOCUMENT_SEARCH_NAME),
                courtDocumentPayload);

        when(requester.request(query)).thenReturn(response);
        assertThat(target.getCourtDocument(query), equalTo(response));
        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());

    }

    @Test
    public void shouldHandleSearchCourtDocumentsQuery() {
        when(query.metadata()).thenReturn(MetadataBuilderFactory.metadataWithDefaults().withName("progression.query.courtdocuments").build());
        when(query.payloadAsJsonObject()).thenReturn(createObjectBuilder().build());
        when(requester.request(query)).thenReturn(response);

        assertThat(target.searchCourtDocuments(query), equalTo(response));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(jsonEnvelope.metadata().name(), equalTo("progression.query.courtdocuments"));
    }

    @Test
    public void shouldNotGetSharedCourtDocumentsForMagsUserNotPartOfHearing() {
        final UUID magsUserId = randomUUID();
        final UUID hearingId = randomUUID();

        final HearingDetails trialHearingDetailsWithNoMags = new HearingDetails();
        trialHearingDetailsWithNoMags.setType("Trial");
        trialHearingDetailsWithNoMags.addUserId(randomUUID());

        when(userDetailsLoader.getGroupsUserBelongsTo(requester, magsUserId)).thenReturn(singletonList(new UserGroupsDetails(randomUUID(), "Magistrates")));
        when(hearingDetailsLoader.getHearingDetails(requester, hearingId)).thenReturn(trialHearingDetailsWithNoMags);
        when(query.metadata()).thenReturn(MetadataBuilderFactory.metadataWithDefaults().withUserId(magsUserId.toString()).withName("progression.query.courtdocuments").build());
        when(query.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearingId", hearingId.toString()).build());
        when(requester.request(query)).thenReturn(response);

        assertThat(target.searchCourtDocuments(query), equalTo(response));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), equalTo("progression.query.courtdocuments"));
    }

    private Map<UUID, ReferenceDataService.ReferenceHearingDetails> getMochHearingTypes() {

        final JsonObject hearingTypes = buildHearingTypeListJsonObject();
        final JsonArray hearingTypesJsonArray = hearingTypes.getJsonArray("hearingTypes");
        Map<UUID, ReferenceDataService.ReferenceHearingDetails> referenceHearingTypeDetails = new HashMap<>();
        for(int i = 0; i < hearingTypesJsonArray.size(); i++) {
            final ReferenceDataService.ReferenceHearingDetails referenceHearingDetails = convertToHearingDetais(hearingTypesJsonArray.getJsonObject(i));
            referenceHearingTypeDetails.put(referenceHearingDetails.getHearingTypeId(), referenceHearingDetails);
        }
        return referenceHearingTypeDetails;
    }

    private ReferenceDataService.ReferenceHearingDetails convertToHearingDetais(final JsonObject hearingType) {
        return new ReferenceDataService.ReferenceHearingDetails(UUID.fromString(hearingType.getString("id")), hearingType.getString("hearingCode"), hearingType.getString("hearingDescription"));
    }

    @Test
    public void shouldGetSharedCourtDocumentsForTrialHearingAndMagistratesUserWhoBelongsToHearing() {
        final UUID magsUserId = randomUUID();
        final UUID magsGroupId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final HearingDetails trialHearingDetails = new HearingDetails();
        trialHearingDetails.setType("Trial");
        trialHearingDetails.addUserId(magsUserId);
        trialHearingDetails.setHearingTypeId(HEARING_TYPE_ID_FOR_TRIAL);

        when(referenceDataService.getHearingTypes(any())).thenReturn(getMochHearingTypes());
        when(userDetailsLoader.getGroupsUserBelongsTo(requester, magsUserId)).thenReturn(singletonList(new UserGroupsDetails(magsGroupId, "Magistrates")));
        when(hearingDetailsLoader.getHearingDetails(requester, hearingId)).thenReturn(trialHearingDetails);
        when(query.metadata()).thenReturn(MetadataBuilderFactory.metadataWithDefaults().withUserId(magsUserId.toString()).withName("progression.query.courtdocuments").build());
        when(query.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearingId", hearingId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString()).build());
        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);


        assertThat(target.searchCourtDocuments(query), equalTo(response));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("userGroupId"), is(true));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("userGroupId"), is(magsGroupId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("hearingId"), is(true));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonEnvelope.metadata().name(), equalTo("progression.query.shared-court-documents"));

    }

    @Test
    public void shouldGetSharedCourtDocumentsForTrialofIssueHearingAndMagistratesUserWhoBelongsToHearing() {
        final UUID magsUserId = randomUUID();
        final UUID magsGroupId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final HearingDetails trialHearingDetails = new HearingDetails();
        trialHearingDetails.setType("Trial of Issue");
        trialHearingDetails.addUserId(magsUserId);
        trialHearingDetails.setHearingTypeId(HEARING_TYPE_ID_FOR_TRIAL_OF_ISSUE);

        when(referenceDataService.getHearingTypes(any())).thenReturn(getMochHearingTypes());
        when(userDetailsLoader.getGroupsUserBelongsTo(requester, magsUserId)).thenReturn(singletonList(new UserGroupsDetails(magsGroupId, "Magistrates")));
        when(hearingDetailsLoader.getHearingDetails(requester, hearingId)).thenReturn(trialHearingDetails);
        when(query.metadata()).thenReturn(MetadataBuilderFactory.metadataWithDefaults().withUserId(magsUserId.toString()).withName("progression.query.courtdocuments").build());
        when(query.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearingId", hearingId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString()).build());
        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);


        assertThat(target.searchCourtDocuments(query), equalTo(response));

        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();

        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("userGroupId"), is(true));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("userGroupId"), is(magsGroupId.toString()));
        assertThat(jsonEnvelope.payloadAsJsonObject().containsKey("hearingId"), is(true));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonEnvelope.metadata().name(), equalTo("progression.query.shared-court-documents"));

    }

    @Test
    public void shouldNotGetSharedCourtDocumentsForNonTrialHearing() {
        final UUID magsUserId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final HearingDetails nonTrialHearingDetails = new HearingDetails();
        nonTrialHearingDetails.addUserId(magsUserId);
        nonTrialHearingDetails.setType("Not trial");
        nonTrialHearingDetails.setHearingTypeId(HEARING_TYPE_ID_FOR_NON_TRIAL);

        when(referenceDataService.getHearingTypes(any())).thenReturn(getMochHearingTypes());
        when(userDetailsLoader.getGroupsUserBelongsTo(requester, magsUserId)).thenReturn(singletonList(new UserGroupsDetails(randomUUID(), "Magistrates")));
        when(hearingDetailsLoader.getHearingDetails(requester, hearingId)).thenReturn(nonTrialHearingDetails);
        when(query.metadata()).thenReturn(MetadataBuilderFactory.metadataWithDefaults().withUserId(magsUserId.toString()).withName("progression.query.courtdocuments").build());
        when(query.payloadAsJsonObject()).thenReturn(createObjectBuilder().add("hearingId", hearingId.toString()).add("caseId", caseId.toString()).add("defendantId", defendantId.toString()).build());
        when(requester.request(query)).thenReturn(response);

        assertThat(target.searchCourtDocuments(query), equalTo(response));


        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), equalTo("progression.query.courtdocuments"));
    }


}
