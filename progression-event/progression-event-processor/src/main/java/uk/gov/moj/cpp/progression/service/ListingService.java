package uk.gov.moj.cpp.progression.service;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.progression.processor.CasesReferredToCourtProcessor;
import uk.gov.moj.cpp.progression.service.dto.HearingList;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ListingService {

    public static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.list-court-hearing";
    public static final String LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    public static final String LISTING_SEARCH_HEARING = "listing.search.hearing";
    public static final String LISTING_ANY_ALLOCATION_SEARCH_HEARINGS = "listing.any-allocation.search.hearings";

    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private UtcClock utcClock;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public void listCourtHearing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing) {
        final JsonObject listCourtHearingJson = objectToJsonObjectConverter.convert(listCourtHearing);
        LOGGER.info(" Posting Send Case For Listing to listing '{}' ", listCourtHearingJson);
        sender.send(Enveloper.envelop(listCourtHearingJson).withName(LISTING_COMMAND_SEND_CASE_FOR_LISTING).withMetadataFrom(jsonEnvelope));
    }

    public void listUnscheduledHearings(final JsonEnvelope jsonEnvelope, final ListUnscheduledCourtHearing listUnscheduledCourtHearing) {
        final JsonObject payloadJson = objectToJsonObjectConverter.convert(listUnscheduledCourtHearing);
        LOGGER.info(" Posting UnscheduledCourtHearing to listing '{}' ", payloadJson);
        sender.send(Enveloper.envelop(payloadJson).withName(LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING).withMetadataFrom(jsonEnvelope));
    }

    public List<UUID> getShadowListedOffenceIds(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final Set<UUID> shadowListedOffenceIds = new HashSet<>();
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), LISTING_SEARCH_HEARING);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .build();
        final Hearing hearingListed = requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), Hearing.class).payload();
        ofNullable(hearingListed).ifPresent(hearing ->
                ofNullable(hearing.getListedCases()).ifPresent(listedCases -> {
                    final List<ListedCase> shadowListedCases = listedCases.stream()
                            .filter(listedCase -> ofNullable(listedCase.getShadowListed()).isPresent() && listedCase.getShadowListed().orElse(Boolean.FALSE))
                            .collect(Collectors.toList());

                    shadowListedCases.stream()
                            .forEach(listedCase ->
                                    listedCase.getDefendants().stream().forEach(defendant ->
                                            defendant.getOffences().stream()
                                                    .forEach(offence -> shadowListedOffenceIds.add(offence.getId()))));

                    listedCases.stream()
                            .filter(listedCase -> !shadowListedCases.contains(listedCase))
                            .forEach(listedCase ->
                                    listedCase.getDefendants().stream().forEach(defendant ->
                                            defendant.getOffences().stream()
                                                    .filter(offence -> ofNullable(offence.getShadowListed()).isPresent() && offence.getShadowListed().orElse(Boolean.FALSE))
                                                    .forEach(offence -> shadowListedOffenceIds.add(offence.getId()))));
                })
        );
        return shadowListedOffenceIds.stream().collect(Collectors.toList());
    }

    public List<Hearing> getFutureHearings(final JsonEnvelope jsonEnvelope, final String caseUrn) {
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), LISTING_ANY_ALLOCATION_SEARCH_HEARINGS);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("caseUrn", caseUrn)
                .build();
        final HearingList hearingListed = requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), HearingList.class).payload();

        if (hearingListed == null || hearingListed.getHearings() == null) {
            return Collections.emptyList();
        }
        return hearingListed.getHearings().stream()
                .filter(hearing -> hearing.getHearingDays() != null && hearing.getHearingDays().stream()
                        .anyMatch(hearingDay -> hearingDay.getStartTime().compareTo(utcClock.now()) >= 0))
                .collect(Collectors.toList());
    }
}
