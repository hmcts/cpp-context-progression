package uk.gov.moj.cpp.progression.service;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static uk.gov.justice.core.courts.CaseDocument.caseDocument;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.CourtDocument.courtDocument;
import static uk.gov.justice.core.courts.DocumentCategory.documentCategory;
import static uk.gov.justice.core.courts.HearingDay.hearingDay;
import static uk.gov.justice.progression.courts.GetHearingsAtAGlance.getHearingsAtAGlance;
import static uk.gov.justice.progression.courts.Hearings.hearings;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.utils.PayloadUtil.getPayloadAsJsonObject;

import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsEmailNotificationServiceTest {

    private final String prosecutionCaseSampleWithCourtDocument = "progression.event.court-document-send-to-cps.json";

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @InjectMocks
    private CpsEmailNotificationService cpsEmailNotificationService;

    @Mock
    private HearingVO hearingVOMock;

    @Mock
    private CPSNotificationVO cpsNotificationVO;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private GetHearingsAtAGlance getHearingsAtAGlance;

    @BeforeEach
    public void initMocks() {
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldGetRecentHearingDay() throws Exception {
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(hearingDay().withSittingDay(now().minusDays(7)).build());
        hearingDays.add(hearingDay().withSittingDay(now().minusDays(1)).build());
        hearingDays.add(hearingDay().withSittingDay(now().minusDays(4)).build());

        final ZonedDateTime zonedDateTime = invokeMethod(cpsEmailNotificationService, "getRecentDate", hearingDays);
        assertThat("Earliest hearing day mismatched ", zonedDateTime.toLocalDate(), is(LocalDate.now().minusDays(1)));
    }

    @Test
    public void shouldGetRecentResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithPastHearings();
        final List<Hearings> futureHearings = invokeMethod(cpsEmailNotificationService, "getPastResultedHearings", getHearingAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> recentHearing = invokeMethod(cpsEmailNotificationService, "getRecentHearing", futureHearings);
        assertThat("Recent  date mismatched", recentHearing.get().getValue().toLocalDate().isEqual(now().minusHours(1).toLocalDate()));
    }

    @Test
    public void shouldGetFutureNonResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        assertThat("Hearing size mismatched ", 3, is(getHearingAtAGlance.getHearings().size()));

        final List<Hearings> futureHearings = invokeMethod(cpsEmailNotificationService, "getNonResultedHearing", getHearingAtAGlance);
        assertThat("Future hearing size mismatched ", 2, is(futureHearings.size()));
    }

    @Test
    public void shouldGetEarliestHearingDay() throws Exception {
        List<HearingDay> hearingDays = new ArrayList<>();
        hearingDays.add(HearingDay.hearingDay().withSittingDay(now().plusDays(7)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(now().plusDays(1)).build());
        hearingDays.add(HearingDay.hearingDay().withSittingDay(now().plusDays(4)).build());

        final ZonedDateTime zonedDateTime = invokeMethod(cpsEmailNotificationService, "getEarliestDate", hearingDays);
        assertThat("Earliest hearing day mismatched ", zonedDateTime.toLocalDate(), is(LocalDate.now().plusDays(1)));
    }

    @Test
    public void shouldGetPastResultedHearings() throws Exception {
        final GetHearingsAtAGlance getHearingAtAGlance = getCaseAtAGlanceWithPastHearings();
        assertThat("Hearing size mismatched ", 3, is(getHearingAtAGlance.getHearings().size()));

        final List<Hearings> futureHearings = invokeMethod(cpsEmailNotificationService, "getPastResultedHearings", getHearingAtAGlance);
        assertThat("Future hearing size mismatched ", 2, is(futureHearings.size()));
    }

    @Test
    public void shouldGetEarliestNonResultedHearing() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        final List<Hearings> futureHearings = invokeMethod(cpsEmailNotificationService, "getNonResultedHearing", getHearingsAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> earliestHearing = invokeMethod(cpsEmailNotificationService, "getEarliestHearing", futureHearings);
        assertThat("Earliest date mismatched", earliestHearing.get().getValue().toLocalDate().isEqual(now().minusDays(1).toLocalDate()));
    }

    @Test
    public void shouldGetHearingValueObject() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithNonResultedHearings();
        final List<Hearings> futureHearings = invokeMethod(cpsEmailNotificationService, "getNonResultedHearing", getHearingsAtAGlance);
        final Optional<Map.Entry<UUID, ZonedDateTime>> earliestHearing = invokeMethod(cpsEmailNotificationService, "getEarliestHearing", futureHearings);

        if (earliestHearing.isPresent()) {
            final String hearingDate = earliestHearing.get().getValue().toString();
            final Optional<HearingVO> hearingVOOptional = invokeMethod(cpsEmailNotificationService, "getHearingValueObject", hearingDate, futureHearings, earliestHearing);
            final HearingVO hearingVO = hearingVOOptional.get();
            final ZonedDateTime zonedDateTime = ZonedDateTime.parse(hearingVO.getHearingDate());
            assertThat("Hearing date mismatched", zonedDateTime.toLocalDate().isEqual(now().minusDays(1).toLocalDate()), is(true));
            assertNotNull(hearingVO.getCourtName(), "Court center name should not be empty");
            assertNotNull(hearingVO.getCourtCenterId(), "Court center id should not be empty");
        }
    }

    @Test
    public void shouldPopulateCPSNotificationAndSendEmail() throws Exception {
        final String testCPSEmail = "abc@xyz.com";
        final Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getPayloadAsJsonObject(prosecutionCaseSampleWithCourtDocument));
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();
        final UUID uuid = randomUUID();
        final UUID materialId = randomUUID();
        final String courtDocumentTemplateId = randomUUID().toString();
        final UUID prosecutionCaseId = fromString(prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase").getString("id"));
        final String materialUrl = "http://localhost:8080/material-query-api/query/api/rest/material/material/" + materialId + "?stream=true&requestPdf=true";
        hearingVOMock = HearingVO.builder().hearingDate(now().toString()).courtCenterId(uuid).courtName("testName").build();
        final CourtDocument courtDocument = courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory()
                        .withCaseDocument(caseDocument()
                                .withProsecutionCaseId(prosecutionCaseId)
                                .build())
                        .build())
                .withMaterials(Collections.singletonList(Material.material().withId(materialId).build()))
                .build();

        when(referenceDataService.getOrganisationUnitById(uuid, jsonEnvelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        when(applicationParameters.getCpsCourtDocumentTemplateId()).thenReturn(courtDocumentTemplateId);

        invokeMethod(cpsEmailNotificationService, "populateCPSNotificationAndSendEmail",
                jsonEnvelope, courtDocument, prosecutionCaseJsonOptional.get(), materialUrl, hearingVOMock, prosecutionCaseId, materialId, EmailTemplateType.COURT_DOCUMENT);

        verify(referenceDataService, times(1)).getOrganisationUnitById(uuid, jsonEnvelope, requester);
    }

    @Test
    public void shouldGetHearingDetails() throws Exception {
        final GetHearingsAtAGlance getHearingsAtAGlance = getCaseAtAGlanceWithPastHearings();

        Optional<HearingVO> hearingVO = invokeMethod(cpsEmailNotificationService, "getHearingDetails", getHearingsAtAGlance);

        assertThat("Hearing vo is not null", hearingVO.isPresent(), is(true));

    }

    @Test
    public void shouldGetHearingDetailsWithNullHearingVO() throws Exception {
        Optional<HearingVO> hearingVO = invokeMethod(cpsEmailNotificationService, "getHearingDetails", getHearingsAtAGlance);

        assertThat("Hearing vo is not null", hearingVO.isPresent(), is(false));

    }

    @Test
    public void shouldGetCaseDetailsWithoutDefendantList() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getPayloadAsJsonObject(prosecutionCaseSampleWithCourtDocument));
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


        Optional<CaseVO> caseVOOptional = invokeMethod(cpsEmailNotificationService, "getCaseDetails", prosecutionCaseJsonOptional.get(), courtDocument);


        assertThat("Case details should not be null for sending emailNotification for courtDocument ", caseVOOptional.isPresent(), is(true));

        CaseVO CaseVO = caseVOOptional.get();
        assertThat("Mismatch caseId", "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b", is(CaseVO.getCaseId().toString()));
        assertThat("Mismatch caseURN", "TFL9135196", is(CaseVO.getCaseURN()));
        assertThat("Mismatch defendantList", null, is(CaseVO.getDefendantList()));
    }

    @Test
    public void shouldGetCaseDetailsWithDefendantList() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getPayloadAsJsonObject(prosecutionCaseSampleWithCourtDocument));
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


        Optional<CaseVO> caseVOOptional = invokeMethod(cpsEmailNotificationService, "getCaseDetails", prosecutionCaseJsonOptional.get(), courtDocument);


        assertThat("Case details should not be null for sending emailNotification for courtDocument ", caseVOOptional.isPresent(), is(true));

        CaseVO CaseVO = caseVOOptional.get();
        assertThat("Mismatch caseId", "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b", is(CaseVO.getCaseId().toString()));
        assertThat("Mismatch caseURN", "TFL9135196", is(CaseVO.getCaseURN()));
        assertThat("Mismatch defendantList", "Fred Smith,Norman Blogg", is(CaseVO.getDefendantList()));
    }

    @Test
    public void shouldBuildEmailChannel() throws Exception {
        Optional<JsonObject> prosecutionCaseJsonOptional = Optional.of(getPayloadAsJsonObject(prosecutionCaseSampleWithCourtDocument));
        JsonObject prosecutionCaseJsonObject = prosecutionCaseJsonOptional.get().getJsonObject("prosecutionCase");
        ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJsonObject, ProsecutionCase.class);
        final String defendantCourtDocumentTemplateId = randomUUID().toString();
        final String materialUrl = randomUUID().toString();
        when(applicationParameters.getCpsDefendantCourtDocumentTemplateId()).thenReturn(defendantCourtDocumentTemplateId);

        final UUID courtCenterId = randomUUID();
        final CaseVO caseVO = CaseVO.builder()
                .caseId(prosecutionCase.getId())
                .caseURN("FGR4567")
                .defendantList("Norman Blogg, Henry Smith")
                .build();
        final HearingVO hearingVO = HearingVO.builder()
                .hearingDate(LocalDate.now().toString())
                .courtCenterId(courtCenterId)
                .courtName("Liverpool Crown Court")
                .build();
        final CPSNotificationVO cpsNotificationVO = CPSNotificationVO.builder()
                .caseVO(Optional.of(caseVO))
                .hearingVO(hearingVO)
                .cpsEmailAddress("abc@xyz.com")
                .templateType(EmailTemplateType.COURT_DOCUMENT)
                .build();

        final EmailChannel emailChannel = invokeMethod(cpsEmailNotificationService, "buildEmailChannel", cpsNotificationVO, materialUrl);

        assertThat("Mismatch Case URN", "FGR4567", is(emailChannel.getPersonalisation().getAdditionalProperties().get("URN")));
        assertThat("Mismatch Defendant List", "Norman Blogg, Henry Smith", is(emailChannel.getPersonalisation().getAdditionalProperties().get("defendant_list")));
        assertThat("Mismatch Court Document Template Id", defendantCourtDocumentTemplateId, is(emailChannel.getTemplateId().toString()));
        assertThat("Mismatch Send to Address", "abc@xyz.com", is(emailChannel.getSendToAddress()));


    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithNonResultedHearings() {

        CourtCentre courtCentre = courtCentre().withId(randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = hearingDay().withSittingDay(now().plusDays(3)).build();
        hearingDays.add(hd);
        hd = hearingDay().withSittingDay(now().plusHours(1)).build();
        hearingDays.add(hd);

        hearings.add(hearings().withId(randomUUID()).withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = hearingDay().withSittingDay(now().minusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(hearings().withId(randomUUID()).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(hearings()
                .withId(randomUUID())
                .withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_INITIALISED)
                .withHearingDays(singletonList(
                        hearingDay().withSittingDay(now().plusWeeks(1)).build())).build());

        return getHearingsAtAGlance().withHearings(hearings).build();

    }

    private GetHearingsAtAGlance getCaseAtAGlanceWithPastHearings() {
        CourtCentre courtCentre = courtCentre().withId(randomUUID()).withName("test court name").build();

        List<Hearings> hearings = new ArrayList<>();

        List<HearingDay> hearingDays = new ArrayList<>();


        HearingDay hd = hearingDay().withSittingDay(now().minusDays(2)).build();
        hearingDays.add(hd);
        hd = hearingDay().withSittingDay(now().minusHours(1)).build();
        hearingDays.add(hd);

        hearings.add(hearings().withId(randomUUID()).withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .withHearingDays(hearingDays).build());

        List<HearingDay> hearingDays2 = new ArrayList<>();

        hd = hearingDay().withSittingDay(now().plusDays(1)).build();
        hearingDays.add(hd);

        hearingDays2.add(hd);

        hearings.add(hearings().withId(randomUUID()).withHearingDays(hearingDays2).withCourtCentre(courtCentre).build());

        hearings.add(hearings()
                .withId(randomUUID())
                .withCourtCentre(courtCentre)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .withHearingDays(Collections.singletonList(
                        hearingDay().withSittingDay(now().minusWeeks(1)).build())).build());

        return getHearingsAtAGlance().withHearings(hearings).build();
    }

}