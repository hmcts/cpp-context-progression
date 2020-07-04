package uk.gov.moj.cpp.progression.service;

import static java.util.Optional.ofNullable;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S2789", "squid:S3655", "squid:S1192", "squid:S1168", "pmd:NullAssignment", "squid:CallToDeprecatedMethod"})
public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    private static final String APPLICATION_ID = "applicationId";
    public static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";

    public static final String HEARING_ID = "hearingId";
    public static final String DEFENDANT_ID = "defendantId";
    private static final String PROGRESSION_COMMAND_CREATE_PROSECUTION_CASE = "progression.command.create-prosecution-case";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String PROGRESSION_QUERY_SEARCH_CASES = "progression.query.search-cases";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_DEFENDANT_REQUEST = "progression.query.defendant-request";
    private static final String PROGRESSION_QUERY_HEARING = "progression.query.hearing";
    private static final String PROGRESSION_QUERY_LINKED_CASES = "progression.query.case-lsm-info";
    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND = "progression.command.update-defendant-listing-status";
    private static final String PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING = "progression.command.record-unscheduled-hearing";
    private static final String PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND = "progression.command.list-unscheduled-hearing";
    private static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA = "progression.command.prepare-summons-data";
    private static final String PUBLIC_EVENT_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    private static final String UNSCHEDULED = "isUnscheduled";
    private static final String HEARING = "hearing";
    private static final String HEARING_INITIALISED = "HEARING_INITIALISED";
    private static final String SENT_FOR_LISTING = "SENT_FOR_LISTING";
    private static final String EMPTY_STRING = "";
    private static final String PROGRESSION_QUERY_COURT_APPLICATION = "progression.query.application";
    private static final String PROGRESSION_COMMAND_UPDATE_COURT_APPLICATION_STATUS = "progression.command.update-court-application-status";
    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH = "progression.command.update-defendant-for-prosecution-case";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK = "progression.command.create-hearing-application-link";
    private static final String PROGRESSION_COMMAND_HEARING_RESULTED_UPDATE_CASE = "progression.command.hearing-resulted-update-case";
    private static final String PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS = "progression.command.hearing-confirmed-update-case-status";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION = "progression.command.update-hearing-for-partial-allocation";
    public static final String CASE_STATUS = "caseStatus";
    private static final String COURT_APPLICATIONS = "courtApplications";
    public static final String UNSCHEDULED_HEARING_IDS = "unscheduledHearingIds";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    private static JsonArray transformProsecutionCases(final List<ConfirmedProsecutionCase> prosecutionCases) {
        final JsonArrayBuilder prosecutionCasesArrayBuilder = createArrayBuilder();

        if (isNotEmpty(prosecutionCases)) {
            for (final ConfirmedProsecutionCase prosecutionCase : prosecutionCases) {
                final JsonArrayBuilder defendantsArrayBuilder = createArrayBuilder();
                prosecutionCase.getDefendants().stream()
                        .map(ConfirmedDefendant::getId)
                        .map(UUID::toString)
                        .forEach(defendantsArrayBuilder::add);

                final JsonObject prosecutionCaseJson = createObjectBuilder()
                        .add("id", prosecutionCase.getId().toString())
                        .add("confirmedDefendantIds", defendantsArrayBuilder.build())
                        .build();

                prosecutionCasesArrayBuilder.add(prosecutionCaseJson);
            }
        }

        return prosecutionCasesArrayBuilder.build();
    }

    private static Hearing transformHearingListingNeeds(final HearingListingNeeds hearingListingNeeds) {
        final ZonedDateTime hearingDateTime = nonNull(hearingListingNeeds.getEarliestStartDateTime()) ?
                hearingListingNeeds.getEarliestStartDateTime() : hearingListingNeeds.getListedStartDateTime();

        return Hearing.hearing()
                .withHearingDays(populateHearingDays(hearingDateTime, hearingListingNeeds.getEstimatedMinutes()))
                .withCourtCentre(hearingListingNeeds.getCourtCentre())
                .withJurisdictionType(hearingListingNeeds.getJurisdictionType())
                .withId(hearingListingNeeds.getId())
                .withJudiciary(hearingListingNeeds.getJudiciary())
                .withReportingRestrictionReason(hearingListingNeeds.getReportingRestrictionReason())
                .withType(hearingListingNeeds.getType())
                .withProsecutionCases(hearingListingNeeds.getProsecutionCases())
                .build();
    }

    private static List<HearingDay> populateHearingDays(final ZonedDateTime earliestStartDateTime, final Integer
            getEstimatedMinutes) {

        List<HearingDay> days = null;

        if(nonNull(earliestStartDateTime) && nonNull(getEstimatedMinutes)) {
            days = new ArrayList<>();
            days.add(HearingDay.hearingDay().withListedDurationMinutes(getEstimatedMinutes).withSittingDay(earliestStartDateTime).build());
        }
        return days;
    }

    private static List<Defendant> filterDefendants(final ConfirmedProsecutionCase confirmedProsecutionCase, final
    ProsecutionCase prosecutionCase, final LocalDate earliestHearingDate) {

        final List<Defendant> defendantsList = new ArrayList<>();

        confirmedProsecutionCase.getDefendants().stream().forEach(confirmedDefendantConsumer -> {

            final Defendant matchedDefendant = prosecutionCase.getDefendants().stream()
                    .filter(pc -> pc.getId().equals(confirmedDefendantConsumer.getId()))
                    .findFirst().get();

            final List<Offence> matchedDefendantOffence = matchedDefendant.getOffences().stream()
                    .filter(offence -> confirmedDefendantConsumer.getOffences().stream()
                            .anyMatch(o -> o.getId().equals(offence.getId())))
                    .collect(Collectors.toList());
            defendantsList.add(populateDefendant(matchedDefendant, matchedDefendantOffence, earliestHearingDate));
        });

        return defendantsList;
    }

    private static Defendant populateDefendant(final Defendant matchedDefendant, final List<Offence>
            matchedDefendantOffence, final LocalDate earliestHearingDate) {
        final Defendant.Builder builder = Defendant.defendant()
                .withId(matchedDefendant.getId())
                .withMasterDefendantId(matchedDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(matchedDefendant.getCourtProceedingsInitiated())
                .withOffences(matchedDefendantOffence)
                .withAssociatedPersons(matchedDefendant.getAssociatedPersons())
                .withDefenceOrganisation(matchedDefendant.getDefenceOrganisation())
                .withAssociatedDefenceOrganisation(matchedDefendant.getAssociatedDefenceOrganisation())
                .withLegalEntityDefendant(matchedDefendant.getLegalEntityDefendant())
                .withMitigation(matchedDefendant.getMitigation())
                .withMitigationWelsh(matchedDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(matchedDefendant.getNumberOfPreviousConvictionsCited())
                .withPersonDefendant(matchedDefendant.getPersonDefendant())
                .withProsecutionAuthorityReference(matchedDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(matchedDefendant.getProsecutionCaseId())
                .withWitnessStatement(matchedDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(matchedDefendant.getWitnessStatementWelsh())
                .withCroNumber(matchedDefendant.getCroNumber())
                .withAliases(matchedDefendant.getAliases())
                .withPncId(matchedDefendant.getPncId())
                .withJudicialResults(matchedDefendant.getJudicialResults());
        if (nonNull(matchedDefendant.getPersonDefendant()) &&
                nonNull(matchedDefendant.getPersonDefendant().getPersonDetails()) &&
                nonNull(matchedDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth())) {

            builder.withIsYouth(LocalDateUtils.isYouth(matchedDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), earliestHearingDate));
        }
        return builder.build();
    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public void createCourtDocument(final JsonEnvelope jsonEnvelope, final List<CourtDocument> courtDocuments) {
        courtDocuments.forEach(courtDocument -> {
            final JsonObject jsonObject = Json.createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build();
            LOGGER.info("court document is being created '{}' ", jsonObject);
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT).apply(jsonObject));
        });
    }

    public void createProsecutionCases(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases) {
        prosecutionCases.forEach(prosecutionCase -> {
            final JsonObject jsonObject = Json.createObjectBuilder().add("prosecutionCase", objectToJsonObjectConverter.convert(prosecutionCase)).build();
            LOGGER.info("prosecution case is being created '{}' ", jsonObject);
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_PROSECUTION_CASE).apply(jsonObject));
            relayCaseToCourtStore(prosecutionCase);
        });
    }

    private void relayCaseToCourtStore(final ProsecutionCase prosecutionCase) {

        if (prosecutionCase != null && prosecutionCase.getProsecutionCaseIdentifier() != null && prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() != null) {
            final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder();
            payloadBuilder.add("CaseReference", prosecutionCase.getProsecutionCaseIdentifier().getCaseURN());
            try {
                this.azureFunctionService.relayCaseOnCPP(payloadBuilder.build().toString());
            } catch (final IOException ex) {
                LOGGER.error(String.format("Failed to call Azure function %s", ex));
            }
        }
    }

    public void prepareSummonsData(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing) {
        final JsonObject casesConfirmedPayload = createObjectBuilder()
                .add("hearingId", confirmedHearing.getId().toString())
                .add("courtCentre", objectToJsonObjectConverter.convert(confirmedHearing.getCourtCentre()))
                .add("hearingDateTime", ZonedDateTimes.toString(getEarliestDate(confirmedHearing.getHearingDays())))
                .add("confirmedProsecutionCaseIds", transformProsecutionCases(confirmedHearing.getProsecutionCases()))
                .build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA).apply(casesConfirmedPayload));
    }

    public void updateDefendantYouthForProsecutionCase(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate) {
        final List<ProsecutionCase> prosecutionCases = hearingInitiate.getHearing().getProsecutionCases();
        if (isNotEmpty(prosecutionCases)) {
            prosecutionCases.stream().forEach(pc -> {
                final Optional<JsonObject> prosecutionCaseJson = getProsecutionCaseDetailById(jsonEnvelope, pc.getId().toString());
                if (prosecutionCaseJson.isPresent()) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectConverter.convert(prosecutionCaseJson.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    pc.getDefendants().stream().forEach(confirmedDefendantConsumer -> {
                        final Optional<Defendant> matchedDefendant = prosecutionCaseEntity.getDefendants().stream()
                                .filter(defEnt -> defEnt.getId().equals(confirmedDefendantConsumer.getId()) && isYouthUpdated(confirmedDefendantConsumer, defEnt))
                                .findFirst();

                        if (matchedDefendant.isPresent()) {
                            final JsonObject updateYouthPayload = createObjectBuilder()
                                    .add("defendant", objectToJsonObjectConverter.convert(transformDefendantFromEntity(matchedDefendant.get(), confirmedDefendantConsumer.getIsYouth(), pc.getId())))
                                    .add("id", confirmedDefendantConsumer.getId().toString())
                                    .add("prosecutionCaseId", pc.getId().toString())
                                    .build();

                            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH).apply(updateYouthPayload));
                        }
                    });
                }
            });
        }
    }

    public void updateDefendantYouthForProsecutionCase(final JsonEnvelope jsonEnvelope, final List<ProsecutionCase> prosecutionCases) {
        if (isNotEmpty(prosecutionCases)) {
            prosecutionCases.stream().forEach(pc -> {
                final Optional<JsonObject> prosecutionCaseJson = getProsecutionCaseDetailById(jsonEnvelope, pc.getId().toString());
                if (prosecutionCaseJson.isPresent()) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectConverter.convert(prosecutionCaseJson.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    pc.getDefendants().stream().forEach(confirmedDefendantConsumer -> {
                        final Optional<Defendant> matchedDefendant = prosecutionCaseEntity.getDefendants().stream()
                                .filter(defEnt -> defEnt.getId().equals(confirmedDefendantConsumer.getId()) && isYouthUpdated(confirmedDefendantConsumer, defEnt))
                                .findFirst();

                        if (matchedDefendant.isPresent()) {
                            final JsonObject updateYouthPayload = createObjectBuilder()
                                    .add("defendant", objectToJsonObjectConverter.convert(transformDefendantFromEntity(matchedDefendant.get(), confirmedDefendantConsumer.getIsYouth(), pc.getId())))
                                    .add("id", confirmedDefendantConsumer.getId().toString())
                                    .add("prosecutionCaseId", pc.getId().toString())
                                    .build();

                            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH).apply(updateYouthPayload));
                        }
                    });
                }
            });
        }
    }

    private boolean isYouthUpdated(final Defendant confirmedDefendantConsumer, final Defendant defEnt) {
        if (nonNull(confirmedDefendantConsumer.getIsYouth())) {
            return !confirmedDefendantConsumer.getIsYouth().equals(defEnt.getIsYouth());
        }
        return false;
    }

    private DefendantUpdate transformDefendantFromEntity(final Defendant defendantEntity, final boolean isYouth, final UUID prosecutionCaseId) {
        return DefendantUpdate.defendantUpdate()
                .withIsYouth(isYouth)
                .withProsecutionCaseId(prosecutionCaseId)
                .withId(defendantEntity.getId())
                .withMasterDefendantId(defendantEntity.getMasterDefendantId())
                .withAssociatedPersons(defendantEntity.getAssociatedPersons())
                .withDefenceOrganisation(defendantEntity.getDefenceOrganisation())
                .withLegalEntityDefendant(defendantEntity.getLegalEntityDefendant())
                .withMitigation(defendantEntity.getMitigation())
                .withMitigationWelsh(defendantEntity.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendantEntity.getNumberOfPreviousConvictionsCited())
                .withPersonDefendant(defendantEntity.getPersonDefendant())
                .withProsecutionAuthorityReference(defendantEntity.getProsecutionAuthorityReference())
                .withWitnessStatement(defendantEntity.getWitnessStatement())
                .withWitnessStatementWelsh(defendantEntity.getWitnessStatementWelsh())
                .withCroNumber(defendantEntity.getCroNumber())
                .withAliases(defendantEntity.getAliases())
                .withPncId(defendantEntity.getPncId())
                .withJudicialResults(defendantEntity.getJudicialResults())
                .build();
    }

    public Optional<JsonObject> searchCaseDetailByReference(final JsonEnvelope envelope, final String reference) {

        final JsonObject requestParameter = createObjectBuilder().add("q", reference).build();

        LOGGER.info("search for case detail with reference {} ", reference);

        final JsonEnvelope response = requester.request(enveloper.withMetadataFrom(envelope, PROGRESSION_QUERY_SEARCH_CASES).apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("search for case detail response {}", response.toObfuscatedDebugString());
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getProsecutionCaseDetailById(final JsonEnvelope envelope, final String caseId) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} ,   get prosecution case detail request {}", caseId, requestParameter);

        final JsonEnvelope prosecutioncase = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_PROSECUTION_CASES)
                .apply(requestParameter));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutioncase.toObfuscatedDebugString());
        }

        if (!prosecutioncase.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(prosecutioncase.payloadAsJsonObject());
        }
        return result;
    }

    public Optional<JsonObject> searchLinkedCases(final JsonEnvelope envelope, final String caseId) {

        final JsonObject requestParameter = createObjectBuilder().add("caseId", caseId).build();

        LOGGER.info("search for already linked/merged cases with case id {} ", caseId);

        final JsonEnvelope response = requester.request(enveloper.withMetadataFrom(envelope, PROGRESSION_QUERY_LINKED_CASES).apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("search for already linked/merged cases response {}", response.toObfuscatedDebugString());
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getDefendantRequestByDefendantId(final JsonEnvelope envelope, final String defendantId) {
        final JsonObject requestParameter = createObjectBuilder()
                .add(DEFENDANT_ID, defendantId)
                .build();

        LOGGER.info("defendantId {}, get defendant request {}", defendantId, requestParameter);

        final JsonEnvelope defendantRequest = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_DEFENDANT_REQUEST)
                .apply(requestParameter));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("defendantId {} get defendant request payload {}", defendantId, defendantRequest.toObfuscatedDebugString());
        }
        return ofNullable(defendantRequest.payloadAsJsonObject());
    }

    public void updateHearingListingStatusToHearingInitiated(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate) {
        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                .add(HEARING_LISTING_STATUS, HEARING_INITIALISED)
                .add(HEARING, objectToJsonObjectConverter.convert(hearingInitiate.getHearing()))
                .build();
        LOGGER.info("update hearing listing status after initiate hearing with payload {}", hearingListingStatusCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
    }

    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing) {
        listCourtHearing.getHearings().forEach(hearingListingNeeds -> {
            final Hearing hearing = transformHearingListingNeeds(hearingListingNeeds);
            final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                    .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                    .add(HEARING, objectToJsonObjectConverter.convert(hearing)).build();
            LOGGER.info("update hearing listing status after send case for listing with payload {}", hearingListingStatusCommand);
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
        });
    }


    public void listUnscheduledHearings(final JsonEnvelope jsonEnvelope, final Hearing hearing) {
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING, objectToJsonObjectConverter.convert(hearing))
                .build();

        sender.send(Enveloper.envelop(payload).withName(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND).withMetadataFrom(jsonEnvelope));
    }

    public void sendUpdateDefendantListingStatusForUnscheduledListing(final JsonEnvelope jsonEnvelope, final List<Hearing> unscheduledHearings) {
        unscheduledHearings.forEach(unscheduledHearing -> {
            final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                    .add(UNSCHEDULED, true)
                    .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                    .add(HEARING, objectToJsonObjectConverter.convert(unscheduledHearing))
                    .build();
            sender.send(Enveloper.envelop(hearingListingStatusCommand).withName(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).withMetadataFrom(jsonEnvelope));
        });
    }

    public void recordUnlistedHearing(final JsonEnvelope jsonEnvelope, final UUID originalHearingId, List<Hearing> newHearingIds) {
        final JsonArrayBuilder newHearingIdArrays = createArrayBuilder();
        newHearingIds.stream().forEach(s->newHearingIdArrays.add(s.getId().toString()));


        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                                                                   .add(HEARING_ID, originalHearingId.toString())
                                                                   .add(UNSCHEDULED_HEARING_IDS,newHearingIdArrays.build())
                                                                   .build();
            sender.send(Enveloper.envelop(hearingListingStatusCommand).withName(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING).withMetadataFrom(jsonEnvelope));
    }

    public void updateHearingListingStatusToHearingUpdate(final JsonEnvelope jsonEnvelope, final Hearing hearing) {
        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                .add(HEARING_LISTING_STATUS, "HEARING_INITIALISED")
                .add(HEARING, objectToJsonObjectConverter.convert(hearing))
                .build();
        LOGGER.info("update hearing listing status after initiate hearing with payload {}", hearingListingStatusCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
    }

    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope, final HearingUpdated hearingUpdated) {
        final JsonObject hearingDetailChangedPayload = Json.createObjectBuilder()
                .add(HEARING, objectToJsonObjectConverter.convert(transformUpdatedHearing(hearingUpdated.getUpdatedHearing(), jsonEnvelope)))
                .build();
        LOGGER.info("publish public hearing details changed event with payload {}", hearingDetailChangedPayload);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_EVENT_HEARING_DETAIL_CHANGED).apply(hearingDetailChangedPayload));
    }

    public Optional<CourtApplication> getCourtApplicationByIdTyped(final JsonEnvelope envelope, final String courtApplicationId) {
        final Optional<JsonObject> jsonObject = getCourtApplicationById(envelope, courtApplicationId);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("getCourtApplicationByIdTyped courtApplication: %s payload: %s", courtApplicationId, jsonObject.toString())
            );

        }
        return jsonObject.map(json -> jsonObjectConverter.convert(json.getJsonObject("courtApplication"), CourtApplication.class));
    }

    public Optional<JsonObject> getCourtApplicationById(final JsonEnvelope envelope, final String courtApplicationId) {

        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(APPLICATION_ID, courtApplicationId)
                .build();

        LOGGER.info("courtApplicationId {} ,   get court application {}", courtApplicationId, requestParameter);

        final JsonEnvelope courtApplication = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_COURT_APPLICATION)
                .apply(requestParameter));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("courtApplicationId {} ,   get court application {}", courtApplicationId, courtApplication.toObfuscatedDebugString());
        }

        if (!courtApplication.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(courtApplication.payloadAsJsonObject());
        }
        return result;
    }

    public void updateCourtApplicationStatus(final JsonEnvelope jsonEnvelope, final List<UUID> applicationIds, final ApplicationStatus status) {
        applicationIds.forEach(applicationId -> updateCourtApplicationStatus(jsonEnvelope, applicationId, status));
    }

    public void updateCourtApplicationStatus(final JsonEnvelope jsonEnvelope, final UUID applicationId, final ApplicationStatus status) {
        final JsonObject updateApplicationStatus = Json.createObjectBuilder()
                .add("id", applicationId.toString())
                .add("applicationStatus", status.toString())
                .build();

        LOGGER.info("Application id '{}' has been updated with status of '{}'", applicationId, updateApplicationStatus);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_UPDATE_COURT_APPLICATION_STATUS).apply(updateApplicationStatus));
    }

    public Optional<JsonObject> getHearing(final JsonEnvelope envelope, final String hearingId) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add("hearingId", hearingId)
                .build();
        final JsonEnvelope hearing = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_HEARING)
                .apply(requestParameter));
        if (!hearing.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(hearing.payloadAsJsonObject());
        }

        return result;
    }

    private Hearing transformUpdatedHearing(final ConfirmedHearing updatedHearing, final JsonEnvelope jsonEnvelope) {
        return Hearing.hearing()
                .withId(updatedHearing.getId())
                .withType(updatedHearing.getType())
                .withJurisdictionType(updatedHearing.getJurisdictionType())
                .withReportingRestrictionReason(updatedHearing.getReportingRestrictionReason())
                .withHearingLanguage(updatedHearing.getHearingLanguage())
                .withHearingDays(updatedHearing.getHearingDays())
                .withCourtCentre(transformCourtCentre(updatedHearing.getCourtCentre(), jsonEnvelope))
                .withJudiciary(enrichJudiciaries(updatedHearing.getJudiciary(), jsonEnvelope))
                .build();
    }

    public Hearing transformConfirmedHearing(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope) {

        final LocalDate earliestHearingDate = getEarliestDate(confirmedHearing.getHearingDays()).toLocalDate();

        return Hearing.hearing()
                .withHearingDays(confirmedHearing.getHearingDays())
                .withCourtCentre(transformCourtCentre(confirmedHearing.getCourtCentre(), jsonEnvelope))
                .withJurisdictionType(confirmedHearing.getJurisdictionType())
                .withId(confirmedHearing.getId())
                .withHearingLanguage(confirmedHearing.getHearingLanguage())
                .withJudiciary(enrichJudiciaries(confirmedHearing.getJudiciary(), jsonEnvelope))
                .withReportingRestrictionReason(confirmedHearing.getReportingRestrictionReason())
                .withType(confirmedHearing.getType())
                .withHasSharedResults(false)
                .withProsecutionCases(transformProsecutionCase(confirmedHearing.getProsecutionCases(), earliestHearingDate, jsonEnvelope))
                .withCourtApplications(extractCourtApplications(confirmedHearing, jsonEnvelope))
                .build();
    }

    public Hearing updateHearingForHearingUpdated(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope, final Hearing hearing) {

        return Hearing.hearing()
                .withHearingDays(confirmedHearing.getHearingDays())
                .withCourtCentre(transformCourtCentre(confirmedHearing.getCourtCentre(), jsonEnvelope))
                .withJurisdictionType(confirmedHearing.getJurisdictionType())
                .withId(confirmedHearing.getId())
                .withHearingLanguage(confirmedHearing.getHearingLanguage())
                .withJudiciary(enrichJudiciaries(confirmedHearing.getJudiciary(), jsonEnvelope))
                .withReportingRestrictionReason(confirmedHearing.getReportingRestrictionReason())
                .withType(confirmedHearing.getType())
                .withHasSharedResults(false)
                .withProsecutionCases(hearing.getProsecutionCases())
                .withCourtApplications(hearing.getCourtApplications())
                .build();
    }

    public Hearing transformBoxWorkApplication(final BoxworkApplicationReferred boxWorkApplicationReferred) {

        return Hearing.hearing()
                .withId(boxWorkApplicationReferred.getHearingRequest().getId())
                .withHearingDays(Arrays.asList(HearingDay.hearingDay()
                        .withListedDurationMinutes(10)
                        .withSittingDay(boxWorkApplicationReferred.getHearingRequest().getListedStartDateTime()).build()))
                .withCourtCentre(boxWorkApplicationReferred.getHearingRequest().getCourtCentre())
                .withJurisdictionType(boxWorkApplicationReferred.getHearingRequest().getJurisdictionType())
                .withIsBoxHearing(true)
                .withJudiciary(boxWorkApplicationReferred.getHearingRequest().getJudiciary())
                .withReportingRestrictionReason(boxWorkApplicationReferred.getHearingRequest().getReportingRestrictionReason())
                .withType(boxWorkApplicationReferred.getHearingRequest().getType())
                .withProsecutionCases(boxWorkApplicationReferred.getHearingRequest().getProsecutionCases())
                .withCourtApplications(boxWorkApplicationReferred.getHearingRequest().getCourtApplications())
                .build();
    }

    public List<CourtApplication> extractCourtApplications(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope) {
        final List<UUID> courtApplicationIds = confirmedHearing.getCourtApplicationIds();
        if (courtApplicationIds != null) {
            final List<CourtApplication> applicationDetails = new ArrayList<>();
            for (final UUID applicationId : courtApplicationIds) {
                getCourtApplicationById(jsonEnvelope, applicationId.toString()).ifPresent(
                        application -> applicationDetails.add(
                                jsonObjectConverter.convert(application.getJsonObject("courtApplication"), CourtApplication.class))
                );
            }
            return applicationDetails;
        }
        return null;
    }

    private CourtCentre transformCourtCentre(final CourtCentre courtCentre, final JsonEnvelope jsonEnvelope) {

        final String ADDRESS_1 = "address1";
        final String ADDRESS_2 = "address2";
        final String ADDRESS_3 = "address3";
        final String ADDRESS_4 = "address4";
        final String ADDRESS_5 = "address5";
        final String POSTCODE = "postcode";

        final JsonObject courtCentreJson = referenceDataService.getOrganisationUnitById(courtCentre.getId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Court center ", courtCentre.getId().toString()));

        return CourtCentre.courtCentre()
                .withId(courtCentre.getId())
                .withName(courtCentreJson.getString("oucodeL3Name"))
                .withRoomName(nonNull(courtCentre.getRoomId()) ? enrichCourtRoomName(courtCentre.getId(), courtCentre.getRoomId(), jsonEnvelope) : null)
                .withRoomId(courtCentre.getRoomId())
                .withAddress(uk.gov.justice.core.courts.Address.address()
                        .withAddress1(courtCentreJson.getString(ADDRESS_1, EMPTY))
                        .withAddress2(courtCentreJson.getString(ADDRESS_2, EMPTY))
                        .withAddress3(courtCentreJson.getString(ADDRESS_3, EMPTY))
                        .withAddress4(courtCentreJson.getString(ADDRESS_4, EMPTY))
                        .withAddress5(courtCentreJson.getString(ADDRESS_5, EMPTY))
                        .withPostcode(courtCentreJson.getString(POSTCODE, EMPTY))
                        .build())
                .build();
    }

    private List<JudicialRole> enrichJudiciaries(final List<JudicialRole> judiciaryList, final JsonEnvelope jsonEnvelope) {
        if (Objects.isNull(judiciaryList) || judiciaryList.isEmpty()) {
            return null;
        }
        final List<UUID> judiciaryIds = judiciaryList.stream()
                .map(JudicialRole::getJudicialId)
                .collect(Collectors.toList());

        final JsonObject judiciariesJson = referenceDataService.getJudiciariesByJudiciaryIdList(judiciaryIds, jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Judiciaries ",
                        judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(","))));

        return judiciaryList.stream()
                .map(e -> mapJudiciaryRefDataToJudiciary(e, judiciariesJson))
                .collect(Collectors.toList());
    }

    private JudicialRole mapJudiciaryRefDataToJudiciary(final JudicialRole judicialRole, final JsonObject judiciariesJson) {

        final JsonObject judiciaryJson = judiciariesJson.getJsonArray("judiciaries").getValuesAs(JsonObject.class).stream()
                .filter(e -> judicialRole.getJudicialId().toString().equals(e.getString("id")))
                .findFirst()
                .orElseThrow(() -> new ReferenceDataNotFoundException("Judiciary ", judicialRole.getJudicialId().toString()));

        return JudicialRole.judicialRole()
                .withJudicialId(judicialRole.getJudicialId())
                .withTitle(judiciaryJson.getString("titlePrefix", EMPTY_STRING))
                .withFirstName(judiciaryJson.getString("forenames", EMPTY_STRING))
                .withLastName(judiciaryJson.getString("surname", EMPTY_STRING))
                .withJudicialRoleType(
                        JudicialRoleType.judicialRoleType()
                                .withJudicialRoleTypeId(UUID.fromString(judiciaryJson.getString("id")))
                                .withJudiciaryType(judiciaryJson.getString("judiciaryType"))
                                .build()
                )
                .withIsBenchChairman(judicialRole.getIsBenchChairman())
                .withIsDeputy(judicialRole.getIsDeputy())
                .withUserId(judiciaryJson.containsKey("cpUserId") ? fromString(judiciaryJson.getString("cpUserId")) : null)
                .build();
    }

    private String enrichCourtRoomName(final UUID courtCentreId, final UUID courtRoomId, final JsonEnvelope jsonEnvelope) {
        final JsonObject courtRoomsJson = referenceDataService.getCourtRoomById(courtCentreId, jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Court room ", courtCentreId.toString()));
        return getValueFromCourtRoomJson(courtRoomsJson, courtRoomId, "courtroomName");
    }

    private String getValueFromCourtRoomJson(final JsonObject courtRoomsJson, final UUID courtRoomId, final String fieldName) {
        return courtRoomsJson.getJsonArray("courtrooms").getValuesAs(JsonObject.class).stream()
                .filter(cr -> courtRoomId.toString().equals(cr.getString("id")))
                .map(cr -> cr.getString(fieldName, EMPTY_STRING)).findFirst().orElse("");
    }

    public List<ProsecutionCase> transformProsecutionCase(final List<ConfirmedProsecutionCase> confirmedProsecutionCases, final LocalDate earliestHearingDate, final JsonEnvelope jsonEnvelope) {
        if (isNotEmpty(confirmedProsecutionCases)) {
            final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
            confirmedProsecutionCases.stream().forEach(pc -> {
                final Optional<JsonObject> prosecutionCaseJson = getProsecutionCaseDetailById(jsonEnvelope, pc.getId().toString());
                if (prosecutionCaseJson.isPresent()) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectConverter.convert(prosecutionCaseJson.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                            .withId(prosecutionCaseEntity.getId())
                            .withCaseStatus(prosecutionCaseEntity.getCaseStatus())
                            .withDefendants(filterDefendants(pc, prosecutionCaseEntity, earliestHearingDate))
                            .withInitiationCode(prosecutionCaseEntity.getInitiationCode())
                            .withOriginatingOrganisation(prosecutionCaseEntity.getOriginatingOrganisation())
                            .withProsecutionCaseIdentifier(prosecutionCaseEntity.getProsecutionCaseIdentifier())
                            .withStatementOfFacts(prosecutionCaseEntity.getStatementOfFacts())
                            .withStatementOfFactsWelsh(prosecutionCaseEntity.getStatementOfFactsWelsh())
                            .withCaseMarkers(prosecutionCaseEntity.getCaseMarkers())
                            .build();
                    prosecutionCases.add(prosecutionCase);
                }
            });

            if (!prosecutionCases.isEmpty()) {
                return prosecutionCases;
            }
        }
        return null;
    }

    public HearingListingNeeds transformHearingToHearingListingNeeds(final Hearing incomingHearing, final UUID existingHearingId) {
        return HearingListingNeeds.hearingListingNeeds()
                .withCourtCentre(incomingHearing.getCourtCentre())
                .withProsecutionCases(incomingHearing.getProsecutionCases())
                .withId(existingHearingId)
                .withJurisdictionType(incomingHearing.getJurisdictionType())
                .withEstimatedMinutes(30)
                .withType(incomingHearing.getType())
                .build();
    }

    public void linkProsecutionCasesToHearing(final JsonEnvelope jsonEnvelope, final UUID hearingId, final List<UUID> caseIds) {
        caseIds.forEach(caseId ->
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK)
                        .apply(createObjectBuilder()
                                .add(CASE_ID, caseId.toString())
                                .add(HEARING_ID, hearingId.toString())
                                .build()))
        );
    }

    public void linkApplicationsToHearing(final JsonEnvelope jsonEnvelope, final Hearing hearing, final List<UUID> applicationIds, final HearingListingStatus status) {
        final JsonObject hearingJson = objectToJsonObjectConverter.convert(hearing);
        applicationIds.forEach(applicationId ->
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                        .apply(createObjectBuilder()
                                .add(APPLICATION_ID, applicationId.toString())
                                .add(HEARING_LISTING_STATUS, status.toString())
                                .add(HEARING, hearingJson)
                                .build()))
        );
    }

    public void updateCase(final JsonEnvelope jsonEnvelope, final ProsecutionCase prosecutionCase, final List<CourtApplication> courtApplications) {
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add(PROSECUTION_CASE, prosecutionCaseJson);
        if (isNotEmpty(courtApplications)) {
            payloadBuilder.add(COURT_APPLICATIONS, listToJsonArrayConverter.convert(courtApplications));
        }
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_HEARING_RESULTED_UPDATE_CASE)
                .apply(payloadBuilder.build()));

    }

    public void updateCaseStatus(final JsonEnvelope jsonEnvelope, final Hearing hearing, final List<UUID> applicationIds) {
        applicationIds.forEach(applicationId -> {
            final Optional<CourtApplication> courtApplication = getCourtApplication(hearing, applicationId);
            if (courtApplication.isPresent()) {
                final Optional<UUID> aCaseId = ofNullable(courtApplication.get().getLinkedCaseId());
                if (aCaseId.isPresent()) {
                    final Optional<ProsecutionCase> aCase = getProsecutionCase(hearing, aCaseId);
                    aCase.ifPresent(aCase1 -> {
                        if (INACTIVE.getDescription().equalsIgnoreCase(aCase1.getCaseStatus()) && !FINALISED.equals(courtApplication.get().getApplicationStatus())) {
                            LOGGER.info("Case id '{}' has been updated with case status of '{}'", aCaseId.get(), ACTIVE.getDescription());
                            sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS)
                                    .apply(createObjectBuilder()
                                            .add(PROSECUTION_CASE, objectToJsonObjectConverter.convert(aCase1))
                                            .add(CASE_STATUS, ACTIVE.getDescription())
                                            .build()));
                        }
                    });
                }
            }

        });

    }

    private Optional<ProsecutionCase> getProsecutionCase(final Hearing hearing, final Optional<UUID> aCaseId) {
        if (isNotEmpty(hearing.getProsecutionCases())) {
            return hearing.getProsecutionCases()
                    .stream()
                    .filter(prosecutionCase -> prosecutionCase.getId().equals(aCaseId.get()))
                    .findFirst();
        }
        return Optional.empty();
    }

    public void prepareSummonsDataForExtendHearing(JsonEnvelope jsonEnvelope, HearingConfirmed hearingConfirmed) {
        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing =
                PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing()
                        .withConfirmedHearing(hearingConfirmed.getConfirmedHearing())
                        .build();

        final JsonObject prepareSummonsDataJsonObject = objectToJsonObjectConverter.convert(prepareSummonsDataForExtendedHearing);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, "progression.command.prepare-summons-data-for-extended-hearing")
                        .apply(prepareSummonsDataJsonObject);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    public void updateHearingForPartialAllocation(JsonEnvelope jsonEnvelope, UpdateHearingForPartialAllocation updateHearingForPartialAllocation){

        final JsonEnvelope updateHearingForPartialAllocationEnvelope = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION)
                .apply(updateHearingForPartialAllocation);

        sender.send(updateHearingForPartialAllocationEnvelope);
    }

    private Optional<CourtApplication> getCourtApplication(final Hearing hearing, final UUID applicationId) {
        return hearing.getCourtApplications()
                .stream()
                .filter(courtApplication -> courtApplication.getId().equals(applicationId))
                .findFirst();
    }
}
