package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.AddMaterialV2.addMaterialV2;
import static uk.gov.justice.core.courts.DefendantSubject.defendantSubject;
import static uk.gov.justice.core.courts.EventNotification.eventNotification;
import static uk.gov.justice.core.courts.ProsecutionCaseSubject.prosecutionCaseSubject;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getCourtTime;

import uk.gov.justice.core.courts.AddMaterialV2;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseSubjects;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantSubject;
import uk.gov.justice.core.courts.EventNotification;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.MaterialTag;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCaseSubject;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.domain.PostalNotification;
import uk.gov.moj.cpp.progression.domain.event.email.PartyType;
import uk.gov.moj.cpp.progression.nows.InvalidNotificationException;
import uk.gov.moj.cpp.progression.nows.Notification;
import uk.gov.moj.cpp.progression.service.dto.InformantNotificationRequired;
import uk.gov.moj.cpp.progression.service.dto.PostalNotificationDetails;
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;
import uk.gov.moj.cpp.progression.service.utils.FileUtil;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by satishkumar on 12/11/2018.
 */
@SuppressWarnings({"WeakerAccess", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod", "squid:S1172", "squid:CallToDeprecatedMethod", "squid:S2221", "squid:S1612"})
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    public static final String CASE_ID = "caseId";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String MATERIAL_ID = "materialId";
    public static final String POSTAGE = "postage";
    public static final String MATERIAL_URL = "materialUrl";
    public static final String STATUS_CODE = "statusCode";
    public static final String COMMA = ",";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class.getName());
    private static final String APPLICATION_ID = "applicationId";
    private static final String ACCEPTED_TIME = "acceptedTime";
    private static final String NOTIFICATIONS = "notifications";
    private static final String FAILED_TIME = "failedTime";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SENT_TIME = "sentTime";
    private static final String COMPLETED_AT = "completedAt";
    private static final String TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String REPLY_TO_ADDRESS = "replyToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final String DEFENDANT_NAME = "defendantName";
    private static final String CASE_URN = "caseURN";
    private static final String PUBLIC_PROGRESSION_EVENTS_WELSH_TRANSLATION_REQUIRED = "public.progression.welsh-translation-required";
    private static final String URN = "URN";
    private static final String SURNAME = "surname";
    private static final String FIRST_NAME = "first_name";
    private static final String MIDDLE_NAME = "middle_name";
    private static final String HEARING_DATE = "hearing_date";
    private static final String COURT_CENTRE_NAME = "venue_name";
    private static final String ORGANISATION_NAME = "organisation_name";
    private static final String ADDRESS_LINE_1 = "address_line_1";
    private static final String ADDRESS_LINE_2 = "address_line_2";
    private static final String ADDRESS_LINE_3 = "address_line_3";
    private static final String ADDRESS_LINE_4 = "address_line_4";
    private static final String POSTCODE = "postcode";
    private static final String EMAIL = "email";
    private static final String PHONE = "phone";
    private static final String EMPTY = "";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String MATERIAL_TYPE = "Court Final orders";
    @Inject
    private Enveloper enveloper;
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;
    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;
    @Inject
    private SystemIdMapperService systemIdMapperService;
    @Inject
    private ApplicationParameters applicationParameters;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private RefDataService referenceDataService;
    @Inject
    private DocumentGeneratorService documentGeneratorService;
    @Inject
    private RestApiNotificationService restApiNotificationService;

    @Inject
    private PostalService postalService;

    @Inject
    private FileUtil fileUtil;

    @Inject
    private MaterialService materialService;

    @Inject
    private CpsRestNotificationService cpsRestNotificationService;

    @Inject
    private DefenceService defenceService;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @Inject
    private CourtApplicationService courtApplicationService;

    public void sendEmail(final JsonEnvelope sourceEnvelope, final UUID notificationId, final UUID caseId, final UUID applicationId, final UUID materialId, final List<EmailChannel> emailNotifications) {

        if (nonNull(emailNotifications)) {

            final JsonArrayBuilder notificationBuilder = buildNotifications(notificationId, emailNotifications);

            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add(NOTIFICATIONS, notificationBuilder);

            ofNullable(caseId).ifPresent(id -> {
                systemIdMapperService.mapNotificationIdToCaseId(caseId, notificationId);
                payloadBuilder.add(CASE_ID, id.toString());
            });

            ofNullable(applicationId).ifPresent(id -> {
                systemIdMapperService.mapNotificationIdToApplicationId(applicationId, notificationId);
                payloadBuilder.add(APPLICATION_ID, id.toString());
            });

            ofNullable(materialId).ifPresent(id -> {
                systemIdMapperService.mapNotificationIdToMaterialId(materialId, notificationId);
                payloadBuilder.add(MATERIAL_ID, id.toString());
            });

            final JsonObject emailPayload = payloadBuilder.build();

            LOGGER.info("sending email payload - {}", emailPayload);

            sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.email").apply(emailPayload));

        } else {

            LOGGER.warn("No Email was sent.");
        }
    }

    public void sendEmail(final JsonEnvelope sourceEnvelope, final UUID caseId, final UUID applicationId, final UUID materialId, final List<EmailChannel> emailNotifications) {

        if (nonNull(emailNotifications)) {

            final List<Notification> notifications = emailNotifications.stream().map(emailChannel -> createNotification(randomUUID(), emailChannel)).collect(Collectors.toList());

            final JsonArrayBuilder notificationBuilder = createArrayBuilder();

            notifications.forEach(notification -> {

                notificationBuilder.add(createNotificationJsonObject(notification));

                ofNullable(caseId).ifPresent(id -> systemIdMapperService.mapNotificationIdToCaseId(caseId, notification.getNotificationId()));

                ofNullable(applicationId).ifPresent(id -> systemIdMapperService.mapNotificationIdToApplicationId(applicationId, notification.getNotificationId()));

                ofNullable(materialId).ifPresent(id -> systemIdMapperService.mapNotificationIdToMaterialId(materialId, notification.getNotificationId()));

            });

            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add(NOTIFICATIONS, notificationBuilder);

            ofNullable(caseId).ifPresent(id -> payloadBuilder.add(CASE_ID, id.toString()));

            ofNullable(applicationId).ifPresent(id -> payloadBuilder.add(APPLICATION_ID, id.toString()));

            ofNullable(materialId).ifPresent(id -> payloadBuilder.add(MATERIAL_ID, id.toString()));

            final JsonObject emailPayload = payloadBuilder.build();

            LOGGER.info("sending email payload - {}", emailPayload);

            sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.email").apply(emailPayload));

        } else {

            LOGGER.warn("No Email was sent.");
        }
    }

    private JsonObjectBuilder createNotificationJsonObject(Notification notification) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notification.getNotificationId().toString())
                .add(TEMPLATE_ID, notification.getTemplateId().toString())
                .add(SEND_TO_ADDRESS, notification.getSendToAddress());

        if (nonNull(notification.getReplyToAddress())) {
            jsonObjectBuilder.add(REPLY_TO_ADDRESS, notification.getReplyToAddress());
        } else {
            jsonObjectBuilder.addNull(REPLY_TO_ADDRESS);
        }

        if (nonNull(notification.getMaterialUrl())) {
            jsonObjectBuilder.add(MATERIAL_URL, notification.getMaterialUrl());
        }

        final Map<String, String> additionalProperties = notification.getPersonalisation();

        final JsonObjectBuilder personalisation = createObjectBuilder();

        additionalProperties.forEach(personalisation::add);

        jsonObjectBuilder.add(PERSONALISATION, personalisation);
        return jsonObjectBuilder;
    }

    public void sendApiNotification(final JsonEnvelope sourceEnvelope, final UUID notificationId, final MaterialDetails materialDetails,
                                    final List<CaseSubjects> caseSubjects, final List<String> defendantAsns, final List<String> cpsDefendantIds) {
        final UUID materialId = materialDetails.getMaterialId();
        if (isNull(materialId)) {
            LOGGER.error("Unable to transform the payload. subjectBusinessObjectId is null.");
            return;
        }

        String materialName = StringUtils.EMPTY;
        String fileName = StringUtils.EMPTY;

        try {
            materialName = materialService.getMaterialMetadataV2(sourceEnvelope, materialId);
            fileName = fileUtil.retrieveFileName(materialDetails.getFileId());
        } catch (FileServiceException e) {
            LOGGER.warn("Failed to retrieve file details.", e);
        } catch(Exception e) {
            LOGGER.info("Exception while fetching materialName",e);
        }

        final DefendantSubject.Builder defendantSubjectBuilder = defendantSubject();
        final List<DefendantSubject> additionalDefendantSubject = new ArrayList<>();

        if (isNotEmpty(defendantAsns)) {
            if (defendantAsns.get(0).contains(COMMA)) {
                buildAdditionalDefendantSubject(defendantAsns, cpsDefendantIds, additionalDefendantSubject);
            } else {
                defendantSubjectBuilder.withAsn(defendantAsns.get(0));
            }
        }

        if (isNotEmpty(cpsDefendantIds)) {
            defendantSubjectBuilder.withCpsDefendantId(getCpsDefendantIds(cpsDefendantIds));
        }

        final ProsecutionCaseSubject.Builder prosecutionCaseSubjectBuilder = prosecutionCaseSubject();
        if(nonNull(caseSubjects) && caseSubjects.size()==1) {
            prosecutionCaseSubjectBuilder.withDefendantSubject(defendantSubjectBuilder.build())
                    .withCaseUrn(caseSubjects.get(0).getUrn())
                    .withProsecutingAuthority(caseSubjects.get(0).getProsecutingAuthorityOUCode())
                    .withOuCode(caseSubjects.get(0).getProsecutingAuthorityOUCode());
        }

        final AddMaterialV2.Builder addMaterialV2 = addMaterialV2();
        addMaterialV2.withMaterial(materialDetails.getMaterialId())
                .withMaterialContentType(CONTENT_TYPE)
                .withMaterialName(materialName)
                .withMaterialType(MATERIAL_TYPE)
                .withFileName(fileName)
                .withTag(new ArrayList<MaterialTag>())
                .withProsecutionCaseSubject(prosecutionCaseSubjectBuilder.build());

        final EventNotification eventNotification = buildEventNotification(addMaterialV2, materialId, caseSubjects, additionalDefendantSubject);

        final Optional<String> transformedJsonPayload = ofNullable(objectToJsonObjectConverter.convert(eventNotification).toString());
        if (transformedJsonPayload.isPresent()) {
            LOGGER.info("Notification triggered");
            cpsRestNotificationService.sendMaterial(transformedJsonPayload.get(), materialId, sourceEnvelope); // Need tp pass court document id. Satish to check and confirm
        } else {
            LOGGER.info("No payload available");
        }
    }

    private void buildAdditionalDefendantSubject(final List<String> defendantAsns, final List<String> cpsDefendantIds, final List<DefendantSubject> additionalDefendantSubject) {
        final List<String> asnList = Stream.of(defendantAsns.get(0).split(COMMA, -1))
                .collect(Collectors.toList());
        asnList.forEach(asn -> {
            final DefendantSubject.Builder defendantSubjectBuilder1 = defendantSubject();
            defendantSubjectBuilder1.withAsn(asn);
            if (isNotEmpty(cpsDefendantIds)) {
                defendantSubjectBuilder1.withCpsDefendantId(getCpsDefendantIds(cpsDefendantIds));
            }
            additionalDefendantSubject.add(defendantSubjectBuilder1.build());
        });
    }

    private String getCpsDefendantIds(final List<String> cpsDefendantIds) {
        return StringUtils.join(cpsDefendantIds, COMMA);
    }

    private EventNotification buildEventNotification(final AddMaterialV2.Builder addMaterialV2, final UUID materialId, final List<CaseSubjects> caseSubjects, final List<DefendantSubject> additionalDefendantSubject) {
        final EventNotification.Builder eventNotificationBuilder = eventNotification();
        eventNotificationBuilder.withSubjectBusinessObjectId(materialId);
        eventNotificationBuilder.withSubjectDetails(addMaterialV2.build());
        eventNotificationBuilder.withBusinessEventType("now-generated-for-cps-subscription");
        eventNotificationBuilder.withAdditionalProperty("notificationDate", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()));
        eventNotificationBuilder.withAdditionalProperty("notificationType", "court-now-created");

        if (nonNull(caseSubjects) && caseSubjects.size() > 1) {
            final JsonArrayBuilder casesBuilder = createArrayBuilder();
            caseSubjects.forEach(caseSubjects1 -> {
                final JsonObjectBuilder object = createObjectBuilder();
                object.add("caseUrn", caseSubjects1.getUrn())
                        .add("prosecutingAuthority", caseSubjects1.getProsecutingAuthorityOUCode());
                casesBuilder.add(object.build());
            });
            eventNotificationBuilder.withAdditionalProperty("cases", casesBuilder.build());
        }

        if (isNotEmpty(additionalDefendantSubject)) {
            eventNotificationBuilder.withAdditionalProperty("additionalDefendantSubject", additionalDefendantSubject);
        }
        return eventNotificationBuilder.build();
    }

    public void sendLetter(final JsonEnvelope sourceEnvelope, final UUID notificationId, final UUID caseId, final UUID applicationId, final UUID materialId, final boolean postage) {

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId.toString())
                .add(MATERIAL_ID, materialId.toString())
                .add(POSTAGE, postage);

        ofNullable(caseId).ifPresent(id -> {
            systemIdMapperService.mapNotificationIdToCaseId(caseId, notificationId);
            payloadBuilder.add(CASE_ID, id.toString());
        });

        ofNullable(applicationId).ifPresent(id -> {
            systemIdMapperService.mapNotificationIdToApplicationId(applicationId, notificationId);
            payloadBuilder.add(APPLICATION_ID, id.toString());
        });

        final JsonObject printPayload = payloadBuilder.build();

        LOGGER.info("sending print payload - {}", printPayload);

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.print").apply(printPayload));
    }

    public void recordNotificationRequestFailure(final JsonEnvelope sourceEnvelope, final UUID targetId, final PartyType partyType) {

        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, payload.getString(NOTIFICATION_ID))
                .add(getName(partyType), targetId.toString())
                .add(FAILED_TIME, payload.getString(FAILED_TIME))
                .add(ERROR_MESSAGE, payload.getString(ERROR_MESSAGE));

        if (payload.containsKey(STATUS_CODE)) {
            jsonObjectBuilder.add(STATUS_CODE, payload.getInt(STATUS_CODE));
        }

        final JsonObject notificationFailedPayload = jsonObjectBuilder.build();

        LOGGER.info("sending notification failure - {}", notificationFailedPayload);

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-notification-request-failure")
                .apply(notificationFailedPayload));
    }

    public void recordNotificationRequestSuccess(final JsonEnvelope sourceEnvelope, final UUID targetId, final PartyType partyType) {
        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();
        final String notificationId = payload.getString(NOTIFICATION_ID);
        final String sentTime = payload.getString(SENT_TIME);
        final String completedAt = payload.getString(COMPLETED_AT, null);
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId)
                .add(getName(partyType), targetId.toString())
                .add(SENT_TIME, sentTime);

        if(nonNull(completedAt)) {
            jsonObjectBuilder.add(COMPLETED_AT, completedAt);
        }

        final JsonObject notificationSucceededPayload = jsonObjectBuilder.build();

        LOGGER.info("sending notification request success - {}", notificationSucceededPayload);

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-notification-request-success")
                .apply(notificationSucceededPayload));
    }

    public void recordPrintRequestAccepted(final JsonEnvelope sourceEnvelope) {

        final JsonObject payload = sourceEnvelope.payloadAsJsonObject();

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, payload.getString(NOTIFICATION_ID))
                .add(MATERIAL_ID, payload.getString(MATERIAL_ID))
                .add(ACCEPTED_TIME, ZonedDateTimes.toString(sourceEnvelope.metadata().createdAt().orElse(ZonedDateTime.now())));

        if (payload.containsKey(CASE_ID)) {
            final String caseId = payload.getString(CASE_ID);
            jsonObjectBuilder.add(CASE_ID, caseId);
        }

        if (payload.containsKey(APPLICATION_ID)) {
            final String applicationId = payload.getString(APPLICATION_ID);
            jsonObjectBuilder.add(APPLICATION_ID, applicationId);
        }

        final JsonObject requestAcceptedPayload = jsonObjectBuilder.build();

        LOGGER.info("sending notification request accepted - {}", requestAcceptedPayload);

        sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-notification-request-accepted").apply(requestAcceptedPayload));
    }

    public void recordEmailRequestAccepted(final JsonEnvelope sourceEnvelope) {

        final JsonObject jsonObject = sourceEnvelope.payloadAsJsonObject();

        final JsonArray notifications = jsonObject.getJsonArray(NOTIFICATIONS);

        notifications.forEach(notification -> {

            final JsonObject payload = (JsonObject) notification;

            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add(NOTIFICATION_ID, payload.getString(NOTIFICATION_ID))
                    .add(ACCEPTED_TIME, ZonedDateTimes.toString(sourceEnvelope.metadata().createdAt().get()));

            if (jsonObject.containsKey(MATERIAL_ID)) {
                jsonObjectBuilder.add(MATERIAL_ID, jsonObject.getString(MATERIAL_ID));
            }

            if (jsonObject.containsKey(APPLICATION_ID)) {
                jsonObjectBuilder.add(APPLICATION_ID, jsonObject.getString(APPLICATION_ID));
            }

            if (jsonObject.containsKey(CASE_ID)) {
                jsonObjectBuilder.add(CASE_ID, jsonObject.getString(CASE_ID));
            }

            final JsonObject notificationSucceededPayload = jsonObjectBuilder.build();

            LOGGER.info("sending notification request accepted - {}", notificationSucceededPayload);

            sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.record-notification-request-accepted")
                    .apply(notificationSucceededPayload));

        });
    }

    public void sendCPSNotification(final JsonEnvelope event, final CPSNotificationVO cpsNotification) {
        requireNonNull(cpsNotification);
        cpsNotification.getCaseVO().ifPresent(caseVO ->
                sendEmail(event, randomUUID(), caseVO.getCaseId(), null, null,
                        Collections.singletonList(buildEmailChannel(cpsNotification)))
        );
    }

    private EmailChannel buildEmailChannel(final CPSNotificationVO cpsNotification) {
        final EmailChannel.Builder emailChannelBuilder = EmailChannel.emailChannel();
        final Map<String, Object> personalisation = new HashMap<>();
        String templateId = "";

        emailChannelBuilder.withSendToAddress(cpsNotification.getCpsEmailAddress());
        cpsNotification.getCaseVO().ifPresent(caseVO -> personalisation.put(URN, caseVO.getCaseURN()));

        cpsNotification.getDefendantVO().ifPresent(
                defendantVO -> {
                    if (Objects.nonNull(defendantVO.getLegalEntityName())) {
                        personalisation.put(SURNAME, defendantVO.getLegalEntityName());
                    } else {
                        personalisation.put(SURNAME, defendantVO.getLastName());
                        personalisation.put(FIRST_NAME, defendantVO.getFirstName());
                        personalisation.put(MIDDLE_NAME, defendantVO.getMiddleName());
                    }
                }
        );

        personalisation.put(HEARING_DATE, cpsNotification.getHearingVO().getHearingDate());
        personalisation.put(COURT_CENTRE_NAME, cpsNotification.getHearingVO().getCourtName());

        cpsNotification.getDefenceOrganisationVO().ifPresent(defenceOrganisationVO -> {
            personalisation.put(ORGANISATION_NAME, defenceOrganisationVO.getName());
            personalisation.put(ADDRESS_LINE_1, defenceOrganisationVO.getAddressLine1());
            personalisation.put(ADDRESS_LINE_2, defenceOrganisationVO.getAddressLine2());
            personalisation.put(ADDRESS_LINE_3, defenceOrganisationVO.getAddressLine3());
            personalisation.put(ADDRESS_LINE_4, defenceOrganisationVO.getAddressLine4());
            personalisation.put(POSTCODE, defenceOrganisationVO.getPostcode());
            personalisation.put(EMAIL, defenceOrganisationVO.getEmail());
            personalisation.put(PHONE, defenceOrganisationVO.getPhoneNumber());
        });

        emailChannelBuilder.withPersonalisation(new Personalisation(personalisation));

        try {
            if (cpsNotification.getTemplateType() == EmailTemplateType.INSTRUCTION) {
                templateId = applicationParameters.getDefenceInstructionTemplateId();
            } else if (cpsNotification.getTemplateType() == EmailTemplateType.DISASSOCIATION) {
                templateId = applicationParameters.getDefenceDisassociationTemplateId();
            } else if (cpsNotification.getTemplateType() == EmailTemplateType.ASSOCIATION) {
                templateId = applicationParameters.getDefenceAssociationTemplateId();
            }
            emailChannelBuilder.withTemplateId(UUID.fromString(templateId));
        } catch (final IllegalArgumentException ex) {
            throw new InvalidNotificationException(String.format("cant notify %s invalid template id: \"%s\"", cpsNotification.getCpsEmailAddress(), templateId), ex);
        }

        return emailChannelBuilder.build();
    }

    private void sendNotificationToThirdParties(final JsonEnvelope event, final CourtApplication courtApplication, final Boolean isWelshTranslationRequired, final CourtCentre courtCentre, final String hearingDate, final String hearingTime, final JurisdictionType jurisdictionType, final Boolean isAmended, final LocalDate issueDate, final InformantNotificationRequired informantNotificationRequired) {
        LOGGER.warn("****** sendNotificationToThirdParties courtApplicationID= {}", courtApplication.getId());
        final List<CourtApplicationParty> thirdParties = ofNullable(courtApplication.getThirdParties()).map(r -> courtApplication.getThirdParties()).orElse(new ArrayList<>());

        thirdParties.forEach(courtApplicationParty -> {
            if (courtApplicationParty.getProsecutingAuthority() != null) {
                informantNotificationRequired.setNotificationAlreadySentTrue();
            }
            sendNotification(event, UUID.randomUUID(), courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, courtApplicationParty, jurisdictionType, "YES", isAmended, issueDate);
        });
    }


    private void sendNotificationToRespondents(final JsonEnvelope event, final CourtApplication courtApplication, final Boolean isWelshTranslationRequired, final CourtCentre courtCentre, final String hearingDate, final String hearingTime, final JurisdictionType jurisdictionType, final Boolean isAmended, final LocalDate issueDate, final InformantNotificationRequired informantNotificationRequired) {
        LOGGER.warn("****** sendNotificationToRespondents courtApplicationID= {}", courtApplication.getId());
        final List<CourtApplicationParty> respondents = ofNullable(courtApplication.getRespondents()).map(r -> courtApplication.getRespondents()).orElse(new ArrayList<>());
        respondents.forEach(courtApplicationParty -> {
            if (nonNull(courtApplicationParty) && !isCpsProsecutor(event, courtApplicationParty.getProsecutingAuthority())) {
                if (courtApplicationParty.getProsecutingAuthority() != null) {
                    informantNotificationRequired.setNotificationAlreadySentTrue();
                }
                final Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation = getAssociatedDefenceOrganisation(event, courtApplicationParty.getMasterDefendant());
                sendNotification(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, courtApplicationParty, associatedDefenceOrganisation, isAmended, issueDate);
            }
        });
    }

    private void sendNotificationToApplicant(final JsonEnvelope event, final CourtApplication courtApplication, final Boolean isWelshTranslationRequired, final CourtCentre courtCentre, final String hearingDate, final String hearingTime, final JurisdictionType jurisdictionType, final Boolean isAmended, final LocalDate issueDate, final InformantNotificationRequired informantNotificationRequired) {
        LOGGER.warn("****** sendNotificationToApplicant courtApplicationID= {}", courtApplication.getId());
        final CourtApplicationParty courtApplicationParty = courtApplication.getApplicant();

        if (nonNull(courtApplicationParty) && !isCpsProsecutor(event, courtApplicationParty.getProsecutingAuthority())) {
            if (courtApplicationParty.getProsecutingAuthority() != null) {
                informantNotificationRequired.setNotificationAlreadySentTrue();
            }
            final Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation = getAssociatedDefenceOrganisation(event, courtApplicationParty.getMasterDefendant());
            sendNotification(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, courtApplicationParty, associatedDefenceOrganisation, isAmended, issueDate);
        }
    }

    private void sendNotificationToInformant(final JsonEnvelope event, final CourtApplication courtApplication, final Boolean isWelshTranslationRequired, final CourtCentre courtCentre, final String hearingDate, final String hearingTime, final JurisdictionType jurisdictionType, final Boolean isAmended, final LocalDate issueDate, final InformantNotificationRequired informantNotificationRequired) {
        LOGGER.warn("****** sendNotificationToInformant courtApplicationID= {}", courtApplication.getId());
        final UUID prosecutionAuthorityId = courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityId();
        final CourtApplicationParty courtApplicationParty = courtApplicationService.getCourtApplicationPartyByProsecutingAuthority(prosecutionAuthorityId, event);
        if (nonNull(courtApplicationParty) && !isCpsProsecutor(event, courtApplicationParty.getProsecutingAuthority())) {
            final Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation = getAssociatedDefenceOrganisation(event, courtApplicationParty.getMasterDefendant());
            sendNotification(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, courtApplicationParty, associatedDefenceOrganisation, isAmended, issueDate);
        }

    }

    private void sendNotification(JsonEnvelope event, CourtApplication courtApplication, Boolean isWelshTranslationRequired, CourtCentre courtCentre, String hearingDate, String hearingTime, JurisdictionType jurisdictionType, CourtApplicationParty courtApplicationParty, Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation, Boolean isAmended, LocalDate issueDate) {
        final PostalNotificationDetails postalNotificationDetails = buildPostalNotificationDetails(courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, courtApplicationParty, jurisdictionType, isAmended, issueDate);
        if (isAssociatedDefenceOrganisationEmailOrAddress(associatedDefenceOrganisation)) {
            final Optional<Address> addressOptional = associatedDefenceOrganisation.filter(defenceOrganisation -> nonNull(defenceOrganisation.getAddress())).map(defenceOrganisation -> toAddress(defenceOrganisation.getAddress()));
            final Optional<String> emailAddressOptional = associatedDefenceOrganisation.filter(defenceOrganisation -> nonNull(defenceOrganisation.getEmail())).map(AssociatedDefenceOrganisation::getEmail);
            sendNotification(event, UUID.randomUUID(), postalNotificationDetails, EMPTY, emailAddressOptional, addressOptional);
        } else {
            sendNotification(event, UUID.randomUUID(), courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, courtApplicationParty, jurisdictionType, EMPTY, isAmended, issueDate);
        }
    }

    private void sendNotification(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final Boolean isWelTranslationRequired, final CourtCentre courtCentre,
                                  final String hearingDate, final String hearingTime, final CourtApplicationParty courtApplicationParty, JurisdictionType jurisdictionType, final String thirdParty,
                                  final Boolean isAmended, final LocalDate issueDate) {

        final Optional<Address> addressOptional = getApplicantAddress(courtApplicationParty);
        final Optional<String> emailAddressOptional = getCourtApplicationPartyEmailAddress(courtApplicationParty);

        final PostalNotificationDetails postalNotificationDetails = buildPostalNotificationDetails(courtApplication, isWelTranslationRequired, courtCentre, hearingDate, hearingTime, courtApplicationParty, jurisdictionType, isAmended, issueDate);
        sendNotification(event, notificationId, postalNotificationDetails, thirdParty, emailAddressOptional, addressOptional);
    }

    private PostalNotificationDetails buildPostalNotificationDetails(final CourtApplication courtApplication, final Boolean isWelTranslationRequired, final CourtCentre courtCentre, final String hearingDate,
                                                                     final String hearingTime, final CourtApplicationParty courtApplicationParty, final JurisdictionType jurisdictionType, final Boolean isAmended, final LocalDate issueDate) {

        final PostalNotificationDetails postalNotificationDetails = new PostalNotificationDetails();
        postalNotificationDetails.setCourtApplication(courtApplication);
        postalNotificationDetails.setWelTranslationRequired(isWelTranslationRequired);
        postalNotificationDetails.setCourtCentre(courtCentre);
        postalNotificationDetails.setHearingDate(hearingDate);
        postalNotificationDetails.setHearingTime(hearingTime);
        postalNotificationDetails.setCourtApplicationParty(courtApplicationParty);
        postalNotificationDetails.setJurisdictionType(jurisdictionType);
        postalNotificationDetails.setAmended(isAmended);
        postalNotificationDetails.setIssueDate(issueDate);
        return postalNotificationDetails;
    }

    private void sendNotification(final JsonEnvelope event, final UUID notificationId, final PostalNotificationDetails postalNotificationDetails, final String thirdParty, final Optional<String> emailAddressOptional, final Optional<Address> addressOptional) {

        final PostalNotification postalNotification = postalService.getPostalNotificationForCourtApplicationParty(event, postalNotificationDetails.getHearingDate(), postalNotificationDetails.getHearingTime(), postalNotificationDetails.getCourtApplication().getApplicationReference(), postalNotificationDetails.getCourtApplication().getType().getType(), postalNotificationDetails.getCourtApplication().getType().getTypeWelsh(), postalNotificationDetails.getCourtApplication().getType().getLegislation(), postalNotificationDetails.getCourtApplication().getType().getLegislationWelsh(), postalNotificationDetails.getCourtCentre(), postalNotificationDetails.getCourtApplicationParty(), postalNotificationDetails.getJurisdictionType(), postalNotificationDetails.getCourtApplication().getApplicationParticulars(), postalNotificationDetails.getCourtApplication(), thirdParty, postalNotificationDetails.getAmended(), postalNotificationDetails.getWelTranslationRequired(), postalNotificationDetails.getIssueDate());

        final JsonObject notificationPayload = objectToJsonObjectConverter.convert(postalNotification);
        final UUID materialId = documentGeneratorService.generateDocument(event, notificationPayload, PostalService.POSTAL_NOTIFICATION, sender, null, postalNotificationDetails.getCourtApplication().getId(), false);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);

        if (Boolean.TRUE.equals(postalNotificationDetails.getWelTranslationRequired())) {
            postalService.sendPostalNotificationAaag(event, postalNotificationDetails.getCourtApplication().getId(), null, materialId);
        } else {
            emailAddressOptional.ifPresent(emailAddress -> sendEmail(event, notificationId, null, postalNotificationDetails.getCourtApplication().getId(), null, Collections.singletonList(buildEmailChannel(emailAddress, postalNotificationDetails.getCourtApplication().getApplicationReference(), postalNotificationDetails.getCourtApplication().getType().getType(), postalNotificationDetails.getCourtApplication().getType().getLegislation(), postalNotificationDetails.getHearingDate(), postalNotificationDetails.getHearingTime(), ofNullable(postalNotificationDetails.getCourtCentre()).map(CourtCentre::getName).orElse(EMPTY), ofNullable(postalNotificationDetails.getCourtCentre()).map(CourtCentre::getAddress).orElse(null), materialUrl))));

            emailAddressOptional.ifPresent(email -> {
                final CourtDocument courtDocument = postalService.courtDocument(postalNotificationDetails.getCourtApplication().getId(), materialId, event, null);
                final JsonObject courtDocumentPayload = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

                LOGGER.info("creating court document payload - {}", courtDocumentPayload);

                sender.send(enveloper.withMetadataFrom(event, PostalService.PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(courtDocumentPayload));
            });

            addressOptional.ifPresent(address -> {
                // send postal notification only if email notification was not sent.
                if (!emailAddressOptional.isPresent()) {
                    // linkedCaseId null; GPE-15039 Commented temporarily
                    postalService.sendPostalNotification(event, postalNotificationDetails.getCourtApplication().getId(), postalNotification, null);
                }
            });
        }

    }

    private boolean isAssociatedDefenceOrganisationEmailOrAddress(final Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation){
        return  associatedDefenceOrganisation.isPresent() && nonNull(associatedDefenceOrganisation.get().getEmail())
                || associatedDefenceOrganisation.isPresent() && nonNull(associatedDefenceOrganisation.get().getAddress());
    }

    private Optional<Address> getApplicantAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        Optional<Address> addressOptional = Optional.empty();

        if (personOptional.isPresent()) {

            addressOptional = personOptional.map(Person::getAddress);

        } else if (organisationOptional.isPresent()) {

            addressOptional = organisationOptional.map(Organisation::getAddress);

        } else if (defendantOptional.isPresent()) {

            addressOptional = getDefendantAddress(defendantOptional.get());

        } else if (prosecutingAuthorityOptional.isPresent()) {

            addressOptional = prosecutingAuthorityOptional.map(ProsecutingAuthority::getAddress);
        }

        return addressOptional;
    }

    private Optional<Address> getDefendantAddress(final MasterDefendant masterDefendant) {
        Optional<Address> address = Optional.empty();

        if (nonNull(masterDefendant.getPersonDefendant()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails().getAddress())) {
            address = Optional.of(masterDefendant.getPersonDefendant().getPersonDetails().getAddress());
        }

        if (nonNull(masterDefendant.getLegalEntityDefendant()) && nonNull(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress())) {
            address = Optional.of(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress());
        }
        return address;
    }

    private Optional<String> getCourtApplicationPartyEmailAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Person> masterDefendantPersonOptional = nonNull(courtApplicationParty.getMasterDefendant()) && nonNull(courtApplicationParty.getMasterDefendant().getPersonDefendant()) ? ofNullable(courtApplicationParty.getMasterDefendant().getPersonDefendant().getPersonDetails()): Optional.empty();

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        Optional<String> emailAddress = Optional.empty();


        if (personOptional.isPresent()) {
            emailAddress = personOptional.map(Person::getContact).map(ContactNumber::getPrimaryEmail);

        }  else if (organisationOptional.isPresent()) {

            emailAddress = organisationOptional.map(Organisation::getContact).map(ContactNumber::getPrimaryEmail);

        } else if (defendantOptional.isPresent()) {

            emailAddress = getDefendantEmailAddress(defendantOptional.get());

        } else if (prosecutingAuthorityOptional.isPresent()) {

            emailAddress = prosecutingAuthorityOptional.map(ProsecutingAuthority::getContact).map(ContactNumber::getPrimaryEmail);
        } else if (masterDefendantPersonOptional.isPresent()) {
            emailAddress = masterDefendantPersonOptional.map(Person::getContact).map(ContactNumber::getPrimaryEmail);
        }

        if (!emailAddress.isPresent()) {
            emailAddress = masterDefendantPersonOptional.map(Person::getContact).map(ContactNumber::getPrimaryEmail);
        }

        return emailAddress;
    }

    private Optional<String> getDefendantEmailAddress(final MasterDefendant masterDefendant) {
        final Optional<String> emailAddress = Optional.empty();

        if (nonNull(masterDefendant.getPersonDefendant()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails().getContact()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails().getContact().getPrimaryEmail())) {
            return Optional.of(masterDefendant.getPersonDefendant().getPersonDetails().getContact().getPrimaryEmail());
        }

        if (nonNull(masterDefendant.getLegalEntityDefendant()) && nonNull(masterDefendant.getLegalEntityDefendant().getOrganisation().getContact()) && nonNull(masterDefendant.getLegalEntityDefendant().getOrganisation().getContact().getPrimaryEmail())) {
            return Optional.of(masterDefendant.getLegalEntityDefendant().getOrganisation().getContact().getPrimaryEmail());
        }
        return emailAddress;
    }

    private EmailChannel buildEmailChannel(final String destination,
                                           final String applicationReference, final String applicationType,
                                           final String legislationText, final String hearingDate,
                                           final String hearingTime,
                                           final String courtCentreName, final Address address,
                                           final String materialUrl) {

        final String APPLICATION_REFERENCE = "application_reference";

        final String APPLICATION_TYPE = "application_type";

        final String LEGISLATION_TEXT = "legislation_text";

        final String DATED = "dated";

        final String TIME = "time";

        final String COURT_CENTRE_NAME_LOCAL = "courtCentreName";

        final String ADDRESS_1 = "address1";

        final String ADDRESS_2 = "address2";

        final String ADDRESS_3 = "address3";

        final EmailChannel.Builder emailChannelBuilder = EmailChannel.emailChannel();

        emailChannelBuilder.withSendToAddress(destination);

        final Map<String, Object> personalisation = new HashMap<>();

        personalisation.put(APPLICATION_REFERENCE, applicationReference);
        personalisation.put(APPLICATION_TYPE, applicationType);
        personalisation.put(LEGISLATION_TEXT, legislationText);
        personalisation.put(DATED, hearingDate);
        personalisation.put(TIME, hearingTime);
        personalisation.put(COURT_CENTRE_NAME_LOCAL, courtCentreName);

        final Optional<Address> addressOptional = ofNullable(address);

        if (addressOptional.isPresent()) {
            final String joined =
                    Stream.of(address.getAddress3(), address.getAddress4(), address.getAddress5(), address.getPostcode())
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(Collectors.joining(" "));

            personalisation.put(ADDRESS_1, addressOptional.map(Address::getAddress1).orElse(EMPTY));
            personalisation.put(ADDRESS_2, addressOptional.map(Address::getAddress2).orElse(EMPTY));
            personalisation.put(ADDRESS_3, joined);
        } else {
            personalisation.put(ADDRESS_1, EMPTY);
            personalisation.put(ADDRESS_2, EMPTY);
            personalisation.put(ADDRESS_3, EMPTY);
        }

        emailChannelBuilder.withPersonalisation(new Personalisation(personalisation));
        emailChannelBuilder.withMaterialUrl(materialUrl);

        try {
            emailChannelBuilder.withTemplateId(UUID.fromString(applicationParameters.getApplicationTemplateId()));
        } catch (final IllegalArgumentException ex) {
            throw new InvalidNotificationException(String.format("cant notify %s invalid template id: \"%s\"", destination, applicationParameters.getApplicationTemplateId()), ex);
        }

        return emailChannelBuilder.build();
    }

    private Notification createNotification(final UUID notificationId, final EmailChannel emailChannel) {

        final Notification emailNotification = new Notification();

        emailNotification.setNotificationId(notificationId);

        emailNotification.setMaterialUrl(emailChannel.getMaterialUrl());

        emailNotification.setSendToAddress(emailChannel.getSendToAddress());

        emailNotification.setTemplateId(emailChannel.getTemplateId());

        final Map<String, Object> additionalProperties = of(emailChannel).map(e -> emailChannel.getPersonalisation()).map(Personalisation::getAdditionalProperties).orElse(new HashMap<>());

        final Map<String, String> personalisation = new HashMap<>();

        additionalProperties.forEach((k, v) -> personalisation.put(k, ofNullable(v).map(Object::toString).orElse("")));

        emailNotification.setPersonalisation(personalisation);

        emailNotification.setReplyToAddress(emailChannel.getReplyToAddress());

        return emailNotification;
    }

    private JsonArrayBuilder buildNotifications(final UUID notificationId, final List<EmailChannel> emailNotifications) {

        final List<Notification> notifications = emailNotifications.stream().map(emailChannel -> createNotification(notificationId, emailChannel)).collect(Collectors.toList());

        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();

        notifications.forEach(notification -> jsonArrayBuilder.add(createNotificationJsonObject(notification)));

        return jsonArrayBuilder;
    }

    private String getName(PartyType partyType) {
        switch (partyType) {
            case CASE:
                return CASE_ID;
            case APPLICATION:
                return APPLICATION_ID;
            default:
                return MATERIAL_ID;
        }
    }

    public void sendNotification(final JsonEnvelope event, final CourtApplication courtApplication, final Boolean isWelshTranslationRequired, final CourtCentre courtCentre, final ZonedDateTime hearingStartDateTime, final JurisdictionType jurisdictionType, final Boolean isAmended) {
        requireNonNull(courtApplication);
        LOGGER.warn("****** sendNotification courtApplicationID= {}", courtApplication.getId());
        if (courtApplication.getType().getSummonsTemplateType().equals(NOT_APPLICABLE)) {
            final String hearingDate = hearingStartDateTime.toLocalDate().toString();
            final String hearingTime = getCourtTime(hearingStartDateTime);

            InformantNotificationRequired informantNotificationRequired = new InformantNotificationRequired();
            sendNotificationToApplicant(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, LocalDate.now(), informantNotificationRequired);
            sendNotificationToRespondents(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, LocalDate.now(), informantNotificationRequired);
            sendNotificationToThirdParties(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, LocalDate.now(), informantNotificationRequired);
            LOGGER.warn("****** sendNotification informantNotificationRequired= {}", informantNotificationRequired);
            LOGGER.warn("****** sendNotification courtApplication.getType()= {}", courtApplication.getType());
            LOGGER.warn("****** sendNotification courtApplication.getType().getAppealFlag()= {}", courtApplication.getType().getAppealFlag());
            if (courtApplication.getType() != null
                    && Boolean.TRUE.equals(courtApplication.getType().getAppealFlag())
                    && informantNotificationRequired.shouldSendNotificationToInformant()) {
                LOGGER.warn("****** sendNotification sending notification");
                sendNotificationToInformant(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, LocalDate.now(), informantNotificationRequired);
            } else {
                LOGGER.warn("****** sendNotification skipping notification");
            }
        }
    }


    public void sendNotificationForAutoApplication(final JsonEnvelope event, final SendNotificationForAutoApplicationInitiated sendNotificationForAutoApplication) {
        final CourtApplication courtApplication = sendNotificationForAutoApplication.getCourtApplication();
        final ZonedDateTime hearingStartDateTime = ZonedDateTime.parse(sendNotificationForAutoApplication.getHearingStartDateTime());
        final Boolean isWelshTranslationRequired = sendNotificationForAutoApplication.getIsWelshTranslationRequired();
        final CourtCentre courtCentre = sendNotificationForAutoApplication.getCourtCentre();
        final JurisdictionType jurisdictionType = sendNotificationForAutoApplication.getJurisdictionType();
        final Boolean isAmended = sendNotificationForAutoApplication.getIsAmended();
        final LocalDate issueDate = sendNotificationForAutoApplication.getIssueDate();
        requireNonNull(courtApplication);

        if (courtApplication.getType().getSummonsTemplateType().equals(NOT_APPLICABLE)) {
            final String hearingDate = hearingStartDateTime.toLocalDate().toString();
            final String hearingTime = getCourtTime(hearingStartDateTime);

            InformantNotificationRequired informantNotificationRequired = new InformantNotificationRequired();

            sendNotificationToApplicant(event, courtApplication, false, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, issueDate, informantNotificationRequired);
            sendNotificationToRespondents(event, courtApplication, isWelshTranslationRequired, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, issueDate, informantNotificationRequired);
            sendNotificationToThirdParties(event, courtApplication, false, courtCentre, hearingDate, hearingTime, jurisdictionType, isAmended, issueDate, informantNotificationRequired);

            if(nonNull(isWelshTranslationRequired) && isWelshTranslationRequired) {
                final String applicantNameFromMasterDefendant = nonNull(courtApplication.getApplicant().getMasterDefendant())  && nonNull(courtApplication.getApplicant().getMasterDefendant().getPersonDefendant()) ? courtApplication.getApplicant().getMasterDefendant().getPersonDefendant().getPersonDetails().getLastName() + " " + courtApplication.getApplicant().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName() : "";
                final String applicationName = nonNull(courtApplication.getApplicant().getPersonDetails()) ? courtApplication.getApplicant().getPersonDetails().getLastName() + " " + courtApplication.getApplicant().getPersonDetails().getFirstName() : applicantNameFromMasterDefendant;
                final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder().add(MASTER_DEFENDANT_ID, courtApplication.getApplicant().getId().toString()).add(DEFENDANT_NAME, applicationName).add(CASE_URN, courtApplication.getApplicationReference());
                final JsonObjectBuilder welshTranslationRequiredBuilder = createObjectBuilder().add("welshTranslationRequired", jsonObjectBuilder.build());
                sender.send(Enveloper.envelop(welshTranslationRequiredBuilder.build()).withName(PUBLIC_PROGRESSION_EVENTS_WELSH_TRANSLATION_REQUIRED).withMetadataFrom(event));
            }
        }
    }

    private Optional<AssociatedDefenceOrganisation> getAssociatedDefenceOrganisation(final JsonEnvelope event, final MasterDefendant masterDefendant) {
        final Optional<UUID> defendantOptional = Optional.ofNullable(masterDefendant)
                .filter(masterDef -> isNotEmpty(masterDef.getDefendantCase()))
                .map(masterDef -> masterDef.getDefendantCase().get(0).getDefendantId());

        if (defendantOptional.isPresent()) {
            return Optional.ofNullable(defenceService.getDefenceOrganisationByDefendantId(event, defendantOptional.get()));
        }
        return Optional.empty();
    }

    private Address toAddress(final DefenceOrganisationAddress address) {
        return Address.address()
                .withAddress1(address.getAddress1())
                .withAddress2(address.getAddress2())
                .withAddress3(address.getAddress3())
                .withAddress4(address.getAddress4())
                .withPostcode(address.getAddressPostcode())
                .build();
    }

    private boolean isCpsProsecutor(final JsonEnvelope event, final ProsecutingAuthority prosecutingAuthority) {
        if (nonNull(prosecutingAuthority)) {
            final Optional<JsonObject> prosecutorJsonObject = referenceDataService.getProsecutorV2(event, prosecutingAuthority.getProsecutionAuthorityId(), requester);
            return prosecutorJsonObject.isPresent() && prosecutorJsonObject.get().getBoolean("cpsFlag", false);
        }

        return false;
    }
}
