package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.LjaDetails.ljaDetails;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferralReason;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferringJudicialDecision;
import uk.gov.justice.core.courts.SjpReferral;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.Country;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningDataAggregatorFactory;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningEnglishDataAggregator;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningGenerationService;
import uk.gov.moj.cpp.progression.service.disqualificationreferral.ReferralDisqualifyWarningWelshDataAggregator;
import uk.gov.moj.cpp.progression.service.utils.PdfHelper;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class ReferralDisqualificationWarningTest {

    public static final UUID APPLICATION_DOCUMENT_TYPE_ID = UUID.fromString("460fbe94-c002-11e8-a355-529269fb1459");
    final private UUID prosecutionCaseId = randomUUID();
    final private UUID defendantId = randomUUID();
    final private UUID offenceId = randomUUID();
    final private UUID referralReasonId = randomUUID();
    final private String ouCode = "B01FA00";
    final private String ljaCode = "2578";
    final private UUID courtCenterId = UUID.randomUUID();
    final private UUID applicationId = null;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Captor
    ArgumentCaptor<JsonObject> disqualificationWarningContentArgumentCaptor;
    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Sender sender;
    @Mock
    private NotificationService notificationService;
    @Mock
    private FileStorer fileStorer;
    @Mock
    private DocumentGeneratorService documentGeneratorService;
    @Mock
    private PdfHelper pdfHelper;
    @Mock
    private Requester requester;
    @InjectMocks
    private ReferralDisqualifyWarningGenerationService referralDisqualifyWarningGenerationService;
    @Mock
    private RefDataService referenceDataService;
    @Mock
    private DocumentGeneratorClient documentGeneratorClient;
    @Mock
    private JsonEnvelope originatingEnvelope;
    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;
    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    @Mock
    private ReferralDisqualifyWarningDataAggregatorFactory dataAggregatorFactory;
    @Mock
    private ReferralDisqualifyWarningEnglishDataAggregator referralDisqualifyWarningEnglishDataAggregator;
    @Mock
    private ReferralDisqualifyWarningWelshDataAggregator referralDisqualifyWarningWelshDataAggregator;
    private final CourtCentre courtCentre = CourtCentre.courtCentre().withName("Test Court Centre").withAddress(Address.address()
            .withAddress1("Test Address 1")
            .withAddress2("Test Address 2")
            .withAddress3("Test Address 3")
            .withPostcode("AS1 1DF").build()).build();

    private static Optional<JsonObject> buildDocumentTypeDataWithRBAC(final String documentCategory) {
        return Optional.ofNullable(Json.createObjectBuilder().add("section", "orders & notices")
                .add("documentCategory", "documentCategory")
                .add("documentTypeDescription", "Applications")
                .add("documentTypeId", documentCategory)
                .add("", "")
                .add("mimeType", "application/pdf")
                .add("courtDocumentTypeRBAC",
                        Json.createObjectBuilder()
                                .add("uploadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer").build()).build())
                                .add("readUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build())
                                .add("downloadUserGroups", createArrayBuilder().add(buildUserGroup("Listing Officer")).add(buildUserGroup("Magistrates")).build()).build())
                .add("seqNum", 10)
                .build());
    }

    private static JsonObjectBuilder buildUserGroup(final String userGroupName) {
        return Json.createObjectBuilder().add("cppGroup", Json.createObjectBuilder().add("id", randomUUID().toString()).add("groupName", userGroupName));
    }

    @Test
    public void shouldGenerateEnglishDocument() throws Exception {
        final byte[] documentData = {34, 56, 78, 90};
        final UUID systemUserId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();

        final String englishPayload = getDocPayloadString(ljaDetails, "referral-disqualify-warning-english-parameters.json");
        final JsonObject docEnglishPayload = stringToJsonObjectConverter.convert(englishPayload);

        final CourtCentre courtCentre = getCourtCentre();
        final CourtReferral courtReferral = getCourtReferral();
        final String courtHouseCode = courtReferral.getSjpReferral().getReferringJudicialDecision().getCourtHouseCode();
        final Optional<JsonObject> documentTypeData = buildDocumentTypeDataWithRBAC("documentCategory");

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtReferral);
        final JsonObject courtReferralPayload = createObjectBuilder().add("courtReferral", jsonObject).build();
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("progression.event.cases-referred-to-court"),
                jsonObject);

        when(originatingEnvelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("progression.event.cases-referred-to-court").withUserId(randomUUID().toString()).build());
        when(referenceDataService.getCourtByCourtHouseOUCode(any(), any(), any())).thenReturn(courtCentre);
        when(referenceDataService.getLjaDetails(any(), any(), any())).thenReturn(ljaDetails.get());
        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(documentTypeData);
        when(dataAggregatorFactory.getAggregator(Locale.ENGLISH)).thenReturn(referralDisqualifyWarningEnglishDataAggregator);
        when(referralDisqualifyWarningEnglishDataAggregator.aggregateReferralDisqualifyWarningData(any(), any(), any(), any())).thenReturn(docEnglishPayload);
        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(documentData);
        when(documentGeneratorClient.generatePdfDocument(docEnglishPayload, "NPE_RefferalDisqualificationWarning", systemUserId)).thenReturn(documentData);
        when(documentGeneratorService.generatePdfDocument(any(), any(), any())).thenReturn(materialId);

        referralDisqualifyWarningGenerationService.generateReferralDisqualifyWarning(originatingEnvelope, "caseUrn", caseId, getDefendant(), courtHouseCode);

        verify(documentGeneratorClient, times(2)).generatePdfDocument(disqualificationWarningContentArgumentCaptor.capture(), anyString(), any(UUID.class));

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.create-court-document"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(anyOf( withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(caseId)),
                withJsonPath("$.courtDocument.section", equalTo("orders & notices")),
                withJsonPath("$.documentCategory", equalTo("documentCategory")),
                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf")))));
        verify(notificationService).sendLetter(Mockito.eq(originatingEnvelope), Mockito.any(UUID.class), Mockito.eq(caseId), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.eq(true));

    }

    @Test
    public void shouldGenerateWelshDocument() throws Exception {
        final byte[] englishDocumentData = {34, 56, 78, 90};
        final byte[] emptypage = {1};
        final byte[] welshDocumentData = {33, 44, 55, 66};
        final byte[] documentData = {33, 44, 55, 66, 1, 34, 56, 78, 90};

        final UUID systemUserId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final Optional<LjaDetails> ljaDetails = getLjaDetails();

        String englishPayload = getDocPayloadString(ljaDetails, "referral-disqualify-warning-english-parameters.json");
        final JsonObject docEnglishPayload = stringToJsonObjectConverter.convert(englishPayload);

        String welshPayload = getDocPayloadString(ljaDetails, "referral-disqualify-warning-welsh-parameters.json");
        final JsonObject docWelshPayload = stringToJsonObjectConverter.convert(welshPayload);

        final CourtCentre courtCentre = getCourtCentre();
        final CourtReferral courtReferral = getCourtReferral();
        final String courtHouseCode = courtReferral.getSjpReferral().getReferringJudicialDecision().getCourtHouseCode();

        final Optional<JsonObject> documentTypeData = buildDocumentTypeDataWithRBAC("documentCategory");

        final JsonObject jsonObject = objectToJsonObjectConverter.convert(courtReferral);
        final JsonObject courtReferralPayload = createObjectBuilder().add("courtReferral", jsonObject).build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("progression.event.cases-referred-to-court"),
                jsonObject);

        when(originatingEnvelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("progression.event.cases-referred-to-court").withUserId(randomUUID().toString()).build());
        when(referenceDataService.getCourtByCourtHouseOUCode(any(), any(), any())).thenReturn(courtCentre);
        when(referenceDataService.getLjaDetails(any(), any(), any())).thenReturn(ljaDetails.get());
        when(referenceDataService.getDocumentTypeAccessData(any(), any(), any())).thenReturn(documentTypeData);
        when(dataAggregatorFactory.getAggregator(Locale.ENGLISH)).thenReturn(referralDisqualifyWarningEnglishDataAggregator);
        when(referralDisqualifyWarningEnglishDataAggregator.aggregateReferralDisqualifyWarningData(any(), any(), any(), any())).thenReturn(docEnglishPayload);

        final Locale WELSH_LOCALE = new Locale("cy");
        when(referralDisqualifyWarningEnglishDataAggregator.aggregateReferralDisqualifyWarningData(any(), any(), any(), any())).thenReturn(docWelshPayload);

        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));
        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(englishDocumentData);
        //when(fileStorer.store(any(), any())).thenReturn(randomUUID());

        when(documentGeneratorClient.generatePdfDocument(any(), any(), any())).thenReturn(welshDocumentData);
        //when(fileStorer.store(any(), any())).thenReturn(randomUUID());


        when(documentGeneratorService.generatePdfDocument(any(), any(), any())).thenReturn(materialId);

        referralDisqualifyWarningGenerationService.generateReferralDisqualifyWarning(originatingEnvelope, "caseUrn", caseId, getDefendant(), courtHouseCode);

        verify(documentGeneratorClient, times(2)).generatePdfDocument(disqualificationWarningContentArgumentCaptor.capture(), anyString(), any(UUID.class));

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.create-court-document"));
        assertThat(envelopeArgumentCaptor.getValue().payload(), notNullValue());
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(anyOf( withJsonPath("$.courtDocument.documentCategory.defendantDocument.prosecutionCaseId", equalTo(caseId)),
                withJsonPath("$.courtDocument.section", equalTo("orders & notices")),
                withJsonPath("$.documentCategory", equalTo("documentCategory")),
                withJsonPath("$.courtDocument.documentTypeId", equalTo(APPLICATION_DOCUMENT_TYPE_ID.toString())),
                withJsonPath("$.courtDocument.documentTypeDescription", equalTo("Applications")),
                withJsonPath("$.courtDocument.mimeType", equalTo("application/pdf")))));

        verify(notificationService).sendLetter(Mockito.eq(originatingEnvelope), Mockito.any(UUID.class), Mockito.eq(caseId), Mockito.eq(applicationId), Mockito.eq(materialId), Mockito.eq(true));

    }

    private String getDocPayloadString(final Optional<LjaDetails> ljaDetails, final String resoureName) throws IOException {
        String inputPayload = Resources.toString(getResource(resoureName), defaultCharset());
        inputPayload = inputPayload.replaceAll("caseurn", "caseurn")
                .replace("ljaCode", ljaDetails.get().getLjaCode())
                .replace("ljaName", ljaDetails.get().getLjaName())
                .replace("courtCentreName", courtCentre.getName())
                .replace("postCode", "HP17 8SD");
        return inputPayload;
    }

    private CourtReferral getCourtReferral() {
        final String referralReason = "For disqualification";
        return CourtReferral.courtReferral().withSjpReferral(getSjpReferral())
                .withProsecutionCases(asList(getProsecutionCase()))
                .withListHearingRequests(asList(getListHearingRequest(""))).build();
    }

    private Optional<LjaDetails> getLjaDetails() {
        return of(ljaDetails().withLjaCode(ljaCode).withLjaName("ljaName").withWelshLjaName("welshLjaName").build());
    }

    private CourtCentre getCourtCentre() {
        return CourtCentre.courtCentre().withId(courtCenterId).withCode(ouCode).withName("courtCentreName").withLja(LjaDetails.ljaDetails().withLjaCode(ljaCode).build()).build();
    }

    private String getCountry() {
        return Country.WALES.toString();
    }

    private ReferredDefendant getDefendant() {
        final Address address = Address.address().withAddress1("one")
                .withAddress2("two")
                .withAddress3("three")
                .withAddress4("four")
                .withAddress5("five")
                .withPostcode("HP17 8SD").build();

        final ReferredPerson person = ReferredPerson.referredPerson()
                .withFirstName("firstname")
                .withLastName("lastname")
                .withMiddleName("middlename")
                .withDateOfBirth(LocalDate.of(1998, 8, 10))
                .withAddress(address).build();

        final ReferredPersonDefendant personDefendant = ReferredPersonDefendant.referredPersonDefendant()
                .withPersonDetails(person).build();

        return ReferredDefendant.referredDefendant().withId(defendantId)
                .withMasterDefendantId(randomUUID())
                .withProsecutionCaseId(prosecutionCaseId)
                .withPersonDefendant(personDefendant).build();
    }


    private SjpReferral getSjpReferral() {
        final SjpReferral sjpReferral = SjpReferral.sjpReferral()
                .withNoticeDate(LocalDate.of(2018, 01, 01))
                .withReferralDate(LocalDate.of(2018, 02, 15))
                .withReferringJudicialDecision(ReferringJudicialDecision.referringJudicialDecision().withCourtHouseCode("courtHoseCode").build()).build();
        return sjpReferral;
    }

    private ListHearingRequest getListHearingRequest(final String referralReason) {
        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withHearingType(HearingType.hearingType().withId(UUID.randomUUID()).build())
                .withEstimateMinutes(Integer.valueOf(15))
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(prosecutionCaseId)
                        .withDefendantOffences(asList(offenceId))
                        .withReferralReason(ReferralReason.referralReason().withDefendantId(defendantId)
                                .withId(referralReasonId)
                                .withDescription(referralReason).build())
                        .build()))
                .build();
        return listHearingRequest;
    }

    private ProsecutionCase getProsecutionCase() {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode("CPS")
                        .withCaseURN("caseURN").build())
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address().withPostcode("CR11111").build()).build()).build())
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();
    }

}
