package uk.gov.moj.cpp.progression.query.api;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.directionmanagement.query.CaseDirections;
import uk.gov.justice.directionmanagement.query.DirectionManagementCasesDirectionList;
import uk.gov.justice.progression.query.CotrDefendant;
import uk.gov.justice.progression.query.CotrDetail;
import uk.gov.justice.progression.query.CotrDetails;
import uk.gov.justice.progression.query.CourtApplications;
import uk.gov.justice.progression.query.DefendantCotrDetails;
import uk.gov.justice.progression.query.Defendants;
import uk.gov.justice.progression.query.IdpcAndCaseHistories;
import uk.gov.justice.progression.query.PetDetails;
import uk.gov.justice.progression.query.PtphDetails;
import uk.gov.justice.progression.query.TrialReadinessHearing;
import uk.gov.justice.progression.query.TrialReadinessHearingDetails;
import uk.gov.justice.progression.query.TrialSummary;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.FormQueryView;
import uk.gov.moj.cpp.progression.query.HearingQueryView;
import uk.gov.moj.cpp.progression.query.PetQueryView;
import uk.gov.moj.cpp.progression.query.ProsecutionCaseQuery;
import uk.gov.moj.cpp.progression.query.api.service.CotrQueryApiService;
import uk.gov.moj.cpp.progression.query.api.service.DefenceService;
import uk.gov.moj.cpp.progression.query.api.service.ListingService;
import uk.gov.moj.cpp.progression.query.api.service.ProgressionService;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@ServiceComponent(Component.QUERY_API)
public class CotrQueryApi {

    private static final String OVERDUE = "OVERDUE";

    @Inject
    private Requester requester;

    @Inject
    private PetQueryView petQueryView;

    @Inject
    private FormQueryView formQueryView;

    @Inject
    private ProsecutionCaseQuery prosecutionCaseQuery;

    @Inject
    private HearingQueryView hearingQueryView;

    @Inject
    private DefenceService defenceService;

    @Inject
    private ListingService listingService;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private CotrQueryApiService cotrQueryApiService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(CotrQueryApi.class);

    @SuppressWarnings({"squid:S3776", "squid:S134"})
    @Handles("progression.query.search-trial-readiness")
    public JsonEnvelope searchTrialReadiness(final JsonEnvelope envelope) {
        LOGGER.info("Calling listing.cotr.search.hearings with query parameters: {}", envelope);
        final Optional<JsonObject> optionalJsonObject = listingService.searchTrialReadiness(envelope);
        if (optionalJsonObject.isPresent()) {
            final JsonObject trialHearingsPayload = optionalJsonObject.get();
            LOGGER.info("Response from listing.cotr.search.hearings: {}", trialHearingsPayload);
            final List<UUID> hearingIds = getHearingIds(trialHearingsPayload);
            final JsonArrayBuilder trialReadinessHearingsBuilder = createArrayBuilder();
            if (isNotEmpty(hearingIds)) {
                final List<Hearing> hearingsList = getHearings(envelope, hearingIds);
                final List<Hearing> filteredHearings = new ArrayList<>();
                final String remandStatus = getValueFromJsonEnvelope(envelope, "remandStatus");
                filterWithRemandStatus(hearingsList, remandStatus, filteredHearings);

                final String trailWithOverdueDirection = getValueFromJsonEnvelope(envelope, "trailWithOverdueDirection");
                final List<TrialReadinessHearing> trialReadinessHearings = new ArrayList<>();

                if ("Y".equalsIgnoreCase(trailWithOverdueDirection)) {
                    filteredHearings.forEach(hearing -> {
                        final Optional<JsonObject> optionalCasesDirections = cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(requester, hearing.getId().toString());
                        DirectionManagementCasesDirectionList directionManagementCasesDirectionList;
                        if (optionalCasesDirections.isPresent()) {
                            directionManagementCasesDirectionList = jsonObjectToObjectConverter.convert(optionalCasesDirections.get(), DirectionManagementCasesDirectionList.class);
                            final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = nonNull(directionManagementCasesDirectionList) ? cotrQueryApiService.convertCasesDirections(directionManagementCasesDirectionList.getCaseDirections()) : null;
                            final Optional<uk.gov.justice.progression.query.CaseDirections> anyOverDueDirection = caseDirections.stream()
                                    .filter(caseDirections1 -> OVERDUE.equalsIgnoreCase(caseDirections1.getStatus()))
                                    .findAny();
                            if (anyOverDueDirection.isPresent()) {
                                trialReadinessHearings.add(cotrQueryApiService.getTrialReadinessHearing(hearing, caseDirections));
                            }
                        }
                    });
                } else {
                    filteredHearings.forEach(hearing -> {
                        final Optional<JsonObject> optionalCasesDirections = cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(requester, hearing.getId().toString());
                        DirectionManagementCasesDirectionList directionManagementCasesDirectionList;
                        if (optionalCasesDirections.isPresent()) {
                            directionManagementCasesDirectionList = jsonObjectToObjectConverter.convert(optionalCasesDirections.get(), DirectionManagementCasesDirectionList.class);
                            final List<uk.gov.justice.progression.query.CaseDirections> caseDirections = nonNull(directionManagementCasesDirectionList) ? cotrQueryApiService.convertCasesDirections(directionManagementCasesDirectionList.getCaseDirections()) : null;
                            trialReadinessHearings.add(cotrQueryApiService.getTrialReadinessHearing(hearing, caseDirections));
                        } else {
                            trialReadinessHearings.add(cotrQueryApiService.getTrialReadinessHearing(hearing, null));
                        }
                    });
                }
                if (isNotEmpty(trialReadinessHearings)) {
                    trialReadinessHearings.forEach(hearing -> {
                        final JsonObject hearingJsonObject = objectToJsonObjectConverter.convert(hearing);
                        trialReadinessHearingsBuilder.add(hearingJsonObject);
                    });
                }
            }
            final JsonObject responsePayload = createObjectBuilder()
                    .add("trialReadinessHearings", trialReadinessHearingsBuilder.build())
                    .build();
            return envelopeFrom(envelope.metadata(), responsePayload);
        }
        return envelopeFrom(envelope.metadata(), JsonObjects.createObjectBuilder().build());
    }

    @Handles("progression.query.trial-readiness-details")
    public JsonEnvelope getTrialReadinessDetails(final JsonEnvelope envelope) {

        final String hearingId = envelope.payloadAsJsonObject().getString("hearingId");
        final Hearing hearing = getHearing(envelope, hearingId);
        final Optional<JsonObject> optionalCasesDirections = cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(requester, hearingId);
        DirectionManagementCasesDirectionList directionManagementCasesDirectionList = null;
        if (optionalCasesDirections.isPresent()) {
            directionManagementCasesDirectionList = jsonObjectToObjectConverter.convert(optionalCasesDirections.get(), DirectionManagementCasesDirectionList.class);
        }

        final NextHearing earliestNextHearing = getEarliestNextHearing(hearing);

        final TrialReadinessHearingDetails trialReadinessHearingDetails = TrialReadinessHearingDetails.trialReadinessHearingDetails()
                .withTrialSummary(TrialSummary.trialSummary()
                        .withHearingType(hearing.getType())
                        .withHearingDay(cotrQueryApiService.getEarliestHearingDay(hearing.getHearingDays()))
                        .withNextHearingDate(getNextHearingDate(earliestNextHearing))
                        .withNextHearingType(nonNull(earliestNextHearing) ? earliestNextHearing.getType() : null)
                        .build())
                .withIdpcAndCaseHistories(createIdpcAndCaseHistory(envelope, hearing))
                .withCourtApplications(isNotEmpty(hearing.getCourtApplications()) ? createCourtApplications(hearing) : null)
                .withCaseDirections(nonNull(directionManagementCasesDirectionList) ? cotrQueryApiService.convertCasesDirections(directionManagementCasesDirectionList.getCaseDirections()) : null)
                .withDefendantCotrDetails(createDefendantCotrDetails(hearing))
                .withPetDetails(createPetDetails(envelope, hearing))
                .withPtphDetails(createPtphDetails(envelope, hearing))
                .build();
        final JsonObject trialReadinessHearingDetailsJson = objectToJsonObjectConverter.convert(trialReadinessHearingDetails);
        return envelopeFrom(envelope.metadata(), trialReadinessHearingDetailsJson);
    }

    private LocalDate getNextHearingDate(final NextHearing earliestNextHearing) {
        if (nonNull(earliestNextHearing)) {
            if (nonNull(earliestNextHearing.getListedStartDateTime())) {
                return earliestNextHearing.getListedStartDateTime().toLocalDate();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private NextHearing getEarliestNextHearing(final Hearing hearing) {
        final List<NextHearing> nextHearings = new ArrayList<>();
        if (isNotEmpty(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().forEach(prosecutionCase ->
                prosecutionCase.getDefendants().forEach(defendant ->
                    defendant.getOffences().forEach(offence -> {
                        if (isNotEmpty(offence.getJudicialResults())) {
                            offence.getJudicialResults().forEach(judicialResult -> {
                                if (nonNull(judicialResult.getNextHearing())) {
                                    nextHearings.add(judicialResult.getNextHearing());
                                }
                            });
                        }
                    })
                )
            );
        }
        if (isEmpty(nextHearings)) {
            return null;
        }
        return nextHearings.stream()
                .min(comparing(NextHearing::getListedStartDateTime))
                .orElse(null);

    }

    private List<PtphDetails> createPtphDetails(JsonEnvelope envelope, Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .map(prosecutionCase -> createPtphDetails(envelope, prosecutionCase))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<PetDetails> createPetDetails(final JsonEnvelope envelope, final Hearing hearing) {
        final CourtCentre courtCentre = hearing.getCourtCentre();
        return hearing.getProsecutionCases().stream()
                .map(prosecutionCase -> createPetDetails(envelope, courtCentre, prosecutionCase))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("squid:S1188")
    private List<PetDetails> createPetDetails(JsonEnvelope envelope, CourtCentre courtCentre, ProsecutionCase prosecutionCase) {
        final List<Defendant> defendants = prosecutionCase.getDefendants();
        final JsonObject petsForCase = progressionService.getPetsForCase(petQueryView, envelope, prosecutionCase.getId().toString());
        if (nonNull(petsForCase)) {
            final JsonArray petsJsonArray = petsForCase.getJsonArray("pets");
            return petsJsonArray.stream()
                    .map(JsonObject.class::cast)
                    .map(obj -> {
                        final String petId = obj.getString("petId");
                        final JsonArray defendantsJsonArray = obj.getJsonArray("defendants");
                        final JsonObject pet = progressionService.getPet(requester, envelope, petId);
                        final String lastUpdated = pet.getString("lastUpdated");
                        final List<String> defendantIds = defendantsJsonArray.stream()
                                .map(JsonObject.class::cast)
                                .map(jsonObject -> jsonObject.getString("defendantId"))
                                .collect(toList());
                        final List<Defendants> defendantList = defendants.stream()
                                .filter(defendant -> defendantIds.contains(defendant.getId().toString()))
                                .map(defendant -> createDefendantDetails(defendant.getPersonDefendant().getPersonDetails()))
                                .collect(toList());
                        final String data = pet.getString("data");
                        return PetDetails.petDetails()
                                .withCourtCentre(courtCentre)
                                .withLastChangeDate(LocalDate.parse(lastUpdated, DateTimeFormatter.ISO_DATE_TIME))
                                .withDefendants(defendantList)
                                .withData(data)
                                .build();
                    })
                    .collect(toList());
        }
        return Collections.emptyList();
    }

    private  List<PtphDetails> createPtphDetails(JsonEnvelope envelope, ProsecutionCase prosecutionCase) {
        final List<Defendant> defendants = prosecutionCase.getDefendants();
        final JsonObject formsForCase = progressionService.getFormsForCase(formQueryView, envelope, prosecutionCase.getId().toString());
        if (nonNull(formsForCase)) {
            final JsonArray formsJsonArray = formsForCase.getJsonArray("forms");
            return formsJsonArray.stream()
                    .map(JsonObject.class::cast)
                    .map(obj -> {
                        final String courtFormId = obj.getString("courtFormId");
                        final JsonObject form = progressionService.getForm(formQueryView, envelope, prosecutionCase.getId().toString(), courtFormId);
                        final String lastUpdated = form.getString("lastUpdated", null);
                        final JsonArray defendantsJsonArray = obj.getJsonArray("defendants");
                        final List<String> defendantIds = defendantsJsonArray.stream()
                                .map(JsonObject.class::cast)
                                .map(jsonObject -> jsonObject.getString("defendantId"))
                                .collect(toList());
                        final List<Defendants> defendantList = defendants.stream()
                                .filter(defendant -> defendantIds.contains(defendant.getId().toString()))
                                .map(defendant -> createDefendantDetails(defendant.getPersonDefendant().getPersonDetails()))
                                .collect(toList());
                        return PtphDetails.ptphDetails()
                                .withLastChangeDate(nonNull(lastUpdated) ? LocalDate.parse(lastUpdated, DateTimeFormatter.ISO_DATE_TIME) : null)
                                .withDefendants(defendantList)
                                .build();
                    })
                    .collect(toList());
        }
        return Collections.emptyList();
    }

    private uk.gov.justice.progression.query.Defendants createDefendantDetails(Person personDetails) {
        return uk.gov.justice.progression.query.Defendants
                .defendants()
                .withFirstName(personDetails.getFirstName())
                .withLastName(personDetails.getLastName())
                .build();
    }

    private List<CourtApplications> createCourtApplications(final Hearing hearing) {
        return hearing.getCourtApplications().stream()
                .map(courtApplication -> CourtApplications.courtApplications()
                        .withId(courtApplication.getId())
                        .withApplicant(courtApplication.getApplicant())
                        .withType(courtApplication.getType())
                        .withDecisionMadeDate(courtApplication.getApplicationDecisionSoughtByDate())
                        .withReceivedDate(courtApplication.getApplicationReceivedDate())
                        .withApplicationStatus(courtApplication.getApplicationStatus())
                        .withRespondents(courtApplication.getRespondents())
                        .build())
                .collect(toList());
    }

    private List<DefendantCotrDetails> createDefendantCotrDetails(final Hearing hearing) {
        final List<DefendantCotrDetails> defendantCotrDetailsList = new ArrayList<>();
        hearing.getProsecutionCases().forEach(prosecutionCase -> {
            final Optional<JsonObject> optionalCasesDirections = cotrQueryApiService.getCaseDirectionsByHearingIdAndDirectionIds(requester, hearing.getId().toString());
            DirectionManagementCasesDirectionList directionManagementCasesDirectionList = null;
            if (optionalCasesDirections.isPresent()) {
                directionManagementCasesDirectionList = jsonObjectToObjectConverter.convert(optionalCasesDirections.get(), DirectionManagementCasesDirectionList.class);
            }
            final Optional<JsonObject> cotrDetailsJson = cotrQueryApiService.getCotrDetails(prosecutionCaseQuery, prosecutionCase.getId().toString());
            CotrDetails cotrDetails = null;
            if (cotrDetailsJson.isPresent()) {
                cotrDetails = jsonObjectToObjectConverter.convert(cotrDetailsJson.get(), CotrDetails.class);
            }

            final DirectionManagementCasesDirectionList directions = directionManagementCasesDirectionList;
            final CotrDetails cotrDetailsForDefendant = cotrDetails;
            prosecutionCase.getDefendants().forEach(defendant -> defendantCotrDetailsList.add(createDefendantCotrDetails(cotrDetailsForDefendant, directions, defendant)));
        });
        return defendantCotrDetailsList;
    }

    @SuppressWarnings("squid:S134")
    private DefendantCotrDetails createDefendantCotrDetails(final CotrDetails cotrDetails, final DirectionManagementCasesDirectionList directionManagementCasesDirectionList, final Defendant defendant) {
        Boolean isProsecutionServed = false;
        Boolean isDefenceServed = false;
        if (nonNull(cotrDetails)) {
            for (final CotrDetail cotrDetail : cotrDetails.getCotrDetails()) {
                for (final CotrDefendant cotrDefendants : cotrDetail.getCotrDefendants()) {
                    if (cotrDefendants.getId().equals(defendant.getId())) {
                        isProsecutionServed = cotrDetail.getIsProsecutionServed();
                        isDefenceServed = cotrDefendants.getIsDefenceServed();
                    }
                }
            }
        }
        CaseDirections caseDirections = null;
        if (nonNull(directionManagementCasesDirectionList) && isNotEmpty(directionManagementCasesDirectionList.getCaseDirections())) {
            caseDirections = directionManagementCasesDirectionList.getCaseDirections().stream()
                    .filter(directions -> defendant.getId().equals(directions.getAssignee().getAssigneePersonId()))
                    .findFirst().orElse(null);
        }
        String cotrStatus = null;
        LocalDate dueDate = null;
        if (nonNull(caseDirections)) {
            cotrStatus = caseDirections.getStatus();
            if (nonNull(caseDirections.getDueDate())) {
                dueDate = LocalDate.parse(caseDirections.getDueDate());
            }
        }
        return DefendantCotrDetails.defendantCotrDetails()
                .withFirstName(cotrQueryApiService.getDefendantFirstName(defendant))
                .withLastName(cotrQueryApiService.getDefendantLastName(defendant))
                .withIsProsecutionServed(isProsecutionServed)
                .withIsDefenceServed(isDefenceServed)
                .withDueDate(dueDate)
                .withCotrStatus(cotrStatus)
                .build();

    }

    private List<IdpcAndCaseHistories> createIdpcAndCaseHistory(final JsonEnvelope envelope, final Hearing hearing) {
        return hearing.getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .map(defendant -> createIdpcAndCaseHistory(envelope, defendant))
                .collect(toList());
    }

    private IdpcAndCaseHistories createIdpcAndCaseHistory(final JsonEnvelope envelope, final Defendant defendant) {

        final Optional<JsonObject> idpcDetailsOptional = defenceService.getIdpcDetailsForDefendant(requester, envelope, defendant.getId().toString());

        final LocalDate idpcServiceDate = idpcDetailsOptional.isPresent() ? LocalDate.parse(idpcDetailsOptional.get().getJsonObject("idpcMetadata").getString("publishedDate")) : null;
        return IdpcAndCaseHistories.idpcAndCaseHistories()
                .withFirstName(cotrQueryApiService.getDefendantFirstName(defendant))
                .withLastName(cotrQueryApiService.getDefendantLastName(defendant))
                .withPlea(defendant.getOffences().get(0).getPlea())
                .withBailStatus(defendant.getPersonDefendant().getBailStatus())
                .withIdpcServiceDate(idpcServiceDate)
                .withCustodyTimeLimit(cotrQueryApiService.createCtlForDefendant(defendant))
                .build();
    }

    private void filterWithRemandStatus(final List<Hearing> hearings, final String remandStatus, List<Hearing> filteredHearings) {
        if (isNull(remandStatus)) {
            filteredHearings.addAll(hearings);
        } else {
            for (final Hearing hearing : hearings) {
                final Optional<Defendant> defendantWithMatchedRemandStatus = hearing.getProsecutionCases().stream()
                        .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                        .filter(defendant -> nonNull(defendant.getPersonDefendant().getBailStatus())
                                && defendant.getPersonDefendant().getBailStatus().getId().toString().equalsIgnoreCase(remandStatus))
                        .findFirst();
                if (defendantWithMatchedRemandStatus.isPresent()) {
                    filteredHearings.add(hearing);
                }
            }
        }
    }

    private String getValueFromJsonEnvelope(final JsonEnvelope envelope, final String name) {
        return envelope.payloadAsJsonObject().getString(name, null);
    }

    private List<Hearing> getHearings(final JsonEnvelope envelope, final List<UUID> hearingIds) {
        final List<Hearing> hearings = new ArrayList<>();
        hearingIds.forEach(hearingId -> {
            final JsonObject hearingJson = progressionService.getHearing(hearingQueryView, envelope, hearingId.toString());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson.getJsonObject("hearing"), Hearing.class);
            hearings.add(hearing);
        });
        return hearings;
    }

    private Hearing getHearing(final JsonEnvelope envelope, final String hearingId) {
        final JsonObject hearingJson = progressionService.getHearing(hearingQueryView, envelope, hearingId);
        return jsonObjectToObjectConverter.convert(hearingJson.getJsonObject("hearing"), Hearing.class);
    }

    private List<UUID> getHearingIds(final JsonObject listingResponse) {
        final JsonArray hearings = listingResponse.getJsonArray("hearings");
        if (isNotEmpty(hearings)) {
            return hearings.stream()
                    .map(jsonValue -> fromString(((JsonObject) jsonValue).getString("id")))
                    .collect(toList());
        }
        return emptyList();
    }

}
