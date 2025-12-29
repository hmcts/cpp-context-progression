package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;

import uk.gov.justice.core.courts.CommittingCourt;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledCourtHearing;
import uk.gov.justice.core.courts.ListUnscheduledNextHearings;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.listing.courts.ListNextHearingsV3;
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
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.progression.processor.CasesReferredToCourtProcessor;
import uk.gov.moj.cpp.progression.service.dto.HearingList;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingService {

    private static final String LISTING_COMMAND_SEND_CASE_FOR_LISTING = "listing.command.list-court-hearing";
    private static final String LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS = "listing.list-next-hearings-v2";
    private static final String LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING = "listing.command.list-unscheduled-court-hearing";
    private static final String LISTING_COMMAND_SEND_UNSCHEDULED_NEXT_COURT_HEARINGS = "listing.list-unscheduled-next-hearings";
    private static final String LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String LISTING_ANY_ALLOCATION_SEARCH_HEARINGS = "listing.any-allocation.search.hearings";

    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtProcessor.class.getCanonicalName());

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private UtcClock utcClock;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public void listCourtHearing(final JsonEnvelope jsonEnvelope, final ListCourtHearing listCourtHearing) {
        final JsonObject listCourtHearingJson = objectToJsonObjectConverter.convert(listCourtHearing);
        LOGGER.info("Posting Send Case For Listing to listing '{}' ", listCourtHearingJson);
        sender.send(Enveloper.envelop(listCourtHearingJson).withName(LISTING_COMMAND_SEND_CASE_FOR_LISTING).withMetadataFrom(jsonEnvelope));
    }

    public void listNextCourtHearings(final JsonEnvelope jsonEnvelope, final ListNextHearings listNextHearings) {
        final JsonObject nextHearingsJson = objectToJsonObjectConverter.convert(listNextHearings);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Posting next hearings to listing for hearing '{}' ", listNextHearings.getHearingId());
        }

        sender.send(Enveloper.envelop(nextHearingsJson).withName(LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS).withMetadataFrom(jsonEnvelope));
    }

    public void listNextCourtHearings(final JsonEnvelope jsonEnvelope, final ListNextHearingsV3 listNextHearings) {
        final JsonObject nextHearingsJson = objectToJsonObjectConverter.convert(listNextHearings);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Posting next hearings to listing for hearing V3 '{}' ", listNextHearings.getHearingId());
        }
        sender.send(Enveloper.envelop(nextHearingsJson).withName(LISTING_COMMAND_SEND_LIST_NEXT_HEARINGS).withMetadataFrom(jsonEnvelope));
    }

    public void listUnscheduledHearings(final JsonEnvelope jsonEnvelope, final ListUnscheduledCourtHearing listUnscheduledCourtHearing) {
        final JsonObject payloadJson = objectToJsonObjectConverter.convert(listUnscheduledCourtHearing);
        LOGGER.info("Posting UnscheduledCourtHearing to listing '{}' ", payloadJson);
        sender.send(Enveloper.envelop(payloadJson).withName(LISTING_COMMAND_SEND_UNSCHEDULED_COURT_HEARING).withMetadataFrom(jsonEnvelope));
    }

    public void listUnscheduledNextHearings(final JsonEnvelope jsonEnvelope, final ListUnscheduledNextHearings listUnscheduledNextHearings) {
        LOGGER.info("Posting listUnscheduledNextHearings to listing for hearing '{}' ", listUnscheduledNextHearings.getHearingId());

        final JsonObject payloadJson = objectToJsonObjectConverter.convert(listUnscheduledNextHearings);
        sender.send(Enveloper.envelop(payloadJson).withName(LISTING_COMMAND_SEND_UNSCHEDULED_NEXT_COURT_HEARINGS).withMetadataFrom(jsonEnvelope));
    }

    public List<UUID> getShadowListedOffenceIds(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final Set<UUID> shadowListedOffenceIds = new HashSet<>();
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), LISTING_SEARCH_HEARING);
        final JsonObject jsonPayLoad = JsonObjects.createObjectBuilder()
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
        final JsonObject jsonPayLoad = JsonObjects.createObjectBuilder()
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

    public Optional<CommittingCourt> getCommittingCourt(final JsonEnvelope jsonEnvelope, final UUID hearingId) {

        final Hearing hearingListed = searchHearing(jsonEnvelope, hearingId);

        final CommittingCourt.Builder builder = CommittingCourt.committingCourt();

        if (nonNull(hearingListed) && nonNull(hearingListed.getListedCases())) {
            extractCommittingCourt(hearingListed, builder);
        }

        final CommittingCourt committingCourt = builder.build();

        if (nonNull(committingCourt.getCourtCentreId())) {
            LOGGER.info("Committing court found with ID: {}", committingCourt.getCourtCentreId());
        }
        return nonNull(committingCourt.getCourtHouseCode()) ? Optional.of(committingCourt) : empty();
    }

    private Hearing searchHearing(final JsonEnvelope jsonEnvelope, final UUID hearingId) {
        final Metadata metadata = metadataWithNewActionName(jsonEnvelope.metadata(), LISTING_SEARCH_HEARING);
        final JsonObject jsonPayLoad = JsonObjects.createObjectBuilder()
                .add("id", hearingId.toString())
                .build();
        return requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), Hearing.class).payload();
    }

    @SuppressWarnings("squid:S3655")
    private void extractCommittingCourt(final Hearing hearingListed, final CommittingCourt.Builder builder) {

        if (nonNull(hearingListed)) {
            hearingListed.getListedCases().forEach(lc ->
                    lc.getDefendants().forEach(ld -> {
                        final Offence offence = ld.getOffences()
                                .stream()
                                .filter(lo -> nonNull(lo.getCommittingCourt()) && lo.getCommittingCourt().isPresent())
                                .findFirst()
                                .orElse(null);

                        if (nonNull(offence) && offence.getCommittingCourt().isPresent()) {
                            final uk.gov.moj.cpp.listing.domain.CommittingCourt committingCourt = offence.getCommittingCourt().get();
                            builder.withCourtCentreId(committingCourt.getCourtCentreId())
                                    .withCourtHouseType(JurisdictionType.MAGISTRATES)
                                    .withCourtHouseShortName(committingCourt.getCourtHouseShortName().get())
                                    .withCourtHouseCode(committingCourt.getCourtHouseCode().get())
                                    .withCourtHouseName(committingCourt.getCourtHouseName());
                        }

                    }));
        }
    }
}
