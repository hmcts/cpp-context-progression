package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;
import uk.gov.moj.cpp.progression.value.object.DefendantVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class CPSEmailNotificationProcessorTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverterMock;

    @InjectMocks
    private CPSEmailNotificationProcessor cpsEmailNotificationProcessor;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObject payload;

    @Mock
    private GetHearingsAtAGlance getHearingsAtAGlance;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private HearingVO hearingVOMock;

    @Mock
    private CPSNotificationVO cpsNotificationVO;

    @Mock
    private Requester requester;
    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    private final String prosecutionCaseSampleWithPersonDefendant = "progression.event.prosecutioncase.persondefendant.cpsnotification.json";
    private final String prosecutionCaseSampleWithLegalEntity = "progression.event.prosecutioncase.legalentity.cpsnotification.json";

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGetCaseDetails() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        Optional<CaseVO> caseVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getCaseDetails", prosecutionCaseJsonOptional);

        assertThat("Case details should not be null", caseVOOptional.isPresent(), is(true));

        CaseVO CaseVO = caseVOOptional.get();
        assertThat("Mismatch caseId", "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b", is(CaseVO.getCaseId().toString()));
        assertThat("Mismatch caseURN", "TFL9135196", is(CaseVO.getCaseURN()));
    }

    @Test
    public void shouldGetDefendantDetailsForPersonDefendant() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        Optional<DefendantVO> defendantVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getDefendantDetails", "924cbf53-0b51-4633-9e99-2682be854af4", prosecutionCaseJsonOptional);

        assertThat("Person Defendant details should not be null", defendantVOOptional.isPresent(), is(true));

        DefendantVO defendantVO = defendantVOOptional.get();
        assertThat("Mismatch first name", "Fred", is(defendantVO.getFirstName()));
        assertThat("Mismatch middle name", "John", is(defendantVO.getMiddleName()));
        assertThat("Mismatch last name", "Smith", is(defendantVO.getLastName()));
    }

    @Test
    public void shouldGetDefendantDetailsForLegalEntityDefendant() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithLegalEntity));
        Optional<DefendantVO> defendantVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getDefendantDetails", "f9ef2dbf-d205-4444-8059-fefed44111dd", prosecutionCaseJsonOptional);

        assertThat("LegalEntity Defendant details should not be null", defendantVOOptional.isPresent(), is(true));

        DefendantVO defendantVO = defendantVOOptional.get();
        assertThat("Mismatch legal entity name", defendantVO.getLegalEntityName(), is("ABC LTD"));
    }

    @Test
    public void shouldGetFutureHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithFutureHearings();
        assertThat("Hearing size mismatched ", 4, is(getHearingAtAGlance.getHearings().size()));

        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getHearingAtAGlance);
        assertThat("Future hearing size mismatched ", 2, is(futureHearings.size()));
    }

    @Test
    public void shouldGetEarliestHearing() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getHearingsAtAGlance);
        final Optional<Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearing", futureHearings);
        assertThat("Earliest date mismatched", earliestHearing.get().getValue().toLocalDate().isEqual(LocalDate.now().plusDays(1)));
    }

    @Test
    public void shouldGetCPSEmail() throws Exception {
        final UUID courtCenterId = randomUUID();
        final String testCPSEmail = "abc@xyz.com";
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();

         when(referenceDataService.getOrganisationUnitById(courtCenterId, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
         Optional<String> cpsEmailOptional = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getCPSEmail", jsonEnvelope, courtCenterId);
         assertEquals(testCPSEmail, cpsEmailOptional.get());
   }

    @Test
    public void shouldGetEarliestHearingDay() throws Exception {
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(3)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build());

        final ZonedDateTime zonedDateTime = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearingDay", hearingDays);
        assertThat("Earliest hearing day mismatched ", zonedDateTime.toLocalDate(), is(LocalDate.now().plusDays(1)));
    }

    @Test
    public void shouldGetHearingVO() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithFutureHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getHearingsAtAGlance);
        final Optional<Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearing", futureHearings);

        if (earliestHearing.isPresent()) {
            final String hearingDate = earliestHearing.get().getValue().toString();
            final Optional<HearingVO> hearingVOOptional = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getHearingVO", hearingDate, futureHearings, earliestHearing);
            final HearingVO hearingVO = hearingVOOptional.get();
            final ZonedDateTime zonedDateTime = ZonedDateTime.parse(hearingVO.getHearingDate());
            assertThat("Hearing date mismatched", zonedDateTime.toLocalDate().isEqual(LocalDate.now().plusDays(1)), is(true));
            Assertions.assertNotNull(hearingVO.getCourtName(), "Court center name should not be empty");
            Assertions.assertNotNull(hearingVO.getCourtCenterId(), "Court center id should not be empty");
        }
    }

    @Test
    public void shouldGetDefendantJson() throws Exception {
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        final UUID defendantId = UUID.fromString("924cbf53-0b51-4633-9e99-2682be854af4");

        final JsonObject resultJsonObject = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getDefendantJson", prosecutionCaseJsonObject, defendantId);
        assertThat("Defendant is mismatched", defendantId.toString(), is(resultJsonObject.getString("id")));
    }

    @Test
    public void shouldPpulateCPSNotificationAndSendEmail() throws Exception {
        final String testCPSEmail = "abc@xyz.com";
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final UUID defendantId = UUID.fromString("924cbf53-0b51-4633-9e99-2682be854af4");
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final UUID uuid = randomUUID();
        hearingVOMock = HearingVO.builder().hearingDate(ZonedDateTime.now().toString()).courtCenterId(uuid).courtName("testName").build();

        when(referenceDataService.getOrganisationUnitById(uuid, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        when(usersGroupService.getDefenceOrganisationDetails(uuid, jsonEnvelope.metadata())).thenReturn(buildDefenceOrganisationVO());
        Whitebox.invokeMethod(cpsEmailNotificationProcessor, "populateCPSNotificationAndSendEmail",
                jsonEnvelope, defendantId.toString(), uuid, prosecutionCaseJsonOptional, hearingVOMock, EmailTemplateType.INSTRUCTION);

        verify(referenceDataService,times(1)).getOrganisationUnitById(uuid, jsonEnvelope, requester);
        verify(usersGroupService,times(1)).getDefenceOrganisationDetails(uuid, jsonEnvelope.metadata());
    }

    @Test
    public void shouldPopulateCPSNotification() throws Exception {
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final UUID randomUUID = randomUUID();

        jsonObject = createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString())).thenReturn(prosecutionCaseJsonOptional);

        Whitebox.invokeMethod(cpsEmailNotificationProcessor, "populateCPSNotification", jsonEnvelope, jsonObject, EmailTemplateType.INSTRUCTION);

        verify(progressionService, times(1)).getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString());
    }

    @Test
    public void shouldGetHearingDetailsWithNullHearingVO() throws Exception {
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final UUID randomUUID = randomUUID();

        jsonObject = createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();

        Optional<HearingVO> hearingVO = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getHearingDetails", prosecutionCaseJsonOptional);

        assertThat("Hearing vo is not null", hearingVO.isPresent(), is(false));

    }

    @Test
    void shouldCallDisassociationCommandForCaseAndApplicationWhenApplicationFoundForCase() {
        
        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonObject defencePublicEventPayload = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("organisationId",organisationId.toString())
                .add("caseId", caseId.toString())
                .build();

        final JsonObject applicationQueryResponsePayload = createObjectBuilder()
                .add("linkedApplications", createArrayBuilder().add(
                        createObjectBuilder().add("applicationId", applicationId.toString())
                                .build()
                ))
                .build();;

        final JsonEnvelope publiceventEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                defencePublicEventPayload);

        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(ofNullable(applicationQueryResponsePayload));

        cpsEmailNotificationProcessor.processDisassociatedEmailNotification(publiceventEnvelope);
        verify(sender, VerificationModeFactory.times(2)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
        assertThat(capturedEvents.get(0).metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
        JsonObject capturedEventPayload = capturedEvents.get(0).payload();
        assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
        assertThat(capturedEventPayload.getString("caseId"),  is(caseId.toString()));

        assertThat(capturedEvents.get(1).metadata().name(), is("progression.command.handler.disassociate-defence-organisation-for-application"));
        capturedEventPayload = capturedEvents.get(1).payload();
        assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
        assertThat(capturedEventPayload.getString("applicationId"),  is(applicationId.toString()));

    }

    @Test
    void shouldCallDisassociationCommandForCaseOnlyWhenNoApplicationFoundForCase() {

        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonObject defencePublicEventPayload = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("organisationId",organisationId.toString())
                .add("caseId", caseId.toString())
                .build();

        final JsonObject applicationQueryResponsePayload = createObjectBuilder()
                .build();

        final JsonEnvelope publiceventEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                defencePublicEventPayload);

        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(ofNullable(applicationQueryResponsePayload));

        cpsEmailNotificationProcessor.processDisassociatedEmailNotification(publiceventEnvelope);
        verify(sender, VerificationModeFactory.times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
        assertThat(capturedEvents.get(0).metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
        JsonObject capturedEventPayload = capturedEvents.get(0).payload();
        assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
        assertThat(capturedEventPayload.getString("caseId"),  is(caseId.toString()));

    }

    @Test
    void shouldCallDisassociationCommandForCaseAndApplicationWhenApplicationForCaseHearingStatusIsSendForListing() {

        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonObject defencePublicEventPayload = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("organisationId",organisationId.toString())
                .add("caseId", caseId.toString())
                .build();

        final JsonObject applicationQueryResponsePayload = createObjectBuilder()
                .add("linkedApplications", createArrayBuilder().add(
                        createObjectBuilder().add("applicationId", applicationId.toString())
                                .build()
                ))
                .build();;

        final JsonEnvelope publiceventEnvelope = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("public.defence.defence-organisation-disassociated"),
                defencePublicEventPayload);

        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(ofNullable(applicationQueryResponsePayload));

        cpsEmailNotificationProcessor.processDisassociatedEmailNotification(publiceventEnvelope);
        verify(sender, VerificationModeFactory.times(2)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
        assertThat(capturedEvents.get(0).metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
        JsonObject capturedEventPayload = capturedEvents.get(0).payload();
        assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
        assertThat(capturedEventPayload.getString("caseId"),  is(caseId.toString()));

        assertThat(capturedEvents.get(1).metadata().name(), is("progression.command.handler.disassociate-defence-organisation-for-application"));
        capturedEventPayload = capturedEvents.get(1).payload();
        assertThat(capturedEventPayload.getString("defendantId"), is(defendantId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), is(organisationId.toString()));
        assertThat(capturedEventPayload.getString("applicationId"),  is(applicationId.toString()));

    }

    private JsonObject getProsecutionCaseResponse(String sampleJson) {
        String response = null;
        try {
            response = Resources.toString(
                    Resources.getResource(sampleJson),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
        }

        return new StringToJsonObjectConverter().convert(response);
    }

    private Optional<DefenceOrganisationVO> buildDefenceOrganisationVO() {
        return Optional.of(DefenceOrganisationVO.builder()
                .postcode("POSTCODE")
                .addressLine1("line1")
                .addressLine2("line2")
                .addressLine3("line3")
                .addressLine4("line4")
                .name("organisation name")
                .phoneNumber("12345668")
                .email("abc@xyz.com").build());
    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithFutureHearings() {

        CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED).withId(randomUUID()).withCourtCentre(courtCentre)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(randomUUID()).withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(Hearings.hearings().withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED).withId(randomUUID()).withCourtCentre(courtCentre).withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build())).build());

        hearings.add(Hearings.hearings().withHearingListingStatus(HearingListingStatus.SENT_FOR_LISTING).withId(randomUUID()).withCourtCentre(courtCentre).build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();

    }
}
