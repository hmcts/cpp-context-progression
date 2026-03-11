package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.FALSE;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssignDefendantRequestFromCurrentHearingToExtendHearing;
import uk.gov.justice.core.courts.AssignDefendantRequestToExtendHearing;
import uk.gov.justice.core.courts.CaseLinkedToHearing;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.ExtendHearing;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdateRequested;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OnlinePleaNotification;
import uk.gov.justice.core.courts.PrepareSummonsDataForExtendedHearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.progression.courts.HearingConfirmedReplayed;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;
import uk.gov.justice.progression.courts.UpdateRelatedHearingCommand;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.processor.exceptions.CourtApplicationAndCaseNotFoundException;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.CalendarService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.transformer.ProsecutionCasesReferredToCourtTransformer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:CallToDeprecatedMethod", "pmd:BeanMembersShouldSerialize"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingConfirmedEventProcessor {

    private static final String FIRST_HEARING = "First hearing";
    private static final long NUMBER_OF_WEEKDAYS_ELIGIBLE_FOR_ONLINE_PLEA_NOTIFICATION = 18;
    public static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    private static final String HEARING_INITIATE_COMMAND = "hearing.initiate";
    private static final String PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING = "progression.command-link-prosecution-cases-to-hearing";
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING = "progression.command.extend-hearing";
    private static final String PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE = "progression.command-enrich-hearing-initiate";
    private static final String PRIVATE_PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTEND_HEARING = "progression.command.prepare-summons-data-for-extended-hearing";
    private static final String PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING_DEFENDANT_REQUEST_UPDATE_REQUESTED = "progression.command.extend-hearing-defendant-request-update-requested";
    private static final String EITHER_WAY_ADULT_TEMPLATE_NAME = "plea_eitherway_offences_adult";
    private static final String EITHER_WAY_YOUTH_TEMPLATE_NAME = "plea_eitherway_offences_youth";
    private static final String INDICTABLE_ONLY_TEMPLATE_NAME = "plea_indictable_only_offences";
    private static final String WELSH_EITHER_WAY_ADULT_TEMPLATE_NAME = "welsh_plea_eitherway_offences_adult";
    private static final String WELSH_EITHER_WAY_YOUTH_TEMPLATE_NAME = "welsh_plea_eitherway_offences_youth";
    private static final String WELSH_INDICTABLE_ONLY_TEMPLATE_NAME = "welsh_plea_indictable_only_offences";

    private static final String PRIVATE_PROGRESSION_COMMAND_SEND_NOTIFICATION_FOR_AUTO_APPLICATION = "progression.command.send-notification-for-auto-application";
    private static final String NEW_HEARING_NOTIFICATION_TEMPLATE_NAME = "NewHearingNotification";

    private static final String EITHER_WAY = "Either Way";
    private static final String INDICTABLE = "Indictable";

    private static final String FEATURE_OPA = "OPA";

    public static final String HEARING_START_DATE_TIME = "hearingStartDateTime";
    public static final String JURISDICTION_TYPE = "jurisdictionType";
    public static final String COURT_APPLICATION = "courtApplication";
    public static final String COURT_CENTRE = "courtCentre";
    public static final String WARRANT_OF_FURTHER_DETENTION_HEARING_TYPE_ID = "638ced9d-3f95-4e99-b27b-47fa5a2c6add";
    public static final String PRE_CHARGE_BAIL_HEARING_TYPE_ID = "3a2d160f-363b-4360-96e1-0007a400a64c";

    @Inject
    private Logger LOGGER;
    @Inject
    ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationService notificationService;

    @Inject
    private PartialHearingConfirmService partialHearingConfirmService;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private ListingService listingService;

    @Inject
    private HearingNotificationHelper hearingNotificationHelper;

    @Inject
    private ApplicationParameters applicationParameters;


    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private CalendarService calendarService;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("progression.event.hearing-confirmed-replayed")
    public void processHearingConfirmedReplayed(final JsonEnvelope jsonEnvelope){
        final HearingConfirmedReplayed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmedReplayed.class);
        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();
        final Hearing hearingInProgression = hearingConfirmed.getHearingInProgression();
        boolean sendNotificationToParties = false;
        if (nonNull(hearingConfirmed.getSendNotificationToParties())) {
            sendNotificationToParties = hearingConfirmed.getSendNotificationToParties();
        }

        confirmHearing(jsonEnvelope, sendNotificationToParties,confirmedHearing,hearingInProgression );
    }

    @SuppressWarnings("squid:S3776")
    @Handles("public.listing.hearing-confirmed")
    public void processEvent(final JsonEnvelope jsonEnvelope) {

        LOGGER.info("public.listing.hearing-confirmed event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), jsonEnvelope.payloadAsJsonObject());

        final HearingConfirmed hearingConfirmed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingConfirmed.class);
        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();
        final Hearing hearingInProgression = progressionService.retrieveHearing(jsonEnvelope, confirmedHearing.getId());
        if(isNull(hearingInProgression)){
            sender.send(Enveloper
                    .envelop(hearingConfirmed)
                    .withName("progression.command.replay-hearing-confirmed")
                    .withMetadataFrom(jsonEnvelope));
            return;
        }
        boolean sendNotificationToParties = false;
        if (nonNull(hearingConfirmed.getSendNotificationToParties())) {
            sendNotificationToParties = hearingConfirmed.getSendNotificationToParties();
        }
        confirmHearing(jsonEnvelope, sendNotificationToParties,confirmedHearing,hearingInProgression );

    }

    private void confirmHearing(final JsonEnvelope jsonEnvelope, final boolean sendNotificationToParties, final ConfirmedHearing confirmedHearing, final Hearing hearingInProgression){

        triggerRetryOnMissingCaseAndApplication(confirmedHearing.getId(), hearingInProgression);

        if (nonNull(confirmedHearing.getExistingHearingId())) {
            final Optional<JsonObject> hearingFromDb = progressionService.getHearing(jsonEnvelope, confirmedHearing.getExistingHearingId().toString());
            if (isHearingInitialised(hearingFromDb)) {
                processExtendHearing(jsonEnvelope, confirmedHearing, hearingInProgression);
            }

        } else {

            final SeedingHearing seedingHearing = hearingInProgression.getSeedingHearing();

            final Initiate hearingInitiate = Initiate.initiate()
                    .withHearing(progressionService.transformConfirmedHearing(confirmedHearing, jsonEnvelope, seedingHearing))
                    .build();

            List<ProsecutionCase> deltaProsecutionCases = Collections.emptyList();
            if(!Optional.ofNullable(confirmedHearing.getIsGroupProceedings()).orElse(false)) {
                deltaProsecutionCases = processDeltaProsecutionCases(jsonEnvelope, confirmedHearing, hearingInProgression, seedingHearing, hearingInitiate);
            }

            final List<UUID> applicationIds = confirmedHearing.getCourtApplicationIds();
            final List<ConfirmedProsecutionCase> confirmedProsecutionCases = confirmedHearing.getProsecutionCases();

            final Hearing hearing = hearingInitiate.getHearing();
            final ZonedDateTime hearingStartDateTime = getEarliestDate(hearing.getHearingDays());
            LOGGER.info("List of application ids {} ", applicationIds);

            final List<CourtApplication> courtApplications = ofNullable(hearing.getCourtApplications()).orElse(new ArrayList<>());

            courtApplications.forEach(courtApplication -> LOGGER.info("sending notification for Application : {}", objectToJsonObjectConverter.convert(courtApplication)));

            sendApplicationNotification(jsonEnvelope, confirmedHearing.getType().getId().toString(), hearing, hearingStartDateTime, courtApplications);

            if (isNotEmpty(applicationIds)) {
                LOGGER.info("Based on JudicialResults of the application, update application status to LISTED for Applications with ids {}, in associate Hearing id: {}  ", applicationIds, hearing.getId());
                progressionService.updateCourtApplicationStatus(jsonEnvelope, hearing, applicationIds, ApplicationStatus.LISTED);
                progressionService.updateCaseStatus(jsonEnvelope, hearing, applicationIds);
            }

            if (isNotEmpty(confirmedProsecutionCases)) {
                confirmedProsecutionCases.forEach(prosecutionCase ->
                        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_EVENT_LINK_PROSECUTION_CASES_TO_HEARING).apply(
                                CaseLinkedToHearing.caseLinkedToHearing().withHearingId(hearing.getId()).withCaseId(prosecutionCase.getId()).build()))
                );

            }

            ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                    .forEach(prosecutionCase -> progressionService.increaseListingNumber(jsonEnvelope, prosecutionCase, hearing.getId()));


            if (!isBulkCase(confirmedHearing)) {
                progressionService.prepareSummonsData(jsonEnvelope, confirmedHearing);
            }

            final JsonObject hearingInitiateCommand = objectToJsonObjectConverter.convert(hearingInitiate);
            final JsonEnvelope hearingInitiateTransformedPayload = enveloper.withMetadataFrom(jsonEnvelope, PROGRESSION_PRIVATE_COMMAND_ERICH_HEARING_INITIATE).apply(hearingInitiateCommand);

            LOGGER.info(" hearing initiate transformed payload {}", hearingInitiateTransformedPayload.toObfuscatedDebugString());

            sender.send(hearingInitiateTransformedPayload);

            progressionService.updateDefendantYouthForProsecutionCase(jsonEnvelope, hearingInitiate, deltaProsecutionCases);
            if (featureControlGuard.isFeatureEnabled(FEATURE_OPA)) {
                sendOnlinePlea(jsonEnvelope, hearing);
            }
        }

        if (sendNotificationToParties) {
            sendHearingNotificationsToDefenceAndProsecutor(jsonEnvelope, confirmedHearing, hearingInProgression);
        } else {
            LOGGER.info("Notification is not sent for HearingId {}  , Notification sent flag {}", confirmedHearing.getId(), false);
        }
    }

    private void sendApplicationNotification(final JsonEnvelope jsonEnvelope, final String hearingTypeId, final Hearing hearing,
                                             final ZonedDateTime hearingStartDateTime, final List<CourtApplication> courtApplications) {
        boolean isWarrantHearingType = WARRANT_OF_FURTHER_DETENTION_HEARING_TYPE_ID.equalsIgnoreCase(hearingTypeId);
        boolean isPCBHearingType = PRE_CHARGE_BAIL_HEARING_TYPE_ID.equalsIgnoreCase(hearingTypeId);

        if (!isWarrantHearingType && !isPCBHearingType) {
            LOGGER.info("Sending notification as hearing type is not : Warrant of Further Detention or Pre-Charge Bail");
            courtApplications.forEach(courtApplication ->
                    sendNotification(jsonEnvelope, hearing, hearingStartDateTime, courtApplication)
            );
        }
    }

    private void sendNotification(final JsonEnvelope jsonEnvelope,
                                  final Hearing hearing,
                                  final ZonedDateTime hearingStartDateTime,
                                  final CourtApplication courtApplication) {

        JsonObject payload = createObjectBuilder()
                .add(HEARING_START_DATE_TIME, hearingStartDateTime.toString())
                .add(JURISDICTION_TYPE, hearing.getJurisdictionType().toString())
                .add(COURT_APPLICATION, objectToJsonObjectConverter.convert(courtApplication))
                .add(COURT_CENTRE, objectToJsonObjectConverter.convert(hearing.getCourtCentre()))
                .build();

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_SEND_NOTIFICATION_FOR_AUTO_APPLICATION)
                .apply(objectToJsonObjectConverter.convert(payload)));
    }

    private boolean hasNextHearing(CourtApplication courtApplication) {
        return courtApplication.getJudicialResults() != null &&
                courtApplication.getJudicialResults().stream()
                        .anyMatch(result -> result.getNextHearing() != null);
    }

    @SuppressWarnings("squid:S1188")
    private void sendOnlinePlea(final JsonEnvelope jsonEnvelope, final Hearing hearing) {
        if (FIRST_HEARING.equalsIgnoreCase(hearing.getType().getDescription())) {
            final LocalDate hearingDay = hearing.getHearingDays().stream()
                    .map(HearingDay::getSittingDay)
                    .sorted()
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new)
                    .toLocalDate();
            final long numberOfWorkingDaysBetweenTodayAndHearingDay = getNumberOfWorkingDaysBetweenTodayAndHearingDay(hearingDay);
            LOGGER.info("numberOfWorkingDaysBetweenTodayAndHearingDay: {}", numberOfWorkingDaysBetweenTodayAndHearingDay);

            if (numberOfWorkingDaysBetweenTodayAndHearingDay > NUMBER_OF_WEEKDAYS_ELIGIBLE_FOR_ONLINE_PLEA_NOTIFICATION && isNotEmpty(hearing.getProsecutionCases())) {
                final UUID courtCentreId = hearing.getCourtCentre().getId();
                final JsonObject courtCentreLocationJson = referenceDataService.getOrganisationUnitById(courtCentreId, jsonEnvelope, requester)
                        .orElseThrow(() -> new ReferenceDataNotFoundException("Court center ", courtCentreId.toString()));
                final Optional<JsonObject> courtCentreJsonOptional = referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, jsonEnvelope, requester);
                final JsonObject courtCentreJson = courtCentreJsonOptional.orElseThrow(() -> new IllegalArgumentException(String.format("Court centre '%s' not found", hearing.getCourtCentre().getId())));
                final boolean isWelshCourt = courtCentreJson.getBoolean("isWelsh", false);
                final LocalDateTime hearingDate = getHearingDateTime(hearing.getHearingDays());
                hearing.getProsecutionCases().forEach(prosecutionCase -> {
                    final String caseReference = prosecutionCase.getProsecutionCaseIdentifier().getCaseURN();
                    prosecutionCase.getDefendants().forEach(defendant -> {
                        final boolean eligible = isDefendantEligibleForPostalNotification(defendant);
                        if (eligible) {
                            final String defendantName = getDefendantName(defendant);
                            final OnlinePleaNotification onlinePleaNotification = OnlinePleaNotification.onlinePleaNotification()
                                    .withPostingDate(LocalDate.now())
                                    .withAddress(getDefendantAddress(defendant))
                                    .withCaseReferenceNumber(caseReference)
                                    .withOnlinePleaValidUntil(calculateOnlinePleaValidUntilDate(defendant))
                                    .withHearingDate(hearingDate.toLocalDate())
                                    .withDefendantName(defendantName)
                                    .withOffences(getOffences(isWelshCourt, defendant.getOffences()))
                                    .withCourtCentreLocation(courtCentreLocationJson.getString("oucodeL3Name"))
                                    .withHearingTime(getHearingTime(hearingDate))
                                    .build();
                            final JsonObject contentForPdf = objectToJsonObjectConverter.convert(onlinePleaNotification);
                            final String fileName = defendantName.replace(" ", "_") + RandomStringUtils.randomAlphabetic(10);
                            final String templateName = getTemplateName(isWelshCourt, defendant);
                            documentGeneratorService.generatePostalDocumentForOpa(sender, jsonEnvelope, contentForPdf, templateName, fileName, hearing.getId(), prosecutionCase.getId());
                        }
                    });
                });
            }
        }
    }

    private LocalDate calculateOnlinePleaValidUntilDate(final Defendant defendant) {
        return calendarService.plusWorkingDays(LocalDate.now(), getValidityPeriodOfOnlinePleaInDays(defendant), requester);
    }

    private static String getHearingTime(final LocalDateTime hearingDate) {
        return hearingDate.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    private long getValidityPeriodOfOnlinePleaInDays(final Defendant defendant) {
        if (isEitherWayNotification(defendant)) {
            return 11;
        } else {
            return 13;
        }
    }

    @SuppressWarnings("squid:S3776")
    private String getTemplateName(final boolean isWelshCourt, final Defendant defendant) {
        if (isEitherWayNotification(defendant)) {
            if (nonNull(defendant.getIsYouth()) && defendant.getIsYouth()) {
                if (isWelshCourt) {
                    return WELSH_EITHER_WAY_YOUTH_TEMPLATE_NAME;
                } else {
                    return EITHER_WAY_YOUTH_TEMPLATE_NAME;
                }
            } else {
                if (isWelshCourt) {
                    return WELSH_EITHER_WAY_ADULT_TEMPLATE_NAME;
                } else {
                    return EITHER_WAY_ADULT_TEMPLATE_NAME;
                }
            }
        } else {
            if (isWelshCourt) {
                return WELSH_INDICTABLE_ONLY_TEMPLATE_NAME;
            } else {
                return INDICTABLE_ONLY_TEMPLATE_NAME;
            }
        }
    }

    private List<String> getOffences(final boolean isWelshCourt, final List<Offence> offences) {
        if (isWelshCourt) {
            return offences.stream()
                    .map(Offence::getOffenceTitleWelsh)
                    .collect(toList());
        } else {
            return offences.stream()
                    .map(Offence::getOffenceTitle)
                    .collect(toList());
        }

    }

    private Address getDefendantAddress(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getAddress();
        }
        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getAddress();
        }
        return null;
    }

    public String getDefendantName(Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getFirstName() + " " + defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        if (nonNull(defendant.getLegalEntityDefendant()) && nonNull(defendant.getLegalEntityDefendant().getOrganisation())) {
            return defendant.getLegalEntityDefendant().getOrganisation().getName();
        }
        return null;
    }

    private LocalDateTime getHearingDateTime(List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new)
                .toLocalDateTime();
    }

    private boolean isEitherWayNotification(final Defendant defendant) {
        return defendant.getOffences().stream()
                .anyMatch(offence -> EITHER_WAY.equalsIgnoreCase(offence.getModeOfTrial()));
    }

    private boolean isDefendantEligibleForPostalNotification(final Defendant defendant) {
        return defendant.getOffences().stream()
                .anyMatch(offence -> EITHER_WAY.equalsIgnoreCase(offence.getModeOfTrial()) || INDICTABLE.equalsIgnoreCase(offence.getModeOfTrial()));
    }

    private long getNumberOfWorkingDaysBetweenTodayAndHearingDay(final LocalDate hearingDay) {
        final LocalDate today = LocalDate.now();
        final Predicate<LocalDate> isWeekend = day -> day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
        final long daysBetween = ChronoUnit.DAYS.between(today, hearingDay);
        return Stream.iterate(today, date -> date.plusDays(1))
                .limit(daysBetween)
                .filter((isWeekend).negate())
                .count();
    }
    @Handles("progression.event.send-notification-for-auto-application-initiated")
    public void sendNotificationForAutoApplication(final JsonEnvelope jsonEnvelope) {
        final SendNotificationForAutoApplicationInitiated sendNotificationForAutoApplication = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), SendNotificationForAutoApplicationInitiated.class);
        notificationService.sendNotificationForAutoApplication(jsonEnvelope, sendNotificationForAutoApplication);
    }
    private boolean isBulkCase(final ConfirmedHearing confirmedHearing) {
        return nonNull(confirmedHearing.getIsGroupProceedings()) && confirmedHearing.getIsGroupProceedings();
    }


    /**
     * If partial allocation is happened in confirm process, a new list hearing request generated
     * for the left over prosecutionCases. Regarding the hearing seeded or not, it calls
     * listNextCourtHearings or listCourtHearing. And also if the hearing has related seededHearing
     * , related seededHearingsProsecutionCases is removed from deltaProsecutionCases and the
     * related seed process raises for each related seedingHearing
     *
     * @param jsonEnvelope
     * @param confirmedHearing
     * @param hearingInProgression
     * @param seedingHearing
     * @param hearingInitiate
     */
    private List<ProsecutionCase> processDeltaProsecutionCases(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final Hearing hearingInProgression, final SeedingHearing seedingHearing, final Initiate hearingInitiate) {
        final List<ProsecutionCase> deltaProsecutionCases = partialHearingConfirmService.getDifferences(confirmedHearing, hearingInProgression);

        if (isNotEmpty(deltaProsecutionCases)) {
            updateHearingForPartialAllocation(jsonEnvelope, confirmedHearing, deltaProsecutionCases);
            HearingListingNeeds newHearingForRemainingUnallocatedOffences;
            if (nonNull(seedingHearing)) {
                final List<ProsecutionCase> deltaSeededProsecutionCases = partialHearingConfirmService.getDeltaSeededProsecutionCases(confirmedHearing, hearingInProgression, seedingHearing);
                final ListNextHearings listNextHearings = partialHearingConfirmService.transformToListNextCourtHearing(deltaSeededProsecutionCases, hearingInitiate.getHearing(), hearingInProgression, seedingHearing);
                listingService.listNextCourtHearings(jsonEnvelope, listNextHearings);
                newHearingForRemainingUnallocatedOffences = listNextHearings.getHearings().get(0);
                final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap = partialHearingConfirmService.getRelatedSeedingHearingsProsecutionCasesMap(confirmedHearing, hearingInProgression, seedingHearing);
                processCommandUpdateRelatedHearing(jsonEnvelope, newHearingForRemainingUnallocatedOffences, relatedSeedingHearingsProsecutionCasesMap);

            } else {
                final ListCourtHearing listCourtHearing = partialHearingConfirmService.transformToListCourtHearing(deltaProsecutionCases, hearingInitiate.getHearing(), hearingInProgression);
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
                newHearingForRemainingUnallocatedOffences = listCourtHearing.getHearings().get(0);
            }

            assignDefendantRequestFromCurrentHearingToExtendHearing(jsonEnvelope, hearingInitiate, newHearingForRemainingUnallocatedOffences);

            progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, Collections.singletonList(newHearingForRemainingUnallocatedOffences), seedingHearing);
        }

        return deltaProsecutionCases;
    }


    /**
     * if partial allocation happened in  seededHearing exists , the method  calls
     * "command.update-related-hearing" for each related seedingHearing.
     *
     * @param jsonEnvelope
     * @param hearingListingNeed
     * @param relatedSeedingHearingsProsecutionCasesMap
     */
    private void processCommandUpdateRelatedHearing(final JsonEnvelope jsonEnvelope, final HearingListingNeeds hearingListingNeed, final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap) {
        for (final Map.Entry<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearing : relatedSeedingHearingsProsecutionCasesMap.entrySet()) {
            final UpdateRelatedHearingCommand updateRelatedHearingCommand = UpdateRelatedHearingCommand.updateRelatedHearingCommand()
                    .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                            .withId(hearingListingNeed.getId())
                            .withProsecutionCases(relatedSeedingHearing.getValue())
                            .build())
                    .withSeedingHearing(relatedSeedingHearing.getKey())
                    .build();
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.update-related-hearing").apply(objectToJsonObjectConverter.convert(updateRelatedHearingCommand)));
        }
    }

    private void assignDefendantRequestFromCurrentHearingToExtendHearing(final JsonEnvelope jsonEnvelope, final Initiate hearingInitiate, final HearingListingNeeds hearingListingNeeds) {
        final JsonObject command = objectToJsonObjectConverter.convert(AssignDefendantRequestFromCurrentHearingToExtendHearing.assignDefendantRequestFromCurrentHearingToExtendHearing()
                .withCurrentHearingId(hearingInitiate.getHearing().getId())
                .withExtendHearingId(hearingListingNeeds.getId())
                .build());
        final JsonEnvelope payload = enveloper.withMetadataFrom(jsonEnvelope, "progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing").apply(command);
        sender.send(payload);
    }

    private void updateHearingForPartialAllocation(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final List<ProsecutionCase> deltaProsecutionCases) {
        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(confirmedHearing.getId(), deltaProsecutionCases);
        progressionService.updateHearingForPartialAllocation(jsonEnvelope, updateHearingForPartialAllocation);
    }

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(JsonEnvelope jsonEnvelope) {

        LOGGER.info(" hearing initiate with payload {}", jsonEnvelope.toObfuscatedDebugString());

        final Initiate hearingInitiate = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), Initiate.class);

        sender.send(enveloper.withMetadataFrom(jsonEnvelope, HEARING_INITIATE_COMMAND).apply(objectToJsonObjectConverter.convert(hearingInitiate)));
        if (isNotEmpty(hearingInitiate.getHearing().getProsecutionCases())) {
            final List<ProsecutionCasesReferredToCourt> prosecutionCasesReferredToCourts = ProsecutionCasesReferredToCourtTransformer
                    .transform(hearingInitiate, null);

            prosecutionCasesReferredToCourts.forEach(prosecutionCasesReferredToCourt -> {
                final JsonObject prosecutionCasesReferredToCourtJson = objectToJsonObjectConverter.convert(prosecutionCasesReferredToCourt);

                final JsonEnvelope caseReferToCourt = enveloper.withMetadataFrom(jsonEnvelope, PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT)
                        .apply(prosecutionCasesReferredToCourtJson);

                LOGGER.info(" Prosecution Cases Referred To Courts with payload {}", caseReferToCourt.toObfuscatedDebugString());

                sender.send(caseReferToCourt);
            });
            progressionService.updateHearingListingStatusToHearingInitiated(jsonEnvelope, hearingInitiate);
        } else {
            LOGGER.info("hearing-confirmed event populate hearing to probation caseworker for hearingId '{}' ", hearingInitiate.getHearing().getId());
            progressionService.populateHearingToProbationCaseworker(jsonEnvelope, hearingInitiate.getHearing().getId());
        }
    }

    private static ZonedDateTime getEarliestDate(final List<HearingDay> hearingDays) {
        return hearingDays.stream()
                .map(HearingDay::getSittingDay)
                .sorted()
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private void processExtendHearing(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing, final Hearing hearingInProgression) {
        LOGGER.info(" processing extend hearing for hearing id {}", confirmedHearing.getExistingHearingId());

        prepareSummonsDataForExtendHearing(jsonEnvelope, confirmedHearing);

        final Hearing incomingHearing = progressionService.transformToHearingFrom(confirmedHearing, jsonEnvelope);

        final HearingListingNeeds hearingListingNeeds =
                progressionService.transformHearingToHearingListingNeeds(incomingHearing, confirmedHearing.getExistingHearingId());

        final boolean isPartiallyAllocated = !partialHearingConfirmService.getDifferences(confirmedHearing, hearingInProgression).isEmpty();

        if (isPartiallyAllocated) {
            final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = partialHearingConfirmService.transformConfirmProsecutionCasesToUpdateHearingForPartialAllocation(confirmedHearing.getId(), confirmedHearing.getProsecutionCases());
            sender.send(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.update-hearing-for-partial-allocation").apply(objectToJsonObjectConverter.convert(updateHearingForPartialAllocation)));
        }

        ofNullable(incomingHearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .forEach(prosecutionCase -> progressionService.increaseListingNumber(jsonEnvelope, prosecutionCase, hearingListingNeeds.getId()));

        final ExtendHearing extendHearing = ExtendHearing.extendHearing()
                .withExtendedHearingFrom(confirmedHearing.getId())
                .withHearingRequest(hearingListingNeeds)
                .withIsAdjourned(FALSE)
                .withIsPartiallyAllocated(isPartiallyAllocated)
                .build();

        final JsonObject extendHearingCommand = objectToJsonObjectConverter.convert(extendHearing);

        final JsonEnvelope hearingExtendTransformedPayload =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING).apply(extendHearingCommand);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" hearing extend transformed payload {}", hearingExtendTransformedPayload.toObfuscatedDebugString());
        }

        sender.send(hearingExtendTransformedPayload);

        final Boolean fullExtension = confirmedHearing.getFullExtension();
        if (nonNull(fullExtension) && fullExtension.booleanValue()) {
            progressionService.sendListingCommandToDeleteHearing(jsonEnvelope, confirmedHearing.getId());
        }
    }

    @Handles("progression.event.defendant-request-from-current-hearing-to-extend-hearing-created")
    public void processDefendantRequestFromCurrentHearingToExtendHearingCreated(final JsonEnvelope jsonEnvelope) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.info("processing 'progression.event.defendant-request-from-current-hearing-to-extend-hearing-created' {}", jsonEnvelope.toObfuscatedDebugString());
        }

        final DefendantRequestFromCurrentHearingToExtendHearingCreated event =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantRequestFromCurrentHearingToExtendHearingCreated.class);

        final AssignDefendantRequestToExtendHearing command = AssignDefendantRequestToExtendHearing.assignDefendantRequestToExtendHearing()
                .withHearingId(event.getExtendHearingId())
                .withDefendantRequests(event.getDefendantRequests())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(command);

        final JsonEnvelope assignDefendantRequestToExtendHearingEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, "progression.command.assign-defendant-request-to-extend-hearing")
                        .apply(payload);

        sender.send(assignDefendantRequestToExtendHearingEnvelope);
    }

    @Handles("progression.event.extend-hearing-defendant-request-created")
    public void processExtendHearingDefendantRequestCreated(final JsonEnvelope jsonEnvelope) {

        LOGGER.info(" processing 'progression.event.extend-hearing-defendant-request-created' {}", jsonEnvelope.toObfuscatedDebugString());

        final ExtendHearingDefendantRequestCreated extendHearingDefendantRequestCreated =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ExtendHearingDefendantRequestCreated.class);

        final ExtendHearingDefendantRequestUpdateRequested extendHearingDefendantRequestUpdateRequested = ExtendHearingDefendantRequestUpdateRequested
                .extendHearingDefendantRequestUpdateRequested()
                .withDefendantRequests(extendHearingDefendantRequestCreated.getDefendantRequests())
                .withConfirmedHearing(extendHearingDefendantRequestCreated.getConfirmedHearing())
                .build();

        final JsonObject extendHearingDefendantRequestUpdateRequestedJson = objectToJsonObjectConverter.convert(extendHearingDefendantRequestUpdateRequested);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_EXTEND_HEARING_DEFENDANT_REQUEST_UPDATE_REQUESTED)
                        .apply(extendHearingDefendantRequestUpdateRequestedJson);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    @Handles("progression.event.extend-hearing-defendant-request-updated")
    public void processExtendHearingDefendantRequestUpdated(final JsonEnvelope jsonEnvelope) {

        LOGGER.info(" processing 'progression.event.extend-hearing-defendant-request-updated' {}", jsonEnvelope.toObfuscatedDebugString());

        final ExtendHearingDefendantRequestUpdated extendHearingDefendantRequestUpdated =
                jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ExtendHearingDefendantRequestUpdated.class);

        final ConfirmedHearing confirmedHearing = extendHearingDefendantRequestUpdated.getConfirmedHearing();

        final ConfirmedHearing confirmedHearingForSummons = ConfirmedHearing.confirmedHearing()
                .withId(confirmedHearing.getExistingHearingId())
                .withCourtCentre(confirmedHearing.getCourtCentre())
                .withHearingDays(confirmedHearing.getHearingDays())
                .withProsecutionCases(confirmedHearing.getProsecutionCases())
                .build();

        progressionService.prepareSummonsData(jsonEnvelope, confirmedHearingForSummons);

    }


    private void prepareSummonsDataForExtendHearing(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedHearing) {
        final PrepareSummonsDataForExtendedHearing prepareSummonsDataForExtendedHearing =
                PrepareSummonsDataForExtendedHearing.prepareSummonsDataForExtendedHearing()
                        .withConfirmedHearing(confirmedHearing)
                        .build();

        final JsonObject prepareSummonsDataJsonObject = objectToJsonObjectConverter.convert(prepareSummonsDataForExtendedHearing);

        final JsonEnvelope prepareSummonsDataJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_PREPARE_SUMMONS_DATA_FOR_EXTEND_HEARING)
                        .apply(prepareSummonsDataJsonObject);

        sender.send(prepareSummonsDataJsonEnvelope);
    }

    private boolean isHearingInitialised(final Optional<JsonObject> hearingIdFromQuery) {
        if (hearingIdFromQuery.isPresent()) {
            final String listingStatus = hearingIdFromQuery.get().getString("hearingListingStatus", null);
            if (nonNull(listingStatus) && listingStatus.equals(HearingListingStatus.HEARING_INITIALISED.name())) {
                LOGGER.info(" hearing listing status is : {}", listingStatus);
                return true;
            }
        }
        LOGGER.info(" hearing is not found ");
        return false;
    }

    private void triggerRetryOnMissingCaseAndApplication(final UUID hearingId, final Hearing hearingInProgression) {
        if (isEmpty(hearingInProgression.getCourtApplications()) && isEmpty(hearingInProgression.getProsecutionCases())) {

            throw new CourtApplicationAndCaseNotFoundException(format("Prosecution case and court application not found for hearing id : %s", hearingId));
        }
    }

    private void sendHearingNotificationsToDefenceAndProsecutor(final JsonEnvelope jsonEnvelope, final ConfirmedHearing confirmedUpdatedHearing, Hearing hearingInProgression) {
        final HearingNotificationInputData hearingNotificationInputData = new HearingNotificationInputData();
        Set<UUID> caseIds = new HashSet<>();

        final Map<UUID, List<UUID>> defendantOffenceListMap = new HashMap<>();
        final Set<UUID> defendantIdSet = new HashSet<>();

        if(isNotEmpty(confirmedUpdatedHearing.getProsecutionCases())){
            caseIds = confirmedUpdatedHearing.getProsecutionCases()
                    .stream().map(ConfirmedProsecutionCase::getId).collect(toSet());

            confirmedUpdatedHearing.getProsecutionCases().stream()
                    .flatMap(confirmedProsecutionCase -> confirmedProsecutionCase.getDefendants().stream())
                    .forEach(defendant -> {
                        defendantIdSet.add(defendant.getId());
                        defendantOffenceListMap.put(defendant.getId(),
                                defendant.getOffences().stream()
                                        .map(ConfirmedOffence::getId)
                                        .collect(toList()));
                    });
        } else if (isNotEmpty(hearingInProgression.getCourtApplications())) {
            final Set<UUID> courtApplicationCaseIdSet = new HashSet<>();
            hearingInProgression.getCourtApplications().stream().filter(courtApplication -> isNotEmpty(courtApplication.getCourtApplicationCases()))
                    .flatMap(courtApplication -> courtApplication.getCourtApplicationCases().stream())
                    .forEach(courtApplicationCase -> courtApplicationCaseIdSet.add(courtApplicationCase.getProsecutionCaseId()));

            caseIds.addAll(courtApplicationCaseIdSet);
            courtApplicationCaseIdSet.stream().forEach(applicationCaseId -> {
                final JsonObject prosecutionCaseJson = progressionService.getProsecutionCaseById(jsonEnvelope, applicationCaseId.toString());
                if (nonNull(prosecutionCaseJson)) {
                    final ProsecutionCase prosecutionCaseEntity = jsonObjectConverter.convert(prosecutionCaseJson.getJsonObject("prosecutionCase"), ProsecutionCase.class);
                    prosecutionCaseEntity.getDefendants().stream().forEach(defendant -> {
                        defendantIdSet.add(defendant.getId());
                        defendantOffenceListMap.put(defendant.getId(),
                                defendant.getOffences().stream()
                                        .map(Offence::getId)
                                        .collect(toList()));
                    });
                }

            });
        }


        final ZonedDateTime hearingStartDateTime = getEarliestDate(confirmedUpdatedHearing.getHearingDays());

        hearingNotificationInputData.setHearingType(confirmedUpdatedHearing.getType().getDescription());
        hearingNotificationInputData.setCaseIds(new ArrayList<>(caseIds));
        hearingNotificationInputData.setDefendantIds(new ArrayList<>(defendantIdSet));
        hearingNotificationInputData.setDefendantOffenceListMap(defendantOffenceListMap);
        hearingNotificationInputData.setTemplateName(NEW_HEARING_NOTIFICATION_TEMPLATE_NAME);
        hearingNotificationInputData.setHearingId(confirmedUpdatedHearing.getId());
        hearingNotificationInputData.setHearingDateTime(hearingNotificationHelper.getEarliestStartDateTime(hearingStartDateTime));
        hearingNotificationInputData.setEmailNotificationTemplateId(fromString(applicationParameters.getNotifyHearingTemplateId()));
        hearingNotificationInputData.setCourtCenterId(confirmedUpdatedHearing.getCourtCentre().getId());
        hearingNotificationInputData.setCourtRoomId(confirmedUpdatedHearing.getCourtCentre().getRoomId());

        hearingNotificationHelper.sendHearingNotificationsToRelevantParties(jsonEnvelope, hearingNotificationInputData);
    }


}
