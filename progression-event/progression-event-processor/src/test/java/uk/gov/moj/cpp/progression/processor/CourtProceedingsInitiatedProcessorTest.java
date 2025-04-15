package uk.gov.moj.cpp.progression.processor;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.test.FileUtil.givenPayload;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.MigrationSourceSystem;
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
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    private static final String PCF_CASE_URN = "PCF_CASE_URN";

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Spy
    private ListToJsonArrayConverter<ListHearingRequest> hearingRequestListToJsonArrayConverter;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    @Mock
    private AzureFunctionService azureFunctionService;

    public static final String SEXUAL_OFFENCE_RR_DESCRIPTION = "Complainant's anonymity protected by virtue of Section 1 of the Sexual Offences Amendment Act 1992";
    public static final String YOUTH_OFFENCE_RR_DESCRIPTION = "Section 49 of the Children and Young Persons Act 1933 applies";

    @BeforeEach
    public void initMocks() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.progressionService, "objectToJsonObjectConverter", this.objectToJsonObjectConverter);
        setField(this.progressionService, "hearingRequestListToJsonArrayConverter", this.hearingRequestListToJsonArrayConverter);
        setField(this.progressionService, "enveloper", this.enveloper);
        setField(this.progressionService, "sender", this.sender);
        setField(this.progressionService, "azureFunctionService", this.azureFunctionService);
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

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId), offenceId, offenceCode, true);

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
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

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

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId), offenceId, offenceCode, false);

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
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
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
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

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

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId), offenceId, offenceCode, true);

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
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
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

    @Test
    public void shouldSendPublicMessageInCaseOfGroupCases() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCaseForGroupCases(groupId, caseId, List.of(defendantId), offenceId, offenceCode, true, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build());

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
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withIsGroupMaster(true)
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
        when(objectToJsonObjectConverter.convert(any())).thenReturn(Json.createObjectBuilder().build());

        this.eventProcessor.handle(requestMessage);
        verify(sender, VerificationModeFactory.times(2)).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getAllValues().get(0).metadata().name(), is("public.progression.group-prosecution-cases-created"));
        assertThat(envelopeCaptor.getAllValues().get(0).payload().getString("groupId"), is(groupId.toString()));
    }


    @Test
    public void shouldCreateAnotherCaseWhenExistsCaseEjected() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        String existingCaseId = UUID.randomUUID().toString();
        final JsonObject searchResult = createObjectBuilder().add("caseId", existingCaseId).build();

        final JsonObject searchProsecutionCaseResult = createObjectBuilder().add("prosecutionCase",createObjectBuilder().add("caseStatus", "EJECTED").build()).build();


        final ProsecutionCase prosecutionCase = getProsecutionCaseWithCaseURN(caseId, List.of(defendantId), offenceId, offenceCode, true);

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
        when(azureFunctionService.relayCaseOnCPP(anyString())).thenReturn(1);


        doReturn(Optional.of(searchResult)).when(progressionService).caseExistsByCaseUrn(requestMessage, PCF_CASE_URN);

        doReturn(Optional.of(searchProsecutionCaseResult)).when(progressionService).prosecutionCaseByCaseId(requestMessage, existingCaseId);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(PCF_CASE_URN).build())
                        .build()))
                .build());
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
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

    @Test
    public void shouldNotCreateAnotherCaseWhenExistsNotCaseEjected() {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        String existingCaseId = UUID.randomUUID().toString();
        final JsonObject searchResult = createObjectBuilder().add("caseId", existingCaseId).build();
        final JsonObject searchProsecutionCaseResult = createObjectBuilder().add("prosecutionCase",createObjectBuilder().add("caseStatus", "NOT_EJECTED").build()).build();


        final ProsecutionCase prosecutionCase = getProsecutionCaseWithCaseURN(caseId, List.of(defendantId), offenceId, offenceCode, true);

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


        doReturn(Optional.of(searchResult)).when(progressionService).caseExistsByCaseUrn(requestMessage, PCF_CASE_URN);

        doReturn(Optional.of(searchProsecutionCaseResult)).when(progressionService).prosecutionCaseByCaseId(requestMessage, existingCaseId);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(PCF_CASE_URN).build())
                        .build()))
                .build());
        when(listCourtHearingTransformer.transform(any(), any(), anyList(), any())).thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());
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

        verify(sender, times(2)).send(envelopeCaptor.capture());

        List<String> metadataNames = envelopeCaptor.getAllValues().stream()
                .map(value -> value.metadata().name())
                .toList();

        assertFalse(
                metadataNames.contains("progression.command.create-prosecution-case"),
                "Expected 'progression.command.create-prosecution-case' to not be present in the metadata names."
        );
    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessageWhenMultipleHearingRequestHasSameListHearingRequestWithDifferentDefendant() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);
        final UUID roomId = UUID.randomUUID();
        final UUID hearingTypeId = UUID.randomUUID();
        final ZonedDateTime zonedDateTime = new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next();

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId1, defendantId2), offenceId, offenceCode, false);

        final ListHearingRequest listHearingRequest1 = populateListHearingRequest(caseId, defendantId1, offenceId, roomId, hearingTypeId, zonedDateTime);

        final ListHearingRequest listHearingRequest2 = populateListHearingRequest(caseId, defendantId2, offenceId, roomId, hearingTypeId, zonedDateTime);

        final List<ListHearingRequest> listHearingRequestList = List.of(listHearingRequest1, listHearingRequest2);

        final List<JsonObject> referenceDataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(listHearingRequestList);
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester)))
                .thenReturn(Optional.of(referenceDataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(),anyList(), anyList(), any()))
                .thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

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
    public void shouldHandleCasesReferredToCourtEventMessageWhenMultipleHearingRequestHasSameListHearingRequestWithSameDefendant() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
//        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);
        final UUID roomId = UUID.randomUUID();
        final UUID hearingTypeId = UUID.randomUUID();
        final ZonedDateTime zonedDateTime = new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next();

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId), offenceId, offenceCode, false);

        final ListHearingRequest listHearingRequest1 = populateListHearingRequest(caseId, defendantId, offenceId, roomId, hearingTypeId, zonedDateTime);

        final ListHearingRequest listHearingRequest2 = populateListHearingRequest(caseId, defendantId, offenceId, roomId, hearingTypeId, zonedDateTime);

        final List<ListHearingRequest> listHearingRequestList = List.of(listHearingRequest1, listHearingRequest2);

        final List<JsonObject> referenceDataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(listHearingRequestList);
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester)))
                .thenReturn(Optional.of(referenceDataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(),anyList(), anyList(), any()))
                .thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());

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
        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

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
    public void shouldHandleCasesReferredToCourtEventMessageWhenMultipleHearingRequestHasDifferentListHearingRequestWithSameDefendant() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId), offenceId, offenceCode, false);

        final ListHearingRequest listHearingRequest1 = populateListHearingRequest(caseId, defendantId, offenceId);

        final ListHearingRequest listHearingRequest2 = populateListHearingRequest(caseId, defendantId, offenceId);

        final List<ListHearingRequest> listHearingRequestList = List.of(listHearingRequest1, listHearingRequest2);

        final List<JsonObject> referenceDataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(listHearingRequestList);
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester)))
                .thenReturn(Optional.of(referenceDataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(),anyList(), anyList(), any()))
                .thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);

        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

        verify(sender, times(4)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> jsonObjectEnvelope = envelopeCaptor.getAllValues().get(2);

        assertThat("progression.command.create-prosecution-case", is(jsonObjectEnvelope.metadata().name()));
        assertThat(caseId.toString(), is(jsonObjectEnvelope.payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray reportingRestrictionsArray = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");


        assertThat(reportingRestrictionsArray.size(), is(2));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("orderedDate"), is(LocalDate.now().toString()));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("orderedDate"), is(LocalDate.now().toString()));

        final Envelope<JsonObject> jsonObjectEnvelope1 = envelopeCaptor.getAllValues().get(3);

        assertThat("progression.command.update-defendant-listing-status", is(jsonObjectEnvelope1.metadata().name()));
        assertThat(caseId.toString(), is(jsonObjectEnvelope1.payload().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id")));
        assertThat("SENT_FOR_LISTING", is(jsonObjectEnvelope1.payload().getString("hearingListingStatus")));

        final JsonArray reportingRestrictionsOfUpdateDefendantListingStatus = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
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
    public void shouldHandleCasesReferredToCourtEventMessageWhenMultipleHearingRequestHasDifferentListHearingRequestWithDifferentDefendant() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId1, defendantId2), offenceId, offenceCode, false);

        final ListHearingRequest listHearingRequest1 = populateListHearingRequest(caseId, defendantId1, offenceId);

        final ListHearingRequest listHearingRequest2 = populateListHearingRequest(caseId, defendantId2, offenceId);

        final List<ListHearingRequest> listHearingRequestList = List.of(listHearingRequest1, listHearingRequest2);

        final List<JsonObject> referenceDataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(listHearingRequestList);
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester)))
                .thenReturn(Optional.of(referenceDataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        final List<HearingListingNeeds> hearingsList = new ArrayList<>();
        hearingsList.add(HearingListingNeeds.hearingListingNeeds()
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withDefendants(List.of(Defendant.defendant()
                                .withOffences(List.of(Offence.offence()
                                        .withId(UUID.randomUUID())
                                        .withReportingRestrictions(List.of(ReportingRestriction.reportingRestriction()
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
        when(listCourtHearingTransformer.transform(any(),anyList(), anyList(), any()))
                .thenReturn(ListCourtHearing.listCourtHearing().withHearings(hearingsList).build());

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

        verify(sender, times(4)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> jsonObjectEnvelope = envelopeCaptor.getAllValues().get(2);

        assertThat("progression.command.create-prosecution-case", is(jsonObjectEnvelope.metadata().name()));
        assertThat(caseId.toString(), is(jsonObjectEnvelope.payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray reportingRestrictionsArray = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");


        assertThat(reportingRestrictionsArray.size(), is(2));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("orderedDate"), is(LocalDate.now().toString()));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("orderedDate"), is(LocalDate.now().toString()));

        final Envelope<JsonObject> jsonObjectEnvelope1 = envelopeCaptor.getAllValues().get(3);

        assertThat("progression.command.update-defendant-listing-status", is(jsonObjectEnvelope1.metadata().name()));
        assertThat(caseId.toString(), is(jsonObjectEnvelope1.payload().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getString("id")));
        assertThat("SENT_FOR_LISTING", is(jsonObjectEnvelope1.payload().getString("hearingListingStatus")));

        final JsonArray reportingRestrictionsOfUpdateDefendantListingStatus = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
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
    public void shouldHandleCasesReferredToCourtEventMessageWhenNoHearingRequestExist() throws IOException {
        //Given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId1 = UUID.randomUUID();
        final UUID defendantId2 = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final String offenceCode = RandomStringUtils.randomAlphanumeric(8);

        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, List.of(defendantId1, defendantId2), offenceId, offenceCode, false);

        final List<JsonObject> referenceDataOffencesJsonObject = prepareReferenceDataOffencesJsonObject(offenceId, offenceCode, SEXUAL_OFFENCE_RR_DESCRIPTION, "/referencedataoffences.offences-list.json");

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-proceedings-initiated"),
                payload);

        //When
        when(payload.getJsonObject("courtReferral")).thenReturn(courtReferralJson);
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);
        when(courtReferral.getProsecutionCases()).thenReturn(singletonList(prosecutionCase));
        when(courtReferral.getListHearingRequests()).thenReturn(null);
        when(referenceDataOffenceService.getMultipleOffencesByOffenceCodeList(anyList(), eq(requestMessage), eq(requester)))
                .thenReturn(Optional.of(referenceDataOffencesJsonObject));
        when(jsonObjectToObjectConverter.convert(courtReferralJson, CourtReferral.class)).thenReturn(courtReferral);

        when(listCourtHearingTransformer.transform(any(),anyList(), anyList(), any()))
                .thenReturn(ListCourtHearing.listCourtHearing().withHearings(List.of()).build());

        when(objectToJsonObjectConverter.convert(any())).thenReturn(buildProsecutionCase(caseId));

        this.eventProcessor.handle(requestMessage);
        verify(summonsHearingRequestService).addDefendantRequestToHearing(any(), anyList());

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final Envelope<JsonObject> jsonObjectEnvelope = envelopeCaptor.getAllValues().get(0);

        assertThat("progression.command.create-prosecution-case", is(jsonObjectEnvelope.metadata().name()));
        assertThat(caseId.toString(), is(jsonObjectEnvelope.payload().getJsonObject("prosecutionCase").getString("id")));

        final JsonArray reportingRestrictionsArray = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");


        assertThat(reportingRestrictionsArray.size(), is(2));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(0).getString("orderedDate"), is(LocalDate.now().toString()));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(reportingRestrictionsArray.getJsonObject(1).getString("orderedDate"), is(LocalDate.now().toString()));

        final JsonArray reportingRestrictionsOfUpdateDefendantListingStatus = jsonObjectEnvelope.payload().getJsonObject("prosecutionCase")
                .getJsonArray("prosecutionCases").getJsonObject(0)
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonArray("offences").getJsonObject(0)
                .getJsonArray("reportingRestrictions");

        assertThat(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(0).getString("label"), is(YOUTH_OFFENCE_RR_DESCRIPTION));
        assertThat(LocalDate.now().toString(), is(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(0).getString("orderedDate")));

        assertThat(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(1).getString("label"), is(SEXUAL_OFFENCE_RR_DESCRIPTION));
        assertThat(LocalDate.now().toString(), is(reportingRestrictionsOfUpdateDefendantListingStatus.getJsonObject(1).getString("orderedDate")));
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final List<UUID> defendantIdList, final UUID offenceId, final String offenceCode, final boolean isYouth) {
        return getProsecutionCase(caseId, defendantIdList, offenceId, offenceCode, isYouth, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build(), false);
    }

    private ProsecutionCase getProsecutionCaseWithCaseURN(final UUID caseId, final List<UUID> defendantIdList, final UUID offenceId, final String offenceCode, final boolean isYouth) {
        return getProsecutionCase(caseId, defendantIdList, offenceId, offenceCode, isYouth, ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(PCF_CASE_URN).build(), true);
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final List<UUID> defendantIdList, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier, final boolean hasMigration) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier);

        builder.withDefendants(buildDefendant(defendantIdList, offenceId, offenceCode, isYouth, LocalDate.now().minusYears(10)));

        if (hasMigration) {
            builder.withMigrationSourceSystem(MigrationSourceSystem.migrationSourceSystem().withMigrationSourceSystemCaseIdentifier("anyUId").withMigrationSourceSystemName("EXHIHBIT").build());
        }


        return builder.build();
    }

    private static List<Defendant> buildDefendant(final List<UUID> defendantIdList, final UUID offenceId, final String offenceCode, final boolean isYouth, final LocalDate dateOfBirth) {
        return defendantIdList.stream().map(defendantId -> {
            final Defendant.Builder defendantBuilder = new Defendant.Builder()
                    .withId(defendantId)
                    .withOffences(Stream.of(Offence.offence()
                                    .withId(offenceId)
                                    .withOffenceCode(offenceCode)
                                    .build())
                            .collect(Collectors.toList()));
            if (isYouth) {
                defendantBuilder.withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withDateOfBirth(dateOfBirth).build()).build());
            }
            return defendantBuilder.build();
        }).toList();
    }

    private ProsecutionCase getProsecutionCaseForGroupCases(final UUID groupId, final UUID caseId, final List<UUID> defendantIdList, final UUID offenceId, final String offenceCode, final boolean isYouth, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        final ProsecutionCase.Builder builder = new ProsecutionCase.Builder().withId(caseId)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier)
                .withGroupId(groupId)
                .withIsGroupMaster(true);

        builder.withDefendants(buildDefendant(defendantIdList, offenceId, offenceCode, isYouth, LocalDate.of(2005, 11, 11)));

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

        return jsonReader.readObject().getJsonArray("offences").getValuesAs(JsonObject.class);
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return ListHearingRequest.listHearingRequest()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCourtHearingLocation("B01IX05")
                        .withRoomId(UUID.randomUUID())
                        .build())
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(List.of(offenceId))
                        .build()))
                .withListedStartDateTime(new ZonedDateTimeGenerator(Period.ofDays(20), ZonedDateTime.now(), DateGenerator.Direction.FUTURE).next())
                .withEstimatedDuration("1 week")
                .withEstimateMinutes(20)
                .withHearingType(HearingType.hearingType().withId(UUID.randomUUID()).withDescription("Sentence").build())
                .build();
    }

    private ListHearingRequest populateListHearingRequest(final UUID caseId, final UUID defendantId, final UUID offenceId, final UUID roomId, final UUID hearingTypeId, final ZonedDateTime zonedDateTime) {
        return ListHearingRequest.listHearingRequest()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withCourtHearingLocation("B01IX05")
                        .withRoomId(roomId)
                        .build())
                .withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withDefendantOffences(List.of(offenceId))
                        .build()))
                .withListedStartDateTime(zonedDateTime)
                .withEstimatedDuration("1 week")
                .withEstimateMinutes(20)
                .withHearingType(HearingType.hearingType().withId(hearingTypeId).withDescription("Sentence").build())
                .build();
    }

    private JsonObject buildProsecutionCase(final UUID caseId) {

        final JsonArrayBuilder arrayBuilder = createArrayBuilder().add(createObjectBuilder()
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

        return createObjectBuilder()
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
                .build();
    }
}
