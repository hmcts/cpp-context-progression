package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingUnallocatedCourtroomRemoved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;


@SuppressWarnings({"squid:S3655", "squid:S2629", "squid:CallToDeprecatedMethod", "pmd:BeanMembersShouldSerialize"})
@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingUnallocatedCourtroomRemovedEventProcessor {

     private static final String PRIVATE_PROGRESSION_COMMAND_HEARING_UNALLOCATED_COURTROOM_REMOVED = "progression.command.unallocate-hearing-remove-courtroom";

    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String PROGRESSION_COMMAND_DECREASE_LISTING_NUMBER_FOR_PROSECUTION_CASE = "progression.command.decrease-listing-number-for-prosecution-case";


    @Inject
    private Logger LOGGER;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private NotificationService notificationService;

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ListingService listingService;


    @Inject
    private RefDataService referenceDataService;

    @Inject
    private Requester requester;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Handles("public.listing.hearing-unallocated-courtroom-removed")
    public void processHearingUnallocatedCourtRoomRemoved(final JsonEnvelope jsonEnvelope) {

        LOGGER.info(" processing 'progression.event.hearing-unallocated-courtroom-removed' {}", jsonEnvelope.toObfuscatedDebugString());

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final JsonEnvelope hearingUnallocatedCourtroomRemovedPrivateJsonEnvelope =
                enveloper.withMetadataFrom(jsonEnvelope, PRIVATE_PROGRESSION_COMMAND_HEARING_UNALLOCATED_COURTROOM_REMOVED)
                        .apply(payload);

        sender.send(hearingUnallocatedCourtroomRemovedPrivateJsonEnvelope);
    }

    @Handles("progression.event.hearing-unallocated-courtroom-removed")
    public void handleHearingUnallocatedCourtroomRemoved(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved = jsonObjectToObjectConverter.convert(payload, HearingUnallocatedCourtroomRemoved.class);

        if (Objects.isNull(hearingUnallocatedCourtroomRemoved.getHearingId())) {
            return;
        }

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingUnallocatedCourtroomRemoved.getHearingId());
        if (hearingEntity == null || hearingEntity.getPayload() == null) {
            return;
        }

        final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
        if (hearingJson == null) {
            return;
        }

        final Hearing originalHearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
        if (originalHearing == null) {
            return;
        }

        final Map<UUID, List<UUID>> offenceIds = ofNullable(originalHearing.getProsecutionCases())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(prosecutionCase -> nonNull(prosecutionCase) && nonNull(prosecutionCase.getId()))
                .collect(Collectors.toMap(
                        ProsecutionCase::getId,
                        prosecutionCase -> ofNullable(prosecutionCase.getDefendants())
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .filter(defendant -> nonNull(defendant))
                                .flatMap(defendant -> ofNullable(defendant.getOffences())
                                        .map(Collection::stream)
                                        .orElseGet(Stream::empty)
                                        .filter(offence -> nonNull(offence) && nonNull(offence.getId()))
                                        .map(Offence::getId))
                                .collect(Collectors.toList())
                ));

        offenceIds.forEach((prosecutionCaseId, offenceIdsForCase) -> {
            if (isNotEmpty(offenceIdsForCase)) {
                sendCommandDecreaseListingNumberForProsecutionCase(event, prosecutionCaseId, offenceIdsForCase);
            }
        });


    }

    private void sendCommandDecreaseListingNumberForProsecutionCase(final JsonEnvelope jsonEnvelope, final UUID prosecutionCaseId, final List<UUID> offenceIds) {
        final JsonArrayBuilder offenceIdsBuilder = createArrayBuilder();
        offenceIds.forEach(id -> offenceIdsBuilder.add(id.toString()));

        final JsonObjectBuilder decreaseListingNumberCommandBuilder = createObjectBuilder()
                .add(PROSECUTION_CASE_ID, prosecutionCaseId.toString())
                .add("offenceIds", offenceIdsBuilder.build());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_DECREASE_LISTING_NUMBER_FOR_PROSECUTION_CASE),
                decreaseListingNumberCommandBuilder.build()));
    }
}
