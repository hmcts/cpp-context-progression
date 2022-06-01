package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequested;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.listing.courts.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.transformer.ListCourtHearingTransformer;

@ServiceComponent(EVENT_PROCESSOR)
public class ListHearingRequestedProcessor {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ListHearingRequestedProcessor.class);

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ListingService listingService;

    @Inject
    private ListCourtHearingTransformer listCourtHearingTransformer;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;


    @Handles("progression.event.list-hearing-requested")
    public void handle(final JsonEnvelope jsonEnvelope){

        final ListHearingRequested listHearingRequested = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), ListHearingRequested.class);

        final ListCourtHearing listCourtHearing = convertListCourtHearing(listHearingRequested, jsonEnvelope);

        listingService.listCourtHearing(jsonEnvelope, listCourtHearing);

        progressionService.updateHearingListingStatusToSentForListing(jsonEnvelope, listCourtHearing);

    }


    @Handles("public.listing.hearing-requested-for-listing")
    public void handlePublicEvent(final JsonEnvelope jsonEnvelope){
        final HearingRequestedForListing hearingRequestedForListing = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingRequestedForListing.class);

        final CourtCentre enrichedCourtCentre = progressionService.transformCourtCentre(hearingRequestedForListing.getListNewHearing().getCourtCentre(), jsonEnvelope);

        final HearingRequestedForListing enrichedHearingRequestedForListing = HearingRequestedForListing.hearingRequestedForListing()
                .withValuesFrom(hearingRequestedForListing)
                .withListNewHearing(CourtHearingRequest.courtHearingRequest()
                        .withValuesFrom(hearingRequestedForListing.getListNewHearing())
                        .withCourtCentre(enrichedCourtCentre)
                        .build())
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(enrichedHearingRequestedForListing))
                        .withName("progression.command.list-new-hearing")
                        .withMetadataFrom(jsonEnvelope));
    }

    @Handles("public.listing.hearing-partially-updated")
    public void handlePublicEventForPartiallyUpdate(final JsonEnvelope jsonEnvelope){
        LOGGER.info("Handling public.listing.hearing-partially-updated {}", jsonEnvelope.payload());

        HearingPartiallyUpdated hearingPartiallyUpdated = jsonObjectToObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingPartiallyUpdated.class);
        UpdateHearingForPartialAllocation updateHearingForPartialAllocation = UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingPartiallyUpdated.getHearingIdToBeUpdated())
                .withProsecutionCasesToRemove(hearingPartiallyUpdated.getProsecutionCases().stream()
                        .map(prosecutionCase -> ProsecutionCasesToRemove.prosecutionCasesToRemove()
                                .withCaseId(prosecutionCase.getCaseId())
                                .withDefendantsToRemove(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> DefendantsToRemove.defendantsToRemove()
                                                .withDefendantId(defendant.getDefendantId())
                                                .withOffencesToRemove(defendant.getOffences().stream()
                                                        .map(offence -> OffencesToRemove.offencesToRemove()
                                                                .withOffenceId(offence.getOffenceId())
                                                                .build())
                                                        .collect(Collectors.toList()))
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();

        sender.send(
                Enveloper.envelop(objectToJsonObjectConverter.convert(updateHearingForPartialAllocation))
                        .withName("progression.command.update-hearing-for-partial-allocation")
                        .withMetadataFrom(jsonEnvelope));
    }

    private ListCourtHearing convertListCourtHearing(ListHearingRequested listHearingRequested, final JsonEnvelope jsonEnvelope){
        final Set<UUID> caseIds = listHearingRequested.getListNewHearing().getListDefendantRequests().stream()
                .map(ListDefendantRequest::getProsecutionCaseId)
                .collect(Collectors.toSet());

        final List<ProsecutionCase> cases = caseIds.stream().map(caseId -> progressionService.getProsecutionCaseDetailById(jsonEnvelope, caseId.toString()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(jsonObject -> jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .collect(Collectors.toList());

        return listCourtHearingTransformer.transform(jsonEnvelope, cases, listHearingRequested.getListNewHearing(), listHearingRequested.getHearingId());
    }

}
