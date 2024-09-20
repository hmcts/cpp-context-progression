package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.test.FileUtil.givenPayload;

import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.services.test.utils.core.random.DateGenerator;
import uk.gov.justice.services.test.utils.core.random.ZonedDateTimeGenerator;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtProceedingsInitiatedProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @InjectMocks
    private CourtProceedingsInitiatedProcessor eventProcessor;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonObject courtReferralJson;

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CourtReferral courtReferral;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Spy
    private SummonsHearingRequestService summonsHearingRequestService;

    @Spy
    private ProgressionService progressionService;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Spy
    private ListToJsonArrayConverter<ListHearingRequest> hearingRequestListToJsonArrayConverter;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    public static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    public static final String YOUTH_OFFENCE_RR_DESCRIPTION = "Section 49 of the Children and Young Persons Act 1933 applies";


    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.progressionService, "objectToJsonObjectConverter", this.objectToJsonObjectConverter);
        setField(this.progressionService, "hearingRequestListToJsonArrayConverter", this.hearingRequestListToJsonArrayConverter);
        setField(this.progressionService, "enveloper", this.enveloper);
        setField(this.progressionService, "sender", this.sender);
        setField(this.summonsHearingRequestService, "objectToJsonObjectConverter", this.objectToJsonObjectConverter);
        setField(this.summonsHearingRequestService, "sender", this.sender);
        setField(this.hearingRequestListToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.hearingRequestListToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessageWithMultipleRestrictions() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, true);

        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                        .withOffences(Arrays.asList(Offence.offence()
                                                                        .withId(UUID.randomUUID())
                                                                        .withReportingRestrictions(Arrays.asList(ReportingRestriction.reportingRestriction()
                                                                                        .withLabel(YOUTH_OFFENCE_RR_DESCRIPTION)
                                                                                        .withOrderedDate(LocalDate.now())
                                                                                .build(),
                                                                                ReportingRestriction.reportingRestriction()
                                                                                        .withLabel(SEXUAL_OFFENCE_RR_DESCRIPTION)
                                                                                        .withOrderedDate(LocalDate.now())
                                                                                        .build()))
                                                                .build()))
                                                .build()))
                                        .withId(caseId)
                                .build()))
                .build());
        when(listCourtHearingTransformer.transform(any(),any(), (List<ListHearingRequest>) any(),any(),any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        arrayBuilder.add(createObjectBuilder()
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("reportingRestrictions", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("label", YOUTH_OFFENCE_RR_DESCRIPTION)
                                                .add("orderedDate", LocalDate.now().toString())
                                                .build())
                                        .add(createObjectBuilder()
                                                .add("label", SEXUAL_OFFENCE_RR_DESCRIPTION)
                                                .add("orderedDate", LocalDate.now().toString())
                                                .build())
                                        .build())
                                .build()))
                .build());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", arrayBuilder.build())
                .add("prosecutionCases", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", caseId.toString())
                                .add("defendants", createArrayBuilder()
                                        .add(createObjectBuilder()
                                                .add("offences", createArrayBuilder()
                                                        .add(createObjectBuilder()
                                                                .add("reportingRestrictions", createArrayBuilder()
                                                                        .add(createObjectBuilder()
                                                                                .add("label", YOUTH_OFFENCE_RR_DESCRIPTION)
                                                                                .add("orderedDate", LocalDate.now().toString())
                                                                                .build())
                                                                        .add(createObjectBuilder()
                                                                                .add("label", SEXUAL_OFFENCE_RR_DESCRIPTION)
                                                                                .add("orderedDate", LocalDate.now().toString())
                                                                                .build())
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build());

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), eq(listHearingRequest.getListDefendantRequests()), any());

        verify(sender, times(3)).send(envelopeCaptor.capture());

        assertThat("progression.command.create-prosecution-case", is(envelopeCaptor.getAllValues().get(1).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray reportingRestrictionsArray = envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");


        assertThat(reportingRestrictionsArray.size(), is(2));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("orderedDate"), is(LocalDate.now().toString()));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("orderedDate"), is(LocalDate.now().toString()));

        assertThat("progression.command.update-defendant-listing-status", is(envelopeCaptor.getAllValues().get(2).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(2).payload().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id")));
        assertThat("SENT_FOR_LISTING", is(envelopeCaptor.getAllValues().get(2).payload().getString("hearingListingStatus")));

        final JsonArray reportingRestrictionsOfUpdateDefendantListingStatus = envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase")
                .getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");

        assertThat(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(LocalDate.now().toString(), is(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(0).getString("orderedDate")));

        assertThat(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(LocalDate.now().toString(), is(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(1).getString("orderedDate")));
    }

    @Test
    public void shouldNotIncludeRestrictionsListIfReferenceDataQueryReturnsNoRR() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, false);

        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list-withoutRR.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        when(listCourtHearingTransformer.transform(any(),any(), (List<ListHearingRequest>) any(),any(),any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        arrayBuilder.add(createObjectBuilder()
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", UUID.randomUUID().toString())
                                .build()))
                .build());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", arrayBuilder.build())
                .build());

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), eq(listHearingRequest.getListDefendantRequests()), any());

        verify(sender, times(2)).send(envelopeCaptor.capture());

        assertThat("progression.command.create-prosecution-case", is(envelopeCaptor.getAllValues().get(1).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray offencesArray = envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences");

        assertThat(offencesArray.size(), is(1));
        assertThat(offencesArray.getJsonObject(0).containsKey("reportingRestrictions"), is(false));
    }

    @Test
    public void shouldIncludeYouthRRIfReferenceDataQueryReturnsNoRRButDefendantIsYouth() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, defendantId, offenceId, offenceCode, true);

        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester))).thenReturn(Optional.of(emptyList()));

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(Arrays.asList(ReportingRestriction.reportingRestriction()
                                                        .withLabel(YOUTH_OFFENCE_RR_DESCRIPTION)
                                                        .withOrderedDate(LocalDate.now())
                                                        .build(),
                                                ReportingRestriction.reportingRestriction()
                                                        .withLabel(SEXUAL_OFFENCE_RR_DESCRIPTION)
                                                        .withOrderedDate(LocalDate.now())
                                                        .build()))
                                        .build()))
                                .build()))
                        .withId(caseId)
                        .build()))
                .build());
        when(listCourtHearingTransformer.transform(any(),any(), (List<ListHearingRequest>) any(),any(),any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        arrayBuilder.add(createObjectBuilder()
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("reportingRestrictions", createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("label", YOUTH_OFFENCE_RR_DESCRIPTION)
                                                        .add("orderedDate", LocalDate.now().toString())
                                                        .build())
                                        .build()))
                .build()));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder()
                        .add("id", caseId.toString())
                        .add("defendants", arrayBuilder.build())
                .build());
        this.eventProcessor.handle(requestMessage);

        verify(sender, times(3)).send(envelopeCaptor.capture());

        assertThat("progression.command.create-prosecution-case", is(envelopeCaptor.getAllValues().get(1).metadata().name()));
        assertThat(caseId.toString(), is(envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray offencesArray = envelopeCaptor.getAllValues().get(1).payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences");

        assertThat(offencesArray.size(), is(1));
        assertThat(offencesArray.getJsonObject(0).containsKey("reportingRestrictions"), is(true));

        final JsonObject reportingRestrictionJsonObject = offencesArray.getJsonObject(0).getJsonArray("reportingRestrictions").getJsonObject(0);
        assertThat(reportingRestrictionJsonObject.getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionJsonObject.getString("orderedDate"), is(LocalDate.now().toString()));
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth) {
        return getProsecutionCase(caseId, defendantId, offenceId, offenceCode, isYouth, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build());
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier);

        final Defendant.Builder defendantBuilder = new Defendant.Builder()
                .withId(defendantId)
                .withOffences(Stream.of(Offence.offence()
                        .withId(offenceId)
                        .withOffenceCode(offenceCode)
                        .build())
                        .collect(Collectors.toList()));
        if (isYouth) {
            defendantBuilder.withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(LocalDate.now().minusYears(10)).build()).build());
        }
        builder.withDefendants(Arrays.asList(defendantBuilder.build()));


        return builder.build();
    }

    private ProsecutionCase getProsecutionCaseForGroupCases(final UUID groupId, final UUID caseId, final UUID defendantId, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withGroupId(groupId)
                .withIsGroupMaster(true);

        final Defendant.Builder defendantBuilder = new Defendant.Builder()
                .withId(defendantId)
                .withOffences(Stream.of(Offence.offence()
                        .withId(offenceId)
                        .withOffenceCode(offenceCode)
                        .build())
                        .collect(Collectors.toList()));
        if (isYouth) {
            defendantBuilder.withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(LocalDate.of(2005, 11, 11)).build()).build());
        }
        builder.withDefendants(Arrays.asList(defendantBuilder.build()));


        return builder.build();
    }

    private List<JsonObject> prepareReferenceDataOffencesJsonObject(final UUID offenceId,
                                                                    final String offenceCode,
                                                                    final String legislation,
                                                                    final String payloadPath) throws IOException {
        final String referenceDataOffenceJsonString = givenPayload(payloadPath).toString()
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("OFFENCE_CODE", offenceCode)
                .replace("LEGISLATION", legislation);
        final JsonReader jsonReader = Json.createReader(new StringReader(referenceDataOffenceJsonString));


        final List<JsonObject> referencedataOffencesJsonObject = jsonReader.readObject().getJsonArray("offences").getValuesAs(JsonObject.class);
        return referencedataOffencesJsonObject;
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ListHearingRequest.listHearingRequest()
                .withListDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(Arrays.asList(offenceId))
                        .build()))
                .withListedStartDateTime(new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next())
                .build();
    }

    @Test
    public void shouldSendPublicMessageInCaseOfGroupCases() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCaseForGroupCases(groupId, caseId, defendantId, offenceId, offenceCode, true, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build());

        final ListHearingRequest listHearingRequest = populateListHearingRequest(caseId, defendantId, offenceId);

        final List<JsonObject> referencedataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(singletonList(listHearingRequest));
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester))).thenReturn(Optional.of(referencedataOffencesJsonObject));

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withIsGroupMaster(true)
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(Arrays.asList(ReportingRestriction.reportingRestriction()
                                                        .withLabel(YOUTH_OFFENCE_RR_DESCRIPTION)
                                                        .withOrderedDate(LocalDate.now())
                                                        .build(),
                                                ReportingRestriction.reportingRestriction()
                                                        .withLabel(SEXUAL_OFFENCE_RR_DESCRIPTION)
                                                        .withOrderedDate(LocalDate.now())
                                                        .build()))
                                        .build()))
                                .build()))
                        .withId(caseId)
                        .build()))
                .build());
        when(listCourtHearingTransformer.transform(any(),any(), (List<ListHearingRequest>) any(),any(),any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());

        this.eventProcessor.handle(requestMessage);
        verify(sender, VerificationModeFactory.times(2)).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("public.progression.group-prosecution-cases-created"));
        assertThat(envelopeCaptor.getAllValues().get(0).payload().getString("groupId"), is(groupId.toString()));
    }

}
