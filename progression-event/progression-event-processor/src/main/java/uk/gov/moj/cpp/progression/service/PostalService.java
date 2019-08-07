package uk.gov.moj.cpp.progression.service;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.PostalAddress;
import uk.gov.moj.cpp.progression.domain.PostalAddressee;
import uk.gov.moj.cpp.progression.domain.PostalDefendant;
import uk.gov.moj.cpp.progression.domain.PostalHearingCourtDetails;
import uk.gov.moj.cpp.progression.domain.PostalNotification;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostalService.class.getName());

    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";

    private static final String POSTAL_NOTIFICATION = "PostalNotification";

    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";

    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";

    private static final String NAME = "name";

    private static final String LJA = "lja";

    private static final String EMPTY = "";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @SuppressWarnings({"squid:S00107"})
    public void sendPostToCourtApplicationParty(final JsonEnvelope envelope,
                                                final String hearingDate,
                                                final String hearingTime,
                                                final UUID applicationId,
                                                final String applicationReference,
                                                final String applicationType,
                                                final String legislation,
                                                final CourtCentre courtCentre,
                                                final CourtApplicationParty courtApplicationParty) {

        final Optional<CourtCentre> orderingCourtOptional = ofNullable(courtCentre);

        JsonObject localJusticeArea = Json.createObjectBuilder().build();

        if(orderingCourtOptional.isPresent()) {

            final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getOrganisationUnitById(courtCentre.getId(), envelope);

            final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(IllegalArgumentException::new);

            final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(envelope, courtCentreJson.getString(LJA));

            localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        }

        final PostalNotification postalNotification = buildPostalNotification(hearingDate,
                hearingTime,
                applicationReference,
                applicationType,
                legislation,
                courtCentre,
                localJusticeArea,
                courtApplicationParty);

        sendPostalNotification(sender, envelope, applicationId, postalNotification);

    }

    private void sendPostalNotification(final Sender sender, final JsonEnvelope envelope, final UUID applicationId, final PostalNotification postalNotification) {

        final JsonObject postalNotificationPayload = objectToJsonObjectConverter.convert(postalNotification);

        LOGGER.info("Sending Postal Notification payload - {}", postalNotificationPayload);

        final UUID materialId = documentGeneratorService.generateDocument(envelope, postalNotificationPayload, POSTAL_NOTIFICATION, sender, null, applicationId);

        final CourtDocument courtDocument = courtDocument(applicationId, materialId);

        final JsonObject courtDocumentPayload = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

        LOGGER.info("creating court document payload - {}", courtDocumentPayload);

        sender.send(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(courtDocumentPayload));

    }

    @SuppressWarnings({"squid:S00107"})
    private PostalNotification buildPostalNotification(final String hearingDate,
                                                       final String hearingTime,
                                                       final String applicationReference,
                                                       final String applicationType,
                                                       final String legislation,
                                                       final CourtCentre courtCentre,
                                                       final JsonObject localJusticeArea,
                                                       final CourtApplicationParty courtApplicationParty) {

        final PostalNotification.Builder builder = PostalNotification.builder()
                .withReference(ofNullable(applicationReference).orElse(EMPTY))
                .withLjaCode(ofNullable(localJusticeArea).map(area -> area.getString(NATIONAL_COURT_CODE, EMPTY)).orElse(EMPTY))
                .withLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME, EMPTY)).orElse(EMPTY))
                .withIssueDate(LocalDate.now())
                .withApplicationType(ofNullable(applicationType).orElse(EMPTY))
                .withLegislationText(ofNullable(legislation).orElse(EMPTY))
                .withCourtCentreName(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY));

        //Check if the applicant is a defendant
        final Optional<Defendant> defendantOptional = ofNullable(courtApplicationParty.getDefendant());

        defendantOptional.ifPresent(defendant -> builder.withDefendant(buildDefendant(defendant)));

        final PostalAddressee postalAddressee = PostalAddressee.builder()
                .withName(getName(courtApplicationParty))
                .withAddress(getAddress(courtApplicationParty))
                .build();

        builder.withAddressee(postalAddressee);

        @SuppressWarnings({"squid:S2259"})
        final PostalAddress postalAddress = getPostalAddress(ofNullable(courtCentre).map(CourtCentre::getAddress).orElse(null));

        final PostalHearingCourtDetails.Builder hearingCourtDetailsBuilder = PostalHearingCourtDetails.builder()
                .withCourtName(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY))
                .withHearingDate(hearingDate)
                .withHearingTime(hearingTime);

        ofNullable(postalAddress).ifPresent(hearingCourtDetailsBuilder::withCourtAddress);

        builder.withHearingCourtDetails(hearingCourtDetailsBuilder.build());

        return builder.build();
    }

    private PostalDefendant buildDefendant(final Defendant defendant) {
        return PostalDefendant.builder()
                .withName(defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName())
                .withDateOfBirth(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth())
                .withAddress(getPostalAddress(defendant.getPersonDefendant().getPersonDetails().getAddress()))
                .build();
    }

    private PostalAddress getPostalAddress(final Address address) {
        final PostalAddress.Builder postalAddressBuilder = PostalAddress.builder();
        final Optional<Address> addressOptional = ofNullable(address);
        if (addressOptional.isPresent()) {
            final String joined =
                    Stream.of(address.getAddress3(), address.getAddress4(), address.getAddress5())
                            .filter(s -> s != null && !s.isEmpty())
                            .collect(Collectors.joining(" "));

            postalAddressBuilder.withLine1(addressOptional.map(Address::getAddress1).orElse(EMPTY));
            postalAddressBuilder.withLine2(addressOptional.map(Address::getAddress2).orElse(EMPTY));
            postalAddressBuilder.withLine3(joined);
            postalAddressBuilder.withPostCode(address.getPostcode());
        }
        return postalAddressBuilder.build();
    }

    private String getName(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<Defendant> defendantOptional = ofNullable(courtApplicationParty.getDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        if (personOptional.isPresent()) {
            return personOptional.map(person -> {
                final String firstName = ofNullable(person.getFirstName()).orElse(EMPTY);
                final String lastName = ofNullable(person.getLastName()).orElse(EMPTY);
                return firstName + " " +lastName;
            }).orElse(EMPTY);
        }

        if (organisationOptional.isPresent()) {
            return organisationOptional.map(Organisation::getName).orElse(EMPTY);
        }

        return defendantOptional.map(Defendant::getPersonDefendant).map(PersonDefendant::getPersonDetails)
                .map(person -> {
                    final String firstName = ofNullable(person.getFirstName()).orElse(EMPTY);
                    final String lastName = ofNullable(person.getLastName()).orElse(EMPTY);
                    return firstName + " " +lastName;
                })
                .orElseGet(() -> prosecutingAuthorityOptional.map(ProsecutingAuthority::getName).orElse(EMPTY));
    }

    private PostalAddress getAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<Defendant> defendantOptional = ofNullable(courtApplicationParty.getDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        if (personOptional.isPresent()) {
            return getPostalAddress(personOptional.get().getAddress());
        }

        if (organisationOptional.isPresent()) {
            return getPostalAddress(organisationOptional.get().getAddress());
        }

        return defendantOptional.map(defendant -> getPostalAddress(defendant.getPersonDefendant().getPersonDetails().getAddress()))
                .orElseGet(() -> prosecutingAuthorityOptional.map(prosecutingAuthority -> getPostalAddress(prosecutingAuthority.getAddress())).orElse(null));
    }

    private CourtDocument courtDocument(final UUID applicationId, final UUID materialId) {
        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withApplicationId(applicationId)
                .build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withApplicationDocument(applicationDocument)
                .build();

        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(UUID.randomUUID())
                .withDocumentTypeDescription(POSTAL_NOTIFICATION)
                .withMaterials(Collections.singletonList(Material.material()
                                .withId(materialId)
                                .withGenerationStatus(null)
                                .withUploadDateTime(ZonedDateTime.now())
                                .withName(POSTAL_NOTIFICATION)
                                .build()
                        )
                )
                .withDocumentCategory(documentCategory)
                .withName(POSTAL_NOTIFICATION)
                .build();
    }
}
