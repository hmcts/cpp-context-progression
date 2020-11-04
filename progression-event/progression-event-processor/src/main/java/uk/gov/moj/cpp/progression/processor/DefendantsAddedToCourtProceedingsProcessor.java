package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.HearingListingStatus;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class DefendantsAddedToCourtProceedingsProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

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


            if (!listHearingRequestByFutureAndNewHearings.getNewDefendantsAddedToExistingHearings().getListHearingRequests().isEmpty()) {
                sender.send(envelopeFrom(
                        metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-court-proceedings"),
                        payload
                ));
            }

            if (!listHearingRequestByFutureAndNewHearings.getNewDefendantsToCreateNewHearings().getListHearingRequests().isEmpty()) {
                final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                        Collections.singletonList(prosecutionCase), listHearingRequestByFutureAndNewHearings.getNewDefendantsToCreateNewHearings().getListHearingRequests());
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
                progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);
            }
        }
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

            ).collect(Collectors.toList());
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
                final boolean anyMatchedHearing = futureHearings.stream()
                        .anyMatch(hearing ->
                                checkForSameCourtCentre(listHearingRequest, hearing)
                                        && checkForSameHearingDateTime(listHearingRequest, hearing)
                        );
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

    private List<Defendant> getMatchingDefendants(final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings, final ArrayList<ListHearingRequest> existingListHearingRequests) {
        return defendantsAddedToCourtProceedings.getDefendants().stream()
                .filter(defendant -> existingListHearingRequests.stream()
                        .flatMap(listHearingRequest -> listHearingRequest.getListDefendantRequests().stream())
                        .anyMatch(listDefendantRequest -> Objects.equals(defendant.getId(), listDefendantRequest.getDefendantId()))
                ).collect(Collectors.toList());
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
