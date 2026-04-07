package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseDocument;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.CommunicationType;
import uk.gov.moj.cpp.progression.NotificationInfoStatus;
import uk.gov.moj.cpp.progression.RecipientType;
import uk.gov.moj.cpp.progression.domain.PostalAddress;
import uk.gov.moj.cpp.progression.domain.PostalAddressee;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;
import uk.gov.moj.cpp.progression.domain.PostalHearingCourtDetails;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.entity.NotificationInfo;
import uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository.NotificationInfoJdbcRepository;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.dto.CaseOffence;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.service.dto.HearingTemplatePayload;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingNotificationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingNotificationHelper.class);

    private static final ZoneId UK_TIME_ZONE = ZoneId.of("Europe/London");

    private static final String EMPTY = "";
    private static final String HEARING_NOTIFICATION_DATE = "hearing_notification_date";

    public static final String HEARING_DATE_PATTERN = "dd/MM/yyy HH:mm";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS");
    private static final Map<String, String> welshTemplateResolverMap = ImmutableMap.of("AmendedHearingNotification", "BilingualAmendedHearingNotification",
            "NewHearingNotification", "BilingualNewHearingNotification");

    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String RECIPIENT_TYPE = "recipientType";
    private static final String HEARING_CONFIRMED = "HEARING_CONFIRMED";
    public static final String ENGLISH_OFFENCE_TITLE_LIST = "englishOffenceTitleList";
    public static final String WELSH_OFFENCE_TITLE_LIST = "welshOffenceTitleList";
    public static final String FULL_NAME = "fullName";

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefenceService defenceService;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private NotificationService notificationService;

    @Inject
    DocumentGeneratorService documentGeneratorService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private NotificationInfoJdbcRepository notificationInfoJdbcRepository;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;


    public void sendHearingNotificationsToRelevantParties(final JsonEnvelope jsonEnvelope, final HearingNotificationInputData hearingNotificationInputData) {
        LOGGER.info("sending hearing notifications to relevant parties for hearing : {}", hearingNotificationInputData.getHearingId());

        final List<ProsecutionCase> cases = hearingNotificationInputData.getCaseIds().stream().map(caseId -> progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .collect(Collectors.toList());
        final CourtCentre inputCourtCenter = CourtCentre.courtCentre()
                .withId(hearingNotificationInputData.getCourtCenterId())
                .withRoomId(hearingNotificationInputData.getCourtRoomId())
                .build();

        final CourtCentre enrichedCourtCentre = progressionService.transformCourtCentreV2(inputCourtCenter, jsonEnvelope);

        for (final ProsecutionCase prosecutionCase : cases) {
            final UUID prosecutorId = getProsecutorId(prosecutionCase);

            final Optional<JsonObject> prosecutorDetails = referenceDataService.getProsecutor(jsonEnvelope, prosecutorId, requester);

            prosecutionCase.getDefendants().stream()
                    .filter(defendant -> hearingNotificationInputData.getDefendantIds().contains(defendant.getId()))
                    .forEach(defendant -> sendNotificationToParties(prosecutionCase, defendant, prosecutorDetails, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope));
        }
    }

    private void sendNotificationToParties(final ProsecutionCase prosecutionCase, final Defendant defendant, final Optional<JsonObject> prosecutorDetails, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope) {
        sendHearingNotificationToDefendant(prosecutionCase, defendant, prosecutorDetails, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
        sendHearingNotificationToProsecutor(prosecutionCase, defendant, prosecutorDetails, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
    }

    private void sendHearingNotificationToDefendant(final ProsecutionCase prosecutionCase, final Defendant defendant, final Optional<JsonObject> prosecutorDetails, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Sending HearingNotification on HearingId {} and DefendantId {}", hearingNotificationInputData.getHearingId(), defendant.getId());
        final UUID caseId = prosecutionCase.getId();
        final UUID defendantId = defendant.getId();
        final String templateName = enrichedCourtCentre.getWelshCourtCentre() ? welshTemplateResolverMap.get(hearingNotificationInputData.getTemplateName()) : hearingNotificationInputData.getTemplateName();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = defenceService.getDefenceOrganisationByDefendantId(jsonEnvelope, defendantId);
        final DefenceOrganisationVO defenceOrganisationVO = buildDefenceOrgVO(associatedDefenceOrganisation);
        final PostalAddressee defendantAddressee = getPostalAddresseeForDefendant(defendant, defenceOrganisationVO);
        String prosecutorName = EMPTY;
        if(prosecutorDetails.isPresent()){
            final JsonObject prosecutorJsonObject = prosecutorDetails.get();
            prosecutorName = prosecutorJsonObject.getString(FULL_NAME, EMPTY);
        }
        //payload is same for newHearingTemplate and amendedHearingTemplate
        final JsonObject documentPayload = createDocumentPayload(prosecutionCase, defendant, defendantAddressee, enrichedCourtCentre, hearingNotificationInputData, prosecutorName);
        final UUID materialId = randomUUID();

        final RecipientType recipientType = nonNull(defenceOrganisationVO) ? RecipientType.DEFENCE : RecipientType.DEFENDANT ;
        final String fileName = getNotificationPdfName(templateName, recipientType.getRecipientName());
        documentGeneratorService.generateNonNowDocument(jsonEnvelope, documentPayload, templateName, materialId, fileName);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID notificationId = randomUUID();

        addCourtDocument(jsonEnvelope, caseId, materialId, fileName);
        if(shouldSendTheNotifications(prosecutionCase, defendant)) {
            if (nonNull(defenceOrganisationVO)) {
                sendNotificationToDefendantOrganisation(hearingNotificationInputData, jsonEnvelope, caseId, defenceOrganisationVO, materialId, materialUrl, notificationId);
            } else if (nonNull(defendant.getPersonDefendant())) {
                final PersonDefendant personDefendant = defendant.getPersonDefendant();
                sendNotificationToPersonDefendant(hearingNotificationInputData, jsonEnvelope, caseId, personDefendant, materialId, materialUrl, notificationId);
            } else if (nonNull(defendant.getLegalEntityDefendant())) {
                final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
                sendNotificationToLegalEntityDefendant(hearingNotificationInputData, jsonEnvelope, caseId, legalEntityDefendant, materialId, materialUrl, notificationId);
            }else {
                LOGGER.info("Notification entity is neither defence organisation not person or org defendant hence not sending any notification");
            }
        }
    }

    public void addCourtDocument(final JsonEnvelope jsonEnvelope, final UUID caseId, final UUID materialId, final String fileName) {
        final CourtDocument courtDocument = buildCourtDocument(caseId, materialId, fileName);
        final JsonObject jsonObject = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("courtDocument", objectToJsonObjectConverter.convert(courtDocument))
                .build();
        final Envelope<JsonObject> data = envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata())
                .withName("progression.command.add-court-document"), jsonObject);
        sender.send(data);
    }

    public CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String fileName) {
        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withCaseDocument(CaseDocument.caseDocument()
                        .withProsecutionCaseId(caseId)
                        .build())
                .build();

        final Material material = Material.material().withId(materialId)
                .withReceivedDateTime(ZonedDateTime.now())
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategory)
                .withDocumentTypeDescription(DOCUMENT_TYPE_DESCRIPTION)
                .withDocumentTypeId(CASE_DOCUMENT_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(fileName)
                .withMaterials(Collections.singletonList(material))
                .withSendToCps(false)
                .withContainsFinancialMeans(false)
                .build();
    }

    private boolean shouldSendTheNotifications(final ProsecutionCase prosecutionCase, final Defendant defendant) {
        if (nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil()) {
            boolean isExParteOffence = prosecutionCase.getDefendants().stream()
                    .filter(def -> def.getId().equals(defendant.getId()))
                    .anyMatch(def -> def.getOffences().stream()
                            .anyMatch(offence -> nonNull(offence.getCivilOffence()) && offence.getCivilOffence().getIsExParte()));
            return !isExParteOffence;
        }
        return true;
    }

    private void sendNotificationToDefendantOrganisation(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final DefenceOrganisationVO defenceOrganisationVO,
                                                         final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (isNotEmpty(defenceOrganisationVO.getEmail())) {
            saveNotificationInfo(notificationId, RecipientType.DEFENCE, CommunicationType.EMAIL.getType());
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, defenceOrganisationVO.getEmail(), materialId, materialUrl, notificationId);
        } else {
            saveNotificationInfo(notificationId, RecipientType.DEFENCE, CommunicationType.LETTER.getType());
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true);
        }
    }

    private void sendNotificationToPersonDefendant(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final PersonDefendant personDefendant,
                                                   final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (nonNull(personDefendant)
                && nonNull(personDefendant.getPersonDetails())
                && nonNull(personDefendant.getPersonDetails().getContact())
                && nonNull(personDefendant.getPersonDetails().getContact().getPrimaryEmail())) {
            final String defendantEmail = personDefendant.getPersonDetails().getContact().getPrimaryEmail();
            saveNotificationInfo(notificationId, RecipientType.DEFENDANT, CommunicationType.EMAIL.getType());
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, defendantEmail, materialId, materialUrl, notificationId);
        } else {
            saveNotificationInfo(notificationId, RecipientType.DEFENDANT, CommunicationType.LETTER.getType());
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true);
        }
    }

    private void sendNotificationToLegalEntityDefendant(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final LegalEntityDefendant legalEntityDefendant,
                                                        final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (nonNull(legalEntityDefendant)
                && nonNull(legalEntityDefendant.getOrganisation())
                && nonNull(legalEntityDefendant.getOrganisation().getContact())
                && nonNull(legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail())) {
            final String orgDefendantEmail = legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail();
            saveNotificationInfo(notificationId, RecipientType.DEFENDANT, CommunicationType.EMAIL.getType());
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, orgDefendantEmail, materialId, materialUrl, notificationId);
        } else {
            saveNotificationInfo(notificationId, RecipientType.DEFENDANT, CommunicationType.LETTER.getType());
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true);
        }
    }

    private void sendEmail(final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope, final UUID caseId, final String email, final UUID materialId, final String materialUrl, final UUID notificationId) {
        final EmailChannel emailChannel = EmailChannel.emailChannel()
                .withPersonalisation(Personalisation.personalisation()
                        .withAdditionalProperty(HEARING_NOTIFICATION_DATE, hearingNotificationInputData.getHearingDateTime()
                                .withZoneSameInstant(UK_TIME_ZONE).format(DateTimeFormatter.ofPattern(HEARING_DATE_PATTERN)))
                        .build())
                .withMaterialUrl(materialUrl)
                .withTemplateId(hearingNotificationInputData.getEmailNotificationTemplateId())
                .withSendToAddress(email)
                .withReplyToAddress("NOREPLY@noreply.com")
                .build();
        notificationService.sendEmail(jsonEnvelope, notificationId, caseId, null, materialId, Arrays.asList(emailChannel));
    }


    private void sendHearingNotificationToProsecutor(final ProsecutionCase prosecutionCase, final Defendant defendant, final Optional<JsonObject> prosecutorDetails, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope) {
        final UUID caseId = prosecutionCase.getId();
        if (prosecutorDetails.isEmpty()) {
            LOGGER.info("Hearing notification will not be sent for caseId {} due to absence of prosecutor details", caseId);
            return;
        }
        final JsonObject prosecutorJsonObject = prosecutorDetails.get();
        if (prosecutorJsonObject.getBoolean("cpsFlag", false)) {
            LOGGER.info("Hearing notification will not be sent for CPS prosecutor caseId {}", caseId);
            return;
        }

        final String prosecutorName = prosecutorJsonObject.getString(FULL_NAME);
        final String prosecutorEmail = prosecutorJsonObject.getString("contactEmailAddress", null);
        final Address prosecutorAddress = isNull(prosecutorJsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(prosecutorJsonObject.getJsonObject("address"), Address.class);
        final PostalAddressee postalAddressee = PostalAddressee.builder()
                .withName(prosecutorName)
                .withAddress(buildPostalAddress(prosecutorAddress, false))
                .build();

        final JsonObject documentPayload = createDocumentPayload(prosecutionCase, defendant, postalAddressee, enrichedCourtCentre, hearingNotificationInputData, prosecutorName);
        final UUID materialId = randomUUID();
        final String templateName = hearingNotificationInputData.getTemplateName();
        final String fileName = getNotificationPdfName(templateName, RecipientType.PROSECUTOR.getRecipientName());
        documentGeneratorService.generateNonNowDocument(jsonEnvelope, documentPayload, templateName, materialId, fileName);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID notificationId = randomUUID();
        addCourtDocument(jsonEnvelope, caseId, materialId, fileName);

        if(shouldSendTheNotifications(prosecutionCase, defendant)) {
            if (isNotEmpty(prosecutorEmail)) {
                saveNotificationInfo(notificationId, RecipientType.PROSECUTOR, CommunicationType.EMAIL.getType());
                sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, prosecutorEmail, materialId, materialUrl, notificationId);
            } else {
                saveNotificationInfo(notificationId, RecipientType.PROSECUTOR, CommunicationType.LETTER.getType());
                notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true);
            }
        }
    }

    private void saveNotificationInfo(UUID notificationId, RecipientType recipientType, String notificationType) {
        notificationInfoJdbcRepository.save(NotificationInfo.Builder.builder()
                .withNotificationId(notificationId)
                .withNotificationType(notificationType)
                .withProcessName(HEARING_CONFIRMED)
                .withPayload(createObjectBuilder().add(RECIPIENT_TYPE, recipientType.getRecipientName()).build().toString())
                .withProcessedTimestamp(ZonedDateTime.now())
                .withStatus(NotificationInfoStatus.PENDING.getType())
                .build());
    }

    private UUID getProsecutorId(final ProsecutionCase prosecutionCase) {
        UUID prosecutorId = null;
        if (nonNull(prosecutionCase.getProsecutor()) && nonNull(prosecutionCase.getProsecutor().getProsecutorId())) {
            prosecutorId = prosecutionCase.getProsecutor().getProsecutorId();
        } else if (nonNull(prosecutionCase.getProsecutionCaseIdentifier()) && nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())) {
            prosecutorId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId();
        } else {
            LOGGER.info("Prosecutor id is null for caseId {}", prosecutionCase.getId());
        }
        return prosecutorId;
    }

    private Map<String, List<CaseOffence>> getOffenceTitles(List<UUID> offenceId, final ProsecutionCase prosecutionCase) {
        Map<String, List<CaseOffence>> offenceMap = new HashMap<>();
        final List<CaseOffence> offenceList = new ArrayList<>();
        final List<CaseOffence> welshOffenceList = new ArrayList<>();

        offenceId.forEach(offId ->
                prosecutionCase.getDefendants().stream().forEach(defendant -> defendant.getOffences().stream()
                        .filter(offence -> offence.getId().toString().equalsIgnoreCase(offId.toString()))
                        .forEach(offence -> populateOffenceLists(offence, offenceList, welshOffenceList)))
        );
        offenceMap.put(ENGLISH_OFFENCE_TITLE_LIST, offenceList);
        offenceMap.put(WELSH_OFFENCE_TITLE_LIST, welshOffenceList);
        return offenceMap;
    }

    private static void populateOffenceLists(final Offence offence, final List<CaseOffence> offenceList, final List<CaseOffence> welshOffenceList) {

        CaseOffence caseOffence = new CaseOffence();
        caseOffence.setOffence(offence.getOffenceTitle());
        offenceList.add(caseOffence);

        if (isNotEmpty(offence.getOffenceTitleWelsh())) {
            CaseOffence welshCaseOffence = new CaseOffence();
            welshCaseOffence.setOffence(offence.getOffenceTitleWelsh());
            welshOffenceList.add(welshCaseOffence);
        } else {
            CaseOffence welshCaseOffence = new CaseOffence();
            welshCaseOffence.setOffence(offence.getOffenceTitle());
            welshOffenceList.add(welshCaseOffence);
        }
    }

    private JsonObject createDocumentPayload(final ProsecutionCase prosecutionCase, final Defendant defendant, final PostalAddressee postalAddressee, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final String applicantName) {

        final String caseReference = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
        final PostalDefendant postalDefendant = getPostalDefendant(defendant);

        final PostalHearingCourtDetails.Builder postalHearingCourtDetailsBuilder = PostalHearingCourtDetails.builder()
                .withCourtName(enrichedCourtCentre.getName())
                .withCourtNameWelsh(enrichedCourtCentre.getWelshName())
                .withCourtroomName(enrichedCourtCentre.getRoomName())
                .withCourtroomNameWelsh(enrichedCourtCentre.getWelshRoomName())
                .withHearingDate(hearingNotificationInputData.getHearingDateTime().toLocalDate().toString())
                .withCourtAddress(buildPostalAddress(enrichedCourtCentre.getAddress(), false))
                .withHearingTime(hearingNotificationInputData.getHearingDateTime().withZoneSameInstant(UK_TIME_ZONE).toLocalTime().toString());

        if (nonNull(enrichedCourtCentre.getWelshCourtCentre()) && enrichedCourtCentre.getWelshCourtCentre()) {
            postalHearingCourtDetailsBuilder.withCourtAddressWelsh(buildPostalAddress(enrichedCourtCentre.getWelshAddress(), enrichedCourtCentre.getWelshCourtCentre()));
        }
        Map<String, List<CaseOffence>> offenceMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(hearingNotificationInputData.getDefendantOffenceListMap().get(defendant.getId()))) {
            offenceMap = getOffenceTitles(hearingNotificationInputData.getDefendantOffenceListMap().get(defendant.getId()), prosecutionCase);
        }

        final boolean isCivil = nonNull(prosecutionCase.getIsCivil()) && prosecutionCase.getIsCivil();

        final HearingTemplatePayload hearingTemplatePayload = buildHearingTemplatePayload(enrichedCourtCentre, hearingNotificationInputData, LocalDate.now().toString(), caseReference,
                postalAddressee, postalDefendant, postalHearingCourtDetailsBuilder.build(), offenceMap, isCivil, applicantName);
        return objectToJsonObjectConverter.convert(hearingTemplatePayload);
    }


    private PostalDefendant getPostalDefendant(final Defendant defendant) {
        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        PostalDefendant postalDefendant = null;
        if (nonNull(personDefendant)) {
            final Person defendantPerson = personDefendant.getPersonDetails();
            postalDefendant = PostalDefendant.builder()
                    .withDateOfBirth(defendantPerson.getDateOfBirth())
                    .withAddress(buildPostalAddress(defendantPerson.getAddress(), false))
                    .withFirstName(defendantPerson.getFirstName())
                    .withLastName(defendantPerson.getLastName())
                    .withMiddleName(defendantPerson.getMiddleName())
                    .withName(getDefendantName(defendant))
                    .withTitle(defendantPerson.getTitle())
                    .build();
        }
        final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
        if (nonNull(legalEntityDefendant)) {
            final Organisation organisation = legalEntityDefendant.getOrganisation();
            postalDefendant = PostalDefendant.builder()
                    .withName(organisation.getName())
                    .withAddress(buildPostalAddress(organisation.getAddress(), false))
                    .build();
        }

        return postalDefendant;
    }

    private PostalAddressee getPostalAddresseeForDefendant(final Defendant defendant, final DefenceOrganisationVO defenceOrganisationVO) {
        PostalAddressee postalAddressee = null;
        if (nonNull(defenceOrganisationVO)) {
            postalAddressee = PostalAddressee.builder()
                    .withAddress(PostalAddress.builder()
                            .withLine1(defenceOrganisationVO.getAddressLine1())
                            .withLine2(defenceOrganisationVO.getAddressLine2())
                            .withLine3(defenceOrganisationVO.getAddressLine3())
                            .withLine4(defenceOrganisationVO.getAddressLine4())
                            .withPostCode(defenceOrganisationVO.getPostcode())
                            .build())
                    .withName(defenceOrganisationVO.getName())
                    .build();
        } else {
            final Address defendantAddress = getDefendantAddress(defendant);
            postalAddressee = PostalAddressee.builder()
                    .withAddress(buildPostalAddress(defendantAddress, false))
                    .withName(getDefendantName(defendant))
                    .build();
        }

        return postalAddressee;
    }

    public HearingTemplatePayload buildHearingTemplatePayload(final CourtCentre courtCenter, final HearingNotificationInputData hearingNotificationInputData, String issueDate, final String reference,
                                                              final PostalAddressee addressee, final PostalDefendant defendant, final PostalHearingCourtDetails hearingCourtDetails,
                                                              final Map<String, List<CaseOffence>> offenceMap, final boolean isCivil, final String applicantName) {

        return HearingTemplatePayload.builder()
                .withReference(ofNullable(reference).orElse(EMPTY))
                .withIssueDate(issueDate)
                .withLjaCode(ofNullable(courtCenter.getLja().getLjaCode()).orElse(EMPTY))
                .withLjaName(ofNullable(courtCenter.getLja().getLjaName()).orElse(EMPTY))
                .withPostalAddressee(ofNullable(addressee).orElse(null))
                .withPostalDefendant(ofNullable(defendant).orElse(null))
                .withPostalHearingCourtDetails(ofNullable(hearingCourtDetails).orElse(null))
                .withOffence(ofNullable(offenceMap.get(ENGLISH_OFFENCE_TITLE_LIST)).orElse(null))
                .withWelshOffence(ofNullable(offenceMap.get(WELSH_OFFENCE_TITLE_LIST)).orElse(null))
                .withIsCivil(isCivil)
                .withCourtCentreName(courtCenter.getName())
                .withHearingType(hearingNotificationInputData.getHearingType())
                .withApplicantName(applicantName)
                .build();
    }

    private String getDefendantName(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        if (nonNull(defendant.getLegalEntityDefendant())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getName();
        }
        return EMPTY;
    }

    private Address getDefendantAddress(Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant())) {
            return defendant.getPersonDefendant().getPersonDetails().getAddress();
        }

        if (nonNull(defendant.getLegalEntityDefendant())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getAddress();
        }

        return null;
    }

    private PostalAddress buildPostalAddress(final Address address, final boolean isWelsh) {
        if (isNull(address)) {
            return null;
        }
        final PostalAddress.Builder addressBuilder = PostalAddress.builder();
        if (isWelsh) {
            addressBuilder
                    .withLine1(address.getWelshAddress1())
                    .withLine2(address.getWelshAddress2())
                    .withLine3(address.getWelshAddress3())
                    .withLine4(address.getWelshAddress4())
                    .withLine5(address.getWelshAddress5());
        } else {
            addressBuilder
                    .withLine1(address.getAddress1())
                    .withLine2(address.getAddress2())
                    .withLine3(address.getAddress3())
                    .withLine4(address.getAddress4())
                    .withLine5(address.getAddress5());
        }
        addressBuilder.withPostCode(address.getPostcode());
        return addressBuilder.build();
    }

    private DefenceOrganisationVO buildDefenceOrgVO(AssociatedDefenceOrganisation associatedDefenceOrganisation) {

        if (isNull(associatedDefenceOrganisation)) {
            return null;
        }

        final DefenceOrganisationVO.DefenceOrganisationVOBuilder builder = DefenceOrganisationVO.builder();
        builder.name(associatedDefenceOrganisation.getOrganisationName())
                .phoneNumber(associatedDefenceOrganisation.getPhoneNumber())
                .email(associatedDefenceOrganisation.getEmail());

        if (nonNull(associatedDefenceOrganisation.getAddress())) {
            final DefenceOrganisationAddress defenceOrganisationAddress = associatedDefenceOrganisation.getAddress();
            builder.addressLine1(defenceOrganisationAddress.getAddress1())
                    .addressLine2(defenceOrganisationAddress.getAddress2())
                    .addressLine3(defenceOrganisationAddress.getAddress3())
                    .addressLine4(defenceOrganisationAddress.getAddress4())
                    .postcode(defenceOrganisationAddress.getAddressPostcode());

        }
        return builder.build();
    }

    private String getNotificationPdfName(final String templateName, String receipientType) {
        return templateName + " " + formatter.format(LocalDateTime.now()) + " " + receipientType + " copy";
    }

    public ZonedDateTime getEarliestStartDateTime(final ZonedDateTime earliestStartDateTime) {
        if(earliestStartDateTime != null){
            return earliestStartDateTime.withZoneSameInstant(ZoneId.of("Europe/London"));
        }
        return null;
    }
}
