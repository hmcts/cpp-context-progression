package uk.gov.moj.cpp.progression.service;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.getCourtTime;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.payloads.CourtAddress;
import uk.gov.moj.cpp.progression.service.payloads.OrderingCourt;
import uk.gov.moj.cpp.progression.service.payloads.StatDacAppointmentLetterPayload;
import uk.gov.moj.cpp.progression.service.payloads.StatDecAppointmentLetterDefendant;
import uk.gov.moj.cpp.progression.service.payloads.StatDecAppointmentLetterDefendantAddress;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class StatDecLetterService {

    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";

    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";

    private static final String NAME = "name";

    private static final String WELSH_NAME = "welshName";

    private static final String LJA = "lja";

    private static final String EMPTY = "";

    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public UUID  generateAppointmentLetterDocument(final JsonEnvelope envelope,
                                                   final ZonedDateTime hearingStartDateTime,
                                                   final CourtApplication courtApplication,
                                                   final CourtCentre courtCentre,
                                                   final CourtApplicationParty courtApplicationParty,
                                                   final JurisdictionType jurisdictionType,
                                                   final String documentTemplateName) {

        final Optional<CourtCentre> orderingCourtOptional = ofNullable(courtCentre);

        JsonObject localJusticeArea = createObjectBuilder().build();
        JsonObject courtCentreJson = createObjectBuilder().build();


        if (orderingCourtOptional.isPresent()) {

            final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getOrganisationUnitById(courtCentre.getId(), envelope, requester);

            courtCentreJson = courtCentreJsonOptional.orElseThrow(IllegalArgumentException::new);

            final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(envelope, courtCentreJson.getString(LJA), requester);

            localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
        }

        final StatDacAppointmentLetterPayload statDacAppointmentLetterPayload = getStatDecAppointmentLetterPayload(hearingStartDateTime,courtApplication.getApplicationReference(),courtCentre,
                 courtCentreJson, localJusticeArea,courtApplicationParty,jurisdictionType, documentTemplateName);
        final JsonObject documentPayload = objectToJsonObjectConverter.convert(statDacAppointmentLetterPayload);

        final StatDecAppointmentLetterDefendantAddress statDecAppointmentLetterDefendantAddress= statDacAppointmentLetterPayload.getOrderAddressee().getAddress();
        final boolean isPostable = (nonNull(statDecAppointmentLetterDefendantAddress) && isBlank(statDecAppointmentLetterDefendantAddress.getPostCode())) ? Boolean.FALSE : Boolean.TRUE;

        return  documentGeneratorService.generateDocument(envelope, documentPayload, documentTemplateName, sender, null, courtApplication.getId(), isPostable);

    }

    @SuppressWarnings({"squid:S00107"})
    public StatDacAppointmentLetterPayload getStatDecAppointmentLetterPayload(final ZonedDateTime hearingStartDateTime,
                                                                                final String applicationReference,
                                                                                final CourtCentre courtCentre,
                                                                                final JsonObject courtCentreJson,
                                                                                final JsonObject localJusticeArea,
                                                                                final CourtApplicationParty courtApplicationParty,
                                                                                final JurisdictionType jurisdictionType,
                                                                                final String documentTemplateName) {

        final String hearingDate = hearingStartDateTime.toLocalDate().toString();
        final String hearingTime = getCourtTime(hearingStartDateTime);

        final StatDacAppointmentLetterPayload.Builder builder = StatDacAppointmentLetterPayload.builder()
                .withCaseApplicationReference(asList(ofNullable(applicationReference).orElse(EMPTY)))
                .withOrderDate(LocalDate.now())
                .withHearingDate(hearingDate)
                .withHearingTime(hearingTime);
        if(jurisdictionType.equals(MAGISTRATES)){
            builder.withOrderingCourt(OrderingCourt.builder()
                    .withLjaCode(ofNullable(localJusticeArea).map(area -> area.getString(NATIONAL_COURT_CODE, EMPTY)).orElse(EMPTY))
                    .withLjaName(ofNullable(localJusticeArea).map(area -> area.getString(NAME, EMPTY)).orElse(EMPTY))
                    .withWelshLjaName(ofNullable(localJusticeArea).map(area -> area.getString(WELSH_NAME, EMPTY)).orElse(EMPTY))
                    .withCourtCenterName(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY))
                    .withWelshCourtCenterName(courtCentreJson.getString("oucodeL3WelshName", EMPTY))
                    .build());
        }
        builder.withCourtAddress(CourtAddress.builder()
                .withAddress1(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(ADDRESS_1, EMPTY)).orElse(EMPTY))
                .withAddress2(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(ADDRESS_2, EMPTY)).orElse(EMPTY))
                .withAddress3(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(ADDRESS_3, EMPTY)).orElse(EMPTY))
                .withAddress4(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(ADDRESS_4, EMPTY)).orElse(EMPTY))
                .withAddress5(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(ADDRESS_5, EMPTY)).orElse(EMPTY))
                .withPostCode(ofNullable(courtCentreJson).map(courtCenterObject -> courtCenterObject.getString(POSTCODE, EMPTY)).orElse(EMPTY))
                .withHouse(ofNullable(courtCentre).map(CourtCentre::getName).orElse(EMPTY))
                .build());

        //Check if the applicant is a defendant
        final Optional<MasterDefendant> defendantOptional = ofNullable(courtApplicationParty.getMasterDefendant());

        defendantOptional.ifPresent(defendant -> builder.withOrderAddresse(buildDefendant(defendant)));


        return builder.build();
    }

    private StatDecAppointmentLetterDefendant buildDefendant(final MasterDefendant defendant) {
        return StatDecAppointmentLetterDefendant.builder()
                .withName(getDefendantName(defendant))
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

    private StatDecAppointmentLetterDefendantAddress getDefendantPostalAddress(final MasterDefendant masterDefendant) {
        if (nonNull(masterDefendant.getPersonDefendant())) {
            return getPostalAddress(masterDefendant.getPersonDefendant().getPersonDetails().getAddress());
        }
        if (nonNull(masterDefendant.getLegalEntityDefendant())) {
            return getPostalAddress(masterDefendant.getLegalEntityDefendant().getOrganisation().getAddress());
        }
        return null;
    }

    private StatDecAppointmentLetterDefendantAddress getPostalAddress(final Address address) {
        final StatDecAppointmentLetterDefendantAddress.Builder postalAddressBuilder = StatDecAppointmentLetterDefendantAddress.builder();
        final Optional<Address> addressOptional = ofNullable(address);
        if (addressOptional.isPresent()) {
            postalAddressBuilder.withLine1(addressOptional.map(Address::getAddress1).orElse(EMPTY));
            postalAddressBuilder.withLine2(addressOptional.map(Address::getAddress2).orElse(EMPTY));
            postalAddressBuilder.withLine3(addressOptional.map(Address :: getAddress3).orElse(EMPTY));
            postalAddressBuilder.withLine4(addressOptional.map(Address :: getAddress4).orElse(EMPTY));
            postalAddressBuilder.withLine5(addressOptional.map(Address :: getAddress5).orElse(EMPTY));
            postalAddressBuilder.withPostCode(address.getPostcode());
        }
        return postalAddressBuilder.build();
    }
}
