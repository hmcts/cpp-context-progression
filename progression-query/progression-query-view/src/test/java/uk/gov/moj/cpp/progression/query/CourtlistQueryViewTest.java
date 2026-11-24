package uk.gov.moj.cpp.progression.query;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
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
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
                createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));

        final JsonObject defendant = actual.getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.getString("pncId"), is("1234567"));
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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

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
                createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases-without-listing-number.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    @Disabled("Test failing due to date of birth")
    public void shouldEnrichBenchlistDocumentPayloadForCourtApplications() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-court-application.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-court-applications.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    @Disabled("Test failing due to date of birth")
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
                createObjectBuilder().build());

        final JsonObject expected = getAndReplaceJsonPayload("courtlist-expected-with-court-applications-with-restricted-defendant.json",  defendantId.toString(), defendantId2.toString());
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldReturnEmptyPayloadWhenListingServiceReturnsEmpty() {
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(Optional.empty());

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        assertThat(actual.isEmpty(), is(true));
    }

    @Test
    public void searchPrisonCourtlistShouldDelegateToSearchCourtlist() throws IOException {
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
                        .withName("progression.search.prison.court.list").build(),
                createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchPrisonCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldReturnPayloadWithoutEnrichmentWhenHearingDatesIsEmpty() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-empty-hearing-dates.json"));
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        assertThat(actual.getString("listType"), is("public"));
        assertThat(actual.getString("courtCentreName"), is("Test Court"));
        assertThat(actual.getJsonArray("hearingDates").size(), is(0));
    }

    @Test
    public void shouldReturnPayloadWithoutEnrichmentWhenHearingsMapIsEmpty() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(emptyList());

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        assertThat(actual, notNullValue());
        assertThat(actual.containsKey("hearingDates"), is(true));
    }

    @Test
    public void shouldOmitHearingFromOutputWhenHearingIdNotInHearingsMap() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final Hearing hearingWithDifferentId = mock(Hearing.class);
        when(hearingWithDifferentId.getId()).thenReturn(randomUUID());
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(singletonList(hearingWithDifferentId));

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        final int hearingsInFirstSlot = actual.getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").size();
        assertThat(hearingsInFirstSlot, is(0));
    }

    @Test
    public void shouldNotAddLjaInformationWhenCourtCentreIsNull() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-prosecution-case.json"));
        final Hearing hearingWithNullCourtCentre = mock(Hearing.class);
        when(hearingWithNullCourtCentre.getId()).thenReturn(UUID.fromString("82b243f8-c4d3-4790-92ef-6192db00539c"));
        when(hearingWithNullCourtCentre.getCourtCentre()).thenReturn(null);
        when(hearingWithNullCourtCentre.getProsecutionCases()).thenReturn(emptyList());
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(singletonList(hearingWithNullCourtCentre));
        final ProsecutionCase prosecutionCase = getHearings("courtlists.hearings.repository.all.json").get(0).getProsecutionCases().get(0);
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        assertThat(actual.containsKey("ljaCode"), is(false));
        assertThat(actual.containsKey("ljaName"), is(false));
    }

    @Test
    public void shouldAddLjaInformationIncludingWelshLjaNameWhenCourtCentreHasLjaDetails() throws IOException {
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
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        assertThat(actual.containsKey("ljaCode"), is(true));
        assertThat(actual.getString("ljaCode"), is("2577"));
        assertThat(actual.containsKey("ljaName"), is(true));
        assertThat(actual.getString("ljaName"), is("South West London Magistrates' Court"));
        assertThat(actual.containsKey("welshLjaName"), is(true));
        assertThat(actual.getString("welshLjaName"), is("East Hampshire Magistrates' Court"));
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
                createObjectBuilder().build());

        final JsonObject expected = getJsonPayload("courtlist-expected-with-prosecution-cases.json");
        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();
        assertThat(actual, is(expected));
        assertPleaValue(actual, true);
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(final String methodName, final Class<?>[] paramTypes, final Object... args) throws Exception {
        final Method method = CourtlistQueryView.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return (T) method.invoke(courtlistQueryView, args);
    }

    @Test
    public void getApplicationOffenceListingNumbers_shouldReturnEmptyListWhenNoApplicationOffences() throws Exception {
        final JsonObject hearingJson = createObjectBuilder().build();
        final List<UUID> result = invokePrivateMethod("getApplicationOffenceListingNumbers", new Class<?>[]{JsonObject.class}, hearingJson);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void getApplicationOffenceListingNumbers_shouldReturnOffenceIdsWhenApplicationOffencesPresent() throws Exception {
        final String id1 = "072319bf-73c2-41b5-b309-c8c86c9b077b";
        final String id2 = "651fc68b-8b9f-4cf2-912a-0b55d536323c";
        final JsonObject hearingJson = createObjectBuilder()
                .add("applicationOffences", createArrayBuilder()
                        .add(createObjectBuilder().add("id", id1).build())
                        .add(createObjectBuilder().add("id", id2).build())
                        .build())
                .build();
        final List<UUID> result = invokePrivateMethod("getApplicationOffenceListingNumbers", new Class<?>[]{JsonObject.class}, hearingJson);
        assertThat(result.size(), is(2));
        assertThat(result.contains(fromString(id1)), is(true));
        assertThat(result.contains(fromString(id2)), is(true));
    }

    @Test
    public void addWelshOffenceTitleFromListingIfMissing_shouldUseListingWelshTitleWhenProgressionHasNone() throws Exception {
        final JsonObjectBuilder offenceBuilder = createObjectBuilder();
        final Offence offenceWithoutWelsh = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("TTH105HY")
                .withOffenceTitle("ROBBERY")
                .build();
        final JsonObject offenceFromListing = createObjectBuilder()
                .add("welshOffenceTitle", "Listing Welsh Title")
                .build();

        invokePrivateMethod("addWelshOffenceTitleFromListingIfMissing",
                new Class<?>[]{JsonObjectBuilder.class, Offence.class, JsonObject.class},
                offenceBuilder, offenceWithoutWelsh, offenceFromListing);

        final JsonObject result = offenceBuilder.build();
        assertThat(result.getString("welshOffenceTitle"), is("Listing Welsh Title"));
    }

    @Test
    public void addWelshOffenceTitleFromListingIfMissing_shouldNotAddWhenProgressionAlreadyHasWelshTitle() throws Exception {
        final JsonObjectBuilder offenceBuilder = createObjectBuilder();
        final Offence offenceWithWelsh = Offence.offence()
                .withId(randomUUID())
                .withOffenceCode("TTH105HY")
                .withOffenceTitle("ROBBERY")
                .withOffenceTitleWelsh("Progression Welsh Title")
                .build();
        final JsonObject offenceFromListing = createObjectBuilder()
                .add("welshOffenceTitle", "Listing Welsh Title")
                .build();

        invokePrivateMethod("addWelshOffenceTitleFromListingIfMissing",
                new Class<?>[]{JsonObjectBuilder.class, Offence.class, JsonObject.class},
                offenceBuilder, offenceWithWelsh, offenceFromListing);

        final JsonObject result = offenceBuilder.build();
        assertThat(result.containsKey("welshOffenceTitle"), is(false));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromProsecutingAuthority() throws Exception {
        final ProsecutingAuthority pa = ProsecutingAuthority.prosecutingAuthority()
                .withName("CPS London")
                .build();
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(pa)
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("CPS London"));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromProsecutionAuthorityCodeWhenNameNull() throws Exception {
        final ProsecutingAuthority pa = ProsecutingAuthority.prosecutingAuthority()
                .withProsecutionAuthorityCode("CPS01")
                .build();
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withProsecutingAuthority(pa)
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("CPS01"));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromMasterDefendantPerson() throws Exception {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withFirstName("John")
                                .withLastName("Doe")
                                .withDateOfBirth(LocalDate.of(1990, 1, 15))
                                .build())
                        .build())
                .withPncId("PNC-RESP-001")
                .withMasterDefendantId(randomUUID())
                .build();
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(masterDefendant)
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("John Doe"));
        assertThat(result.getString("dateOfBirth"), is("15 Jan 1990"));
        assertThat(result.getString("pncId"), is("PNC-RESP-001"));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromMasterDefendantLegalEntity() throws Exception {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation().withName("Acme Ltd").build())
                        .build())
                .withPncId("PNC-RESP-ORG-001")
                .withMasterDefendantId(randomUUID())
                .build();
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withMasterDefendant(masterDefendant)
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("Acme Ltd"));
        assertThat(result.getString("pncId"), is("PNC-RESP-ORG-001"));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromOrganisation() throws Exception {
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withOrganisation(Organisation.organisation().withName("Transport for London").build())
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("Transport for London"));
    }

    @Test
    public void buildCourtApplicationParty_shouldAddNameFromPersonDetails() throws Exception {
        final CourtApplicationParty party = CourtApplicationParty.courtApplicationParty()
                .withPersonDetails(Person.person()
                        .withFirstName("Jane")
                        .withLastName("Smith")
                        .withDateOfBirth(LocalDate.of(1985, 6, 20))
                        .build())
                .build();
        final JsonObject result = invokePrivateMethod("buildCourtApplicationParty", new Class<?>[]{CourtApplicationParty.class}, party);
        assertThat(result.getString("name"), is("Jane Smith"));
        assertThat(result.getString("dateOfBirth"), is("20 Jun 1985"));
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromProsecutingAuthority() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                                .withName("CPS")
                                .withProsecutionAuthorityCode("CPS01")
                                .build())
                        .build())
                .build();
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("name"), is("CPS"));
        assertThat(result.getJsonArray("reportingRestrictions"), notNullValue());
        assertThat(result.getJsonArray("offences"), notNullValue());
        assertThat(result.containsKey("organisationName"), is(false));
        assertThat(result.containsKey("asn"), is(false));
        assertThat(result.containsKey("gender"), is(false));
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromOrganisation() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation().withName("Acme Organisation").build())
                        .build())
                .build();
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("organisationName"), is("Acme Organisation"));
        assertThat(result.getString("welshOrganisationName"), is(""));
        assertThat(result.containsKey("firstName"), is(false));
        assertThat(result.containsKey("surname"), is(false));
        assertThat(result.containsKey("welshSurname"), is(false));
        assertThat(result.containsKey("dateOfBirth"), is(false));
        assertThat(result.containsKey("age"), is(false));
        assertThat(result.containsKey("nationality"), is(false));
        assertThat(result.containsKey("asn"), is(false));
        assertThat(result.containsKey("gender"), is(false));
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromPersonDetails() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName("Alice")
                                .withLastName("Brown")
                                .withDateOfBirth(LocalDate.of(1992, 3, 10))
                                .withGender(Gender.FEMALE)
                                .build())
                        .build())
                .build();
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("firstName"), is("Alice"));
        assertThat(result.getString("surname"), is("Brown"));
        assertThat(result.getString("dateOfBirth"), is("10 Mar 1992"));
        assertThat(result.containsKey("organisationName"), is(false));
        assertThat(result.containsKey("welshOrganisationName"), is(false));
        assertThat(result.getString("asn"), is(""));
        assertThat(result.getString("gender"), is("FEMALE"));
        assertThat(result.getJsonArray("offences"), notNullValue());
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromMasterDefendantPersonWithOrganisation() throws Exception {
        final Person person = mock(Person.class);
        when(person.getFirstName()).thenReturn("Bob");
        when(person.getLastName()).thenReturn("Wilson");
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(1988, 7, 5));
        when(person.getGender()).thenReturn(Gender.MALE);
        final PersonDefendant personDefendant = mock(PersonDefendant.class);
        when(personDefendant.getPersonDetails()).thenReturn(person);
        when(personDefendant.getArrestSummonsNumber()).thenReturn("APPLICANT-ASN-001");
        final MasterDefendant masterDefendant = mock(MasterDefendant.class);
        when(masterDefendant.getPersonDefendant()).thenReturn(personDefendant);
        when(masterDefendant.getPncId()).thenReturn("APPLICANT-PNC-001");
        final CourtApplicationParty applicant = mock(CourtApplicationParty.class);
        when(applicant.getMasterDefendant()).thenReturn(masterDefendant);
        final CourtApplication courtApplication = mock(CourtApplication.class);
        when(courtApplication.getApplicant()).thenReturn(applicant);
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("firstName"), is("Bob"));
        assertThat(result.getString("surname"), is("Wilson"));
        assertThat(result.getString("dateOfBirth"), is("5 Jul 1988"));
        assertThat(result.getString("asn"), is("APPLICANT-ASN-001"));
        assertThat(result.getString("gender"), is("MALE"));
        assertThat(result.getString("pncId"), is("APPLICANT-PNC-001"));
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromMasterDefendantLegalEntity() throws Exception {
        final Organisation org = mock(Organisation.class);
        when(org.getName()).thenReturn("Corporate Defendant Ltd");
        final LegalEntityDefendant legalEntityDefendant = mock(LegalEntityDefendant.class);
        when(legalEntityDefendant.getOrganisation()).thenReturn(org);
        final MasterDefendant masterDefendant = mock(MasterDefendant.class);
        when(masterDefendant.getPersonDefendant()).thenReturn(null);
        when(masterDefendant.getLegalEntityDefendant()).thenReturn(legalEntityDefendant);
        when(masterDefendant.getPncId()).thenReturn("APPLICANT-ORG-PNC-001");
        final CourtApplicationParty applicant = mock(CourtApplicationParty.class);
        when(applicant.getMasterDefendant()).thenReturn(masterDefendant);
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicant(applicant)
                .build();
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("organisationName"), is("Corporate Defendant Ltd"));
        assertThat(result.containsKey("firstName"), is(false));
        assertThat(result.containsKey("surname"), is(false));
        assertThat(result.containsKey("welshSurname"), is(false));
        assertThat(result.containsKey("dateOfBirth"), is(false));
        assertThat(result.containsKey("age"), is(false));
        assertThat(result.containsKey("nationality"), is(false));
        assertThat(result.containsKey("asn"), is(false));
        assertThat(result.containsKey("gender"), is(false));
        assertThat(result.getString("pncId"), is("APPLICANT-ORG-PNC-001"));
    }

    @Test
    public void buildApplicantForCourtApplication_shouldBuildApplicantFromRepresentationOrganisation() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withRepresentationOrganisation(Organisation.organisation().withName("Solicitors Ltd").build())
                        .build())
                .build();
        final JsonObject result = invokePrivateMethod("buildApplicantForCourtApplication",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, emptyList(), JurisdictionType.MAGISTRATES);
        assertThat(result.getString("name"), is("Solicitors Ltd"));
        assertThat(result.containsKey("organisationName"), is(false));
        assertThat(result.containsKey("asn"), is(false));
        assertThat(result.containsKey("gender"), is(false));
        assertThat(result.getJsonArray("reportingRestrictions"), notNullValue());
        assertThat(result.getJsonArray("offences"), notNullValue());
    }

    @Test
    public void buildApplicantReportingRestrictions_shouldReturnEmptyArrayWhenNoOffencesMatch() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(emptyList())
                .build();
        final JsonArray result = invokePrivateMethod("buildApplicantReportingRestrictions",
                new Class<?>[]{CourtApplication.class, List.class}, courtApplication, singletonList(randomUUID()));
        assertThat(result.size(), is(0));
    }

    @Test
    public void buildApplicationOffences_shouldReturnEmptyArrayWhenNoOffencesMatch() throws Exception {
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withCourtApplicationCases(emptyList())
                .build();
        final JsonArray result = invokePrivateMethod("buildApplicationOffences",
                new Class<?>[]{CourtApplication.class, List.class, JurisdictionType.class}, courtApplication, singletonList(randomUUID()), JurisdictionType.MAGISTRATES);
        assertThat(result.size(), is(0));
    }

    private Hearing getHearingWithCourtApplications() throws IOException {
        final List<Hearing> list = getHearings("courtlists.hearings.repository.all.json");
        return list.stream()
                .filter(h -> h != null && h.getCourtApplications() != null && !h.getCourtApplications().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Test data has no hearing with court applications"));
    }

    @Test
    public void buildDefendantFromCourtApplication_shouldBuildDefendantWithOffencesAndAsn() throws Exception {
        final JsonObject hearingFromListing = getJsonPayload("listing-hearing-with-court-application.json")
                .getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
        final Hearing hearing = getHearingWithCourtApplications();
        final CourtApplication courtApplication = hearing.getCourtApplications().stream()
                .filter(ca -> ca.getId().equals(fromString("528fe634-20f3-474b-a110-4c6141c58c99")))
                .findFirst().orElseThrow();
        final List<UUID> offencesForApplications = List.of(
                fromString("072319bf-73c2-41b5-b309-c8c86c9b077b"),
                fromString("651fc68b-8b9f-4cf2-912a-0b55d536323c"));
        final JsonObject result = invokePrivateMethod("buildDefendantFromCourtApplication",
                new Class<?>[]{JsonObject.class, CourtApplication.class, Hearing.class, List.class},
                hearingFromListing, courtApplication, hearing, offencesForApplications);
        assertThat(result.containsKey("id"), is(true));
        assertThat(result.getString("asn"), is("Arrest456"));
        assertThat(result.getString("pncId"), is("PNC-APP-001"));
        assertThat(result.containsKey("offences"), is(true));
        assertThat(result.getJsonArray("offences").size(), is(2));
        assertThat(result.containsKey("defenceOrganization"), is(true));
    }

    @Test
    public void buildDefendantFromCourtApplication_shouldSurfacePncIdForLegalEntitySubject() throws Exception {
        final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                .withMasterDefendantId(randomUUID())
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation().withName("Acme Ltd").build())
                        .build())
                .withPncId("PNC-SUBJ-ORG-001")
                .build();
        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(randomUUID())
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withId(randomUUID())
                        .withMasterDefendant(masterDefendant)
                        .build())
                .withDefendantASN("SUBJ-ASN-001")
                .build();
        final Hearing hearing = Hearing.hearing().withId(randomUUID()).build();
        final JsonObject hearingFromListing = createObjectBuilder()
                .add("defendants", createArrayBuilder().build())
                .build();

        final JsonObject result = invokePrivateMethod("buildDefendantFromCourtApplication",
                new Class<?>[]{JsonObject.class, CourtApplication.class, Hearing.class, List.class},
                hearingFromListing, courtApplication, hearing, emptyList());

        // Legal-entity subject has no personDefendant, so the person block is skipped; pncId must still surface.
        assertThat(result.getString("pncId"), is("PNC-SUBJ-ORG-001"));
        assertThat(result.getString("asn"), is("SUBJ-ASN-001"));
        assertThat(result.containsKey("id"), is(false));
    }

    @Test
    public void enrichHearingFromCourtApplication_shouldEnrichHearingWithCourtApplicationBlock() throws Exception {
        final JsonObject hearingFromListing = getJsonPayload("listing-hearing-with-court-application.json")
                .getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
        final Hearing hearing = getHearingWithCourtApplications();
        final UUID courtApplicationId = fromString("528fe634-20f3-474b-a110-4c6141c58c99");
        final JsonObject result = invokePrivateMethod("enrichHearingFromCourtApplication",
                new Class<?>[]{JsonObject.class, Hearing.class, UUID.class},
                hearingFromListing, hearing, courtApplicationId);
        assertThat(result.containsKey("courtApplication"), is(true));
        final JsonObject courtApplication = result.getJsonObject("courtApplication");
        assertThat(courtApplication.containsKey("applicationType"), is(true));
        assertThat(courtApplication.containsKey("applicant"), is(true));
        assertThat(courtApplication.getJsonObject("applicant").containsKey("name"), is(true));
        assertThat(courtApplication.containsKey("respondents"), is(true));
        assertThat(result.containsKey("defendants"), is(true));
        assertThat(result.getJsonArray("defendants").size(), is(1));
    }

    @Test
    public void shouldEnrichCourtlistWithCourtApplicationApplicantAndDefendants() throws IOException {
        final Optional<JsonObject> listingResponse = Optional.of(getJsonPayload("listing-hearing-with-court-application.json"));
        final List<Hearing> hearingList = getHearings("courtlists.hearings.repository.all.json");
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(listingResponse);
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearingList);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        final JsonObject actual = courtlistQueryView.searchCourtlist(query).payloadAsJsonObject();

        final JsonObject firstHearing = actual.getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
        assertThat(firstHearing.containsKey("courtApplication"), is(true));
        final JsonObject courtApplication = firstHearing.getJsonObject("courtApplication");
        assertThat(courtApplication.getString("applicationType"), is("Application for first hearing summons for criminal case"));
        assertThat(courtApplication.getJsonObject("applicant").getString("name"), is("Transport for London"));
        assertThat(courtApplication.getJsonArray("respondents").size(), is(2));
        assertThat(firstHearing.getJsonArray("defendants").size(), is(1));
        final JsonObject defendant = firstHearing.getJsonArray("defendants").getJsonObject(0);
        assertThat(defendant.containsKey("offences"), is(true));
        assertThat(defendant.getJsonArray("offences").size(), is(2));
    }

    /**
     * LPT-2677: the court list numbers each offence with its orderIndex (the number Case at a glance and
     * Manage hearing show). The offence rebuilt here must keep listing's orderIndex and count, so a gap
     * left by a removed offence (1, 3) is printed as 1, 3 and not as positions 1, 2.
     */
    @Test
    public void shouldKeepListingOrderIndexAndCountOnEachOffence() throws IOException {
        final JsonObject actualOffence = firstOffenceOfCourtList(withOrderingOnListingOffences(
                getJsonPayload("listing-hearing-with-prosecution-case.json"), 3, 2));

        assertThat(actualOffence.getInt("orderIndex"), is(3));
        assertThat(actualOffence.getInt("count"), is(2));
    }

    @Test
    public void shouldTakeOrderIndexAndCountFromProgressionWhenListingHasNone() throws IOException {
        final JsonObject actualOffence = firstOffenceOfCourtList(getJsonPayload("listing-hearing-with-prosecution-case.json"));

        assertThat(actualOffence.getInt("orderIndex"), is(1));
        assertThat(actualOffence.getInt("count"), is(0));
    }

    private static final String APPLICATION_OFFENCE_A = "072319bf-73c2-41b5-b309-c8c86c9b077b";
    private static final String APPLICATION_OFFENCE_B = "651fc68b-8b9f-4cf2-912a-0b55d536323c";

    /**
     * LPT-2677: application offences are built from progression's own court application, in its stored
     * array order. They are ordered and numbered by orderIndex, as listing orders a case's offences.
     */
    @Test
    public void shouldOrderAndNumberApplicationOffencesByOrderIndexForMagistrates() throws IOException {
        final List<Hearing> hearings = getHearings("courtlists.hearings.repository.all.json");
        setApplicationOffenceOrdering(hearings, APPLICATION_OFFENCE_A, 5, 0);
        setApplicationOffenceOrdering(hearings, APPLICATION_OFFENCE_B, 2, 0);

        final JsonObject courtListHearing = firstHearingOfApplicationCourtList(hearings);

        final JsonArray defendantOffences = courtListHearing.getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(idsAndOrderIndexes(defendantOffences), is(List.of(APPLICATION_OFFENCE_B + ":2", APPLICATION_OFFENCE_A + ":5")));
        final JsonArray applicantOffences = courtListHearing.getJsonObject("courtApplication").getJsonObject("applicant").getJsonArray("offences");
        assertThat(idsAndOrderIndexes(applicantOffences), is(List.of(APPLICATION_OFFENCE_B + ":2", APPLICATION_OFFENCE_A + ":5")));
    }

    @Test
    public void shouldPutApplicationOffencesWithACountFirstForCrown() throws IOException {
        final List<Hearing> hearings = getHearings("courtlists.hearings.repository.all.json").stream()
                .map(hearing -> Hearing.hearing().withValuesFrom(hearing).withJurisdictionType(JurisdictionType.CROWN).build())
                .collect(toList());
        setApplicationOffenceOrdering(hearings, APPLICATION_OFFENCE_A, 5, 1);
        setApplicationOffenceOrdering(hearings, APPLICATION_OFFENCE_B, 2, 0);

        final JsonObject courtListHearing = firstHearingOfApplicationCourtList(hearings);

        final JsonArray defendantOffences = courtListHearing.getJsonArray("defendants").getJsonObject(0).getJsonArray("offences");
        assertThat(idsAndOrderIndexes(defendantOffences), is(List.of(APPLICATION_OFFENCE_A + ":5", APPLICATION_OFFENCE_B + ":2")));
        assertThat(defendantOffences.getJsonObject(0).getInt("count"), is(1));
    }

    private JsonObject firstHearingOfApplicationCourtList(final List<Hearing> hearings) throws IOException {
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(Optional.of(getJsonPayload("listing-hearing-with-court-application.json")));
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearings);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        return courtlistQueryView.searchCourtlist(query).payloadAsJsonObject()
                .getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0);
    }

    private static void setApplicationOffenceOrdering(final List<Hearing> hearings, final String offenceId, final int orderIndex, final int count) {
        hearings.stream()
                .filter(hearing -> hearing.getCourtApplications() != null)
                .flatMap(hearing -> hearing.getCourtApplications().stream())
                .filter(application -> application.getCourtApplicationCases() != null)
                .flatMap(application -> application.getCourtApplicationCases().stream())
                .filter(applicationCase -> applicationCase.getOffences() != null)
                .forEach(applicationCase -> applicationCase.getOffences().replaceAll(offence -> offence.getId().toString().equals(offenceId)
                        ? Offence.offence().withValuesFrom(offence).withOrderIndex(orderIndex).withCount(count).build()
                        : offence));
    }

    private static List<String> idsAndOrderIndexes(final JsonArray offences) {
        return offences.getValuesAs(JsonObject.class).stream()
                .map(offence -> offence.getString("id") + ":" + (offence.containsKey("orderIndex") ? offence.get("orderIndex") : "none"))
                .collect(toList());
    }

    private JsonObject firstOffenceOfCourtList(final JsonObject listingPayload) throws IOException {
        final List<Hearing> hearings = getHearings("courtlists.hearings.repository.all.json");
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(hearings.get(0).getProsecutionCases().get(0)).toString());
        when(listingService.searchCourtlist(any(JsonEnvelope.class))).thenReturn(Optional.of(listingPayload));
        when(hearingQueryView.getHearings(any(List.class))).thenReturn(hearings);
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        final JsonEnvelope query = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.search.court.list").build(),
                createObjectBuilder().build());

        return firstDefendant(courtlistQueryView.searchCourtlist(query).payloadAsJsonObject())
                .getJsonArray("offences").getJsonObject(0);
    }

    private static JsonObject firstDefendant(final JsonObject courtList) {
        return courtList.getJsonArray("hearingDates").getJsonObject(0)
                .getJsonArray("courtRooms").getJsonObject(0)
                .getJsonArray("timeslots").getJsonObject(0)
                .getJsonArray("hearings").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0);
    }

    private static JsonObject withOrderingOnListingOffences(final JsonObject listingPayload, final int orderIndex, final int count) {
        final String json = listingPayload.toString().replace("\"id\":\"3789ab16-0bb7-4ef1-87ef-c936bf0364f1\"",
                "\"id\":\"3789ab16-0bb7-4ef1-87ef-c936bf0364f1\",\"orderIndex\":" + orderIndex + ",\"count\":" + count);
        return createReader(new ByteArrayInputStream(json.getBytes())).readObject();
    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset());
        return createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();
    }

    private List<Hearing> getHearings(final String resourceName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(resourceName), defaultCharset());
        return createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private List<Hearing> getHearings(final String resourceName, final String defId, final String defId2) throws IOException {
        final String jsonString = getStringFromResourceAndReplaceValues(resourceName, defId, defId2);
        return createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private JsonObject getAndReplaceJsonPayload(final String fileName, final String defId, final String defId2) throws IOException {
        final String jsonString = getStringFromResourceAndReplaceValues(fileName, defId, defId2);
        return createReader(new ByteArrayInputStream(jsonString.getBytes())).readObject();
    }

    private String getStringFromResourceAndReplaceValues(final String fileName, final String defId, final String defId2) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset()).replaceAll("DEFENDANT_ID", defId)
                .replaceAll("DEF_ID2", defId2);
        return jsonString;
    }

    private List<Hearing> getBulkCivilCasesHearings() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("courtlists.hearings.repository.bulk.civil.cases.json"), defaultCharset());
        return createReader(
                new ByteArrayInputStream(jsonString.getBytes()))
                .readArray().stream()
                .map(jsonObject -> jsonObjectToObjectConverter.convert((JsonObject) jsonObject, Hearing.class))
                .collect(toList());
    }

    private List<Hearing> getHearingsWithoutCase() throws IOException {
        final String jsonString = Resources.toString(Resources.getResource("courtlists.hearings.repository.without.case.json"), defaultCharset());
        return createReader(
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
