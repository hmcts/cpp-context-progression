package uk.gov.justice.api.resource.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
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

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    @Test
    public void shouldRetrievePleaStatusTypeDescriptions(){
        final Envelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("name").build(), buildPleaStatusTypesPayload());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final Map<String, String> actual = referenceDataService.retrievePleaTypeDescriptions();
        assertThat(actual.get(PLEA_VALUE_1), is(PLEA_DESC_1));
        assertThat(actual.get(PLEA_VALUE_2), is(PLEA_DESC_2));
    }

    @Test
    public void shouldRetrieveJudiciaries(){
        final Envelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(), buildJudiciariesPayload());
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final Optional<JsonObject> actual = referenceDataService.getJudiciary(UUID.randomUUID());
        assertThat(actual.get().getString(JUDICIARY_VALUE_1), equalTo(JUDICIARY_DESC_1));
    }

    @Test
    public void shouldRetrieveCourtCentreIdsByClusterId(){
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(UUID.randomUUID()).withName("ids").build(), buildClusterOrganisationPayload());
        when(requester.request(any())).thenReturn(envelope);
        final JsonEnvelope actual = referenceDataService.getCourtCentreIdsByClusterId(UUID.randomUUID());
        assertThat(actual.payloadAsJsonObject().getJsonArray(FIELD_ORGGANISATION_UNITS).size(), equalTo(1));
    }

    private JsonObject buildPleaStatusTypesPayload(){
        return createObjectBuilder().add(FIELD_PLEA_STATUS_TYPES, createArrayBuilder()
                .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_1).add(FIELD_PLEA_TYPE_DESCRIPTION, PLEA_DESC_1))
                .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_2).add(FIELD_PLEA_TYPE_DESCRIPTION, PLEA_DESC_2)))
                .build();
    }
    private JsonObject buildJudiciariesPayload(){
        return createObjectBuilder().add(FIELD_JUDICIARIES, createArrayBuilder()
                .add(createObjectBuilder().add(JUDICIARY_VALUE_1, JUDICIARY_DESC_1)))
                .build();
    }

    private JsonObject buildClusterOrganisationPayload(){
        return createObjectBuilder().add(FIELD_CLUSTER_ID, "53b3c80f-57ea-3915-8b2d-457291d94d9a")
                        .add(FIELD_ORGGANISATION_UNITS, createArrayBuilder().add(createObjectBuilder().add("ouId", "2608ebcc-d643-4260-8175-b8a24ac5cae5")))
                .build();
    }
}
