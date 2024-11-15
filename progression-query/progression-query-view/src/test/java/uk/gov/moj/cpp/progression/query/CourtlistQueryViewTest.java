package uk.gov.moj.cpp.progression.query;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.ListingService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtlistQueryViewTest {

    @Mock
    private ListingService listingService;

    @Mock
    private HearingQueryView hearingQueryView;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @InjectMocks
    private CourtlistQueryView courtlistQueryView;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayloadForProsecutionCases() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldNotIncludeGenderAndArrestNumberForBulkCivilCases() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getBulkCivilCasesHearings();
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final ProsecutionCase prosecutionCase = getBulkCivilCasesHearings().get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-for-bulk-civil-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.getString("welshCourtCentreName"), is(expected.getString("welshCourtCentreName")));
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayloadForProsecutionCases_ReadCaseFromProgressionViewStoreWithDifferentListingNumber() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.without.listing.number.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldEnrichUshersListDocumentPayloadForProsecutionCases_ReadCaseFromProgressionViewStoreWithDifferentListingNumber() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case-ushers-list.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.without.listing.number.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases-ushers-list.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
        assertPleaValue(actual, true);
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayload_NoPlea() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.noplea.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.noplea.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertPleaValue(actual, false);
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayload_WithIndicatedGuiltyPlea() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.indicated.guilty.plea.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.indicated.guilty.plea.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertIndicatedPleaValue(actual, true);
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayload_WithNoIndicatedNotGuiltyPlea() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.indicated.not.guilty.plea.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.indicated.not.guilty.plea.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertIndicatedPleaValue(actual, false);
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayloadForProsecutionCasesWhenListingNumberIsNull() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final List<Hearing> hearingList = getHearingsWithoutCase();
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.without.listing.number.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases-without-listing-number.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldEnrichBenchlistDocumentPayloadForCourtApplications() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-court-application.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-court-applications.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldEnrichBenchlistDocumentPayloadForCourtApplications2() throws IOException {
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final Optional<JsonObject> listingResponse = Optional.of(getAndReplaceJsonPayload("listing-hearing-with-court-application-with-restricted-defendant.json", defendantId.toString(), defendantId2.toString()));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all-for-restricted-defendant-publish-hearing.json", defendantId.toString(), defendantId2.toString());
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getAndReplaceJsonPayload("courtlist-expected-with-court-applications-with-restricted-defendant.json",  defendantId.toString(), defendantId2.toString());
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldEnrichCourtlistDocumentPayloadForProsecutionCases_WhenHearingISPresentInListingButMissingInProgression() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case-oneHearing-missing-in-progression.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.without.listing.number.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                Json.createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
        assertPleaValue(actual, true);
    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset());
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();
    }

    private List<Hearing> getHearings(final String resourceName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(resourceName), defaultCharset());
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private List<Hearing> getHearings(final String resourceName, final String defId, final String defId2) throws IOException {
        final String jsonString = getStringFromResourceAndReplaceValues(resourceName, defId, defId2);
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private JsonObject getAndReplaceJsonPayload(final String fileName, final String defId, final String defId2) throws IOException {
        final String jsonString = getStringFromResourceAndReplaceValues(fileName, defId, defId2);
        return Json.createReader(new ByteArrayInputStream(jsonString.getBytes())).readObject();
    }

    private String getStringFromResourceAndReplaceValues(final String fileName, final String defId, final String defId2) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset()).replaceAll("DEFENDANT_ID", defId)
                .replaceAll("DEF_ID2", defId2);
        return jsonString;
    }

    private List<Hearing> getBulkCivilCasesHearings() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("courtlists.hearings.repository.bulk.civil.cases.json"), defaultCharset());
        return Json.createReader(
                new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private List<Hearing> getHearingsWithoutCase() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("courtlists.hearings.repository.without.case.json"), defaultCharset());
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private void assertPleaValue(final JsonObject actual, final boolean present) {
        final JsonObject plea = actual.getJsonArray("hearingDates").getJsonObject(0).getJsonArray("courtRooms").getJsonObject(0).getJsonArray("timeslots").getJsonObject(0).getJsonArray("hearings")
                .getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0);
        if (present) {
            assertThat(plea.getString("plea"), is("GUILTY"));
            assertThat(plea.getString("maxPenalty"), is("Max Penalty"));
        } else {
            assertThat(plea.containsKey("plea"), is(false));
        }
    }

    private void assertIndicatedPleaValue(final JsonObject actual, final boolean present) {
        final JsonObject plea = actual.getJsonArray("hearingDates").getJsonObject(0).getJsonArray("courtRooms").getJsonObject(0).getJsonArray("timeslots").getJsonObject(0).getJsonArray("hearings")
                .getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0);
        if (present) {
            assertThat(plea.getString("plea"), is("INDICATED_GUILTY"));
            assertThat(plea.getString("pleaDate"), is("2020-01-01"));
        } else {
            assertThat(plea.containsKey("plea"), is(false));
        }
    }
}
