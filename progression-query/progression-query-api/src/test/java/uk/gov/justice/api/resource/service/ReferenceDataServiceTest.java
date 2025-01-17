package uk.gov.justice.api.resource.service;

import static java.util.Objects.nonNull;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.utils.FileUtil;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_RESULT_DEFINITIONS = "resultDefinitions";
    private static final String FIELD_PLEA_TYPE_DESCRIPTION = "pleaTypeDescription";
    private static final String FIELD_PLEA_VALUE = "pleaValue";
    private static final String PLEA_VALUE_1 = "pleaValue1";
    private static final String PLEA_VALUE_2 = "pleaValue2";
    private static final String PLEA_DESC_1 = "pleaDesc1";
    private static final String PLEA_DESC_2 = "pleaDesc2";
    private static final String FIELD_JUDICIARIES = "judiciaries";
    private static final String JUDICIARY_VALUE_1 = "judiciaryValue1";
    private static final String JUDICIARY_DESC_1 = "judiciaryDesc1";
    private static final String FIELD_CLUSTER_ID = "clusterId";
    private static final String FIELD_ORGGANISATION_UNITS = "organisationUnits";
    private static final String FIELD_ID = "id";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_RESULT_DEFINITION_GROUP = "resultDefinitionGroup";
    public static final String LAVENDER_HILL_MAGISTRATES_COURT = "Lavender Hill Magistrates' Court";

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    private JsonEnvelope jsonEnvelope;


    @Test
    public void shouldRetrievePleaStatusTypeDescriptions() {
        final Envelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("name").build(), buildPleaStatusTypesPayload());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final Map<String, String> actual = referenceDataService.retrievePleaTypeDescriptions();
        assertThat(actual.get(PLEA_VALUE_1), is(PLEA_DESC_1));
        assertThat(actual.get(PLEA_VALUE_2), is(PLEA_DESC_2));
    }

    @Test
    public void shouldRetrieveJudiciaries() {
        final Envelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(), buildJudiciariesPayload());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final Optional<JsonObject> actual = referenceDataService.getJudiciary(UUID.randomUUID());
        assertThat(actual.get().getString(JUDICIARY_VALUE_1), equalTo(JUDICIARY_DESC_1));
    }

    @Test
    public void shouldRetrieveCourtCentreIdsByClusterId() {
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(), buildClusterOrganisationPayload());
        when(requester.request(any())).thenReturn(envelope);
        final JsonEnvelope actual = referenceDataService.getCourtCentreIdsByClusterId(UUID.randomUUID());
        assertThat(actual.payloadAsJsonObject().getJsonArray(FIELD_ORGGANISATION_UNITS).size(), equalTo(1));
    }

    @Test
    public void shouldgetCourtCenterDataByCourtName() {
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(),
                FileUtil.jsonFromPath("stub-data/referencedata.query.ou.courtrooms.ou-courtroom-name.json"));
        when(requester.request(any())).thenReturn(envelope);
        final Optional<JsonObject> actual = referenceDataService.getCourtCenterDataByCourtName(envelope, LAVENDER_HILL_MAGISTRATES_COURT);

        final JsonObject expectedJson = createObjectBuilder()
                .add("ouCourtRoomName", LAVENDER_HILL_MAGISTRATES_COURT)
                .build();


        verify(requester).request(envelopeArgumentCaptor.capture());
        final JsonEnvelope envelopeCapture = envelopeArgumentCaptor.getValue();
        assertEquals("referencedata.query.ou.courtrooms.ou-courtroom-name", envelopeCapture.metadata().name());
        assertEquals(expectedJson, envelopeCapture.payloadAsJsonObject());

        assertThat(actual.get().getString("id"), equalTo("f8254db1-1683-483e-afb3-b87fde5a0a26"));
        assertThat(actual.get().getString("oucode"), equalTo("B01LY00"));
    }

    @Test
    public void shouldGetOrganisationUnitById() {
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(),
                createObjectBuilder().build());
        when(requester.request(any())).thenReturn(envelope);
        final Optional<JsonObject> actual = referenceDataService.getOrganisationUnitById(envelope, UUID.randomUUID());

        verify(requester).request(envelopeArgumentCaptor.capture());
        final JsonEnvelope envelopeCapture = envelopeArgumentCaptor.getValue();
        assertEquals("referencedata.query.organisation-unit.v2", envelopeCapture.metadata().name());
    }

    @Test
    public void shouldReturnEmptyJsonWhenNoJudiciariesInReferenceData() {
        final Envelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(),
                createObjectBuilder().add(FIELD_JUDICIARIES, createArrayBuilder()).build());

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);

        final Optional<JsonObject> actual = referenceDataService.getJudiciary(UUID.randomUUID());

        assertThat(actual.isPresent(), equalTo(false));
    }

    @Test
    public void shouldgetResultDefinitionsByIdsWithCategoryAndGroup() {
        final UUID rdId = UUID.randomUUID();
        final List<UUID> resultDefinitionIdList = List.of(rdId);
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(),
                buildResultDefinitionByIdsResponse(rdId, "I", "CommittedToCC"));
        when(requester.request(any())).thenReturn(envelope);
        final List<ResultDefinition> resultDefinitionsByIdsResponse = referenceDataService.getResultDefinitionsByIds(envelope, resultDefinitionIdList);

        final JsonObject expectedJson = createObjectBuilder()
                .add("ids", rdId.toString())
                .build();

        verify(requester).request(envelopeArgumentCaptor.capture());
        final JsonEnvelope envelopeCapture = envelopeArgumentCaptor.getValue();
        assertEquals("referencedata.query-result-definitions-by-ids", envelopeCapture.metadata().name());
        assertEquals(expectedJson, envelopeCapture.payloadAsJsonObject());

        assertThat(resultDefinitionsByIdsResponse.get(0).getId().toString(), equalTo(rdId.toString()));
        assertThat(resultDefinitionsByIdsResponse.get(0).getCategory(), equalTo("I"));
        assertThat(resultDefinitionsByIdsResponse.get(0).getResultDefinitionGroup(), equalTo("CommittedToCC"));
    }

    @Test
    public void shouldgetResultDefinitionsByIdsWithCategoryAndNoGroup() {
        final UUID rdId = UUID.randomUUID();
        final List<UUID> resultDefinitionIdList = List.of(rdId);
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(),
                buildResultDefinitionByIdsResponse(rdId, "A", null));
        when(requester.request(any())).thenReturn(envelope);
        final List<ResultDefinition> resultDefinitionsByIdsResponse = referenceDataService.getResultDefinitionsByIds(envelope, resultDefinitionIdList);

        final JsonObject expectedJson = createObjectBuilder()
                .add("ids", rdId.toString())
                .build();

        verify(requester).request(envelopeArgumentCaptor.capture());
        final JsonEnvelope envelopeCapture = envelopeArgumentCaptor.getValue();
        assertEquals("referencedata.query-result-definitions-by-ids", envelopeCapture.metadata().name());
        assertEquals(expectedJson, envelopeCapture.payloadAsJsonObject());

        assertThat(resultDefinitionsByIdsResponse.get(0).getId().toString(), equalTo(rdId.toString()));
        assertThat(resultDefinitionsByIdsResponse.get(0).getCategory(), equalTo("A"));
        assertThat(resultDefinitionsByIdsResponse.get(0).getResultDefinitionGroup(), nullValue());
    }

    private JsonObject buildPleaStatusTypesPayload() {
        return createObjectBuilder().add(FIELD_PLEA_STATUS_TYPES, createArrayBuilder()
                        .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_1).add(FIELD_PLEA_TYPE_DESCRIPTION, PLEA_DESC_1))
                        .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_2).add(FIELD_PLEA_TYPE_DESCRIPTION, PLEA_DESC_2)))
                .build();
    }

    private JsonObject buildJudiciariesPayload() {
        return createObjectBuilder().add(FIELD_JUDICIARIES, createArrayBuilder()
                        .add(createObjectBuilder().add(JUDICIARY_VALUE_1, JUDICIARY_DESC_1)))
                .build();
    }

    private JsonObject buildClusterOrganisationPayload() {
        return createObjectBuilder().add(FIELD_CLUSTER_ID, "53b3c80f-57ea-3915-8b2d-457291d94d9a")
                .add(FIELD_ORGGANISATION_UNITS, createArrayBuilder().add(createObjectBuilder().add("ouId", "2608ebcc-d643-4260-8175-b8a24ac5cae5")))
                .build();
    }

    private JsonValue buildResultDefinitionByIdsResponse(final UUID id, final String category, final String resultDefinitionGroup) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add(FIELD_ID, id.toString()).add(FIELD_CATEGORY, category);
        if (nonNull(resultDefinitionGroup)) {
            objectBuilder.add(FIELD_RESULT_DEFINITION_GROUP, resultDefinitionGroup);
        }

        return createObjectBuilder()
                .add(FIELD_RESULT_DEFINITIONS, createArrayBuilder().add(objectBuilder.build())).build();
    }

}
