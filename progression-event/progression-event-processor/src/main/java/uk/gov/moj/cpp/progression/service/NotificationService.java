package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.helper.SummonsDataHelper.getCourtTime;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationSummonsRecipientType;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.email.PartyType;
import uk.gov.moj.cpp.progression.nows.InvalidNotificationException;
import uk.gov.moj.cpp.progression.nows.Notification;

import java.time.ZonedDateTime;
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
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by satishkumar on 12/11/2018.
 */
@SuppressWarnings("WeakerAccess")
public class NotificationService {

    public static final String CASE_ID = "caseId";
    public static final String NOTIFICATION_ID = "notificationId";
    public static final String MATERIAL_ID = "materialId";
    public static final String STATUS_CODE = "statusCode";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class.getName());
    private static final String APPLICATION_ID = "applicationId";
    private static final String ACCEPTED_TIME = "acceptedTime";
    private static final String NOTIFICATIONS = "notifications";
    private static final String FAILED_TIME = "failedTime";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String SENT_TIME = "sentTime";
    private static final String TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String REPLY_TO_ADDRESS = "replyToAddress";
    private static final String PERSONALISATION = "personalisation";
    private static final String EMPTY = "";
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
    private ReferenceDataService referenceDataService;
    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private PostalService postalService;

    public void sendEmail(final JsonEnvelope sourceEnvelope, final UUID notificationId, final UUID caseId, final UUID applicationId, final UUID materialId, final List<EmailChannel> emailNotifications) {

        if (nonNull(emailNotifications)) {

            final JsonArrayBuilder notificationBuilder = buildNotifications(notificationId, emailNotifications);

            final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                    .add(NOTIFICATIONS, notificationBuilder);

            Optional.ofNullable(caseId).ifPresent(id -> {
                systemIdMapperService.mapNotificationIdToCaseId(caseId, notificationId);
                payloadBuilder.add(CASE_ID, id.toString());
            });

            Optional.ofNullable(applicationId).ifPresent(id -> {
                systemIdMapperService.mapNotificationIdToApplicationId(applicationId, notificationId);
                payloadBuilder.add(APPLICATION_ID, id.toString());
            });

            Optional.ofNullable(materialId).ifPresent(id -> payloadBuilder.add(MATERIAL_ID, id.toString()));

            final JsonObject emailPayload = payloadBuilder.build();

            LOGGER.info("sending email payload - {}", emailPayload);

            sender.send(enveloper.withMetadataFrom(sourceEnvelope, "progression.command.email").apply(emailPayload));

        } else {

            LOGGER.warn("No Email was sent.");
        }
    }

    public void print(final JsonEnvelope sourceEnvelope, final UUID notificationId, final UUID caseId, final UUID applicationId, final UUID materialId) {

        final JsonObjectBuilder payloadBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId.toString())
                .add(MATERIAL_ID, materialId.toString());

        Optional.ofNullable(caseId).ifPresent(id -> {
            systemIdMapperService.mapNotificationIdToCaseId(caseId, notificationId);
            payloadBuilder.add(CASE_ID, id.toString());
        });

        Optional.ofNullable(applicationId).ifPresent(id -> {
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
                .add(FAILED_TIME, payload.getString(FAILED_TIME))
                .add(ERROR_MESSAGE, payload.getString(ERROR_MESSAGE));

        if (partyType == CASE) {
            jsonObjectBuilder.add(CASE_ID, targetId.toString());
        } else {
            jsonObjectBuilder.add(APPLICATION_ID, targetId.toString());
        }

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

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(NOTIFICATION_ID, notificationId)
                .add(SENT_TIME, sentTime);

        if (partyType == CASE) {
            jsonObjectBuilder.add(CASE_ID, targetId.toString());
        } else {
            jsonObjectBuilder.add(APPLICATION_ID, targetId.toString());
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

    public void sendNotification(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final CourtCentre courtCentre, final ZonedDateTime hearingStartDateTime) {

        Objects.requireNonNull(courtApplication);

        final String hearingDate = hearingStartDateTime.toLocalDate().toString();

        final String hearingTime = getCourtTime(hearingStartDateTime);

        final Optional<ApplicationSummonsRecipientType> recipientTypeOptional = ofNullable(courtApplication.getType()).map(CourtApplicationType::getApplicationSummonsRecipientType);

        //If court application type is null means no summons will be send to applicant and respondents
        if (recipientTypeOptional.isPresent()) {
            final ApplicationSummonsRecipientType applicationSummonsRecipientType = recipientTypeOptional.get();
            if (applicationSummonsRecipientType == ApplicationSummonsRecipientType.APPLICANT) {
                sendNotificationToRespondents(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime);
            } else if (applicationSummonsRecipientType == ApplicationSummonsRecipientType.RESPONDENT) {
                sendNotificationToApplicant(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime);
            }
        } else {
            sendNotificationToApplicant(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime);
            sendNotificationToRespondents(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime);
        }
    }

    private void sendNotificationToRespondents(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final CourtCentre courtCentre, final String hearingDate, final String hearingTime) {

        final List<CourtApplicationRespondent> respondents = ofNullable(courtApplication.getRespondents()).map(r -> courtApplication.getRespondents()).orElse(new ArrayList<>());

        respondents.stream().map(CourtApplicationRespondent::getPartyDetails).forEach(courtApplicationParty -> sendNotification(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime, courtApplicationParty));
    }

    private void sendNotificationToApplicant(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final CourtCentre courtCentre, final String hearingDate, final String hearingTime) {

        final CourtApplicationParty courtApplicationParty = courtApplication.getApplicant();

        sendNotification(event, notificationId, courtApplication, courtCentre, hearingDate, hearingTime, courtApplicationParty);
    }

    private void sendNotification(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final CourtCentre courtCentre, final String hearingDate, final String hearingTime, final CourtApplicationParty courtApplicationParty) {

        final Optional<Address> addressOptional = getApplicantAddress(courtApplicationParty);

        final Optional<String> emailAddressOptional = getApplicantEmailAddress(courtApplicationParty);

        emailAddressOptional.ifPresent(emailAddress -> sendEmail(event, notificationId, null, courtApplication.getId(), null, Collections.singletonList(buildEmailChannel(emailAddress,
                courtApplication.getType().getApplicationType(),
                courtApplication.getType().getApplicationLegislation(),
                hearingDate,
                hearingTime,
                ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY),
                ofNullable(courtCentre).map(CourtCentre::getAddress).orElse(null)))));

        addressOptional.ifPresent(address -> {
            if (!emailAddressOptional.isPresent()) { // send postal notification only if email notification was not sent.
                postalService.sendPostToCourtApplicationParty(
                        event,
                        hearingDate,
                        hearingTime,
                        courtApplication.getId(),
                        courtApplication.getApplicationReference(),
                        courtApplication.getType().getApplicationType(),
                        courtApplication.getType().getApplicationLegislation(),
                        courtCentre,
                        courtApplicationParty);
            }
        });
    }

    private Optional<Address> getApplicantAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<Defendant> defendantOptional = ofNullable(courtApplicationParty.getDefendant());

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

    private Optional<Address> getDefendantAddress(Defendant defendant) {
        Optional<Address> address = Optional.empty();

        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getAddress())) {
            address = Optional.of(defendant.getPersonDefendant().getPersonDetails().getAddress());
        }

        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation().getAddress())) {
            address = Optional.of(defendant.getLegalEntityDefendant().getOrganisation().getAddress());
        }
        return address;
    }

    private Optional<String> getApplicantEmailAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<Defendant> defendantOptional = ofNullable(courtApplicationParty.getDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        Optional<String> emailAddress = Optional.empty();

        if (personOptional.isPresent()) {

            emailAddress = personOptional.map(Person::getContact).map(ContactNumber::getPrimaryEmail);

        } else if (organisationOptional.isPresent()) {

            emailAddress = organisationOptional.map(Organisation::getContact).map(ContactNumber::getPrimaryEmail);

        } else if (defendantOptional.isPresent()) {

            emailAddress = getDefendantEmailAddress(defendantOptional.get());

        } else if (prosecutingAuthorityOptional.isPresent()) {

            emailAddress = prosecutingAuthorityOptional.map(ProsecutingAuthority::getContact).map(ContactNumber::getPrimaryEmail);
        }

        return emailAddress;
    }

    private Optional<String> getDefendantEmailAddress(Defendant defendant) {
        final Optional<String> emailAddress = Optional.empty();

        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getContact()) && nonNull(defendant.getPersonDefendant().getPersonDetails().getContact().getPrimaryEmail())) {
            return Optional.of(defendant.getPersonDefendant().getPersonDetails().getContact().getPrimaryEmail());
        }

        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation().getContact()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation().getContact().getPrimaryEmail())) {
            return Optional.of(defendant.getLegalEntityDefendant().getOrganisation().getContact().getPrimaryEmail());
        }
        return emailAddress;
    }

    private EmailChannel buildEmailChannel(final String destination, final String applicationType, final String legislationText, final String hearingDate, final String hearingTime,
                                           final String courtCentreName, final Address address) {

        final String APPLICATION_TYPE = "application_type";

        final String LEGISLATION_TEXT = "legislation_text";

        final String DATED = "dated";

        final String TIME = "time";

        final String COURT_CENTRE_NAME = "courtCentreName";

        final String ADDRESS_1 = "address1";

        final String ADDRESS_2 = "address2";

        final String ADDRESS_3 = "address3";


        final EmailChannel.Builder emailChannelBuilder = EmailChannel.emailChannel();

        emailChannelBuilder.withSendToAddress(destination);

        final Map<String, Object> personalisation = new HashMap<>();

        personalisation.put(APPLICATION_TYPE, applicationType);
        personalisation.put(LEGISLATION_TEXT, legislationText);
        personalisation.put(DATED, hearingDate);
        personalisation.put(TIME, hearingTime);
        personalisation.put(COURT_CENTRE_NAME, courtCentreName);

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

        try {
            emailChannelBuilder.withTemplateId(UUID.fromString(applicationParameters.getApplicationTemplateId()));
        } catch (IllegalArgumentException ex) {
            throw new InvalidNotificationException(String.format("cant notify %s invalid template id: \"%s\"", destination, applicationParameters.getApplicationTemplateId()), ex);
        }

        return emailChannelBuilder.build();
    }

    private Notification createNotification(final UUID notificationId, final EmailChannel emailChannel) {

        final Notification emailNotification = new Notification();

        emailNotification.setNotificationId(notificationId);

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

        notifications.forEach(notification -> {

            final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                    .add(NOTIFICATION_ID, notification.getNotificationId().toString())
                    .add(TEMPLATE_ID, notification.getTemplateId().toString())
                    .add(SEND_TO_ADDRESS, notification.getSendToAddress());

            if (nonNull(notification.getReplyToAddress())) {
                jsonObjectBuilder.add(REPLY_TO_ADDRESS, notification.getReplyToAddress());
            } else {
                jsonObjectBuilder.addNull(REPLY_TO_ADDRESS);
            }

            final Map<String, String> additionalProperties = notification.getPersonalisation();

            final JsonObjectBuilder personalisation = createObjectBuilder();

            additionalProperties.forEach(personalisation::add);

            jsonObjectBuilder.add(PERSONALISATION, personalisation);

            jsonArrayBuilder.add(jsonObjectBuilder);

        });

        return jsonArrayBuilder;
    }
}
