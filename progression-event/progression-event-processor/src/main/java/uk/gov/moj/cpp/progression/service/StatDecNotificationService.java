package uk.gov.moj.cpp.progression.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.nows.InvalidNotificationException;

import javax.inject.Inject;
import javax.json.JsonObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class StatDecNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatDecNotificationService.class);

    @Inject
    private StatDecLetterService statDecLetterService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private ApplicationParameters applicationParameters;

    @Inject
    private MaterialUrlGenerator materialUrlGenerator;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    public static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    public static final String COURT_DOCUMENT = "courtDocument";
    public static final String MATERIAL_ID = "materialId";
    private static final UUID ORDERS_NOTICES_DIRECTIONS_TYPE_ID = UUID.fromString("460fbe94-c002-11e8-a355-529269fb1459");
    public static final String APPLICATION_PDF = "application/pdf";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String STAT_DEC = "STATDEC";

    public void sendNotification(final JsonEnvelope event, final UUID notificationId, final CourtApplication courtApplication, final CourtCentre courtCentre,  final ZonedDateTime hearingStartDateTime, final JurisdictionType jurisdictionType, final String documentTemplateName) {
        Objects.requireNonNull(courtApplication);

        final CourtApplicationParty courtApplicationParty = courtApplication.getApplicant();

        final Optional<String> emailAddressOptional = getApplicantEmailAddress(courtApplicationParty);

        final Optional<Address> addressOptional = getApplicantAddress(courtApplicationParty);


        //Generating appointment letter

        final UUID materialId = statDecLetterService.generateAppointmentLetterDocument(event, hearingStartDateTime, courtApplication, courtCentre, courtApplicationParty, jurisdictionType, documentTemplateName);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("materialId generated from document generator service - {}", materialId);
        }

        // adding material as court document
        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(COURT_DOCUMENT, objectToJsonObjectConverter.convert(buildCourtDocument(event, courtApplication, materialId))).build();

        sender.send(envelopeFrom(metadataFrom(event.metadata()).withName(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT), payload));


        // Sending Email with attachment as letter if email is Present
        emailAddressOptional.ifPresent(emailAddress ->
                notificationService.sendEmail(event, notificationId, null, courtApplication.getId(), materialId, Collections.singletonList(buildEmailChannel(emailAddress, materialId))));

        // Sending letter if email is not  Present

        addressOptional.ifPresent(address -> {
            if (!emailAddressOptional.isPresent()) {
                notificationService.sendLetter(event, notificationId, null, courtApplication.getId(), materialId, true);
            }
        });


    }

    private EmailChannel buildEmailChannel(final String destination, final UUID materialId) {
        final EmailChannel.Builder emailChannelBuilder = EmailChannel.emailChannel();
        emailChannelBuilder.withSendToAddress(destination);
        final String materialUrl = materialUrlGenerator.pdfFileStreamUrlFor(materialId);
        emailChannelBuilder.withMaterialUrl(materialUrl);

        try {
            emailChannelBuilder.withTemplateId(UUID.fromString(applicationParameters.getStatDecSendAppointmentLetterTemplateId()));
        } catch (final IllegalArgumentException ex) {
            throw new InvalidNotificationException(String.format("cant notify %s invalid template id: \"%s\"", destination, applicationParameters.getStatDecSendAppointmentLetterTemplateId()), ex);
        }

        return emailChannelBuilder.build();
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


    private Optional<String> getApplicantEmailAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

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

    private CourtDocument buildCourtDocument(final JsonEnvelope envelope, final CourtApplication courtApplication, final UUID materialId) {

        final DocumentCategory.Builder documentCategoryBuilder = DocumentCategory.documentCategory();
        final List<DefendantCase> defendantCaseList = courtApplication.getApplicant().getMasterDefendant().getDefendantCase();
        final String fileName =   format("%s_%s.pdf", STAT_DEC, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));

        if (isNotEmpty(defendantCaseList)) {
            final List<UUID> defendants = defendantCaseList.stream()
                    .map(defendant -> defendant.getDefendantId())
                    .collect(Collectors.toList());

            documentCategoryBuilder.withDefendantDocument(DefendantDocument.defendantDocument()
                    .withProsecutionCaseId(defendantCaseList.get(0).getCaseId())
                    .withDefendants(defendants)
                    .build());
        }

        final Material material = Material.material().withId(materialId)
                .withUploadDateTime(ZonedDateTime.now())
                .build();

        final JsonObject documentTypeData = referenceDataService.getDocumentTypeAccessData(ORDERS_NOTICES_DIRECTIONS_TYPE_ID, envelope, requester)
                .orElseThrow(() -> new RuntimeException("Unable to retrieve document type details for '" + ORDERS_NOTICES_DIRECTIONS_TYPE_ID + "'"));

        return CourtDocument.courtDocument()
                .withCourtDocumentId(randomUUID())
                .withDocumentCategory(documentCategoryBuilder.build())
                .withDocumentTypeDescription(documentTypeData.getString("section"))
                .withDocumentTypeId(ORDERS_NOTICES_DIRECTIONS_TYPE_ID)
                .withMimeType(APPLICATION_PDF)
                .withName(fileName)
                .withMaterials(Collections.singletonList(material))
                .build();
    }

}
