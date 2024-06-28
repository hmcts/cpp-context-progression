package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.UpdateHearingWithNewDefendant;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.progression.processor.exceptions.CaseNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

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

    @Handles("progression.event.defendants-added-to-court-proceedings")
    public void process(final JsonEnvelope jsonEnvelope) {

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings = jsonObjectToObjectConverter.convert(payload, DefendantsAddedToCourtProceedings.class);

        final String prosecutionCaseId = defendantsAddedToCourtProceedings.getDefendants().get(0).getProsecutionCaseId().toString();
        final Optional<JsonObject> prosecutionCaseJsonObject = progressionService.getProsecutionCaseDetailById(jsonEnvelope, prosecutionCaseId);

        if (prosecutionCaseJsonObject.isPresent()) {

            final JsonObject jsonObject = createObjectBuilder()
                    .add("prosecutionCaseId", prosecutionCaseId)
                    .build();

            sender.send(envelopeFrom(
                    metadataFrom(jsonEnvelope.metadata()).withName("progression.command.process-matched-defendants"),
                    jsonObject
            ));

            sender.send(envelopeFrom(
                    metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-case"),
                    payload
            ));


            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                    ProsecutionCase.class);


            final List<Hearing> futureHearings = getFutureHearings(jsonEnvelope, prosecutionCase, prosecutionCaseJsonObject.get());

            final ListHearingRequestByFutureAndNewHearings listHearingRequestByFutureAndNewHearings =
                    separateAddDefendantsCourtProceedingsWithFutureHearings(futureHearings, defendantsAddedToCourtProceedings);


            final List<ListHearingRequest> listHearingRequestsForExistingHearing = listHearingRequestByFutureAndNewHearings.getNewDefendantsAddedToExistingHearings().getListHearingRequests();
            if (isNotEmpty(listHearingRequestsForExistingHearing)) {

                sender.send(envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-court-proceedings"),
                        jsonEnvelope.payloadAsJsonObject()
                ));

                sender.send(envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName("progression.command.add-or-store-defendants-and-listing-hearing-requests"),
                        payload
                ));

                // run the commands("progression.command.update-hearing-with-new-defendant" and  "progression.command.create-hearing-defendant-request") for each future hearings
                futureHearings.forEach(futureHearing -> {
                    LOGGER.info("Adding newly added defendants on case '{} to existing hearing '{}'", prosecutionCaseId, futureHearing.getId());
                    final List<UUID> offenceList = defendantsAddedToCourtProceedings.getDefendants().stream()
                            .flatMap(r -> r.getOffences().stream()).map(Offence::getId).collect(toList());
                    final JsonArrayBuilder offenceIdJsonArrayBuilder = Json.createArrayBuilder();
                    offenceList.stream().forEach(id -> offenceIdJsonArrayBuilder.add(id.toString()));
                    increaseListingNumber(jsonEnvelope, prosecutionCase.getId(), futureHearing.getId(), offenceIdJsonArrayBuilder.build());
                    summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, getListDefendantRequests(listHearingRequestsForExistingHearing), futureHearing.getId());
                    sender.send(envelopeFrom(
                            metadataFrom(jsonEnvelope.metadata()).withName("progression.command.update-hearing-with-new-defendant"),
                            tranformToUpdateHearing(futureHearing.getId(), prosecutionCaseId, defendantsAddedToCourtProceedings.getDefendants())
                    ));

                });

            }

            final List<ListHearingRequest> listHearingRequestsForNewHearing = listHearingRequestByFutureAndNewHearings.getNewDefendantsToCreateNewHearings().getListHearingRequests();
            if (isNotEmpty(listHearingRequestsForNewHearing)) {
                final UUID hearingId = randomUUID();
                final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                        Collections.singletonList(prosecutionCase), listHearingRequestsForNewHearing, hearingId);
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
                progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

                // add requests to the new hearing being created
                LOGGER.info("Adding newly added defendants on case '{} to new hearing '{}'", prosecutionCaseId, hearingId);
                summonsHearingRequestService.addDefendantRequestToHearing(jsonEnvelope, getListDefendantRequests(listHearingRequestsForNewHearing), hearingId);
            }

        } else {
            throw new CaseNotFoundException("Prosecution case not found in view store, so retrying -->> " + prosecutionCaseId);
        }
    }

    public void increaseListingNumber(final JsonEnvelope jsonEnvelope, final UUID prosecutionCaseId, final UUID hearingId, final JsonArray offenceListingNumbersJsonArray) {

        final JsonObjectBuilder updateCommandBuilder = createObjectBuilder()
                .add("prosecutionCaseId", prosecutionCaseId.toString())
                .add("hearingId", hearingId.toString())
                .add("offenceIds", offenceListingNumbersJsonArray);

        sender.send(JsonEnvelope.envelopeFrom(JsonEnvelope.metadataFrom(jsonEnvelope.metadata()).withName("progression.command.increase-listing-number-to-prosecution-case"),
                updateCommandBuilder.build()));
    }

    private JsonObject tranformToUpdateHearing(final UUID hearingId, final String prosecutionCaseId, final List<Defendant> defendants) {
        final UpdateHearingWithNewDefendant updateHearingWithNewDefendant = UpdateHearingWithNewDefendant.updateHearingWithNewDefendant()
                .withHearingId(hearingId)
                .withProsecutionCaseId(UUID.fromString(prosecutionCaseId))
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
    private List<ListDefendantRequest> getListDefendantRequests(final List<ListHearingRequest> listHearingRequestsForNewHearing) {
        return listHearingRequestsForNewHearing.stream().map(ListHearingRequest::getListDefendantRequests).flatMap(Collection::stream).collect(toList());
    }

    public List<Hearing> getFutureHearings(final JsonEnvelope jsonEnvelope, final ProsecutionCase prosecutionCase, final JsonObject prosecutionCaseJsonObject) {
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.getJsonObject("hearingsAtAGlance"),
                GetHearingsAtAGlance.class);
        final String caseURN = getCaseUrn(prosecutionCase);
        final List<Hearing> futureHearings = listingService.getFutureHearings(jsonEnvelope, caseURN);
        if (hearingsAtAGlance == null || hearingsAtAGlance.getHearings() == null) {
            return futureHearings;
        } else {
            return futureHearings.stream().filter(futureHearing ->
                    hearingsAtAGlance.getHearings().stream()
                            .noneMatch(hearings -> Objects.equals(futureHearing.getId(), hearings.getId())
                                    && hearings.getHearingListingStatus() == HearingListingStatus.HEARING_RESULTED)

            ).collect(toList());
        }
    }

    public String getCaseUrn(final ProsecutionCase prosecutionCase) {
        return prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() != null ?
                prosecutionCase.getProsecutionCaseIdentifier().getCaseURN() :
                prosecutionCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
    }

    private ListHearingRequestByFutureAndNewHearings separateAddDefendantsCourtProceedingsWithFutureHearings(final List<Hearing> futureHearings, final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings) {
        final ArrayList<ListHearingRequest> newListHearingRequests = new ArrayList<>();
        final ArrayList<ListHearingRequest> existingListHearingRequests = new ArrayList<>();

        for (final ListHearingRequest listHearingRequest : defendantsAddedToCourtProceedings.getListHearingRequests()) {

            final ZonedDateTime startDateTime = nonNull(listHearingRequest.getListedStartDateTime()) ? listHearingRequest.getListedStartDateTime() : listHearingRequest.getEarliestStartDateTime();
            if (startDateTime != null && startDateTime.toLocalDate().compareTo(LocalDate.now()) >= 0) {
                final boolean anyMatchedHearing = futureHearings.stream().anyMatch(getHearingMatchedPredicate(listHearingRequest));
                if (!anyMatchedHearing) {
                    newListHearingRequests.add(listHearingRequest);
                } else {
                    existingListHearingRequests.add(listHearingRequest);
                }
            }
        }
        final DefendantsAddedToCourtProceedings newDefendantsAddedToExistingHearings = DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
                .withListHearingRequests(existingListHearingRequests)
                .withDefendants(getMatchingDefendants(defendantsAddedToCourtProceedings, existingListHearingRequests))
                .build();
        final DefendantsAddedToCourtProceedings newDefendantsToCreateNewHearings = DefendantsAddedToCourtProceedings.defendantsAddedToCourtProceedings()
                .withListHearingRequests(newListHearingRequests)
                .withDefendants(getMatchingDefendants(defendantsAddedToCourtProceedings, newListHearingRequests))
                .build();

        return new ListHearingRequestByFutureAndNewHearings(newDefendantsAddedToExistingHearings, newDefendantsToCreateNewHearings);
    }


    private Predicate<Hearing> getHearingMatchedPredicate(final ListHearingRequest listHearingRequest) {
        return hearing ->
                checkForSameCourtCentre(listHearingRequest, hearing)
                        && checkForSameHearingDateTime(listHearingRequest, hearing);
    }

    private List<Defendant> getMatchingDefendants(final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings, final ArrayList<ListHearingRequest> existingListHearingRequests) {
        return defendantsAddedToCourtProceedings.getDefendants().stream()
                .filter(defendant -> existingListHearingRequests.stream()
                        .flatMap(listHearingRequest -> listHearingRequest.getListDefendantRequests().stream())
                        .anyMatch(listDefendantRequest -> Objects.equals(defendant.getId(), listDefendantRequest.getDefendantId()))
                ).collect(toList());
    }

    private boolean checkForSameCourtCentre(final ListHearingRequest listHearingRequest, final Hearing hearing) {
        return nonNull(hearing.getCourtCentreId()) && nonNull(listHearingRequest.getCourtCentre()) &&
                Objects.equals(hearing.getCourtCentreId(), listHearingRequest.getCourtCentre().getId());
    }

    private boolean checkForSameHearingDateTime(final ListHearingRequest listHearingRequest, final Hearing hearing) {
        return nonNull(hearing.getHearingDays()) &&
                hearing.getHearingDays().stream()
                        .anyMatch(hearingDay -> hearingDay.getStartTime().compareTo(listHearingRequest.getListedStartDateTime()) == 0);
    }

    private class ListHearingRequestByFutureAndNewHearings {
        DefendantsAddedToCourtProceedings newDefendantsAddedToExistingHearings;
        DefendantsAddedToCourtProceedings newDefendantsToCreateNewHearings;

        public ListHearingRequestByFutureAndNewHearings(final DefendantsAddedToCourtProceedings newDefendantsAddedToExistingHearings, final DefendantsAddedToCourtProceedings newDefendantsToCreateNewHearings) {
            this.newDefendantsAddedToExistingHearings = newDefendantsAddedToExistingHearings;
            this.newDefendantsToCreateNewHearings = newDefendantsToCreateNewHearings;
        }

        public DefendantsAddedToCourtProceedings getNewDefendantsAddedToExistingHearings() {
            return newDefendantsAddedToExistingHearings;
        }

        public void setNewDefendantsAddedToExistingHearings(final DefendantsAddedToCourtProceedings newDefendantsAddedToExistingHearings) {
            this.newDefendantsAddedToExistingHearings = newDefendantsAddedToExistingHearings;
        }

        public DefendantsAddedToCourtProceedings getNewDefendantsToCreateNewHearings() {
            return newDefendantsToCreateNewHearings;
        }

        public void setNewDefendantsToCreateNewHearings(final DefendantsAddedToCourtProceedings newDefendantsToCreateNewHearings) {
            this.newDefendantsToCreateNewHearings = newDefendantsToCreateNewHearings;
        }
    }
}
