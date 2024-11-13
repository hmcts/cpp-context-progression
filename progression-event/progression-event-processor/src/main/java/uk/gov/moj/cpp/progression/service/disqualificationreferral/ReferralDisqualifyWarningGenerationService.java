package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.progression.Country.WALES;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.DocumentTypeRBAC;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.Country;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.utils.PdfHelper;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferralDisqualifyWarningGenerationService {

    public static final String ENGLISH_TEMPLATE_NAME = "NPE_RefferalDisqualificationWarning";
    public static final String TEMPLATE_IDENTIFIER_EMPTY_PAGE = "EmptyPage";
    public static final String WELSE_TEMPLATE_NAME = "NPB_RefferalDisqualificationWarning";
    public static final UUID ORDERS_NOTICES_AND_DIRECTION = UUID.fromString("460fbe94-c002-11e8-a355-529269fb1459");
    public static final String COURT_DOCUMENT_TYPE_RBAC = "courtDocumentTypeRBAC";
    public static final String READ_USER_GROUPS = "readUserGroups";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferralDisqualifyWarningGenerationService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String UPLOAD_ACCESS = "uploadUserGroups";
    private static final String READ_ACCESS = "readUserGroups";
    private static final String DOWNLOAD_ACCESS = "downloadUserGroups";
    private static final String DELETE_ACCESS = "deleteUserGroups";


    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ReferralDisqualifyWarningDataAggregatorFactory dataAggregatorFactory;

    @Inject
    private NotificationService notificationService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private PdfHelper pdfHelper;

    public void generateReferralDisqualifyWarning(final JsonEnvelope event, final String caseUrn, final UUID caseId, final ReferredDefendant defendant, final String courtHouseCode) throws IOException {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            final String filename = format("%s_REFERRAL_DISQUALIFICATION_WARNING_%s.pdf", caseUrn, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
            final Optional<UUID> materialId = processPdfDocument(event, caseUrn, defendant, courtHouseCode, filename);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("materialId generated from document generator service - {}", materialId);
            }
            if(materialId.isPresent()) {
                generateCourtDocument(event, caseId, defendant, filename, materialId.get());
                notificationService.sendLetter(event, UUID.randomUUID(), caseId, null, materialId.get(), true, null);
            } else {
                LOGGER.warn("ProcessPDF document getSupportingData is empty as courtHouseCode is not present ");
            }
        }
    }

    private void generateCourtDocument(final JsonEnvelope event, final UUID caseId, final ReferredDefendant defendant, final String filename, final UUID materialId) {
        final CourtDocument courtDocument = courtDocument(event, asList(defendant.getId()), materialId, caseId, filename);
        final JsonObject courtDocumentPayload = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

        LOGGER.info("creating ReferralDisqualifyWarning court document payload - {}", courtDocumentPayload);
        sender.send(envelop(courtDocumentPayload).withName(PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).withMetadataFrom(event));
    }

    private Optional<UUID> processPdfDocument(final JsonEnvelope event, final String caseUrn, final ReferredDefendant defendant, final String courtHouseCode, final String filename) throws IOException{
        final ReferredPerson personDetails = defendant.getPersonDefendant().getPersonDetails();
        final Optional<JsonObject> jsonData = getSupportingData(caseUrn, event, ENGLISH, personDetails, courtHouseCode);
        if(jsonData.isPresent()) {
            final UUID systemUserId = systemUserProvider.getContextSystemUserId().orElseThrow(() -> new RuntimeException("systemUserProvider.getContextSystemUserId() not available"));
            byte[] referralDisqualifyWarning = this.documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(jsonData.get(), ENGLISH_TEMPLATE_NAME, systemUserId);
            final byte[] emptyPage = this.documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(jsonData.get(), TEMPLATE_IDENTIFIER_EMPTY_PAGE, systemUserId); //check
            final String postCode = jsonData.get().getJsonObject("orderAddressee").getJsonObject("address").getString("postCode", null);
            if (nonNull(postCode)) {
                referralDisqualifyWarning = getWelshDocument(event, caseUrn, courtHouseCode, personDetails, systemUserId, referralDisqualifyWarning, emptyPage, postCode);
            }
             return Optional.ofNullable(documentGeneratorService.generatePdfDocument(event, filename, referralDisqualifyWarning));
        }
        return Optional.empty();
    }

    private byte[] getWelshDocument(final JsonEnvelope event, final String caseUrn, final String courtHouseCode, final ReferredPerson personDetails, final UUID systemUserId, byte[] referralDisqualifyWarning, final byte[] emptyPage, final String postCode) throws IOException {
        final Country country = this.progressionService.getCountryByPostcode(postCode, event);
        if (WALES.equals(country)) {
            final Optional<JsonObject> welshJsonData = getSupportingData(caseUrn, event, new Locale("CY"), personDetails, courtHouseCode);
            if(welshJsonData.isPresent()) {
                final byte[] welshReferralDisqualifyWarning = this.documentGeneratorClientProducer.documentGeneratorClient().generatePdfDocument(welshJsonData.get(), WELSE_TEMPLATE_NAME, systemUserId);
                referralDisqualifyWarning = pdfHelper.mergePdfDocuments(welshReferralDisqualifyWarning, emptyPage, referralDisqualifyWarning);
            }
        }
        return referralDisqualifyWarning;
    }

    private Optional<JsonObject> getSupportingData(final String caseId, final JsonEnvelope event, final Locale locale, final ReferredPerson personDeails, final String courtHouseCode) {
        if(nonNull(courtHouseCode)) {
            final Optional<CourtCentre> organisationUnit = getOrganisationUnit(courtHouseCode, event);
            if (organisationUnit.isPresent() && (nonNull(organisationUnit.get().getLja()))) {
                final String ljaCode = organisationUnit.get().getLja().getLjaCode();
                final LjaDetails ljaDetails = isNotBlank(ljaCode) ? referenceDataService.getLjaDetails(event, ljaCode, requester) : null;
                return Optional.ofNullable(dataAggregatorFactory.getAggregator(locale).aggregateReferralDisqualifyWarningData(caseId, organisationUnit.get(), ljaDetails, personDeails));
            }
        }
        return Optional.empty();
    }

    private Optional<CourtCentre> getOrganisationUnit(final String courtHouseCode, final JsonEnvelope event) {
            return Optional.ofNullable(referenceDataService.getCourtByCourtHouseOUCode(courtHouseCode, event, requester));
    }


    private CourtDocument courtDocument(final JsonEnvelope envelope, final List<UUID> defendantIds, final UUID materialId, final UUID caseId, final String filename) {

        final DefendantDocument defendantDocument = DefendantDocument.defendantDocument().withDefendants(defendantIds).
                withProsecutionCaseId(caseId).build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withDefendantDocument(defendantDocument)
                .build();
        final Optional<JsonObject> documentTypeData = referenceDataService.getDocumentTypeAccessData(ORDERS_NOTICES_AND_DIRECTION, envelope, requester);
        final JsonObject documentTypeDataJsonObject = documentTypeData.orElseThrow(() -> new RuntimeException("failed to look up Orders, Notices document type " + ORDERS_NOTICES_AND_DIRECTION));
        final JsonObject documentTypeRBACData = documentTypeDataJsonObject.getJsonObject(COURT_DOCUMENT_TYPE_RBAC);
        final Integer seqNum = Integer.parseInt(documentTypeDataJsonObject.getJsonNumber("seqNum") == null ? "0" : documentTypeDataJsonObject.getJsonNumber("seqNum").toString());
        final List<String> rbacUserGroups = getRBACUserGroups(documentTypeRBACData, READ_USER_GROUPS);
        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(ORDERS_NOTICES_AND_DIRECTION)
                .withDocumentTypeDescription(documentTypeDataJsonObject.getString("section"))
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withUserGroups(rbacUserGroups)
                                .withName(filename)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withDocumentTypeRBAC(getRBACDataObject(documentTypeRBACData))
                .withContainsFinancialMeans(false)
                .withSeqNum(seqNum)
                .withName(filename)
                .withMimeType("application/pdf")
                .withSendToCps(false)
                .build();

    }

    private DocumentTypeRBAC getRBACDataObject(final JsonObject documentTypeRBACData) {
        return DocumentTypeRBAC.
                documentTypeRBAC()
                .withUploadUserGroups(getRBACUserGroups(documentTypeRBACData, UPLOAD_ACCESS))
                .withReadUserGroups(getRBACUserGroups(documentTypeRBACData, READ_ACCESS))
                .withDownloadUserGroups(getRBACUserGroups(documentTypeRBACData, DOWNLOAD_ACCESS))
                .withDeleteUserGroups(getRBACUserGroups(documentTypeRBACData, DELETE_ACCESS))
                .build();
    }

    private List<String> getRBACUserGroups(final JsonObject documentTypeRBACData, final String accessLevel) {

        if (!documentTypeRBACData.containsKey(accessLevel)) {
            return new ArrayList<>();
        }
        final JsonArray documentTypeRBACJsonArray = documentTypeRBACData.getJsonArray(accessLevel);

        return IntStream.range(0, (documentTypeRBACJsonArray).size()).mapToObj(i -> documentTypeRBACJsonArray.getJsonObject(i).getJsonObject("cppGroup").getString("groupName")).collect(toList());
    }

}