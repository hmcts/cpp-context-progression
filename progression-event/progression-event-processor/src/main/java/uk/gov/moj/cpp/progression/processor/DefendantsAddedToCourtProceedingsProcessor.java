package uk.gov.moj.cpp.progression.processor;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.progression.HearingRequest.hearingRequest;
import static uk.gov.moj.cpp.progression.task.Task.RETRY_ADD_DEFENDANT_TO_CASE;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ReplayedDefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.UpdateHearingWithNewDefendant;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Priority;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.progression.HearingRequest;
import uk.gov.moj.cpp.progression.processor.exceptions.CaseNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantsAddedToCourtProceedingsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantsAddedToCourtProceedingsProcessor.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    private Sender sender;

    @Inject
    private SummonsHearingRequestService summonsHearingRequestService;

    @Inject
    private ExecutionService executionService;

    @Inject
    private UtcClock utcClock;

    @Inject
    private ApplicationParameters applicationParameters;


    @Handles("progression.event.replayed-defendants-added-to-court-proceedings")
    public void processReplay(final JsonEnvelope jsonEnvelope) {
        final ReplayedDefendantsAddedToCourtProceedings replayedDefendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ReplayedDefendantsAddedToCourtProceedings.class);
        final String prosecutionCaseId = replayedDefendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString();
        final Integer interval = replayedDefendantsAddedToCourtProceedings.getInterval();

        final JsonObjectBuilder builder = createObjectBuilder();
        jsonEnvelope.payloadAsJsonObject().keySet().stream().filter(key -> !"interval".equals(key)).forEach(key -> builder.add(key, jsonEnvelope.payloadAsJsonObject().get(key)));
        JsonEnvelope envelope = JsonEnvelope.envelopeFrom(jsonEnvelope.metadata(), builder.build());

        addDefendantToCourtProceedings(envelope, prosecutionCaseId, replayedDefendantsAddedToCourtProceedings.getDefendants(), replayedDefendantsAddedToCourtProceedings.getListHearingRequests(), interval);
    }

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void process(final JsonEnvelope jsonEnvelope) {
        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), DefendantsAddedToCourtProceedings.class);
        final String prosecutionCaseId = defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString();

        addDefendantToCourtProceedings(jsonEnvelope, prosecutionCaseId, defendantsAddedToCourtProceedings.getDefendants(), defendantsAddedToCourtProceedings.getListHearingRequests(), 0);
    }

    private void addDefendantToCourtProceedings(final JsonEnvelope jsonEnvelope, final String prosecutionCaseId,  final List<Defendant> defendants, List<ListHearingRequest> listingRequests, int retryInterval) {
        final Optional<JsonObject> pcFromViewStore = progressionService.getProsecutionCaseDetailById(jsonEnvelope, prosecutionCaseId);
        if (pcFromViewStore.isPresent()) {
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(pcFromViewStore.get().getJsonObject("prosecutionCase"), ProsecutionCase.class);
            final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(pcFromViewStore.get().getJsonObject("hearingsAtAGlance"), GetHearingsAtAGlance.class);

            publishDefendantAddedToCase(jsonEnvelope, prosecutionCaseId);

            final List<Hearing> futureHearings = getFutureHearings(jsonEnvelope, getCaseUrn(prosecutionCase), hearingsAtAGlance);
            final List<HearingRequest> hearingRequests = separateNewAndAddToExistingHearingRequests(futureHearings, listingRequests);

            for (final HearingRequest hearingRequest : hearingRequests) {
                if (TRUE.equals(hearingRequest.getIsNewHearing())) {
                    createNewHearingForNewDefendant(jsonEnvelope, prosecutionCase, hearingRequest);
                } else {
                    addNewDefendantToExistingHearing(jsonEnvelope, hearingRequest, defendants, prosecutionCase);
                }
            }
        } else {
            Optional<Integer> retryInt = Arrays.stream(applicationParameters.getAddDefendantRetryIntervals().split("-")).sorted().skip(retryInterval).findFirst().map(Integer::valueOf);
            if(retryInt.isPresent()) {
                final JsonObjectBuilder builder = createObjectBuilder();
                jsonEnvelope.payloadAsJsonObject().forEach(builder::add);
                builder.add("interval", retryInterval+1);

                final ExecutionInfo executionInfo = new ExecutionInfo(createObjectBuilder()
                        .add("metadata", metadataFrom(jsonEnvelope.metadata()).withName("progression.command.replay-defendants-added-to-court-proceedings").build().asJsonObject())
                        .add("payload", builder.build())
                        .build(),
                        RETRY_ADD_DEFENDANT_TO_CASE.getTaskName(), utcClock.now().plusSeconds(retryInt.get()), STARTED, Priority.MEDIUM);

                executionService.executeWith(executionInfo);
            } else {
                throw new CaseNotFoundException("Prosecution case not found in view store  -->> " + prosecutionCaseId);
            }
        }
    }

    private void createNewHearingForNewDefendant(final JsonEnvelope jsonEnvelope,
                                                 final ProsecutionCase prosecutionCase,
                                                 final HearingRequest hearingRequest) {
        final UUID hearingId = randomUUID();
        final ListHearingRequest listHearingRequest = hearingRequest.getListHearingRequest();
        final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                singletonList(prosecutionCase), singletonList(listHearingRequest), hearingId, null);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);
        summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, listHearingRequest.getListDefendantRequests(), hearingId);

        LOGGER.info("Adding newly added defendants on case '{} to new hearing '{}'", prosecutionCase.getId(), hearingId);
    }

    private void addNewDefendantToExistingHearing(final JsonEnvelope jsonEnvelope,
                                                  final HearingRequest hearingRequest,
                                                  List<Defendant> defendants,
                                                  final ProsecutionCase prosecutionCase) {
        LOGGER.info("Adding newly added defendants on case '{} to existing hearing '{}'", prosecutionCase.getId(), hearingRequest.getHearingId());

        publishDefendantsAddedToCourtProceedings(jsonEnvelope);

        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.update-hearing-with-new-defendant"),
                transformToUpdateHearing(hearingRequest.getHearingId(), prosecutionCase.getId(), defendants));

        summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, hearingRequest.getListHearingRequest().getListDefendantRequests(), hearingRequest.getHearingId());
        increaseListingNumber(jsonEnvelope, prosecutionCase.getId(), hearingRequest.getHearingId(), getDefendantOffences(defendants));
    }

    public void increaseListingNumber(final JsonEnvelope jsonEnvelope, final UUID prosecutionCaseId, final UUID hearingId, final JsonArray offenceListingNumbersJsonArray) {
        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .add("hearingId", hearingId.toString())
                .add("offenceIds", offenceListingNumbersJsonArray);

        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.increase-listing-number-to-prosecution-case"),
                updateCommandBuilder.build());
    }

    private JsonObject transformToUpdateHearing(final UUID hearingId, final UUID prosecutionCaseId, final List<Defendant> defendants) {
        final UpdateHearingWithNewDefendant updateHearingWithNewDefendant = UpdateHearingWithNewDefendant.updateHearingWithNewDefendant()
                .withHearingId(hearingId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withDefendants(defendants).build();
        return objectToJsonObjectConverter.convert(updateHearingWithNewDefendant);
    }

    @Handles("progression.event.defendants-and-listing-hearing-requests-added")
    public void processDefendantsAndListHearingRequestsAdded(final JsonEnvelope jsonEnvelope) {
        sender.send(envelopeFrom(
                metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-hearing"),
                jsonEnvelope.payloadAsJsonObject()
        ));
    }

    public List<Hearing> getFutureHearings(final JsonEnvelope jsonEnvelope, final String caseURN, final GetHearingsAtAGlance hearingsAtAGlance) {
        final List<Hearing> futureHearings = listingService.getFutureHearings(jsonEnvelope, caseURN);

        if (hearingsAtAGlance == null || isEmpty(hearingsAtAGlance.getHearings())) {
            return futureHearings;
        } else {
            return futureHearings.stream()
                    .filter(futureHearing ->
                            hearingsAtAGlance.getHearings().stream()
                                    .noneMatch(hearings -> Objects.equals(futureHearing.getId(), hearings.getId())
                                            && hearings.getHearingListingStatus() == HearingListingStatus.HEARING_RESULTED))
                    .collect(toList());
        }
    }

    public String getCaseUrn(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() != null ?
                prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() :
                prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
    }

    private List<HearingRequest> separateNewAndAddToExistingHearingRequests(final List<Hearing> futureHearings, final List<ListHearingRequest> listingRequests) {
        final List<HearingRequest> hearingRequests = new ArrayList<>();

        for (final ListHearingRequest listHearingRequest : listingRequests) {
            final ZonedDateTime startDateTime = nonNull(listHearingRequest.getListedStartDateTime()) ? listHearingRequest.getListedStartDateTime() : listHearingRequest.getEarliestStartDateTime();

            if (startDateTime != null && !startDateTime.toLocalDate().isBefore(LocalDate.now())) {
                final HearingRequest hearingRequest = futureHearings.stream()
                        .filter(isAnyMatchingFutureHearingExists(listHearingRequest))
                        .map(hearing -> hearingRequest()
                                .withIsNewHearing(false)
                                .withHearingId(hearing.getId())
                                .withListHearingRequest(listHearingRequest)
                                .build())
                        .findFirst()
                        .orElse(hearingRequest()
                                .withIsNewHearing(true)
                                .withListHearingRequest(listHearingRequest)
                                .build());
                hearingRequests.add(hearingRequest);
            }
        }

        return hearingRequests;
    }

    private Predicate<Hearing> isAnyMatchingFutureHearingExists(final ListHearingRequest listHearingRequest) {
        return hearing ->
                checkForSameCourtCentre(listHearingRequest, hearing)
                        && checkForSameHearingDateTime(listHearingRequest, hearing);
    }

    private boolean checkForSameCourtCentre(final ListHearingRequest listHearingRequest, final Hearing hearing) {
        return nonNull(hearing.getCourtCentreId()) && nonNull(listHearingRequest.getCourtCentre()) &&
                Objects.equals(hearing.getCourtCentreId(), listHearingRequest.getCourtCentre().getId());
    }

    private boolean checkForSameHearingDateTime(final ListHearingRequest listHearingRequest, final Hearing hearing) {
        return nonNull(hearing.getHearingDays()) &&
                hearing.getHearingDays().stream()
                        .anyMatch(hearingDay -> hearingDay.getStartTime().toLocalDateTime().isEqual(listHearingRequest.getListedStartDateTime().toLocalDateTime()));
    }

    private static JsonArray getDefendantOffences(final List<Defendant> defendants) {
        final JsonArrayBuilder offenceIdArrayBuilder = Json.createArrayBuilder();
        defendants.stream()
                .flatMap(r -> r.getOffences().stream())
                .map(Offence::getId)
                .map(UUID::toString)
                .forEach(offenceIdArrayBuilder::add);
        return offenceIdArrayBuilder.build();
    }

    private void publishDefendantAddedToCase(final JsonEnvelope jsonEnvelope, final String prosecutionCaseId) {
        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.process-matched-defendants"),
                createObjectBuilder()
                        .add("prosecutionCaseId", prosecutionCaseId)
                        .build());

        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-case"),
                jsonEnvelope.payloadAsJsonObject());
    }

    private void publishDefendantsAddedToCourtProceedings(final JsonEnvelope jsonEnvelope) {
        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-court-proceedings"),
                jsonEnvelope.payloadAsJsonObject());

        publishEvent(metadataFrom(jsonEnvelope.metadata()).withName("progression.command.add-or-store-defendants-and-listing-hearing-requests"),
                jsonEnvelope.payloadAsJsonObject());
    }

    private void publishEvent(final MetadataBuilder metadata, final JsonObject payload) {
        sender.send(envelopeFrom(metadata, payload));
    }
}
