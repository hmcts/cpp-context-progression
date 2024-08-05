package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDocument;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.DocumentCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
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
import uk.gov.moj.cpp.progression.service.payloads.AssociatedDefenceOrganisation;
import uk.gov.moj.cpp.progression.service.payloads.DefenceOrganisationAddress;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class PostalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostalService.class.getName());

    public static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";

    public static final UUID APPLICATIONS_DOCUMENT_TYPE_ID = UUID.fromString("460fa7ce-c002-11e8-a355-529269fb1459");

    public static final String POSTAL_NOTIFICATION = "PostalNotification";

    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";

    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";

    private static final String NAME = "name";

    private static final String NAME_WELSH = "welshName";

    private static final String LJA = "lja";

    private static final String EMPTY = "";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private DefenceService defenceService;

    @SuppressWarnings({"squid:S00107"})
    public PostalNotification getPostalNotificationForCourtApplicationParty(final JsonEnvelope envelope,
                                                final String hearingDate,
                                                final String hearingTime,
                                                final String applicationReference,
                                                final String applicationType,
                                                final String applicationTypeWelsh,
                                                final String legislation,
                                                final String legislationWelsh,
                                                final CourtCentre courtCentre,
                                                final CourtApplicationParty courtApplicationParty,
                                                final JurisdictionType jurisdictionType, String applicationParticulars, final CourtApplication courtApplication, final String thirdParty) {

        final Optional<CourtCentre> orderingCourtOptional = ofNullable(courtCentre);

        JsonObject localJusticeArea = Json.createObjectBuilder().build();

        if (orderingCourtOptional.isPresent()) {

            final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getOrganisationUnitById(courtCentre.getId(), envelope, requester);

            final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(IllegalArgumentException::new);

            final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(envelope, courtCentreJson.getString(LJA), requester);

            localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        }

        String courtCentreNameWelsh = null;
        if(nonNull(courtCentre)){
            final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getCourtCentreWithCourtRoomsById(courtCentre.getId(), envelope, requester);
            if (nonNull(courtCentreJsonOptional)) {
                final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(() -> new IllegalArgumentException(String.format("Court centre '%s' not found", courtCentre.getId())));

                courtCentreNameWelsh = courtCentreJson.getString("oucodeL3WelshName", "");
            }
        }
        final MasterDefendant masterDefendant = courtApplication.getApplicant().getMasterDefendant();


        final String applicantPersonal = nonNull(courtApplication.getApplicant().getPersonDetails()) ? courtApplication.getApplicant().getPersonDetails().getFirstName() + " " + courtApplication.getApplicant().getPersonDetails().getLastName() : "";
        final String applicantOther = nonNull(courtApplication.getApplicant().getProsecutingAuthority()) ? courtApplication.getApplicant().getProsecutingAuthority().getProsecutionAuthorityCode() : applicantPersonal;
        final String applicant = nonNull(masterDefendant) && nonNull(masterDefendant.getPersonDefendant()) && nonNull(masterDefendant.getPersonDefendant().getPersonDetails()) ? masterDefendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + masterDefendant.getPersonDefendant().getPersonDetails().getLastName() : applicantOther;

        final PostalAddressee postalAddressee = getPostalAddressee(envelope, courtApplicationParty);

        return buildPostalNotification(hearingDate,
                hearingTime,
                applicationReference,
                applicationType,
                applicationTypeWelsh,
                legislation,
                legislationWelsh,
                courtCentre,
                courtCentreNameWelsh,
                localJusticeArea,
                courtApplicationParty,
                jurisdictionType,
                applicationParticulars,
                courtApplication, applicant,
                thirdParty, postalAddressee);
    }

    public void sendPostalNotification(final JsonEnvelope envelope, final UUID applicationId, final PostalNotification postalNotification, final UUID linkedCaseId) {

        final JsonObject postalNotificationPayload = objectToJsonObjectConverter.convert(postalNotification);

        LOGGER.info("Sending Postal Notification payload - {}", postalNotificationPayload);

        final UUID materialId = documentGeneratorService.generateDocument(envelope, postalNotificationPayload, POSTAL_NOTIFICATION, sender, null, applicationId, isPostable(postalNotification));

        final CourtDocument courtDocument = courtDocument(applicationId, materialId, envelope, linkedCaseId);

        final JsonObject courtDocumentPayload = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();

        LOGGER.info("creating court document payload - {}", courtDocumentPayload);

        sender.send(enveloper.withMetadataFrom(envelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(courtDocumentPayload));

    }

    @SuppressWarnings({"squid:S00107"})
    public PostalNotification buildPostalNotification(final String hearingDate,
                                                      final String hearingTime,
                                                      final String applicationReference,
                                                      final String applicationType,
                                                      final String applicationTypeWelsh,
                                                      final String legislation,
                                                      final String legislationWelsh,
                                                      final CourtCentre courtCentre,
                                                      final String courtCentreNameWelsh,
                                                      final JsonObject localJusticeArea,
                                                      final CourtApplicationParty courtApplicationParty,
                                                      final JurisdictionType jurisdictionType, String applicationParticulars,
                                                      final CourtApplication courtApplication, final String applicant,
                                                      final String thirdParty, final PostalAddressee postalAddressee) {

        final PostalNotification.Builder builder = PostalNotification.builder()
                .withReference(ofNullable(applicationReference).orElse(EMPTY))
                .withIssueDate(LocalDate.now())
                .withApplicationType(ofNullable(applicationType).orElse(EMPTY))
                .withLegislationText(ofNullable(legislation).orElse(EMPTY))
                .withCourtCentreName(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY))
                .withCourtCentreNameWelsh(courtCentreNameWelsh)
                .withApplicantName(applicant)
                .withThirdParty(thirdParty)
                .withApplicationParticulars(applicationParticulars);
        if(jurisdictionType.equals(JurisdictionType.MAGISTRATES)){
            builder.withLjaCode(ofNullable(localJusticeArea).map(area -> area.getString(NATIONAL_COURT_CODE, EMPTY)).orElse(EMPTY))
                    .withLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME, EMPTY)).orElse(EMPTY))
                    .withLjaNameWelsh(ofNullable(localJusticeArea).map(area -> area.getString(NAME_WELSH, EMPTY)).orElse(EMPTY));
        }

        //Check if the applicant is a defendant
        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());
        defendantOptional.ifPresent(defendant -> builder.withDefendant(buildDefendant(defendant)));
        if (nonNull(courtApplication.getRespondents())) {
            courtApplication.getRespondents().forEach(respondent -> {
                final Optional<MasterDefendant> masterDefendantOptional = ofNullable(respondent.getMasterDefendant());
                masterDefendantOptional.ifPresent(defendant -> builder.withDefendant(buildDefendant(defendant)));
            });
        }


        final Optional<MasterDefendant> defendantOptional1 = ofNullable(courtApplication.getApplicant().getMasterDefendant());

        defendantOptional1.ifPresent(defendant -> builder.withDefendant(buildDefendant(defendant)));

        builder.withAddressee(postalAddressee);
        builder.withApplicationTypeWelsh(applicationTypeWelsh);
        builder.withLegislationTextWelsh(legislationWelsh);

        @SuppressWarnings({"squid:S2259"}) final PostalAddress postalAddress = getPostalAddress(ofNullable(courtCentre).map(CourtCentre::getAddress).orElse(null));

        final PostalHearingCourtDetails.Builder hearingCourtDetailsBuilder = PostalHearingCourtDetails.builder()
                .withCourtName(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY))
                .withCourtNameWelsh(courtCentreNameWelsh)
                .withHearingDate(hearingDate)
                .withHearingTime(hearingTime);

        ofNullable(postalAddress).ifPresent(hearingCourtDetailsBuilder::withCourtAddress);

        builder.withHearingCourtDetails(hearingCourtDetailsBuilder.build());

        return builder.build();
    }

    private PostalAddressee getPostalAddressee(final JsonEnvelope envelope, final CourtApplicationParty courtApplicationParty) {

        final Optional<AssociatedDefenceOrganisation> associatedDefenceOrganisation = getAssociatedDefenceOrganisation(envelope, courtApplicationParty.getMasterDefendant());

        if (associatedDefenceOrganisation.isPresent() && nonNull(associatedDefenceOrganisation.get().getAddress())){
            final DefenceOrganisationAddress defenceOrganisationAddress = associatedDefenceOrganisation.get().getAddress();
            return PostalAddressee.builder()
                    .withName(associatedDefenceOrganisation.get().getOrganisationName())
                    .withAddress(PostalAddress.builder()
                            .withLine1(defenceOrganisationAddress.getAddress1())
                            .withLine2(defenceOrganisationAddress.getAddress2())
                            .withLine3(defenceOrganisationAddress.getAddress3())
                            .withLine4(defenceOrganisationAddress.getAddress4())
                            .withPostCode(defenceOrganisationAddress.getAddressPostcode())
                            .build())
                    .build();
        } else {
            return PostalAddressee.builder()
                    .withName(getName(courtApplicationParty))
                    .withAddress(getAddress(courtApplicationParty))
                    .build();
        }
    }

    private PostalDefendant buildDefendant(final MasterDefendant defendant) {
        return PostalDefendant.builder()
                .withName(getDefendantName(defendant))
                .withDateOfBirth(nonNull(defendant.getPersonDefendant()) ? defendant.getPersonDefendant().getPersonDetails().getDateOfBirth() : null)
                .withAddress(getDefendantPostalAddress(defendant))
                .build();
    }

    private String getDefendantName(final MasterDefendant masterDefendant) {
        if (nonNull(masterDefendant.getPersonDefendant())) {
            return masterDefendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + masterDefendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        if (nonNull(masterDefendant.getLegalEntityDefendant())) {
            return masterDefendant.getLegalEntityDefendant().getOrganisation().getName();
        }
        return EMPTY;
    }

    private PostalAddress getDefendantPostalAddress(final MasterDefendant masterDefendant) {
        if (nonNull(masterDefendant.getPersonDefendant())) {
            return getPostalAddress(masterDefendant.getPersonDefendant().getPersonDetails().getAddress());
        }
        if (nonNull(masterDefendant.getLegalEntityDefendant())) {
            return getPostalAddress(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress());
        }
        return null;
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

        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        if (personOptional.isPresent()) {
            return personOptional.map(person -> {
                final String firstName = ofNullable(person.getFirstName()).orElse(EMPTY);
                final String lastName = ofNullable(person.getLastName()).orElse(EMPTY);
                return firstName + " " + lastName;
            }).orElse(EMPTY);
        }

        if (organisationOptional.isPresent()) {
            return organisationOptional.map(Organisation::getName).orElse(EMPTY);
        }

        if (prosecutingAuthorityOptional.isPresent()) {
            return prosecutingAuthorityOptional.get().getProsecutionAuthorityCode();
        }

        if (defendantOptional.isPresent()) {
            return getDefendantName(defendantOptional.get());
        }

        return EMPTY;
    }

    private PostalAddress getAddress(final CourtApplicationParty courtApplicationParty) {

        final Optional<Person> personOptional = ofNullable(courtApplicationParty.getPersonDetails());

        final Optional<Organisation> organisationOptional = ofNullable(courtApplicationParty.getOrganisation());

        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

        final Optional<ProsecutingAuthority> prosecutingAuthorityOptional = ofNullable(courtApplicationParty.getProsecutingAuthority());

        if (personOptional.isPresent()) {
            return getPostalAddress(personOptional.get().getAddress());
        }

        if (organisationOptional.isPresent()) {
            return getPostalAddress(organisationOptional.get().getAddress());
        }

        if (defendantOptional.isPresent()) {
            return getDefendantPostalAddress(defendantOptional.get());
        }

        return prosecutingAuthorityOptional.map(prosecutingAuthority -> getPostalAddress(prosecutingAuthority.getAddress())).orElse(null);
    }

    public CourtDocument courtDocument(final UUID applicationId, final UUID materialId, final JsonEnvelope envelope, final UUID linkedCaseId) {
        final ApplicationDocument applicationDocument = ApplicationDocument.applicationDocument()
                .withApplicationId(applicationId)
                .withProsecutionCaseId(linkedCaseId)
                .build();

        final DocumentCategory documentCategory = DocumentCategory.documentCategory()
                .withApplicationDocument(applicationDocument)
                .build();

        final JsonObject documentTypeData = referenceDataService.getDocumentTypeAccessData(APPLICATIONS_DOCUMENT_TYPE_ID, envelope, requester)
                .orElseThrow(() -> new RuntimeException("Unable to retrieve document type details for '" + APPLICATIONS_DOCUMENT_TYPE_ID + "'"));
        return CourtDocument.courtDocument()
                .withCourtDocumentId(UUID.randomUUID())
                .withDocumentTypeId(APPLICATIONS_DOCUMENT_TYPE_ID)
                .withDocumentTypeDescription(documentTypeData.getString("section"))
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
                .withMimeType("application/pdf")
                .build();
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

    private boolean isPostable(final PostalNotification postalNotification) {
        final PostalAddress postalAddress = postalNotification.getDefendant().getAddress();
        return (nonNull(postalAddress) && isBlank(postalAddress.getPostCode())) ? Boolean.FALSE : Boolean.TRUE ;
    }

}
