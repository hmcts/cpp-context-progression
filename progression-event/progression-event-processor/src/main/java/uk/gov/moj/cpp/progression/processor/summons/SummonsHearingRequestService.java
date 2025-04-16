package uk.gov.moj.cpp.progression.processor.summons;

import static uk.gov.justice.core.courts.CreateHearingDefendantRequest.createHearingDefendantRequest;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.core.courts.CreateHearingApplicationRequest;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.model.HearingListing;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class SummonsHearingRequestService {

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    private static final String PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST = "progression.command.create-hearing-defendant-request";
    private static final String PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_REQUEST = "progression.command.create-hearing-application-request";

    public void addDefendantRequestToHearing(final JsonEnvelope jsonEnvelope, final List<HearingListing> hearingListingList) {
        hearingListingList.forEach(hearingListing -> {
            final JsonObject hearingDefendantRequestJson = objectToJsonObjectConverter.convert(createHearingDefendantRequest()
                    .withHearingId(hearingListing.hearingId())
                    .withDefendantRequests(hearingListing.listDefendantRequests())
                    .build());
            sender.send(envelop(hearingDefendantRequestJson)
                    .withName(PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST)
                    .withMetadataFrom(jsonEnvelope));
        });
    }

    public void addDefendantRequestToHearing(final JsonEnvelope jsonEnvelope, final List<ListDefendantRequest> listDefendantRequests, final UUID hearingId) {
        final JsonObject hearingDefendantRequestJson = objectToJsonObjectConverter.convert(createHearingDefendantRequest()
                .withHearingId(hearingId)
                .withDefendantRequests(listDefendantRequests)
                .build());
        sender.send(envelop(hearingDefendantRequestJson)
                .withName(PROGRESSION_COMMAND_CREATE_HEARING_DEFENDANT_REQUEST)
                .withMetadataFrom(jsonEnvelope));
    }

    public void addApplicationRequestToHearing(final JsonEnvelope jsonEnvelope, final CreateHearingApplicationRequest createHearingApplicationRequest) {
        final JsonObject hearingHearingApplicationRequestJson = objectToJsonObjectConverter.convert(createHearingApplicationRequest);
        sender.send(envelop(hearingHearingApplicationRequestJson)
                .withName(PROGRESSION_COMMAND_CREATE_HEARING_APPLICATION_REQUEST)
                .withMetadataFrom(jsonEnvelope));
    }
}
