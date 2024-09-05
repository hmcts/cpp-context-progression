package uk.gov.moj.cpp.progression.service;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.core.courts.ConfirmedProsecutionCaseId.confirmedProsecutionCaseId;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.FeeType.CONTESTED;
import static uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxworkApplicationReferred;
import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.ConfirmedProsecutionCaseId;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.DefendantsWithWelshTranslation;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.InitiateApplicationForCaseRequested;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.PrepareSummonsData;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.progression.courts.StoreBookingReferenceCourtScheduleIds;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.Country;
import uk.gov.moj.cpp.progression.domain.utils.LocalDateUtils;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.processor.exceptions.CourtApplicationAndCaseNotFoundException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:CommentedOutCodeLine", "squid:S1188", "squid:S2789", "squid:S3655", "squid:S1192", "squid:S1168", "pmd:NullAssignment", "squid:CallToDeprecatedMethod", "squid:S1166", "squid:S2221"})
public class ProgressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressionService.class);

    private static final String APPLICATION_ID = "applicationId";
    public static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";

    public static final String HEARING_ID = "hearingId";
    public static final String HEARING_TYPE = "hearingType";
    public static final String DEFENDANT_ID = "defendantId";
    private static final String PROGRESSION_COMMAND_CREATE_PROSECUTION_CASE = "progression.command.create-prosecution-case";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_DOCUMENT = "progression.command.create-court-document";
    private static final String PROGRESSION_QUERY_SEARCH_CASES = "progression.query.search-cases";
    private static final String PROGRESSION_QUERY_PUBLIC_LIST_OPA_NOTICES = "progression.query.public-list-opa-notices";
    private static final String PROGRESSION_QUERY_PRESS_LIST_OPA_NOTICES = "progression.query.press-list-opa-notices";
    private static final String PROGRESSION_QUERY_RESULT_LIST_OPA_NOTICES = "progression.query.result-list-opa-notices";
    private static final String PROGRESSION_QUERY_SEARCH_CASES_BY_CASEURN = "progression.query.search-cases-by-caseurn";
    private static final String PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN = "progression.query.case-exist-by-caseurn";
    private static final String PROGRESSION_QUERY_PROSECUTION_CASES = "progression.query.prosecutioncase";
    private static final String PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION = "progression.query.case.status-for-application";
    private static final String PROGRESSION_QUERY_HEARING = "progression.query.hearing";
    private static final String PROGRESSION_QUERY_APPLICATIONS = "progression.query.application.aaag";
    private static final String PROGRESSION_QUERY_LINKED_CASES = "progression.query.case-lsm-info";
    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND = "progression.command.update-defendant-listing-status";
    private static final String PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND_V3 = "progression.command.update-defendant-listing-status-v3";
    private static final String PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND = "progression.command.create-hearing-for-application";
    private static final String PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING = "progression.command.record-unscheduled-hearing";
    private static final String PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND = "progression.command.list-unscheduled-hearing";
    private static final String PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA = "progression.command.prepare-summons-data";
    private static final String PUBLIC_EVENT_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String HEARING_LISTING_STATUS = "hearingListingStatus";
    private static final String UNSCHEDULED = "isUnscheduled";
    private static final String HEARING = "hearing";
    private static final String LIST_NEXT_HEARINGS = "listNextHearings";
    private static final String LIST_HEARING_REQUESTS = "listHearingRequests";
    private static final String HEARING_INITIALISED = "HEARING_INITIALISED";
    private static final String SENT_FOR_LISTING = "SENT_FOR_LISTING";
    private static final String EMPTY_STRING = "";
    private static final String PROGRESSION_QUERY_COURT_APPLICATION = "progression.query.application";
    private static final String PROGRESSION_QUERY_CASE = "progression.query.case";
    private static final String PROGRESSION_QUERY_COURT_APPLICATION_ONLY = "progression.query.application-only";
    private static final String PROGRESSION_COMMAND_UPDATE_COURT_APPLICATION_STATUS = "progression.command.update-court-application-status";
    private static final String PROGRESSION_COMMAND_UPDATE_DEFENDANT_AS_YOUTH = "progression.command.update-defendant-for-prosecution-case";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_PROSECUTION_CASE_LINK = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK = "progression.command.create-hearing-application-link";
    private static final String PROGRESSION_COMMAND_HEARING_RESULTED_UPDATE_CASE = "progression.command.hearing-resulted-update-case";
    private static final String PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS = "progression.command.hearing-confirmed-update-case-status";
    private static final String PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION = "progression.command.update-hearing-for-partial-allocation";
    private static final String PROGRESSION_COMMAND_UPDATE_CIVIL_FEES = "progression.command.update-civil-fees";
    public static final String CASE_STATUS = "caseStatus";
    private static final String COURT_APPLICATIONS = "courtApplications";
    public static final String UNSCHEDULED_HEARING_IDS = "unscheduledHearingIds";
    private static final String PLEA_TYPE_GUILTY_NO = "No";
    private static final String PLEA_TYPE_GUILTY_FLAG_FIELD = "pleaTypeGuiltyFlag";
    public static final String NOTIFY_NCES = "notifyNCES";
    private static final String DEFENDANT_JUDICIAL_RESULTS = "defendantJudicialResults";
    private static final String COURT_CENTRE = "courtCentre";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String REMIT_RESULT_IDS = "remitResultIds";
    private static final String IS_BOX_HEARING = "isBoxHearing";

    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String PROGRESSION_QUERY_ACTIVE_APPLICATIONS_ON_CASE = "progression.query.active-applications-on-case";

    private DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private Map<String, List<CpResultActionMapping>> remitResultIds = new ConcurrentHashMap<>(1);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ListToJsonArrayConverter<CourtApplication> listToJsonArrayConverter;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private ListingService listingService;

    @Inject
    private ListToJsonArrayConverter<DefendantJudicialResult> resultListToJsonArrayConverter;

    @Inject
    private ListToJsonArrayConverter<ListHearingRequest> hearingRequestListToJsonArrayConverter;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;


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

    private Hearing transformHearingListingNeeds(final HearingListingNeeds hearingListingNeeds, final SeedingHearing seedingHearing, final Boolean isGroupProceedings, final Integer numberOfGroupCases) {
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
                .withSeedingHearing(seedingHearing)
                .withCourtApplications(removeJudicialResults(hearingListingNeeds.getCourtApplications()))
                .withNumberOfGroupCases(numberOfGroupCases)
                .withIsGroupProceedings(isGroupProceedings)
                .build();
    }

    private List<CourtApplication> removeJudicialResults(final List<CourtApplication> courtApplications) {
        if (courtApplications == null) {
            return null;
        }
        return courtApplications.stream()
                .map(courtApplication -> CourtApplication.courtApplication().withValuesFrom(courtApplication)
                        .withJudicialResults(null)
                        .withCourtOrder(ofNullable(courtApplication.getCourtOrder())
                                .map(courtOrder -> CourtOrder.courtOrder()
                                        .withValuesFrom(courtOrder)
                                        .withCourtOrderOffences(ofNullable(courtOrder.getCourtOrderOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                                .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence().withValuesFrom(courtOrderOffence)
                                                        .withOffence(Offence.offence().withValuesFrom(courtOrderOffence.getOffence())
                                                                .withJudicialResults(null)
                                                                .build())
                                                        .build())
                                                .collect(toList()))
                                        .build())
                                .orElse(null))
                        .withCourtApplicationCases(nonNull(courtApplication.getCourtApplicationCases()) ? courtApplication.getCourtApplicationCases().stream()
                                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                                        .withValuesFrom(courtApplicationCase)
                                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                                .map(courtApplicationOffence -> Offence.offence().withValuesFrom(courtApplicationOffence).withJudicialResults(null).build())
                                                .collect(collectingAndThen(toList(), getListOrNull())))
                                        .build())
                                .collect(collectingAndThen(toList(), getListOrNull())) : null)
                        .build())
                .collect(toList());
    }

    private static List<HearingDay> populateHearingDays(final ZonedDateTime earliestStartDateTime, final Integer
            getEstimatedMinutes) {

        List<HearingDay> days = null;

        if (nonNull(earliestStartDateTime) && nonNull(getEstimatedMinutes)) {
            days = new ArrayList<>();
            days.add(HearingDay.hearingDay().withListedDurationMinutes(getEstimatedMinutes).withSittingDay(earliestStartDateTime).build());
        }
        return days;
    }

    /**
     * Convert ConfirmedDefendant to Defendant, sync with prosecution case (from DB) defendant and update offence to remove plea if,
     * <p>
     * 1. Moving from magistrate to crown court and,
     * 2. Has no verdict and,
     * 3. Plea type guilty flag is 'No'
     *
     * @param confirmedProsecutionCase The prosecution case from the payload
     * @param prosecutionCaseEntity    The prosecution case from progression view store
     * @param earliestHearingDate      The earliest hearing date
     * @param seedingHearing           The seeding hearing if any
     * @return The list of filter defendants and the offences
     */
    private List<Defendant> filterDefendantsAndUpdateOffences(final ConfirmedProsecutionCase confirmedProsecutionCase, final ProsecutionCase prosecutionCaseEntity,
                                                              final LocalDate earliestHearingDate, final SeedingHearing seedingHearing) {

        final List<Defendant> defendantsList = new ArrayList<>();

        confirmedProsecutionCase.getDefendants().forEach(confirmedDefendantConsumer -> {

            final Optional<Defendant> matchedDefendant = prosecutionCaseEntity.getDefendants().stream()
                    .filter(pcd -> pcd.getId().equals(confirmedDefendantConsumer.getId()))
                    .findFirst();
            matchedDefendant.ifPresent(defendantEntity -> {
                final List<Offence> matchedDefendantOffence = defendantEntity.getOffences().stream()
                        .filter(offence -> confirmedDefendantConsumer.getOffences().stream()
                                .anyMatch(o -> o.getId().equals(offence.getId())))
                        .map(offence -> Offence.offence().withValuesFrom(offence).withListingNumber(ofNullable(offence.getListingNumber()).orElse(0) + 1).build())
                        .collect(Collectors.toList());

                final List<Offence> matchedDefendantAndPleaRemovedOffences = new ArrayList<>();

                matchedDefendantOffence.forEach(offence -> {
                    if (isNull(offence.getJudicialResults()) || isNull(seedingHearing) || isNull(offence.getPlea()) || nonNull(offence.getVerdict())) {
                        matchedDefendantAndPleaRemovedOffences.add(offence);
                    } else {
                        matchedDefendantAndPleaRemovedOffences.add(populateOffenceBasedOnPleaGuiltyType(offence, seedingHearing));
                    }
                });
                if(isNotEmpty(matchedDefendantAndPleaRemovedOffences)){
                    defendantsList.add(populateDefendant(defendantEntity, matchedDefendantAndPleaRemovedOffences, earliestHearingDate));
                }
            });
        });

        return defendantsList;
    }

    /**
     * This method returns an Offence with plea information removed if
     * <p>
     * 1. Moving from magistrate to crown court and, 2. Has no verdict and, 3. Plea type guilty flag
     * is 'No'
     *
     * @param offence
     * @param seedingHearing
     * @return
     */
    private Offence populateOffenceBasedOnPleaGuiltyType(final Offence offence, final SeedingHearing seedingHearing) {

        LOGGER.info("Populate offence based on plea type guilty flag: {}", offence.getId());

        // Find one judicial result with next hearing and jurisdiction type CROWN
        final Optional<JudicialResult> judicialResultWithNextHearingInCrownCourt = offence.getJudicialResults()
                .stream()
                .filter(judicialResult -> nonNull(judicialResult.getNextHearing()))
                .filter(judicialResult -> judicialResult.getNextHearing().getJurisdictionType() == JurisdictionType.CROWN)
                .findAny();

        if (!judicialResultWithNextHearingInCrownCourt.isPresent()) {
            return offence;
        }

        final String pleaTypeValue = offence.getPlea().getPleaValue();
        final Optional<JsonObject> pleaType = referenceDataService.getPleaType(pleaTypeValue, requester);

        if (!pleaType.isPresent()) {
            return offence;
        }

        final String pleaTypeGuiltyFlag = pleaType.get().getString(PLEA_TYPE_GUILTY_FLAG_FIELD);

        final boolean pleaToBeRemoved = PLEA_TYPE_GUILTY_NO.equals(pleaTypeGuiltyFlag) &&
                nonNull(seedingHearing.getJurisdictionType()) &&
                seedingHearing.getJurisdictionType() == JurisdictionType.MAGISTRATES;

        if (pleaToBeRemoved) {
            return Offence.offence()
                    .withValuesFrom(offence)
                    .withPlea(null)
                    .build();
        } else {
            return offence;
        }
    }

    public void updateListingNumber(final JsonEnvelope jsonEnvelope, final ProsecutionCase prosecutionCase) {
        final JsonArrayBuilder offenceListingNumbersBuilder = Json.createArrayBuilder();
        prosecutionCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .forEach(offence -> offenceListingNumbersBuilder.add(createObjectBuilder()
                        .add("offenceId", offence.getId().toString())
                        .add("listingNumber", offence.getListingNumber())));

        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCase.getId().toString())
                .add("offenceListingNumbers", offenceListingNumbersBuilder.build());

        sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName("progression.command.update-listing-number-to-prosecution-case"),
                updateCommandBuilder.build()));
    }

    public void increaseListingNumber(final JsonEnvelope jsonEnvelope, final ProsecutionCase prosecutionCase, final UUID hearingId) {
        final JsonArrayBuilder offenceListingNumbersBuilder = Json.createArrayBuilder();
        prosecutionCase.getDefendants().stream()
                .flatMap(defendant -> defendant.getOffences().stream())
                .forEach(offence -> offenceListingNumbersBuilder.add(offence.getId().toString()));

        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCase.getId().toString())
                .add("hearingId", hearingId.toString())
                .add("offenceIds", offenceListingNumbersBuilder.build());

        sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName("progression.command.increase-listing-number-to-prosecution-case"),
                updateCommandBuilder.build()));
    }

    public void sendListingCommandToDeleteHearing(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final JsonObject deleteHearingPayload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        sender.sendAsAdmin(
                Envelope.envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName("listing.command.delete-hearing"),
                        deleteHearingPayload
                )
        );
    }

    private static Defendant populateDefendant(final Defendant matchedDefendant, final List<Offence>
            matchedDefendantOffence, final LocalDate earliestHearingDate) {
        final Defendant.Builder builder = Defendant.defendant()
                .withId(matchedDefendant.getId())
                .withMasterDefendantId(matchedDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(matchedDefendant.getCourtProceedingsInitiated())
                .withOffences(matchedDefendantOffence.stream().map(offence -> Offence.offence().withValuesFrom(offence).withJudicialResults(null).build()).collect(toList()))
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
                .withDefendantCaseJudicialResults(matchedDefendant.getDefendantCaseJudicialResults());
        if (nonNull(matchedDefendant.getPersonDefendant()) &&
                nonNull(matchedDefendant.getPersonDefendant().getPersonDetails()) &&
                nonNull(matchedDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth())) {

            builder.withIsYouth(LocalDateUtils.isYouth(matchedDefendant.getPersonDefendant().getPersonDetails().getDateOfBirth(), earliestHearingDate));
        }
        return builder.build();
    }

    public static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
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
                .add(COURT_CENTRE, objectToJsonObjectConverter.convert(confirmedHearing.getCourtCentre()))
                .add("hearingDateTime", ZonedDateTimes.toString(getEarliestDate(confirmedHearing.getHearingDays())))
                .add("confirmedProsecutionCaseIds", transformProsecutionCases(confirmedHearing.getProsecutionCases()))
                .add("confirmedApplicationIds", transformApplicationIds(confirmedHearing))
                .build();
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA).apply(casesConfirmedPayload));
    }

    public void prepareSummonsDataForAddedDefendant(final Envelope<PublicListingNewDefendantAddedForCourtProceedings> envelope) {
        final PublicListingNewDefendantAddedForCourtProceedings defendantAddedEvent = envelope.payload();
        final ConfirmedProsecutionCaseId confirmedProsecutionCaseId = confirmedProsecutionCaseId().withId(defendantAddedEvent.getCaseId()).withConfirmedDefendantIds(singletonList(defendantAddedEvent.getDefendantId())).build();

        final PrepareSummonsData prepareSummonsData = PrepareSummonsData.prepareSummonsData()
                .withCourtCentre(defendantAddedEvent.getCourtCentre())
                .withHearingDateTime(defendantAddedEvent.getHearingDateTime())
                .withConfirmedProsecutionCaseIds(singletonList(confirmedProsecutionCaseId))
                .withHearingId(defendantAddedEvent.getHearingId())
                .build();


        final Metadata metadata = metadataFrom(envelope.metadata()).withName(PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA).build();
        sender.send(envelopeFrom(metadata, prepareSummonsData));
    }

    private JsonArray transformApplicationIds(final ConfirmedHearing confirmedHearing) {
        if (isEmpty(confirmedHearing.getCourtApplicationIds())) {
            return createArrayBuilder().build();
        }

        return confirmedHearing.getCourtApplicationIds().stream().map(UUID::toString).collect(Json::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add).build();
    }

    public void updateDefendantYouthForProsecutionCase(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate, final List<ProsecutionCase> deltaProsecutionCases) {
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

                        if (matchedDefendant.isPresent() && isMatchedDefendantsAllOffenceNotInDelta(matchedDefendant.get(), deltaProsecutionCases)) {
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

    /**
     * Checks any defendant's->offences not exists in delta cases regarding partial allocation
     *
     * @param defendant
     * @param deltaProsecutionCases
     * @return
     */
    private boolean isMatchedDefendantsAllOffenceNotInDelta(final Defendant defendant, final List<ProsecutionCase> deltaProsecutionCases) {
        if (deltaProsecutionCases.isEmpty()) {
            return true;
        }

        final List<UUID> deltaOffenceIds = deltaProsecutionCases.stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(Collection::stream)
                .filter(d -> defendant.getId().equals(d.getId()))
                .map(Defendant::getOffences)
                .flatMap(Collection::stream)
                .map(Offence::getId)
                .collect(Collectors.toList());

        final List<UUID> offenceIds = defendant.getOffences().stream()
                .map(Offence::getId)
                .collect(Collectors.toList());

        return !CollectionUtils.isEqualCollection(deltaOffenceIds, offenceIds);
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
            return !confirmedDefendantConsumer.getIsYouth().equals(ofNullable(defEnt.getIsYouth()).orElse(Boolean.FALSE));
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
                .withJudicialResults(defendantEntity.getDefendantCaseJudicialResults())
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

    public Optional<JsonObject> getPublicListNotices(final JsonEnvelope envelope) {
        LOGGER.info("get public list notices {} ", LocalDate.now());

        final JsonEnvelope response = requester.request(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_QUERY_PUBLIC_LIST_OPA_NOTICES).build(), createObjectBuilder().build()));

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getPressListNotices(final JsonEnvelope envelope) {
        LOGGER.info("get press list notices {} ", LocalDate.now());

        final JsonEnvelope response = requester.request(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_QUERY_PRESS_LIST_OPA_NOTICES).build(), createObjectBuilder().build()));

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> getResultListNotices(final JsonEnvelope envelope) {
        LOGGER.info("get result list notices {}", LocalDate.now());

        final JsonEnvelope response = requester.request(envelopeFrom(metadataFrom(envelope.metadata()).withName(PROGRESSION_QUERY_RESULT_LIST_OPA_NOTICES).build(), createObjectBuilder().build()));

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> searchCaseDetailByURN(final JsonEnvelope envelope, final String reference) {

        final JsonObject requestParameter = createObjectBuilder().add("caseUrn", reference).build();

        LOGGER.info("search for case detail with reference {} ", reference);

        final JsonEnvelope response = requester.request(enveloper.withMetadataFrom(envelope, PROGRESSION_QUERY_SEARCH_CASES_BY_CASEURN).apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("search for case detail response {}", response.toObfuscatedDebugString());
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public Optional<JsonObject> caseExistsByCaseUrn(final JsonEnvelope envelope, final String reference) {

        final JsonObject requestParameter = createObjectBuilder().add("caseUrn", reference).build();

        LOGGER.info("search for case detail with reference {} ", reference);

        final JsonEnvelope response = requester.request(enveloper.withMetadataFrom(envelope, PROGRESSION_QUERY_CASE_EXISTS_BY_CASEURN).apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("search for case detail response {}", response.toObfuscatedDebugString());
        }

        return Optional.of(response.payloadAsJsonObject());
    }

    public JsonObject getReferralReasonByReferralReasonId(final JsonEnvelope jsonEnvelope, final UUID referralId) {

        LOGGER.info("search for Referral reason detail with reference {} ", referralId);

        return referenceDataService.getReferralReasonByReferralReasonId(jsonEnvelope, referralId, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Referral Reason ", referralId.toString()));
    }


    public Country getCountryByPostcode(final String postCode, final JsonEnvelope envelope) {
        final String upperCasePostCodeWithoutWhiteSpaces = postCode.replaceAll("\\s+", "").toUpperCase();
        final JsonObject payload = createObjectBuilder().add("postCode", upperCasePostCodeWithoutWhiteSpaces).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(envelope, "referencedata.query.country-by-postcode").apply(payload);
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return Country.getCountryByName(response.payloadAsJsonObject().getString("country"));

    }

    public Optional<JsonObject> getActiveApplicationsOnCase(final JsonEnvelope envelope, final String caseId){
        Optional<JsonObject> result = Optional.empty();
        final JsonObject payload = Json.createObjectBuilder().add(PROSECUTION_CASE_ID, caseId).build();
        final JsonEnvelope activeLinkedApplications = requester.request(enveloper.withMetadataFrom(envelope, PROGRESSION_QUERY_ACTIVE_APPLICATIONS_ON_CASE).apply(payload));
        if (!activeLinkedApplications.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(activeLinkedApplications.payloadAsJsonObject());
        }
        return result;
    }

    public Optional<JsonObject> getProsecutionCaseDetailById(final JsonEnvelope envelope, final String caseId) {
        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} ,   get prosecution case detail request {}", caseId, requestParameter);

        try {
            final JsonEnvelope prosecutioncase = requester.requestAsAdmin(enveloper
                    .withMetadataFrom(envelope, PROGRESSION_QUERY_PROSECUTION_CASES)
                    .apply(requestParameter));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutioncase.toObfuscatedDebugString());
            }

            if (!prosecutioncase.payloadAsJsonObject().isEmpty()) {
                result = Optional.of(prosecutioncase.payloadAsJsonObject());
            }
        } catch (final Exception e) {

            LOGGER.debug(String.format("Prosecution case detail not found for case id : %s", caseId), e.getCause());

            throw new CourtApplicationAndCaseNotFoundException(String.format("Prosecution case detail not found for case id : %s", caseId));

        }
        return result;
    }

    public Optional<JsonObject> getCaseStatusForApplicationId(final JsonEnvelope envelope, final String applicationId, final String caseId) {
        Optional<JsonObject> result = Optional.empty();

        final JsonObject requestParameter = createObjectBuilder()
                .add(APPLICATION_ID, applicationId)
                .add(CASE_ID, caseId)
                .build();
        LOGGER.info("applicationId {} , caseId{}   get case status for Application Id request {}", applicationId, caseId, requestParameter);
        try {
            final JsonEnvelope prosecutioncase = requester.requestAsAdmin(enveloper
                    .withMetadataFrom(envelope, PROGRESSION_QUERY_CASE_STATUS_FOR_APPLICATION)
                    .apply(requestParameter));

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("applicationId {} case status for applicationid payload {}", applicationId, prosecutioncase.toObfuscatedDebugString());
            }

            if (!prosecutioncase.payloadAsJsonObject().isEmpty()) {
                result = Optional.of(prosecutioncase.payloadAsJsonObject());
            }
        } catch (final Exception e) {

            LOGGER.debug(String.format("Case status not found for Application id : %s", applicationId), e.getCause());

            throw new CourtApplicationAndCaseNotFoundException(String.format("Case status not found for applicationId id : %s", applicationId));

        }
        return result;
    }

    public JsonObject getProsecutionCaseById(final JsonEnvelope envelope, final String caseId) {
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, caseId)
                .build();

        LOGGER.info("caseId {} ,   get prosecution case detail request {}", caseId, requestParameter);

        final JsonEnvelope prosecutionCase = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_PROSECUTION_CASES)
                .apply(requestParameter));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("caseId {} prosecution case detail payload {}", caseId, prosecutionCase.toObfuscatedDebugString());
        }

        if (isNull(prosecutionCase) || isNull(prosecutionCase.payloadAsJsonObject()) || isNull(prosecutionCase.payloadAsJsonObject().get("prosecutionCase"))) {
            throw new CourtApplicationAndCaseNotFoundException(String.format("Prosecution case detail not found for case id : %s", caseId));
        }
        return prosecutionCase.payloadAsJsonObject();
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

    public void updateHearingListingStatusToHearingInitiated(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate) {
        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                .add(HEARING_LISTING_STATUS, HEARING_INITIALISED)
                .add(HEARING, objectToJsonObjectConverter.convert(hearingInitiate.getHearing()))
                .build();
        LOGGER.info("update hearing listing status after initiate hearing with payload {}", hearingListingStatusCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
    }

    /**
     * @param jsonEnvelope
     * @param hearings       - the hearings to update the status for
     * @param seedingHearing - The originating hearing details
     */
    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final List<HearingListingNeeds> hearings, final SeedingHearing seedingHearing, final List<ListHearingRequest> listHearingRequests, final Boolean isGroupProceedings, final Integer numberOfGroupCases) {
        hearings.forEach(hearingListingNeeds -> {
            final Hearing hearing = transformHearingListingNeeds(hearingListingNeeds, seedingHearing, isGroupProceedings, numberOfGroupCases);
            if (isNotEmpty(hearing.getProsecutionCases())) {
                final JsonObjectBuilder hearingListingStatusCommandBuilder = Json.createObjectBuilder()
                        .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                        .add(HEARING, objectToJsonObjectConverter.convert(hearing));
                if (isNotEmpty(listHearingRequests)) {
                    hearingListingStatusCommandBuilder.add(LIST_HEARING_REQUESTS, hearingRequestListToJsonArrayConverter.convert(listHearingRequests));
                }

                final JsonObject hearingListingStatusCommand = hearingListingStatusCommandBuilder.build();
                LOGGER.info("update hearing listing status after send case for listing with payload {}", hearingListingStatusCommand);
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
            } else {

                final JsonObjectBuilder hearingCreatedForApplicationCommandBuilder = Json.createObjectBuilder()
                        .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                        .add(HEARING, objectToJsonObjectConverter.convert(hearing));

                if (isNotEmpty(listHearingRequests)) {
                    hearingCreatedForApplicationCommandBuilder.add(LIST_HEARING_REQUESTS, hearingRequestListToJsonArrayConverter.convert(listHearingRequests));
                }

                final JsonObject hearingCreatedForApplicationCommand = hearingCreatedForApplicationCommandBuilder.build();

                LOGGER.info("create hearing listing status after send application for listing with payload {}", hearingCreatedForApplicationCommand);

                sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND),
                        hearingCreatedForApplicationCommand));

            }
        });
    }

    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final ListNextHearingsV3 listNextHearings) {
        final SeedingHearing seedingHearing = listNextHearings.getSeedingHearing();
        listNextHearings.getHearings().forEach(hearingListingNeeds -> {
            final Hearing hearing = transformHearingListingNeeds(hearingListingNeeds, seedingHearing, false, null);

            if (isNotEmpty(hearing.getProsecutionCases())) {
                final JsonObjectBuilder hearingListingStatusCommandBuilder = Json.createObjectBuilder()
                        .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                        .add(HEARING, objectToJsonObjectConverter.convert(hearing));

                final ListNextHearingsV3 listNextHearingsWithOneHearingListingNeeds = getListNextHearings(seedingHearing, listNextHearings.getShadowListedOffences(), hearingListingNeeds);
                if(nonNull(listNextHearingsWithOneHearingListingNeeds)){
                    LOGGER.info("A next hearing payload: {}", objectToJsonValueConverter.convert(listNextHearingsWithOneHearingListingNeeds));
                    hearingListingStatusCommandBuilder.add(LIST_NEXT_HEARINGS, objectToJsonObjectConverter.convert(listNextHearingsWithOneHearingListingNeeds));
                } else {
                    LOGGER.error("Next hearing without hearing not possible");
                    throw new DataValidationException("Next hearing without hearing not possible");
                }

                final JsonObject hearingListingStatusCommand = hearingListingStatusCommandBuilder.build();
                LOGGER.info("update hearing listing status after send case for listing payload with listNextHearings-2 {}", objectToJsonValueConverter.convert(hearingListingStatusCommand));
                try {
                    jsonSchemaValidator.validate(hearingListingStatusCommand.toString(), PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND_V3);
                } catch (JsonSchemaValidationException e) {
                    throw new BadRequestException("Error update hearing listing status after send case for listing, request has schema violations", e);
                }

                sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND_V3),
                        hearingListingStatusCommand));
            } else {
                final JsonObjectBuilder hearingCreatedForApplicationCommandBuilder = Json.createObjectBuilder()
                        .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                        .add(HEARING, objectToJsonObjectConverter.convert(hearing));

                final JsonObject hearingCreatedForApplicationCommand = hearingCreatedForApplicationCommandBuilder.build();

                LOGGER.info("create hearing listing status after send application for listing with payload {}", hearingCreatedForApplicationCommand);

                sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_CREATE_HEARING_FOR_APPLICATION_COMMAND),
                        hearingCreatedForApplicationCommand));
                listingService.listNextCourtHearings(jsonEnvelope, listNextHearings);
            }
        });
    }

    private ListNextHearingsV3 getListNextHearings(final SeedingHearing seedingHearing, final List<UUID> shadowListedOffences, final HearingListingNeeds hearingListingNeeds) {
        if(nonNull(seedingHearing)){
            return ListNextHearingsV3.listNextHearingsV3()
                    .withHearingId(seedingHearing.getSeedingHearingId())
                    .withSeedingHearing(seedingHearing)
                    .withAdjournedFromDate(LocalDate.now())
                    .withHearings(Arrays.asList(hearingListingNeeds))
                    .withShadowListedOffences(shadowListedOffences)
                    .build();
        }
        return null;
    }

    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final List<HearingListingNeeds> hearings, final SeedingHearing seedingHearing) {
        updateHearingListingStatusToSentForListing(jsonEnvelope, hearings, seedingHearing, null, false, null);
    }

    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing, final List<ListHearingRequest> listHearingRequests) {
        updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing.getHearings(), null, listHearingRequests, false, null);
    }

    public void updateHearingListingStatusToSentForListing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing) {
        updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing.getHearings(), null);
    }

    public void listUnscheduledHearings(final JsonEnvelope jsonEnvelope, final Hearing hearing) {
        final JsonObject payload = Json.createObjectBuilder()
                .add(HEARING, objectToJsonObjectConverter.convert(hearing))
                .build();

        sender.send(Enveloper.envelop(payload).withName(PROGRESSION_LIST_UNSCHEDULED_HEARING_COMMAND).withMetadataFrom(jsonEnvelope));
    }

    public void sendUpdateDefendantListingStatusForUnscheduledListing(final JsonEnvelope jsonEnvelope, final List<Hearing> unscheduledHearings, final Set<UUID> hearingsToBeSentNotification) {
        unscheduledHearings.forEach(unscheduledHearing -> {
            final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                    .add(UNSCHEDULED, true)
                    .add(NOTIFY_NCES, hearingsToBeSentNotification.contains(unscheduledHearing.getId()))
                    .add(HEARING_LISTING_STATUS, SENT_FOR_LISTING)
                    .add(HEARING, objectToJsonObjectConverter.convert(unscheduledHearing))
                    .build();
            sender.send(Enveloper.envelop(hearingListingStatusCommand).withName(PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).withMetadataFrom(jsonEnvelope));
        });
    }

    public void recordUnlistedHearing(final JsonEnvelope jsonEnvelope, final UUID originalHearingId, final List<Hearing> newHearingIds) {
        final JsonArrayBuilder newHearingIdArrays = createArrayBuilder();
        newHearingIds.stream().forEach(s -> newHearingIdArrays.add(s.getId().toString()));


        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                .add(HEARING_ID, originalHearingId.toString())
                .add(UNSCHEDULED_HEARING_IDS, newHearingIdArrays.build())
                .build();
        sender.send(envelop(hearingListingStatusCommand).withName(PROGRESSION_COMMAND_RECORD_UNSCHEDULED_HEARING).withMetadataFrom(jsonEnvelope));
    }

    public void updateHearingListingStatusToHearingUpdate(final JsonEnvelope jsonEnvelope, final Hearing hearing) {
        final JsonObject hearingListingStatusCommand = Json.createObjectBuilder()
                .add(HEARING_LISTING_STATUS, "HEARING_INITIALISED")
                .add(HEARING, objectToJsonObjectConverter.convert(hearing))
                .build();
        LOGGER.info("update hearing listing status after initiate hearing with payload {}", hearingListingStatusCommand);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_UPDATE_DEFENDANT_LISTING_STATUS_COMMAND).apply(hearingListingStatusCommand));
    }

    public void publishHearingDetailChangedPublicEvent(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing) {
        final JsonObject hearingDetailChangedPayload = Json.createObjectBuilder()
                .add(HEARING, objectToJsonObjectConverter.convert(transformUpdatedHearing(confirmedHearing, jsonEnvelope)))
                .build();
        LOGGER.info("publish public hearing details changed event with payload {}", hearingDetailChangedPayload);
        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_EVENT_HEARING_DETAIL_CHANGED).apply(hearingDetailChangedPayload));
    }

    public Optional<CourtApplication> getCourtApplicationByIdTyped(final JsonEnvelope envelope, final String courtApplicationId) {
        final Optional<JsonObject> jsonObject = getCourtApplicationById(envelope, courtApplicationId);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("getCourtApplicationByIdTyped courtApplication: %s payload: %s", courtApplicationId, jsonObject.toString()));
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


    public Optional<JsonObject> getProsecutionCase(final JsonEnvelope envelope, final String prosecutionCaseId) {

        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(CASE_ID, prosecutionCaseId)
                .build();

        LOGGER.info("caseId {} ,   get prosceutionCase {}", prosecutionCaseId, requestParameter);

        final JsonEnvelope prosecutionCase = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_CASE)
                .apply(requestParameter));


        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("case {} ,   get court payLoad {}", prosecutionCaseId, prosecutionCase.payloadAsJsonObject());
        }

        if (!prosecutionCase.payloadAsJsonObject().isEmpty()) {
            result = Optional.of(prosecutionCase.payloadAsJsonObject());
        }
        return result;
    }

    /**
     * Retrieves the court application by id. But only retrieves the {@link CourtApplication}, not
     * the additional information that {@link #getCourtApplicationById(JsonEnvelope, String)}
     * returns.
     *
     * @param envelope           - the requesting envelope
     * @param courtApplicationId - the id of the application to retrieve
     * @return the court application for the id provided.
     */
    public Optional<JsonObject> getCourtApplicationOnlyById(final JsonEnvelope envelope, final String courtApplicationId) {

        Optional<JsonObject> result = Optional.empty();
        final JsonObject requestParameter = createObjectBuilder()
                .add(APPLICATION_ID, courtApplicationId)
                .build();

        LOGGER.info("courtApplicationId {} ,   get court application {}", courtApplicationId, requestParameter);

        final JsonEnvelope courtApplication = requester.requestAsAdmin(enveloper
                .withMetadataFrom(envelope, PROGRESSION_QUERY_COURT_APPLICATION_ONLY)
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

    public Optional<JsonObject> getApplication(final JsonEnvelope envelope, final String applicationId) {
        final JsonObject requestParameter = createObjectBuilder()
                .add("applicationId", applicationId)
                .build();
        final Envelope<JsonObject> requestEnvelop = envelop(requestParameter)
                .withName(PROGRESSION_QUERY_APPLICATIONS)
                .withMetadataFrom(envelope);
        final Envelope<JsonObject> jsonObjectEnvelope = requester.request(requestEnvelop, JsonObject.class);
        return Optional.of(jsonObjectEnvelope.payload());
    }

    public Hearing retrieveHearing(final JsonEnvelope event, final UUID hearingId) {
        final Optional<JsonObject> hearingPayloadOptional = getHearing(event, hearingId.toString());
        if (hearingPayloadOptional.isPresent()) {
            return jsonObjectConverter.convert(hearingPayloadOptional.get().getJsonObject("hearing"), Hearing.class);
        }
        throw new IllegalStateException("Hearing not found for hearingId:" + hearingId);
    }

    public JsonObject retrieveApplication(final JsonEnvelope event, final UUID applicationId) {
        final Optional<JsonObject> applicationPayload = getApplication(event, applicationId.toString());
        if (applicationPayload.isPresent()) {
            return applicationPayload.get();
        }
        throw new IllegalStateException("Application not found for applicationId:" + applicationId);
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

    /**
     * Transform ConfirmedHearing to Hearing
     *
     * @param confirmedHearing
     * @param jsonEnvelope
     * @return
     */
    public Hearing transformConfirmedHearing(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope) {
        return transformConfirmedHearing(confirmedHearing, jsonEnvelope, null);
    }

    /**
     * Transform ConfirmedHearing to Hearing
     *
     * @param confirmedHearing
     * @param jsonEnvelope
     * @param seedingHearing
     * @return
     */
    public Hearing transformConfirmedHearing(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope, final SeedingHearing seedingHearing) {

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
                .withProsecutionCases(transformProsecutionCase(confirmedHearing.getProsecutionCases(), earliestHearingDate, jsonEnvelope, seedingHearing))
                .withCourtApplications(extractCourtApplications(confirmedHearing, jsonEnvelope))
                .withShadowListedOffences(listingService.getShadowListedOffenceIds(jsonEnvelope, confirmedHearing.getId()))
                .withEstimatedDuration(confirmedHearing.getEstimatedDuration())
                .withIsGroupProceedings(confirmedHearing.getIsGroupProceedings())
                .build();
    }

    public Hearing updateHearingForHearingUpdated(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope, final Hearing hearing) {

        return Hearing.hearing()
                .withValuesFrom(hearing)
                .withHearingDays(confirmedHearing.getHearingDays())
                .withCourtCentre(transformCourtCentre(confirmedHearing.getCourtCentre(), jsonEnvelope))
                .withJurisdictionType(confirmedHearing.getJurisdictionType())
                .withId(confirmedHearing.getId())
                .withHearingLanguage(confirmedHearing.getHearingLanguage())
                .withJudiciary(enrichJudiciaries(confirmedHearing.getJudiciary(), jsonEnvelope))
                .withReportingRestrictionReason(confirmedHearing.getReportingRestrictionReason())
                .withType(confirmedHearing.getType())
                .withHasSharedResults(false)
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
                getCourtApplicationOnlyById(jsonEnvelope, applicationId.toString()).ifPresent(
                        application -> {
                            final CourtApplication courtApplication = jsonObjectConverter.convert(application.getJsonObject("courtApplication"), CourtApplication.class);
                            final CourtApplication.Builder builder = CourtApplication.courtApplication().withValuesFrom(courtApplication)
                                    .withJudicialResults(null)
                                    .withFutureSummonsHearing(null);
                            updateCourtApplicationCases(courtApplication, builder);
                            updateCourtOrder(courtApplication, builder);
                            applicationDetails.add(builder.build());
                        }
                );
            }
            return applicationDetails;
        }
        return null;
    }

    private void updateCourtOrder(final CourtApplication courtApplication, final CourtApplication.Builder builder) {
        if (nonNull(courtApplication.getCourtOrder())) {
            final CourtOrder courtOrder = ofNullable(courtApplication.getCourtOrder())
                    .map(order -> CourtOrder.courtOrder().withValuesFrom(order)
                            .withCourtOrderOffences(order.getCourtOrderOffences().stream()
                                    .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence()
                                            .withValuesFrom(courtOrderOffence)
                                            .withOffence(Offence.offence().withValuesFrom(courtOrderOffence.getOffence()).withJudicialResults(null).build()).build())
                                    .collect(toList()))
                            .build())
                    .orElse(null);
            builder.withCourtOrder(courtOrder);
        }
    }

    private void updateCourtApplicationCases(final CourtApplication courtApplication, final CourtApplication.Builder builder) {
        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {
            final List<CourtApplicationCase> courtApplicationCases = courtApplication.getCourtApplicationCases().stream()
                    .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                            .withValuesFrom(courtApplicationCase)
                            .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                    .map(courtApplicationOffence -> Offence.offence().withValuesFrom(courtApplicationOffence).withJudicialResults(null).build())
                                    .collect(collectingAndThen(toList(), getListOrNull())))
                            .build())
                    .collect(collectingAndThen(toList(), getListOrNull()));
            builder.withCourtApplicationCases(courtApplicationCases);
        }
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }

    public CourtCentre transformCourtCentre(final CourtCentre courtCentre, final JsonEnvelope jsonEnvelope) {

        final String ADDRESS_1 = "address1";
        final String ADDRESS_2 = "address2";
        final String ADDRESS_3 = "address3";
        final String ADDRESS_4 = "address4";
        final String ADDRESS_5 = "address5";
        final String POSTCODE = "postcode";

        final JsonObject courtCentreJson = referenceDataService.getOrganisationUnitById(courtCentre.getId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Court center ", courtCentre.getId().toString()));

        final LjaDetails ljaDetails = referenceDataService.getLjaDetails(jsonEnvelope, courtCentreJson.getString("lja"), requester);
        final String code = ofNullable(courtCentre.getRoomId())
                .map(roomId -> referenceDataService.getOuCourtRoomCode(roomId.toString(), requester).map(obj -> obj.getJsonArray("ouCourtRoomCodes")).map(codes -> codes.isEmpty() ? null : codes.getString(0)).orElse(null))
                .orElseGet(() -> courtCentreJson.getString("oucode", null));

        return courtCentre()
                .withId(courtCentre.getId())
                .withLja(ljaDetails)
                .withCode(courtCentreJson.getString("oucode", null))
                .withCourtHearingLocation(code)
                .withName(courtCentreJson.getString("oucodeL3Name"))
                .withRoomName(nonNull(courtCentre.getRoomId()) ? enrichCourtRoomName(courtCentre.getId(), courtCentre.getRoomId(), jsonEnvelope) : null)
                .withRoomId(courtCentre.getRoomId())
                .withAddress(Address.address()
                        .withAddress1(courtCentreJson.getString(ADDRESS_1))
                        .withAddress2(courtCentreJson.getString(ADDRESS_2, EMPTY_STRING))
                        .withAddress3(courtCentreJson.getString(ADDRESS_3, EMPTY_STRING))
                        .withAddress4(courtCentreJson.getString(ADDRESS_4, EMPTY_STRING))
                        .withAddress5(courtCentreJson.getString(ADDRESS_5, EMPTY_STRING))
                        .withPostcode(courtCentreJson.getString(POSTCODE, null))
                        .build())
                .build();
    }

    public CourtCentre transformCourtCentreV2(final CourtCentre courtCentre, final JsonEnvelope jsonEnvelope) {

        final String ADDRESS_1 = "address1";
        final String ADDRESS_2 = "address2";
        final String ADDRESS_3 = "address3";
        final String ADDRESS_4 = "address4";
        final String ADDRESS_5 = "address5";
        final String WELSH_ADDRESS_1 = "welshAddress1";
        final String WELSH_ADDRESS_2 = "welshAddress2";
        final String WELSH_ADDRESS_3 = "welshAddress3";
        final String WELSH_ADDRESS_4 = "welshAddress4";
        final String WELSH_ADDRESS_5 = "welshAddress5";
        final String POSTCODE = "postcode";

        final JsonObject courtCentreJson = referenceDataService.getCourtCentreWithCourtRoomsById(courtCentre.getId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Court center ", courtCentre.getId().toString()));

        final LjaDetails ljaDetails = referenceDataService.getLjaDetails(jsonEnvelope, courtCentreJson.getString("lja"), requester);
        final boolean isWelshCourtCenter = courtCentreJson.getBoolean("isWelsh", false);
        final Optional<JsonObject> courtRoomDetails = courtCentreJson.getJsonArray("courtrooms").getValuesAs(JsonObject.class).stream()
                .filter(courtroom -> nonNull(courtCentre.getRoomId()))
                .filter(courtroom -> courtCentre.getRoomId().toString().equalsIgnoreCase(courtroom.getString("id", null)))
                .findFirst();


        final CourtCentre.Builder courtCenterBuilder = courtCentre()
                .withId(courtCentre.getId())
                .withLja(ljaDetails)
                .withCode(courtCentreJson.getString("oucode", null))
                .withCourtHearingLocation(courtCentreJson.getString("courtLocationCode", null))
                .withName(courtCentreJson.getString("oucodeL3Name"))
                .withRoomName(courtRoomDetails.isPresent() ? courtRoomDetails.get().getString("courtroomName") : EMPTY_STRING)
                .withRoomId(courtCentre.getRoomId())
                .withWelshCourtCentre(isWelshCourtCenter)
                .withAddress(Address.address()
                        .withAddress1(courtCentreJson.getString(ADDRESS_1))
                        .withAddress2(courtCentreJson.getString(ADDRESS_2, EMPTY_STRING))
                        .withAddress3(courtCentreJson.getString(ADDRESS_3, EMPTY_STRING))
                        .withAddress4(courtCentreJson.getString(ADDRESS_4, EMPTY_STRING))
                        .withAddress5(courtCentreJson.getString(ADDRESS_5, EMPTY_STRING))
                        .withPostcode(courtCentreJson.getString(POSTCODE, null))
                        .build());
        if (isWelshCourtCenter) {
            courtCenterBuilder.withWelshAddress(
                    Address.address()
                            .withAddress1(courtCentreJson.getString(WELSH_ADDRESS_1))
                            .withAddress2(courtCentreJson.getString(WELSH_ADDRESS_2, EMPTY_STRING))
                            .withAddress3(courtCentreJson.getString(WELSH_ADDRESS_3, EMPTY_STRING))
                            .withAddress4(courtCentreJson.getString(WELSH_ADDRESS_4, EMPTY_STRING))
                            .withAddress5(courtCentreJson.getString(WELSH_ADDRESS_5, EMPTY_STRING))
                            .withPostcode(courtCentreJson.getString(POSTCODE, null))
                            .build())
                    .withWelshName(courtCentreJson.getString("oucodeL3WelshName"))
                    .withWelshRoomName((courtRoomDetails.isPresent() ? courtRoomDetails.get().getString("welshCourtroomName") : EMPTY_STRING));
        }

        return courtCenterBuilder.build();
    }

    private List<JudicialRole> enrichJudiciaries(final List<JudicialRole> judiciaryList, final JsonEnvelope jsonEnvelope) {
        if (isNull(judiciaryList) || judiciaryList.isEmpty()) {
            return null;
        }
        final List<UUID> judiciaryIds = judiciaryList.stream()
                .map(JudicialRole::getJudicialId)
                .collect(Collectors.toList());

        final Optional<JsonObject> optionalJudiciariesJson = referenceDataService.getJudiciariesByJudiciaryIdList(judiciaryIds, jsonEnvelope, requester);
        if (optionalJudiciariesJson.isPresent()) {
            final JsonObject judiciariesJson = optionalJudiciariesJson.get();
            return judiciaryList.stream()
                    .map(e -> mapJudiciaryRefDataToJudiciary(e, judiciariesJson))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            if(LOGGER.isErrorEnabled()) {
                LOGGER.error("Reference data not for Judiciaries with id's {}  ",
                        judiciaryIds.stream().map(UUID::toString).collect(Collectors.joining(",")));
            }
            return null;
        }

    }

    private JudicialRole mapJudiciaryRefDataToJudiciary(final JudicialRole judicialRole, final JsonObject judiciariesJson) {

        final Optional<JsonObject> optionalJudiciaryJson = judiciariesJson.getJsonArray("judiciaries").getValuesAs(JsonObject.class).stream()
                .filter(e -> judicialRole.getJudicialId().toString().equals(e.getString("id")))
                .findFirst();
        if (optionalJudiciaryJson.isPresent()) {

            final JsonObject judiciaryJson = optionalJudiciaryJson.get();
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

        } else {
            if(LOGGER.isErrorEnabled()) {
                LOGGER.error("Reference data not for Judiciary with id {}  ",
                        judicialRole.getJudicialId());
            }
            return judicialRole;
        }

    }

    private String enrichCourtRoomName(final UUID courtCentreId, final UUID courtRoomId, final JsonEnvelope jsonEnvelope) {
        final JsonObject courtRoomsJson = referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Court room ", courtCentreId.toString()));
        return getValueFromCourtRoomJson(courtRoomsJson, courtRoomId, "courtroomName");
    }

    private String getValueFromCourtRoomJson(final JsonObject courtRoomsJson, final UUID courtRoomId, final String fieldName) {
        final JsonObject matchingCourtroom = courtRoomsJson.getJsonArray("courtrooms").getValuesAs(JsonObject.class).stream()
                .filter(cr -> courtRoomId.toString().equals(cr.getString("id")))
                .findFirst().orElse(null);
        return isNull(matchingCourtroom) ? EMPTY_STRING : matchingCourtroom.getString(fieldName, EMPTY_STRING);
    }

    /**
     * Transform ConfirmedProsecutionCase to ProsecutionCase
     *
     * @param confirmedProsecutionCases
     * @param earliestHearingDate
     * @param jsonEnvelope
     * @return
     */
    public List<ProsecutionCase> transformProsecutionCase(final List<ConfirmedProsecutionCase> confirmedProsecutionCases, final LocalDate earliestHearingDate, final JsonEnvelope jsonEnvelope) {
        return transformProsecutionCase(confirmedProsecutionCases, earliestHearingDate, jsonEnvelope, null);
    }

    /**
     * Transform ConfirmedProsecutionCase to ProsecutionCase and remove Plea information from
     * Offence if, 1. Moving from magistrate to crown court and, 2. Has no verdict and, 3. Plea type
     * guilty flag is 'No'
     *
     * @param confirmedProsecutionCases
     * @param earliestHearingDate
     * @param jsonEnvelope
     * @return
     */
    public List<ProsecutionCase> transformProsecutionCase(final List<ConfirmedProsecutionCase> confirmedProsecutionCases, final LocalDate earliestHearingDate, final JsonEnvelope jsonEnvelope,
                                                          final SeedingHearing seedingHearing) {
        if (isNotEmpty(confirmedProsecutionCases)) {
            final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
            confirmedProsecutionCases.forEach(pc -> {
                final JsonObject prosecutionCaseJson = getProsecutionCaseById(jsonEnvelope, pc.getId().toString());
                if (nonNull(prosecutionCaseJson)) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectConverter.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    final ProsecutionCase prosecutionCase = prosecutionCase()
                            .withValuesFrom(prosecutionCaseEntity)
                            .withDefendants(filterDefendantsAndUpdateOffences(pc, prosecutionCaseEntity, earliestHearingDate, seedingHearing))
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
                .withCourtApplications(incomingHearing.getCourtApplications())
                .withId(existingHearingId)
                .withJurisdictionType(incomingHearing.getJurisdictionType())
                .withEstimatedMinutes(30)
                .withEstimatedDuration(incomingHearing.getEstimatedDuration())
                .withType(incomingHearing.getType())
                .build();
    }

    public Hearing transformToHearingFrom(final ConfirmedHearing confirmedHearing, final JsonEnvelope jsonEnvelope) {
        final LocalDate earliestHearingDate = getEarliestDate(confirmedHearing.getHearingDays()).toLocalDate();

        return Hearing.hearing()
                .withCourtCentre(transformCourtCentre(confirmedHearing.getCourtCentre(), jsonEnvelope))
                .withJurisdictionType(confirmedHearing.getJurisdictionType())
                .withType(confirmedHearing.getType())
                .withProsecutionCases(transformProsecutionCase(confirmedHearing.getProsecutionCases(), earliestHearingDate, jsonEnvelope, null))
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
        applicationIds.stream().collect(toSet()).forEach(applicationId ->
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                        .apply(createObjectBuilder()
                                .add(APPLICATION_ID, applicationId.toString())
                                .add(HEARING_LISTING_STATUS, status.toString())
                                .add(HEARING, hearingJson)
                                .build()))
        );
    }

    public void linkApplicationToHearing(final JsonEnvelope jsonEnvelope, final Hearing hearing, final HearingListingStatus status) {
        final JsonObject hearingJson = objectToJsonObjectConverter.convert(hearing);
        hearing.getCourtApplications().stream().distinct().forEach(application ->
                sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_LINK)
                        .apply(createObjectBuilder()
                                .add(APPLICATION_ID, application.getId().toString())
                                .add(HEARING_LISTING_STATUS, status.toString())
                                .add(HEARING, hearingJson)
                                .build()))
        );
    }

    public void updateCase(final JsonEnvelope jsonEnvelope, final ProsecutionCase prosecutionCase, final List<CourtApplication> courtApplications,
                           final List<DefendantJudicialResult> defendantJudicialResults, final CourtCentre courtCentre,
                           final UUID hearingId, final HearingType hearingType, final JurisdictionType jurisdictionType, final Boolean isBoxHearing) {

        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final JsonObject courtCentreJson = objectToJsonObjectConverter.convert(courtCentre);
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add(PROSECUTION_CASE, prosecutionCaseJson);
        if (isNotEmpty(courtApplications)) {
            payloadBuilder.add(COURT_APPLICATIONS, listToJsonArrayConverter.convert(courtApplications));
        }
        if (isNotEmpty(defendantJudicialResults)) {
            payloadBuilder.add(DEFENDANT_JUDICIAL_RESULTS, resultListToJsonArrayConverter.convert(defendantJudicialResults));
        }
        payloadBuilder.add(COURT_CENTRE, courtCentreJson);
        payloadBuilder.add(HEARING_ID, hearingId.toString());
        payloadBuilder.add(HEARING_TYPE, hearingType.getDescription());
        payloadBuilder.add(REMIT_RESULT_IDS, getRemitResultIdsAsJsonArray());
        payloadBuilder.add(JURISDICTION_TYPE, jurisdictionType.name());
        ofNullable(isBoxHearing).ifPresent(boxHearing -> payloadBuilder.add(IS_BOX_HEARING, boxHearing));

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_HEARING_RESULTED_UPDATE_CASE)
                .apply(payloadBuilder.build()));
    }

    public void updateCaseStatus(final JsonEnvelope jsonEnvelope, final Hearing hearing, final List<UUID> applicationIds) {
        applicationIds.forEach(applicationId -> {
            final Optional<CourtApplication> courtApplication = getCourtApplication(hearing, applicationId);
            courtApplication.ifPresent(application -> application.getCourtApplicationCases().forEach(courtApplicationCase -> {
                final Optional<ProsecutionCase> aCase = getProsecutionCase(hearing, Optional.of(courtApplicationCase.getProsecutionCaseId()));
                aCase.ifPresent(aCase1 -> {
                    if (INACTIVE.getDescription().equalsIgnoreCase(aCase1.getCaseStatus()) && !FINALISED.equals(application.getApplicationStatus())) {
                        LOGGER.info("Case id '{}' has been updated with case status of '{}'", aCase.get().getId(), ACTIVE.getDescription());
                        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_HEARING_CONFIRMED_UPDATE_CASE_STATUS)
                                .apply(createObjectBuilder()
                                        .add(PROSECUTION_CASE, objectToJsonObjectConverter.convert(aCase1))
                                        .add(CASE_STATUS, ACTIVE.getDescription())
                                        .build()));
                    }
                });
            }));
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

    public void prepareSummonsDataForExtendHearing(final JsonEnvelope jsonEnvelope, final HearingConfirmed hearingConfirmed) {
        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing =
                prepareSummonsDataForExtendedHearing()
                        .withConfirmedHearing(hearingConfirmed.getConfirmedHearing())
                        .build();

        final JsonObject prepareSummonsDataJsonObject = objectToJsonObjectConverter.convert(prepareSummonsDataForExtendedHearing);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, "progression.command.prepare-summons-data-for-extended-hearing")
                        .apply(prepareSummonsDataJsonObject);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    public void updateHearingForPartialAllocation(final JsonEnvelope jsonEnvelope, final UpdateHearingForPartialAllocation updateHearingForPartialAllocation) {

        final JsonEnvelope updateHearingForPartialAllocationEnvelope = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_COMMAND_UPDATE_HEARING_FOR_PARTIAL_ALLOCATION)
                .apply(updateHearingForPartialAllocation);

        sender.send(updateHearingForPartialAllocationEnvelope);
    }

    public void storeBookingReferencesWithCourtScheduleIds(final JsonEnvelope jsonEnvelope, final StoreBookingReferenceCourtScheduleIds storeBookingReferenceCourtScheduleIds) {

        LOGGER.info("Store booking references with court schedule ids for hearing '{}' ", storeBookingReferenceCourtScheduleIds.getHearingId());

        this.sender.send(envelop(objectToJsonObjectConverter.convert(storeBookingReferenceCourtScheduleIds))
                .withName("progression.command.store-booking-reference-court-schedule-ids")
                .withMetadataFrom(jsonEnvelope));

    }

    public void populateHearingToProbationCaseworker(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        this.sender.send(envelop(payload)
                .withName("progression.command.populate-hearing-to-probation-caseworker")
                .withMetadataFrom(jsonEnvelope));
    }

    public void populateHearingToProbationCaseworker(final Metadata metadata, final UUID hearingId) {
        final JsonObject payload = Json.createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .build();

        final Metadata commandMetadata = metadataFrom(metadata).withName("progression.command.populate-hearing-to-probation-caseworker").build();

        sender.send(envelopeFrom(commandMetadata, payload));
    }

    private Optional<CourtApplication> getCourtApplication(final Hearing hearing, final UUID applicationId) {
        return hearing.getCourtApplications()
                .stream()
                .filter(courtApplication -> courtApplication.getId().equals(applicationId))
                .filter(courtApplication -> courtApplication.getCourtApplicationCases() != null)
                .findFirst();
    }

    public void initiateNewCourtApplication(final JsonEnvelope event, final InitiateApplicationForCaseRequested initiateApplicationForCaseRequested) {
        final Hearing hearing = initiateApplicationForCaseRequested.getHearing();
        final ProsecutionCase prosecutionCase = initiateApplicationForCaseRequested.getProsecutionCase();
        final Defendant defendant = initiateApplicationForCaseRequested.getDefendant();
        final NextHearing nextHearing = initiateApplicationForCaseRequested.getNextHearing();
        final UUID applicationId = initiateApplicationForCaseRequested.getApplicationId();
        final UUID oldApplicationId = initiateApplicationForCaseRequested.getOldApplicationId();
        final Boolean isAmended = initiateApplicationForCaseRequested.getIsAmended();
        final LocalDate issueDate = initiateApplicationForCaseRequested.getIssueDate();
        if (nonNull(referenceDataService.retrieveApplicationType(nextHearing.getApplicationTypeCode(), requester))) {
            final CourtCentre courtCentre = nextHearing.getCourtCentre();
            LOGGER.info("CourtCentre populated {}", courtCentre);

            final Organisation organisation = Organisation.organisation().withName(courtCentre.getName()).withAddress(courtCentre.getAddress()).build();
            LOGGER.info("Organization {}", organisation.getName());
            final List<CourtApplicationParty> respondents = new ArrayList<>();
            final MasterDefendant masterDefendant = MasterDefendant.masterDefendant()
                    .withPersonDefendant(defendant.getPersonDefendant())
                    .withMasterDefendantId(defendant.getMasterDefendantId())
                    .withAssociatedPersons(defendant.getAssociatedPersons())
                    .withDefendantCase(singletonList(DefendantCase.defendantCase()
                            .withCaseId(defendant.getProsecutionCaseId())
                            .withDefendantId(defendant.getId())
                            .withCaseReference(nonNull(prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference()) ?
                                    prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference() : prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                            .build()))
                    .build();
            respondents.add(
                    CourtApplicationParty.courtApplicationParty()
                            .withPersonDetails(defendant.getPersonDefendant().getPersonDetails())
                            .withNotificationRequired(false)
                            .withSummonsRequired(false)
                            .withId(defendant.getId())
                            .withMasterDefendant(masterDefendant)
                            .build());
            LOGGER.info("Respondents {}", respondents);

            final CourtApplicationParty subject = CourtApplicationParty.courtApplicationParty()
                    .withId(defendant.getId())
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .withMasterDefendant(masterDefendant)
                    .build();

            final CourtApplicationParty.Builder thirdPartyBuilder = CourtApplicationParty.courtApplicationParty();
            if (nextHearing.getProbationTeamName() != null) {
                thirdPartyBuilder.withOrganisation(Organisation.organisation()
                        .withName(nextHearing.getProbationTeamName())
                        .withAddress(Address.address()
                                .withAddress1(nextHearing.getProbationTeamAddress()).build()).build());
            }
            thirdPartyBuilder.withSummonsRequired(false);
            thirdPartyBuilder.withNotificationRequired(false);
            thirdPartyBuilder.withId(defendant.getId());

            final CourtApplicationParty applicant = CourtApplicationParty.courtApplicationParty()
                    .withId(nextHearing.getCourtCentre().getId())
                    .withOrganisation(Organisation.organisation()
                            .withName(nextHearing.getCourtCentre().getName())
                            .withAddress(nextHearing.getCourtCentre().getAddress()).build())
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build();


            final CourtApplicationCase courtApplicationCase = CourtApplicationCase.courtApplicationCase()
                    .withProsecutionCaseId(prosecutionCase.getId())
                    .withIsSJP(false)
                    .withCaseStatus(INACTIVE.getDescription())
                    .withProsecutionCaseIdentifier(prosecutionCase.getProsecutionCaseIdentifier()).build();
            final CourtApplication newCourtApplication = CourtApplication.courtApplication()
                    .withId(applicationId)
                    .withType(referenceDataService.retrieveApplicationType(nextHearing.getApplicationTypeCode(), requester))
                    .withApplicationReceivedDate(LocalDate.now())
                    .withApplicant(applicant)
                    .withCourtApplicationCases(Collections.singletonList(courtApplicationCase))
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withThirdParties(singletonList(thirdPartyBuilder.build()))
                    .withSubject(subject)
                    .withRespondents(respondents)
                    .withApplicationParticulars(getApplicationParticulars(nextHearing))
                    .build();

            LOGGER.info("==========    Case status for creating new application ================= {}", prosecutionCase.getCaseStatus());


            final CourtHearingRequest courtHearingRequest = CourtHearingRequest.courtHearingRequest()
                    .withHearingType(nextHearing.getType())
                    .withJurisdictionType(nextHearing.getJurisdictionType())
                    .withEndDate(nextHearing.getEndDate())
                    .withJurisdictionType(nextHearing.getJurisdictionType())
                    .withJudiciary(nextHearing.getJudiciary())
                    .withEarliestStartDateTime(hearing.getEarliestNextHearingDate())
                    .withListedStartDateTime(nextHearing.getListedStartDateTime())
                    .withCourtCentre(courtCentre)
                    .withEstimatedMinutes(nextHearing.getEstimatedMinutes())
                    .build();
            final JsonObjectBuilder commandBuilder = createObjectBuilder()
                    .add("courtApplication", objectToJsonObjectConverter.convert(newCourtApplication))
                    .add("courtHearing", objectToJsonObjectConverter.convert(courtHearingRequest))
                    .add("isAmended", isAmended)
                    .add("issueDate", issueDate.toString())
                    .add("isWelshTranslationRequired", isWelshTranslationRequired(hearing.getDefendantsWithWelshTranslationList(), defendant));

            if (nonNull(oldApplicationId)) {
                commandBuilder.add("oldApplicationId", oldApplicationId.toString());
            }

            sender.send(
                    envelop(commandBuilder.build())
                            .withName("progression.command.initiate-court-proceedings-for-application")
                            .withMetadataFrom(event));
        }

    }

    private boolean isWelshTranslationRequired(final List<DefendantsWithWelshTranslation> defendantsWithWelshTranslationList, final Defendant defendant){

        return nonNull(defendantsWithWelshTranslationList) && defendantsWithWelshTranslationList.stream()
                .anyMatch(dwl -> defendant.getId().equals(dwl.getDefendantId()) && dwl.getWelshTranslation());
    }

    private JsonArrayBuilder getRemitResultIdsAsJsonArray() {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        getCpActionResultMappingsForRemitResults()
                .stream()
                .map(a -> a.getResultRefId().toString())
                .forEach(arrayBuilder::add);
        return arrayBuilder;
    }

    private List<CpResultActionMapping> getCpActionResultMappingsForRemitResults() {
        return remitResultIds.computeIfAbsent("remitResultIds", key -> referenceDataService.getResultIdsByActionCode("REM", requester));
    }

    private String getApplicationParticulars(final NextHearing nextHearing) {
        return Stream.of(nextHearing.getOrderName(),
                        nonNull(nextHearing.getSuspendedPeriod()) ? "Suspended Period: " + nextHearing.getSuspendedPeriod() : null,
                        nonNull(nextHearing.getTotalCustodialPeriod()) ? "Total Custodial Period: " + nextHearing.getTotalCustodialPeriod() : null,
                        getEndDateString(nextHearing))
                .filter(applicationParticulars -> applicationParticulars != null && !applicationParticulars.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String getEndDateString(final NextHearing nextHearing) {
        return nextHearing.getEndDate() != null ? "End date: " + dateFormat.format(nextHearing.getEndDate()) : null;
    }

    public void updateCivilFees(final JsonEnvelope envelope, final ProsecutionCase prosecutionCase) {
        if (TRUE.equals(prosecutionCase.getIsCivil())) {
            LOGGER.info("Update fee type has been started for this case id {}", prosecutionCase.getId());
            final List<CivilFees> updatedCivilFeeList = prosecutionCase.getCivilFees().stream()
                    .map(civilFees -> CivilFees.civilFees()
                            .withValuesFrom(civilFees)
                            .withFeeType(CONTESTED)
                            .build()).collect(toList());

            final CivilFeesUpdated civilFeesUpdated = CivilFeesUpdated.civilFeesUpdated()
                    .withCaseId(prosecutionCase.getId())
                    .withCivilFees(updatedCivilFeeList)
                    .build();
            final JsonObject payload = objectToJsonObjectConverter.convert(civilFeesUpdated);

            final Metadata commandMetadata = metadataFrom(envelope.metadata())
                    .withName(PROGRESSION_COMMAND_UPDATE_CIVIL_FEES).build();
            sender.send(envelopeFrom(commandMetadata, payload));
        }
    }


}
