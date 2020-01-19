package uk.gov.moj.cpp.progression.processor;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetCaseAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;
import uk.gov.moj.cpp.progression.value.object.DefendantVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
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
    private GetCaseAtAGlance getCaseAtAGlanceMock;

    @Mock
    private UsersGroupService usersGroupService;

    @Mock
    private ReferenceDataService referenceDataService;

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



    private final String prosecutionCaseSampleWithPersonDefendant = "progression.event.prosecutioncase.persondefendant.cpsnotification.json";
    private final String prosecutionCaseSampleWithLegalEntity = "progression.event.prosecutioncase.legalentity.cpsnotification.json";

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void getCaseDetails() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);

        when(jsonObjectToObjectConverterMock.convert(prosecutionCaseJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);

        Optional<CaseVO> caseVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getCaseDetails", prosecutionCaseJsonOptional);

        assertTrue("Case details should not be null", caseVOOptional.isPresent());

        CaseVO CaseVO = caseVOOptional.get();
        assertEquals("Mismatch caseId","01702930-c1c8-4cfb-8f1c-1df9a58f4e5b" , CaseVO.getCaseId().toString());
        assertEquals("Mismatch caseURN", "TFL9135196", CaseVO.getCaseURN());
    }

    @Test
    public void getDefendantDetailsForPersonDefendant() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);

        when(jsonObjectToObjectConverterMock.convert(prosecutionCaseJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);

        Optional<DefendantVO> defendantVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getDefendantDetails","924cbf53-0b51-4633-9e99-2682be854af4", prosecutionCaseJsonOptional);

        assertTrue("Person Defendant details should not be null", defendantVOOptional.isPresent());

        DefendantVO defendantVO = defendantVOOptional.get();
        assertEquals("Mismatch first name","Fred" , defendantVO.getFirstName());
        assertEquals("Mismatch middle name", "John", defendantVO.getMiddleName());
        assertEquals("Mismatch last name", "Smith", defendantVO.getLastName());
    }

    @Test
    public void getDefendantDetailsForLegalEntityDefendant() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithLegalEntity));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);

        when(jsonObjectToObjectConverterMock.convert(prosecutionCaseJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);

        Optional<DefendantVO> defendantVOOptional = Whitebox
                .invokeMethod(cpsEmailNotificationProcessor, "getDefendantDetails","f9ef2dbf-d205-4444-8059-fefed44111dd", prosecutionCaseJsonOptional);

        assertTrue("LegalEntity Defendant details should not be null", defendantVOOptional.isPresent());

        DefendantVO defendantVO = defendantVOOptional.get();
        assertEquals("Mismatch legal entity name","ABC LTD" , defendantVO.getLegalEntityName());
    }

    @Test
    public void getFutureHearings() throws Exception{
        final GetCaseAtAGlance getCaseAtAGlance = getCaseAtAGlanceWithFutureHearings();
        Assert.assertEquals("Hearing size mismatched ",3 ,getCaseAtAGlance.getHearings().size());

        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getCaseAtAGlance);
        Assert.assertEquals("Future hearing size mismatched ",2 ,futureHearings.size());
  }

    @Test
    public void getEarliestHearing() throws Exception{
        final GetCaseAtAGlance getCaseAtAGlance = getCaseAtAGlanceWithFutureHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getCaseAtAGlance);
        final Optional<Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearing", futureHearings);
        Assert.assertTrue("Earliest date mismatched",earliestHearing.get().getValue().toLocalDate().isEqual(LocalDate.now().plusDays(1)));
    }

    @Test
    public void getCPSEmail() throws Exception{
         final UUID courtCenterId = UUID.randomUUID();
         final String testCPSEmail = "abc@xyz.com";
         final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress",testCPSEmail).build();

         when(referenceDataService.getOrganisationUnitById(courtCenterId, jsonEnvelope)).thenReturn(Optional.of(sampleJsonObject));
         Optional<String> cpsEmailOptional = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getCPSEmail", jsonEnvelope, courtCenterId);
         Assert.assertEquals("CPSEmail is mismatched", testCPSEmail, cpsEmailOptional.get());
   }

    @Test
    public void getEarliestHearingDay() throws Exception{
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(3)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build());

        final ZonedDateTime zonedDateTime = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearingDay", hearingDays);
        Assert.assertEquals("Earliest hearing day mismatched ",zonedDateTime.toLocalDate(), LocalDate.now().plusDays(1));
    }

    @Test
    public void getHearingVO() throws Exception{
        final GetCaseAtAGlance getCaseAtAGlance = getCaseAtAGlanceWithFutureHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getFutureHearings", getCaseAtAGlance);
        final Optional<Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getEarliestHearing", futureHearings);

        if(earliestHearing.isPresent()) {
            final String hearingDate = earliestHearing.get().getValue().toString();
            final Optional<HearingVO> hearingVOOptional = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getHearingVO",hearingDate, futureHearings, earliestHearing);
            final HearingVO hearingVO = hearingVOOptional.get();
            final ZonedDateTime zonedDateTime = ZonedDateTime.parse(hearingVO.getHearingDate());
            Assert.assertTrue("Hearing date mismatched", zonedDateTime.toLocalDate().isEqual(LocalDate.now().plusDays(1)));
            Assert.assertNotNull("Court center name should not be empty", hearingVO.getCourtName());
            Assert.assertNotNull("Court center id should not be empty", hearingVO.getCourtCenterId());
        }
    }

    @Test
    public void getDefendantJson() throws Exception{
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        final UUID defendantId = UUID.fromString("924cbf53-0b51-4633-9e99-2682be854af4");

        final JsonObject resultJsonObject = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getDefendantJson", prosecutionCaseJsonObject, defendantId);
        Assert.assertEquals("Defendant is mismatched", defendantId.toString(), resultJsonObject.getString("id"));
    }

    @Test
    public void populateCPSNotificationAndSendEmail() throws Exception{
        final String testCPSEmail = "abc@xyz.com";
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final UUID defendantId = UUID.fromString("924cbf53-0b51-4633-9e99-2682be854af4");
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress",testCPSEmail).build();
        final UUID uuid = UUID.randomUUID();
        hearingVOMock = HearingVO.builder().hearingDate(ZonedDateTime.now().toString()).courtCenterId(uuid).courtName("testName").build();

        when(referenceDataService.getOrganisationUnitById(uuid, jsonEnvelope)).thenReturn(Optional.of(sampleJsonObject));
        when(usersGroupService.getDefenceOrganisationDetails(uuid, jsonEnvelope.metadata())).thenReturn(buildDefenceOrganisationVO());
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);

        Whitebox.invokeMethod(cpsEmailNotificationProcessor, "populateCPSNotificationAndSendEmail",
                jsonEnvelope, defendantId.toString(), uuid, prosecutionCaseJsonOptional, hearingVOMock, EmailTemplateType.INSTRUCTION);

        verify(referenceDataService,times(1)).getOrganisationUnitById(uuid, jsonEnvelope);
        verify(usersGroupService,times(1)).getDefenceOrganisationDetails(uuid, jsonEnvelope.metadata());
    }

    @Test
    public void populateCPSNotification() throws Exception{
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final JsonObject caseAtAGlanceJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("caseAtAGlance");
        final UUID randomUUID = UUID.randomUUID();

        jsonObject =  createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString())).thenReturn(prosecutionCaseJsonOptional);
        when(usersGroupService.getDefenceOrganisationDetails(randomUUID, jsonEnvelope.metadata())).thenReturn(buildDefenceOrganisationVO());
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        when(jsonObjectToObjectConverterMock.convert(caseAtAGlanceJsonObject, GetCaseAtAGlance.class)).thenReturn(getCaseAtAGlanceMock);

        Whitebox.invokeMethod(cpsEmailNotificationProcessor, "populateCPSNotification", jsonEnvelope, jsonObject, EmailTemplateType.INSTRUCTION);

        verify(progressionService,times(1)).getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString());
    }

    @Test
    public void getHearingDetailsWithNullHearingVO() throws Exception{
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithPersonDefendant));
        final String testCPSEmail = "abc@xyz.com";

        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress",testCPSEmail).build();
        final UUID randomUUID = UUID.randomUUID();

        jsonObject =  createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString())).thenReturn(prosecutionCaseJsonOptional);
        when(referenceDataService.getOrganisationUnitById(randomUUID, jsonEnvelope)).thenReturn(Optional.of(sampleJsonObject));
        when(usersGroupService.getDefenceOrganisationDetails(randomUUID, jsonEnvelope.metadata())).thenReturn(buildDefenceOrganisationVO());
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        when(jsonObjectToObjectConverterMock.convert(payload, GetCaseAtAGlance.class)).thenReturn(getCaseAtAGlanceMock);

        Optional<HearingVO> hearingVO = Whitebox.invokeMethod(cpsEmailNotificationProcessor, "getHearingDetails", prosecutionCaseJsonOptional);

        Assert.assertTrue("Hearing vo is not null", !hearingVO.isPresent());

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

    private Optional<DefenceOrganisationVO> buildDefenceOrganisationVO(){
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

  private GetCaseAtAGlance getCaseAtAGlanceWithFutureHearings(){

        CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(2)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withCourtCentre(courtCentre)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(Hearings.hearings().withId(UUID.randomUUID()).withCourtCentre(courtCentre).withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build())).build());

        return GetCaseAtAGlance.getCaseAtAGlance().withHearings(hearings).build();

    }
}
