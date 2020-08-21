package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.core.courts.DefendantsAddedToCourtProceedings;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.progression.courts.Hearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
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


            final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("hearingsAtAGlance"),
                    GetHearingsAtAGlance.class);

            final List<Hearings> futureHearings = hearingsAtAGlance.getHearings().stream()
                    .filter(hearing -> hearing.getHearingDays().stream()
                            .anyMatch(hearingDay -> hearingDay.getSittingDay().toLocalDate().compareTo(LocalDate.now()) >= 0))
                    .collect(Collectors.toList());

            sender.send(envelopeFrom(
                    metadataFrom(jsonEnvelope.metadata()).withName("public.progression.defendants-added-to-court-proceedings"),
                    payload
            ));

            final List<ListHearingRequest> newListHearingRequests = findDefendantListHearingRequest(futureHearings, defendantsAddedToCourtProceedings);

            if (!newListHearingRequests.isEmpty()) {
                final ListCourtHearing listCourtHearing = listCourtHearingTransformer.transform(jsonEnvelope,
                        Collections.singletonList(jsonObjectToObjectConverter.convert(prosecutionCaseJsonObject.get().getJsonObject("prosecutionCase"),
                                ProsecutionCase.class)), newListHearingRequests);
                listingService.listCourtHearing(jsonEnvelope, listCourtHearing);
            }
        }
    }

    private List<ListHearingRequest> findDefendantListHearingRequest(final List<Hearings> futureHearings, final DefendantsAddedToCourtProceedings defendantsAddedToCourtProceedings) {

        final ArrayList<ListHearingRequest> newListHearingRequests = new ArrayList<>();
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
                }
            }
        }
        return newListHearingRequests;
    }

    private boolean checkForSameCourtCentre(final ListHearingRequest listHearingRequest, final Hearings hearing) {
        return nonNull(hearing.getCourtCentre()) && nonNull(listHearingRequest.getCourtCentre()) &&
                Objects.equals(hearing.getCourtCentre().getId(), listHearingRequest.getCourtCentre().getId()) &&
                Objects.equals(hearing.getCourtCentre().getRoomId(), listHearingRequest.getCourtCentre().getRoomId());
    }

    private boolean checkForSameHearingDateTime(final ListHearingRequest listHearingRequest, final Hearings hearing) {
        return nonNull(hearing.getHearingDays()) &&
                hearing.getHearingDays().stream()
                        .anyMatch(hearingDay -> hearingDay.getSittingDay().compareTo(listHearingRequest.getListedStartDateTime()) == 0);
    }
}
