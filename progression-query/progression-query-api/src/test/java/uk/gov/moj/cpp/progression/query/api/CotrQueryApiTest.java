package uk.gov.moj.cpp.progression.query.api;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.progression.query.CotrDefendant;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.progression.query.CotrDetails;
import uk.gov.justice.progression.query.TrialReadinessHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.api.service.CotrQueryApiService;
import uk.gov.moj.cpp.progression.query.api.service.DefenceService;
import uk.gov.moj.cpp.progression.query.api.service.ListingService;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CotrQueryApiTest {

    @Mock
    private DefenceService defenceService;

    @Mock
    private ListingService listingService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private CotrQueryApiService cotrQueryApiService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private CotrQueryApi cotrQueryApi;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldReturnAllTrialHearingsWhenNoRemandStatusAndNoOverDueDirectionsSelected() throws IOException {

        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing.search.hearings.json"));
        final Optional<JsonObject> directionsResponse = Optional.of(getJsonPayload("directionmanagement.query.case-directions-list.json"));
        final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = Arrays.asList(uk.gov.justice.progression.query.CaseDirections.caseDirections()
                .withDirectionTypeId(randomUUID())
                .withStatus("PENDING")
                .build());
        final JsonObject hearing1Json = getHearing("progression.query.hearing1.json");
        final JsonObject hearing2Json = getHearing("progression.query.hearing2.json");

        List<TrialReadinessHearing> trialReadinessHearings = new ArrayList<>();
        final TrialReadinessHearing trialReadinessHearing1 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("82b243f8-c4d3-4790-92ef-6192db00539c"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing1);
        final TrialReadinessHearing trialReadinessHearing2 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing2);

        when(listingService.searchTrialReadiness(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(any(), any())).thenReturn(directionsResponse);
        when(cotrQueryApiService.convertCasesDirections(any())).thenReturn(caseDirections);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("82b243f8-c4d3-4790-92ef-6192db00539c"))).thenReturn(hearing1Json);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))).thenReturn(hearing2Json);
        when(cotrQueryApiService.getTrialReadinessHearing(any(), any())).thenReturn(trialReadinessHearing1);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-trial-readiness").build(),
                createObjectBuilder().build());

        final JsonObject response = cotrQueryApi.searchTrialReadiness(query).payloadAsJsonObject();

        assertThat(response.getJsonArray("trialReadinessHearings").size(), is(2));

    }

    @Test
    public void shouldReturnOneTrialHearingWhenRemandStatusIsSelected() throws IOException {

        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing.search.hearings.json"));
        final Optional<JsonObject> directionsResponse = Optional.of(getJsonPayload("directionmanagement.query.case-directions-list.json"));
        final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = Arrays.asList(uk.gov.justice.progression.query.CaseDirections.caseDirections()
                .withDirectionTypeId(randomUUID())
                .withStatus("PENDING")
                .build());
        final JsonObject hearing1Json = getHearing("progression.query.hearing1.json");
        final JsonObject hearing2Json = getHearing("progression.query.hearing2.json");

        List<TrialReadinessHearing> trialReadinessHearings = new ArrayList<>();
        final TrialReadinessHearing trialReadinessHearing1 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("82b243f8-c4d3-4790-92ef-6192db00539c"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing1);
        final TrialReadinessHearing trialReadinessHearing2 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing2);

        when(listingService.searchTrialReadiness(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(any(), any())).thenReturn(directionsResponse);
        when(cotrQueryApiService.convertCasesDirections(any())).thenReturn(caseDirections);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("82b243f8-c4d3-4790-92ef-6192db00539c"))).thenReturn(hearing1Json);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))).thenReturn(hearing2Json);
        when(cotrQueryApiService.getTrialReadinessHearing(any(), any())).thenReturn(trialReadinessHearing1);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-trial-readiness").build(),
                createObjectBuilder().add("remandStatus", "dd4073b6-22be-3875-9d63-5da286bb3ece").build());

        final JsonObject response = cotrQueryApi.searchTrialReadiness(query).payloadAsJsonObject();

        assertThat(response.getJsonArray("trialReadinessHearings").size(), is(1));
    }

    @Test
    public void shouldReturnTwoTrialHearingsWhenOverDueDirectionsAvailableForHearings() throws IOException {

        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing.search.hearings.json"));
        final Optional<JsonObject> directionsResponse = Optional.of(getJsonPayload("directionmanagement.query.case-directions-list.json"));
        final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = Arrays.asList(uk.gov.justice.progression.query.CaseDirections.caseDirections()
                .withDirectionTypeId(randomUUID())
                .withStatus("OVERDUE")
                .build());
        final JsonObject hearing1Json = getHearing("progression.query.hearing1.json");
        final JsonObject hearing2Json = getHearing("progression.query.hearing2.json");

        List<TrialReadinessHearing> trialReadinessHearings = new ArrayList<>();
        final TrialReadinessHearing trialReadinessHearing1 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("82b243f8-c4d3-4790-92ef-6192db00539c"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing1);
        final TrialReadinessHearing trialReadinessHearing2 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing2);

        when(listingService.searchTrialReadiness(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(any(), any())).thenReturn(directionsResponse);
        when(cotrQueryApiService.convertCasesDirections(any())).thenReturn(caseDirections);
        when(progressionService.getHearing(any(), any(), eq("82b243f8-c4d3-4790-92ef-6192db00539c"))).thenReturn(hearing1Json);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))).thenReturn(hearing2Json);
        when(cotrQueryApiService.getTrialReadinessHearing(any(), any())).thenReturn(trialReadinessHearing1);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-trial-readiness").build(),
                createObjectBuilder().add("trailWithOverdueDirection", "Y").build());

        final JsonObject response = cotrQueryApi.searchTrialReadiness(query).payloadAsJsonObject();

        assertThat(response.getJsonArray("trialReadinessHearings").size(), is(2));

    }

    @Test
    public void shouldReturnTwoTrialHearingsWhenAllTrialsSelected() throws IOException {

        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing.search.hearings.json"));
        final Optional<JsonObject> directionsResponse = Optional.of(getJsonPayload("directionmanagement.query.case-directions-list.json"));
        final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = Arrays.asList(uk.gov.justice.progression.query.CaseDirections.caseDirections()
                .withDirectionTypeId(randomUUID())
                .withStatus("PENDING")
                .build());
        final JsonObject hearing1Json = getHearing("progression.query.hearing1.json");
        final JsonObject hearing2Json = getHearing("progression.query.hearing2.json");

        List<TrialReadinessHearing> trialReadinessHearings = new ArrayList<>();
        final TrialReadinessHearing trialReadinessHearing1 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("82b243f8-c4d3-4790-92ef-6192db00539c"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing1);
        final TrialReadinessHearing trialReadinessHearing2 = TrialReadinessHearing.trialReadinessHearing()
                .withId(UUID.fromString("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))
                .build();
        trialReadinessHearings.add(trialReadinessHearing2);

        when(listingService.searchTrialReadiness(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(any(), any())).thenReturn(directionsResponse);
        when(cotrQueryApiService.convertCasesDirections(any())).thenReturn(caseDirections);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("82b243f8-c4d3-4790-92ef-6192db00539c"))).thenReturn(hearing1Json);
        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq("111d2f19-89a7-4f4f-8d14-a90fd3977be4"))).thenReturn(hearing2Json);
        when(cotrQueryApiService.getTrialReadinessHearing(any(), any())).thenReturn(trialReadinessHearing1);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-trial-readiness").build(),
                createObjectBuilder().add("trailWithOverdueDirection", "N").build());

        final JsonObject response = cotrQueryApi.searchTrialReadiness(query).payloadAsJsonObject();

        assertThat(response.getJsonArray("trialReadinessHearings").size(), is(2));

    }

    @Test
    public void shouldGetTrialReadinessHearingDetails() throws IOException {

        final String hearingId = "82b243f8-c4d3-4790-92ef-6192db00539c";
        final UUID defendantId = UUID.fromString("df73207f-3ced-488a-82a0-3fba79c2ce86");

        final JsonObject hearingJson = getHearing("progression.query.hearing1.json");

        JsonObject defendantIdpcMetadata = createObjectBuilder()
                .add("idpcMetadata", createObjectBuilder()
                        .add("publishedDate", "2022-02-10")
                        .build())
                .build();

        final JsonObject directionsList = getJsonPayload("directionmanagement.query.case-directions-list.json");

        final CotrDetails cotrDetails = CotrDetails.cotrDetails()
                .withCotrDetails(Arrays.asList(CotrDetail.cotrDetail()
                        .withId(randomUUID())
                        .withIsProsecutionServed(true)
                        .withCotrDefendants(Arrays.asList(CotrDefendant.cotrDefendant()
                                .withId(defendantId)
                                .withFirstName("firstname")
                                .withLastName("lastname")
                                .build()))
                        .build()))
                .build();
        final JsonObject cotrDetailsJson = objectToJsonObjectConverter.convert(cotrDetails);

        final JsonObject petsForDefendantPayload = getJsonPayload("progression.query.pets-for-case.json");
        final JsonObject ptphForDefendantPayload = getJsonPayload("progression.query.forms-for-case.json");

        when(progressionService.getHearing(any(), any(JsonEnvelope.class), eq(hearingId))).thenReturn(hearingJson);
        when(defenceService.getIdpcDetailsForDefendant(any(), any(), any())).thenReturn(Optional.of(defendantIdpcMetadata));
        when(cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(any(), any())).thenReturn(Optional.of(directionsList));
        when(cotrQueryApiService.getCotrDetails(any(), any())).thenReturn(Optional.of(cotrDetailsJson));
        when(progressionService.getPetsForCase(any(), any(JsonEnvelope.class), any())).thenReturn(petsForDefendantPayload);
        when(progressionService.getPet(any(), any(JsonEnvelope.class), any())).thenReturn(createObjectBuilder()
                .add("petId", "733d43c3-4e04-464d-a8a7-c96fcc22bc38")
                .add("formId", UUID.randomUUID().toString())
                .add("data", "{ \"firstName\": \"John\", \"lastName\": \"Doe\" }")
                .add("lastUpdated", "2021-01-13T10:15")
                .add("defendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", "df73207f-3ced-488a-82a0-3fba79c2ce86").build()))
                .build());
        when(progressionService.getFormsForCase(any(), any(JsonEnvelope.class), any())).thenReturn(ptphForDefendantPayload);
        when(progressionService.getForm(any(), any(JsonEnvelope.class), any(), any())).thenReturn(createObjectBuilder()
                .add("lastUpdated", "2021-01-13T10:15")
                .add("defendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", "df73207f-3ced-488a-82a0-3fba79c2ce86").build()))
                .build());

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.query.search-trial-readiness").build(),
                createObjectBuilder().add("hearingId", hearingId).build());

        JsonObject response = cotrQueryApi.getTrialReadinessDetails(query).payloadAsJsonObject();

        assertThat(response.getJsonObject("trialSummary").getJsonObject("hearingType").getString("description"), is("Trial"));
        assertThat(response.getJsonArray("petDetails").getJsonObject(0).getString("lastChangeDate"), is("2021-01-13"));
        assertThat(response.getJsonArray("petDetails").getJsonObject(0).getJsonObject("courtCentre").getString("id"), is("f8254db1-1683-483e-afb3-b87fde5a0a26"));
        assertThat(response.getJsonArray("petDetails").getJsonObject(0).getString("data"), is(notNullValue()));
        assertThat(response.getJsonArray("petDetails").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getString("firstName"), is("Harry"));
        assertThat(response.getJsonArray("petDetails").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getString("lastName"), is("Kane Junior"));
        assertThat(response.getJsonArray("ptphDetails").getJsonObject(0).getString("lastChangeDate"), is("2021-01-13"));
        assertThat(response.getJsonArray("ptphDetails").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getString("firstName"), is("Harry"));
        assertThat(response.getJsonArray("ptphDetails").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getString("lastName"), is("Kane Junior"));
        assertThat(response.getJsonArray("idpcAndCaseHistories").getJsonObject(0).getJsonObject("plea"), is(notNullValue()));
        assertThat(response.getJsonArray("idpcAndCaseHistories").getJsonObject(0).getString("idpcServiceDate"), is("2022-02-10"));
        assertThat(response.getJsonArray("defendantCotrDetails").getJsonObject(0), is(notNullValue()));

    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset());
        return Json.createReader(
                new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();
    }

    private JsonObject getHearing(final String resourceName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(resourceName), defaultCharset());
        return stringToJsonObjectConverter.convert(jsonString);
    }
}
