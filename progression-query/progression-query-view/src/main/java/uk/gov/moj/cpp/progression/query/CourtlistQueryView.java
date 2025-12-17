package uk.gov.moj.cpp.progression.query;

import static java.time.LocalDate.now;
import static java.time.Period.between;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.containsAny;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.constant.DateTimeFormats.STANDARD;
import static uk.gov.moj.cpp.progression.domain.helper.JsonHelper.addProperty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefenceCounsel;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCounsel;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.view.service.ListingService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1170", "squid:S00116", "squid:S3776"})
@ServiceComponent(Component.QUERY_VIEW)
public class CourtlistQueryView {

    private static final String NAME = "name";
    private static final String DATE_OF_BIRTH = "dateOfBirth";
    private static final String APPLICANT = "applicant";
    private static final String RESPONDENTS = "respondents";
    private static final DateTimeFormatter DATE_FORMATTER = ofPattern(STANDARD.getValue());
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtlistQueryView.class);
    private final String ID = "id";
    private final String CASE_ID = "caseId";
    private final String DEFENDANTS = "defendants";
    private final String COURT_APPLICATION = "courtApplication";
    private final String OFFENCES = "offences";
    private final String HEARING_DATES = "hearingDates";
    private final String COURT_ROOMS = "courtRooms";
    private final String TIME_SLOTS = "timeslots";
    private final String HEARINGS = "hearings";
    private final String LISTING_NUMBER = "listingNumber";
    private final String COURT_APPLICATION_ID = "courtApplicationId";
    private final String PROSECUTOR_TYPE = "prosecutorType";
    private final String DEFENCE_COUNSELS = "defenceCounsels";
    private final String PROSECUTION_COUNSELS = "prosecutionCounsels";
    @Inject
    private ListingService listingService;
    @Inject
    private HearingQueryView hearingQueryView;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.search.court.list")
    public JsonEnvelope searchCourtlist(final JsonEnvelope query) {
        LOGGER.info("Calling listing.search.court.list.payload with query parameters: {}", query);
        final Optional<JsonObject> optionalJsonObject = listingService.searchCourtlist(query);
        if (optionalJsonObject.isPresent()) {
            JsonObject documentPayload = optionalJsonObject.get();
            LOGGER.info("Response from listing.search.court.list.payload: {}", documentPayload);
            final List<UUID> hearingIds = getHearingIds(documentPayload);
            if (isNotEmpty(hearingIds)) {
                final Map<UUID, Hearing> hearingsMap = getHearingsMap(hearingIds);
                if (!hearingsMap.isEmpty()) {
                    documentPayload = addLjaInformation(documentPayload, getCourtCentreFromHearingMap(hearingsMap));
                    documentPayload = addHearingInformation(documentPayload, hearingsMap);
                }
            }
            return envelopeFrom(query.metadata(), documentPayload);
        }
        return envelopeFrom(query.metadata(), Json.createObjectBuilder().build());
    }

    @Handles("progression.search.prison.court.list")
    public JsonEnvelope searchPrisonCourtlist(final JsonEnvelope query) {
        return searchCourtlist(query);
    }

    private CourtCentre getCourtCentreFromHearingMap(final Map<UUID, Hearing> hearingsMap) {

        return hearingsMap.values().stream()
                .filter(hearing -> nonNull(hearing))
                .filter(hearing -> nonNull(hearing.getCourtCentre()))
                .map(Hearing::getCourtCentre)
                .findFirst().orElse(null);

    }

    private Map<UUID, Hearing> getHearingsMap(final List<UUID> hearingIds) {

        return hearingQueryView.getHearings(hearingIds).stream()
                .collect(toMap(Hearing::getId, hearing -> hearing));

    }

    private List<UUID> getHearingIds(final JsonObject listingResponse) {
        final JsonArray hearings = listingResponse.getJsonArray(HEARING_DATES);
        if (isNotEmpty(hearings)) {
            return hearings.stream()
                    .flatMap(jsonValue -> ((JsonObject) jsonValue).getJsonArray(COURT_ROOMS).stream())
                    .flatMap(jsonValue -> ((JsonObject) jsonValue).getJsonArray(TIME_SLOTS).stream())
                    .flatMap(jsonValue -> ((JsonObject) jsonValue).getJsonArray(HEARINGS).stream())
                    .map(jsonValue -> fromString(((JsonObject) jsonValue).getString(ID)))
                    .collect(toList());
        }
        return emptyList();
    }

    private List<UUID> getApplicationOffenceListingNumbers(final JsonObject hearingJson) {
        if (hearingJson.containsKey("applicationOffences")) {
            return hearingJson.getJsonArray("applicationOffences").stream()
                    .map(jsonValue -> ((JsonObject) jsonValue))
                    .map(jsonObject -> fromString(jsonObject.getString(ID)))
                    .collect(toList());
        }
        return emptyList();
    }

    private JsonObject addHearingInformation(JsonObject documentPayload, final Map<UUID, Hearing> hearingsMap) {
        final JsonArrayBuilder hearingDatesArray = createArrayBuilder();

        documentPayload.getJsonArray(HEARING_DATES).stream()
                .map(hearingDate -> (JsonObject) hearingDate)
                .forEach(hearingFromListing -> hearingDatesArray.add(enrichHearingDate(hearingFromListing, hearingsMap)));

        documentPayload = addProperty(documentPayload, HEARING_DATES, hearingDatesArray.build());
        return documentPayload;
    }

    private JsonObject enrichHearingDate(JsonObject hearingFromListing, final Map<UUID, Hearing> hearingsMap) {
        final JsonArrayBuilder courtRoomsArray = createArrayBuilder();

        hearingFromListing.getJsonArray(COURT_ROOMS).stream()
                .map(courtRoom -> (JsonObject) courtRoom)
                .forEach(courtRoomFromListing -> courtRoomsArray.add(enrichCourtRoom(courtRoomFromListing, hearingsMap)));

        hearingFromListing = addProperty(hearingFromListing, COURT_ROOMS, courtRoomsArray.build());
        return hearingFromListing;
    }

    private JsonObject enrichCourtRoom(JsonObject courtRoomFromListing, final Map<UUID, Hearing> hearingsMap) {
        final JsonArrayBuilder timeSlotsArray = createArrayBuilder();

        courtRoomFromListing.getJsonArray(TIME_SLOTS).stream()
                .map(timeSlot -> (JsonObject) timeSlot)
                .forEach(timeSlotFromListing -> timeSlotsArray.add(enrichTimeslot(timeSlotFromListing, hearingsMap)));

        courtRoomFromListing = addProperty(courtRoomFromListing, TIME_SLOTS, timeSlotsArray.build());
        return courtRoomFromListing;
    }


    private JsonObject enrichTimeslot(JsonObject timeSlotFromListing, final Map<UUID, Hearing> hearingsMap) {
        final JsonArrayBuilder hearingsArray = createArrayBuilder();

        timeSlotFromListing.getJsonArray(HEARINGS).stream()
                .map(hearing -> (JsonObject) hearing)
                .forEach(hearingFromListing -> {
                    final UUID hearingId = fromString(hearingFromListing.getString(ID));
                    final Hearing hearing = hearingsMap.get(hearingId);
                    if (nonNull(hearing)) {
                        if (hearingFromListing.containsKey(CASE_ID)) {
                            final UUID caseId = fromString(hearingFromListing.getString(CASE_ID));
                            hearingsArray.add(enrichHearingFromCase(hearingFromListing, hearing, caseId));
                        } else if (hearingFromListing.containsKey(COURT_APPLICATION_ID)) {
                            final UUID courtApplicationId = fromString(hearingFromListing.getString(COURT_APPLICATION_ID));
                            hearingsArray.add(enrichHearingFromCourtApplication(hearingFromListing, hearing, courtApplicationId));
                        }
                    }
                });

        timeSlotFromListing = addProperty(timeSlotFromListing, HEARINGS, hearingsArray.build());
        return timeSlotFromListing;
    }

    private JsonObject enrichHearingFromCase(JsonObject hearingFromListing, final Hearing hearing, final UUID caseId) {
        //DD-15717: deliberately loading case details from database as the copy of case on hearing object within progression is out of date
        final List<ProsecutionCase> prosecutionCasesFromViewStore = getProsecutionCaseFromDb(caseId);
        final List<ProsecutionCase> prosecutionCases = ofNullable(hearing.getProsecutionCases())
                .map(prosecutionCasesFromHearing -> getProsecutionCaseWithListingNumberInHearings(prosecutionCasesFromViewStore, prosecutionCasesFromHearing))
                .orElseGet(() -> prosecutionCasesFromViewStore);

        if (isNotEmpty(prosecutionCases)) {
            final Optional<ProsecutionCase> prosecutionCase = prosecutionCases.stream()
                    .filter(pc -> pc.getId().equals(caseId))
                    .findFirst();

            if (prosecutionCase.isPresent()) {
                final ProsecutionCase pc = prosecutionCase.get();
                if (nonNull(pc.getProsecutor())) {
                    hearingFromListing = addProperty(hearingFromListing, PROSECUTOR_TYPE, pc.getProsecutor().getProsecutorCode());
                } else {
                    hearingFromListing = addProperty(hearingFromListing, PROSECUTOR_TYPE, pc.getProsecutionCaseIdentifier().getProsecutionAuthorityCode());
                }
            }
        }

        final JsonArrayBuilder defendantsArray = createArrayBuilder();

        hearingFromListing.getJsonArray(DEFENDANTS)
                .stream()
                .map(defendant -> (JsonObject) defendant)
                .forEach(defendantFromListing -> {
                    final UUID defendantId = fromString((defendantFromListing).getString(ID));
                    prosecutionCases.stream()
                            .filter(prosecutionCase -> prosecutionCase.getId().equals(caseId))
                            .forEach(prosecutionCase -> prosecutionCase.getDefendants()
                                    .forEach(defendant -> {
                                        if (defendantId.equals(defendant.getId())) {
                                            defendantsArray.add(enrichDefendant(defendantFromListing, defendant, hearing, prosecutionCase));
                                        }
                                    }));
                });

        hearingFromListing = addProperty(hearingFromListing, DEFENDANTS, defendantsArray.build());
        return hearingFromListing;
    }

    private List<ProsecutionCase> getProsecutionCaseWithListingNumberInHearings(final List<ProsecutionCase> prosecutionCasesFromViewStore, final List<ProsecutionCase> prosecutionCasesFromHearing) {

        final Map<UUID, Integer> listingMap = prosecutionCasesFromHearing.stream().flatMap(pc -> pc.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .filter(offence -> nonNull(offence.getListingNumber()))
                .collect(toMap(uk.gov.justice.core.courts.Offence::getId, uk.gov.justice.core.courts.Offence::getListingNumber, Math::max));

        return prosecutionCasesFromViewStore.stream()
                .map(ps -> ProsecutionCase.prosecutionCase().withValuesFrom(ps)
                        .withDefendants(ps.getDefendants().stream()
                                .map(def -> Defendant.defendant().withValuesFrom(def)
                                        .withOffences(def.getOffences().stream()
                                                .map(of -> Offence.offence().withValuesFrom(of)
                                                        .withListingNumber(listingMap.get(of.getId()))
                                                        .build())
                                                .collect(toList()))
                                        .build())
                                .collect(toList()))
                        .build())
                .collect(toList());
    }

    private List<ProsecutionCase> getProsecutionCaseFromDb(final UUID caseId) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(caseId);
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCaseFromDb = jsonObjectToObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        return singletonList(prosecutionCaseFromDb);
    }

    private JsonObject enrichHearingFromCourtApplication(JsonObject hearingFromListing, final Hearing hearing, final UUID courtApplicationId) {

        final List<UUID> offencesForApplications = getApplicationOffenceListingNumbers(hearingFromListing);

        final JsonArrayBuilder defendantsArray = createArrayBuilder();

        if (isNotEmpty(hearing.getCourtApplications())) {
            final JsonObject finalHearingFromListing = hearingFromListing;
            hearing.getCourtApplications()
                    .forEach(courtApplication -> {
                        if (courtApplication.getId().equals(courtApplicationId)) {
                            defendantsArray.add(buildDefendantFromCourtApplication(finalHearingFromListing, courtApplication, hearing, offencesForApplications));
                        }
                    });

            final Optional<CourtApplication> hearingCourtApplication = hearing.getCourtApplications().stream()
                    .filter(courtApplication -> courtApplication.getId().equals(courtApplicationId))
                    .findFirst();

            final JsonObjectBuilder courtApplicationBuilder = createObjectBuilder();
            hearingCourtApplication.ifPresent(courtApplication -> {
                courtApplicationBuilder.add(APPLICANT, buildCourtApplicationParty(courtApplication.getApplicant()));
                ofNullable(courtApplication.getRespondents()).ifPresent(respondents -> {
                    final JsonArrayBuilder respondentsBuilder = createArrayBuilder();
                    respondents.forEach(respondent -> respondentsBuilder.add(buildCourtApplicationParty(respondent)));
                    courtApplicationBuilder.add(RESPONDENTS, respondentsBuilder.build());
                });
            });
            hearingFromListing = addProperty(hearingFromListing, COURT_APPLICATION, courtApplicationBuilder.build());

        }

        hearingFromListing = addProperty(hearingFromListing, DEFENDANTS, defendantsArray.build());
        return hearingFromListing;
    }

    private JsonObject buildCourtApplicationParty(final CourtApplicationParty applicant) {
        final JsonObjectBuilder partyBuilder = createObjectBuilder();
        if (applicant.getMasterDefendant() != null) {
            addMasterDefendantToPartyBuilder(applicant.getMasterDefendant(), partyBuilder);
        } else if (applicant.getProsecutingAuthority() != null) {
            addProsecutionAuthorityToPartyBuilder(applicant.getProsecutingAuthority(), partyBuilder);
        } else if (applicant.getOrganisation() != null && applicant.getOrganisation().getName() != null) {
            partyBuilder.add(NAME, applicant.getOrganisation().getName());
        } else if (applicant.getPersonDetails() != null) {
            final Person person = applicant.getPersonDetails();
            partyBuilder.add(NAME, String.format("%s %s", person.getFirstName(), person.getLastName()));
            ofNullable(person.getDateOfBirth()).ifPresent(dateOfBirth -> partyBuilder.add(DATE_OF_BIRTH, dateOfBirth.format(DOB_FORMATTER)));
        } else if (applicant.getRepresentationOrganisation() != null && applicant.getRepresentationOrganisation().getName() != null) {
            partyBuilder.add(NAME, applicant.getRepresentationOrganisation().getName());
        }
        return partyBuilder.build();
    }

    private void addProsecutionAuthorityToPartyBuilder(final ProsecutingAuthority prosecutingAuthority, final JsonObjectBuilder partyBuilder) {
        if (prosecutingAuthority.getName() != null) {
            partyBuilder.add(NAME, prosecutingAuthority.getName());
        } else if (prosecutingAuthority.getProsecutionAuthorityCode() != null) {
            partyBuilder.add(NAME, prosecutingAuthority.getProsecutionAuthorityCode());
        }
    }

    private void addMasterDefendantToPartyBuilder(final MasterDefendant masterDefendant, final JsonObjectBuilder partyBuilder) {
        if (masterDefendant.getPersonDefendant() != null
                && masterDefendant.getPersonDefendant().getPersonDetails() != null) {
            final Person person = masterDefendant.getPersonDefendant().getPersonDetails();
            partyBuilder.add(NAME, String.format("%s %s", person.getFirstName(), person.getLastName()));
            ofNullable(person.getDateOfBirth()).ifPresent(dateOfBirth -> partyBuilder.add(DATE_OF_BIRTH, dateOfBirth.format(DOB_FORMATTER)));
        } else if (masterDefendant.getLegalEntityDefendant() != null
                && masterDefendant.getLegalEntityDefendant().getOrganisation() != null
                && masterDefendant.getLegalEntityDefendant().getOrganisation().getName() != null) {
            partyBuilder.add(NAME, masterDefendant.getLegalEntityDefendant().getOrganisation().getName());
        }
    }

    private JsonObject buildDefendantFromCourtApplication(JsonObject hearingFromListing, final CourtApplication courtApplication, final Hearing hearing, final List<UUID> offencesForApplications) {

        final JsonObjectBuilder defendantBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder offencesArray = createArrayBuilder();
        final List<UUID> caseIdList = new ArrayList<>();

        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {

            caseIdList.addAll(courtApplication.getCourtApplicationCases().stream()
                    .filter(courtApplicationCase -> isNotEmpty(courtApplicationCase.getOffences()))
                    .map(CourtApplicationCase::getProsecutionCaseId)
                    .collect(toSet()));

            courtApplication.getCourtApplicationCases().stream()
                    .filter(courtApplicationCase -> isNotEmpty(courtApplicationCase.getOffences()))
                    .flatMap(courtApplicationCase -> courtApplicationCase.getOffences().stream())
                    .filter(offence -> offencesForApplications.contains(offence.getId()))
                    .forEach(offence -> {
                        final JsonObjectBuilder offenceBuilder = Json.createObjectBuilder();
                        buildOffence(offenceBuilder, offence, null);
                        addApplicationInformation(offenceBuilder, courtApplication);
                        offencesArray.add(offenceBuilder.build());
                    });

        } else if (nonNull(courtApplication.getCourtOrder()) && isNotEmpty(courtApplication.getCourtOrder().getCourtOrderOffences())) {
            caseIdList.addAll(courtApplication.getCourtOrder().getCourtOrderOffences().stream()
                    .map(CourtOrderOffence::getProsecutionCaseId)
                    .collect(toSet()));

            courtApplication.getCourtOrder().getCourtOrderOffences().stream()
                    .map(CourtOrderOffence::getOffence)
                    .filter(offence -> offencesForApplications.contains(offence.getId()))
                    .forEach(offence -> {
                        final JsonObjectBuilder offenceBuilder = Json.createObjectBuilder();
                        buildOffence(offenceBuilder, offence, null);
                        addApplicationInformation(offenceBuilder, courtApplication);
                        offencesArray.add(offenceBuilder.build());
                    });
        }

        final MasterDefendant masterDefendant = courtApplication.getSubject().getMasterDefendant();
        if (nonNull(masterDefendant) && nonNull(masterDefendant.getPersonDefendant())) {
            final Person person = masterDefendant.getPersonDefendant().getPersonDetails();

            final JsonObjectBuilder defendantFromListingBuilder = Json.createObjectBuilder();
            if (isNotEmpty(hearingFromListing.getJsonArray(DEFENDANTS))){
                hearingFromListing.getJsonArray(DEFENDANTS)
                        .stream()
                        .map(defendant -> (JsonObject) defendant)
                        .forEach(defFromListing -> {
                            final UUID defendantId = fromString((defFromListing).getString(ID));
                            if(defendantId.equals(masterDefendant.getMasterDefendantId()) || defendantId.equals(courtApplication.getSubject().getId())){
                                defFromListing.forEach((name, value) -> defendantFromListingBuilder.add(name, value));
                            }
                        });
            }
            defendantBuilder.add(ID, masterDefendant.getMasterDefendantId().toString());
            ofNullable(person.getFirstName()).ifPresent(firstName -> defendantBuilder.add("firstName", firstName));
            defendantBuilder.add("surname", person.getLastName());
            defendantBuilder.add("gender", person.getGender().toString());

            //Replace defendant name found from Listing
            final JsonObject defeFromListingJsonObject = defendantFromListingBuilder.build();
            if(!defeFromListingJsonObject.isEmpty() && nonNull(defeFromListingJsonObject.getString(ID))){
                final UUID defendantId = fromString(defeFromListingJsonObject.getString(ID));
                if(defendantId.equals(masterDefendant.getMasterDefendantId()) || defendantId.equals(courtApplication.getSubject().getId())){
                    defeFromListingJsonObject.forEach((name, value) -> defendantBuilder.add(name, value));
                }
            }

            final Integer defendantAge = getAge(person.getDateOfBirth());
            if (nonNull(defendantAge)) {
                defendantBuilder.add("age", defendantAge);
            }
            ofNullable(person.getAddress()).ifPresent(address -> defendantBuilder.add("address", objectToJsonObjectConverter.convert(address)));
            ofNullable(person.getDateOfBirth()).ifPresent(dateOfBirth -> defendantBuilder.add(DATE_OF_BIRTH, dateOfBirth.format(DOB_FORMATTER)));
            ofNullable(person.getNationalityDescription()).ifPresent(nationalityDescription -> defendantBuilder.add("nationality", nationalityDescription));
            if (isNotEmpty(hearing.getDefenceCounsels())) {
                defendantBuilder.add(DEFENCE_COUNSELS, buildDefenceCounsels(hearing.getDefenceCounsels(), masterDefendant.getMasterDefendantId()));
            }
        }
        ofNullable(courtApplication.getDefendantASN()).ifPresent(asn -> defendantBuilder.add("asn", asn));
        //TODO not sure about defenceOrganization
        defendantBuilder.add("defenceOrganization", "-");
        if (isNotEmpty(hearing.getProsecutionCounsels())) {
            defendantBuilder.add(PROSECUTION_COUNSELS, buildProsecutionCounsels(hearing.getProsecutionCounsels(), caseIdList));
        }

        defendantBuilder.add(OFFENCES, offencesArray.build());

        return defendantBuilder.build();
    }


    private JsonObject enrichDefendant(final JsonObject defendantFromListing, final Defendant defendant, final Hearing hearing, final ProsecutionCase prosecutionCase) {
        final JsonObjectBuilder defendantJsonBuilder = createObjectBuilder();
        defendantFromListing.forEach((name, value) -> defendantJsonBuilder.add(name, value));

        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        if (nonNull(personDefendant)) {
            defendantJsonBuilder.add("gender", personDefendant.getPersonDetails().getGender().toString());
            ofNullable(personDefendant.getArrestSummonsNumber()).ifPresent(arrestSummonsNumber -> defendantJsonBuilder.add("asn", arrestSummonsNumber));
        } else {
            if (nonNull(defendant.getLegalEntityDefendant())) {
                ofNullable(defendant.getLegalEntityDefendant().getOrganisation().getName()).ifPresent(name -> defendantJsonBuilder.add("name", name));
                ofNullable(defendant.getLegalEntityDefendant().getOrganisation().getAddress()).ifPresent(address -> defendantJsonBuilder.add("address", objectToJsonObjectConverter.convert(address)));
            }
        }

        final Optional<String> defenceOrganisation = findDefenceOrg(defendant);

        defenceOrganisation.ifPresent(org -> defendantJsonBuilder.add("defenceOrganization", org));

        final List<Offence> offencesFromHearing = getOffencesFromHearing(defendant, hearing, prosecutionCase);

        final JsonArrayBuilder offencesArray = createArrayBuilder();
        JsonObject newDefendantFromListing = defendantJsonBuilder.build();
        ofNullable(newDefendantFromListing.get(OFFENCES)).map(jsonValue -> (JsonArray) jsonValue).orElseGet(() -> createArrayBuilder().build()).stream()
                .map(offenceFromListing -> ((JsonObject) offenceFromListing))
                .forEach(offenceFromListing -> {
                    final UUID offenceId = fromString(offenceFromListing.getString(ID));
                    defendant.getOffences()
                            .forEach(offence -> {
                                if (offence.getId().equals(offenceId)) {
                                    final JsonObjectBuilder offenceBuilder = Json.createObjectBuilder();

                                    if (nonNull(offencesFromHearing)) {
                                        offencesFromHearing.forEach(offence1 -> {
                                            if (offence1.getId().equals(offenceId)) {
                                                buildOffence(offenceBuilder, offence, offence1);
                                            }
                                        });
                                    } else {
                                        buildOffence(offenceBuilder, offence, null);
                                    }
                                    addOffenceInformation(offenceBuilder, offence);
                                    offencesArray.add(offenceBuilder.build());
                                }
                            });
                });
        if (isNotEmpty(hearing.getProsecutionCounsels())) {
            newDefendantFromListing = addProperty(newDefendantFromListing, PROSECUTION_COUNSELS, buildProsecutionCounsels(hearing.getProsecutionCounsels(), singletonList(prosecutionCase.getId())));
        }
        if (isNotEmpty(hearing.getDefenceCounsels())) {
            newDefendantFromListing = addProperty(newDefendantFromListing, DEFENCE_COUNSELS, buildDefenceCounsels(hearing.getDefenceCounsels(), defendant.getId()));
        }
        newDefendantFromListing = addProperty(newDefendantFromListing, OFFENCES, offencesArray.build());
        return newDefendantFromListing;
    }

    private List<Offence> getOffencesFromHearing(final Defendant defendant, final Hearing hearing, final ProsecutionCase prosecutionCase) {
        final AtomicReference<List<Offence>> offencesFromHearing = new AtomicReference<>();
        if (nonNull(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().forEach(pc -> {
                if (pc.getId().equals(prosecutionCase.getId())) {
                    pc.getDefendants().forEach(defendant1 -> {
                        if (defendant1.getId().equals(defendant.getId())) {
                            offencesFromHearing.set(defendant1.getOffences());
                        }
                    });
                }
            });
        }

        return offencesFromHearing.get();
    }

    private Optional<String> findDefenceOrg(final Defendant defendant) {

        return nonNull(defendant.getAssociatedDefenceOrganisation()) ?
                of(defendant.getAssociatedDefenceOrganisation().getDefenceOrganisation().getOrganisation().getName())
                : getDefenceOrganisation(defendant);
    }

    private Optional<String> getDefenceOrganisation(final Defendant defendant) {

        return nonNull(defendant.getDefenceOrganisation()) ? of(defendant.getDefenceOrganisation().getName()) : Optional.empty();
    }

    private void addOffenceInformation(final JsonObjectBuilder offenceBuilder, final Offence offence) {
        offenceBuilder.add("offenceCode", offence.getOffenceCode());
        offenceBuilder.add("offenceTitle", offence.getOffenceTitle());
        offenceBuilder.add("offenceWording", offence.getWording());
        ofNullable(offence.getListingNumber()).ifPresent(listingNumber -> offenceBuilder.add(LISTING_NUMBER, listingNumber));
        ofNullable(offence.getOffenceTitleWelsh()).ifPresent(welshOffenceTitle -> offenceBuilder.add("welshOffenceTitle", welshOffenceTitle));
        ofNullable(offence.getOffenceLegislation()).ifPresent(offenceLegislation -> offenceBuilder.add("offenceLegislation", offenceLegislation));
        ofNullable(offence.getMaxPenalty()).ifPresent(maxPenalty -> offenceBuilder.add("maxPenalty", maxPenalty));
    }

    private void addApplicationInformation(final JsonObjectBuilder offenceBuilder, final CourtApplication courtApplication) {
        final CourtApplicationType type = courtApplication.getType();

        offenceBuilder.add("offenceTitle", type.getType());

        ofNullable(type.getCode()).ifPresent(offenceCode -> offenceBuilder.add("offenceCode", offenceCode));
        ofNullable(type.getTypeWelsh()).ifPresent(welshOffenceTitle -> offenceBuilder.add("welshOffenceTitle", welshOffenceTitle));
        ofNullable(type.getLegislation()).ifPresent(offenceLegislation -> offenceBuilder.add("offenceLegislation", offenceLegislation));
        ofNullable(courtApplication.getApplicationParticulars()).ifPresent(offenceWording -> offenceBuilder.add("offenceWording", offenceWording));
    }

    private void buildOffence(final JsonObjectBuilder offenceBuilder, final Offence offence, final Offence offenceFromHearing) {
        offenceBuilder.add(ID, offence.getId().toString());

        if (nonNull(offence.getOffenceFacts())) {
            final OffenceFacts offenceFacts = offence.getOffenceFacts();
            ofNullable(offenceFacts.getAlcoholReadingAmount())
                    .ifPresent(alcoholReadingAmount -> offenceBuilder.add("alcoholReadingAmount", alcoholReadingAmount));

            ofNullable(offenceFacts.getAlcoholReadingMethodDescription())
                    .ifPresent(alcoholReadingMethodDescription -> offenceBuilder.add("alcoholReadingMethodDescription", alcoholReadingMethodDescription));
        }


        if ((nonNull(offenceFromHearing)) && (nonNull(offenceFromHearing.getPlea()))) {
            final Plea pLea = offenceFromHearing.getPlea();
            setPleaAndPleaDateIfNotIndicatedNotGuilty(offenceBuilder, pLea.getPleaValue(), pLea.getPleaDate());
        } else if (nonNull(offence.getPlea())) {
            final Plea pLea = offence.getPlea();
            setPleaAndPleaDateIfNotIndicatedNotGuilty(offenceBuilder, pLea.getPleaValue(), pLea.getPleaDate());
        }

        if ((nonNull(offenceFromHearing)) && (nonNull(offenceFromHearing.getIndicatedPlea()))) {
            final IndicatedPlea pLea = offenceFromHearing.getIndicatedPlea();
            setPleaAndPleaDateIfNotIndicatedNotGuilty(offenceBuilder, pLea.getIndicatedPleaValue().name(), pLea.getIndicatedPleaDate());
        } else if (nonNull(offence.getIndicatedPlea())) {
            final IndicatedPlea pLea = offence.getIndicatedPlea();
            setPleaAndPleaDateIfNotIndicatedNotGuilty(offenceBuilder, pLea.getIndicatedPleaValue().name(), pLea.getIndicatedPleaDate());
        }

        ofNullable(offence.getMaxPenalty()).ifPresent(maxPenalty -> offenceBuilder.add("maxPenalty", maxPenalty));
        ofNullable(offence.getConvictionDate()).ifPresent(convictedOn -> offenceBuilder.add("convictedOn", convictedOn.format(DATE_FORMATTER)));
        ofNullable(offence.getLastAdjournDate()).ifPresent(adjournedDate -> offenceBuilder.add("adjournedDate", adjournedDate.format(DATE_FORMATTER)));
        ofNullable(offence.getLastAdjournedHearingType()).ifPresent(adjournedHearingType -> offenceBuilder.add("adjournedHearingType", adjournedHearingType.replaceAll("\n", ",")));
    }

    private void setPleaAndPleaDateIfNotIndicatedNotGuilty(final JsonObjectBuilder offenceBuilder, final String plea, LocalDate pleaDate) {
        if (!plea.equals(IndicatedPleaValue.INDICATED_NOT_GUILTY.name())) {
            offenceBuilder.add("plea", plea);
            offenceBuilder.add("pleaDate", pleaDate.format(DATE_FORMATTER));
        }
    }


    private JsonArray buildProsecutionCounsels(final List<ProsecutionCounsel> prosecutionCounsels, final List<UUID> prosecutionCaseIds) {
        final JsonArrayBuilder prosecutionCounselsArray = createArrayBuilder();
        prosecutionCounsels.stream()
                .filter(prosecutionCounsel -> containsAny(prosecutionCounsel.getProsecutionCases(), prosecutionCaseIds))
                .forEach(prosecutionCounsel -> prosecutionCounselsArray.add(buildCounsel(prosecutionCounsel.getFirstName(), prosecutionCounsel.getMiddleName(), prosecutionCounsel.getLastName())));
        return prosecutionCounselsArray.build();
    }

    private JsonArray buildDefenceCounsels(final List<DefenceCounsel> defenceCounsels, final UUID defendantId) {
        final JsonArrayBuilder defenceCounselArray = createArrayBuilder();
        defenceCounsels.stream()
                .filter(defenceCounsel -> defenceCounsel.getDefendants().contains(defendantId))
                .forEach(defenceCounsel -> defenceCounselArray.add(buildCounsel(defenceCounsel.getFirstName(), defenceCounsel.getMiddleName(), defenceCounsel.getLastName())));
        return defenceCounselArray.build();
    }

    private JsonObject buildCounsel(final String firstName, final String middleName, final String lastName) {
        final JsonObjectBuilder counsel = Json.createObjectBuilder();
        ofNullable(firstName).ifPresent(fn -> counsel.add("firstName", fn));
        ofNullable(middleName).ifPresent(mn -> counsel.add("middleName", mn));
        ofNullable(lastName).ifPresent(ln -> counsel.add("lastName", ln));
        return counsel.build();
    }


    private Integer getAge(final LocalDate dateOfBirth) {
        return nonNull(dateOfBirth) ? between(dateOfBirth, now()).getYears() : null;
    }

    private JsonObject addLjaInformation(JsonObject documentPayload, final CourtCentre courtCentre) {
        if (nonNull(courtCentre)) {
            final LjaDetails ljaDetails = courtCentre.getLja();
            if (nonNull(ljaDetails)) {
                documentPayload = addProperty(documentPayload, "ljaCode", ljaDetails.getLjaCode());
                documentPayload = addProperty(documentPayload, "ljaName", ljaDetails.getLjaName());
                if (nonNull(ljaDetails.getWelshLjaName())) {
                    documentPayload = addProperty(documentPayload, "welshLjaName", ljaDetails.getWelshLjaName());
                }
            }
        }
        return documentPayload;
    }
}
