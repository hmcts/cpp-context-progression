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
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import javax.json.Json;
import javax.json.JsonObject;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentNotifiedProcessorTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private CourtDocumentNotifiedProcessor courtDocumentNotifiedProcessor;

    @Mock
    private JsonObject jsonObject;

    @Mock
    private JsonObject payload;

    @Mock
    private GetHearingsAtAGlance getHearingsAtAGlance;

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

    @Mock
    private Requester requester;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    private final String prosecutionCaseSampleWithCourtDocument = "progression.event.court-document-send-to-cps.json";
    private  static final String HEARINGS_AT_A_GLANCE = "hearingsAtAGlance";



    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGetFutureNonResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        assertThat("Hearing size mismatched ", 3, is(getHearingAtAGlance.getHearings().size()));

        final List<Hearings> futureHearings = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getNonResultedHearing", getHearingAtAGlance);
        assertThat("Future hearing size mismatched ", 2, is(futureHearings.size()));
    }

    @Test
    public void shouldGetEarliestNonResultedHearing() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getNonResultedHearing", getHearingsAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getEarliestHearing", futureHearings);
        assertThat("Earliest date mismatched", earliestHearing.get().getValue().toLocalDate().isEqual(ZonedDateTime.now().minusDays(1).toLocalDate()));
    }
    @Test
    public void shouldGetEarliestHearingDay() throws Exception {
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(7)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(4)).build());

        final ZonedDateTime zonedDateTime = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getEarliestDate", hearingDays);
        assertThat("Earliest hearing day mismatched ", zonedDateTime.toLocalDate(), is(LocalDate.now().plusDays(1)));
    }

    @Test
    public void shouldGetPastResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithPastHearings();
        assertThat("Hearing size mismatched ", 3, is(getHearingAtAGlance.getHearings().size()));

        final List<Hearings> futureHearings = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getPastResultedHearings", getHearingAtAGlance);
        assertThat("Future hearing size mismatched ", 2, is(futureHearings.size()));
    }

    @Test
    public void shouldGetRecentResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithPastHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getPastResultedHearings", getHearingAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> recentHearing = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getRecentHearing", futureHearings);
        assertThat("Recent  date mismatched", recentHearing.get().getValue().toLocalDate().isEqual(ZonedDateTime.now().minusHours(1).toLocalDate()));
    }

    @Test
    public void shouldGetRecentHearingDay() throws Exception {
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(7)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(4)).build());

        final ZonedDateTime zonedDateTime = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getRecentDate", hearingDays);
        assertThat("Earliest hearing day mismatched ", zonedDateTime.toLocalDate(), is(LocalDate.now().minusDays(1)));
    }

    @Test
    public void shouldGetHearingValueObject() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        final List<Hearings> futureHearings = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getNonResultedHearing", getHearingsAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> earliestHearing = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getEarliestHearing", futureHearings);

        if (earliestHearing.isPresent()) {
            final String hearingDate = earliestHearing.get().getValue().toString();
            final Optional<HearingVO> hearingVOOptional = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getHearingValueObject", hearingDate, futureHearings, earliestHearing);
            final HearingVO hearingVO = hearingVOOptional.get();
            final ZonedDateTime zonedDateTime = ZonedDateTime.parse(hearingVO.getHearingDate());
            Assert.assertThat("Hearing date mismatched", zonedDateTime.toLocalDate().isEqual(ZonedDateTime.now().minusDays(1).toLocalDate()), is(true));
            Assert.assertNotNull("Court center name should not be empty", hearingVO.getCourtName());
            Assert.assertNotNull("Court center id should not be empty", hearingVO.getCourtCenterId());
        }
    }

    @Test
    public void shouldGetCaseDetailsWithoutDefendantList() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);
        final UUID materialId = randomUUID();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(prosecutionCase.getId())
                                .build())
                        .build())
                .withMaterials(Collections.singletonList(Material.material().withId(materialId).build()))
                .build();


        when(jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);



        Optional<CaseVO> caseVOOptional = Whitebox
                .invokeMethod(courtDocumentNotifiedProcessor, "getCaseDetails", prosecutionCaseJsonOptional, courtDocument);


        assertThat("Case details should not be null for sending emailNotification for courtDocument ", caseVOOptional.isPresent(), is(true));

        CaseVO CaseVO = caseVOOptional.get();
        assertThat("Mismatch caseId", "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b", is(CaseVO.getCaseId().toString()));
        assertThat("Mismatch caseURN", "TFL9135196", is(CaseVO.getCaseURN()));
        assertThat("Mismatch defendantList", null, is(CaseVO.getDefendantList()));
    }

    @Test
    public void shouldGetCaseDetailsWithDefendantList() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);
        final UUID defendantId1 = fromString("924cbf53-0b51-4633-9e99-2682be854af4");
        final UUID defendantId2 = fromString("924cbf53-0b51-4632-9e99-2682be854af4");
        final UUID materialId = randomUUID();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withDefendantDocument(DefendantDocument.defendantDocument()
                                .withDefendants(Collections.unmodifiableList(Arrays.asList(defendantId1, defendantId2)))
                                .withProsecutionCaseId(prosecutionCase.getId()).build())
                                .build())
                .withMaterials(Collections.singletonList(Material.material().withId(materialId).build()))
                .build();


        when(jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class)).thenReturn(prosecutionCase);



        Optional<CaseVO> caseVOOptional = Whitebox
                .invokeMethod(courtDocumentNotifiedProcessor, "getCaseDetails", prosecutionCaseJsonOptional, courtDocument);


        assertThat("Case details should not be null for sending emailNotification for courtDocument ", caseVOOptional.isPresent(), is(true));

        CaseVO CaseVO = caseVOOptional.get();
        assertThat("Mismatch caseId", "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b", is(CaseVO.getCaseId().toString()));
        assertThat("Mismatch caseURN", "TFL9135196", is(CaseVO.getCaseURN()));
        assertThat("Mismatch defendantList", "Fred Smith,Norman Blogg", is(CaseVO.getDefendantList()));
    }

    @Test
    public void shouldBuildEmailChannel() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);
        final String defendantCourtDocumentTemplateId = randomUUID().toString();
        when(applicationParameters.getCpsDefendantCourtDocumentTemplateId()).thenReturn(defendantCourtDocumentTemplateId);

        final UUID courtCenterId = randomUUID();
        final CaseVO caseVO = CaseVO.builder()
                .caseId(prosecutionCase.getId())
                .caseURN( "FGR4567")
                .defendantList("Norman Blogg, Henry Smith")
                .build();
        final HearingVO hearingVO =HearingVO.builder()
                .hearingDate(LocalDate.now().toString())
                .courtCenterId(courtCenterId)
                .courtName("Liverpool Crown Court")
                .build();
        final CPSNotificationVO  cpsNotificationVO =CPSNotificationVO.builder()
                .caseVO(Optional.of(caseVO))
                .hearingVO(hearingVO)
                .cpsEmailAddress("abc@xyz.com")
                .templateType(EmailTemplateType.COURT_DOCUMENT)
                .build();

        final EmailChannel emailChannel = Whitebox
                .invokeMethod(courtDocumentNotifiedProcessor, "buildEmailChannel", cpsNotificationVO);

        assertThat("Mismatch Case URN", "FGR4567", is(emailChannel.getPersonalisation().getAdditionalProperties().get("URN")));
        assertThat("Mismatch Defendant List", "Norman Blogg, Henry Smith", is(emailChannel.getPersonalisation().getAdditionalProperties().get("defendant_list")));
        assertThat("Mismatch Court Document Template Id", defendantCourtDocumentTemplateId, is(emailChannel.getTemplateId().toString()));
        assertThat("Mismatch Send to Address", "abc@xyz.com", is(emailChannel.getSendToAddress()));


    }

    @Test
    public void shouldGetHearingDetailsWithNullHearingVO() throws Exception {
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        final String testCPSEmail = "abc@xyz.com";

        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final UUID randomUUID = UUID.randomUUID();

        jsonObject = createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString())).thenReturn(prosecutionCaseJsonOptional);
        when(referenceDataService.getOrganisationUnitById(randomUUID, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        when(jsonObjectConverter.convert(jsonObject, GetHearingsAtAGlance.class)).thenReturn(getHearingsAtAGlance);

        Optional<HearingVO> hearingVO = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getHearingDetails", getHearingsAtAGlance);

        assertThat("Hearing vo is not null", hearingVO.isPresent(), is(false));

    }

    @Test
    public void shouldGetHearingDetails() throws Exception {
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        final String testCPSEmail = "abc@xyz.com";

        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final UUID randomUUID = UUID.randomUUID();

        jsonObject = createObjectBuilder().add("caseId", randomUUID.toString())
                .add("defendantId", randomUUID.toString())
                .add("organisationId", randomUUID.toString()).build();
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithPastHearings();

        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, randomUUID.toString())).thenReturn(prosecutionCaseJsonOptional);
        when(referenceDataService.getOrganisationUnitById(randomUUID, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        when(jsonObjectConverter.convert(jsonObject, GetHearingsAtAGlance.class)).thenReturn(getHearingsAtAGlance);

        Optional<HearingVO> hearingVO = Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "getHearingDetails", getHearingsAtAGlance);

        assertThat("Hearing vo is not null", hearingVO.isPresent(), is(true));

    }


    @Test
    public void shouldPopulateCPSNotificationAndSendEmail() throws Exception {
        final String testCPSEmail = "abc@xyz.com";
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final UUID uuid = randomUUID();
        final UUID materialId = randomUUID();
        final String courtDocumentTemplateId = randomUUID().toString();
        final UUID prosecutionCaseId = fromString(prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase").getString("id"));
        final String materialUrl = "http://localhost:8080/material-query-api/query/api/rest/material/material/" + materialId.toString() + "?stream=true&requestPdf=true";
        hearingVOMock = HearingVO.builder().hearingDate(ZonedDateTime.now().toString()).courtCenterId(uuid).courtName("testName").build();
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(prosecutionCaseId)
                                .build())
                        .build())
                .withMaterials(Collections.singletonList(Material.material().withId(materialId).build()))
                .build();

        when(referenceDataService.getOrganisationUnitById(uuid, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        when(applicationParameters.getCpsCourtDocumentTemplateId()).thenReturn(courtDocumentTemplateId);
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);

        Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "populateCPSNotificationAndSendEmail",
                jsonEnvelope, courtDocument, prosecutionCaseJsonOptional, materialUrl, hearingVOMock, prosecutionCaseId, materialId, EmailTemplateType.COURT_DOCUMENT);

        verify(referenceDataService, times(1)).getOrganisationUnitById(uuid, jsonEnvelope, requester);
    }

    @Test
    public void shouldProcessCourtDocumentSendToCPS() throws Exception {
        final String testCPSEmail = "abc@xyz.com";
        final UUID materialId = randomUUID();
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final String courtDocumentTemplateId = randomUUID().toString();
        final String materialUrl = "http://localhost:8080/material-query-api/query/api/rest/material/material/" + materialId.toString() + "?stream=true&requestPdf=true";
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithPastHearings();
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getProsecutionCaseResponse(prosecutionCaseSampleWithCourtDocument));
        final String prosecutionCaseId = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase").getString("id");
        final CourtDocument courtDocument = CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(DocumentCategory.documentCategory()
                        .withCaseDocument(CaseDocument.caseDocument()
                                .withProsecutionCaseId(fromString(prosecutionCaseId))
                                .build())
                        .build())
                .withMaterials(Collections.singletonList(Material.material().withId(materialId).build()))
                .build();
        final JsonObject courtDocumentJsonObject = Json.createObjectBuilder()
                .build();
        final UUID courtCenterId = getHearingsAtAGlance.getHearings().get(0).getCourtCentre().getId();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);
        when(payload.getJsonObject("courtDocument")).thenReturn(courtDocumentJsonObject);
        when(jsonObjectConverter.convert(courtDocumentJsonObject, CourtDocument.class)).thenReturn(courtDocument);
        when(materialUrlGenerator.pdfFileStreamUrlFor(materialId)).thenReturn(materialUrl);
        when(progressionService.getProsecutionCaseDetailById(jsonEnvelope, prosecutionCaseId)).thenReturn(prosecutionCaseJsonOptional);

        when(referenceDataService.getOrganisationUnitById(courtCenterId, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        doNothing().when(notificationService).sendCPSNotification(jsonEnvelope, cpsNotificationVO);
        when(jsonObjectConverter.convert(prosecutionCaseJsonOptional.get().getJsonObject(HEARINGS_AT_A_GLANCE), GetHearingsAtAGlance.class)).thenReturn(getHearingsAtAGlance);
        when(applicationParameters.getCpsCourtDocumentTemplateId()).thenReturn(courtDocumentTemplateId);
        Whitebox.invokeMethod(courtDocumentNotifiedProcessor, "processCourtDocumentSendToCPS", jsonEnvelope);
        verify(referenceDataService, times(1)).getOrganisationUnitById(courtCenterId, jsonEnvelope, requester);

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



    private GetHearingsAtAGlance getCaseAtAGlanceWithPastHearings() {
        CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(2)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusHours(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(randomUUID()).withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(randomUUID()).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusWeeks(1)).build())).build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();
    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithNonResultedHearings() {

        CourtCentre courtCentre = CourtCentre.courtCentre().withId(randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusDays(3)).build();
        hearingDays.add(hd);
        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusHours(1)).build();
        hearingDays.add(hd);

        hearings.add(Hearings.hearings().withId(randomUUID()).withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(Hearings.hearings().withId(randomUUID()).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(Hearings.hearings()
                .withId(randomUUID())
                .withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .withHearingDays(Collections.singletonList(
                HearingDay.hearingDay().withSittingDay(ZonedDateTime.now().plusWeeks(1)).build())).build());

        return GetHearingsAtAGlance.getHearingsAtAGlance().withHearings(hearings).build();

    }


}
