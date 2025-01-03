package uk.gov.moj.cpp.progression.processor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.MODEOFTRIAL_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.listing.courts.Defendants;
import uk.gov.justice.listing.courts.HearingPartiallyUpdated;
import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.persist.NotificationInfoRepository;
import uk.gov.moj.cpp.progression.persist.entity.NotificationInfo;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
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
public class ListHearingRequestedProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID MULTI_OFFENCE_DEFENDANT_ID = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_1 = randomUUID();
    private static final UUID SAME_DEFENDANT_OFFENCE_ID_2 = randomUUID();

    private static final UUID materialId = randomUUID();

    public static final String TEMPLATE_ID = "e4648583-eb0f-438e-aab5-5eff29f3f7b4";

    static final List<Offence> offences = new ArrayList<Offence>() {{
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_1).build());
        add(Offence.offence().withId(SAME_DEFENDANT_OFFENCE_ID_2).build());
    }};

    private static final uk.gov.justice.core.courts.Defendant multiOffenceDefendant = Defendant.defendant().withId(MULTI_OFFENCE_DEFENDANT_ID)
            .withOffences(offences)
            .withProsecutionCaseId(CASE_ID)
            .withPersonDefendant(PersonDefendant.personDefendant()
                    .withPersonDetails(Person.person()
                            .build())
                    .build())
            .build();

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private NotificationService notificationService;


    @Mock
    private RefDataService refDataService;

    @Mock
    private DefenceService defenceService;

    @Mock
    private NotificationInfoRepository notificationInfoRepository;

    @Mock
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private ApplicationParameters applicationParameters;

    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataOffenceService referenceDataOffenceService;

    @InjectMocks
    private ListHearingRequestedProcessor listHearingRequestedProcessor;

    @Captor
    private ArgumentCaptor<ListCourtHearing> listingCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> senderCaptor;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> prosecutorEmailCapture;

    @Captor
    private ArgumentCaptor<List<EmailChannel>> defendantEmailCapture;

    private HearingNotificationHelper hearingNotificationHelper = new HearingNotificationHelper();

    @BeforeEach
    public void initMocks() {
        final Address address = Address.address()
                .withAddress1("testAddress1")
                .withAddress2("testAddress2")
                .withAddress3("address3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("sl6 1nb")
                .build();
        final LjaDetails ljaDetails = LjaDetails.ljaDetails()
                .withLjaCode("testLja")
                .withWelshLjaName("testWalesLja")
                .withLjaName("ljaName")
                .build();
        final CourtCentre enrichedCourtCenter = CourtCentre.courtCentre()
                .withCourtHearingLocation("Burmimgham")
                .withId(randomUUID())
                .withLja((ljaDetails)).withName("Lavender Court")
                .withAddress(address)
                .withWelshCourtCentre(false)
                .build();
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.listHearingRequestedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);

        setField(this.hearingNotificationHelper, "progressionService", progressionService);
        setField(this.hearingNotificationHelper, "defenceService", defenceService);
        setField(this.hearingNotificationHelper, "notificationInfoRepository", notificationInfoRepository);
        setField(this.hearingNotificationHelper, "referenceDataService", refDataService);
        setField(this.hearingNotificationHelper, "referenceDataOffenceService", referenceDataOffenceService);
        setField(this.hearingNotificationHelper, "notificationService", notificationService);
        setField(this.hearingNotificationHelper, "materialUrlGenerator", materialUrlGenerator);
        setField(this.hearingNotificationHelper, "documentGeneratorService", documentGeneratorService);
        setField(this.hearingNotificationHelper, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        setField(this.hearingNotificationHelper, "objectToJsonObjectConverter", objectToJsonObjectConverter);
        setField(this.hearingNotificationHelper, "sender", sender);
        setField(this.hearingNotificationHelper, "requester", requester);
        setField(this.listHearingRequestedProcessor, "hearingNotificationHelper", hearingNotificationHelper);

    }


    @Test
    public void shouldCallCommands() {
        final ListHearingRequested listHearingRequested = ListHearingRequested.listHearingRequested()
                .withHearingId(UUID.randomUUID())
                .withListNewHearing(receivePayloadOfListHearingRequestWithOneCaseMultipleDefendantsWithReferralReason())
                .withSendNotificationToParties(true)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(listHearingRequested);


        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                payload);

        final ProsecutionCase prosecutionCase = getProsecutionCaseWithMultiOffence();
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), any(String.class))).thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));
        final ListCourtHearing listCourtHearing = ListCourtHearing.listCourtHearing().build();
        when(listCourtHearingTransformer.transform(any(JsonEnvelope.class), any(List.class), any(CourtHearingRequest.class), any(UUID.class))).thenReturn(listCourtHearing);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);

        listHearingRequestedProcessor.handle(requestMessage);

        verify(listingService).listCourtHearing(any(JsonEnvelope.class), listingCaptor.capture());
        assertThat(listingCaptor.getValue(), is(listCourtHearing));

        verify(progressionService).updateHearingListingStatusToSentForListing(any(JsonEnvelope.class), listingCaptor.capture());
        assertThat(listingCaptor.getValue(), is(listCourtHearing));

    }

    @Test
    public void souldCallCommand() {

        final HearingPartiallyUpdated hearingPartiallyUpdated = HearingPartiallyUpdated.hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(randomUUID())
                .withProsecutionCases(singletonList(ProsecutionCases.prosecutionCases()
                        .withCaseId(randomUUID())
                        .withDefendants(singletonList(Defendants.defendants()
                                .withDefendantId(randomUUID())
                                .withOffences(singletonList(Offences.offences()
                                        .withOffenceId(randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingPartiallyUpdated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-partially-updated"),
                payload);

        listHearingRequestedProcessor.handlePublicEventForPartiallyUpdate(requestMessage);

        verify(sender).send(senderCaptor.capture());
        assertThat(senderCaptor.getValue().metadata().name(), is("progression.command.update-hearing-for-partial-allocation"));

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = jsonObjectToObjectConverter.convert(senderCaptor.getValue().payload(), UpdateHearingForPartialAllocation.class);

        assertThat(updateHearingForPartialAllocation.getHearingId(), is(hearingPartiallyUpdated.getHearingIdToBeUpdated()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getCaseId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getCaseId()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getDefendantId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getDefendants().get(0).getDefendantId()));
        assertThat(updateHearingForPartialAllocation.getProsecutionCasesToRemove().get(0).getDefendantsToRemove().get(0).getOffencesToRemove().get(0).getOffenceId(), is(hearingPartiallyUpdated.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId()));

    }

    @Test
    public void sendHearingNotificationsToRelevantParties() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));
        when(notificationInfoRepository.save(any())).thenReturn(NotificationInfo.Builder.builder().build());

        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(2)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());

    }

    @Test
    public void sendHearingNotificationsToPartiesWithNoProsecutorEmail() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withEmail("organisation@org.com")
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));

        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(notificationService, times(1)).sendLetter(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    public void sendHearingNotificationsToPartiesWithNoDefandantRepByOrgAndNoProsecutorCpsFlagOn() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email-and-cpsFlag-on.json")));

        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(null);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));

        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(1)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());
        verify(notificationService, times(0)).sendLetter(any(), any(), any(), any(), any(), anyBoolean(), any());

        List<EmailChannel> prosecutorEmailData = prosecutorEmailCapture.getValue();
    }

    @Test
    public void sendHearingNotificationsToPartiesWithDefendantAddressAndProsecutorAddress() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final UUID materialsId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));

        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));
        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(2)).sendLetter(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    public void sendHearingNotificationsToPartiesWithDefendantAddressAndProsecutorAddressWithNotifyFlag() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final UUID materialsId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested-with-notify-false.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));

        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(0)).sendLetter(any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(notificationService, times(0)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());

    }
    @Test
    public void sendHearingNotificationsToRelevantPartiesOnTheirAddress() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-no-email.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder()
                .withOrganisationId(randomUUID())
                .withAddress(DefenceOrganisationAddress.defenceOrganisationAddressBuilder()
                        .withAddress1("addressLine1")
                        .withAddress2("addressLine2")
                        .withAddress3("addressLine3")
                        .withAddress4("addressLine4")
                        .withAddressPostcode("CR01JS")
                        .build())
                .withOrganisationName("defence Organisation")
                .build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));
        listHearingRequestedProcessor.handle(requestMessage);
        verify(notificationService, times(2)).sendLetter(any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    public void sendHearingNotificationsToRelevantPartiesWithNoEmailWithNoLetter() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("progression.event.list-hearing-requested.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.list-hearing-requested"),
                objectToJsonObjectConverter.convert(payload));

        final JsonObject prosecutionCase = FileUtil.jsonFromString(FileUtil.getPayload("progressioncase.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));
        when(progressionService.transformCourtCentreV2(any(), any())).thenReturn(getCourtCentre());
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(createObjectBuilder().
                add("prosecutionCase", prosecutionCase)
                .build()
        ));
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(TEMPLATE_ID);
        when(refDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getPayload("prosecutor-with-cpsFlag-on.json")));
        AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisationBuilder().build();
        when(defenceService.getDefenceOrganisationByDefendantId(any(), any())).thenReturn(associatedDefenceOrganisation);
        when(referenceDataOffenceService.getOffenceById(any(), any(), any())).thenReturn(of(getOffence("trial")));

        listHearingRequestedProcessor.handle(requestMessage);

        verify(notificationService, times(0)).sendEmail(any(), any(), any(), any(), any(), prosecutorEmailCapture.capture());

    }


    @Test
    public void shouldEnrichCourtCenterWhenCallNewHearng() {

        final HearingRequestedForListing hearingRequestedForListing = HearingRequestedForListing.hearingRequestedForListing()
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .build())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.listing.hearing-requested-for-listing"),
                objectToJsonObjectConverter.convert(hearingRequestedForListing));

        final CourtCentre enrichedCourtCenter = getCourtCentre();
        when(progressionService.transformCourtCentre(any(), any())).thenReturn(enrichedCourtCenter);
        listHearingRequestedProcessor.handlePublicEvent(requestMessage);

        verify(sender).send(senderCaptor.capture());
        assertThat(senderCaptor.getValue().metadata().name(), is("progression.command.list-new-hearing"));

        final HearingRequestedForListing command = jsonObjectToObjectConverter.convert(senderCaptor.getValue().payload(), HearingRequestedForListing.class);
        assertThat(command.getListNewHearing().getCourtCentre(), is(enrichedCourtCenter));

    }

    private CourtHearingRequest receivePayloadOfListHearingRequestWithOneCaseMultipleDefendantsWithReferralReason() {
        return CourtHearingRequest.courtHearingRequest()
                .withHearingType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("typeDescription")
                        .build())
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(randomUUID())
                        .withName("Court 1")
                        .build())
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(CASE_ID)
                        .withDefendantOffences(asList(randomUUID(), randomUUID()))
                        .withDefendantId(MULTI_OFFENCE_DEFENDANT_ID)
                        .build(), ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(CASE_ID)
                        .withDefendantOffences(asList(randomUUID(), randomUUID()))
                        .withDefendantId(randomUUID())
                        .build()))
                .withEarliestStartDateTime(ZonedDateTime.now())
                .build();
    }

    private ProsecutionCase getProsecutionCaseWithMultiOffence() {
        return ProsecutionCase.prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(CASE_ID)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(Arrays.asList(multiOffenceDefendant))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
    }

    public JsonObject getPayload(final String path) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    private static JsonObject getOffence(final String modeoftrial) {
        return Json.createObjectBuilder().add(LEGISLATION, "E12")
                .add(LEGISLATION_WELSH, "123")
                .add(OFFENCE_TITLE, "title-of-offence")
                .add(WELSH_OFFENCE_TITLE, "welsh-title")
                .add(MODEOFTRIAL_CODE, modeoftrial)
                .add(CJS_OFFENCE_CODE, "British").build();
    }

    private CourtCentre getCourtCentre() {
        return CourtCentre.courtCentre()
                .withCourtHearingLocation("Burmimgham")
                .withId(randomUUID())
                .withLja((LjaDetails.ljaDetails().build())).withName("Lavender Court")
                .withAddress(Address.address()
                        .withAddress1("Test Address 1")
                        .withAddress2("Test Address 2")
                        .withAddress3("Test Address 3")
                        .withPostcode("AS1 1DF").build())
                .withWelshCourtCentre(false)
                .build();
    }


}
