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
import uk.gov.moj.cpp.progression.RecipientType;
import uk.gov.moj.cpp.progression.domain.PostalAddress;
import uk.gov.moj.cpp.progression.domain.PostalAddressee;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;
import uk.gov.moj.cpp.progression.domain.PostalHearingCourtDetails;
import uk.gov.moj.cpp.progression.domain.constant.NotificationType;
import uk.gov.moj.cpp.progression.persist.NotificationInfoRepository;
import uk.gov.moj.cpp.progression.persist.entity.NotificationInfo;
import uk.gov.moj.cpp.progression.service.DefenceService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.dto.CaseOffence;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.service.dto.HearingTemplatePayload;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static final String EMPTY = "";
    private static final String HEARING_NOTIFICATION_DATE = "hearing_notification_date";
    public static final String RECIPIENT_TYPE_ADDITION_PROPERTY = "recipient_type";
    public static final String CASE_ID_ADDITION_PROPERTY = "case_id";

    public static final String OFFENCE_TITLE = "title";
    private static final String HEARING_DATE_PATTERN = "dd/MM/yyy HH:mm a";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS");
    private static final Map<String, String> welshTemplateResolverMap = ImmutableMap.of("AmendedHearingNotification", "BilingualAmendedHearingNotification",
            "NewHearingNotification", "BilingualNewHearingNotification");

    private static final String DOCUMENT_TYPE_DESCRIPTION = "Electronic Notifications";
    private static final UUID CASE_DOCUMENT_TYPE_ID = fromString("f471eb51-614c-4447-bd8d-28f9c2815c9e");
    private static final String APPLICATION_PDF = "application/pdf";

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
    private ReferenceDataOffenceService referenceDataOffenceService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private NotificationInfoRepository notificationInfoRepository;

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
        sendHearingNotificationToDefendant(prosecutionCase, defendant, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
        sendHearingNotificationToProsecutor(prosecutionCase, defendant, prosecutorDetails, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
    }

    private void sendHearingNotificationToDefendant(final ProsecutionCase prosecutionCase, final Defendant defendant, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope) {
        LOGGER.info("Sending HearingNotification on HearingId {} and DefendantId {}", hearingNotificationInputData.getHearingId(), defendant.getId());
        final UUID caseId = prosecutionCase.getId();
        final UUID defendantId = defendant.getId();
        final String templateName = enrichedCourtCentre.getWelshCourtCentre() ? welshTemplateResolverMap.get(hearingNotificationInputData.getTemplateName()) : hearingNotificationInputData.getTemplateName();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = defenceService.getDefenceOrganisationByDefendantId(jsonEnvelope, defendantId);
        final DefenceOrganisationVO defenceOrganisationVO = buildDefenceOrgVO(associatedDefenceOrganisation);
        final PostalAddressee defendantAddressee = getPostalAddresseeForDefendant(defendant, defenceOrganisationVO);
        //payload is same for newHearingTemplate and amendedHearingTemplate
        final JsonObject documentPayload = createDocumentPayload(prosecutionCase, defendant, defendantAddressee, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
        final UUID materialId = randomUUID();

        RecipientType recipientType = nonNull(defenceOrganisationVO) ? RecipientType.DEFENCE : RecipientType.DEFENDANT ;
        final String fileName = getNotificationPdfName(templateName, recipientType.getRecipientName());
        documentGeneratorService.generateNonNowDocument(jsonEnvelope, documentPayload, templateName, materialId, fileName);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID notificationId = randomUUID();

        addCourtDocument(jsonEnvelope, caseId, materialId, fileName);
        if (nonNull(defenceOrganisationVO)) {
            sendNotificationToDefendantOrganisation(hearingNotificationInputData, jsonEnvelope, caseId, defenceOrganisationVO, materialId, materialUrl, notificationId);
        } else if (nonNull(defendant.getPersonDefendant())) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            sendNotificationToPersonDefendant(hearingNotificationInputData, jsonEnvelope, caseId, personDefendant, materialId, materialUrl, notificationId);
        } else if (nonNull(defendant.getLegalEntityDefendant())) {
            final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
            sendNotificationToLegalEntityDefendant(hearingNotificationInputData, jsonEnvelope, caseId, legalEntityDefendant, materialId, materialUrl, notificationId);
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

    private CourtDocument buildCourtDocument(final UUID caseId, final UUID materialId, final String fileName) {
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

    private void sendNotificationToDefendantOrganisation(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final DefenceOrganisationVO defenceOrganisationVO,
                                                         final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (isNotEmpty(defenceOrganisationVO.getEmail())) {
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, defenceOrganisationVO.getEmail(), materialId, materialUrl, notificationId, RecipientType.DEFENCE);
        } else {
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true, RecipientType.DEFENCE);
        }
    }

    private void sendNotificationToPersonDefendant(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final PersonDefendant personDefendant,
                                                   final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (nonNull(personDefendant)
                && nonNull(personDefendant.getPersonDetails())
                && nonNull(personDefendant.getPersonDetails().getContact())
                && nonNull(personDefendant.getPersonDetails().getContact().getPrimaryEmail())) {
            final String defendantEmail = personDefendant.getPersonDetails().getContact().getPrimaryEmail();
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, defendantEmail, materialId, materialUrl, notificationId, RecipientType.DEFENDANT);
        } else {
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true, RecipientType.DEFENDANT);
        }
    }

    private void sendNotificationToLegalEntityDefendant(final HearingNotificationInputData hearingNotificationInputData, JsonEnvelope jsonEnvelope, final UUID caseId, final LegalEntityDefendant legalEntityDefendant,
                                                        final UUID materialId, final String materialUrl, final UUID notificationId) {
        if (nonNull(legalEntityDefendant)
                && nonNull(legalEntityDefendant.getOrganisation())
                && nonNull(legalEntityDefendant.getOrganisation().getContact())
                && nonNull(legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail())) {
            final String orgDefendantEmail = legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail();
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, orgDefendantEmail, materialId, materialUrl, notificationId, RecipientType.DEFENDANT);
        } else {
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true, RecipientType.DEFENDANT);
        }
    }

    private void sendEmail(final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope, final UUID caseId, final String email, final UUID materialId, final String materialUrl, final UUID notificationId,
                           final RecipientType recipientType) {
        EmailChannel emailChannel;
        emailChannel = EmailChannel.emailChannel()
                .withPersonalisation(Personalisation.personalisation()
                        .withAdditionalProperty(HEARING_NOTIFICATION_DATE, hearingNotificationInputData.getHearingDateTime().format(DateTimeFormatter.ofPattern(HEARING_DATE_PATTERN)))
                        .withAdditionalProperty(RECIPIENT_TYPE_ADDITION_PROPERTY, recipientType.getRecipientName())
                        .withAdditionalProperty(CASE_ID_ADDITION_PROPERTY, caseId)
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
        if (!prosecutorDetails.isPresent()) {
            LOGGER.info("Hearing notification will not be sent for caseId {} due to absence of prosecutor details", caseId);
            return;
        }
        final JsonObject prosecutorJsonObject = prosecutorDetails.get();
        if (prosecutorJsonObject.getBoolean("cpsFlag", false)) {
            LOGGER.info("Hearing notification will not be sent for CPS prosecutor caseId {}", caseId);
            return;
        }

        final String prosecutorName = prosecutorJsonObject.getString("fullName");
        final String prosecutorEmail = prosecutorJsonObject.getString("contactEmailAddress", null);
        final Address prosecutorAddress = isNull(prosecutorJsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(prosecutorJsonObject.getJsonObject("address"), Address.class);
        final PostalAddressee postalAddressee = PostalAddressee.builder()
                .withName(prosecutorName)
                .withAddress(buildPostalAddress(prosecutorAddress, false))
                .build();

        final JsonObject documentPayload = createDocumentPayload(prosecutionCase, defendant, postalAddressee, enrichedCourtCentre, hearingNotificationInputData, jsonEnvelope);
        final UUID materialId = randomUUID();
        final String templateName = hearingNotificationInputData.getTemplateName();
        final String fileName = getNotificationPdfName(templateName, RecipientType.PROSECUTOR.getRecipientName());
        documentGeneratorService.generateNonNowDocument(jsonEnvelope, documentPayload, templateName, materialId, fileName);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        final UUID notificationId = randomUUID();
        addCourtDocument(jsonEnvelope, caseId, materialId, fileName);

        if (isNotEmpty(prosecutorEmail)) {
            sendEmail(hearingNotificationInputData, jsonEnvelope, caseId, prosecutorEmail, materialId, materialUrl, notificationId, RecipientType.PROSECUTOR);
            //saveNotificationInfo(notificationId, RecipientType.PROSECUTOR, NotificationType.EMAIL);
        } else {
            notificationService.sendLetter(jsonEnvelope, notificationId, caseId, null, materialId, true, RecipientType.PROSECUTOR);
            //saveNotificationInfo(notificationId, RecipientType.PROSECUTOR, NotificationType.PRINT);
        }
    }

    private void saveNotificationInfo(UUID notificationId, RecipientType recipientType, NotificationType notificationType) {
        notificationInfoRepository.save(NotificationInfo.Builder.builder().withNotificationId(notificationId)
                .withNotificationType(notificationType.toString())
                .withPayload(createObjectBuilder().add("RecipientType", recipientType.getRecipientName()).build().toString())
                .withProcessedTimestamp(ZonedDateTime.now()).build());
    }


    private UUID getProsecutorId(final ProsecutionCase prosecutionCase) {
        UUID prosecutorId = null;
        if (nonNull(prosecutionCase.getProsecutor()) && nonNull(prosecutionCase.getProsecutor().getProsecutorId())) {
            prosecutorId = prosecutionCase.getProsecutor().getProsecutorId();
        } else if (nonNull(prosecutionCase.getProsecutionCaseIdentifier()) && nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId())) {
            prosecutorId = prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityId();
        }
        return prosecutorId;
    }


    private List<CaseOffence> getOffenceTitles(List<UUID> offenceId, JsonEnvelope envelope, Requester requester) {
        final List<CaseOffence> offenceList = new ArrayList<>();
        offenceId.forEach(x -> {
                    final Optional<JsonObject> offencePayload = referenceDataOffenceService.getOffenceById(x, envelope, requester);
                    if (offencePayload.isPresent()) {
                        final CaseOffence offence = new CaseOffence();
                        offence.setOffence(offencePayload.get().getString(OFFENCE_TITLE));
                        offenceList.add(offence);
                    }
                }
        );
        return offenceList;
    }

    private JsonObject createDocumentPayload(final ProsecutionCase prosecutionCase, final Defendant defendant, final PostalAddressee postalAddressee, final CourtCentre enrichedCourtCentre, final HearingNotificationInputData hearingNotificationInputData, final JsonEnvelope jsonEnvelope) {

        final String caseReference = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
        final PostalDefendant postalDefendant = getPostalDefendant(defendant);

        final PostalHearingCourtDetails.Builder postalHearingCourtDetailsBuilder = PostalHearingCourtDetails.builder()
                .withCourtName(enrichedCourtCentre.getName())
                .withCourtNameWelsh(enrichedCourtCentre.getWelshName())
                .withCourtroomName(enrichedCourtCentre.getRoomName())
                .withCourtroomNameWelsh(enrichedCourtCentre.getWelshRoomName())
                .withHearingDate(hearingNotificationInputData.getHearingDateTime().toLocalDate().toString())
                .withCourtAddress(buildPostalAddress(enrichedCourtCentre.getAddress(), false))
                .withHearingTime(hearingNotificationInputData.getHearingDateTime().toLocalTime().toString());

        if (enrichedCourtCentre.getWelshCourtCentre()) {
            postalHearingCourtDetailsBuilder.withCourtAddressWelsh(buildPostalAddress(enrichedCourtCentre.getWelshAddress(), enrichedCourtCentre.getWelshCourtCentre()));
        }
        List<CaseOffence> offenceList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(hearingNotificationInputData.getDefendantOffenceListMap().get(defendant.getId()))) {
            offenceList = getOffenceTitles(hearingNotificationInputData.getDefendantOffenceListMap().get(defendant.getId()), jsonEnvelope, requester);
        }

        final HearingTemplatePayload hearingTemplatePayload = buildHearingTemplatePayload(enrichedCourtCentre, hearingNotificationInputData, LocalDate.now().toString(), caseReference,
                postalAddressee, postalDefendant, postalHearingCourtDetailsBuilder.build(), offenceList);
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
                                                              final List<CaseOffence> offenceList) {

        return HearingTemplatePayload.builder()
                .withReference(ofNullable(reference).orElse(EMPTY))
                .withIssueDate(issueDate)
                .withLjaCode(ofNullable(courtCenter.getLja().getLjaCode()).orElse(EMPTY))
                .withLjaName(ofNullable(courtCenter.getLja().getLjaName()).orElse(EMPTY))
                .withPostalAddressee(ofNullable(addressee).orElse(null))
                .withPostalDefendant(ofNullable(defendant).orElse(null))
                .withPostalHearingCourtDetails(ofNullable(hearingCourtDetails).orElse(null))
                .withOffence(ofNullable(offenceList).orElse(null))
                .withCourtCentreName(courtCenter.getName())
                .withHearingType(hearingNotificationInputData.getHearingType())
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
}
